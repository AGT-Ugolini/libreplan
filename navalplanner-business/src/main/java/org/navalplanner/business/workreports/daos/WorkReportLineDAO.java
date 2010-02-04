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

package org.navalplanner.business.workreports.daos;

import java.util.Collection;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.navalplanner.business.common.daos.IntegrationEntityDAO;
import org.navalplanner.business.orders.entities.OrderElement;
import org.navalplanner.business.workreports.entities.WorkReportLine;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;


/**
 * Dao for {@link WorkReportLineDAO}
 *
 * @author Diego Pino García <dpino@igalia.com>
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */
@Repository
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class WorkReportLineDAO extends IntegrationEntityDAO<WorkReportLine>
        implements IWorkReportLineDAO {

    @SuppressWarnings("unchecked")
    @Override
    public List<WorkReportLine> findByOrderElement(OrderElement orderElement){
        Criteria c = getSession().createCriteria(WorkReportLine.class).createCriteria("orderElement");
        c.add(Restrictions.idEq(orderElement.getId()));
        return (List<WorkReportLine>) c.list();
    }

    @Override
    public List<WorkReportLine> findByOrderElementAndChildren(
            OrderElement orderElement) {
        return findByOrderElementAndChildren(orderElement, false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<WorkReportLine> findByOrderElementAndChildren(OrderElement orderElement, boolean sortByDate) {
        // Create collection with current orderElement and all its children
        Collection<OrderElement> orderElements = orderElement.getAllChildren();
        orderElements.add(orderElement);

        // Prepare criteria
        final Criteria criteria = getSession().createCriteria(WorkReportLine.class);
        criteria.add(Restrictions.in("orderElement", orderElements));
        if (sortByDate) {
            criteria.addOrder(org.hibernate.criterion.Order.asc("date"));
        }
        return criteria.list();
    }

}
