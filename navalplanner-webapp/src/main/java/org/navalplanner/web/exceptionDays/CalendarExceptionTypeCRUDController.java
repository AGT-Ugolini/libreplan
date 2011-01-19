/*
 * This file is part of NavalPlan
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
package org.navalplanner.web.exceptionDays;

import static org.navalplanner.web.I18nHelper._;

import java.util.ConcurrentModificationException;
import java.util.List;

import org.apache.commons.logging.LogFactory;
import org.navalplanner.business.calendars.entities.CalendarExceptionType;
import org.navalplanner.business.calendars.entities.Capacity;
import org.navalplanner.business.calendars.entities.PredefinedCalendarExceptionTypes;
import org.navalplanner.business.common.exceptions.InstanceNotFoundException;
import org.navalplanner.business.common.exceptions.ValidationException;
import org.navalplanner.web.common.IMessagesForUser;
import org.navalplanner.web.common.Level;
import org.navalplanner.web.common.MessagesForUser;
import org.navalplanner.web.common.OnlyOneVisible;
import org.navalplanner.web.common.Util;
import org.navalplanner.web.common.Util.Getter;
import org.navalplanner.web.common.Util.Setter;
import org.navalplanner.web.common.components.CapacityPicker;
import org.navalplanner.web.common.components.EffortDurationPicker;
import org.navalplanner.web.common.components.NewDataSortableGrid;
import org.springframework.beans.factory.annotation.Autowired;
import org.zkoss.util.InvalidValueException;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.CheckEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.MouseEvent;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

/**
 *
 * @author Diego Pino <dpino@igalia.com>
 *
 */
public class CalendarExceptionTypeCRUDController extends GenericForwardComposer {

    private static final org.apache.commons.logging.Log LOG = LogFactory
            .getLog(CalendarExceptionTypeCRUDController.class);

    @Autowired
    private ICalendarExceptionTypeModel calendarExceptionTypeModel;

    private Window listWindow;

    private Window editWindow;

    private Textbox tbName;

    private Textbox tbColor;

    private Checkbox overAssignable;

    private EffortDurationPicker standardEffort;

    private EffortDurationPicker extraEffort;

    private OnlyOneVisible visibility;

    private IMessagesForUser messagesForUser;

    private Component messagesContainer;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        messagesForUser = new MessagesForUser(messagesContainer);
        comp.setVariable("controller", this, true);
        initializeEditWindowComponents();
        showListWindow();
    }

    private void initializeCapacityPicker() {
        final CalendarExceptionType exceptionType = getExceptionDayType();
        CapacityPicker.workWith(overAssignable, standardEffort, extraEffort,
                new Getter<Capacity>() {

                    @Override
                    public Capacity get() {
                        return exceptionType.getCapacity();
                    }
                }, new Setter<Capacity>() {

                    @Override
                    public void set(Capacity value) {
                        exceptionType.setCapacity(value);
                    }
                });
    }

    private void initializeEditWindowComponents() {
        tbName = (Textbox) editWindow.getFellowIfAny("tbName");
        tbColor = (Textbox) editWindow.getFellowIfAny("tbColor");
        overAssignable = Util.findComponentAt(editWindow, "overAssignable");
        standardEffort = Util.findComponentAt(editWindow, "standardEffort");
        extraEffort = Util.findComponentAt(editWindow, "extraEffort");
    }

    private void showListWindow() {
        showWindow(listWindow);
    }

    private void showWindow(Window window) {
        getVisibility().showOnly(window);
    }

    private OnlyOneVisible getVisibility() {
        if (visibility == null) {
            visibility = new OnlyOneVisible(listWindow, editWindow);
        }
        return visibility;
    }

    private void showEditWindow() {
        initializeCapacityPicker();
        editWindow.setTitle(_("Edit Exception Day Type"));
        showWindow(editWindow);
    }

    public void goToCreateForm() {
        calendarExceptionTypeModel.initCreate();
        showCreateWindow();
        Util.reloadBindings(editWindow);
    }

    private void showCreateWindow() {
        initializeCapacityPicker();
        editWindow.setTitle(_("Create Exception Day Type"));
        showWindow(editWindow);
    }

    public CalendarExceptionType getExceptionDayType() {
        return calendarExceptionTypeModel.getExceptionDayType();
    }

    public List<CalendarExceptionType> getExceptionDayTypes() {
        return calendarExceptionTypeModel.getExceptionDayTypes();
    }

    public void cancel() {
        clearFields();
        showListWindow();
    }

    private void clearFields() {
        tbName.setRawValue("");
        tbColor.setRawValue("");
    }

    private boolean save() {
        try {
            calendarExceptionTypeModel.confirmSave();
            messagesForUser.showMessage(Level.INFO, _("Calendar Exception Type saved"));
            return true;
        } catch (ValidationException e) {
            messagesForUser.showInvalidValues(e);
            return false;
        }
    }

    public void saveAndExit() {
        boolean couldSave = save();
        if (couldSave) {
            clearFields();
            showListWindow();
            Util.reloadBindings(listWindow);
        }
    }

    public void saveAndContinue() {
        boolean couldSave = save();
        if (couldSave) {
            calendarExceptionTypeModel.initEdit(calendarExceptionTypeModel
                .getExceptionDayType());
        }
    }

    public void showRemoveConfirmationMessage(MouseEvent event) {
        Button button = (Button) event.getTarget();
        Component comp = (Component) event.getTarget();
        CalendarExceptionType exceptionType = (CalendarExceptionType) ((Row) button
                .getParent().getParent()).getValue();

        if (PredefinedCalendarExceptionTypes.contains(exceptionType)) {
            throw new WrongValueException(comp, "Cannot remove a predefined Exception Day Type");
        } else {
            showRemoveConfirmationMessage(exceptionType);
        }
    }

    public void showRemoveConfirmationMessage(
            CalendarExceptionType exceptionType) {
        try {
            int status = Messagebox
                    .show(_("Delete item {0}. Are you sure?",
                            exceptionType.getName()), _("Delete"),
                            Messagebox.OK | Messagebox.CANCEL,
                            Messagebox.QUESTION);
            if (Messagebox.OK == status) {
                confirmDelete(exceptionType);
                Util.reloadBindings(listWindow);
            }
        } catch (InterruptedException e) {
            LOG.error(_("Error on showing delete confirm"), e);
        }
    }

    public void confirmDelete(CalendarExceptionType exceptionType) {
        try {
            calendarExceptionTypeModel.confirmDelete(exceptionType);
        } catch (InstanceNotFoundException e) {
            e.printStackTrace();
        } catch (InvalidValueException e) {
            NewDataSortableGrid listExceptionDayTypes = (NewDataSortableGrid) listWindow
                    .getFellowIfAny("listExceptionDayTypes");
            Row row = findRowByValue(listExceptionDayTypes, exceptionType);
            throw new WrongValueException(row, e.getMessage());
        }
    }

    private Row findRowByValue(Grid grid, Object value) {
        final List<Row> rows = grid.getRows().getChildren();
        for (Row row: rows) {
            if (row.getValue().equals(value)) {
                return row;
            }
        }
        return null;
    }

    public void goToEditForm(CalendarExceptionType exceptionType) {
        calendarExceptionTypeModel.initEdit(exceptionType);
        showEditWindow();
        Util.reloadBindings(editWindow);
    }

    public void onCheckGenerateCode(Event e) {
        CheckEvent ce = (CheckEvent) e;
        if (ce.isChecked()) {
            // we have to auto-generate the code for new objects
            try {
                calendarExceptionTypeModel.setCodeAutogenerated(ce.isChecked());
            } catch (ConcurrentModificationException err) {
                messagesForUser.showMessage(Level.ERROR, err.getMessage());
            }
        }
        Util.reloadBindings(editWindow);
    }

}
