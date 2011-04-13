/*
 * This file is part of NavalPlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                         Desenvolvemento Tecnolóxico de Galicia
 * Copyright (C) 2010-2011 Igalia, S.L.
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

package org.navalplanner.business.planner.entities;

import java.util.Arrays;
import java.util.List;

import org.navalplanner.business.planner.entities.allocationalgorithms.ResourcesPerDayModification;

/**
 *
 * @author Diego Pino García <dpino@igalia.com>
 *
 *         Calculate hours per day for resource based on total amount of hours to
 *         be done and number of resources per day
 *
 */
public class NoneFunction extends AssignmentFunction {

    public static NoneFunction create() {
        return create(new NoneFunction());
    }

    protected NoneFunction() {

    }

    public String getName() {
        return ASSIGNMENT_FUNCTION_NAME.NONE.toString();
    }


    public void applyTo(ResourceAllocation<?> resourceAllocation) {
        apply(resourceAllocation);
    }

    private void apply(ResourceAllocation<?> resourceAllocation) {
        int hours = resourceAllocation.getAssignedHours();

        List<ResourcesPerDayModification> resourcesPerDayModification = Arrays.asList(resourceAllocation
                    .asResourcesPerDayModification());

        ResourceAllocation
            .allocating(resourcesPerDayModification)
            .untilAllocating(hours);
    }

}