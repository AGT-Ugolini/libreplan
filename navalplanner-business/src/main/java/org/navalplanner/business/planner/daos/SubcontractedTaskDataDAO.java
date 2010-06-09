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

package org.navalplanner.business.planner.daos;

import java.util.ArrayList;
import java.util.List;

import org.navalplanner.business.common.daos.GenericDAOHibernate;
import org.navalplanner.business.common.exceptions.InstanceNotFoundException;
import org.navalplanner.business.orders.entities.Order;
import org.navalplanner.business.orders.entities.OrderElement;
import org.navalplanner.business.planner.entities.SubcontractedTaskData;
import org.navalplanner.business.planner.entities.Task;
import org.navalplanner.business.planner.entities.TaskElement;
import org.navalplanner.business.scenarios.bootstrap.PredefinedScenarios;
import org.navalplanner.business.scenarios.entities.Scenario;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO for the {@link SubcontractedTaskDataDAO} entity.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
@Repository
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class SubcontractedTaskDataDAO extends
        GenericDAOHibernate<SubcontractedTaskData, Long> implements
        ISubcontractedTaskDataDAO {

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public boolean existsInAnohterTransaction(Long id) {
        if (id == null) {
            return false;
        }

        try {
            SubcontractedTaskData found = find(id);
            return (found != null) && (found.getId().equals(id));
        } catch (InstanceNotFoundException e) {
            return false;
        }
    }

    @Override
    public List<SubcontractedTaskData> getAll() {
        return list(SubcontractedTaskData.class);
    }

    @Override
    public void removeOrphanedSubcontractedTaskData() {
        for (SubcontractedTaskData subcontractedTaskData : getAll()) {
            if (subcontractedTaskData.getTask() == null) {
                getSession().delete(subcontractedTaskData);
            }
        }
    }

    @Override
    public List<SubcontractedTaskData> getAllForMasterScenario() {
        Scenario masterScenario = PredefinedScenarios.MASTER.getScenario();

        List<SubcontractedTaskData> result = new ArrayList<SubcontractedTaskData>();
        for (Order order : masterScenario.getOrders().keySet()) {
            order.useSchedulingDataFor(masterScenario);
            for (OrderElement orderElement : order.getAllOrderElements()) {
                for (TaskElement taskElement : orderElement.getTaskElements()) {
                    if (taskElement.isSubcontracted()) {
                        result.add(((Task) taskElement)
                                .getSubcontractedTaskData());
                    }
                }
            }
        }

        return result;
    }

}