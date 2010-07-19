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

package org.navalplanner.ws.subcontract.api;

import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import org.navalplanner.business.planner.entities.SubcontractedTaskData;
import org.navalplanner.ws.common.api.OrderDTO;
import org.navalplanner.ws.common.api.OrderElementDTO;
import org.navalplanner.ws.common.api.OrderLineDTO;
import org.navalplanner.ws.common.api.OrderLineGroupDTO;

/**
 * DTO for {@link SubcontractedTaskData} entity.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
@XmlRootElement(name = "subcontracted-task-data")
public class SubcontractedTaskDataDTO {

    @XmlAttribute(name = "external-company-nif")
    public String externalCompanyNif;

    @XmlAttribute(name = "work-description")
    public String workDescription;

    @XmlAttribute(name = "subcontracted-price")
    public BigDecimal subcontractPrice;

    @XmlAttribute(name = "subcontracted-code")
    public String subcontractedCode;

    @XmlElements( {
            @XmlElement(name = "order-line", type = OrderLineDTO.class),
            @XmlElement(name = "order-line-group", type = OrderLineGroupDTO.class),
            @XmlElement(name = "order", type = OrderDTO.class) })
    public OrderElementDTO orderElementDTO;

    public SubcontractedTaskDataDTO() {
    }

    public SubcontractedTaskDataDTO(String externalCompanyNif,
            String workDescription, BigDecimal subcontractPrice,
            String subcontractedCode, OrderElementDTO orderElementDTO) {
        this.externalCompanyNif = externalCompanyNif;
        this.workDescription = workDescription;
        this.subcontractPrice = subcontractPrice;
        this.subcontractedCode = subcontractedCode;
        this.orderElementDTO = orderElementDTO;
    }

}
