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

package org.navalplanner.business.planner.entities;

import static org.navalplanner.business.workingday.EffortDuration.min;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.validator.Valid;
import org.joda.time.LocalDate;
import org.navalplanner.business.calendars.entities.AvailabilityTimeLine;
import org.navalplanner.business.calendars.entities.ICalendar;
import org.navalplanner.business.planner.entities.EffortDistributor.IResourceSelector;
import org.navalplanner.business.planner.entities.EffortDistributor.ResourceWithAssignedDuration;
import org.navalplanner.business.planner.entities.allocationalgorithms.HoursModification;
import org.navalplanner.business.planner.entities.allocationalgorithms.ResourcesPerDayModification;
import org.navalplanner.business.resources.daos.IResourceDAO;
import org.navalplanner.business.resources.entities.Criterion;
import org.navalplanner.business.resources.entities.CriterionCompounder;
import org.navalplanner.business.resources.entities.ICriterion;
import org.navalplanner.business.resources.entities.Resource;
import org.navalplanner.business.scenarios.entities.Scenario;
import org.navalplanner.business.util.deepcopy.OnCopy;
import org.navalplanner.business.util.deepcopy.Strategy;
import org.navalplanner.business.workingday.EffortDuration;
import org.navalplanner.business.workingday.ResourcesPerDay;
import org.navalplanner.business.workingday.TaskDate;

/**
 * Represents the relation between {@link Task} and a generic {@link Resource}.
 * @author Diego Pino García <dpino@igalia.com>
 */
public class GenericResourceAllocation extends
        ResourceAllocation<GenericDayAssignment> {

    public static GenericResourceAllocation create() {
        return create(new GenericResourceAllocation());
    }

    public static GenericResourceAllocation createForTesting(
            ResourcesPerDay resourcesPerDay, Task task) {
        return create(new GenericResourceAllocation(
                resourcesPerDay, task));
    }

    public static Map<Set<Criterion>, List<GenericResourceAllocation>> byCriterions(
            Collection<GenericResourceAllocation> genericAllocations) {
        Map<Set<Criterion>, List<GenericResourceAllocation>> result = new HashMap<Set<Criterion>, List<GenericResourceAllocation>>();
        for (GenericResourceAllocation genericResourceAllocation : genericAllocations) {
            Set<Criterion> criterions = genericResourceAllocation.getCriterions();
            if(! result.containsKey(criterions)){
                result.put(criterions, new ArrayList<GenericResourceAllocation>());
            }
            result.get(criterions).add(genericResourceAllocation);
        }
        return result;
    }

    public static Map<Resource, List<GenericResourceAllocation>> byResource(
            Collection<GenericResourceAllocation> allocations) {
        Map<Resource, List<GenericResourceAllocation>> result = new HashMap<Resource, List<GenericResourceAllocation>>();
        for (GenericResourceAllocation resourceAllocation : allocations) {
            for (Resource resource : resourceAllocation
                    .getAssociatedResources()) {
                initializeIfNeeded_(result, resource);
                result.get(resource).add(resourceAllocation);
            }
        }
        return result;
    }

    private static void initializeIfNeeded_(
            Map<Resource, List<GenericResourceAllocation>> result,
            Resource resource) {
        if (!result.containsKey(resource)) {
            result.put(resource, new ArrayList<GenericResourceAllocation>());
        }
    }

    @OnCopy(Strategy.SHARE_COLLECTION_ELEMENTS)
    private Set<Criterion> criterions = new HashSet<Criterion>();

    private Set<GenericDayAssignmentsContainer> genericDayAssignmentsContainers = new HashSet<GenericDayAssignmentsContainer>();

    @Valid
    private Set<GenericDayAssignmentsContainer> getGenericDayAssignmentsContainers() {
        return new HashSet<GenericDayAssignmentsContainer>(
                genericDayAssignmentsContainers);
    }

    @OnCopy(Strategy.IGNORE)
    private Map<Resource, List<GenericDayAssignment>> orderedDayAssignmentsByResource = null;

    private GenericResourceAllocation(ResourcesPerDay resourcesPerDay, Task task) {
        super(resourcesPerDay, task);
    }

    /**
     * Constructor for Hibernate. DO NOT USE!
     */
    public GenericResourceAllocation() {
        this.assignmentsState = buildFromDBState();
    }

    public static GenericResourceAllocation create(Task task) {
        return create(new GenericResourceAllocation(
                task));
    }

    public static GenericResourceAllocation createForLimiting(Task task,
            Collection<? extends Criterion> criterions) {
        GenericResourceAllocation result = new GenericResourceAllocation(task);
        result.criterions = new HashSet<Criterion>(criterions);
        result.setResourcesPerDayToAmount(1);
        return create(result);
    }

    private GenericResourceAllocation(Task task) {
        super(task);
        this.criterions = task.getCriterions();
        this.assignmentsState = buildInitialTransientState();
    }

    private GenericDayAssignmentsContainer retrieveOrCreateContainerFor(
            Scenario scenario) {
        Map<Scenario, GenericDayAssignmentsContainer> containers = containersByScenario();
        GenericDayAssignmentsContainer retrieved = containers.get(scenario);
        if (retrieved != null) {
            return retrieved;
        }
        GenericDayAssignmentsContainer result = GenericDayAssignmentsContainer
                .create(this, scenario);
        genericDayAssignmentsContainers.add(result);
        return result;
    }

    private DayAssignmentsState buildFromDBState() {
        return new GenericDayAssignmentsNoExplicitlySpecifiedScenario();
    }

    private TransientState buildInitialTransientState() {
        return new TransientState(new HashSet<GenericDayAssignment>());
    }

    private Map<Scenario, GenericDayAssignmentsContainer> containersByScenario() {
        Map<Scenario, GenericDayAssignmentsContainer> result = new HashMap<Scenario, GenericDayAssignmentsContainer>();
        for (GenericDayAssignmentsContainer each : genericDayAssignmentsContainers) {
            assert !result.containsKey(each);
            result.put(each.getScenario(), each);
        }
        return result;
    }

    public List<GenericDayAssignment> getOrderedAssignmentsFor(Resource resource) {
        List<GenericDayAssignment> list = getOrderedAssignmentsFor().get(
                resource);
        if (list == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(list);
    }

    private Map<Resource, List<GenericDayAssignment>> getOrderedAssignmentsFor() {
        if (orderedDayAssignmentsByResource == null) {
            orderedDayAssignmentsByResource = DayAssignment
                    .byResourceAndOrdered(getDayAssignmentsState()
                            .getUnorderedAssignments());
        }
        return orderedDayAssignmentsByResource;
    }

    public Set<Criterion> getCriterions() {
        return Collections.unmodifiableSet(criterions);
    }

    private static Date toDate(LocalDate day) {
        return day.toDateTimeAtStartOfDay().toDate();
    }

    private final class ResourcesSatisfyingCriterionsSelector implements
            IResourceSelector {

        @Override
        public boolean isSelectable(Resource resource, LocalDate day) {
            ICriterion compoundCriterion = CriterionCompounder.buildAnd(
                    criterions).getResult();
            return compoundCriterion.isSatisfiedBy(resource, toDate(day));
        }
    }

    private class GenericAllocation extends AssignmentsAllocation {

        private EffortDistributor hoursDistributor;
        private final List<Resource> resources;

        public GenericAllocation(List<Resource> resources) {
            this.resources = resources;
            hoursDistributor = new EffortDistributor(resources,
                    getAssignedHoursForResource(),
                    new ResourcesSatisfyingCriterionsSelector());
        }

        @Override
        protected List<GenericDayAssignment> distributeForDay(LocalDate day,
                EffortDuration effort) {
            List<GenericDayAssignment> result = new ArrayList<GenericDayAssignment>();
            for (ResourceWithAssignedDuration each : hoursDistributor
                    .distributeForDay(day, effort)) {
                result.add(GenericDayAssignment.create(day, each.duration,
                        each.resource));
            }
            return result;
        }

        @Override
        protected AvailabilityTimeLine getResourcesAvailability() {
            return AvailabilityCalculator.buildSumOfAvailabilitiesFor(
                    getCriterions(), resources);
        }

    }

    private IAssignedHoursForResource assignedHoursCalculatorOverriden = null;

    public void overrideAssignedHoursForResource(
            GenericResourceAllocation allocation) {
        assignedHoursCalculatorOverriden = allocation
                .getAssignedHoursForResource();
    }

    private IAssignedHoursForResource getAssignedHoursForResource() {
        if (assignedHoursCalculatorOverriden != null) {
            return assignedHoursCalculatorOverriden;
        }
        return new AssignedHoursDiscounting(this);
    }

    @Override
    protected ICalendar getCalendarGivenTaskCalendar(ICalendar taskCalendar) {
        return taskCalendar;
    }

    public IAllocatable forResources(Collection<? extends Resource> resources) {
        return new GenericAllocation(new ArrayList<Resource>(resources));
    }

    private class ExplicitlySpecifiedScenarioState extends DayAssignmentsState {
        private final GenericResourceAllocation outerGenericAllocation = GenericResourceAllocation.this;

        private final GenericDayAssignmentsContainer container;

        ExplicitlySpecifiedScenarioState(Scenario scenario) {
            this.container = retrieveOrCreateContainerFor(scenario);
        }

        @Override
        protected void clearFieldsCalculatedFromAssignments() {
            orderedDayAssignmentsByResource = null;
        }

        @Override
        protected Collection<GenericDayAssignment> getUnorderedAssignments() {
            return container.getDayAssignments();
        }

        @Override
        protected void removeAssignments(
                List<? extends DayAssignment> assignments) {
            container.removeAll(assignments);
        }

        @Override
        protected void setParentFor(GenericDayAssignment each) {
            each.setGenericResourceAllocation(outerGenericAllocation);
        }

        @Override
        protected void addAssignments(
                Collection<? extends GenericDayAssignment> assignments) {
            container.addAll(assignments);
        }

        @Override
        protected void resetTo(Collection<GenericDayAssignment> assignments) {
            container.resetTo(assignments);
        }

        @Override
        protected DayAssignmentsState switchTo(Scenario scenario) {
            return new ExplicitlySpecifiedScenarioState(
                    scenario);
        }

        @Override
        TaskDate getEndDateWithinADay() {
            return container.getEndDateWithinADay();
        }

        @Override
        public void setEndDateWithinADay(TaskDate endDateWithinADay) {
            container.setEndDateWithinADay(endDateWithinADay);
        }
    }

    private class TransientState extends DayAssignmentsState {
        private final GenericResourceAllocation outerGenericAllocation = GenericResourceAllocation.this;

        private final Set<GenericDayAssignment> genericDayAssignments;

        private TaskDate endDateWithinADay;

        TransientState(Set<GenericDayAssignment> genericDayAssignments) {
            this.genericDayAssignments = genericDayAssignments;
        }

        @Override
        protected void clearFieldsCalculatedFromAssignments() {
            orderedDayAssignmentsByResource = null;
        }

        @Override
        protected Collection<GenericDayAssignment> getUnorderedAssignments() {
            return genericDayAssignments;
        }

        @Override
        protected void removeAssignments(
                List<? extends DayAssignment> assignments) {
            genericDayAssignments.removeAll(assignments);
        }

        @Override
        protected void setParentFor(GenericDayAssignment each) {
            each.setGenericResourceAllocation(outerGenericAllocation);
        }

        @Override
        protected void addAssignments(
                Collection<? extends GenericDayAssignment> assignments) {
            genericDayAssignments.addAll(assignments);
        }

        @Override
        protected void resetTo(Collection<GenericDayAssignment> assignments) {
            genericDayAssignments.clear();
            genericDayAssignments.addAll(assignments);
        }

        @Override
        protected DayAssignmentsState switchTo(Scenario scenario) {
            ExplicitlySpecifiedScenarioState result = new ExplicitlySpecifiedScenarioState(
                    scenario);
            result.resetTo(genericDayAssignments);
            return result;
        }

        @Override
        TaskDate getEndDateWithinADay() {
            return endDateWithinADay;
        }

        @Override
        public void setEndDateWithinADay(TaskDate endDateWithinADay) {
            this.endDateWithinADay = endDateWithinADay;
        }
    }

    private Set<GenericDayAssignment> getUnorderedForScenario(
            Scenario scenario) {
        GenericDayAssignmentsContainer container = containersByScenario()
                .get(scenario);
        if (container == null) {
            return new HashSet<GenericDayAssignment>();
        }
        return container.getDayAssignments();
    }

    private TaskDate getEndDataWithinADayFor(Scenario scenario) {
        GenericDayAssignmentsContainer container = containersByScenario().get(
                scenario);
        if (container == null) {
            return null;
        }
        return container.getEndDateWithinADay();
    }

    private class GenericDayAssignmentsNoExplicitlySpecifiedScenario extends
            NoExplicitlySpecifiedScenario {

        @Override
        protected Collection<GenericDayAssignment> getUnorderedAssignmentsForScenario(
                Scenario scenario) {
            return getUnorderedForScenario(scenario);
        }

        @Override
        protected DayAssignmentsState switchTo(Scenario scenario) {
            return new ExplicitlySpecifiedScenarioState(scenario);
        }

        @Override
        protected TaskDate getEndDateWithinADay(Scenario scenario) {
            return getEndDataWithinADayFor(scenario);
        }
    }

    @OnCopy(Strategy.IGNORE)
    private DayAssignmentsState assignmentsState;

    @Override
    protected void scenarioChangedTo(Scenario scenario) {
        assignmentsState = getDayAssignmentsState().switchTo(scenario);
    }

    @Override
    protected ResourceAllocation<GenericDayAssignment>.DayAssignmentsState getDayAssignmentsState() {
        return assignmentsState;
    }

    @Override
    protected Class<GenericDayAssignment> getDayAssignmentType() {
        return GenericDayAssignment.class;
    }

    public List<DayAssignment> createAssignmentsAtDay(List<Resource> resources,
            LocalDate day, ResourcesPerDay resourcesPerDay,
            final EffortDuration maxLimit) {
        final EffortDuration durations = min(
                calculateTotalToDistribute(day, resourcesPerDay), maxLimit);
        GenericAllocation genericAllocation = new GenericAllocation(resources);
        return new ArrayList<DayAssignment>(genericAllocation.distributeForDay(
                day, durations));
    }


    @Override
    public List<Resource> getAssociatedResources() {
        return new ArrayList<Resource>(DayAssignment
                .getAllResources(getAssignments()));
    }

    @Override
    public IAllocatable withPreviousAssociatedResources() {
        return forResources(getAssociatedResources());
    }

    @Override
    ResourceAllocation<GenericDayAssignment> createCopy(Scenario scenario) {
        GenericResourceAllocation allocation = create();
        allocation
                .toTransientStateWithInitial(getUnorderedForScenario(scenario));
        allocation.criterions = new HashSet<Criterion>(criterions);
        allocation.assignedHoursCalculatorOverriden = new AssignedHoursDiscounting(
                this);
        return allocation;
    }

    private void toTransientStateWithInitial(
            Set<GenericDayAssignment> initialAssignments) {
        this.assignmentsState = new TransientState(initialAssignments);
    }

    @Override
    public ResourcesPerDayModification asResourcesPerDayModification() {
        return ResourcesPerDayModification.create(this,
                getResourcesPerDay(), getAssociatedResources());
    }

    @Override
    public HoursModification asHoursModification() {
        return HoursModification.create(this, getAssignedHours(),
                getAssociatedResources());
    }

    @Override
    public ResourcesPerDayModification withDesiredResourcesPerDay(
            ResourcesPerDay resourcesPerDay) {
        return ResourcesPerDayModification.create(this, resourcesPerDay,
                getAssociatedResources());
    }

    @Override
    public List<Resource> querySuitableResources(IResourceDAO resourceDAO) {
        return resourceDAO.findSatisfyingCriterionsAtSomePoint(getCriterions());
    }

    public static Map<Criterion, List<GenericResourceAllocation>> byCriterion(
            List<GenericResourceAllocation> generics) {
        Map<Criterion, List<GenericResourceAllocation>> result = new HashMap<Criterion, List<GenericResourceAllocation>>();
        for (GenericResourceAllocation genericResourceAllocation : generics) {
            Set<Criterion> criterions = genericResourceAllocation
                    .getCriterions();
            for (Criterion criterion : criterions) {
                if (!result.containsKey(criterion)) {
                    result.put(criterion,
                            new ArrayList<GenericResourceAllocation>());
                }
                result.get(criterion).add(genericResourceAllocation);
            }
        }
        return result;
    }

    @Override
    public void makeAssignmentsContainersDontPoseAsTransientAnyMore() {
        for (GenericDayAssignmentsContainer each : genericDayAssignmentsContainers) {
            each.dontPoseAsTransientObjectAnymore();
        }
    }

    @Override
    public void copyAssignments(Scenario from, Scenario to) {
        GenericDayAssignmentsContainer fromContainer = retrieveOrCreateContainerFor(from);
        GenericDayAssignmentsContainer toContainer = retrieveOrCreateContainerFor(to);
        toContainer.resetTo(fromContainer.getDayAssignments());
    }

    @Override
    protected void removePredecessorContainersFor(Scenario scenario) {
        Map<Scenario, GenericDayAssignmentsContainer> byScenario = containersByScenario();
        for (Scenario each : scenario.getPredecessors()) {
            GenericDayAssignmentsContainer container = byScenario.get(each);
            if (container != null) {
                genericDayAssignmentsContainers.remove(container);
            }
        }
    }

    @Override
    protected void removeContainersFor(Scenario scenario) {
        GenericDayAssignmentsContainer container = containersByScenario().get(
                scenario);
        if (container != null) {
            genericDayAssignmentsContainers.remove(container);
        }
    }

    public void overrideConsolidatedDayAssignments(
            GenericResourceAllocation origin) {
        if (origin != null) {
            List<GenericDayAssignment> originAssignments = origin
                    .getConsolidatedAssignments();
            resetAssignmentsTo(GenericDayAssignment
                    .copyToAssignmentsWithoutParent(originAssignments));
        }
    }

}
