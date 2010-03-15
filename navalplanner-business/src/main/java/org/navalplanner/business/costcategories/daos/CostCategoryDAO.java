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

package org.navalplanner.business.costcategories.daos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.joda.time.LocalDate;
import org.navalplanner.business.common.daos.IntegrationEntityDAO;
import org.navalplanner.business.common.exceptions.InstanceNotFoundException;
import org.navalplanner.business.costcategories.entities.CostCategory;
import org.navalplanner.business.costcategories.entities.HourCost;
import org.navalplanner.business.costcategories.entities.ResourcesCostCategoryAssignment;
import org.navalplanner.business.resources.entities.Resource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Jacobo Aragunde Perez <jaragunde@igalia.com>
 * @author Fernando Bellas Permuy <fbellas@udc.es>
 */
@Repository
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class CostCategoryDAO extends IntegrationEntityDAO<CostCategory>
        implements ICostCategoryDAO {

    @Override
    public List<CostCategory> findActive() {

        Criteria c = getSession().createCriteria(CostCategory.class);
        c.add(Restrictions.eq("enabled", true));

        List<CostCategory> list = new ArrayList<CostCategory>();
        list.addAll(c.list());
        return list;
    }

    @Override
    public CostCategory findUniqueByName(String name) 
    throws InstanceNotFoundException {
        Criteria c = getSession().createCriteria(CostCategory.class).
        add(Restrictions.eq("name", name).ignoreCase());
        CostCategory costCategory = (CostCategory) c.uniqueResult();

        if (costCategory == null) {
            throw new InstanceNotFoundException(name,
                    CostCategory.class.getName());
        } else {
            return costCategory;
        }
    }

    @Override
    public CostCategory findUniqueByCode(String code)
            throws InstanceNotFoundException {
        if (code == null) {
            throw new InstanceNotFoundException(code, CostCategory.class
                    .getName());
        }

        Criteria c = getSession().createCriteria(CostCategory.class).add(
                Restrictions.eq("code", code).ignoreCase());
        CostCategory costCategory = (CostCategory) c.uniqueResult();

        if (costCategory == null) {
            throw new InstanceNotFoundException(code, CostCategory.class
                    .getName());
        } else {
            return costCategory;
        }

    }

    @Transactional(readOnly = true)
    public static BigDecimal getPriceByResourceDateAndHourType(
            Resource resource,
            LocalDate date, String type) {

        for (ResourcesCostCategoryAssignment each : resource
                .getResourcesCostCategoryAssignments()) {
            if ((date.isAfter(each.getInitDate()))
                    && (!date.isBefore(each.getInitDate()))) {
                for (HourCost hourCost : each.getCostCategory().getHourCosts()) {
                    if (hourCost.isActiveAtDate(date)
                            && hourCost.getType().getCode().equals(type)) {
                        return hourCost.getPriceCost();
                    }
                }
            }
        }
        return null;
    }

}
