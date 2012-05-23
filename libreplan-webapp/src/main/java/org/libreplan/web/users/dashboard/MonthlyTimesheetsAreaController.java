/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2012 Igalia, S.L.
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

package org.libreplan.web.users.dashboard;

import java.util.List;

import javax.annotation.Resource;

import org.joda.time.LocalDate;
import org.libreplan.business.workreports.entities.WorkReport;
import org.libreplan.web.common.Util;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;

/**
 * Controller for "Monthly timesheets" area in the user dashboard window
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
@SuppressWarnings("serial")
public class MonthlyTimesheetsAreaController extends GenericForwardComposer {

    private IMonthlyTimesheetsAreaModel monthlyTimesheetsAreaModel;

    @Resource
    private IMonthlyTimesheetController monthlyTimesheetController;

    private RowRenderer monthlyTimesheetsRenderer = new RowRenderer() {

        @Override
        public void render(Row row, Object data) throws Exception {
            final MonthlyTimesheet monthlyTimesheet = (MonthlyTimesheet) data;
            row.setValue(monthlyTimesheet);

            Util.appendLabel(row, monthlyTimesheet.getDate().toString("MMMM y"));

            Util.appendOperationsAndOnClickEvent(row, new EventListener() {

                @Override
                public void onEvent(Event event) throws Exception {
                    monthlyTimesheetController.goToCreateOrEditForm(monthlyTimesheet
                            .getDate());
                }
            }, null);
        }

    };

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        comp.setAttribute("controller", this);
    }

    public List<MonthlyTimesheet> getMonthlyTimesheets() {
        return monthlyTimesheetsAreaModel.getMonthlyTimesheets();
    }

    public RowRenderer getMonthlyTimesheetsRenderer() {
        return monthlyTimesheetsRenderer;
    }

}

/**
 * Simple class to represent the monthly timesheets to be shown in the list.<br />
 *
 * This is only a simple class for the UI, everything will be saved using
 * {@link WorkReport}.
 */
class MonthlyTimesheet {
    private LocalDate date;

    /**
     * Only the year and month are used, the day is reseted to first day of the
     * month. As there's only one timesheet per month.
     */
    MonthlyTimesheet(LocalDate date) {
        this.date = new LocalDate(date.getYear(), date.getMonthOfYear(), 1);
    }

    public LocalDate getDate() {
        return date;
    }
}
