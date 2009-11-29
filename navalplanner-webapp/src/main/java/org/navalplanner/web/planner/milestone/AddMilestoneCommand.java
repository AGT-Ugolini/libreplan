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

package org.navalplanner.web.planner.milestone;

import static org.navalplanner.web.I18nHelper._;

import org.apache.commons.lang.Validate;
import org.navalplanner.business.planner.daos.ITaskElementDAO;
import org.navalplanner.business.planner.entities.TaskElement;
import org.navalplanner.business.planner.entities.TaskGroup;
import org.navalplanner.business.planner.entities.TaskMilestone;
import org.navalplanner.web.planner.order.PlanningState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.zkoss.ganttz.data.Position;
import org.zkoss.ganttz.extensions.IContextWithPlannerTask;

/**
 * Command to add a new {@link TaskMilestone} <br />
 * @author Lorenzo Tilve Álvaro <ltilve@igalia.com>
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class AddMilestoneCommand implements IAddMilestoneCommand {

    private PlanningState planningState;

    @Autowired
    private ITaskElementDAO taskElementDAO;

    @Override
    public void setState(PlanningState planningState) {
        this.planningState = planningState;
    }

    @Override
    @Transactional(readOnly = true)
    public void doAction(IContextWithPlannerTask<TaskElement> context,
            TaskElement task) {
        TaskMilestone milestone = TaskMilestone.create();
        milestone.setName("new milestone");
        taskElementDAO.reattach(task);
        InsertionPoint insertionPoint = getInsertionPoint(task);
        insertionPoint.root.addTaskElement(insertionPoint.insertionPosition,
                milestone);
        context.add(Position
                .createAtTopPosition(insertionPoint.insertionPosition),
                milestone);
        planningState.added(milestone.getParent());
    }

    private static class InsertionPoint {
        final TaskGroup root;

        final int insertionPosition;

        private InsertionPoint(TaskGroup root, int position) {
            this.root = root;
            this.insertionPosition = position;
        }
    }

    private InsertionPoint getInsertionPoint(TaskElement task) {
        Validate.isTrue(task.getParent() != null,
                "the task parent is not null "
                        + "since all shown tasks are children "
                        + "of the root TaskGroup");
        TaskGroup taskParent = task.getParent();
        if (taskParent.getParent() == null) {
            return new InsertionPoint(taskParent, taskParent.getChildren()
                    .indexOf(task));
        }
        return getInsertionPoint(taskParent);
    }

    @Override
    public String getName() {
        return _("Add Milestone");
    }

}