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

package org.navalplanner.web.planner.chart;

import static org.navalplanner.business.workingday.EffortDuration.zero;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.navalplanner.business.calendars.entities.BaseCalendar;
import org.navalplanner.business.calendars.entities.ResourceCalendar;
import org.navalplanner.business.calendars.entities.SameWorkHoursEveryDay;
import org.navalplanner.business.planner.entities.DayAssignment;
import org.navalplanner.business.resources.entities.Resource;
import org.navalplanner.business.workingday.EffortDuration;
import org.zkforge.timeplot.Plotinfo;
import org.zkforge.timeplot.Timeplot;
import org.zkforge.timeplot.data.PlotDataSource;
import org.zkforge.timeplot.geometry.DefaultTimeGeometry;
import org.zkforge.timeplot.geometry.DefaultValueGeometry;
import org.zkforge.timeplot.geometry.TimeGeometry;
import org.zkforge.timeplot.geometry.ValueGeometry;
import org.zkoss.ganttz.servlets.CallbackServlet;
import org.zkoss.ganttz.servlets.CallbackServlet.IServletRequestHandler;
import org.zkoss.ganttz.timetracker.zoom.ZoomLevel;
import org.zkoss.ganttz.util.Interval;
import org.zkoss.zk.ui.Executions;

/**
 * Abstract class with the basic functionality to fill the chart.
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
public abstract class ChartFiller implements IChartFiller {

    @Deprecated
    protected abstract class HoursByDayCalculator<T> {
        public SortedMap<LocalDate, BigDecimal> calculate(
                Collection<? extends T> elements) {
            SortedMap<LocalDate, BigDecimal> result = new TreeMap<LocalDate, BigDecimal>();
            if (elements.isEmpty()) {
                return result;
            }
            for (T element : elements) {
                if (included(element)) {
                    int hours = getHoursFor(element);
                    LocalDate day = getDayFor(element);
                    if (!result.containsKey(day)) {
                        result.put(day, BigDecimal.ZERO);
                    }
                    result.put(day, result.get(day).add(new BigDecimal(hours)));
                }
            }
            return convertAsNeededByZoom(result);
        }

        protected abstract LocalDate getDayFor(T element);

        protected abstract int getHoursFor(T element);

        protected boolean included(T each) {
            return true;
        }
    }

    protected abstract class EffortByDayCalculator<T> {
        public SortedMap<LocalDate, EffortDuration> calculate(
                Collection<? extends T> elements) {
            SortedMap<LocalDate, EffortDuration> result = new TreeMap<LocalDate, EffortDuration>();
            if (elements.isEmpty()) {
                return result;
            }
            for (T element : elements) {
                if (included(element)) {
                    EffortDuration duration = getDurationFor(element);
                    LocalDate day = getDayFor(element);
                    EffortDuration previous = result.get(day);
                    previous = previous == null ? zero() : previous;
                    result.put(day, previous.plus(duration));
                }
            }
            return groupAsNeededByZoom(result);
        }

        protected abstract LocalDate getDayFor(T element);

        protected abstract EffortDuration getDurationFor(T element);

        protected boolean included(T each) {
            return true;
        }
    }

    protected class DefaultDayAssignmentCalculator extends
            HoursByDayCalculator<DayAssignment> {
        public DefaultDayAssignmentCalculator() {
        }

        @Override
        protected LocalDate getDayFor(DayAssignment element) {
            return element.getDay();
        }

        @Override
        protected int getHoursFor(DayAssignment element) {
            return element.getHours();
        }
    }

    protected static EffortDuration min(EffortDuration... durations) {
        return Collections.min(Arrays.asList(durations));
    }

    @Deprecated
    protected final int sumHoursForDay(
            Collection<? extends Resource> resources,
            LocalDate day) {
        int sum = 0;
        for (Resource resource : resources) {
            sum += hoursFor(resource, day);
        }
        return sum;
    }

    protected static EffortDuration sumAvailabilitiesForDay(
            Collection<? extends Resource> resources, LocalDate day) {
        EffortDuration sum = zero();
        for (Resource resource : resources) {
            sum = sum.plus(availabilityFor(resource, day));
        }
        return sum;
    }

    private static EffortDuration availabilityFor(Resource resource,
            LocalDate day) {
        return resource.getCalendarOrDefault().getCapacityDurationAt(day);
    }

    private int hoursFor(Resource resource, LocalDate day) {
        int result = 0;
        ResourceCalendar calendar = resource.getCalendar();
        if (calendar != null) {
            result += calendar.getCapacityAt(day);
        } else {
            result += SameWorkHoursEveryDay.getDefaultWorkingDay()
                    .getCapacityAt(day);
        }
        return result;
    }

    protected abstract class GraphicSpecificationCreator implements
            IServletRequestHandler {

        private final LocalDate finish;
        private final SortedMap<LocalDate, BigDecimal> map;
        private final LocalDate start;

        protected GraphicSpecificationCreator(Date finish,
                SortedMap<LocalDate, BigDecimal> map, Date start) {
            this.finish = new LocalDate(finish);
            this.map = map;
            this.start = new LocalDate(start);
        }

        protected Set<LocalDate> getDays() {
            return map.keySet();
        }

        @Override
        public void handle(HttpServletRequest request,
                HttpServletResponse response) throws ServletException,
                IOException {
            PrintWriter writer = response.getWriter();
            fillValues(writer);
            writer.close();
        }

        private void fillValues(PrintWriter writer) {
            fillZeroValueFromStart(writer);
            fillInnerValues(writer, firstDay(), lastDay());
            fillZeroValueToFinish(writer);
        }

        protected abstract void fillInnerValues(PrintWriter writer,
                LocalDate firstDay, LocalDate lastDay);

        protected LocalDate nextDay(LocalDate date) {
            if (isZoomByDay()) {
                return date.plusDays(1);
            } else {
                return date.plusWeeks(1);
            }
        }

        private LocalDate firstDay() {
            LocalDate date = map.firstKey();
            return convertAsNeededByZoom(date);
        }

        private LocalDate lastDay() {
            LocalDate date = map.lastKey();
            return convertAsNeededByZoom(date);
        }

        private LocalDate convertAsNeededByZoom(LocalDate date) {
            if (isZoomByDay()) {
                return date;
            } else {
                return getThursdayOfThisWeek(date);
            }
        }

        protected BigDecimal getHoursForDay(LocalDate day) {
            return map.get(day) != null ? map
                    .get(day) : BigDecimal.ZERO;
        }

        protected void printLine(PrintWriter writer, LocalDate day,
                BigDecimal hours) {
            writer.println(day.toString("yyyyMMdd") + " " + hours);
        }

        private void fillZeroValueFromStart(PrintWriter writer) {
            if (!startIsDayOfFirstAssignment()) {
                printLine(writer, start, BigDecimal.ZERO);
                if (startIsPreviousToPreviousDayToFirstAssignment()) {
                    printLine(writer, previousDayToFirstAssignment(),
                            BigDecimal.ZERO);
                }
            }
        }

        private boolean startIsDayOfFirstAssignment() {
            return !map.isEmpty() && start.compareTo(map.firstKey()) == 0;
        }

        private boolean startIsPreviousToPreviousDayToFirstAssignment() {
            return !map.isEmpty()
                    && start.compareTo(previousDayToFirstAssignment()) < 0;
        }

        private LocalDate previousDayToFirstAssignment() {
            return map.firstKey().minusDays(1);
        }

        private void fillZeroValueToFinish(PrintWriter writer) {
            if (!finishIsDayOfLastAssignment()) {
                if (finishIsPosteriorToNextDayToLastAssignment()) {
                    printLine(writer, nextDayToLastAssignment(),
                            BigDecimal.ZERO);
                }
                printLine(writer, finish, BigDecimal.ZERO);
            }
        }

        private boolean finishIsDayOfLastAssignment() {
            return !map.isEmpty() && start.compareTo(map.lastKey()) == 0;
        }

        private boolean finishIsPosteriorToNextDayToLastAssignment() {
            return !map.isEmpty()
                    && finish.compareTo(nextDayToLastAssignment()) > 0;
        }

        private LocalDate nextDayToLastAssignment() {
            return map.lastKey().plusDays(1);
        }
    }

    protected class DefaultGraphicSpecificationCreator extends
            GraphicSpecificationCreator {

        private DefaultGraphicSpecificationCreator(Date finish,
                SortedMap<LocalDate, BigDecimal> map, Date start) {
            super(finish, map, start);
        }

        @Override
        protected void fillInnerValues(PrintWriter writer, LocalDate firstDay,
                LocalDate lastDay) {
            for (LocalDate day = firstDay; day.compareTo(lastDay) <= 0; day = nextDay(day)) {
                BigDecimal hours = getHoursForDay(day);
                printLine(writer, day, hours);
            }
        }

    }

    protected class JustDaysWithInformationGraphicSpecificationCreator extends
            GraphicSpecificationCreator {

        public JustDaysWithInformationGraphicSpecificationCreator(Date finish,
                SortedMap<LocalDate, BigDecimal> map, Date start) {
            super(finish, map, start);
        }

        @Override
        protected void fillInnerValues(PrintWriter writer, LocalDate firstDay,
                LocalDate lastDay) {
            for (LocalDate day : getDays()) {
                BigDecimal hours = getHoursForDay(day);
                printLine(writer, day, hours);
            }
        }

    }

    /**
     * Number of days to Thursday since the beginning of the week. In order to
     * calculate the middle of a week.
     */
    private final static int DAYS_TO_THURSDAY = 3;

    private ZoomLevel zoomLevel = ZoomLevel.DETAIL_ONE;

    private BigDecimal minimumValueForChart = BigDecimal.ZERO;
    private BigDecimal maximumValueForChart = BigDecimal.ZERO;

    @Override
    public abstract void fillChart(Timeplot chart, Interval interval,
            Integer size);

    private void setMinimumValueForChartIfLess(BigDecimal min) {
        if (minimumValueForChart.compareTo(min) > 0) {
            minimumValueForChart = min;
        }
    }

    private void setMaximumValueForChartIfGreater(BigDecimal max) {
        if (maximumValueForChart.compareTo(max) < 0) {
            maximumValueForChart = max;
        }
    }

    private static LocalDate getThursdayOfThisWeek(LocalDate date) {
        return date.dayOfWeek().withMinimumValue().plusDays(DAYS_TO_THURSDAY);
    }

    private boolean isZoomByDay() {
        return zoomLevel.equals(ZoomLevel.DETAIL_FIVE);
    }

    protected void resetMinimumAndMaximumValueForChart() {
        this.minimumValueForChart = BigDecimal.ZERO;
        this.maximumValueForChart = BigDecimal.ZERO;
    }

    protected BigDecimal getMinimumValueForChart() {
        return minimumValueForChart;
    }

    protected BigDecimal getMaximumValueForChart() {
        return maximumValueForChart;
    }

    protected SortedMap<LocalDate, BigDecimal> groupByWeek(
            SortedMap<LocalDate, BigDecimal> map) {
        SortedMap<LocalDate, BigDecimal> result = new TreeMap<LocalDate, BigDecimal>();
        for (Entry<LocalDate, BigDecimal> entry : map.entrySet()) {
            LocalDate day = entry.getKey();
            LocalDate key = getThursdayOfThisWeek(day);
            BigDecimal hours = entry.getValue() == null ? BigDecimal.ZERO
                    : entry.getValue();
            if (result.get(key) == null) {
                result.put(key, hours);
            } else {
                result.put(key, result.get(key).add(hours));
            }
        }
        for (Entry<LocalDate, BigDecimal> entry : result.entrySet()) {
            LocalDate day = entry.getKey();
            result.put(entry.getKey(), result.get(day).setScale(2).divide(
                    new BigDecimal(7), RoundingMode.DOWN));
        }
        return result;
    }

    @Deprecated
    protected SortedMap<LocalDate, BigDecimal> convertAsNeededByZoom(
            SortedMap<LocalDate, BigDecimal> map) {
        if (isZoomByDay()) {
            return map;
        } else {
            return groupByWeek(map);
        }
    }

    protected SortedMap<LocalDate, EffortDuration> groupAsNeededByZoom(
            SortedMap<LocalDate, EffortDuration> map) {
        if (isZoomByDay()) {
            return map;
        }
        return groupByWeekDurations(map);
    }

    protected SortedMap<LocalDate, EffortDuration> groupByWeekDurations(
            SortedMap<LocalDate, EffortDuration> map) {
        return average(accumulatePerWeek(map));
    }

    private static SortedMap<LocalDate, EffortDuration> accumulatePerWeek(
            SortedMap<LocalDate, EffortDuration> map) {
        SortedMap<LocalDate, EffortDuration> result = new TreeMap<LocalDate, EffortDuration>();
        for (Entry<LocalDate, EffortDuration> each : map.entrySet()) {
            LocalDate centerOfWeek = getThursdayOfThisWeek(each.getKey());
            EffortDuration accumulated = result.get(centerOfWeek);
            accumulated = accumulated == null ? zero() : accumulated;
            result.put(centerOfWeek, accumulated.plus(each.getValue()));
        }
        return result;
    }

    private static SortedMap<LocalDate, EffortDuration> average(
            SortedMap<LocalDate, EffortDuration> accumulatedPerWeek) {
        SortedMap<LocalDate, EffortDuration> result = new TreeMap<LocalDate, EffortDuration>();
        for (Entry<LocalDate, EffortDuration> each : accumulatedPerWeek
                .entrySet()) {
            result.put(each.getKey(), each.getValue().divideBy(7));
        }
        return result;
    }

    protected TimeGeometry getTimeGeometry(Interval interval) {
        LocalDate start = new LocalDate(interval.getStart());
        LocalDate finish = new LocalDate(interval.getFinish());

        TimeGeometry timeGeometry = new DefaultTimeGeometry();

        if (!isZoomByDay()) {
            start = getThursdayOfThisWeek(start);
            finish = getThursdayOfThisWeek(finish);
        }

        String min = start.toString("yyyyMMdd");
        String max = finish.toString("yyyyMMdd");

        timeGeometry.setMin(Integer.valueOf(min));
        timeGeometry.setMax(Integer.valueOf(max));
        timeGeometry.setAxisLabelsPlacement("bottom");
        // Remove year separators
        timeGeometry.setGridColor("#FFFFFF");

        return timeGeometry;
    }

    protected ValueGeometry getValueGeometry() {
        DefaultValueGeometry valueGeometry = new DefaultValueGeometry();
        valueGeometry.setMin(getMinimumValueForChart().intValue());
        valueGeometry.setMax(getMaximumValueForChart().intValue());
        valueGeometry.setGridColor("#000000");
        valueGeometry.setAxisLabelsPlacement("left");

        return valueGeometry;
    }

    @Deprecated
    protected SortedMap<LocalDate, Map<Resource, Integer>> groupDayAssignmentsByDayAndResource(
            List<DayAssignment> dayAssignments) {
        SortedMap<LocalDate, Map<Resource, EffortDuration>> original = groupDurationsByDayAndResource(dayAssignments);
        SortedMap<LocalDate, Map<Resource, Integer>> result = new TreeMap<LocalDate, Map<Resource, Integer>>();
        for (Entry<LocalDate, Map<Resource, EffortDuration>> each : original
                .entrySet()) {
            result.put(each.getKey(), toHoursInteger(each.getValue()));
        }
        return result;
    }

    private Map<Resource, Integer> toHoursInteger(
            Map<Resource, EffortDuration> value) {
        Map<Resource, Integer> result = new HashMap<Resource, Integer>();
        for (Entry<Resource, EffortDuration> each : value.entrySet()) {
            result.put(each.getKey(),
                    BaseCalendar.roundToHours(each.getValue()));
        }
        return result;
    }

    protected SortedMap<LocalDate, Map<Resource, EffortDuration>> groupDurationsByDayAndResource(
            List<DayAssignment> dayAssignments) {
        SortedMap<LocalDate, Map<Resource, EffortDuration>> map = new TreeMap<LocalDate, Map<Resource, EffortDuration>>();

        for (DayAssignment dayAssignment : dayAssignments) {
            final LocalDate day = dayAssignment.getDay();
            final EffortDuration dayAssignmentDuration = dayAssignment
                    .getDuration();
            Resource resource = dayAssignment.getResource();
            if (map.get(day) == null) {
                map.put(day, new HashMap<Resource, EffortDuration>());
            }
            Map<Resource, EffortDuration> forDay = map.get(day);
            EffortDuration previousDuration = forDay.get(resource);
            previousDuration = previousDuration != null ? previousDuration
                    : EffortDuration.zero();
            forDay.put(dayAssignment.getResource(),
                    previousDuration.plus(dayAssignmentDuration));
        }
        return map;
    }

    protected void addCost(SortedMap<LocalDate, BigDecimal> currentCost,
            SortedMap<LocalDate, BigDecimal> additionalCost) {
        for (LocalDate day : additionalCost.keySet()) {
            if (!currentCost.containsKey(day)) {
                currentCost.put(day, BigDecimal.ZERO);
            }
            currentCost.put(day, currentCost.get(day).add(
                    additionalCost.get(day)));
        }
    }

    protected SortedMap<LocalDate, BigDecimal> accumulateResult(
            SortedMap<LocalDate, BigDecimal> map) {
        SortedMap<LocalDate, BigDecimal> result = new TreeMap<LocalDate, BigDecimal>();
        if (map.isEmpty()) {
            return result;
        }

        BigDecimal accumulatedResult = BigDecimal.ZERO;
        for (LocalDate day : map.keySet()) {
            BigDecimal value = map.get(day);
            accumulatedResult = accumulatedResult.add(value);
            result.put(day, accumulatedResult);
        }

        return result;
    }

    protected SortedMap<LocalDate, BigDecimal> convertToBigDecimal(
            SortedMap<LocalDate, Integer> map) {
        SortedMap<LocalDate, BigDecimal> result = new TreeMap<LocalDate, BigDecimal>();

        for (LocalDate day : map.keySet()) {
            BigDecimal value = new BigDecimal(map.get(day));
            result.put(day, value);
        }

        return result;
    }

    protected SortedMap<LocalDate, BigDecimal> calculatedValueForEveryDay(
            SortedMap<LocalDate, BigDecimal> map, Date start, Date finish) {
        return calculatedValueForEveryDay(map, new LocalDate(start),
                new LocalDate(finish));
    }

    protected SortedMap<LocalDate, BigDecimal> calculatedValueForEveryDay(
            SortedMap<LocalDate, BigDecimal> map, LocalDate start,
            LocalDate finish) {
        SortedMap<LocalDate, BigDecimal> result = new TreeMap<LocalDate, BigDecimal>();

        LocalDate previousDay = start;
        BigDecimal previousValue = BigDecimal.ZERO;

        for (LocalDate day : map.keySet()) {
            BigDecimal value = map.get(day);
            fillValues(result, previousDay, day, previousValue, value);

            previousDay = day;
            previousValue = value;
        }

        if (previousDay.compareTo(finish) < 0) {
            fillValues(result, previousDay, finish, previousValue,
                    previousValue);
        }

        return result;
    }

    private void fillValues(SortedMap<LocalDate, BigDecimal> map,
            LocalDate firstDay, LocalDate lastDay, BigDecimal firstValue,
            BigDecimal lastValue) {

        Integer days = Days.daysBetween(firstDay, lastDay).getDays();
        if (days > 0) {
            BigDecimal ammount = lastValue.subtract(firstValue);
            BigDecimal ammountPerDay = ammount.setScale(2, RoundingMode.DOWN).divide(
                    new BigDecimal(days), RoundingMode.DOWN);

            BigDecimal value = firstValue.setScale(2, RoundingMode.DOWN);
            for (LocalDate day = firstDay; day.compareTo(lastDay) <= 0; day = day
                    .plusDays(1)) {
                map.put(day, value);
                value = value.add(ammountPerDay);
            }
        }
    }

    protected Plotinfo createPlotinfoFromDurations(SortedMap<LocalDate, EffortDuration> map,
            Interval interval) {
        return createPlotinfo(toHoursDecimal(map), interval);
    }

    public static <K> SortedMap<K, BigDecimal> toHoursDecimal(
            Map<K, EffortDuration> map) {
        SortedMap<K, BigDecimal> result = new TreeMap<K, BigDecimal>();
        for (Entry<K, EffortDuration> each : map.entrySet()) {
            result.put(each.getKey(), each.getValue()
                    .toHoursAsDecimalWithScale(2));
        }
        return result;
    }

    protected Plotinfo createPlotinfo(SortedMap<LocalDate, BigDecimal> map,
            Interval interval) {
        return createPlotinfo(map, interval, false);
    }

    protected Plotinfo createPlotinfo(SortedMap<LocalDate, BigDecimal> map,
            Interval interval, boolean justDaysWithInformation) {
        return createPlotInfoFrom(createDataSourceUri(map, interval,
                justDaysWithInformation));
    }

    private String createDataSourceUri(SortedMap<LocalDate, BigDecimal> map,
            Interval interval, boolean justDaysWithInformation) {
        return getServletUri(
                map,
                interval.getStart(),
                interval.getFinish(),
                createGraphicSpecification(map, interval,
                        justDaysWithInformation));
    }

    private GraphicSpecificationCreator createGraphicSpecification(
            SortedMap<LocalDate, BigDecimal> map, Interval interval,
            boolean justDaysWithInformation) {
        if (justDaysWithInformation) {
            return new JustDaysWithInformationGraphicSpecificationCreator(
                    interval.getFinish(), map, interval.getStart());
        } else {
            return new DefaultGraphicSpecificationCreator(interval.getFinish(),
                    map, interval.getStart());
        }
    }

    private String getServletUri(
            final SortedMap<LocalDate, BigDecimal> mapDayAssignments,
            final Date start, final Date finish,
            final GraphicSpecificationCreator graphicSpecificationCreator) {
        if (mapDayAssignments.isEmpty()) {
            return "";
        }

        setMinimumValueForChartIfLess(Collections.min(mapDayAssignments
                .values()));
        setMaximumValueForChartIfGreater(Collections.max(mapDayAssignments
                .values()));

        HttpServletRequest request = (HttpServletRequest) Executions
                .getCurrent().getNativeRequest();
        String uri = CallbackServlet.registerAndCreateURLFor(request,
                graphicSpecificationCreator);
        return uri;
    }

    private Plotinfo createPlotInfoFrom(String dataSourceUri) {
        PlotDataSource pds = new PlotDataSource();
        pds.setDataSourceUri(dataSourceUri);
        pds.setSeparator(" ");

        Plotinfo plotinfo = new Plotinfo();
        plotinfo.setPlotDataSource(pds);
        return plotinfo;
    }

    protected void appendPlotinfo(Timeplot chart, Plotinfo plotinfo,
            ValueGeometry valueGeometry, TimeGeometry timeGeometry) {
        plotinfo.setValueGeometry(valueGeometry);
        plotinfo.setTimeGeometry(timeGeometry);
        chart.appendChild(plotinfo);
    }

    @Override
    public void setZoomLevel(ZoomLevel zoomLevel) {
        this.zoomLevel = zoomLevel;
    }

}
