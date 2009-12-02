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

package org.navalplanner.business.costcategories.entities;

import java.math.BigDecimal;
import java.util.Date;

import org.hibernate.validator.NotNull;
import org.navalplanner.business.common.BaseEntity;

/**
 * @author Jacobo Aragunde Perez <jaragunde@igalia.com>
 */
public class HourCost extends BaseEntity {

	@NotNull
    private BigDecimal priceCost;

	@NotNull
    private Date initDate;

    private Date endDate;

    // Default constructor, needed by Hibernate
    protected HourCost() {

    }

    public static HourCost create(BigDecimal priceCost, Date initDate) {
        return (HourCost) create(new HourCost(priceCost, initDate));
    }

    protected HourCost(BigDecimal priceCost, Date initDate) {
        this.priceCost = priceCost;
        this.initDate = initDate;
    }

    public BigDecimal getPriceCost() {
        return priceCost;
    }

    public void setPriceCost(BigDecimal priceCost) {
        this.priceCost = priceCost;
    }

    public Date getInitDate() {
        return initDate;
    }

    public void setInitDate(Date initDate) {
        this.initDate = initDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }
}
