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

package org.navalplanner.business.test.workingday;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.navalplanner.business.workingday.ResourcesPerDay;
import org.navalplanner.business.workingday.ResourcesPerDay.ResourcesPerDayDistributor;

public class ResourcesPerDayTest {

    @Test(expected = IllegalArgumentException.class)
    public void cannotHaveANegativeNumberOfUnits() {
        ResourcesPerDay.amount(-1);
    }

    @Test
    public void theUnitsAmoutCanBeRetrieved() {
        ResourcesPerDay units = ResourcesPerDay.amount(2);
        assertThat(units, readsAs(2, 0));
    }

    private Matcher<ResourcesPerDay> readsAs(final int integerPart,
            final int decimalPart) {
        return new BaseMatcher<ResourcesPerDay>() {

            @Override
            public boolean matches(Object arg) {
                if (arg instanceof ResourcesPerDay) {
                    ResourcesPerDay r = (ResourcesPerDay) arg;
                    return r.getAmount().intValue() == integerPart
                            && getDecimalPart(r) == decimalPart;
                }
                return false;
            }

            private int getDecimalPart(ResourcesPerDay r) {
                BigDecimal onlyDecimal = r.getAmount().subtract(
                        new BigDecimal(r.getAmount().intValue()));
                BigDecimal decimalPartAsInt = onlyDecimal.movePointRight(2);
                int result = decimalPartAsInt.intValue();
                return result;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("must have an integer part of "
                        + integerPart + " and ");
                description.appendText("must have " + decimalPart
                        + " as decimal part");
            }
        };
    }

    @Test
    public void theUnitsAmountCanBeADecimalValue() {
        ResourcesPerDay resourcesPerDay = ResourcesPerDay
                .amount(new BigDecimal(2.2));
        assertThat(resourcesPerDay, readsAs(2, 20));
    }

    @Test
    public void theAmountIsConvertedToABigDecimalOfScale2() {
        ResourcesPerDay resourcesPerDay = ResourcesPerDay
                .amount(new BigDecimal(2.2));
        assertThat(resourcesPerDay.getAmount().scale(), equalTo(2));
    }

    @Test
    public void ifTheAmountSpecifiedHasBiggerScaleThan2ItIsRoundedHalfUp() {
        BigDecimal[] examples = { new BigDecimal(2.236),
                new BigDecimal(2235).movePointLeft(3), new BigDecimal(2.24),
                new BigDecimal(2.2449) };
        for (BigDecimal example : examples) {
            ResourcesPerDay resourcesPerDay = ResourcesPerDay.amount(example);
            assertThat(resourcesPerDay.getAmount().scale(), equalTo(2));
            assertThat(resourcesPerDay, readsAs(2, 24));
        }
    }

    @Test
    public void canBeConvertedToHoursGivenTheWorkingDayHours() {
        ResourcesPerDay units = ResourcesPerDay.amount(2);
        assertThat(units.asHoursGivenResourceWorkingDayOf(8), equalTo(16));
    }

    @Test
    public void ifTheAmountIsDecimalTheRoundingIs() {
        ResourcesPerDay units = ResourcesPerDay.amount(new BigDecimal(2.4));
        assertThat(units.asHoursGivenResourceWorkingDayOf(8), equalTo(19));
        assertThat(units.asHoursGivenResourceWorkingDayOf(10), equalTo(24));
        assertThat(units.asHoursGivenResourceWorkingDayOf(2), equalTo(5));
    }

    @Test
    public void twoResourcesPerDayAreEqualsIfNormalizeToTheSameAmount() {
        ResourcesPerDay a = ResourcesPerDay.amount(new BigDecimal(2.001));
        ResourcesPerDay b = ResourcesPerDay.amount(2);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(a, b);
    }

    @Test
    public void asHoursMustReturnOneIfAmountIsGreaterThanZero() {
        ResourcesPerDay amount = ResourcesPerDay.amount(new BigDecimal(0.05));
        int hours = amount
                .asHoursGivenResourceWorkingDayOf(8);
        assertThat(hours, equalTo(1));
    }

    @Test
    public void ifTheAmountIsZeroMustReturnZero() {
        ResourcesPerDay amount = ResourcesPerDay.amount(BigDecimal.ZERO);
        int hours = amount.asHoursGivenResourceWorkingDayOf(8);
        assertThat(hours, equalTo(0));
    }

    @Test
    public void isZeroIfHaveZeroValue() {
        BigDecimal[] examples = { new BigDecimal(0.0001), new BigDecimal(0),
                new BigDecimal(00), new BigDecimal(0.00) };
        for (BigDecimal example : examples) {
            assertTrue(ResourcesPerDay.amount(example).isZero());
        }
    }

    @Test
    public void notZeroIfNoZeroValue() {
        BigDecimal[] examples = { new BigDecimal(0.01), new BigDecimal(0.009),
                new BigDecimal(1), new BigDecimal(0.10) };
        for (BigDecimal example : examples) {
            assertFalse(ResourcesPerDay.amount(example).isZero());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void canCalculateTheResourcesPerDayFromTheHoursWorkingAndTheWorkableHours() {
        Object[] periodicalNumber = { ResourcesPerDay.calculateFrom(10, 3),
                readsAs(3, 33) };
        Object[][] examples = {
                { ResourcesPerDay.calculateFrom(1000, 1000), readsAs(1, 00) },
                { ResourcesPerDay.calculateFrom(2000, 1000), readsAs(2, 00) },
                { ResourcesPerDay.calculateFrom(500, 1000), readsAs(0, 50) },
                { ResourcesPerDay.calculateFrom(651, 1000), readsAs(0, 65) },
                { ResourcesPerDay.calculateFrom(1986, 1000), readsAs(1, 99) },
                periodicalNumber };
        for (Object[] pair : examples) {
            ResourcesPerDay first = (ResourcesPerDay) pair[0];
            Matcher<ResourcesPerDay> matcher = (Matcher<ResourcesPerDay>) pair[1];
            assertThat(first, matcher);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void canDistributeResourcesPerDay() {
        ResourcesPerDayDistributor distributor = ResourcesPerDay.distributor(
                ResourcesPerDay.amount(new BigDecimal(0.8)), ResourcesPerDay
                        .amount(new BigDecimal(0.2)));
        Object[][] examples = {
                { ResourcesPerDay.amount(10),
                    readsAs(8, 0), readsAs(2, 0) },
                { ResourcesPerDay.amount(1),
                    readsAs(0, 80), readsAs(0, 20) },
                { ResourcesPerDay.amount(new BigDecimal(0.5)),
                    readsAs(0, 40),readsAs(0, 10) } };
        for (Object[] eachExample : examples) {
            ResourcesPerDay toDistribute = (ResourcesPerDay) eachExample[0];
            Matcher<ResourcesPerDay> firstMatcher = (Matcher<ResourcesPerDay>) eachExample[1];
            Matcher<ResourcesPerDay> secondMatcher = (Matcher<ResourcesPerDay>) eachExample[2];
            ResourcesPerDay[] distribute = distributor.distribute(toDistribute);
            assertThat(distribute[0], firstMatcher);
            assertThat(distribute[1], secondMatcher);
        }
    }

}
