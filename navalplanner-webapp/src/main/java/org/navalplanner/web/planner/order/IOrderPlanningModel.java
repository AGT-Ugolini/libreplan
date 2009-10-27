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

package org.navalplanner.web.planner.order;

import java.util.List;

import org.navalplanner.business.orders.entities.Order;
import org.navalplanner.business.planner.entities.TaskElement;
import org.navalplanner.web.common.ViewSwitcher;
import org.navalplanner.web.planner.allocation.ResourceAllocationController;
import org.navalplanner.web.planner.calendar.CalendarAllocationController;
import org.navalplanner.web.planner.splitting.SplittingController;
import org.navalplanner.web.planner.taskedition.EditTaskController;
import org.zkoss.ganttz.Planner;
import org.zkoss.ganttz.extensions.ICommand;

/**
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 */
public interface IOrderPlanningModel {

    void setConfigurationToPlanner(Planner planner, Order order,
            ViewSwitcher viewSwitcher,
            ResourceAllocationController resourceAllocationController,
            EditTaskController editTaskController,
            SplittingController splittingController,
            CalendarAllocationController calendarAllocationController,
            List<ICommand<TaskElement>> additional);

}
