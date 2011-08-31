/*
 * This file is part of NavalPlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                         Desenvolvemento Tecnolóxico de Galicia
 * Copyright (C) 2010-2011 Igalia, S.L.
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

package org.navalplanner.business.workreports.entities;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.hibernate.validator.AssertTrue;
import org.hibernate.validator.NotNull;
import org.hibernate.validator.Valid;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.Seconds;
import org.navalplanner.business.common.IntegrationEntity;
import org.navalplanner.business.common.Registry;
import org.navalplanner.business.common.exceptions.InstanceNotFoundException;
import org.navalplanner.business.costcategories.entities.TypeOfWorkHours;
import org.navalplanner.business.labels.entities.Label;
import org.navalplanner.business.labels.entities.LabelType;
import org.navalplanner.business.orders.entities.OrderElement;
import org.navalplanner.business.resources.entities.Resource;
import org.navalplanner.business.workingday.EffortDuration;
import org.navalplanner.business.workreports.daos.IWorkReportLineDAO;
import org.navalplanner.business.workreports.valueobjects.DescriptionField;
import org.navalplanner.business.workreports.valueobjects.DescriptionValue;

/**
 * Note: this class has a natural ordering that is inconsistent with equals.
 *
 * @author Diego Pino García <dpino@igalia.com>
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */
public class WorkReportLine extends IntegrationEntity implements Comparable {

    public static WorkReportLine create(WorkReport workReport) {
        return create(new WorkReportLine(workReport));
    }

    private EffortDuration effort;

    private Date date;

    private LocalTime clockStart;

    private LocalTime clockFinish;

    private Resource resource;

    private OrderElement orderElement;

    private Set<Label> labels = new HashSet<Label>();

    private Set<DescriptionValue> descriptionValues = new HashSet<DescriptionValue>();

    private WorkReport workReport;

    private TypeOfWorkHours typeOfWorkHours;

    /**
     * Constructor for hibernate. Do not use!
     */
    public WorkReportLine() {
    }

    public WorkReportLine(WorkReport workReport) {
        this.setWorkReport(workReport);
    }

    @NotNull(message = "effort not specified")
    public EffortDuration getEffort() {
        return effort;
    }

    public void setEffort(EffortDuration effort) {
        this.effort = effort;
        if ((workReport != null)
                && (workReport.getWorkReportType() != null)
                && (workReport.getWorkReportType().getHoursManagement()
                        .equals(HoursManagementEnum.HOURS_CALCULATED_BY_CLOCK))) {
            this.effort = getDiferenceBetweenTimeStartAndFinish();
        }
    }

    public LocalTime getClockFinish() {
        return clockFinish;
    }

    public void setClockFinish(Date clockFinish) {
        if (clockFinish != null) {
            setClockFinish(LocalTime.fromDateFields(clockFinish));
        }
    }

    public void setClockFinish(LocalTime clockFinish) {
        this.clockFinish = clockFinish;
        updateEffort();
    }

    public LocalTime getClockStart() {
        return clockStart;
    }

    public void setClockStart(Date clockStart) {
        if (clockStart != null) {
            setClockStart(LocalTime.fromDateFields(clockStart));
        }
    }

    public void setClockStart(LocalTime clockStart) {
        this.clockStart = clockStart;
        updateEffort();
    }

    @NotNull(message = "date not specified")
    public Date getDate() {
        return date;
    }

    public LocalDate getLocalDate() {
        if (getDate() == null) {
            return null;
        }
        return LocalDate.fromDateFields(getDate());
    }

    public void setDate(Date date) {
        this.date = date;
        if ((workReport != null) && (workReport.getWorkReportType() != null)) {
            if (workReport.getWorkReportType().getDateIsSharedByLines()) {
                this.date = workReport.getDate();
            }
        }
    }

    @NotNull(message = "resource not specified")
    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
        if ((workReport != null) && (workReport.getWorkReportType() != null)) {
            if (workReport.getWorkReportType().getResourceIsSharedInLines()) {
                this.resource = workReport.getResource();
            }
        }
    }

    @NotNull(message = "order element not specified")
    public OrderElement getOrderElement() {
        return orderElement;
    }

    public void setOrderElement(OrderElement orderElement) {
        this.orderElement = orderElement;
        if ((workReport != null) && (workReport.getWorkReportType() != null)) {
            if (workReport.getWorkReportType().getOrderElementIsSharedInLines()) {
                this.orderElement = workReport.getOrderElement();
            }
        }
    }

    public Set<Label> getLabels() {
        return labels;
    }

    public void setLabels(Set<Label> labels) {
        this.labels = labels;
    }

    @NotNull(message = "work report not specified")
    public WorkReport getWorkReport() {
        return workReport;
    }

    private void setWorkReport(WorkReport workReport) {
        this.workReport = workReport;

        // update and copy the fields and label for each line
        updateItsFieldsAndLabels();

        // copy the required fields if these are shared by lines
        updatesAllSharedDataByLines();

        // update calculated effort
        updateEffort();
    }

    @Valid
    public Set<DescriptionValue> getDescriptionValues() {
        return descriptionValues;
    }

    public void setDescriptionValues(Set<DescriptionValue> descriptionValues) {
        this.descriptionValues = descriptionValues;
    }

    @NotNull(message = "type of work hours not specified")
    public TypeOfWorkHours getTypeOfWorkHours() {
        return typeOfWorkHours;
    }

    public void setTypeOfWorkHours(TypeOfWorkHours typeOfWorkHours) {
        this.typeOfWorkHours = typeOfWorkHours;
    }

    @Override
    public int compareTo(Object arg0) {
        if (date != null) {
            final WorkReportLine workReportLine = (WorkReportLine) arg0;
            return date.compareTo(workReportLine.getDate());
        }
        return -1;
    }

    @AssertTrue(message = "closckStart:the clockStart must be not null if number of hours is calcultate by clock")
    public boolean checkConstraintClockStartMustBeNotNullIfIsCalculatedByClock() {
        if (!firstLevelValidationsPassed()) {
            return true;
        }

        if (workReport.getWorkReportType().getHoursManagement().equals(
                HoursManagementEnum.HOURS_CALCULATED_BY_CLOCK)) {
            return (getClockStart() != null);
        }
        return true;
    }

    @AssertTrue(message = "clockFinish:the clockStart must be not null if number of hours is calcultate by clock")
    public boolean checkConstraintClockFinishMustBeNotNullIfIsCalculatedByClock() {
        if (!firstLevelValidationsPassed()) {
            return true;
        }

        if (workReport.getWorkReportType().getHoursManagement().equals(
                HoursManagementEnum.HOURS_CALCULATED_BY_CLOCK)) {
            return (getClockFinish() != null);
        }
        return true;
    }

    @AssertTrue(message = "The start hour cannot be higher than finish hour")
    public boolean checkCannotBeHigher() {
        if (!firstLevelValidationsPassed()) {
            return true;
        }

        if (workReport.getWorkReportType().getHoursManagement().equals(
                HoursManagementEnum.HOURS_CALCULATED_BY_CLOCK)) {
            return checkCannotBeHigher(this.clockStart, this.clockFinish);
        }
        return true;
    }

    public boolean checkCannotBeHigher(LocalTime starting, LocalTime ending) {
        return !((ending != null) && (starting != null) && (starting
                .compareTo(ending) > 0));
    }

    void updateItsFieldsAndLabels() {
        if (workReport != null) {
            assignItsLabels(workReport.getWorkReportType());
            assignItsDescriptionValues(workReport.getWorkReportType());
        }
    }

    private void assignItsLabels(WorkReportType workReportType) {
        Set<Label> updatedLabels = new HashSet<Label>();
        if (workReportType != null) {
            for (WorkReportLabelTypeAssigment labelTypeAssigment : workReportType
                    .getLineLabels()) {
                Label label = getLabelBy(labelTypeAssigment);
                if (label != null) {
                    updatedLabels.add(label);
                } else {
                    updatedLabels.add(labelTypeAssigment.getDefaultLabel());
                }
            }
            this.labels = updatedLabels;
        }
    }

    private Label getLabelBy(WorkReportLabelTypeAssigment labelTypeAssigment) {
        LabelType type = labelTypeAssigment.getLabelType();
        for (Label label : labels) {
            if (label.getType().getId().equals(type.getId())) {
                return label;
            }
        }
        return null;
    }

    private void assignItsDescriptionValues(WorkReportType workReportType) {
        Set<DescriptionValue> updatedDescriptionValues = new HashSet<DescriptionValue>();
        if (workReportType != null) {
            for (DescriptionField descriptionField : workReportType
                    .getLineFields()) {
                DescriptionValue descriptionValue;
                try {
                    descriptionValue = this
                            .getDescriptionValueByFieldName(descriptionField
                                    .getFieldName());
                } catch (InstanceNotFoundException e) {
                    descriptionValue = DescriptionValue.create(
                        descriptionField.getFieldName(), null);
                }
                updatedDescriptionValues.add(descriptionValue);
            }
            this.descriptionValues = updatedDescriptionValues;
        }
    }

    void updatesAllSharedDataByLines() {
        // copy the required fields if these are shared by lines
        updateSharedDateByLines();
        updateSharedResourceByLines();
        updateSharedOrderElementByLines();
    }

    void updateSharedDateByLines() {
        if ((workReport != null) && (workReport.getWorkReportType() != null)
                && (workReport.getWorkReportType().getDateIsSharedByLines())) {
            setDate(workReport.getDate());
        }
    }

    void updateSharedResourceByLines() {
        if ((workReport != null)
                && (workReport.getWorkReportType() != null)
                && (workReport.getWorkReportType().getResourceIsSharedInLines())) {
            setResource(workReport.getResource());
        }
    }

    void updateSharedOrderElementByLines() {
        if ((workReport != null)
                && (workReport.getWorkReportType() != null)
                && (workReport.getWorkReportType()
                        .getOrderElementIsSharedInLines())) {
            setOrderElement(workReport.getOrderElement());
        }
    }

    private void updateEffort() {
        if ((workReport != null)
                && (workReport.getWorkReportType() != null)
                && workReport.getWorkReportType().getHoursManagement().equals(
                        HoursManagementEnum.HOURS_CALCULATED_BY_CLOCK)) {
            setEffort(getDiferenceBetweenTimeStartAndFinish());
        }
    }

    private EffortDuration getDiferenceBetweenTimeStartAndFinish() {
        if ((clockStart != null) && (clockFinish != null)) {
            return EffortDuration.seconds(Seconds.secondsBetween(clockStart,
                    clockFinish).getSeconds());
        }
        return null;
    }

    @Override
    protected IWorkReportLineDAO getIntegrationEntityDAO() {
        return Registry.getWorkReportLineDAO();
    }

    @AssertTrue(message = "fields should match with work report data if are shared by lines")
    public boolean checkConstraintFieldsMatchWithWorkReportIfAreSharedByLines() {
        if (!firstLevelValidationsPassed()) {
            return true;
        }

        if (workReport.getWorkReportType().getDateIsSharedByLines()) {
            if (!workReport.getDate().equals(date)) {
                return false;
            }
        }
        if (workReport.getWorkReportType().getOrderElementIsSharedInLines()) {
            if (!workReport.getOrderElement().getId().equals(
                    orderElement.getId())) {
                return false;
            }
        }
        if (workReport.getWorkReportType().getResourceIsSharedInLines()) {
            if (!workReport.getResource().getId().equals(resource.getId())) {
                return false;
            }
        }
        return true;
    }

    @AssertTrue(message = "number of hours is not properly calculated based on clock")
    public boolean checkConstraintHoursCalculatedByClock() {
        if (!firstLevelValidationsPassed()) {
            return true;
        }

        if (workReport.getWorkReportType().getHoursManagement().equals(
                HoursManagementEnum.HOURS_CALCULATED_BY_CLOCK)) {
            if (getDiferenceBetweenTimeStartAndFinish().compareTo(effort) != 0) {
                return false;
            }
        }
        return true;
    }

    private boolean firstLevelValidationsPassed() {
        return (workReport != null) && (typeOfWorkHours != null)
                && (effort != null) && (date != null) && (resource != null)
                && (orderElement != null);
    }

    @AssertTrue(message = "label type:the work report have not assigned this label type")
    public boolean checkConstraintAssignedLabelTypes() {
        if (this.workReport == null
                || this.workReport.getWorkReportType() == null) {
            return true;
        }

        if (this.workReport.getWorkReportType().getLineLabels().size() != this.labels
                .size()) {
            return false;
        }

        for (WorkReportLabelTypeAssigment typeAssigment : this.workReport
                .getWorkReportType().getLineLabels()) {
            try {
                getLabelByType(typeAssigment.getLabelType());
            } catch (InstanceNotFoundException e) {
                return false;
            }
        }
        return true;
    }

    @AssertTrue(message = "description value:the work report have not assigned the description field")
    public boolean checkConstraintAssignedDescriptionValues() {
        if (this.workReport == null
                || this.workReport.getWorkReportType() == null) {
            return true;
        }

        if (this.workReport.getWorkReportType().getLineFields().size() > this.descriptionValues
                .size()) {
            return false;
        }

        for (DescriptionField field : this.workReport.getWorkReportType()
                .getLineFields()) {
            try {
                getDescriptionValueByFieldName(field.getFieldName());
            } catch (InstanceNotFoundException e) {
                return false;
            }
        }
        return true;
    }

    @AssertTrue(message = "There are repeated description values in the work report line")
    public boolean checkConstraintAssignedRepeatedDescriptionValues() {

        Set<String> textFields = new HashSet<String>();

        for (DescriptionValue v : this.descriptionValues) {

            String name = v.getFieldName();

            if (!StringUtils.isBlank(name)) {
                if (textFields.contains(name.toLowerCase())) {
                    return false;
                } else {
                    textFields.add(name.toLowerCase());
                }
            }
        }
        return true;
    }

    public DescriptionValue getDescriptionValueByFieldName(String fieldName)
            throws InstanceNotFoundException {

        if (StringUtils.isBlank(fieldName)) {
            throw new InstanceNotFoundException(fieldName,
                    DescriptionValue.class.getName());
        }

        for (DescriptionValue v : this.descriptionValues) {
            if (v.getFieldName().equalsIgnoreCase(StringUtils.trim(fieldName))) {
                return v;
            }
        }

        throw new InstanceNotFoundException(fieldName, DescriptionValue.class
                .getName());
    }

    public Label getLabelByType(LabelType type)
            throws InstanceNotFoundException {

        if (type == null) {
            throw new InstanceNotFoundException(type, LabelType.class.getName());
        }

        for (Label l : this.labels) {
            if (l.getType().getId().equals(type.getId())) {
                return l;
            }
        }

        throw new InstanceNotFoundException(type, LabelType.class.getName());
    }

    @Override
    public void setCodeAutogenerated(Boolean codeAutogenerated) {
        // do nothing
    }

    @Override
    public Boolean isCodeAutogenerated() {
        return getWorkReport() != null ? getWorkReport().isCodeAutogenerated()
                : false;
    }

    /**
     * TODO remove this method in order to use
     * {@link WorkReportLine#getEffort()}
     * 
     * @deprecated Use {@link WorkReportLine#getEffort()} instead
     */
    public Integer getNumHours() {
        return (getEffort() == null) ? null : getEffort().getHours();
    }

    /**
     * TODO remove this method in order to use
     * {@link WorkReportLine#setEffort()}
     * 
     * @deprecated Use {@link WorkReportLine#setEffort()} instead
     */
    public void setNumHours(Integer hours) {
        setEffort(EffortDuration.hours(hours));
    }

}
