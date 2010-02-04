/*
 * This file is part of NavalPlan
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

package org.navalplanner.ws.resources.api;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * DTO for <code>CriterionSatisfaction</code> entity.
 *
 * @author Fernando Bellas Permuy <fbellas@udc.es>
 */
public class CriterionSatisfactionDTO {

    @XmlAttribute(name="criterion-type-name")
    public String criterionTypeName;

    @XmlAttribute(name="criterion-name")
    public String criterionName;

    @XmlAttribute(name="start-date")
    @XmlSchemaType(name="date")
    public XMLGregorianCalendar startDate;

    @XmlAttribute(name="end-date")
    @XmlSchemaType(name="date")
    public XMLGregorianCalendar endDate;

    public CriterionSatisfactionDTO() {}

    public CriterionSatisfactionDTO(
        String criterionTypeName, String criterionName,
        XMLGregorianCalendar startDate, XMLGregorianCalendar endDate) {

        this.criterionTypeName = criterionTypeName;
        this.criterionName = criterionName;
        this.startDate = startDate;
        this.endDate = endDate;

    }

}
