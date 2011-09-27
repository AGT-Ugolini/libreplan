/*
 * This file is part of NavalPlan
 *
 * Copyright (C) 2011 Igalia, S.L.
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

package org.navalplanner.web.planner.allocation;

import static org.navalplanner.web.I18nHelper._;

import org.navalplanner.business.planner.entities.Task;
import org.navalplanner.business.planner.entities.TaskElement;
import org.navalplanner.web.planner.order.PlanningStateCreator.PlanningState;
import org.navalplanner.web.planner.taskedition.EditTaskController;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.zkoss.ganttz.extensions.IContextWithPlannerTask;

/**
 * A command that opens a window to make the advance allocation of a task.
 *
 * @author Manuel Rego Casasnovas <rego@igalia.com>
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class AdvancedAllocationCommand implements IAdvancedAllocationCommand {

    private EditTaskController editTaskController;
    private PlanningState planningState;

    @Override
    public String getName() {
        return _("Advanced allocation");
    }

    @Override
    public String getIcon() {
        return "/common/img/ico_menu_advanced-assignment.png";
    }

    @Override
    public void doAction(IContextWithPlannerTask<TaskElement> context,
            TaskElement taskElement) {
        if (isApplicableTo(taskElement)) {
            editTaskController.showAdvancedAllocation((Task) taskElement,
                    context, planningState);
        }
    }

    @Override
    public boolean isApplicableTo(TaskElement task) {
        return (task instanceof Task) && !task.isSubcontracted();
    }

    @Override
    public void initialize(EditTaskController editTaskController,
            PlanningState state) {
        this.editTaskController = editTaskController;
        this.planningState = state;
    }

}
