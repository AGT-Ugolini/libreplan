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

package org.navalplanner.business.planner.entities.allocationalgorithms;

import static org.navalplanner.business.i18n.I18nHelper._;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.joda.time.LocalDate;
import org.navalplanner.business.calendars.entities.AvailabilityTimeLine;
import org.navalplanner.business.calendars.entities.CombinedWorkHours;
import org.navalplanner.business.calendars.entities.ICalendar;
import org.navalplanner.business.calendars.entities.ResourceCalendar;
import org.navalplanner.business.calendars.entities.SameWorkHoursEveryDay;
import org.navalplanner.business.planner.entities.AvailabilityCalculator;
import org.navalplanner.business.planner.entities.DayAssignment;
import org.navalplanner.business.planner.entities.GenericResourceAllocation;
import org.navalplanner.business.planner.entities.ResourceAllocation;
import org.navalplanner.business.planner.entities.SpecificResourceAllocation;
import org.navalplanner.business.resources.daos.IResourceDAO;
import org.navalplanner.business.resources.entities.Criterion;
import org.navalplanner.business.resources.entities.Resource;
import org.navalplanner.business.workingday.EffortDuration;
import org.navalplanner.business.workingday.IntraDayDate.PartialDay;
import org.navalplanner.business.workingday.ResourcesPerDay;

public abstract class ResourcesPerDayModification extends
        AllocationModification {

    private static class OnGenericAllocation extends
            ResourcesPerDayModification {
        private final GenericResourceAllocation genericAllocation;

        OnGenericAllocation(
                GenericResourceAllocation resourceAllocation,
                ResourcesPerDay resourcesPerDay,
                Collection<? extends Resource> resources) {
            super(resourceAllocation, resourcesPerDay, resources);
            Validate.isTrue(!resources.isEmpty());
            this.genericAllocation = resourceAllocation;
        }

        @Override
        public void applyAllocationOnAllTaskLength() {
            genericAllocation.forResources(getResources()).allocate(getGoal());
        }

        @Override
        public void applyAllocationUntil(LocalDate endExclusive) {
            genericAllocation.forResources(getResources())
                    .resourcesPerDayUntil(endExclusive).allocate(getGoal());
        }

        @Override
        public List<DayAssignment> createAssignmentsAtDay(PartialDay day,
                EffortDuration limit) {
            return genericAllocation.createAssignmentsAtDay(getResources(),
                    day, getGoal(), limit);
        }

        @Override
        public AvailabilityTimeLine getAvailability() {
            return AvailabilityCalculator.buildSumOfAvailabilitiesFor(
                    (Collection<? extends Criterion>) genericAllocation
                            .getCriterions(), getResources());
        }

        @Override
        public String getNoValidPeriodsMessage() {
            String firstLine = _("There are no days available due to not satisfying the criterions.");
            String secondLine = _("Another possibility is that the resources don't have days available due to their calendars.");
            return firstLine + "\n" + secondLine;
        }

        @Override
        public String getNoValidPeriodsMessageDueToIntersectionMessage() {
            String firstLine = _("There are no days available in the days marked available by the task calendar.");
            String secondLine = _("Maybe the criterions are not satisfied in those days.");
            return firstLine + "\n" + secondLine;
        }

        @Override
        public ICalendar getResourcesCalendar() {
            return CombinedWorkHours.maxOf(resourcesCalendar());
        }

        private List<ICalendar> resourcesCalendar() {
            List<ICalendar> calendar = new ArrayList<ICalendar>();
            for (Resource each : getResources()) {
                calendar.add(calendarFor(each));
            }
            return calendar;
        }

    }

    private static class OnSpecificAllocation extends
            ResourcesPerDayModification {

        private final SpecificResourceAllocation resourceAllocation;

        OnSpecificAllocation(
                SpecificResourceAllocation resourceAllocation,
                ResourcesPerDay resourcesPerDay,
                Collection<? extends Resource> resources) {
            super(resourceAllocation, resourcesPerDay, resources);
            this.resourceAllocation = resourceAllocation;
        }

        @Override
        public void applyAllocationOnAllTaskLength() {
            resourceAllocation.allocate(getGoal());
        }

        @Override
        public void applyAllocationUntil(LocalDate endExclusive) {
            resourceAllocation.resourcesPerDayUntil(endExclusive).allocate(
                    getGoal());
        }

        @Override
        public List<DayAssignment> createAssignmentsAtDay(PartialDay day,
                EffortDuration limit) {
            return resourceAllocation.createAssignmentsAtDay(day, getGoal(),
                    limit);
        }

        @Override
        public AvailabilityTimeLine getAvailability() {
            Resource resource = getAssociatedResource();
            return AvailabilityCalculator.getCalendarAvailabilityFor(resource);
        }

        @Override
        public String getNoValidPeriodsMessage() {
            return _("The resource's calendar has no available days starting from the start of the task.");
        }

        @Override
        public String getNoValidPeriodsMessageDueToIntersectionMessage() {
            return _("There are no days available at resource's calendar in the days marked available by the task's calendar.");
        }

        private Resource getAssociatedResource() {
            return getResources().get(0);
        }

        @Override
        public ICalendar getResourcesCalendar() {
            return calendarFor(getAssociatedResource());
        }

    }

    public static ResourcesPerDayModification create(
            GenericResourceAllocation resourceAllocation,
            ResourcesPerDay resourcesPerDay, List<Resource> resources) {
        return new OnGenericAllocation(resourceAllocation,
                resourcesPerDay, resources);
    }

    public static List<ResourcesPerDayModification> withNewResources(
            List<ResourceAllocation<?>> allocations, IResourceDAO resourceDAO) {
        List<ResourcesPerDayModification> result = fromExistent(allocations);
        for (ResourcesPerDayModification each : result) {
            each.withNewResources(resourceDAO);
        }
        return result;
    }

    public static ResourcesPerDayModification create(
            SpecificResourceAllocation resourceAllocation,
            ResourcesPerDay resourcesPerDay) {
        return new OnSpecificAllocation(resourceAllocation,
                resourcesPerDay, Collections.singletonList(resourceAllocation
                        .getResource()));
    }

    public static List<ResourcesPerDayModification> fromExistent(
            Collection<? extends ResourceAllocation<?>> allocations) {
        List<ResourcesPerDayModification> result = new ArrayList<ResourcesPerDayModification>();
        for (ResourceAllocation<?> resourceAllocation : allocations) {
            Validate.isTrue(resourceAllocation.hasAssignments());
            ResourcesPerDay perDay = resourceAllocation
                    .getResourcesPerDay();
            Validate.notNull(perDay);
            result.add(resourceAllocation.asResourcesPerDayModification());
        }
        return result;
    }

    protected static ICalendar calendarFor(Resource associatedResource) {
        ResourceCalendar calendar = associatedResource.getCalendar();
        return calendar != null ? calendar : SameWorkHoursEveryDay
                .getDefaultWorkingDay();
    }

    private final ResourcesPerDay goal;

    private ResourcesPerDayModification(
            ResourceAllocation<?> resourceAllocation,
            ResourcesPerDay resourcesPerDay,
            Collection<? extends Resource> resources) {
        super(resourceAllocation, resources);
        this.goal = resourcesPerDay;
    }

    public ResourcesPerDay getGoal() {
        return goal;
    }

    public abstract ICalendar getResourcesCalendar();

    public abstract void applyAllocationOnAllTaskLength();

    public abstract void applyAllocationUntil(LocalDate endExclusive);

    public abstract List<DayAssignment> createAssignmentsAtDay(PartialDay day,
            EffortDuration limit);

    public abstract AvailabilityTimeLine getAvailability();

    public abstract String getNoValidPeriodsMessage();

    public abstract String getNoValidPeriodsMessageDueToIntersectionMessage();

    public boolean isDayFilled(LocalDate day, EffortDuration taken) {
        return getBeingModified().getAllocationCalendar()
                .asDurationOn(PartialDay.wholeDay(day), goal).equals(taken);
    }


}
