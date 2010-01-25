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

package org.navalplanner.ws.subcontract.api;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.navalplanner.business.orders.entities.OrderElement;

/**
 * DTO for {@link OrderElement} just with information about advances.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
@XmlRootElement(name = "order-element-list")
public class OrderElementWithAdvanceMeasurementsListDTO {

    @XmlElement(name = "order-element")
    public List<OrderElementWithAdvanceMeasurementsDTO> orderElements = new ArrayList<OrderElementWithAdvanceMeasurementsDTO>();

    public OrderElementWithAdvanceMeasurementsListDTO() {
    }

    public OrderElementWithAdvanceMeasurementsListDTO(
            List<OrderElementWithAdvanceMeasurementsDTO> orderElements) {
        this.orderElements = orderElements;
    }

}
