/*
 * This file is part of LibrePlan
 *
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

package org.libreplan.web.dashboard;

import static org.libreplan.web.I18nHelper._;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.libreplan.business.orders.entities.Order;
import org.libreplan.business.planner.entities.TaskStatusEnum;
import org.libreplan.web.dashboard.DashboardModel.Interval;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;

import br.com.digilabs.jqplot.Chart;
import br.com.digilabs.jqplot.JqPlotUtils;
import br.com.digilabs.jqplot.chart.BarChart;
import br.com.digilabs.jqplot.chart.PieChart;
import br.com.digilabs.jqplot.elements.Serie;

/**
 * @author Nacho Barrientos <nacho@igalia.com>
 * @author Diego Pino García <dpino@igalia.com>
 *
 *         Controller for dashboardfororder view
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DashboardController extends GenericForwardComposer {

    private IDashboardModel dashboardModel;

    private Grid gridTasksSummary;
    private Label lblOvertimeRatio;
    private Label lblAvailabilityRatio;
    private Label lblAbsolute;

    private org.zkoss.zk.ui.Component costStatus;

    private Div projectDashboardChartsDiv;
    private Div projectDashboardNoTasksWarningDiv;

    public DashboardController() {
    }

    @Override
    public void doAfterCompose(org.zkoss.zk.ui.Component comp) throws Exception {
        super.doAfterCompose(comp);
    }

    public void setCurrentOrder(Order order) {
        dashboardModel.setCurrentOrder(order);
        if (dashboardModel.tasksAvailable()) {
            if (self != null) {
                renderGlobalProgress();
                renderTaskStatus();
                renderTaskCompletationLag();
                renderDeadlineViolation();
                renderMarginWithDeadline();
                renderEstimationAccuracy();
                renderCostStatus(order);
                renderOvertimeRatio();
                renderAvailabilityRatio();
            }
            showCharts();
        } else {
            hideCharts();
        }
    }

    private void renderOvertimeRatio() {
        lblOvertimeRatio.setValue(String.format("%.2f", dashboardModel
                .getOvertimeRatio().doubleValue()));
    }

    private void renderAvailabilityRatio() {
        lblAvailabilityRatio.setValue(String.format("%.2f", dashboardModel
                .getAvailabilityRatio().doubleValue()));
    }

    private void renderCostStatus(Order order) {
        CostStatusController costStatusController = getCostStatusController();
        costStatusController.setOrder(order);
        costStatusController.render();
    }

    private CostStatusController getCostStatusController() {
        return (CostStatusController) costStatus.getAttribute("controller");
    }

    private void renderMarginWithDeadline() {

        Integer absoluteMargin = dashboardModel.getAbsoluteMarginWithDeadLine();
        BigDecimal relativeMargin = dashboardModel.getMarginWithDeadLine();

        if ((lblAbsolute != null) && (absoluteMargin != null)) {
            lblAbsolute
                    .setValue(String
                            .format(_("There is a margin of %d days with the project global deadline (%.2f %%)."),
                                    absoluteMargin + 0,
                                    relativeMargin.doubleValue() * 100));
        } else {
            lblAbsolute
                    .setValue(_("It has not been defined a project deadline"));
        }

    }

    private void renderDeadlineViolation() {
        final String divId = "deadline-violation";

        PieChart<Number> pieChart = new PieChart<Number>(
                _("Task deadline violations"));
        pieChart.addValue(_("On schedule"),
                dashboardModel.getPercentageOfOnScheduleTasks());
        pieChart.addValue(_("Violated deadline"),
                dashboardModel.getPercentageOfTasksWithViolatedDeadline());
        pieChart.addValue(_("No deadline"),
                dashboardModel.getPercentageOfTasksWithNoDeadline());
        renderChart(pieChart, divId);
    }

    private void renderChart(Chart<?> chart, String divId) {
        String jsCode = JqPlotUtils.createJquery(chart, divId);
        Clients.evalJavaScript(jsCode);
    }

    private void renderTaskCompletationLag() {
        final String divId = "task-completation-lag";

        BarChart<Integer> barChart;
        barChart = new BarChart<Integer>("Task Completation Lead/Lag");

        barChart.setFillZero(true);
        barChart.setHighlightMouseDown(true);
        barChart.setStackSeries(false);
        barChart.setBarMargin(30);

        barChart.addSeries(new Serie("Tasks"));

        TaskCompletationData taskCompletationData = TaskCompletationData
                .create(dashboardModel);
        barChart.setTicks(taskCompletationData.getTicks());
        barChart.addValues(taskCompletationData.getValues());

        barChart.getAxes().getXaxis()
                .setLabel(_("Number of Days / Days Interval"));

        renderChart(barChart, divId);
    }

    private void renderEstimationAccuracy() {
        final String divId = "estimation-accuracy";

        BarChart<Integer> barChart;
        barChart = new BarChart<Integer>("Estimation Accuracy");

        barChart.setFillZero(true);
        barChart.setHighlightMouseDown(true);
        barChart.setStackSeries(false);
        barChart.setBarMargin(30);

        barChart.addSeries(new Serie("Tasks"));

        EstimationAccuracy estimationAccuracyData = EstimationAccuracy
                .create(dashboardModel);
        barChart.setTicks(estimationAccuracyData.getTicks());
        barChart.addValues(estimationAccuracyData.getValues());

        barChart.getAxes().getXaxis()
                .setLabel(_("Number of Tasks / % Deviation"));

        renderChart(barChart, divId);
    }

    private String statusLegend(TaskStatusEnum status,
            Map<TaskStatusEnum, Integer> taskStatus) {
        return status + String.format(_(" (%d tasks)"), taskStatus.get(status));
    }

    private void renderTaskStatus() {
        final String divId = "task-status";

        Map<TaskStatusEnum, Integer> taskStatus = dashboardModel
                .calculateTaskStatus();
        PieChart<Number> taskStatusPieChart = new PieChart<Number>(
                _("Task Status"));

        taskStatusPieChart.addValue(
                statusLegend(TaskStatusEnum.FINISHED, taskStatus),
                dashboardModel.getPercentageOfFinishedTasks());
        taskStatusPieChart.addValue(
                statusLegend(TaskStatusEnum.IN_PROGRESS, taskStatus),
                dashboardModel.getPercentageOfInProgressTasks());
        taskStatusPieChart.addValue(
                statusLegend(TaskStatusEnum.READY_TO_START, taskStatus),
                dashboardModel.getPercentageOfReadyToStartTasks());
        taskStatusPieChart.addValue(
                statusLegend(TaskStatusEnum.BLOCKED, taskStatus),
                dashboardModel.getPercentageOfBlockedTasks());

        renderChart(taskStatusPieChart, divId);
    }

    private void renderGlobalProgress() {
        GlobalProgress globalProgress = GlobalProgress.create();

        // Current values
        globalProgress.current(GlobalProgress.CRITICAL_PATH_DURATION,
                dashboardModel.getCriticalPathProgressByDuration());
        globalProgress.current(GlobalProgress.CRITICAL_PATH_HOURS,
                dashboardModel.getCriticalPathProgressByNumHours());
        globalProgress.current(GlobalProgress.ALL_TASKS_HOURS,
                dashboardModel.getAdvancePercentageByHours());
        // Expected values
        globalProgress.expected(GlobalProgress.CRITICAL_PATH_DURATION,
                dashboardModel.getExpectedCriticalPathProgressByDuration());
        globalProgress.expected(GlobalProgress.CRITICAL_PATH_HOURS,
                dashboardModel.getExpectedCriticalPathProgressByNumHours());
        globalProgress.expected(GlobalProgress.ALL_TASKS_HOURS,
                dashboardModel.getExpectedAdvancePercentageByHours());

        globalProgress.render();
    }

    private void showCharts() {
        projectDashboardChartsDiv.setVisible(true);
        projectDashboardNoTasksWarningDiv.setVisible(false);
    }

    private void hideCharts() {
        projectDashboardChartsDiv.setVisible(false);
        projectDashboardNoTasksWarningDiv.setVisible(true);
    }

    /**
     *
     * @author Diego Pino García <dpino@igalia.com>
     *
     */
    static class GlobalProgress {

        public static final String ALL_TASKS_HOURS = _("All tasks (hours)");

        public static final String CRITICAL_PATH_HOURS = _("Critical path (hours)");

        public static final String CRITICAL_PATH_DURATION = _("Critical path (duration)");

        private final Map<String, BigDecimal> current = new LinkedHashMap<String, BigDecimal>();

        private final Map<String, BigDecimal> expected = new LinkedHashMap<String, BigDecimal>();

        private static List<Series> series = new ArrayList<Series>() {
            {
                add(Series.create(_("Current"), "#004469"));
                add(Series.create(_("Expected"), "#3C90BE"));
            }
        };

        private GlobalProgress() {

        }

        public void current(String key, BigDecimal value) {
            current.put(key, value);
        }

        public void expected(String key, BigDecimal value) {
            expected.put(key, value);
        }

        public static GlobalProgress create() {
            return new GlobalProgress();
        }

        public String getPercentages() {
            return String.format("'[%s, %s]'",
                    jsonifyPercentages(current.values()),
                    jsonifyPercentages(expected.values()));
        }

        private String jsonifyPercentages(Collection<BigDecimal> array) {
            List<String> result = new ArrayList<String>();

            int i = 1;
            for (BigDecimal each : array) {
                result.add(String.format("[%.2f, %d]", each.doubleValue(), i++));
            }
            return String.format("[%s]", StringUtils.join(result, ","));
        }

        private String jsonify(Collection<?> list) {
            Collection<String> result = new ArrayList<String>();
            for (Object each : list) {
                if (each.getClass() == String.class) {
                    result.add(String.format("\"%s\"", each.toString()));
                } else {
                    result.add(String.format("%s", each.toString()));
                }
            }
            return String.format("'[%s]'", StringUtils.join(result, ','));
        }

        public String getSeries() {
            return jsonify(series);
        }

        /**
         * The order of the ticks is taken from the keys in current
         *
         * @return
         */
        public String getTicks() {
            return jsonify(current.keySet());
        }

        public void render() {
            String command = String.format(
                    "global_progress.render(%s, %s, %s);", getPercentages(),
                    getTicks(), getSeries());
            Clients.evalJavaScript(command);
        }

    }

    /**
     *
     * @author Diego Pino García <dpino@igalia.com>
     *
     */
    static class Series {

        private String label;

        private String color;

        private Series() {

        }

        public static Series create(String label) {
            Series series = new Series();
            series.label = label;
            return series;
        }

        public static Series create(String label, String color) {
            Series series = new Series();
            series.label = label;
            series.color = color;
            return series;
        }

        @Override
        public String toString() {
            return String.format("{\"label\": \"%s\", \"color\": \"%s\"}",
                    label, color);
        }

    }

    /**
     *
     * @author Diego Pino García<dpino@igalia.com>
     *
     */
    static class TaskCompletationData {

        private final IDashboardModel dashboardModel;

        private Map<Interval, Integer> taskCompletationData;

        private TaskCompletationData(IDashboardModel dashboardModel) {
            this.dashboardModel = dashboardModel;
        }

        public static TaskCompletationData create(IDashboardModel dashboardModel) {
            return new TaskCompletationData(dashboardModel);
        }

        private Map<Interval, Integer> getData() {
            if (taskCompletationData == null) {
                taskCompletationData = dashboardModel
                        .calculateTaskCompletion();
            }
            return taskCompletationData;
        }

        public String[] getTicks() {
            Set<Interval> intervals = getData().keySet();
            String[] result = new String[intervals.size()];
            int i = 0;
            for (Interval each : intervals) {
                result[i++] = each.toString();

            }
            return result;
        }

        public Collection<Integer> getValues() {
            return getData().values();
        }

    }

    /**
     *
     * @author Diego Pino García<dpino@igalia.com>
     *
     */
    static class EstimationAccuracy {

        private final IDashboardModel dashboardModel;

        private Map<Interval, Integer> estimationAccuracyData;

        private EstimationAccuracy(IDashboardModel dashboardModel) {
            this.dashboardModel = dashboardModel;
        }

        public static EstimationAccuracy create(IDashboardModel dashboardModel) {
            return new EstimationAccuracy(dashboardModel);
        }

        private Map<Interval, Integer> getData() {
            if (estimationAccuracyData == null) {
                estimationAccuracyData = dashboardModel
                        .calculateEstimationAccuracy();
            }
            return estimationAccuracyData;
        }

        public String[] getTicks() {
            Set<Interval> intervals = getData().keySet();
            String[] result = new String[intervals.size()];
            int i = 0;
            for (Interval each : intervals) {
                result[i++] = each.toString();

            }
            return result;
        }

        public Collection<Integer> getValues() {
            return getData().values();
        }

    }

}
