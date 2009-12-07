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

package org.navalplanner.web.workreports;

import java.util.List;
import java.util.Map;

import org.navalplanner.business.common.exceptions.InstanceNotFoundException;
import org.navalplanner.business.common.exceptions.ValidationException;
import org.navalplanner.business.labels.entities.Label;
import org.navalplanner.business.labels.entities.LabelType;
import org.navalplanner.business.orders.entities.OrderElement;
import org.navalplanner.business.resources.entities.Resource;
import org.navalplanner.business.resources.entities.Worker;
import org.navalplanner.business.workreports.entities.WorkReport;
import org.navalplanner.business.workreports.entities.WorkReportLine;
import org.navalplanner.business.workreports.entities.WorkReportType;
import org.navalplanner.business.workreports.valueobjects.DescriptionField;
import org.navalplanner.business.workreports.valueobjects.DescriptionValue;

/**
 * Contract for {@link WorkRerportType}
 *
 * @author Diego Pino García <dpino@igalia.com>
 */
public interface IWorkReportModel {

    /**
     * Add new {@link WorkReportLine} to {@link WorkReport}
     *
     * @return
     */
    WorkReportLine addWorkReportLine();

    /**
     * Converts @{link Resource} to @{link Worker}
     *
     * @param resource
     * @return
     * @throws InstanceNotFoundException
     */
    Worker asWorker(Resource resource) throws InstanceNotFoundException;

    /**
     * Finds an @{link OrdrElement} by code
     *
     * @param code
     * @return
     */
    OrderElement findOrderElement(String code) throws InstanceNotFoundException;

    /**
     * Find a @{link Worker} by nif
     *
     * @param nif
     * @return
     * @throws InstanceNotFoundException
     */
    Worker findWorker(String nif) throws InstanceNotFoundException;

    /**
     * Returns distinguished code for {@link OrderElement}
     *
     * @param orderElement
     * @return
     */
    String getDistinguishedCode(OrderElement orderElement)
            throws InstanceNotFoundException;

    /**
     * Gets the current {@link WorkReport}.
     *
     * @return A {@link WorkReport}
     */
    WorkReport getWorkReport();

    /**
     * Return all {@link WorkReportLine} associated with current {@link WorkReport}
     *
     * @return
     */
    List<WorkReportLine> getWorkReportLines();

    /**
     * Get all {@link WorkReport} elements
     *
     * @return
     */
    List<WorkReport> getWorkReports();

    /**
     * Returns true if WorkReport is being edited
     *
     * @return
     */
    boolean isEditing();

    /**
     * Makes some operations needed before edit a {@link WorkReport}.
     *
     * @param workReport
     *            The object to be edited
     */
    void initEdit(WorkReport workReport);

    /**
     * Makes some operations needed before create a new {@link WorkReport}.
     */
    void initCreate(WorkReportType workReportType);

    /**
     * Removes {@link WorkReport}
     *
     * @param workReport
     */
    void remove(WorkReport workReport);

    /**
     * Removes {@link WorkReportLine}
     *
     * @param workReportLine
     */
    void removeWorkReportLine(WorkReportLine workReportLine);

    /**
     * Stores the current {@link WorkReport}.
     *
     * @throws ValidationException
     *             If validation fails
     */
    void confirmSave() throws ValidationException;

    /**
     * Return all {@link DescriptionValue} and {@link Label} associated with
     * current {@link WorkReport}
     * @return
     */
    List<Object> getFieldsAndLabelsHeading();

    /**
     * Return all {@link DescriptionField} and {@link Label} associated with the
     * current {@link WorkReportType}
     * @return
     */
    List<Object> getFieldsAndLabelsLineByDefault();

    /**
     * Return all {@link DescriptionValue} and {@link Label} associated with
     * current {@link WorkReportLine}
     * @return
     */
    List<Object> getFieldsAndLabelsLine(WorkReportLine workReportLine);

    /**
     * Return all assigned {@link LabelType} and its {@link Label}
     * @return
     */
    Map<LabelType, List<Label>> getMapAssignedLabelTypes();

    /**
     * Change the default or the old {@link Label} to other new {@link Label} in
     * the current {@link WorkReport}
     * @return
     */
    void changeLabelInWorkReport(Label oldLabel, Label newLabel);

    /**
     * Change the default or the old {@link Label} to other new {@link Label} in
     * the current {@link WorkReportLine}
     * @return
     */
    void changeLabelInWorkReportLine(Label oldLabel, Label newLabel,
            WorkReportLine line);

    /**
     * Return the length description field associated with the description
     * value.
     * @return
     */
    Integer getLength(DescriptionValue descriptionValue);

}
