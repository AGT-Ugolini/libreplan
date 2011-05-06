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

import static org.navalplanner.business.i18n.I18nHelper._;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.hibernate.validator.AssertTrue;
import org.hibernate.validator.Valid;
import org.joda.time.Days;
import org.joda.time.LocalDate;

/**
 * @author Diego Pino García <dpino@igalia.com>
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 *
 *         Assignment function by stretches.
 *
 */
public class StretchesFunction extends AssignmentFunction {

    public static class Interval {

        private LocalDate start;

        private LocalDate end;

        private final BigDecimal loadProportion;

        private boolean readOnly = false;

        public static Interval create(BigDecimal loadProportion, LocalDate start,
                LocalDate end, boolean readOnly) {
            Interval result = create(loadProportion, start, end);
            result.readOnly(readOnly);
            return result;
        }

        public static Interval create(BigDecimal loadProportion, LocalDate start,
                LocalDate end) {
            return new Interval(loadProportion, start, end);
        }

        public Interval(BigDecimal loadProportion, LocalDate start,
                LocalDate end) {
            Validate.notNull(loadProportion);
            Validate.isTrue(loadProportion.signum() >= 0);
            Validate.notNull(end);
            this.loadProportion = loadProportion.setScale(2,
                    RoundingMode.HALF_UP);
            this.start = start;
            this.end = end;
        }

        public static double[] getHoursPointsFor(int totalHours,
                List<Interval> intervalsDefinedByStreches) {
            double[] result = new double[intervalsDefinedByStreches.size() + 1];
            int i = 1;
            result[0] = 0;
            int accumulated = 0;
            for (Interval each : intervalsDefinedByStreches) {
                accumulated += each.getHoursFor(totalHours);
                result[i++] = accumulated;
            }
            return result;
        }

        public static double[] getDayPointsFor(LocalDate start,
                List<Interval> intervalsDefinedByStreches) {
            double[] result = new double[intervalsDefinedByStreches.size() + 1];
            result[0] = 0;
            int i = 1;
            for (Interval each : intervalsDefinedByStreches) {
                result[i++] = Days.daysBetween(start, each.getEnd()).getDays();
            }
            return result;
        }

        public LocalDate getEnd() {
            return end;
        }

        public BigDecimal getLoadProportion() {
            return loadProportion;
        }

        public boolean hasNoStart() {
            return start == null;
        }

        public LocalDate getStart() {
            return start;
        }

        public int getHoursFor(int totalHours) {
            return loadProportion.multiply(new BigDecimal(totalHours))
                    .intValue();
        }

        public LocalDate getStartFor(LocalDate allocationStart) {
            return hasNoStart() ? allocationStart : start;
        }

        private void apply(ResourceAllocation<?> resourceAllocation,
                LocalDate startInclusive, LocalDate taskEnd,
                int intervalHours) {
            Validate.isTrue(!isReadOnly());

            // End has to be exclusive on last Stretch
            LocalDate endDate = getEnd();
            if (endDate.equals(taskEnd)) {
                endDate = endDate.plusDays(1);
            }
            resourceAllocation.withPreviousAssociatedResources()
                    .onIntervalWithinTask(getStartFor(startInclusive), endDate)
                    .allocateHours(intervalHours);
        }

        public static void apply(ResourceAllocation<?> allocation,
                List<Interval> intervalsDefinedByStreches,
                LocalDate allocationStart, LocalDate allocationEnd,
                int totalHours) {
            if (intervalsDefinedByStreches.isEmpty()) {
                return;
            }
            Validate.isTrue(totalHours == allocation.getNonConsolidatedHours());

            int[] hoursPerInterval = getHoursPerInterval(
                    intervalsDefinedByStreches, totalHours);
            int remainder = totalHours - sum(hoursPerInterval);
            hoursPerInterval[0] += remainder;
            int i = 0;
            for (Interval interval : intervalsDefinedByStreches) {
                interval.apply(allocation, allocationStart, allocationEnd,
                        hoursPerInterval[i++]);
            }
        }

        private static int[] getHoursPerInterval(
                List<Interval> intervalsDefinedByStreches, int totalHours) {
            int[] hoursPerInterval = new int[intervalsDefinedByStreches.size()];
            int i = 0;
            for (Interval each : intervalsDefinedByStreches) {
                hoursPerInterval[i++] = each.getHoursFor(totalHours);
            }
            return hoursPerInterval;
        }

        private static int sum(int[] hoursPerInterval) {
            int result = 0;
            for (int each : hoursPerInterval) {
                result += each;
            }
            return result;
        }

        public String toString() {
            return String.format("[%s, %s]: %s ", start, end, loadProportion);
        }

        public boolean isReadOnly() {
            return readOnly;
        }

        public void readOnly(boolean value) {
            readOnly = value;
        }

    }

    private List<Stretch> stretches = new ArrayList<Stretch>();

    private StretchesFunctionTypeEnum type;

    // Transient field. Not stored
    private StretchesFunctionTypeEnum desiredType;

    // Transient. Calculated from resourceAllocation
    private Stretch consolidatedStretch;

    public static StretchesFunction create() {
        return (StretchesFunction) create(new StretchesFunction());
    }

    /**
     * Constructor for hibernate. Do not use!
     */
    protected StretchesFunction() {

    }

    public static List<Interval> intervalsFor(
            Collection<? extends Stretch> streches) {
        ArrayList<Interval> result = new ArrayList<Interval>();
        LocalDate previous = null, stretchDate = null;
        BigDecimal sumOfProportions = BigDecimal.ZERO, loadedProportion = BigDecimal.ZERO;

        for (Stretch each : streches) {
            stretchDate = each.getDate();
            loadedProportion = each.getAmountWorkPercentage().subtract(
                    sumOfProportions);
            if (loadedProportion.signum() < 0) {
                loadedProportion = BigDecimal.ZERO;
            }
            result.add(Interval.create(loadedProportion, previous,
                    stretchDate, each.isReadOnly()));
            sumOfProportions = each.getAmountWorkPercentage();
            previous = stretchDate;
        }
        return result;
    }

    private static <T> T last(List<? extends T> list) {
        return list.get(list.size() - 1);
    }

    public StretchesFunction copy() {
        StretchesFunction result = StretchesFunction.create();
        result.resetToStrechesFrom(this);
        result.type = type;
        result.desiredType = desiredType;
        result.consolidatedStretch = consolidatedStretch;
        return result;
    }

    public void resetToStrechesFrom(StretchesFunction from) {
        this.removeAllStretches();
        for (Stretch each : from.getStretches()) {
            this.addStretch(Stretch.copy(each));
        }
        this.consolidatedStretch = from.consolidatedStretch;
    }

    public void setStretches(List<Stretch> stretches) {
        this.stretches = stretches;
    }

    private void sortStretches() {
        Collections.sort(stretches, new Comparator<Stretch>() {
            @Override
            public int compare(Stretch o1, Stretch o2) {
                return o1.getDate().compareTo(o2.getDate());
            }
        });
    }

    @Valid
    public List<Stretch> getStretches() {
        sortStretches();
        return Collections.unmodifiableList(stretches);
    }

    public StretchesFunctionTypeEnum getType() {
        return type == null ? StretchesFunctionTypeEnum.STRETCHES : type;
    }

    public StretchesFunctionTypeEnum getDesiredType() {
        return desiredType == null ? getType() : desiredType;
    }

    public void changeTypeTo(StretchesFunctionTypeEnum type) {
        desiredType = type;
    }

    public void addStretch(Stretch stretch) {
        stretches.add(stretch);
    }

    public void removeStretch(Stretch stretch) {
        stretches.remove(stretch);
    }

    public void removeAllStretches() {
        stretches.clear();
    }

    @AssertTrue(message = "At least one stretch is needed")
    public boolean checkNoEmpty() {
        return !getStretchesPlusConsolidated().isEmpty();
    }

    @AssertTrue(message = "Some stretch has lower or equal values than the "
            + "previous stretch")
    public boolean checkStretchesOrder() {
        List<Stretch> stretchesPlusConsolidated = getStretchesPlusConsolidated();
        if (stretchesPlusConsolidated.isEmpty()) {
            return false;
        }

        Iterator<Stretch> iterator = stretchesPlusConsolidated.iterator();
        Stretch previous = iterator.next();
        while (iterator.hasNext()) {
            Stretch current = iterator.next();
            if (current.getDate().compareTo(previous.getDate()) <= 0) {
                return false;
            }
            if (current.getLengthPercentage().compareTo(
                    previous.getLengthPercentage()) <= 0) {
                return false;
            }
            if (current.getAmountWorkPercentage().compareTo(
                    previous.getAmountWorkPercentage()) <= 0) {
                return false;
            }
            previous = current;
        }
        return true;
    }

    public List<Stretch> getStretchesPlusConsolidated() {
        List<Stretch> result = new ArrayList<Stretch>();
        result.addAll(stretches);
        if (consolidatedStretch != null) {
            result.add(consolidatedStretch);
        }
        return Collections.unmodifiableList(Stretch.sortByDate(result));
    }

    @AssertTrue(message = "Last stretch should have one hundred percent for "
            + "length and amount of work percentage")
    public boolean checkOneHundredPercent() {
        if (stretches.isEmpty()) {
            return false;
        }
        sortStretches();

        Stretch lastStretch = stretches.get(stretches.size() - 1);
        if (lastStretch.getLengthPercentage().compareTo(BigDecimal.ONE) != 0) {
            return false;
        }
        if (lastStretch.getAmountWorkPercentage().compareTo(BigDecimal.ONE) != 0) {
            return false;
        }

        return true;
    }

    @Override
    public void applyTo(ResourceAllocation<?> resourceAllocation) {
        if (!resourceAllocation.hasAssignments()) {
            return;
        }
        // Is 100% consolidated
        if (resourceAllocation.getFirstNonConsolidatedDate() == null) {
            return;
        }
        getDesiredType().applyTo(resourceAllocation, this);
        type = getDesiredType();
    }

    @Override
    public String getName() {
        if (StretchesFunctionTypeEnum.INTERPOLATED.equals(type)) {
            return ASSIGNMENT_FUNCTION_NAME.INTERPOLATION.toString();
        } else {
            return ASSIGNMENT_FUNCTION_NAME.STRETCHES.toString();
        }
    }

    public List<Interval> getIntervalsDefinedByStreches() {
        List<Stretch> stretches = stretchesFor(type);
        if (stretches.isEmpty()) {
            return Collections.emptyList();
        }
        checkStretchesSumOneHundredPercent();
        return intervalsFor(stretches);
    }

    private List<Stretch> stretchesFor(StretchesFunctionTypeEnum type) {
        return (getDesiredType().equals(StretchesFunctionTypeEnum.INTERPOLATED)) ? getStretchesPlusConsolidated()
                : getStretches();
    }

    private void checkStretchesSumOneHundredPercent() {
        BigDecimal sumOfProportions = stretches.isEmpty() ? BigDecimal.ZERO
                : last(stretches).getAmountWorkPercentage();
        BigDecimal left = calculateLeftFor(sumOfProportions);
        if (!left.equals(BigDecimal.ZERO)) {
            throw new IllegalStateException(_("Stretches must sum 100%"));
        }
    }

    private BigDecimal calculateLeftFor(BigDecimal sumOfProportions) {
        BigDecimal left = BigDecimal.ONE.subtract(sumOfProportions);
        left = left.signum() <= 0 ? BigDecimal.ZERO : left;
        return left;
    }

    public boolean checkHasAtLeastTwoStretches() {
        return getStretchesPlusConsolidated().size() >= 2;
    }

    public boolean checkFirstIntervalIsPosteriorToDate(LocalDate date) {
        List<Interval> intervals = StretchesFunction
                .intervalsFor(getStretchesPlusConsolidated());
        if (intervals.isEmpty()) {
            return false;
        }
        Interval first = intervals.get(0);
        return first.getEnd().compareTo(date) > 0;
    }

    public boolean isInterpolated() {
        return getDesiredType().equals(StretchesFunctionTypeEnum.INTERPOLATED);
    }

    public void setConsolidatedStretch(Stretch stretch) {
        consolidatedStretch = stretch;
    }

    public Stretch getConsolidatedStretch() {
        return consolidatedStretch;
    }

}
