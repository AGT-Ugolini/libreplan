/*
 * This file is part of ###PROJECT_NAME###
 *
 * Copyright (C) 2009 Fundación para o Fomento da Calidade Industrial e
 *                    Desenvolvemento Tecnolóxico de Galicia
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

import java.math.BigDecimal;
import java.util.Date;

import org.hibernate.validator.AssertTrue;
import org.hibernate.validator.NotNull;
import org.navalplanner.business.common.BaseEntity;
import org.navalplanner.business.externalcompanies.entities.ExternalCompany;

/**
 * Gathers all the information related with a subcontracted {@link Task}.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
public class SubcontractedTaskData extends BaseEntity {

    public static SubcontractedTaskData create(Task task) {
        SubcontractedTaskData subcontractedTaskData = new SubcontractedTaskData(task);
        subcontractedTaskData.subcontratationDate = new Date();
        return create(subcontractedTaskData);
    }

    public static SubcontractedTaskData createFrom(
            SubcontractedTaskData subcontractedTaskData) {
        if (subcontractedTaskData == null) {
            return null;
        }

        SubcontractedTaskData result = new SubcontractedTaskData();
        result.task = subcontractedTaskData.getTask();
        result.externalCompany = subcontractedTaskData.externalCompany;
        result.subcontratationDate = subcontractedTaskData.subcontratationDate;
        result.subcontractCommunicationDate = subcontractedTaskData.subcontractCommunicationDate;
        result.workDescription = subcontractedTaskData.workDescription;
        result.subcontractPrice = subcontractedTaskData.subcontractPrice;
        result.subcontractedCode = subcontractedTaskData.subcontractedCode;
        result.nodeWithoutChildrenExported = subcontractedTaskData.nodeWithoutChildrenExported;
        result.labelsExported = subcontractedTaskData.labelsExported;
        result.materialAssignmentsExported = subcontractedTaskData.materialAssignmentsExported;
        result.hoursGroupsExported = subcontractedTaskData.hoursGroupsExported;

        return create(result);
    }

    private Task task;

    private ExternalCompany externalCompany;

    private Date subcontratationDate;

    private Date subcontractCommunicationDate;

    private String workDescription;

    private BigDecimal subcontractPrice;

    private String subcontractedCode;

    private Boolean nodeWithoutChildrenExported;
    private Boolean labelsExported;
    private Boolean materialAssignmentsExported;
    private Boolean hoursGroupsExported;

    private SubcontractState state = SubcontractState.PENDING;

    /**
     * Constructor for hibernate. Do not use!
     */
    public SubcontractedTaskData() {
    }

    private SubcontractedTaskData(Task task) {
        this.task = task;
    }

    @NotNull(message = "task not specified")
    public Task getTask() {
        return task;
    }

    @NotNull(message = "external company not specified")
    public ExternalCompany getExternalCompany() {
        return externalCompany;
    }

    public void setExternalCompany(ExternalCompany externalCompany) {
        this.externalCompany = externalCompany;
    }

    public Date getSubcontractCommunicationDate() {
        return subcontractCommunicationDate;
    }

    public void setSubcontractCommunicationDate(
            Date subcontractCommunicationDate) {
        this.subcontractCommunicationDate = subcontractCommunicationDate;
    }

    public String getWorkDescription() {
        return workDescription;
    }

    public void setWorkDescription(String workDescription) {
        this.workDescription = workDescription;
    }

    public BigDecimal getSubcontractPrice() {
        return subcontractPrice;
    }

    public void setSubcontractPrice(BigDecimal subcontractPrice) {
        this.subcontractPrice = subcontractPrice;
    }

    public String getSubcontractedCode() {
        return subcontractedCode;
    }

    public void setSubcontractedCode(String subcontractedCode) {
        this.subcontractedCode = subcontractedCode;
    }

    public boolean isNodeWithoutChildrenExported() {
        if (nodeWithoutChildrenExported == null) {
            return false;
        }
        return nodeWithoutChildrenExported;
    }

    public void setNodeWithoutChildrenExported(
            Boolean nodeWithoutChildrenExported) {
        if (nodeWithoutChildrenExported == null) {
            nodeWithoutChildrenExported = false;
        }
        this.nodeWithoutChildrenExported = nodeWithoutChildrenExported;
    }

    public boolean isLabelsExported() {
        if (labelsExported == null) {
            return false;
        }
        return labelsExported;
    }

    public void setLabelsExported(Boolean labelsExported) {
        if (labelsExported == null) {
            labelsExported = false;
        }
        this.labelsExported = labelsExported;
    }

    public Boolean isMaterialAssignmentsExported() {
        if (materialAssignmentsExported == null) {
            return false;
        }
        return materialAssignmentsExported;
    }

    public void setMaterialAssignmentsExported(
            Boolean materialAssignmentsExported) {
        if (materialAssignmentsExported == null) {
            materialAssignmentsExported = false;
        }
        this.materialAssignmentsExported = materialAssignmentsExported;
    }

    public Boolean isHoursGroupsExported() {
        if (hoursGroupsExported == null) {
            return false;
        }
        return hoursGroupsExported;
    }

    public void setHoursGroupsExported(Boolean hoursGroupsExported) {
        if (hoursGroupsExported == null) {
            hoursGroupsExported = false;
        }
        this.hoursGroupsExported = hoursGroupsExported;
    }

    @NotNull(message = "subcontratation date not specified")
    public Date getSubcontratationDate() {
        return subcontratationDate;
    }

    public void applyChanges(SubcontractedTaskData subcontratedTask) {
        this.externalCompany = subcontratedTask.externalCompany;
        this.subcontratationDate = subcontratedTask.subcontratationDate;
        this.subcontractCommunicationDate = subcontratedTask.subcontractCommunicationDate;
        this.workDescription = subcontratedTask.workDescription;
        this.subcontractPrice = subcontratedTask.subcontractPrice;
        this.subcontractedCode = subcontratedTask.subcontractedCode;
        this.nodeWithoutChildrenExported = subcontratedTask.nodeWithoutChildrenExported;
        this.labelsExported = subcontratedTask.labelsExported;
        this.materialAssignmentsExported = subcontratedTask.materialAssignmentsExported;
        this.hoursGroupsExported = subcontratedTask.hoursGroupsExported;
    }

    @AssertTrue(message = "external company should be subcontractor")
    public boolean checkConstraintExternalCompanyIsSubcontractor() {
        if (!firstLevelValidationsPassed()) {
            return true;
        }

        return externalCompany.isSubcontractor();
    }

    private boolean firstLevelValidationsPassed() {
        return (externalCompany != null) && (subcontratationDate != null);
    }

    public void setState(SubcontractState state) {
        this.state = state;
    }

    @NotNull(message = "state not specified")
    public SubcontractState getState() {
        return state;
    }

}