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

package org.navalplanner.web.common;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.impl.InputElement;

/**
 * Class for checking if a component is completely valid (checks all constraints within a component)
 *
 * @author Diego Pino García <dpino@igalia.com>
 */
public class ConstraintChecker {

    @SuppressWarnings("unchecked")
    public static boolean isValid(Component component) {
        return checkIsValid(component) && checkIsValid(component.getChildren());
    }

    private static boolean checkIsValid(List<Component> components) {
        boolean result = true;
        for (Component component: components) {
            result &= isValid(component);
        }
        return result;
    }

    private static boolean checkIsValid(Component child) {
        if (child instanceof Textbox) {
            return textboxIsValid((Textbox) child);
        }
        if (child instanceof Datebox) {
            return dateboxIsValid((Datebox) child);
        }
        return true;
    }

    private static boolean textboxIsValid(Textbox component) {
        inputElementIsValid((InputElement) component);
        // Forces constraint to be checked
        component.getValue();
        return (component.isValid());
    }

    private static void inputElementIsValid(InputElement component) {
        if (!component.isValid()) {
            final String errorMessage = component.getErrorMessage();
            if (errorMessage != null) {
                throw new WrongValueException(component, component
                        .getErrorMessage());
            }
        }
    }

    private static boolean dateboxIsValid(Datebox component) {
        inputElementIsValid((InputElement) component);
        component.getValue();
        return (component.isValid());
    }

}
