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
package org.zkoss.ganttz.data.constraint;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.apache.commons.lang.Validate;

/**
 * @author Óscar González Fernández
 *
 */
public class ConstraintOnComparableValues<T extends Comparable<T>> extends
        Constraint<T> {

    public static class DateConstraint {

        public static Constraint<Date> biggerOrEqualThan(Date date) {
            return instantiate(ComparisonType.BIGGER_OR_EQUAL_THAN, date);
        }

        public static Constraint<Date> equalTo(Date date) {
            return instantiate(ComparisonType.EQUAL_TO, date);
        }

    }

    private static <T extends Comparable<T>> Constraint<T> instantiate(
            ComparisonType type, T value) {
        if (value == null) {
            return Constraint.voidConstraint();
        }
        return new ConstraintOnComparableValues<T>(type, value);
    }

    public enum ComparisonType {
        LESS_OR_EQUAL_THAN, BIGGER_OR_EQUAL_THAN, EQUAL_TO;
    }

    private final T comparisonValue;
    private final ComparisonType comparisonType;

    protected ConstraintOnComparableValues(ComparisonType comparisonType, T value) {
        Validate.notNull(value);
        this.comparisonValue = value;
        this.comparisonType = comparisonType;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected T applyConstraintTo(T value) {
        if (value == null) {
            return comparisonValue;
        }
        switch (comparisonType) {
        case LESS_OR_EQUAL_THAN:
            return min(comparisonValue, value);
        case BIGGER_OR_EQUAL_THAN:
            return max(comparisonValue, value);
        case EQUAL_TO:
            return comparisonValue;
        default:
            throw new RuntimeException("can't handle "+comparisonType);
        }
    }

    private T min(T... values) {
        return Collections.min(Arrays.asList(values));
    }

    private T max(T... values) {
        return Collections.max(Arrays.asList(values));
    }

    @Override
    public boolean isSatisfiedBy(T value) {
        switch (comparisonType) {
        case LESS_OR_EQUAL_THAN:
            return value.compareTo(comparisonValue) <= 0;
        case BIGGER_OR_EQUAL_THAN:
            return value.compareTo(comparisonValue) >= 0;
        case EQUAL_TO:
            return value.compareTo(comparisonValue) == 0;
        default:
            throw new RuntimeException("can't handle " + comparisonType);
        }
    }

}
