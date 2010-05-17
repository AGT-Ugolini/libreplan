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

package org.navalplanner.web.resourceload;

import static org.navalplanner.web.I18nHelper._;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.joda.time.LocalDate;
import org.navalplanner.business.calendars.daos.IBaseCalendarDAO;
import org.navalplanner.business.calendars.entities.ResourceCalendar;
import org.navalplanner.business.common.BaseEntity;
import org.navalplanner.business.common.daos.IConfigurationDAO;
import org.navalplanner.business.common.exceptions.InstanceNotFoundException;
import org.navalplanner.business.orders.daos.IOrderDAO;
import org.navalplanner.business.orders.daos.IOrderElementDAO;
import org.navalplanner.business.orders.entities.Order;
import org.navalplanner.business.orders.entities.OrderElement;
import org.navalplanner.business.planner.daos.IDayAssignmentDAO;
import org.navalplanner.business.planner.daos.IResourceAllocationDAO;
import org.navalplanner.business.planner.daos.ITaskElementDAO;
import org.navalplanner.business.planner.entities.DayAssignment;
import org.navalplanner.business.planner.entities.GenericResourceAllocation;
import org.navalplanner.business.planner.entities.ResourceAllocation;
import org.navalplanner.business.planner.entities.SpecificResourceAllocation;
import org.navalplanner.business.planner.entities.Task;
import org.navalplanner.business.planner.entities.TaskElement;
import org.navalplanner.business.resources.daos.IResourceDAO;
import org.navalplanner.business.resources.entities.Criterion;
import org.navalplanner.business.resources.entities.CriterionSatisfaction;
import org.navalplanner.business.resources.entities.Resource;
import org.navalplanner.business.users.daos.IOrderAuthorizationDAO;
import org.navalplanner.business.users.daos.IUserDAO;
import org.navalplanner.business.users.entities.OrderAuthorization;
import org.navalplanner.business.users.entities.OrderAuthorizationType;
import org.navalplanner.business.users.entities.User;
import org.navalplanner.business.users.entities.UserRole;
import org.navalplanner.web.calendars.BaseCalendarModel;
import org.navalplanner.web.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.zkoss.ganttz.data.resourceload.LoadPeriod;
import org.zkoss.ganttz.data.resourceload.LoadTimeLine;
import org.zkoss.ganttz.data.resourceload.TimeLineRole;
import org.zkoss.ganttz.timetracker.zoom.ZoomLevel;
import org.zkoss.ganttz.util.Interval;

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ResourceLoadModel implements IResourceLoadModel {

    @Autowired
    private IResourceDAO resourcesDAO;

    @Autowired
    private IOrderElementDAO orderElementDAO;

    @Autowired
    private IOrderDAO orderDAO;

    @Autowired
    private ITaskElementDAO taskElementDAO;

    @Autowired
    private IResourceAllocationDAO resourceAllocationDAO;

    @Autowired
    private IUserDAO userDAO;

    @Autowired
    private IOrderAuthorizationDAO orderAuthorizationDAO;

    private List<LoadTimeLine> loadTimeLines;
    private Interval viewInterval;

    private Order filterBy;

    private boolean filterByResources = true;

    private List<Resource> resourcesToShowList = new ArrayList<Resource>();

    private List<Criterion> criteriaToShowList = new ArrayList<Criterion>();

    private Date initDateFilter;
    private Date endDateFilter;

    @Autowired
    private IDayAssignmentDAO dayAssignmentDAO;

    @Autowired
    private IBaseCalendarDAO baseCalendarDAO;

    @Autowired
    private IConfigurationDAO configurationDAO;

    @Override
    @Transactional(readOnly = true)
    public void initGlobalView(boolean filterByResources) {
        filterBy = null;
        this.filterByResources = filterByResources;
        doGlobalView();
    }

    @Override
    @Transactional(readOnly = true)
    public void initGlobalView(Order filterBy, boolean filterByResources) {
        this.filterBy = orderDAO.findExistingEntity(filterBy.getId());
        this.filterByResources = filterByResources;
        doGlobalView();
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderByTask(TaskElement task) {
        return orderElementDAO
                .loadOrderAvoidingProxyFor(task.getOrderElement());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userCanRead(Order order, String loginName) {
        if (SecurityUtils.isUserInRole(UserRole.ROLE_READ_ALL_ORDERS)
                || SecurityUtils.isUserInRole(UserRole.ROLE_EDIT_ALL_ORDERS)) {
            return true;
        }
        try {
            User user = userDAO.findByLoginName(loginName);
            for (OrderAuthorization authorization : orderAuthorizationDAO
                    .listByOrderUserAndItsProfiles(order, user)) {
                if (authorization.getAuthorizationType() == OrderAuthorizationType.READ_AUTHORIZATION
                        || authorization.getAuthorizationType() == OrderAuthorizationType.WRITE_AUTHORIZATION) {
                    return true;
                }
            }
        } catch (InstanceNotFoundException e) {
            // this case shouldn't happen, because it would mean that there
            // isn't a logged user
            // anyway, if it happenned we don't allow the user to pass
        }
        return false;
    }

    private void doGlobalView() {
        loadTimeLines = calculateLoadTimeLines();
        if (!loadTimeLines.isEmpty()) {
            viewInterval = LoadTimeLine.getIntervalFrom(loadTimeLines);
        } else {
            viewInterval = new Interval(new Date(), plusFiveYears(new Date()));
        }
    }

    private Date plusFiveYears(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.YEAR, 5);
        return calendar.getTime();
    }

    private List<LoadTimeLine> calculateLoadTimeLines() {
        List<LoadTimeLine> result = new ArrayList<LoadTimeLine>();
        if (filterByResources) {
            result.addAll(groupsFor(resourcesToShow()));
        } else {
            result.addAll(groupsFor(genericAllocationsByCriterion()));
        }
        return result;
    }

    private Map<Criterion, List<GenericResourceAllocation>> genericAllocationsByCriterion() {
        if(!criteriaToShowList.isEmpty()) {
            return resourceAllocationDAO
                    .findGenericAllocationsBySomeCriterion(criteriaToShowList, initDateFilter, endDateFilter);
        }
        if (filter()) {
            List<Criterion> criterions = new ArrayList<Criterion>();
            List<GenericResourceAllocation> generics = new ArrayList<GenericResourceAllocation>();
            List<Task> tasks = justTasks(filterBy
                    .getAllChildrenAssociatedTaskElements());
            for (Task task : tasks) {

                List<ResourceAllocation<?>> listAllocations = new ArrayList<ResourceAllocation<?>>(
                        task.getSatisfiedResourceAllocations());
                for (GenericResourceAllocation generic : (onlyGeneric(listAllocations))) {
                    criterions.addAll(generic.getCriterions());
                }
            }
            return resourceAllocationDAO
                    .findGenericAllocationsBySomeCriterion(criterions, initDateFilter, endDateFilter);
        } else {
            return resourceAllocationDAO.findGenericAllocationsByCriterion(initDateFilter, endDateFilter);
        }
    }

    private List<Resource> resourcesToShow() {
        if(!resourcesToShowList.isEmpty()) {
            return getResourcesToShowReattached();
        }
        // if we haven't manually specified some resources to show, we load them
        if (filter()) {
            return resourcesForActiveTasks();
        } else {
            return allResources();
        }
    }

    private boolean filter() {
        return filterBy != null;
    }

    private List<Resource> resourcesForActiveTasks() {
        return Resource.sortByName(resourcesDAO
                .findResourcesRelatedTo(justTasks(filterBy
                        .getAllChildrenAssociatedTaskElements())));
    }

    private List<Task> justTasks(Collection<? extends TaskElement> tasks) {
        List<Task> result = new ArrayList<Task>();
        for (TaskElement taskElement : tasks) {
            if (taskElement instanceof Task) {
                result.add((Task) taskElement);
            }
        }
        return result;
    }

    private List<Resource> allResources() {
        return Resource.sortByName(resourcesDAO.list(Resource.class));
    }

    private TimeLineRole<BaseEntity> getCurrentTimeLineRole(BaseEntity entity) {
        return new TimeLineRole<BaseEntity>(entity);
    }

    /**
     * @param genericAllocationsByCriterion
     * @return
     */
    private List<LoadTimeLine> groupsFor(
            Map<Criterion, List<GenericResourceAllocation>> genericAllocationsByCriterion) {
        List<LoadTimeLine> result = new ArrayList<LoadTimeLine>();
        List<Criterion> criterions = Criterion
                .sortByTypeAndName(genericAllocationsByCriterion.keySet());
        for (Criterion criterion : criterions) {
            List<GenericResourceAllocation> allocations = ResourceAllocation
                    .sortedByStartDate(genericAllocationsByCriterion
                            .get(criterion));
            TimeLineRole<BaseEntity> role = getCurrentTimeLineRole(criterion);
            LoadTimeLine group = new LoadTimeLine(createPrincipal(criterion,
                    allocations, role),
                    buildSecondLevel(criterion, allocations));
            if (!group.isEmpty()) {
                result.add(group);
            }
        }
        return result;
    }

    private List<LoadTimeLine> buildSecondLevel(Criterion criterion,
            List<GenericResourceAllocation> allocations) {
        List<LoadTimeLine> result = new ArrayList<LoadTimeLine>();
        Map<Order, List<ResourceAllocation<?>>> byOrder = byOrder(new ArrayList<ResourceAllocation<?>>(
                allocations));

        if (filter()) {
            // build time lines for current order
            if (byOrder.get(filterBy) != null) {
                result.addAll(buildTimeLinesForOrder(criterion, byOrder
                        .get(filterBy)));
            }
            byOrder.remove(filterBy);
            // build time lines for other orders
            LoadTimeLine lineOthersOrders = buildTimeLinesForOtherOrders(
                    criterion, byOrder);
            if (lineOthersOrders != null) {
                result.add(lineOthersOrders);
            }
        } else {
            result.addAll(buildTimeLinesGroupForOrder(criterion, byOrder));
        }
        return result;
    }

    private List<LoadTimeLine> buildTimeLinesForOrder(Criterion criterion,
            List<ResourceAllocation<?>> allocations) {
        List<LoadTimeLine> result = new ArrayList<LoadTimeLine>();
        result.addAll(buildTimeLinesForEachTask(criterion, allocations));
        return result;
    }

    private LoadTimeLine buildTimeLinesForOtherOrders(Criterion criterion,
            Map<Order, List<ResourceAllocation<?>>> byOrder) {
        List<ResourceAllocation<?>> allocations = getAllSortedValues(byOrder);
        if (allocations.isEmpty()) {
            return null;
        }

        LoadTimeLine group = new LoadTimeLine(buildTimeLine(criterion,
                "Other orders", "global-generic", allocations,
                getCurrentTimeLineRole(null)),
                buildTimeLinesGroupForOrder(
                criterion, byOrder));
        return group;
    }

    private List<LoadTimeLine> buildTimeLinesGroupForOrder(Criterion criterion,
            Map<Order, List<ResourceAllocation<?>>> byOrder) {
        List<LoadTimeLine> result = new ArrayList<LoadTimeLine>();
        for (Order order : byOrder.keySet()) {
            TimeLineRole<BaseEntity> role = getCurrentTimeLineRole(order);
            result.add(new LoadTimeLine(buildTimeLine(criterion, order
                    .getName(), "global-generic", byOrder.get(order), role),
                    buildTimeLinesForOrder(
                    criterion, byOrder.get(order))));
        }
        return result;
    }

    private List<LoadTimeLine> buildTimeLinesForEachTask(Criterion criterion,
            List<ResourceAllocation<?>> allocations) {
        Map<Task, List<ResourceAllocation<?>>> byTask = ResourceAllocation
                .byTask(allocations);

        List<LoadTimeLine> secondLevel = new ArrayList<LoadTimeLine>();
        for (Entry<Task, List<ResourceAllocation<?>>> entry : byTask.entrySet()) {
            Task task = entry.getKey();
            Set<Criterion> criterions = task.getCriterions();
            TimeLineRole<BaseEntity> role = getCurrentTimeLineRole(task);

            /**
             * Each resource line has the same role than its allocated task, so
             * that link with the resource allocation screen
             */
            LoadTimeLine timeLine = new LoadTimeLine(buildTimeLine(criterions,
                    task, criterion, "global-generic", entry.getValue(), role),
                    buildTimeLinesForEachResource(criterion, onlyGeneric(entry
                            .getValue()), role));
            if (!timeLine.isEmpty()) {
                secondLevel.add(timeLine);
            }

        }
        return secondLevel;
    }

    private List<LoadTimeLine> buildTimeLinesForEachResource(
            Criterion criterion, List<GenericResourceAllocation> allocations,
            TimeLineRole<BaseEntity> role) {
        Map<Resource, List<GenericResourceAllocation>> byResource = GenericResourceAllocation
                .byResource(allocations);

        List<LoadTimeLine> secondLevel = new ArrayList<LoadTimeLine>();
        for (Entry<Resource, List<GenericResourceAllocation>> entry : byResource
                .entrySet()) {
            Resource resource = entry.getKey();
            List<GenericResourceAllocation> resourceAllocations = entry
                    .getValue();
            String descriptionTimeLine = getDescriptionResourceWithCriterions(resource);

            LoadTimeLine timeLine = buildTimeLine(resource,
                    descriptionTimeLine, resourceAllocations, "generic", role);
            if (!timeLine.isEmpty()) {
                secondLevel.add(timeLine);
            }

        }
        return secondLevel;
    }

    private String getDescriptionResourceWithCriterions(Resource resource) {
        Set<CriterionSatisfaction> criterionSatisfactions = resource
                .getCriterionSatisfactions();
        return resource.getShortDescription()
                + getCriterionSatisfactionDescription(criterionSatisfactions);
    }

    private String getCriterionSatisfactionDescription(
            Set<CriterionSatisfaction> satisfactions) {
        if (satisfactions.isEmpty()) {
            return "";
        }
        List<Criterion> criterions = new ArrayList<Criterion>();
        for (CriterionSatisfaction satisfaction : satisfactions) {
            criterions.add(satisfaction.getCriterion());
        }
        return " :: " + Criterion.getNames(criterions);
    }

    private LoadTimeLine createPrincipal(Criterion criterion,
            List<GenericResourceAllocation> orderedAllocations,
            TimeLineRole<BaseEntity> role) {
        return new LoadTimeLine(criterion.getType().getName() + ": " + criterion.getName(),
                createPeriods(criterion, orderedAllocations), "global-generic", role);
    }

    private List<LoadPeriod> createPeriods(Criterion criterion,
            List<GenericResourceAllocation> value) {
        if(initDateFilter != null || endDateFilter != null) {
            return PeriodsBuilder
                .build(LoadPeriodGenerator.onCriterion(criterion,
                    resourcesDAO), value,
                    initDateFilter, endDateFilter);
        }
        return PeriodsBuilder
            .build(LoadPeriodGenerator.onCriterion(criterion,
                    resourcesDAO), value);
    }

    private List<LoadTimeLine> groupsFor(List<Resource> allResources) {
        List<LoadTimeLine> result = new ArrayList<LoadTimeLine>();
        for (Resource resource : allResources) {
            LoadTimeLine group = buildGroup(resource);
            if (!group.isEmpty()) {
                result.add(group);
            }
        }
        return result;
    }

    private LoadTimeLine buildGroup(Resource resource) {
        List<ResourceAllocation<?>> sortedByStartDate = ResourceAllocation
                .sortedByStartDate(resourceAllocationDAO
                        .findAllocationsRelatedTo(resource, initDateFilter, endDateFilter));
        TimeLineRole<BaseEntity> role = getCurrentTimeLineRole(resource);
        LoadTimeLine result = new LoadTimeLine(buildTimeLine(resource, resource
                .getName(), sortedByStartDate, "resource", role),
                buildSecondLevel(resource, sortedByStartDate));
        return result;

    }

    private List<LoadTimeLine> buildSecondLevel(Resource resource,
            List<ResourceAllocation<?>> sortedByStartDate) {
        List<LoadTimeLine> result = new ArrayList<LoadTimeLine>();
        Map<Order, List<ResourceAllocation<?>>> byOrder = byOrder(sortedByStartDate);

        if (filter()) {
            // build time lines for current order
            result.addAll(buildTimeLinesForOrder(resource, byOrder
                    .get(filterBy)));
            byOrder.remove(filterBy);
            // build time lines for other orders
            LoadTimeLine lineOthersOrders = buildTimeLinesForOtherOrders(
                    resource, byOrder);
            if (lineOthersOrders != null) {
                result.add(lineOthersOrders);
            }
        } else {
            result.addAll(buildTimeLinesGroupForOrder(resource, byOrder));
        }
        return result;
    }

    private LoadTimeLine buildTimeLinesForOtherOrders(Resource resource,
            Map<Order, List<ResourceAllocation<?>>> byOrder) {
        List<ResourceAllocation<?>> resourceAllocations = getAllSortedValues(byOrder);
        if (resourceAllocations.isEmpty()) {
            return null;
        }
        TimeLineRole<BaseEntity> role = getCurrentTimeLineRole(null);
        LoadTimeLine group = new LoadTimeLine(buildTimeLine(resource,
                _("Other orders"), resourceAllocations, "resource", role),
                buildTimeLinesGroupForOrder(resource, byOrder));
        return group;
    }

    private List<LoadTimeLine> buildTimeLinesGroupForOrder(Resource resource,
            Map<Order, List<ResourceAllocation<?>>> byOrder) {
        List<LoadTimeLine> result = new ArrayList<LoadTimeLine>();
        for (Order order : byOrder.keySet()) {
            TimeLineRole<BaseEntity> role = getCurrentTimeLineRole(order);
            result.add(new LoadTimeLine(buildTimeLine(resource,
                    order.getName(), byOrder.get(order), "resource", role),
                    buildTimeLinesForOrder(resource, byOrder.get(order))));
        }
        return result;
    }

    private List<ResourceAllocation<?>> getAllSortedValues(
            Map<Order, List<ResourceAllocation<?>>> byOrder) {
        List<ResourceAllocation<?>> resourceAllocations = new ArrayList<ResourceAllocation<?>>();
        for (List<ResourceAllocation<?>> listAllocations : byOrder.values()) {
            resourceAllocations.addAll(listAllocations);
        }
        return ResourceAllocation.sortedByStartDate(resourceAllocations);
    }

    private void initializeIfNeeded(
            Map<Order, List<ResourceAllocation<?>>> result, Order order) {
        if (!result.containsKey(order)) {
            result.put(order, new ArrayList<ResourceAllocation<?>>());
        }
    }

    @Transactional(readOnly = true)
    public Map<Order, List<ResourceAllocation<?>>> byOrder(
            Collection<ResourceAllocation<?>> allocations) {
        Map<Order, List<ResourceAllocation<?>>> result = new HashMap<Order, List<ResourceAllocation<?>>>();
        for (ResourceAllocation<?> resourceAllocation : allocations) {
            if ((resourceAllocation.isSatisfied())
                    && (resourceAllocation.getTask() != null)) {
                OrderElement orderElement = resourceAllocation.getTask()
                        .getOrderElement();
                Order order = orderElementDAO
                        .loadOrderAvoidingProxyFor(orderElement);
                initializeIfNeeded(result, order);
                result.get(order).add(resourceAllocation);
            }
        }
        return result;
    }

    private List<LoadTimeLine> buildTimeLinesForOrder(Resource resource,
            List<ResourceAllocation<?>> sortedByStartDate) {
        List<LoadTimeLine> result = new ArrayList<LoadTimeLine>();
        result.addAll(buildTimeLinesForEachTask(resource,
                onlySpecific(sortedByStartDate)));
        result.addAll(buildTimeLinesForEachCriterion(resource,
                onlyGeneric(sortedByStartDate)));
        return result;
    }

    private List<GenericResourceAllocation> onlyGeneric(
            List<ResourceAllocation<?>> sortedByStartDate) {
        return ResourceAllocation.getOfType(GenericResourceAllocation.class,
                sortedByStartDate);
    }

    private List<SpecificResourceAllocation> onlySpecific(
            List<ResourceAllocation<?>> sortedByStartDate) {
        return ResourceAllocation.getOfType(SpecificResourceAllocation.class,
                sortedByStartDate);
    }

    private List<LoadTimeLine> buildTimeLinesForEachCriterion(
            Resource resource, List<GenericResourceAllocation> sortdByStartDate) {
        Map<Set<Criterion>, List<GenericResourceAllocation>> byCriterions = GenericResourceAllocation
                .byCriterions(sortdByStartDate);

        List<LoadTimeLine> result = new ArrayList<LoadTimeLine>();
        for (Entry<Set<Criterion>, List<GenericResourceAllocation>> entry : byCriterions
                .entrySet()) {

            Map<Task, List<ResourceAllocation<?>>> byTask = ResourceAllocation
                    .byTask(new ArrayList<ResourceAllocation<?>>(entry
                            .getValue()));

            for (Entry<Task, List<ResourceAllocation<?>>> entryTask : byTask
                    .entrySet()) {

                Task task = entryTask.getKey();
                List<GenericResourceAllocation> resouceAllocations = onlyGeneric(entryTask
                        .getValue());
                TimeLineRole<BaseEntity> role = getCurrentTimeLineRole(task);
                LoadTimeLine timeLine = buildTimeLine(entry.getKey(), task,
                        resource, "generic", resouceAllocations, role);
                if (!timeLine.isEmpty()) {
                    result.add(timeLine);
                }

            }
        }
        return result;
    }

    private List<LoadTimeLine> buildTimeLinesForEachTask(Resource resource,
            List<SpecificResourceAllocation> sortedByStartDate) {

        List<ResourceAllocation<?>> listOnlySpecific = new ArrayList<ResourceAllocation<?>>(
                sortedByStartDate);
        Map<Task, List<ResourceAllocation<?>>> byTask = ResourceAllocation
                .byTask(listOnlySpecific);

        List<LoadTimeLine> secondLevel = new ArrayList<LoadTimeLine>();
        for (Entry<Task, List<ResourceAllocation<?>>> entry : byTask.entrySet()) {
            Task task = entry.getKey();
            TimeLineRole<BaseEntity> role = getCurrentTimeLineRole(task);
            LoadTimeLine timeLine = buildTimeLine(resource, task.getName(),
                    entry.getValue(), "specific", role);
            if (!timeLine.isEmpty()) {
                secondLevel.add(timeLine);
            }

        }
        return secondLevel;
    }

    public static String getName(Collection<? extends Criterion> criterions,
            Task task) {
        String prefix = task.getName();
        return (prefix + " :: " + Criterion.getNames(criterions));
    }

    private LoadTimeLine buildTimeLine(Resource resource, String name,
            List<? extends ResourceAllocation<?>> sortedByStartDate,
            String type,
            TimeLineRole<BaseEntity> role) {
        List<LoadPeriod> loadPeriods;
        if(initDateFilter != null || endDateFilter != null) {
            loadPeriods = PeriodsBuilder
                .build(LoadPeriodGenerator.onResource(resource), sortedByStartDate,
                        initDateFilter, endDateFilter);
        }
        else {
            loadPeriods = PeriodsBuilder
                .build(LoadPeriodGenerator.onResource(resource), sortedByStartDate);
        }
        return new LoadTimeLine(name, loadPeriods, type, role);
    }

    private LoadTimeLine buildTimeLine(Criterion criterion, String name,
            String type, List<ResourceAllocation<?>> allocations,
            TimeLineRole<BaseEntity> role) {
        List<GenericResourceAllocation> generics = onlyGeneric(allocations);
        return new LoadTimeLine(name, createPeriods(criterion, generics), type,
                role);
    }

    private LoadTimeLine buildTimeLine(Collection<Criterion> criterions,
            Task task, Criterion criterion, String type,
            List<ResourceAllocation<?>> allocations,
            TimeLineRole<BaseEntity> role) {
        return buildTimeLine(criterion, getName(criterions, task), type,
                allocations,
                role);
    }

    private LoadTimeLine buildTimeLine(Collection<Criterion> criterions,
            Task task, Resource resource, String type,
            List<GenericResourceAllocation> allocationsSortedByStartDate,
            TimeLineRole<BaseEntity> role) {
        LoadPeriodGeneratorFactory periodGeneratorFactory = LoadPeriodGenerator
                .onResourceSatisfying(resource, criterions);
        List<LoadPeriod> loadPeriods;
        if(initDateFilter != null || endDateFilter != null) {
            loadPeriods = PeriodsBuilder
                .build(periodGeneratorFactory, allocationsSortedByStartDate,
                initDateFilter, endDateFilter);
        }
        else {
            loadPeriods = PeriodsBuilder
                .build(periodGeneratorFactory, allocationsSortedByStartDate);
        }

        return new LoadTimeLine(getName(criterions, task), loadPeriods,
                type, role);
    }

    @Override
    public List<LoadTimeLine> getLoadTimeLines() {
        return loadTimeLines;
    }

    @Override
    public Interval getViewInterval() {
        return viewInterval;
    }

    public ZoomLevel calculateInitialZoomLevel() {
        Interval interval = getViewInterval();
        return ZoomLevel.getDefaultZoomByDates(new LocalDate(interval
                .getStart()), new LocalDate(interval.getFinish()));
    }

    @Override
    public void setResourcesToShow(List<Resource> resourcesList) {
        this.resourcesToShowList.clear();
        this.resourcesToShowList.addAll(resourcesList);
    }

    @Override
    public void clearResourcesToShow() {
        resourcesToShowList.clear();
    }

    private List<Resource> getResourcesToShowReattached() {
        List<Resource> list = new ArrayList<Resource>();
        for(Resource worker : resourcesToShowList) {
            try {
                //for some reason, resourcesDAO.reattach(worker) doesn't work
                //and we have to retrieve them again with find
                list.add(resourcesDAO.find(worker.getId()));
            }
            catch(InstanceNotFoundException e) {
                //maybe it was removed by another transaction
                //we just ignore the exception to not show the Resource
            }
        }
        return list;
    }

    @Override
    public void clearCriteriaToShow() {
        criteriaToShowList.clear();
    }

    @Override
    public void setCriteriaToShow(List<Criterion> criteriaList) {
        criteriaToShowList.clear();
        criteriaToShowList.addAll(criteriaList);
    }

    @Override
    public void setEndDateFilter(Date value) {
        endDateFilter = value;
    }

    @Override
    public void setInitDateFilter(Date value) {
        initDateFilter = value;
    }

    @Override
    public Date getEndDateFilter() {
        return endDateFilter;
    }

    @Override
    public Date getInitDateFilter() {
        return initDateFilter;
    }

    @Transactional(readOnly = true)
    public List<DayAssignment> getDayAssignments() {
        return dayAssignmentDAO.findByResources(getResources());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Resource> getResources() {
        List<Resource> resources = resourcesToShow();
        for (Resource resource : resources) {
            resourcesDAO.reattach(resource);
            ResourceCalendar calendar = resource.getCalendar();
            baseCalendarDAO.reattach(calendar);
            BaseCalendarModel.forceLoadBaseCalendar(calendar);
            resource.getAssignments().size();
        }
        return resources;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isExpandResourceLoadViewCharts() {
        return configurationDAO.getConfiguration()
                .isExpandResourceLoadViewCharts();
    }

}

class PeriodsBuilder {

    private final List<? extends ResourceAllocation<?>> sortedByStartDate;

    private final List<LoadPeriodGenerator> loadPeriodsGenerators = new LinkedList<LoadPeriodGenerator>();

    private final LoadPeriodGeneratorFactory factory;

    private PeriodsBuilder(LoadPeriodGeneratorFactory factory,
            List<? extends ResourceAllocation<?>> sortedByStartDate) {
        this.factory = factory;
        this.sortedByStartDate = sortedByStartDate;
    }

    public static List<LoadPeriod> build(LoadPeriodGeneratorFactory factory,
            List<? extends ResourceAllocation<?>> sortedByStartDate) {
        return new PeriodsBuilder(factory, sortedByStartDate).buildPeriods();
    }

    public static List<LoadPeriod> build(LoadPeriodGeneratorFactory factory,
            List<? extends ResourceAllocation<?>> sortedByStartDate,
            Date startDateFilter, Date endDateFilter) {
        List<LoadPeriod> list = new PeriodsBuilder(factory, sortedByStartDate).buildPeriods();
        List<LoadPeriod> toReturn = new ArrayList<LoadPeriod>();
        for(LoadPeriod loadPeriod : list) {
            LocalDate finalStartDate = loadPeriod.getStart();
            LocalDate finalEndDate = loadPeriod.getEnd();
            if(startDateFilter != null) {
                LocalDate startDateFilterLocalDate = new LocalDate(startDateFilter.getTime());
                if(finalStartDate.compareTo(startDateFilterLocalDate) < 0) {
                    finalStartDate = startDateFilterLocalDate;
                }
            }
            if(endDateFilter != null) {
                LocalDate endDateFilterLocalDate = new LocalDate(endDateFilter.getTime());
                if(loadPeriod.getEnd().compareTo(endDateFilterLocalDate) > 0) {
                    finalEndDate = endDateFilterLocalDate;
                }
            }
            if(finalStartDate.compareTo(finalEndDate) < 0) {
                toReturn.add(new LoadPeriod(finalStartDate, finalEndDate,
                        loadPeriod.getTotalResourceWorkHours(),
                        loadPeriod.getAssignedHours(), loadPeriod.getLoadLevel()));
            }
        }
        return toReturn;
    }

    private List<LoadPeriod> buildPeriods() {
        for (ResourceAllocation<?> resourceAllocation : sortedByStartDate) {
            loadPeriodsGenerators.add(factory.create(resourceAllocation));
        }
        joinPeriodGenerators();
        return toGenerators(loadPeriodsGenerators);
    }

    private List<LoadPeriod> toGenerators(List<LoadPeriodGenerator> generators) {
        List<LoadPeriod> result = new ArrayList<LoadPeriod>();
        for (LoadPeriodGenerator loadPeriodGenerator : generators) {
            LoadPeriod period = loadPeriodGenerator.build();
            if (period != null) {
                result.add(period);
            }
        }
        return result;
    }

    private void joinPeriodGenerators() {
        ListIterator<LoadPeriodGenerator> iterator = loadPeriodsGenerators
                .listIterator();
        while (iterator.hasNext()) {
            final LoadPeriodGenerator current = findNextOneOverlapping(iterator);
            if (current != null) {
                rewind(iterator, current);
                iterator.remove();
                LoadPeriodGenerator next = iterator.next();
                iterator.remove();
                List<LoadPeriodGenerator> generated = current.join(next);
                final LoadPeriodGenerator positionToComeBack = generated.get(0);
                final List<LoadPeriodGenerator> remaining = loadPeriodsGenerators
                        .subList(iterator.nextIndex(), loadPeriodsGenerators
                                .size());
                List<LoadPeriodGenerator> generatorsSortedByStartDate = mergeListsKeepingByStartSortOrder(
                        generated, remaining);
                final int takenFromRemaining = generatorsSortedByStartDate
                        .size()
                        - generated.size();
                removeNextElements(iterator, takenFromRemaining);
                addAtCurrentPosition(iterator, generatorsSortedByStartDate);
                rewind(iterator, positionToComeBack);
            }
        }
    }

    private LoadPeriodGenerator findNextOneOverlapping(
            ListIterator<LoadPeriodGenerator> iterator) {
        while (iterator.hasNext()) {
            LoadPeriodGenerator current = iterator.next();
            if (!iterator.hasNext()) {
                return null;
            }
            LoadPeriodGenerator next = peekNext(iterator);
            if (current.overlaps(next)) {
                return current;
            }
        }
        return null;
    }

    private void addAtCurrentPosition(
            ListIterator<LoadPeriodGenerator> iterator,
            List<LoadPeriodGenerator> sortedByStartDate) {
        for (LoadPeriodGenerator l : sortedByStartDate) {
            iterator.add(l);
        }
    }

    private void removeNextElements(ListIterator<LoadPeriodGenerator> iterator,
            final int elementsNumber) {
        for (int i = 0; i < elementsNumber; i++) {
            iterator.next();
            iterator.remove();
        }
    }

    private void rewind(ListIterator<LoadPeriodGenerator> iterator,
            LoadPeriodGenerator nextOne) {
        while (peekNext(iterator) != nextOne) {
            iterator.previous();
        }
    }

    private List<LoadPeriodGenerator> mergeListsKeepingByStartSortOrder(
            List<LoadPeriodGenerator> joined,
            List<LoadPeriodGenerator> remaining) {
        List<LoadPeriodGenerator> result = new ArrayList<LoadPeriodGenerator>();
        ListIterator<LoadPeriodGenerator> joinedIterator = joined
                .listIterator();
        ListIterator<LoadPeriodGenerator> remainingIterator = remaining
                .listIterator();
        while (joinedIterator.hasNext() && remainingIterator.hasNext()) {
            LoadPeriodGenerator fromJoined = peekNext(joinedIterator);
            LoadPeriodGenerator fromRemaining = peekNext(remainingIterator);
            if (fromJoined.getStart().compareTo(fromRemaining.getStart()) <= 0) {
                result.add(fromJoined);
                joinedIterator.next();
            } else {
                result.add(fromRemaining);
                remainingIterator.next();
            }
        }
        if (joinedIterator.hasNext()) {
            result.addAll(joined.subList(joinedIterator.nextIndex(), joined
                    .size()));
        }
        return result;
    }

    private LoadPeriodGenerator peekNext(
            ListIterator<LoadPeriodGenerator> iterator) {
        if (!iterator.hasNext()) {
            return null;
        }
        LoadPeriodGenerator result = iterator.next();
        iterator.previous();
        return result;
    }

}