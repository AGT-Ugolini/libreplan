/*
 * This file is part of NavalPlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
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

package org.navalplanner.web.limitingresources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.navalplanner.business.planner.limiting.entities.LimitingResourceQueueDependency;
import org.navalplanner.business.planner.limiting.entities.LimitingResourceQueueElement;
import org.navalplanner.business.resources.daos.IResourceDAO;
import org.navalplanner.business.resources.entities.LimitingResourceQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.zkoss.ganttz.timetracker.TimeTracker;
import org.zkoss.ganttz.timetracker.TimeTrackerComponent;
import org.zkoss.ganttz.timetracker.TimeTracker.IDetailItemFilter;
import org.zkoss.ganttz.timetracker.zoom.DetailItem;
import org.zkoss.ganttz.timetracker.zoom.IZoomLevelChangedListener;
import org.zkoss.ganttz.timetracker.zoom.ZoomLevel;
import org.zkoss.ganttz.util.ComponentsFinder;
import org.zkoss.ganttz.util.Interval;
import org.zkoss.ganttz.util.MutableTreeModel;
import org.zkoss.ganttz.util.OnZKDesktopRegistry;
import org.zkoss.ganttz.util.script.IScriptsRegister;
import org.zkoss.zk.au.out.AuInvoke;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.HtmlMacroComponent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Separator;
import org.zkoss.zul.SimpleListModel;

public class LimitingResourcesPanel extends HtmlMacroComponent {

    public interface IToolbarCommand {
        public void doAction();

        public String getLabel();

        public String getImage();
    }

    private LimitingResourcesController limitingResourcesController;

    private TimeTrackerComponent timeTrackerComponent;

    private LimitingResourcesLeftPane leftPane;

    private QueueListComponent queueListComponent;

    private MutableTreeModel<LimitingResourceQueue> treeModel;

    private TimeTracker timeTracker;

    private Listbox listZoomLevels;

    private Button paginationDownButton;

    private Button paginationUpButton;

    private Listbox horizontalPagination;

    private Component insertionPointLeftPanel;
    private Component insertionPointRightPanel;
    private Component insertionPointTimetracker;

    public void paginationDown() {
        horizontalPagination.setSelectedIndex(horizontalPagination
                .getSelectedIndex() - 1);
        goToSelectedHorizontalPage();
    }

    public void paginationUp() {
        horizontalPagination.setSelectedIndex(Math.max(1, horizontalPagination
                .getSelectedIndex() + 1));
        goToSelectedHorizontalPage();
    }

    @Autowired
    IResourceDAO resourcesDAO;

    private LimitingDependencyList dependencyList = new LimitingDependencyList(
            this);

    private PaginatorFilter paginatorFilter;

    private TimeTrackerComponent timeTrackerHeader;

    private IZoomLevelChangedListener zoomChangedListener;

    /**
     * Returns the closest upper {@link LimitingResourcesPanel} instance going
     * all the way up from comp
     *
     * @param comp
     * @return
     */
    public static LimitingResourcesPanel getLimitingResourcesPanel(
            Component comp) {
        if (comp == null) {
            return null;
        }
        if (comp instanceof LimitingResourcesPanel) {
            return (LimitingResourcesPanel) comp;
        }
        return getLimitingResourcesPanel(comp.getParent());
    }

    public LimitingResourcesPanel(
            LimitingResourcesController limitingResourcesController,
            TimeTracker timeTracker) {
        init(limitingResourcesController, timeTracker);
    }

    public void init(LimitingResourcesController limitingResourcesController,
            TimeTracker timeTracker) {
        this.limitingResourcesController = limitingResourcesController;
        this.timeTracker = timeTracker;
        this.setVariable("limitingResourcesController",
                limitingResourcesController, true);

        treeModel = createModelForTree();

        timeTrackerComponent = timeTrackerForLimitingResourcesPanel(timeTracker);
        queueListComponent = new QueueListComponent(this, timeTracker,
                treeModel);

        leftPane = new LimitingResourcesLeftPane(treeModel, queueListComponent);
        registerNeededScripts();
    }

    public void appendQueueElementToQueue(LimitingResourceQueueElement element) {
        queueListComponent.appendQueueElement(element);
    }

    private MutableTreeModel<LimitingResourceQueue> createModelForTree() {
        MutableTreeModel<LimitingResourceQueue> result = MutableTreeModel
                .create(LimitingResourceQueue.class);
        for (LimitingResourceQueue LimitingResourceQueue : getLimitingResourceQueues()) {
            result.addToRoot(LimitingResourceQueue);
        }
        return result;
    }

    private List<LimitingResourceQueue> getLimitingResourceQueues() {
        return limitingResourcesController.getLimitingResourceQueues();
    }

    public ListModel getZoomLevels() {
        ZoomLevel[] selectableZoomlevels = { ZoomLevel.DETAIL_THREE,
                ZoomLevel.DETAIL_FOUR, ZoomLevel.DETAIL_FIVE,
                ZoomLevel.DETAIL_SIX };
        return new SimpleListModel(selectableZoomlevels);
    }

    public void setZoomLevel(final ZoomLevel zoomLevel) {
        timeTracker.setZoomLevel(zoomLevel);
    }

    public void zoomIncrease() {
        timeTracker.zoomIncrease();
    }

    public void zoomDecrease() {
        timeTracker.zoomDecrease();
    }

    public void add(final IToolbarCommand... commands) {
        Component toolbar = getToolbar();
        Separator separator = getSeparator();
        for (IToolbarCommand c : commands) {
            toolbar.insertBefore(asButton(c), separator);
        }
    }

    private Button asButton(final IToolbarCommand c) {
        Button result = new Button();
        result.addEventListener(Events.ON_CLICK, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                c.doAction();
            }
        });
        if (!StringUtils.isEmpty(c.getImage())) {
            result.setImage(c.getImage());
            result.setTooltiptext(c.getLabel());
        } else {
            result.setLabel(c.getLabel());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Separator getSeparator() {
        List<Component> children = getToolbar().getChildren();
        Separator separator = ComponentsFinder.findComponentsOfType(
                Separator.class, children).get(0);
        return separator;
    }

    private Component getToolbar() {
        Component toolbar = getFellow("toolbar");
        return toolbar;
    }

    private void registerNeededScripts() {
        // getScriptsRegister().register(
        // ScriptsRequiredByLimitingResourcesPanel.class);
    }

    private IScriptsRegister getScriptsRegister() {
        return OnZKDesktopRegistry.getLocatorFor(IScriptsRegister.class)
                .retrieve();
    }

    private TimeTrackerComponent timeTrackerForLimitingResourcesPanel(
            TimeTracker timeTracker) {
        return new TimeTrackerComponent(timeTracker) {
            @Override
            protected void scrollHorizontalPercentage(int pixelsDisplacement) {
                response("", new AuInvoke(queueListComponent,
                        "adjustScrollHorizontalPosition", pixelsDisplacement
                                + ""));
            }
        };
    }

    @Override
    public void afterCompose() {

        super.afterCompose();
        paginatorFilter = new PaginatorFilter();

        initializeBindings();

        listZoomLevels
                .setSelectedIndex(timeTracker.getDetailLevel().ordinal() - 2);

        // Pagination stuff
        paginationUpButton.setDisabled(paginatorFilter.isLastPage());

        paginatorFilter.setInterval(timeTracker.getRealInterval());
        timeTracker.setFilter(paginatorFilter);

        // Insert leftPane component with limitingresources list
        insertionPointLeftPanel.appendChild(leftPane);
        leftPane.afterCompose();

        insertionPointRightPanel.appendChild(timeTrackerComponent);
        insertionPointRightPanel.appendChild(queueListComponent);
        queueListComponent.afterCompose();

        dependencyList = generateDependencyComponentsList();
        if (dependencyList != null) {
            dependencyList.afterCompose();
            insertionPointRightPanel.appendChild(dependencyList);
        }

        zoomChangedListener = new IZoomLevelChangedListener() {
            @Override
            public void zoomLevelChanged(ZoomLevel newDetailLevel) {
                rebuildDependencies();
                timeTracker.resetMapper();
                paginatorFilter.setInterval(timeTracker.getRealInterval());
                timeTracker.setFilter(paginatorFilter);
                reloadPanelComponents();
                paginatorFilter.populateHorizontalListbox();
                paginatorFilter.goToHorizontalPage(0);
                reloadComponent();

                // Reset mapper for first detail
                if (newDetailLevel == ZoomLevel.DETAIL_THREE) {
                    timeTracker.resetMapper();
                    queueListComponent.invalidate();
                    queueListComponent.afterCompose();
                    rebuildDependencies();
                }
            }

        };
        this.timeTracker.addZoomListener(zoomChangedListener);

        // Insert timetracker headers
        timeTrackerHeader = createTimeTrackerHeader();
        insertionPointTimetracker.appendChild(timeTrackerHeader);
        timeTrackerHeader.afterCompose();
        timeTrackerComponent.afterCompose();

        paginatorFilter.populateHorizontalListbox();
    }

    private void rebuildDependencies() {
        dependencyList.getChildren().clear();
        insertionPointRightPanel.appendChild(dependencyList);
        dependencyList = generateDependencyComponentsList();
        dependencyList.afterCompose();
    }

    private void initializeBindings() {

        // Zoom and pagination
        listZoomLevels = (Listbox) getFellow("listZoomLevels");
        horizontalPagination = (Listbox) getFellow("horizontalPagination");
        paginationUpButton = (Button) getFellow("paginationUpButton");
        paginationDownButton = (Button) getFellow("paginationDownButton");

        insertionPointLeftPanel = getFellow("insertionPointLeftPanel");
        insertionPointRightPanel = getFellow("insertionPointRightPanel");
        insertionPointTimetracker = getFellow("insertionPointTimetracker");
    }

    private void reloadPanelComponents() {

        timeTrackerComponent.getChildren().clear();

        // paginatorFilter.setInterval(timeTracker.getRealInterval());
        // timeTracker.setFilter(paginatorFilter);

        if (timeTrackerHeader != null) {
            timeTrackerHeader.afterCompose();
            timeTrackerComponent.afterCompose();
        }
        dependencyList.invalidate();
    }

    private LimitingDependencyList generateDependencyComponentsList() {
        Map<LimitingResourceQueueElement, QueueTask> queueElementsMap = queueListComponent
                .getLimitingResourceElementToQueueTaskMap();

        for (LimitingResourceQueueElement queueElement : queueElementsMap
                .keySet()) {
            for (LimitingResourceQueueDependency dependency : queueElement
                    .getDependenciesAsOrigin()) {
                addDependencyComponent(dependencyList, queueElementsMap,
                        dependency);
            }
        }
        return dependencyList;
    }

    public void addDependencyComponent(
            LimitingResourceQueueDependency dependency) {
        final Map<LimitingResourceQueueElement, QueueTask> queueElementsMap = queueListComponent
                .getLimitingResourceElementToQueueTaskMap();
        addDependencyComponent(dependencyList, queueElementsMap, dependency);
    }

    private void addDependencyComponent(LimitingDependencyList dependencyList,
            Map<LimitingResourceQueueElement, QueueTask> queueElementsMap,
            LimitingResourceQueueDependency dependency) {

        LimitingDependencyComponent component = createDependencyComponent(
                queueElementsMap, dependency);
        if (component != null) {
            dependencyList.addDependencyComponent(component);
        }
    }

    private LimitingDependencyComponent createDependencyComponent(
            Map<LimitingResourceQueueElement, QueueTask> queueElementsMap,
            LimitingResourceQueueDependency dependency) {

        final QueueTask origin = queueElementsMap.get(dependency
                .getHasAsOrigin());
        final QueueTask destination = queueElementsMap.get(dependency
                .getHasAsDestiny());
        return (origin != null && destination != null) ? new LimitingDependencyComponent(
                origin, destination)
                : null;
    }

    public void clearComponents() {
        getFellow("insertionPointLeftPanel").getChildren().clear();
        getFellow("insertionPointRightPanel").getChildren().clear();
        getFellow("insertionPointTimetracker").getChildren().clear();
    }

    public TimeTrackerComponent getTimeTrackerComponent() {
        return timeTrackerComponent;
    }

    private TimeTrackerComponent createTimeTrackerHeader() {
        return new TimeTrackerComponent(timeTracker) {

            @Override
            protected void scrollHorizontalPercentage(int pixelsDisplacement) {
            }
        };
    }

    public void unschedule(QueueTask task) {
        limitingResourcesController.unschedule(task);
        removeQueueTask(task);
    }

    private void removeQueueTask(QueueTask task) {
        task.detach();
        dependencyList.removeDependencyComponents(task);
    }

    public void moveQueueTask(QueueTask queueTask) {
        if (limitingResourcesController.moveTask(queueTask.getLimitingResourceQueueElement())) {
            removeQueueTask(queueTask);
        }
    }

    public void refreshQueue(LimitingResourceQueue queue) {
        queueListComponent.refreshQueue(queue);
    }

    public void goToSelectedHorizontalPage() {
        paginatorFilter.goToHorizontalPage(horizontalPagination
                .getSelectedIndex());
        reloadComponent();
    }

    public void reloadComponent() {
        timeTrackerHeader.recreate();
        timeTrackerComponent.recreate();
        queueListComponent.invalidate();
        queueListComponent.afterCompose();
        rebuildDependencies();
    }

    private class PaginatorFilter implements IDetailItemFilter {

        private DateTime intervalStart;
        private DateTime intervalEnd;

        private DateTime paginatorStart;
        private DateTime paginatorEnd;

        @Override
        public Interval getCurrentPaginationInterval() {
            return new Interval(paginatorStart.toDate(), paginatorEnd.toDate());
        }

        private Period intervalIncrease() {
            switch (timeTracker.getDetailLevel()) {
            case DETAIL_ONE:
                return Period.years(5);
            case DETAIL_TWO:
                return Period.years(5);
            case DETAIL_THREE:
                return Period.years(2);
            case DETAIL_FOUR:
                return Period.months(12);
            case DETAIL_FIVE:
                return Period.weeks(12);
            case DETAIL_SIX:
                return Period.weeks(12);
            }
            // Default month
            return Period.years(2);
        }

        public void setInterval(Interval realInterval) {
            intervalStart = new DateTime(realInterval.getStart());
            intervalEnd = new DateTime(realInterval.getFinish());
            paginatorStart = intervalStart;
            paginatorEnd = intervalStart.plus(intervalIncrease());
            if ((paginatorEnd.plus(intervalIncrease()).isAfter(intervalEnd))) {
                paginatorEnd = intervalEnd;
            }
            updatePaginationButtons();
        }

        @Override
        public void resetInterval() {
            setInterval(timeTracker.getRealInterval());
        }

        public void paginationDown() {
            paginatorFilter.goToHorizontalPage(horizontalPagination
                    .getSelectedIndex() - 1);
            reloadComponent();
        }

        public void paginationUp() {
            paginatorFilter.goToHorizontalPage(horizontalPagination
                    .getSelectedIndex() + 1);
            reloadComponent();
        }

        @Override
        public Collection<DetailItem> selectsFirstLevel(
                Collection<DetailItem> firstLevelDetails) {
            ArrayList<DetailItem> result = new ArrayList<DetailItem>();
            for (DetailItem each : firstLevelDetails) {
                if ((each.getStartDate() == null)
                        || !(each.getStartDate().isBefore(paginatorStart))
                        && (each.getStartDate().isBefore(paginatorEnd))) {
                    result.add(each);
                }
            }
            return result;
        }

        @Override
        public Collection<DetailItem> selectsSecondLevel(
                Collection<DetailItem> secondLevelDetails) {
            ArrayList<DetailItem> result = new ArrayList<DetailItem>();
            for (DetailItem each : secondLevelDetails) {
                if ((each.getStartDate() == null)
                        || !(each.getStartDate().isBefore(paginatorStart))
                        && (each.getStartDate().isBefore(paginatorEnd))) {
                    result.add(each);
                }
            }
            return result;
        }

        public void populateHorizontalListbox() {
            horizontalPagination.getItems().clear();
            DateTimeFormatter df = DateTimeFormat.forPattern("dd/MMM/yyyy");
            DateTime intervalStart = new DateTime(timeTracker.getRealInterval()
                    .getStart());
            if (intervalStart != null) {
                DateTime itemStart = intervalStart;
                DateTime itemEnd = intervalStart.plus(intervalIncrease());
                while (intervalEnd.isAfter(itemStart)) {
                    if (intervalEnd.isBefore(itemEnd)
                            || !intervalEnd.isAfter(itemEnd
                                    .plus(intervalIncrease()))) {
                        itemEnd = intervalEnd;
                    }
                    Listitem item = new Listitem(df.print(itemStart) + " - "
                            + df.print(itemEnd.minusDays(1)));
                    horizontalPagination.appendChild(item);
                    itemStart = itemEnd;
                    itemEnd = itemEnd.plus(intervalIncrease());
                }
            }
            horizontalPagination.setDisabled(horizontalPagination.getItems()
                    .size() < 2);
        }

        public void goToHorizontalPage(int interval) {
            paginatorStart = intervalStart;
            // paginatorStart = new
            // DateTime(timeTracker.getRealInterval().getStart());
            paginatorStart = timeTracker.getDetailsFirstLevel().iterator()
                    .next().getStartDate();

            for (int i = 0; i < interval; i++) {
                paginatorStart = paginatorStart.plus(intervalIncrease());
            }
            paginatorEnd = paginatorStart.plus(intervalIncrease());
            if ((paginatorEnd.plus(intervalIncrease()).isAfter(intervalEnd))) {
                paginatorEnd = paginatorEnd.plus(intervalIncrease());
            }
            timeTracker.resetMapper();
            updatePaginationButtons();
        }

        private void updatePaginationButtons() {
            paginationDownButton.setDisabled(isFirstPage());
            paginationUpButton.setDisabled(isLastPage());
        }

        public void previous() {
            paginatorStart = paginatorStart.minus(intervalIncrease());
            paginatorEnd = paginatorEnd.minus(intervalIncrease());
            updatePaginationButtons();
        }

        public boolean isFirstPage() {
            return (horizontalPagination.getSelectedIndex() <= 0)
                    || horizontalPagination.isDisabled();
        }

        private boolean isLastPage() {
            return (horizontalPagination.getItemCount() == (horizontalPagination
                    .getSelectedIndex() + 1))
                    || horizontalPagination.isDisabled();
        }
    }
}
