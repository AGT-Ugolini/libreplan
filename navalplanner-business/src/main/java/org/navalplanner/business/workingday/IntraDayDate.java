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
package org.navalplanner.business.workingday;

import static org.navalplanner.business.workingday.EffortDuration.zero;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.validator.NotNull;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

/**
 * @author Óscar González Fernández
 *
 */
public class IntraDayDate implements Comparable<IntraDayDate> {

    public static IntraDayDate min(IntraDayDate... dates) {
        Validate.noNullElements(dates);
        return Collections.min(Arrays.asList(dates));
    }

    public static IntraDayDate max(IntraDayDate... dates) {
        Validate.noNullElements(dates);
        return Collections.max(Arrays.asList(dates));
    }

    public static IntraDayDate startOfDay(LocalDate date) {
        return create(date, zero());
    }

    public static IntraDayDate create(LocalDate date, EffortDuration effortDuration) {
        return new IntraDayDate(date, effortDuration);
    }

    private LocalDate date;

    private EffortDuration effortDuration;

    /**
     * Default constructor for hibernate do not use!
     */
    public IntraDayDate() {
    }

    private IntraDayDate(LocalDate date, EffortDuration effortDuration) {
        Validate.notNull(date);
        Validate.notNull(effortDuration);
        this.date = date;
        this.effortDuration = effortDuration;
    }

    @NotNull
    public LocalDate getDate() {
        return date;
    }

    public EffortDuration getEffortDuration() {
        return effortDuration == null ? EffortDuration.zero() : effortDuration;
    }

    public boolean isStartOfDay() {
        return getEffortDuration().isZero();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IntraDayDate) {
            IntraDayDate other = (IntraDayDate) obj;
            return this.date.equals(other.date)
                    && this.getEffortDuration().equals(
                            other.getEffortDuration());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.date)
                .append(this.getEffortDuration()).toHashCode();
    }

    public boolean areSameDay(Date date) {
        return areSameDay(LocalDate.fromDateFields(date));
    }

    public boolean areSameDay(LocalDate date) {
        return this.date.equals(date);
    }

    public DateTime toDateTimeAtStartOfDay() {
        return this.date.toDateTimeAtStartOfDay();
    }

    @Override
    public int compareTo(IntraDayDate other) {
        int result = date.compareTo(other.date);
        if (result == 0) {
            result = getEffortDuration().compareTo(other.getEffortDuration());
        }
        return result;
    }

    /**
     * Return the day which is the exclusive end given this {@link IntraDayDate}
     * @return
     */
    public LocalDate asExclusiveEnd() {
        if (isStartOfDay()) {
            return getDate();
        }
        return getDate().plusDays(1);
    }

    public int compareTo(LocalDate other) {
        int result = this.date.compareTo(other);
        if (result != 0) {
            return result;
        }
        return isStartOfDay() ? 0 : 1;
    }

    /**
     * It's an interval of {@link IntraDayDate}. It allows to know how much
     * effort can be spent in this day
     */
    public static class PartialDay {

        public static PartialDay wholeDay(LocalDate date) {
            return new PartialDay(IntraDayDate.startOfDay(date),
                    IntraDayDate.startOfDay(date.plusDays(1)));
        }

        private final IntraDayDate start;
        private final IntraDayDate end;

        public PartialDay(IntraDayDate start, IntraDayDate end) {
            Validate.notNull(start);
            Validate.notNull(end);
            Validate.isTrue(end.compareTo(start) >= 0);
            Validate.isTrue(start.areSameDay(end.getDate())
                    || (start.areSameDay(end.getDate().minusDays(1)) && end
                            .getEffortDuration().isZero()));
            this.start = start;
            this.end = end;
        }

        public IntraDayDate getStart() {
            return start;
        }

        public IntraDayDate getEnd() {
            return end;
        }

        public LocalDate getDate() {
            return start.getDate();
        }

        /**
         * <p>
         * Limits the duration that can be worked in a day taking into account
         * this day duration.
         * </p>
         * <ul>
         * <li>
         * If the day is whole then the duration returned is all.</li>
         * <li>If the day has an end and a no zero start, the result will be:
         * <code>max(duration, end) - start</code></li>
         * <li>If the day has a no zero start that must be discounted from the
         * duration</li>
         * <li>If the day has an end, the duration must not surpass this end</li>
         * </ul>
         * @param duration
         * @return a duration that can be employed taking into consideration
         *         this day
         */
        public EffortDuration limitDuration(EffortDuration duration) {
            if (isWholeDay()) {
                return duration;
            }
            EffortDuration alreadyElapsedInDay = start.getEffortDuration();
            if (alreadyElapsedInDay.compareTo(duration) >= 0) {
                return zero();
            }
            EffortDuration durationLimitedByEnd = duration;
            if (!end.getEffortDuration().isZero()) {
                durationLimitedByEnd = EffortDuration
                        .min(end.getEffortDuration(), duration);
            }
            return durationLimitedByEnd.minus(alreadyElapsedInDay);
        }

        private boolean isWholeDay() {
            return start.getEffortDuration().isZero()
                    && end.getEffortDuration().isZero();
        }

        @Override
        public String toString() {
            return Arrays.toString(new Object[] { start, end });
        }

    }

    /**
     * Returns an on demand {@link Iterable} that gives all the days from
     * <code>this</code> to end
     * @param end
     * @return an on demand iterable
     */
    public Iterable<PartialDay> daysUntil(final IntraDayDate end) {
        Validate.isTrue(compareTo(end) <= 0);
        if (compareTo(end) == 0) {
            return Collections.<PartialDay> emptyList();
        }
        return new Iterable<IntraDayDate.PartialDay>() {

            @Override
            public Iterator<PartialDay> iterator() {
                return createIterator(IntraDayDate.this, end);
            }

        };
    }

    public static List<PartialDay> toList(Iterable<PartialDay> days) {
        List<PartialDay> result = new ArrayList<IntraDayDate.PartialDay>();
        for (PartialDay each : days) {
            result.add(each);
        }
        return result;
    }

    private static Iterator<PartialDay> createIterator(
            final IntraDayDate start, final IntraDayDate end) {

        return new Iterator<IntraDayDate.PartialDay>() {
            private IntraDayDate current = start;

            @Override
            public boolean hasNext() {
                return current.compareTo(end) < 0;
            }

            @Override
            public PartialDay next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                IntraDayDate start = current;
                current = calculateNext(current);
                return new PartialDay(start, current);
            }

            private IntraDayDate calculateNext(IntraDayDate date) {
                IntraDayDate nextDay = IntraDayDate.startOfDay(date.date
                        .plusDays(1));
                return min(nextDay, end);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public String toString() {
        return Arrays.toString(new Object[] { date, effortDuration });
    }

    public IntraDayDate nextDayAtStart() {
        return IntraDayDate.startOfDay(getDate().plusDays(1));
    }

}
