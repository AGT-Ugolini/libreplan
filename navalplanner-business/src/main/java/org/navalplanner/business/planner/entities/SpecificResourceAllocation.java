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

package org.navalplanner.business.planner.entities;

import static org.navalplanner.business.workingday.EffortDuration.min;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.hibernate.validator.NotNull;
import org.hibernate.validator.Valid;
import org.joda.time.LocalDate;
import org.navalplanner.business.calendars.entities.AvailabilityTimeLine;
import org.navalplanner.business.calendars.entities.CombinedWorkHours;
import org.navalplanner.business.calendars.entities.ICalendar;
import org.navalplanner.business.common.ProportionalDistributor;
import org.navalplanner.business.planner.entities.allocationalgorithms.HoursModification;
import org.navalplanner.business.planner.entities.allocationalgorithms.ResourcesPerDayModification;
import org.navalplanner.business.planner.limiting.entities.LimitingResourceQueueElement;
import org.navalplanner.business.resources.daos.IResourceDAO;
import org.navalplanner.business.resources.entities.Resource;
import org.navalplanner.business.resources.entities.Worker;
import org.navalplanner.business.scenarios.entities.Scenario;
import org.navalplanner.business.util.deepcopy.OnCopy;
import org.navalplanner.business.util.deepcopy.Strategy;
import org.navalplanner.business.workingday.EffortDuration;
import org.navalplanner.business.workingday.IntraDayDate;
import org.navalplanner.business.workingday.IntraDayDate.PartialDay;
import org.navalplanner.business.workingday.ResourcesPerDay;

/**
 * Represents the relation between {@link Task} and a specific {@link Worker}.
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
public class SpecificResourceAllocation extends
        ResourceAllocation<SpecificDayAssignment> implements IAllocatable {

    public static SpecificResourceAllocation create(Task task) {
        return create(new SpecificResourceAllocation(
                task));
    }

    /**
     * Creates a {@link SpecificResourceAllocation} for a
     * {@link LimitingResourceQueueElement}
     *
     * The process of creating a specific resource allocation for a queue
     * element is different as it's necessary to assign a resource and a number
     * of resources per day without allocating day assignments
     *
     * @param resource
     * @param task
     * @return
     */
    public static SpecificResourceAllocation createForLimiting(Resource resource,
            Task task) {
        assert resource.isLimitingResource();
        SpecificResourceAllocation result = create(new SpecificResourceAllocation(
                task));
        result.setResource(resource);
        result.setResourcesPerDayToAmount(1);
        return result;
    }

    @OnCopy(Strategy.SHARE)
    private Resource resource;

    private Set<SpecificDayAssignmentsContainer> specificDayAssignmentsContainers = new HashSet<SpecificDayAssignmentsContainer>();

    @Valid
    private Set<SpecificDayAssignmentsContainer> getSpecificDayAssignmentsContainers() {
        return new HashSet<SpecificDayAssignmentsContainer>(
                specificDayAssignmentsContainers);
    }

    public static SpecificResourceAllocation createForTesting(
            ResourcesPerDay resourcesPerDay, Task task) {
        return create(new SpecificResourceAllocation(
                resourcesPerDay, task));
    }

    /**
     * Constructor for hibernate. Do not use!
     */
    public SpecificResourceAllocation() {
        state = buildFromDBState();
    }

    private SpecificDayAssignmentsContainer retrieveOrCreateContainerFor(
            Scenario scenario) {
        Map<Scenario, SpecificDayAssignmentsContainer> containers = containersByScenario();
        SpecificDayAssignmentsContainer retrieved = containers.get(scenario);
        if (retrieved != null) {
            return retrieved;
        }
        SpecificDayAssignmentsContainer result = SpecificDayAssignmentsContainer
                .create(this, scenario);
        specificDayAssignmentsContainers.add(result);
        return result;
    }

    private SpecificResourceAllocation(ResourcesPerDay resourcesPerDay,
            Task task) {
        super(resourcesPerDay, task);
        state = buildInitialTransientState();
    }

    private SpecificResourceAllocation(Task task) {
        super(task);
        state = buildInitialTransientState();
    }

    private DayAssignmentsState buildFromDBState() {
        return new SpecificDayAssignmentsNoExplicitlySpecifiedScenario();
    }

    private TransientState buildInitialTransientState() {
        return new TransientState(new HashSet<SpecificDayAssignment>());
    }

    @NotNull
    public Resource getResource() {
        return resource;
    }

    private Map<Scenario, SpecificDayAssignmentsContainer> containersByScenario() {
        Map<Scenario, SpecificDayAssignmentsContainer> result = new HashMap<Scenario, SpecificDayAssignmentsContainer>();
        for (SpecificDayAssignmentsContainer each : specificDayAssignmentsContainers) {
            assert !result.containsKey(each);
            result.put(each.getScenario(), each);
        }
        return result;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    @Override
    public void allocate(ResourcesPerDay resourcesPerDay) {
        Validate.notNull(resourcesPerDay);
        Validate.notNull(resource);
        new SpecificAssignmentsAllocation().allocate(resourcesPerDay);
    }

    @Override
    public IAllocateResourcesPerDay until(LocalDate endExclusive) {
        return new SpecificAssignmentsAllocation().until(endExclusive);
    }

    @Override
    public IAllocateHoursOnInterval fromStartUntil(LocalDate endExclusive) {
        return new SpecificAssignmentsAllocation().fromStartUntil(endExclusive);
    }

    private final class SpecificAssignmentsAllocation extends
            AssignmentsAllocation {

        @Override
        protected List<SpecificDayAssignment> distributeForDay(LocalDate day,
                EffortDuration effort) {
            return Arrays.asList(SpecificDayAssignment.create(day, effort,
                    resource));
        }

        @Override
        protected AvailabilityTimeLine getResourcesAvailability() {
            return AvailabilityCalculator.getCalendarAvailabilityFor(resource);
        }
    }

    @Override
    public IAllocateHoursOnInterval onInterval(LocalDate start, LocalDate end) {
        return new SpecificAssignmentsAllocation().onInterval(start, end);
    }

    @Override
    protected ICalendar getCalendarGivenTaskCalendar(ICalendar taskCalendar) {
        return CombinedWorkHours.minOf(taskCalendar, getResource()
                .getCalendar());
    }

    @Override
    protected Class<SpecificDayAssignment> getDayAssignmentType() {
        return SpecificDayAssignment.class;
    }

    public List<DayAssignment> createAssignmentsAtDay(PartialDay day,
            ResourcesPerDay resourcesPerDay, EffortDuration limit) {
        EffortDuration effort = calculateTotalToDistribute(day, resourcesPerDay);
        SpecificDayAssignment specific = SpecificDayAssignment.create(
                day.getDate(), min(limit, effort), resource);
        List<DayAssignment> result = new ArrayList<DayAssignment>();
        result.add(specific);
        return result;
    }

    @Override
    public IAllocatable withPreviousAssociatedResources() {
        return this;
    }

    @Override
    public List<Resource> getAssociatedResources() {
        return Arrays.asList(resource);
    }

    @Override
    ResourceAllocation<SpecificDayAssignment> createCopy(Scenario scenario) {
        SpecificResourceAllocation result = create(getTask());
        result.toTransientStateWithInitial(getUnorderedFor(scenario),
                getIntraDayEndFor(scenario));
        result.resource = getResource();
        return result;
    }

    private void toTransientStateWithInitial(
            Set<SpecificDayAssignment> initialAssignments, IntraDayDate end) {
        this.state = new TransientState(initialAssignments);
        this.state.setIntraDayEnd(end);
    }

    @Override
    public ResourcesPerDayModification asResourcesPerDayModification() {
        return ResourcesPerDayModification.create(this, getResourcesPerDay());
    }

    @Override
    public HoursModification asHoursModification() {
        return HoursModification.create(this, getAssignedHours());
    }

    @Override
    public ResourcesPerDayModification withDesiredResourcesPerDay(
            ResourcesPerDay resourcesPerDay) {
        return ResourcesPerDayModification.create(this, resourcesPerDay);
    }

    @Override
    public List<Resource> querySuitableResources(IResourceDAO resourceDAO) {
        return Collections.singletonList(resource);
    }

    protected DayAssignmentsState explicitlySpecifiedState(Scenario scenario) {
        SpecificDayAssignmentsContainer container = retrieveOrCreateContainerFor(scenario);
        return new ExplicitlySpecifiedScenarioState(container);
    }

    private class ExplicitlySpecifiedScenarioState extends DayAssignmentsState {

        private SpecificResourceAllocation outerSpecificAllocation = SpecificResourceAllocation.this;
        private final SpecificDayAssignmentsContainer container;

        private ExplicitlySpecifiedScenarioState(
                SpecificDayAssignmentsContainer container) {
            Validate.notNull(container);
            this.container = container;
        }

        @Override
        protected void addAssignments(
                Collection<? extends SpecificDayAssignment> assignments) {
            container.addAll(assignments);
        }

        @Override
        protected Collection<SpecificDayAssignment> getUnorderedAssignments() {
            return container.getDayAssignments();
        }

        @Override
        protected void removeAssignments(
                List<? extends DayAssignment> assignments) {
            container.removeAll(assignments);
        }

        @Override
        protected void resetTo(
                Collection<SpecificDayAssignment> assignmentsCopied) {
            container.resetTo(assignmentsCopied);
        }

        @Override
        IntraDayDate getIntraDayEnd() {
            return container.getIntraDayEnd();
        }

        @Override
        public void setIntraDayEnd(IntraDayDate intraDayEnd) {
            container.setIntraDayEnd(intraDayEnd);
        }

        @Override
        protected void setParentFor(SpecificDayAssignment each) {
            each.setSpecificResourceAllocation(outerSpecificAllocation);
        }

        protected void copyTransientPropertiesIfAppropiateTo(
                DayAssignmentsState newStateForScenario) {
        }

    }

    private class TransientState extends
            ResourceAllocation<SpecificDayAssignment>.TransientState {
        private SpecificResourceAllocation outerSpecificAllocation = SpecificResourceAllocation.this;

        TransientState(Set<SpecificDayAssignment> specificDayAssignments) {
            super(specificDayAssignments);
        }

        @Override
        protected void setParentFor(SpecificDayAssignment each) {
            each.setSpecificResourceAllocation(outerSpecificAllocation);
        }

    }

    private Set<SpecificDayAssignment> getUnorderedFor(Scenario scenario) {
        SpecificDayAssignmentsContainer container = containersByScenario()
                .get(scenario);
        if (container == null) {
            return new HashSet<SpecificDayAssignment>();
        }
        return container.getDayAssignments();
    }

    private IntraDayDate getIntraDayEndFor(Scenario scenario) {
        SpecificDayAssignmentsContainer container = containersByScenario().get(
                scenario);
        if (container == null) {
            return null;
        }
        return container.getIntraDayEnd();
    }

    private class SpecificDayAssignmentsNoExplicitlySpecifiedScenario extends
            NoExplicitlySpecifiedScenario {

        @Override
        protected Collection<SpecificDayAssignment> getUnorderedAssignmentsForScenario(
                Scenario scenario) {
            return getUnorderedFor(scenario);
        }

        @Override
        protected IntraDayDate getIntraDayEndFor(Scenario scenario) {
            return retrieveOrCreateContainerFor(scenario).getIntraDayEnd();
        }

    }

    @OnCopy(Strategy.IGNORE)
    private DayAssignmentsState state;

    @Override
    protected void scenarioChangedTo(Scenario scenario) {
        this.state = getDayAssignmentsState().switchTo(scenario);
    }

    @Override
    protected ResourceAllocation<SpecificDayAssignment>.DayAssignmentsState getDayAssignmentsState() {
        return state;
    }

    @Override
    public void makeAssignmentsContainersDontPoseAsTransientAnyMore() {
        for (SpecificDayAssignmentsContainer each : specificDayAssignmentsContainers) {
            each.dontPoseAsTransientObjectAnymore();
        }
    }

    @Override
    public void copyAssignments(Scenario from, Scenario to) {
        SpecificDayAssignmentsContainer fromContainer = retrieveOrCreateContainerFor(from);
        SpecificDayAssignmentsContainer toContainer = retrieveOrCreateContainerFor(to);
        toContainer.resetTo(fromContainer.getDayAssignments());
    }

    @Override
    protected void removePredecessorContainersFor(Scenario scenario) {
        Map<Scenario, SpecificDayAssignmentsContainer> byScenario = containersByScenario();
        for (Scenario each : scenario.getPredecessors()) {
            SpecificDayAssignmentsContainer container = byScenario.get(each);
            if (container != null) {
                specificDayAssignmentsContainers.remove(container);
            }
        }
    }

    @Override
    protected void removeContainersFor(Scenario scenario) {
        SpecificDayAssignmentsContainer container = containersByScenario().get(
                scenario);
        if (container != null) {
            specificDayAssignmentsContainers.remove(container);
        }
    }

    public void allocateKeepingProportions(LocalDate start,
            LocalDate endExclusive, int newHoursForInterval) {
        List<DayAssignment> assignments = getAssignments(start, endExclusive);
        ProportionalDistributor distributor = ProportionalDistributor
                .create(asHours(assignments));
        int[] newHoursPerDay = distributor.distribute(newHoursForInterval);
        resetAssigmentsForInterval(start, endExclusive, assignmentsForNewHours(
                assignments, newHoursPerDay));
    }

    private List<SpecificDayAssignment> assignmentsForNewHours(
            List<DayAssignment> assignments, int[] newHoursPerDay) {
        List<SpecificDayAssignment> result = new ArrayList<SpecificDayAssignment>();
        int i = 0;
        for (DayAssignment each : assignments) {
            EffortDuration durationForAssignment = EffortDuration
                    .hours(newHoursPerDay[i++]);
            result.add(SpecificDayAssignment.create(each.getDay(),
                    durationForAssignment, resource));
        }
        return result;
    }

    private int[] asHours(List<DayAssignment> assignments) {
        int[] result = new int[assignments.size()];
        int i = 0;
        for (DayAssignment each : assignments) {
            result[i++] = each.getHours();
        }
        return result;
    }

    public void overrideConsolidatedDayAssignments(
            SpecificResourceAllocation origin) {
        if (origin != null) {
            List<SpecificDayAssignment> originAssignments = origin
                    .getConsolidatedAssignments();
            resetAssignmentsTo(SpecificDayAssignment
                    .copyToAssignmentsWithoutParent(originAssignments));
        }
    }

}
