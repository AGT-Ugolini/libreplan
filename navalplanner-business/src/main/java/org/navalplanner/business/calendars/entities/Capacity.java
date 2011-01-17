/*
 * This file is part of NavalPlan
 *
 * Copyright (C) 2011 Igalia, S.L.
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
package org.navalplanner.business.calendars.entities;


import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.navalplanner.business.workingday.EffortDuration;

/**
 * This class is intended as a Hibernate component. It's formed by two
 * components, the standard effort and the allowed extra effort. It represents
 * the capacity for a resource.
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 */
public class Capacity {

    public static Capacity create(EffortDuration hours) {
        return new Capacity(hours, null);
    }

    public static Capacity zero() {
        return new Capacity(EffortDuration.zero(), EffortDuration.zero());
    }

    private EffortDuration standardEffort;

    private EffortDuration allowedExtraEffort;

    /**
     * Default constructor for hibernate. DO NOT USE!
     *
     * @see Capacity#create(EffortDuration)
     */
    public Capacity() {
    }

    private Capacity(EffortDuration standardEffort,
            EffortDuration extraHours) {
        Validate.notNull(standardEffort);
        this.standardEffort = standardEffort;
        this.allowedExtraEffort = extraHours;
    }

    public EffortDuration getStandardEffort() {
        if (standardEffort == null) {
            return EffortDuration.zero();
        }
        return standardEffort;
    }

    public EffortDuration getAllowedExtraEffort() {
        return allowedExtraEffort;
    }

    public boolean isOverAssignable() {
        return allowedExtraEffort == null;
    }

    public Capacity extraEffort(EffortDuration extraEffort) {
        return new Capacity(standardEffort, extraEffort);
    }

    public Capacity withStandardEffort(EffortDuration standardEffort) {
        return new Capacity(standardEffort, allowedExtraEffort);
    }

    public Capacity overAssignable(boolean overAssignable) {
        if (overAssignable) {
            return new Capacity(standardEffort, null);
        } else {
            return new Capacity(standardEffort,
                    allowedExtraEffort == null ? EffortDuration.zero()
                            : allowedExtraEffort);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Capacity) {
            Capacity other = (Capacity) obj;
            return new EqualsBuilder()
                    .append(standardEffort, other.standardEffort)
                    .append(allowedExtraEffort, other.allowedExtraEffort)
                    .isEquals();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(standardEffort)
                .append(allowedExtraEffort).toHashCode();
    }

}
