/*
 * This file is part of NavalPlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                         Desenvolvemento Tecnolóxico de Galicia
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.navalplanner.business.orders.entities;

import static org.navalplanner.business.i18n.I18nHelper._;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.hibernate.validator.AssertTrue;
import org.hibernate.validator.InvalidValue;
import org.hibernate.validator.Valid;
import org.joda.time.LocalDate;
import org.navalplanner.business.advance.bootstrap.PredefinedAdvancedTypes;
import org.navalplanner.business.advance.entities.AdvanceAssignment;
import org.navalplanner.business.advance.entities.AdvanceType;
import org.navalplanner.business.advance.entities.DirectAdvanceAssignment;
import org.navalplanner.business.advance.entities.IndirectAdvanceAssignment;
import org.navalplanner.business.advance.exceptions.DuplicateAdvanceAssignmentForOrderElementException;
import org.navalplanner.business.advance.exceptions.DuplicateValueTrueReportGlobalAdvanceException;
import org.navalplanner.business.common.IntegrationEntity;
import org.navalplanner.business.common.Registry;
import org.navalplanner.business.common.daos.IIntegrationEntityDAO;
import org.navalplanner.business.common.exceptions.ValidationException;
import org.navalplanner.business.labels.entities.Label;
import org.navalplanner.business.materials.entities.MaterialAssignment;
import org.navalplanner.business.orders.daos.IOrderElementDAO;
import org.navalplanner.business.orders.entities.SchedulingState.Type;
import org.navalplanner.business.orders.entities.TaskSource.TaskSourceSynchronization;
import org.navalplanner.business.planner.entities.Task;
import org.navalplanner.business.planner.entities.TaskElement;
import org.navalplanner.business.planner.entities.TaskPositionConstraint;
import org.navalplanner.business.qualityforms.entities.QualityForm;
import org.navalplanner.business.qualityforms.entities.TaskQualityForm;
import org.navalplanner.business.requirements.entities.CriterionRequirement;
import org.navalplanner.business.requirements.entities.DirectCriterionRequirement;
import org.navalplanner.business.requirements.entities.IndirectCriterionRequirement;
import org.navalplanner.business.scenarios.entities.OrderVersion;
import org.navalplanner.business.scenarios.entities.Scenario;
import org.navalplanner.business.templates.entities.OrderElementTemplate;
import org.navalplanner.business.trees.ITreeNode;
import org.navalplanner.business.util.deepcopy.DeepCopy;

public abstract class OrderElement extends IntegrationEntity implements
        ICriterionRequirable, ITreeNode<OrderElement> {

    private InfoComponent infoComponent = new InfoComponent();

    private Date initDate;

    private Date deadline;

    @Valid
    protected Set<DirectAdvanceAssignment> directAdvanceAssignments = new HashSet<DirectAdvanceAssignment>();

    @Valid
    protected Set<MaterialAssignment> materialAssignments = new HashSet<MaterialAssignment>();

    @Valid
    private Set<Label> labels = new HashSet<Label>();

    private Set<TaskQualityForm> taskQualityForms = new HashSet<TaskQualityForm>();

    private Set<CriterionRequirement> criterionRequirements = new HashSet<CriterionRequirement>();

    protected OrderLineGroup parent;

    protected CriterionRequirementOrderElementHandler criterionRequirementHandler =
        CriterionRequirementOrderElementHandler.getInstance();

    /**
     * This field is transient
     */
    private SchedulingState schedulingState = null;

    private OrderElementTemplate template;

    private BigDecimal lastAdvanceMeausurementForSpreading = BigDecimal.ZERO;

    private Boolean dirtyLastAdvanceMeasurementForSpreading = true;

    private SumChargedHours sumChargedHours = SumChargedHours.create();

    public OrderElementTemplate getTemplate() {
        return template;
    }

    private String externalCode;

    private Map<OrderVersion, SchedulingDataForVersion> schedulingDatasForVersion = new HashMap<OrderVersion, SchedulingDataForVersion>();

    protected void removeVersion(OrderVersion orderVersion) {
        schedulingDatasForVersion.remove(orderVersion);
        for (OrderElement each : getChildren()) {
            each.removeVersion(orderVersion);
        }
    }

    private SchedulingDataForVersion.Data current = null;

    public SchedulingDataForVersion.Data getCurrentSchedulingData() {
        if (current == null) {
            throw new IllegalStateException(
                    "in order to use scheduling state related data "
                            + "useSchedulingDataFor(OrderVersion orderVersion) "
                            + "must be called first");
        }
        return current;
    }

    private void schedulingDataNowPointsTo(DeepCopy deepCopy,
            OrderVersion version) {
        current = getCurrentSchedulingData().pointsTo(deepCopy, version,
                schedulingVersionFor(version));
        for (OrderElement each : getChildren()) {
            each.schedulingDataNowPointsTo(deepCopy, version);
        }
    }

    protected void addNeededReplaces(DeepCopy deepCopy,
            OrderVersion newOrderVersion) {
        SchedulingDataForVersion currentVersion = getCurrentSchedulingData()
                .getVersion();
        SchedulingDataForVersion newSchedulingVersion = schedulingVersionFor(newOrderVersion);
        deepCopy.replace(currentVersion, newSchedulingVersion);
        for (OrderElement each : getChildren()) {
            each.addNeededReplaces(deepCopy, newOrderVersion);
        }
    }

    public SchedulingState getSchedulingState() {
        if (schedulingState == null) {
            schedulingState = SchedulingState.createSchedulingState(
                    getSchedulingStateType(), getChildrenStates(),
                    getCurrentSchedulingData().onTypeChangeListener());
        }
        return schedulingState;
    }

    private List<SchedulingState> getChildrenStates() {
        List<SchedulingState> result = new ArrayList<SchedulingState>();
        for (OrderElement each : getChildren()) {
            result.add(each.getSchedulingState());
        }
        return result;
    }

    public boolean hasSchedulingDataBeingModified() {
        return getCurrentSchedulingData().hasPendingChanges()
                || someSchedullingDataModified();
    }

    private boolean someSchedullingDataModified() {
        for (OrderElement each : getChildren()) {
            if (each.hasSchedulingDataBeingModified()) {
                return true;
            }
        }
        return false;
    }

    protected boolean isSchedulingDataInitialized() {
        return current != null;
    }

    public void useSchedulingDataFor(OrderVersion orderVersion) {
        useSchedulingDataFor(orderVersion, true);
    }

    public void useSchedulingDataFor(OrderVersion orderVersion,
            boolean recursive) {
        Validate.notNull(orderVersion);
        SchedulingDataForVersion schedulingVersion = schedulingVersionFor(orderVersion);
        if (recursive) {
            for (OrderElement each : getChildren()) {
                each.useSchedulingDataFor(orderVersion);
            }
        }
        current = schedulingVersion.makeAvailableFor(orderVersion);
    }

    private SchedulingDataForVersion schedulingVersionFor(
            OrderVersion orderVersion) {
        SchedulingDataForVersion currentSchedulingData = schedulingDatasForVersion
                .get(orderVersion);
        if (currentSchedulingData == null) {
            currentSchedulingData = SchedulingDataForVersion
                    .createInitialFor(this);
            schedulingDatasForVersion.put(orderVersion, currentSchedulingData);
        }
        return currentSchedulingData;
    }

    public SchedulingDataForVersion getCurrentSchedulingDataForVersion() {
        return getCurrentSchedulingData().getVersion();
    }

    protected void writeSchedulingDataChanges() {
        getCurrentSchedulingData().writeSchedulingDataChanges();
        for (OrderElement each : getChildren()) {
            each.writeSchedulingDataChanges();
        }
    }

    protected void writeSchedulingDataChangesTo(DeepCopy deepCopy,
            OrderVersion newOrderVersion) {
        schedulingDataNowPointsTo(deepCopy, newOrderVersion);
        writeSchedulingDataChanges();
    }

    protected void removeSpuriousDayAssignments(Scenario scenario) {
        removeAtNotCurrent(scenario);
        removeAtCurrent(scenario);
        for (OrderElement each : getChildren()) {
            each.removeSpuriousDayAssignments(scenario);
        }
    }

    private void removeAtNotCurrent(Scenario scenario) {
        SchedulingDataForVersion currentDataForVersion = getCurrentSchedulingDataForVersion();
        for (Entry<OrderVersion, SchedulingDataForVersion> each : schedulingDatasForVersion
                .entrySet()) {
            SchedulingDataForVersion dataForVersion = each.getValue();
            if (!currentDataForVersion.equals(dataForVersion)) {
                dataForVersion.removeSpuriousDayAssignments(scenario);
            }
        }
    }

    private void removeAtCurrent(Scenario scenario) {
        TaskElement associatedTaskElement = getAssociatedTaskElement();
        if (associatedTaskElement != null) {
            associatedTaskElement.removePredecessorsDayAssignmentsFor(scenario);
        }
    }

    public List<TaskSourceSynchronization> calculateSynchronizationsNeeded() {
        SchedulingDataForVersion schedulingDataForVersion = getCurrentSchedulingData()
                .getVersion();
        return calculateSynchronizationsNeeded(schedulingDataForVersion);
    }

    private List<TaskSourceSynchronization> calculateSynchronizationsNeeded(
            SchedulingDataForVersion schedulingDataForVersion) {
        List<TaskSourceSynchronization> result = new ArrayList<TaskSourceSynchronization>();
        if (isSchedulingPoint()) {
            result
                    .addAll(synchronizationForSchedulingPoint(schedulingDataForVersion));
        } else if (isSuperElementPartialOrCompletelyScheduled()) {
            removeUnscheduled(result);
            if (wasASchedulingPoint()) {
                result.add(taskSourceRemoval());
            }
            result
                    .add(synchronizationForSuperelement(schedulingDataForVersion));
        } else if (schedulingState.isNoScheduled()) {
            removeTaskSource(result);
        }
        return result;
    }

    private TaskSourceSynchronization synchronizationForSuperelement(
            SchedulingDataForVersion schedulingState) {
        List<TaskSourceSynchronization> childrenSynchronizations = childrenSynchronizations();
        if (thereIsNoTaskSource()) {
            getCurrentSchedulingData().requestedCreationOf(
                    TaskSource.createForGroup(schedulingState));
            return TaskSource.mustAddGroup(getTaskSource(),
                    childrenSynchronizations);
        } else {
            return getTaskSource().modifyGroup(childrenSynchronizations);
        }
    }

    private boolean wasASchedulingPoint() {
        return getTaskSource() != null
                && getTaskSource().getTask() instanceof Task;
    }

    private List<TaskSourceSynchronization> childrenSynchronizations() {
        List<TaskSourceSynchronization> childrenOfGroup = new ArrayList<TaskSourceSynchronization>();
        for (OrderElement orderElement : getSomewhatScheduledOrderElements()) {
            childrenOfGroup.addAll(orderElement
                    .calculateSynchronizationsNeeded());
        }
        return childrenOfGroup;
    }

    private void removeUnscheduled(List<TaskSourceSynchronization> result) {
        for (OrderElement orderElement : getNoScheduledOrderElements()) {
            orderElement.removeTaskSource(result);
        }
    }

    private List<TaskSourceSynchronization> synchronizationForSchedulingPoint(
            SchedulingDataForVersion schedulingState) {
        if (thereIsNoTaskSource()) {
            getCurrentSchedulingData().requestedCreationOf(
                    TaskSource.create(schedulingState, getHoursGroups()));
            return Collections.singletonList(TaskSource
                    .mustAdd(getTaskSource()));
        } else if (getTaskSource().getTask().isLeaf()) {
            return Collections.singletonList(getTaskSource()
                    .withCurrentHoursGroup(getHoursGroups()));
        } else {
            return synchronizationsForFromPartiallyScheduledToSchedulingPoint(schedulingState);
        }
    }

    private List<TaskSourceSynchronization> synchronizationsForFromPartiallyScheduledToSchedulingPoint(
            SchedulingDataForVersion schedulingState) {
        List<TaskSourceSynchronization> result = new ArrayList<TaskSourceSynchronization>();
        for (TaskSource each : getTaskSourcesFromBottomToTop()) {
            OrderElement orderElement = each.getOrderElement();
            result.add(orderElement.taskSourceRemoval());
        }
        TaskSource newTaskSource = TaskSource.create(schedulingState,
                getHoursGroups());
        getCurrentSchedulingData().requestedCreationOf(newTaskSource);
        result.add(TaskSource.mustAdd(newTaskSource));
        return result;
    }

    private boolean thereIsNoTaskSource() {
        return getTaskSource() == null;
    }

    private List<OrderElement> getSomewhatScheduledOrderElements() {
        List<OrderElement> result = new ArrayList<OrderElement>();
        for (OrderElement orderElement : getChildren()) {
            if (orderElement.getSchedulingStateType().isSomewhatScheduled()) {
                result.add(orderElement);
            }
        }
        return result;
    }

    private List<OrderElement> getNoScheduledOrderElements() {
        List<OrderElement> result = new ArrayList<OrderElement>();
        for (OrderElement orderElement : getChildren()) {
            if (orderElement.getSchedulingState().isNoScheduled()) {
                result.add(orderElement);
            }
        }
        return result;
    }

    private void removeTaskSource(List<TaskSourceSynchronization> result) {
        removeChildrenTaskSource(result);
        if (getTaskSource() != null) {
            result.add(taskSourceRemoval());
        }
    }

    private TaskSourceSynchronization taskSourceRemoval() {
        Validate.notNull(getTaskSource());
        TaskSourceSynchronization result = TaskSource
                .mustRemove(getTaskSource());
        getCurrentSchedulingData().taskSourceRemovalRequested();
        return result;
    }

    private void removeChildrenTaskSource(List<TaskSourceSynchronization> result) {
        List<OrderElement> children = getChildren();
        for (OrderElement each : children) {
            each.removeTaskSource(result);
        }
    }

    private boolean isSuperElementPartialOrCompletelyScheduled() {
        return getSchedulingState().isSomewhatScheduled();
    }

    public void initializeType(SchedulingState.Type type) {
        if (!isNewObject()) {
            throw new IllegalStateException();
        }
        getCurrentSchedulingData().initializeType(type);
        schedulingState = null;
    }

    public void initializeTemplate(OrderElementTemplate template) {
        if (!isNewObject()) {
            throw new IllegalStateException();
        }
        if (this.template != null) {
            throw new IllegalStateException("already initialized");
        }
        this.template = template;
    }

    public boolean isSchedulingPoint() {
        return getSchedulingState().getType() == Type.SCHEDULING_POINT;
    }

    public OrderLineGroup getParent() {
        return parent;
    }

    public TaskElement getAssociatedTaskElement() {
        if (getTaskSource() == null) {
            return null;
        } else {
            return getTaskSource().getTask();
        }
    }

    protected void setParent(OrderLineGroup parent) {
        this.parent = parent;
    }

    public abstract Integer getWorkHours();

    public abstract List<HoursGroup> getHoursGroups();

    public String getName() {
        return getInfoComponent().getName();
    }

    public void setName(String name) {
        this.getInfoComponent().setName(name);
    }

    public abstract boolean isLeaf();

    public abstract List<OrderElement> getChildren();

    private static Date copy(Date date) {
        return date != null ? new Date(date.getTime()) : date;
    }

    public Date getInitDate() {
        return copy(initDate);
    }

    public void setInitDate(Date initDate) {
        this.initDate = initDate;
    }

    public Date getDeadline() {
        return copy(deadline);
    }

    public void setDeadline(Date deadline) {
        this.deadline = deadline;
    }

    public void setDescription(String description) {
        this.getInfoComponent().setDescription(description);
    }

    public String getDescription() {
        return getInfoComponent().getDescription();
    }

    public abstract OrderLine toLeaf();

    public abstract OrderLineGroup toContainer();

    public boolean isScheduled() {
        return getTaskSource() != null;
    }

    public boolean checkAtLeastOneHoursGroup() {
        return (getHoursGroups().size() > 0);
    }

    public boolean isFormatCodeValid(String code) {

        if (code.contains("_")) {
            return false;
        }
        if (code.equals("")) {
            return false;
        }
        return true;
    }

    public void setCode(String code) {
        this.getInfoComponent().setCode(code);
    }

    public String getCode() {
        return getInfoComponent().getCode();
    }

    public abstract OrderElementTemplate createTemplate();

    public abstract DirectAdvanceAssignment getReportGlobalAdvanceAssignment();

    public abstract DirectAdvanceAssignment getAdvanceAssignmentByType(AdvanceType type);

    public DirectAdvanceAssignment getDirectAdvanceAssignmentByType(
            AdvanceType advanceType) {
        if (advanceType != null) {
            for (DirectAdvanceAssignment directAdvanceAssignment : getDirectAdvanceAssignments()) {
                if (directAdvanceAssignment.getAdvanceType().getId().equals(
                    advanceType.getId())) {
                return directAdvanceAssignment;
                }
            }
        }
        return null;
    }

    public Set<DirectAdvanceAssignment> getDirectAdvanceAssignments() {
        return Collections.unmodifiableSet(directAdvanceAssignments);
    }

    protected abstract Set<DirectAdvanceAssignment> getAllDirectAdvanceAssignments();

    public abstract Set<DirectAdvanceAssignment> getAllDirectAdvanceAssignments(
            AdvanceType advanceType);

    public abstract Set<IndirectAdvanceAssignment> getAllIndirectAdvanceAssignments(
            AdvanceType advanceType);

    public abstract Set<DirectAdvanceAssignment> getDirectAdvanceAssignmentsOfSubcontractedOrderElements();

    protected abstract Set<DirectAdvanceAssignment> getAllDirectAdvanceAssignmentsReportGlobal();

    public void removeAdvanceAssignment(AdvanceAssignment advanceAssignment) {
        if (directAdvanceAssignments.contains(advanceAssignment)) {
            directAdvanceAssignments.remove(advanceAssignment);
            if (this.getParent() != null) {
                this.getParent().removeIndirectAdvanceAssignment(
                        advanceAssignment.getAdvanceType());
                removeChildrenAdvanceInParents(this.getParent());
            }
            markAsDirtyLastAdvanceMeasurementForSpreading();
        }
    }

    public Set<Label> getLabels() {
        return Collections.unmodifiableSet(labels);
    }

    public void setLabels(Set<Label> labels) {
        this.labels = labels;
    }

    public void addLabel(Label label) {
        Validate.notNull(label);

        if (!checkAncestorsNoOtherLabelRepeated(label)) {
            throw new IllegalArgumentException(
                    _("Some ancestor has the same label assigned, "
                            + "so this element is already inheriting this label"));
        }

        removeLabelOnChildren(label);

        labels.add(label);
    }

    public void removeLabel(Label label) {
        labels.remove(label);
    }

    /**
     * Validate if the advanceAssignment can be added to the order element.The
     * list of advanceAssignments must be attached.
     * @param advanceAssignment
     *            must be attached
     * @throws DuplicateValueTrueReportGlobalAdvanceException
     * @throws DuplicateAdvanceAssignmentForOrderElementException
     */
    public void addAdvanceAssignment(
            DirectAdvanceAssignment newAdvanceAssignment)
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {
        checkNoOtherGlobalAdvanceAssignment(newAdvanceAssignment);
        checkAncestorsNoOtherAssignmentWithSameAdvanceType(this,
                newAdvanceAssignment);
        checkChildrenNoOtherAssignmentWithSameAdvanceType(this,
                newAdvanceAssignment);

        newAdvanceAssignment.setOrderElement(this);
        this.directAdvanceAssignments.add(newAdvanceAssignment);

        if (this.getParent() != null) {
            addChildrenAdvanceInParents(this.getParent());
            this.getParent().addIndirectAdvanceAssignment(
                    newAdvanceAssignment.createIndirectAdvanceFor(this.getParent()));
        }
    }

    public void addChildrenAdvanceInParents(OrderLineGroup parent) {
        if ((parent != null) && (!parent.existChildrenAdvance())) {
            parent.addChildrenAdvanceOrderLineGroup();
            addChildrenAdvanceInParents(parent.getParent());
        }

    }

    public void removeChildrenAdvanceInParents(OrderLineGroup parent) {
        if ((parent != null) && (parent.existChildrenAdvance())
                && (!itsChildsHasAdvances(parent))) {
            parent.removeChildrenAdvanceOrderLineGroup();
            removeChildrenAdvanceInParents(parent.getParent());
        }
    }

    private boolean itsChildsHasAdvances(OrderElement orderElement) {
        for (OrderElement child : orderElement.getChildren()) {
            if ((!child.getIndirectAdvanceAssignments().isEmpty())
                    || (!child.getDirectAdvanceAssignments().isEmpty())) {
                return true;
            }
            if (itsChildsHasAdvances(child)) {
                return true;
            }
        }
        return false;
    }

    protected void checkNoOtherGlobalAdvanceAssignment(
            DirectAdvanceAssignment newAdvanceAssignment)
            throws DuplicateValueTrueReportGlobalAdvanceException {
        if (!newAdvanceAssignment.getReportGlobalAdvance()) {
            return;
        }
        for (DirectAdvanceAssignment directAdvanceAssignment : directAdvanceAssignments) {
            if (directAdvanceAssignment.getReportGlobalAdvance()) {
                throw new DuplicateValueTrueReportGlobalAdvanceException(
                        _("Cannot spread two advances in the same task"),
                        this, OrderElement.class);
            }
        }
    }

    /**
     * It checks there are no {@link DirectAdvanceAssignment} with the same type
     * in {@link OrderElement} and ancestors
     *
     * @param orderElement
     * @param newAdvanceAssignment
     * @throws DuplicateAdvanceAssignmentForOrderElementException
     */
    private void checkAncestorsNoOtherAssignmentWithSameAdvanceType(
            OrderElement orderElement,
            DirectAdvanceAssignment newAdvanceAssignment)
            throws DuplicateAdvanceAssignmentForOrderElementException {
        for (DirectAdvanceAssignment directAdvanceAssignment : orderElement
                .getDirectAdvanceAssignments()) {

            if (AdvanceType.equivalentInDB(directAdvanceAssignment
                    .getAdvanceType(), newAdvanceAssignment.getAdvanceType())) {
                throw new DuplicateAdvanceAssignmentForOrderElementException(
                        _("Duplicate Advance Assignment For Task"),
                        this,
                        OrderElement.class);
            }
        }
        if (orderElement.getParent() != null) {
            checkAncestorsNoOtherAssignmentWithSameAdvanceType(orderElement
                    .getParent(), newAdvanceAssignment);
        }
    }

    /**
     * It checks there are no {@link AdvanceAssignment} with the same type in
     * orderElement and its children
     * @param orderElement
     * @param newAdvanceAssignment
     * @throws DuplicateAdvanceAssignmentForOrderElementException
     */
    protected void checkChildrenNoOtherAssignmentWithSameAdvanceType(
            OrderElement orderElement,
            DirectAdvanceAssignment newAdvanceAssignment)
            throws DuplicateAdvanceAssignmentForOrderElementException {
        for (DirectAdvanceAssignment directAdvanceAssignment : orderElement
                .getDirectAdvanceAssignments()) {
            if (AdvanceType.equivalentInDB(directAdvanceAssignment
                    .getAdvanceType(), newAdvanceAssignment.getAdvanceType())) {
                throw new DuplicateAdvanceAssignmentForOrderElementException(
                        _("Duplicate Advance Assignment For Task"),
                        this,
                        OrderElement.class);
            }
        }
        if (!orderElement.getChildren().isEmpty()) {
            for (OrderElement child : orderElement.getChildren()) {
                checkChildrenNoOtherAssignmentWithSameAdvanceType(child,
                        newAdvanceAssignment);
            }
        }
    }

    public BigDecimal getAdvancePercentage() {
        if ((dirtyLastAdvanceMeasurementForSpreading == null)
                || dirtyLastAdvanceMeasurementForSpreading) {
            lastAdvanceMeausurementForSpreading = getAdvancePercentage(null);
            dirtyLastAdvanceMeasurementForSpreading = false;
        }
        return lastAdvanceMeausurementForSpreading;
    }

    public abstract BigDecimal getAdvancePercentage(LocalDate date);

    public abstract Set<IndirectAdvanceAssignment> getIndirectAdvanceAssignments();

    public abstract DirectAdvanceAssignment calculateFakeDirectAdvanceAssignment(
            IndirectAdvanceAssignment indirectAdvanceAssignment);

    public abstract BigDecimal getAdvancePercentageChildren();

    public List<OrderElement> getAllChildren() {
        List<OrderElement> children = getChildren();
        List<OrderElement> result = new ArrayList<OrderElement>();
        for (OrderElement orderElement : children) {
            result.add(orderElement);
            result.addAll(orderElement.getAllChildren());
        }
        return result;
    }

    public void setCriterionRequirements(
            Set<CriterionRequirement> criterionRequirements) {
        this.criterionRequirements = criterionRequirements;
    }

    @Valid
    @Override
    public Set<CriterionRequirement> getCriterionRequirements() {
        return Collections.unmodifiableSet(criterionRequirements);
    }

    protected Set<CriterionRequirement> myCriterionRequirements() {
        return criterionRequirements;
    }


    /*
     * Operations to manage the criterion requirements of a orderElement
     * (remove, adding, update of the criterion requirement of the orderElement
     * such as the descendent's criterion requirement)
     */

    public void setValidCriterionRequirement(IndirectCriterionRequirement requirement,boolean valid){
        requirement.setValid(valid);
        criterionRequirementHandler.propagateValidCriterionRequirement(this,
                requirement.getParent(), valid);
    }

    public void removeDirectCriterionRequirement(DirectCriterionRequirement criterionRequirement){
        criterionRequirementHandler.propagateRemoveCriterionRequirement(this,
                criterionRequirement);
        removeCriterionRequirement(criterionRequirement);

    }

    protected void removeCriterionRequirement(CriterionRequirement requirement) {
        criterionRequirements.remove(requirement);
        if (requirement instanceof IndirectCriterionRequirement) {
            ((IndirectCriterionRequirement)requirement).getParent().
                    getChildren().remove((IndirectCriterionRequirement)requirement);
        }
    }

    @Override
    public void addCriterionRequirement(
            CriterionRequirement criterionRequirement) {
        criterionRequirementHandler.addCriterionRequirement(this, criterionRequirement);
    }

    public void addDirectCriterionRequirement(
            CriterionRequirement criterionRequirement) {
        criterionRequirementHandler.addDirectCriterionRequirement(this, criterionRequirement);
    }

    public void addIndirectCriterionRequirement(
            IndirectCriterionRequirement criterionRequirement) {
        criterionRequirementHandler.addIndirectCriterionRequirement(this,
                criterionRequirement);
    }

    protected void basicAddCriterionRequirement(
            CriterionRequirement criterionRequirement) {
            criterionRequirement.setOrderElement(this);
            this.criterionRequirements.add(criterionRequirement);
    }

    public void updateCriterionRequirements() {
        criterionRequirementHandler.updateMyCriterionRequirements(this);
        criterionRequirementHandler.propagateUpdateCriterionRequirements(this);
    }

    public boolean canAddCriterionRequirement(
            DirectCriterionRequirement newRequirement) {
        return criterionRequirementHandler.canAddCriterionRequirement(this,
                newRequirement);
    }

    protected Set<IndirectCriterionRequirement> getIndirectCriterionRequirement() {
        return criterionRequirementHandler.getIndirectCriterionRequirement(criterionRequirements);
    }

    public void applyStartConstraintTo(Task task) {
        boolean scheduleBackwards = getOrder().isScheduleBackwards();
        OrderElement current = this;
        while (current != null) {
            boolean applied = current.applyConstraintBasedOnInitOrEndDate(task,
                    scheduleBackwards);
            if (applied) {
                return;
            }
            current = current.getParent();
        }
        if (scheduleBackwards) {
            task.getPositionConstraint().asLateAsPossible();
        } else {
            task.getPositionConstraint().asSoonAsPossible();
        }
    }

    protected boolean applyConstraintBasedOnInitOrEndDate(Task task,
            boolean scheduleBackwards) {
        TaskPositionConstraint constraint = task.getPositionConstraint();
        if (getInitDate() != null
                && (getDeadline() == null || !scheduleBackwards)) {
            constraint.notEarlierThan(
                    LocalDate.fromDateFields(this.getInitDate()));
            return true;
        }
        if (getDeadline() != null) {
            constraint.finishNotLaterThan(
                    LocalDate.fromDateFields(this.getDeadline()));
            return true;
        }
        return false;
    }

    public Set<DirectCriterionRequirement> getDirectCriterionRequirement() {
        return criterionRequirementHandler
                .getDirectCriterionRequirement(criterionRequirements);
    }

    public SchedulingState.Type getSchedulingStateType() {
        return getCurrentSchedulingData().getSchedulingStateType();
    }

    public TaskSource getTaskSource() {
        return getCurrentSchedulingData().getTaskSource();
    }

    public Set<TaskElement> getTaskElements() {
        if (getTaskSource() == null) {
            return Collections.emptySet();
        }
        return Collections.singleton(getTaskSource().getTask());
    }

    public List<TaskSource> getTaskSourcesFromBottomToTop() {
        List<TaskSource> result = new ArrayList<TaskSource>();
        taskSourcesFromBottomToTop(result);
        return result;
    }

    public List<TaskSource> getAllScenariosTaskSourcesFromBottomToTop() {
        List<TaskSource> result = new ArrayList<TaskSource>();
        allScenariosTaskSourcesFromBottomToTop(result);
        return result;
    }

    public List<SchedulingDataForVersion> getSchedulingDatasForVersionFromBottomToTop() {
        List<SchedulingDataForVersion> result = new ArrayList<SchedulingDataForVersion>();
        schedulingDataForVersionFromBottomToTop(result);
        return result;
    }

    private void schedulingDataForVersionFromBottomToTop(
            List<SchedulingDataForVersion> result) {
        for (OrderElement each : getChildren()) {
            each.schedulingDataForVersionFromBottomToTop(result);
        }
        result.addAll(schedulingDatasForVersion.values());
    }

    private void taskSourcesFromBottomToTop(List<TaskSource> result) {
        for (OrderElement each : getChildren()) {
            each.taskSourcesFromBottomToTop(result);
        }
        if (getTaskSource() != null) {
            result.add(getTaskSource());
        }
    }

    private void allScenariosTaskSourcesFromBottomToTop(List<TaskSource> result) {
        for (OrderElement each : getChildren()) {
            each.allScenariosTaskSourcesFromBottomToTop(result);
        }
        for (Entry<OrderVersion, SchedulingDataForVersion> each : schedulingDatasForVersion
                .entrySet()) {
            TaskSource taskSource = each.getValue().getTaskSource();
            if (taskSource != null) {
                result.add(taskSource);
            }
        }
    }

    @Valid
    public Set<MaterialAssignment> getMaterialAssignments() {
        return Collections.unmodifiableSet(materialAssignments);
    }

    public void addMaterialAssignment(MaterialAssignment materialAssignment) {
        materialAssignments.add(materialAssignment);
        materialAssignment.setOrderElement(this);
    }

    public void removeMaterialAssignment(MaterialAssignment materialAssignment) {
        materialAssignments.remove(materialAssignment);
    }

    public BigDecimal getTotalMaterialAssigmentUnits() {
        BigDecimal result = BigDecimal.ZERO;

        final Set<MaterialAssignment> materialAssigments = getMaterialAssignments();
        for (MaterialAssignment each: materialAssigments) {
            result = result.add(each.getUnits());
        }
        return result;
    }

    public BigDecimal getTotalMaterialAssigmentPrice() {
        BigDecimal result = new BigDecimal(0);

        final Set<MaterialAssignment> materialAssigments = getMaterialAssignments();
        for (MaterialAssignment each: materialAssigments) {
            result = result.add(each.getTotalPrice());
        }
        return result;
    }

    public Order getOrder() {
        if (parent == null) {
            return null;
        }
        return parent.getOrder();
    }

    @Valid
    public Set<TaskQualityForm> getTaskQualityForms() {
        return Collections.unmodifiableSet(taskQualityForms);
    }

    public Set<QualityForm> getQualityForms() {
        Set<QualityForm> result = new HashSet<QualityForm>();
        for (TaskQualityForm each : taskQualityForms) {
            result.add(each.getQualityForm());
        }
        return result;
    }

    public void setTaskQualityFormItems(Set<TaskQualityForm> taskQualityForms) {
        this.taskQualityForms = taskQualityForms;
    }

    public TaskQualityForm addTaskQualityForm(QualityForm qualityForm)
            throws ValidationException {
        ckeckUniqueQualityForm(qualityForm);
        TaskQualityForm taskQualityForm = TaskQualityForm.create(this,
                qualityForm);
        this.taskQualityForms.add(taskQualityForm);
        return taskQualityForm;
    }

    public void removeTaskQualityForm(TaskQualityForm taskQualityForm) {
        this.taskQualityForms.remove(taskQualityForm);
    }

    private void ckeckUniqueQualityForm(QualityForm qualityForm)
            throws ValidationException, IllegalArgumentException {
        Validate.notNull(qualityForm);
        for (TaskQualityForm taskQualityForm : getTaskQualityForms()) {
            if (qualityForm.equals(taskQualityForm.getQualityForm())) {
                throw new ValidationException(new InvalidValue(_(
                        "{0} already exists", qualityForm.getName()),
                        QualityForm.class, "name", qualityForm.getName(),
                        qualityForm));
            }
        }
    }

    @Override
    public boolean checkConstraintUniqueCode() {
        // the automatic checking of this constraint is avoided because it uses
        // the wrong code property
        return true;
    }

    @AssertTrue(message = "a label can not be assigned twice in the same branch")
    public boolean checkConstraintLabelNotRepeatedInTheSameBranch() {
        return checkConstraintLabelNotRepeatedInTheSameBranch(new HashSet<Label>());
    }

    private boolean checkConstraintLabelNotRepeatedInTheSameBranch(
            HashSet<Label> parentLabels) {
        HashSet<Label> withThisLabels = new HashSet<Label>(parentLabels);
        for (Label label : getLabels()) {
            if (containsLabel(withThisLabels, label)) {
                return false;
            }
            withThisLabels.add(label);
        }
        for (OrderElement child : getChildren()) {
            if (!child
                    .checkConstraintLabelNotRepeatedInTheSameBranch(withThisLabels)) {
                return false;
            }
        }

        return true;
    }

    private boolean containsLabel(HashSet<Label> labels, Label label) {
        for (Label each : labels) {
            if (each.isEqualTo(label)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkAncestorsNoOtherLabelRepeated(Label newLabel) {
        for (Label label : labels) {
            if (label.isEqualTo(newLabel)) {
                return false;
            }
        }

        if (parent != null) {
            if (!((OrderElement) parent)
                    .checkAncestorsNoOtherLabelRepeated(newLabel)) {
                return false;
            }
        }
        return true;
    }

    private void removeLabelOnChildren(Label newLabel) {
        Label toRemove = null;

        for (Label label : labels) {
            if (label.equals(newLabel)) {
                toRemove = label;
                break;
            }
        }

        if (toRemove != null) {
            removeLabel(toRemove);
        }

        for (OrderElement child : getChildren()) {
            child.removeLabelOnChildren(newLabel);
        }
    }

    public boolean containsOrderElement(String code) {
        for (OrderElement child : getChildren()) {
            if (child.getCode().equals(code)) {
                return true;
            }
        }

        return false;
    }

    public OrderElement getOrderElement(String code) {
        if (code == null) {
            return null;
        }

        for (OrderElement child : getChildren()) {
            if (child.getCode().equals(code)) {
                return child;
            }
        }

        return null;
    }

    public boolean containsLabel(String code) {
        for (Label label : getLabels()) {
            if (label.getCode().equals(code)) {
                return true;
            }
        }

        return false;
    }

    public boolean containsMaterialAssignment(String materialCode) {
        for (MaterialAssignment materialAssignment : getMaterialAssignments()) {
            if (materialAssignment.getMaterial().getCode().equals(materialCode)) {
                return true;
            }
        }

        return false;
    }

    public MaterialAssignment getMaterialAssignment(String materialCode) {
        for (MaterialAssignment materialAssignment : getMaterialAssignments()) {
            if (materialAssignment.getMaterial().getCode().equals(materialCode)) {
                return materialAssignment;
            }
        }

        return null;
    }

    public DirectAdvanceAssignment getDirectAdvanceAssignmentSubcontractor() {
        for (DirectAdvanceAssignment directAdvanceAssignment : directAdvanceAssignments) {
            if (directAdvanceAssignment.getAdvanceType().getUnitName().equals(
                    PredefinedAdvancedTypes.SUBCONTRACTOR.getTypeName())) {
                return directAdvanceAssignment;
            }
        }

        return null;
    }

    public DirectAdvanceAssignment addSubcontractorAdvanceAssignment()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {
        boolean reportGlobalAdvance = false;
        if (getReportGlobalAdvanceAssignment() == null) {
            reportGlobalAdvance = true;
        }

        DirectAdvanceAssignment directAdvanceAssignment = DirectAdvanceAssignment
                .create(reportGlobalAdvance, new BigDecimal(100));
        directAdvanceAssignment
                .setAdvanceType(PredefinedAdvancedTypes.SUBCONTRACTOR.getType());

        addAdvanceAssignment(directAdvanceAssignment);
        return directAdvanceAssignment;
    }

    @Valid
    public InfoComponent getInfoComponent() {
        if (infoComponent == null) {
            infoComponent = new InfoComponent();
        }
        return infoComponent;
    }

    @Override
    public OrderElement getThis() {
        return this;
    }

    protected void setExternalCode(String externalCode) {
        this.externalCode = externalCode;
    }

    public String getExternalCode() {
        return externalCode;
    }

    public void moveCodeToExternalCode() {
        setExternalCode(getCode());
        setCode(null);

        for (OrderElement child : getChildren()) {
            child.moveCodeToExternalCode();
        }
    }

    public abstract OrderLine calculateOrderLineForSubcontract();

    public Set<MaterialAssignment> getAllMaterialAssignments() {
        Set<MaterialAssignment> result = new HashSet<MaterialAssignment>();

        result.addAll(getMaterialAssignments());

        for (OrderElement orderElement : getChildren()) {
            result.addAll(orderElement.getAllMaterialAssignments());
        }

        return result;
    }

    /**
     * Calculate if the tasks of the planification point has finished
     */

    public boolean isFinishPlanificationPointTask() {
        // look up into the order elements tree
        TaskElement task = lookToUpAssignedTask();
        if (task != null) {
            return task.getOrderElement().isFinishedAdvance();
        }
        // look down into the order elements tree
        List<TaskElement> listTask = lookToDownAssignedTask();
        if (!listTask.isEmpty()) {
            for (TaskElement taskElement : listTask) {
                if (!taskElement.getOrderElement().isFinishedAdvance()) {
                    return false;
                }
            }
        }
        // not exist assigned task
        IOrderElementDAO orderElementDAO = Registry.getOrderElementDAO();
        return (orderElementDAO.loadOrderAvoidingProxyFor(this))
                .isFinishedAdvance();
    }

    private TaskElement lookToUpAssignedTask() {
        OrderElement current = this;
        while (current != null) {
            if (isSchedulingPoint()) {
                return getAssociatedTaskElement();
            }
            current = current.getParent();
        }
        return null;
    }

    private List<TaskElement> lookToDownAssignedTask() {
        List<TaskElement> resultTask = new ArrayList<TaskElement>();
        for (OrderElement child : getAllChildren()) {
            if (child.isSchedulingPoint()) {
                TaskElement task = child.getAssociatedTaskElement();
                if (task != null) {
                    resultTask.add(task);
                }
            }
        }
        return resultTask;
    }

    public boolean isFinishedAdvance() {
        BigDecimal measuredProgress = getAdvancePercentage();
        measuredProgress = (measuredProgress.setScale(0, BigDecimal.ROUND_UP)
                .multiply(new BigDecimal(100)));
        return (measuredProgress.compareTo(new BigDecimal(100)) == 0);
    }

    @Override
    protected IIntegrationEntityDAO<OrderElement> getIntegrationEntityDAO() {
        return Registry.getOrderElementDAO();
    }

    public void markAsDirtyLastAdvanceMeasurementForSpreading() {
        if (parent != null) {
            parent.markAsDirtyLastAdvanceMeasurementForSpreading();
        }
        dirtyLastAdvanceMeasurementForSpreading = true;
    }

    public void setSumChargedHours(SumChargedHours sumChargedHours) {
        this.sumChargedHours = sumChargedHours;
    }

    public SumChargedHours getSumChargedHours() {
        return sumChargedHours;
    }

    public void updateAdvancePercentageTaskElement() {
        BigDecimal advancePercentage = this.getAdvancePercentage();
        if (this.getTaskSource() != null) {
            if (this.getTaskSource().getTask() != null) {
                this.getTaskSource().getTask().setAdvancePercentage(
                        advancePercentage);
            }
        }
    }

    public static void checkConstraintOrderUniqueCode(OrderElement order) {
        OrderElement repeatedOrder;

        // Check no code is repeated in this order
        if (order instanceof OrderLineGroup) {
            repeatedOrder = ((OrderLineGroup) order).findRepeatedOrderCode();
            if (repeatedOrder != null) {
                throw new ValidationException(_(
                        "Repeated Project code {0} in Project {1}",
                        repeatedOrder.getCode(), repeatedOrder.getName()));
            }
        }

        // Check no code is repeated within the DB
        repeatedOrder = Registry.getOrderElementDAO()
                .findRepeatedOrderCodeInDB(order);
        if (repeatedOrder != null) {
            throw new ValidationException(_(
                    "Repeated Project code {0} in Project {1}",
                    repeatedOrder.getCode(), repeatedOrder.getName()));
        }
    }

    public void setCodeAutogenerated(Boolean codeAutogenerated) {
        if (getOrder().equals(this)) {
            super.setCodeAutogenerated(codeAutogenerated);
        }
    }

    public Boolean isCodeAutogenerated() {
        if (getOrder().equals(this)) {
            return super.isCodeAutogenerated();
        }
        return getOrder() != null ? getOrder().isCodeAutogenerated() : false;
    }
}
