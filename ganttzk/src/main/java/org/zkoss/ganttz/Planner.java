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

package org.zkoss.ganttz;

import static org.zkoss.ganttz.i18n.I18nHelper._;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.zkoss.ganttz.adapters.IDisabilityConfiguration;
import org.zkoss.ganttz.adapters.PlannerConfiguration;
import org.zkoss.ganttz.data.Dependency;
import org.zkoss.ganttz.data.GanttDiagramGraph;
import org.zkoss.ganttz.data.Position;
import org.zkoss.ganttz.data.Task;
import org.zkoss.ganttz.data.GanttDiagramGraph.GanttZKDiagramGraph;
import org.zkoss.ganttz.data.GanttDiagramGraph.IGraphChangeListener;
import org.zkoss.ganttz.extensions.ICommand;
import org.zkoss.ganttz.extensions.ICommandOnTask;
import org.zkoss.ganttz.extensions.IContext;
import org.zkoss.ganttz.timetracker.TimeTracker;
import org.zkoss.ganttz.timetracker.TimeTrackerComponent;
import org.zkoss.ganttz.timetracker.TimeTrackerComponentWithoutColumns;
import org.zkoss.ganttz.timetracker.zoom.ZoomLevel;
import org.zkoss.ganttz.util.ComponentsFinder;
import org.zkoss.ganttz.util.LongOperationFeedback;
import org.zkoss.ganttz.util.OnZKDesktopRegistry;
import org.zkoss.ganttz.util.WeakReferencedListeners;
import org.zkoss.ganttz.util.LongOperationFeedback.ILongOperation;
import org.zkoss.ganttz.util.WeakReferencedListeners.IListenerNotification;
import org.zkoss.ganttz.util.script.IScriptsRegister;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.HtmlMacroComponent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkex.zul.api.South;
import org.zkoss.zul.Button;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Separator;
import org.zkoss.zul.SimpleListModel;

public class Planner extends HtmlMacroComponent  {

    public static void registerNeededScripts() {
        IScriptsRegister register = getScriptsRegister();
        register.register(ScriptsRequiredByPlanner.class);
    }

    private static IScriptsRegister getScriptsRegister() {
        return OnZKDesktopRegistry.getLocatorFor(IScriptsRegister.class)
                .retrieve();
    }

    public static boolean guessContainersExpandedByDefaultGivenPrintParameters(
            Map<String, String> printParameters) {
        return guessContainersExpandedByDefault(convertToURLParameters(printParameters));
    }

    private static Map<String, String[]> convertToURLParameters(
            Map<String, String> printParameters) {
        Map<String, String[]> result = new HashMap<String, String[]>();
        for (Entry<String, String> each : printParameters.entrySet()) {
            result.put(each.getKey(), new String[] { each.getValue() });
        }
        return result;
    }

    public static boolean guessContainersExpandedByDefault(
            Map<String, String[]> queryURLParameters) {
        String[] values = queryURLParameters.get("expanded");
        if (values == null) {
            return false;
        }
        return toLowercaseSet(values).contains("all");
    }

    private static Set<String> toLowercaseSet(String[] values) {
        Set<String> result = new HashSet<String>();
        for (String each : values) {
            result.add(each.toLowerCase());
        }
        return result;
    }

    private GanttZKDiagramGraph diagramGraph;

    private LeftPane leftPane;

    private GanttPanel ganttPanel;

    private boolean fixedZoomByUser = false;

    private List<? extends CommandContextualized<?>> contextualizedGlobalCommands;

    private CommandContextualized<?> goingDownInLastArrowCommand;

    private List<? extends CommandOnTaskContextualized<?>> commandsOnTasksContextualized;

    private CommandOnTaskContextualized<?> doubleClickCommand;

    private FunctionalityExposedForExtensions<?> context;

    private transient IDisabilityConfiguration disabilityConfiguration;

    private boolean isShowingCriticalPath = false;

    private boolean isShowingLabels = false;

    private boolean isShowingResources = false;

    private ZoomLevel initialZoomLevel = null;

    private Listbox listZoomLevels = null;

    private WeakReferencedListeners<IChartVisibilityChangedListener> chartVisibilityListeners = WeakReferencedListeners
            .create();

    public Planner() {
        registerNeededScripts();
    }

    TaskList getTaskList() {
        if (ganttPanel == null) {
            return null;
        }
        List<Object> children = ganttPanel.getChildren();
        return ComponentsFinder.findComponentsOfType(TaskList.class, children).get(0);
    }

    public int getTaskNumber() {
        return getTaskList().getTasksNumber();
    }

    public int getAllTasksNumber() {
        return diagramGraph.getTasks().size();
    }

    public String getContextPath() {
        return Executions.getCurrent().getContextPath();
    }

    public DependencyList getDependencyList() {
        if (ganttPanel == null) {
            return null;
        }
        List<Object> children = ganttPanel.getChildren();
        List<DependencyList> found = ComponentsFinder.findComponentsOfType(DependencyList.class,
                children);
        if (found.isEmpty()) {
            return null;
        }
        return found.get(0);
    }

    public void addTasks(Position position, Collection<? extends Task> newTasks) {
        TaskList taskList = getTaskList();
        if (taskList != null && leftPane != null) {
            taskList.addTasks(position, newTasks);
            leftPane.addTasks(position, newTasks);
        }
    }

    public void addTask(Position position, Task task) {
        addTasks(position, Arrays.asList(task));
    }

    void addDependencies(Collection<? extends Dependency> dependencies) {
        DependencyList dependencyList = getDependencyList();
        if (dependencyList == null) {
            return;
        }
        for (DependencyComponent d : getTaskList().asDependencyComponents(
                dependencies)) {
            dependencyList.addDependencyComponent(d);
        }
    }

    public ListModel getZoomLevels() {
        return new SimpleListModel(ZoomLevel.values());
    }

    public void setZoomLevel(final ZoomLevel zoomLevel) {
        if (ganttPanel == null) {
            return;
        }
        this.fixedZoomByUser = true;
        initialZoomLevel = zoomLevel;
        ganttPanel.setZoomLevel(zoomLevel);
    }

    public void zoomIncrease() {
        if (ganttPanel == null) {
            return;
        }
        LongOperationFeedback.execute(ganttPanel, new ILongOperation() {

            @Override
            public String getName() {
                return _("increasing zoom");
            }

            @Override
            public void doAction() throws Exception {
                ganttPanel.zoomIncrease();
            }
        });
    }

    public void zoomDecrease() {
        if (ganttPanel == null) {
            return;
        }
        LongOperationFeedback.execute(ganttPanel, new ILongOperation() {
            @Override
            public String getName() {
                return _("decreasing zoom");
            }

            @Override
            public void doAction() throws Exception {
                ganttPanel.zoomDecrease();
            }
        });
    }

    public <T> void setConfiguration(PlannerConfiguration<T> configuration) {
        if (configuration == null) {
            return;
        }

        this.diagramGraph = GanttDiagramGraph.create(configuration
                .getStartConstraints(), configuration.getEndConstraints(), configuration.isDependenciesConstraintsHavePriority());
        FunctionalityExposedForExtensions<T> newContext = new FunctionalityExposedForExtensions<T>(
                this, configuration, diagramGraph);
        diagramGraph.addPreChangeListeners(configuration
                .getPreChangeListeners());
        diagramGraph.addPostChangeListeners(configuration
                .getPostChangeListeners());
        this.contextualizedGlobalCommands = contextualize(newContext,
                configuration.getGlobalCommands());
        this.commandsOnTasksContextualized = contextualize(newContext,
                configuration.getCommandsOnTasks());
        goingDownInLastArrowCommand = contextualize(newContext, configuration
                .getGoingDownInLastArrowCommand());
        doubleClickCommand = contextualize(newContext, configuration
                .getDoubleClickCommand());
        this.context = newContext;
        this.disabilityConfiguration = configuration;
        resettingPreviousComponentsToNull();
        newContext.add(configuration.getData());
        setupComponents();

        setAt("insertionPointLeftPanel", leftPane);
        leftPane.afterCompose();
        setAt("insertionPointRightPanel", ganttPanel);
        ganttPanel.afterCompose();
        leftPane.setGoingDownInLastArrowCommand(goingDownInLastArrowCommand);

        TimeTrackerComponent timetrackerheader = new TimeTrackerComponentWithoutColumns(
                ganttPanel.getTimeTracker(), "timetrackerheader");

        setAt("insertionPointTimetracker", timetrackerheader);
        timetrackerheader.afterCompose();

        Component chartComponent = configuration.getChartComponent();
        if (chartComponent != null) {
            setAt("insertionPointChart", chartComponent);
        }

        if (!configuration.isCriticalPathEnabled()) {
            Button showCriticalPathButton = (Button) getFellow("showCriticalPath");
            showCriticalPathButton.setVisible(false);
        }
        if (!configuration.isExpandAllEnabled()) {
            Button expandAllButton = (Button) getFellow("expandAll");
            expandAllButton.setVisible(false);
        }
        if (!configuration.isFlattenTreeEnabled()) {
            Button flattenTree = (Button) getFellow("flattenTree");
            flattenTree.setVisible(false);
        }
        listZoomLevels.setSelectedIndex(getZoomLevel().ordinal());

        this.visibleChart = configuration.isExpandPlanningViewCharts();
        ((South) getFellow("graphics")).setOpen(this.visibleChart);
    }

    private void resettingPreviousComponentsToNull() {
        this.ganttPanel = null;
        this.leftPane = null;
    }

    private void setAt(String insertionPointId, Component component) {
        Component insertionPoint = getFellow(insertionPointId);
        insertionPoint.getChildren().clear();
        insertionPoint.appendChild(component);
    }

    private <T> List<CommandOnTaskContextualized<T>> contextualize(
            FunctionalityExposedForExtensions<T> context,
            List<ICommandOnTask<T>> commands) {
        List<CommandOnTaskContextualized<T>> result = new ArrayList<CommandOnTaskContextualized<T>>();
        for (ICommandOnTask<T> c : commands) {
            result.add(contextualize(context, c));
        }
        return result;
    }

    private <T> CommandOnTaskContextualized<T> contextualize(
            FunctionalityExposedForExtensions<T> context,
            ICommandOnTask<T> commandOnTask) {
        return CommandOnTaskContextualized.create(commandOnTask, context
                .getMapper(), context);
    }

    private <T> CommandContextualized<T> contextualize(IContext<T> context,
            ICommand<T> command) {
        if (command == null) {
            return null;
        }
        return CommandContextualized.create(command, context);
    }

    private <T> List<CommandContextualized<T>> contextualize(
            IContext<T> context, Collection<? extends ICommand<T>> commands) {
        ArrayList<CommandContextualized<T>> result = new ArrayList<CommandContextualized<T>>();
        for (ICommand<T> command : commands) {
            result.add(contextualize(context, command));
        }
        return result;
    }

    private void setupComponents() {
        insertGlobalCommands();

        predicate = new FilterAndParentExpandedPredicates(context) {

            @Override
            public boolean accpetsFilterPredicate(Task task) {
                return true;
            }
        };
        this.leftPane = new LeftPane(disabilityConfiguration, this.diagramGraph
                .getTopLevelTasks(), predicate);
        this.ganttPanel = new GanttPanel(this.context,
                commandsOnTasksContextualized, doubleClickCommand,
                disabilityConfiguration, predicate);

        Button button = (Button) getFellow("btnPrint");
        button.setDisabled(!context.isPrintEnabled());
    }

    @SuppressWarnings("unchecked")
    private void insertGlobalCommands() {
        Component toolbar = getToolbar();
        Component firstSeparator = getFirstSeparatorFromToolbar();
        toolbar.getChildren().removeAll(getBefore(toolbar, firstSeparator));
        for (CommandContextualized<?> c : contextualizedGlobalCommands) {
            toolbar.insertBefore(c.toButton(), firstSeparator);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Component> getBefore(Component parent, Component child) {
        List<Component> children = parent.getChildren();
        List<Component> result = new ArrayList<Component>();
        for (Component object : children) {
            if (object == child) {
                break;
            }
            result.add(object);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Component getFirstSeparatorFromToolbar() {
        Component toolbar = getToolbar();
        List<Component> children = toolbar.getChildren();
        List<Separator> separators = ComponentsFinder
                .findComponentsOfType(
                Separator.class, children);
        return separators.get(0);
    }

    private Component getToolbar() {
        Component toolbar = getFellow("toolbar");
        return toolbar;
    }

    void removeTask(Task task) {
        TaskList taskList = getTaskList();
        taskList.remove(task);
        getDependencyList().taskRemoved(task);
        leftPane.taskRemoved(task);
        setHeight(getHeight());// forcing smart update
        taskList.adjustZoomColumnsHeight();
        getDependencyList().redrawDependencies();
    }

    @Override
    public void afterCompose() {
        super.afterCompose();
        listZoomLevels = (Listbox) getFellow("listZoomLevels");

        Component westContainer = getFellow("taskdetailsContainer");
        westContainer.addEventListener(Events.ON_SIZE, new EventListener() {

            @Override
            public void onEvent(Event event) throws Exception {
                Clients.evalJavaScript("zkTaskContainer.legendResize();");

            }

        });

    }

    public TimeTracker getTimeTracker() {
        return ganttPanel.getTimeTracker();
    }

    private IGraphChangeListener showCriticalPathOnChange = new IGraphChangeListener() {

        @Override
        public void execute() {
            context.showCriticalPath();
        }
    };

    private boolean containersExpandedByDefault = false;

    private FilterAndParentExpandedPredicates predicate;

    private boolean visibleChart;

    public void showCriticalPath() {
        Button showCriticalPathButton = (Button) getFellow("showCriticalPath");
        if (disabilityConfiguration.isCriticalPathEnabled()) {
            if (isShowingCriticalPath) {
                context.hideCriticalPath();
                diagramGraph.removePostGraphChangeListener(showCriticalPathOnChange);
                showCriticalPathButton.setSclass("planner-command");
                showCriticalPathButton.setTooltiptext(_("Show critical path"));
            } else {
                context.showCriticalPath();
                diagramGraph.addPostGraphChangeListener(showCriticalPathOnChange);
                showCriticalPathButton.setSclass("planner-command clicked");
                showCriticalPathButton.setTooltiptext(_("Hide critical path"));
            }
            isShowingCriticalPath = !isShowingCriticalPath;
        }
    }

    public void showAllLabels() {
        Button showAllLabelsButton = (Button) getFellow("showAllLabels");
        if (isShowingLabels) {
            Clients.evalJavaScript("zkTasklist.hideAllTooltips();");
            showAllLabelsButton.setSclass("planner-command show-labels");
        } else {
            Clients.evalJavaScript("zkTasklist.showAllTooltips();");
            showAllLabelsButton
                    .setSclass("planner-command show-labels clicked");
        }
        isShowingLabels = !isShowingLabels;
    }

    public void showAllResources() {
        Button showAllLabelsButton = (Button) getFellow("showAllResources");
        if (isShowingResources) {
            Clients.evalJavaScript("zkTasklist.hideResourceTooltips();");
            showAllLabelsButton.setSclass("planner-command show-resources");
        } else {
            Clients.evalJavaScript("zkTasklist.showResourceTooltips();");
            showAllLabelsButton
            .setSclass("planner-command show-resources clicked");
        }
        isShowingResources = !isShowingResources;
    }

    public void print() {
        // Pending to raise print configuration popup. Information retrieved
        // should be passed as parameter to context print method
        context.print();
    }

    public ZoomLevel getZoomLevel() {
        if (ganttPanel == null) {
            return initialZoomLevel != null ? initialZoomLevel
                    : ZoomLevel.DETAIL_ONE;
        }
        return ganttPanel.getTimeTracker().getDetailLevel();
    }

    public boolean isFixedZoomByUser() {
        return this.fixedZoomByUser;
    }

    public void setInitialZoomLevel(final ZoomLevel zoomLevel) {
        this.initialZoomLevel = zoomLevel;
    }

    public boolean areContainersExpandedByDefault() {
        return containersExpandedByDefault;
    }

    public void setAreContainersExpandedByDefault(
            boolean containersExpandedByDefault) {
        this.containersExpandedByDefault = containersExpandedByDefault;
    }

    public void expandAll() {
        Button expandAllButton = (Button) getFellow("expandAll");
        if (disabilityConfiguration.isExpandAllEnabled()) {
            if (expandAllButton.getSclass().equals("planner-command")) {
                context.expandAll();
                expandAllButton.setSclass("planner-command clicked");
            } else {
                context.collapseAll();
                expandAllButton.setSclass("planner-command");
            }
        }
    }

    public void expandAllAlways() {
        Button expandAllButton = (Button) getFellow("expandAll");
        if (disabilityConfiguration.isExpandAllEnabled()) {
                context.expandAll();
                expandAllButton.setSclass("planner-command clicked");
        }
    }

    public void updateSelectedZoomLevel() {
        if (!isFixedZoomByUser()) {
            Listitem selectedItem = (Listitem) listZoomLevels.getItems().get(
                    initialZoomLevel.ordinal());
            listZoomLevels.setSelectedItem(selectedItem);
            listZoomLevels.invalidate();
        }
    }

    public IContext<?> getContext() {
        return context;
    }

    public void setTaskListPredicate(FilterAndParentExpandedPredicates predicate) {
        this.predicate = predicate;
        leftPane.setPredicate(predicate);
        getTaskList().setPredicate(predicate);
        getDependencyList().redrawDependencies();

        if (isShowingLabels) {
            Clients.evalJavaScript("zkTasklist.showAllTooltips();");
        }

        if (isShowingResources) {
            Clients.evalJavaScript("zkTasklist.showResourceTooltips();");
        }
    }

    public void flattenTree() {
        Button flattenTreeButton = (Button) getFellow("flattenTree");
        if (disabilityConfiguration.isFlattenTreeEnabled()) {
            if (flattenTreeButton.getSclass().equals("planner-command")) {
                predicate.setFilterContainers(true);
                flattenTreeButton.setSclass("planner-command clicked");
            } else {
                predicate.setFilterContainers(false);
                flattenTreeButton.setSclass("planner-command");
            }
            setTaskListPredicate(predicate);
        }
    }

    public FilterAndParentExpandedPredicates getPredicate() {
        return predicate;
    }

    public void changeChartVisibility(boolean visible) {
        visibleChart = visible;
        chartVisibilityListeners
                .fireEvent(new IListenerNotification<IChartVisibilityChangedListener>() {
                    @Override
                    public void doNotify(
                            IChartVisibilityChangedListener listener) {
                        listener.chartVisibilityChanged(visibleChart);
                    }
                });
    }

    public boolean isVisibleChart() {
        return visibleChart;
    }

    public void addChartVisibilityListener(
            IChartVisibilityChangedListener chartVisibilityChangedListener) {
        chartVisibilityListeners.addListener(chartVisibilityChangedListener);
    }

}
