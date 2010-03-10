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
package org.navalplanner.business.planner.entities;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.joda.time.LocalDate;
import org.navalplanner.business.calendars.entities.AvailabilityTimeLine;
import org.navalplanner.business.calendars.entities.ResourceCalendar;
import org.navalplanner.business.resources.entities.Criterion;
import org.navalplanner.business.resources.entities.CriterionSatisfaction;
import org.navalplanner.business.resources.entities.Resource;

/**
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 *
 */
public class AvailabilityCalculator {

    private AvailabilityCalculator() {
    }

    public static AvailabilityTimeLine getCalendarAvailabilityFor(
            Resource resource) {
        ResourceCalendar resourceCalendar = resource.getCalendar();
        return resourceCalendar != null ? resourceCalendar.getAvailability()
                : AvailabilityTimeLine.allValid();
    }

    public static AvailabilityTimeLine buildSumOfAvailabilitiesFor(
            Collection<? extends Criterion> criterions, List<Resource> resources) {
        AvailabilityTimeLine result = AvailabilityTimeLine.createAllInvalid();
        for (Resource each : resources) {
            result = result.or(AvailabilityCalculator.availabilityFor(
                    criterions, each));
        }
        return result;
    }

    public static AvailabilityTimeLine availabilityFor(
            Collection<? extends Criterion> criterions, Resource each) {
        AvailabilityTimeLine result = AvailabilityTimeLine.allValid();
        result = result.and(getCalendarAvailabilityFor(each));
        return result.and(getCriterionsAvailabilityFor(criterions, each));
    }

    private static AvailabilityTimeLine getCriterionsAvailabilityFor(
            Collection<? extends Criterion> criterions, Resource resource) {
        AvailabilityTimeLine result = AvailabilityTimeLine.allValid();
        for (Criterion each : criterions) {
            result = result.and(buildTimeline(resource
                    .getSatisfactionsFor(each)));
        }
        return result;
    }

    private static AvailabilityTimeLine buildTimeline(
            List<CriterionSatisfaction> satisfactions) {
        if (satisfactions.isEmpty()) {
            return AvailabilityTimeLine.createAllInvalid();
        }
        AvailabilityTimeLine result = AvailabilityTimeLine.allValid();
        LocalDate previousEnd = null;
        for (CriterionSatisfaction each : satisfactions) {
            LocalDate startDate = asLocal(each.getStartDate());
            assert startDate != null : "satisfactions start date is not null";
            if (previousEnd == null) {
                result.invalidUntil(startDate);
            } else {
                result.invalidAt(previousEnd, startDate);
            }
            previousEnd = asLocal(each.getEndDate());
            if (previousEnd == null) {
                break;
            }
        }
        if (previousEnd != null) {
            result.invalidFrom(previousEnd);
        }
        return result;
    }

    private static LocalDate asLocal(Date date) {
        return date != null ? LocalDate.fromDateFields(date) : null;
    }

}
