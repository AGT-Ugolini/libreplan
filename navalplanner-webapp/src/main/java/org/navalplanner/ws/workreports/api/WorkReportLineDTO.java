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

package org.navalplanner.ws.workreports.api;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.datatype.XMLGregorianCalendar;

import org.navalplanner.business.workreports.entities.WorkReportLine;
import org.navalplanner.ws.common.api.IntegrationEntityDTO;
import org.navalplanner.ws.common.api.LabelReferenceDTO;

/**
 * DTO for {@link WorkReportLine} entity.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
@XmlRootElement(name = "work-report-line")
public class WorkReportLineDTO extends IntegrationEntityDTO {

    public final static String ENTITY_TYPE = "work-report-line";

    @XmlAttribute
    public XMLGregorianCalendar date;

    @XmlAttribute
    public String resource;

    @XmlAttribute(name = "work-order")
    public String orderElement;

    @XmlAttribute(name = "hour-type")
    public String typeOfWorkHours;

    @XmlAttribute(name = "start-hour")
    public XMLGregorianCalendar clockStart;

    @XmlAttribute(name = "finish-hour")
    public XMLGregorianCalendar clockFinish;

    @XmlAttribute(name = "hours")
    public Integer numHours;

    @XmlElementWrapper(name = "label-list")
    @XmlElement(name = "label")
    public Set<LabelReferenceDTO> labels = new HashSet<LabelReferenceDTO>();

    @XmlElementWrapper(name = "text-field-list")
    @XmlElement(name = "text-field")
    public Set<DescriptionValueDTO> descriptionValues = new HashSet<DescriptionValueDTO>();

    public WorkReportLineDTO() {
    }

    public WorkReportLineDTO(String code, XMLGregorianCalendar date,
            String resource,
 String orderElement, String typeOfWorkHours,
            XMLGregorianCalendar clockStart, XMLGregorianCalendar clockFinish,
            Integer numHours, Set<LabelReferenceDTO> labels,
            Set<DescriptionValueDTO> descriptionValues) {
        super(code);
        this.date = date;
        this.resource = resource;
        this.orderElement = orderElement;
        this.typeOfWorkHours = typeOfWorkHours;
        this.clockStart = clockStart;
        this.clockFinish = clockFinish;
        this.numHours = numHours;
        this.labels = labels;
        this.descriptionValues = descriptionValues;
    }

    @Override
    public String getEntityType() {
        return ENTITY_TYPE;
    }

}
