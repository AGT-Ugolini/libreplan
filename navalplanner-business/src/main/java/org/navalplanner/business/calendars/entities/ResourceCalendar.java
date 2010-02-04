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

package org.navalplanner.business.calendars.entities;

import org.hibernate.validator.AssertTrue;
import org.hibernate.validator.NotNull;
import org.joda.time.LocalDate;
import org.navalplanner.business.resources.entities.Resource;

/**
 * Calendar for a {@link Resource}.
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 * @author Lorenzo Tilve Álvaro <ltilve@igalia.com>
 */
public class ResourceCalendar extends BaseCalendar {

    private Resource resource;

    private Integer capacity = 1;

    public Integer getCapacity() {
        if (capacity == null) {
            return 1;
        }
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public static ResourceCalendar create() {
        return create(new ResourceCalendar(CalendarData.create()));
    }

    /**
     * Constructor for hibernate. Do not use!
     */
    public ResourceCalendar() {
    }

    private ResourceCalendar(CalendarData calendarData) {
        super(calendarData);
        CalendarAvailability calendarAvailability = CalendarAvailability
                .create(new LocalDate(), null);
        addNewCalendarAvailability(calendarAvailability);
    }

    @Override
    public Integer getCapacityAt(LocalDate date) {
        if (!isActive(date)) {
            return 0;
        }
        return multiplyByCapacity(super.getCapacityAt(date));
    }

    @Override
    public boolean isActive(LocalDate date) {
        if (getCalendarAvailabilities().isEmpty()) {
            return true;
        }
        for (CalendarAvailability calendarAvailability : getCalendarAvailabilities()) {
            if (calendarAvailability.isActive(date)) {
                return true;
            }
        }
        return false;
    }

    private Integer multiplyByCapacity(Integer workableHours) {
        if (capacity == null) {
            return workableHours;
        }
        return capacity * workableHours;
    }

    @AssertTrue(message = "Capacity must be a positive integer number")
    public boolean checkCapacityPositiveIntegerNumber() {
        return (capacity >= 1);
    }

    @NotNull(message = "resource not specified")
    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

}
