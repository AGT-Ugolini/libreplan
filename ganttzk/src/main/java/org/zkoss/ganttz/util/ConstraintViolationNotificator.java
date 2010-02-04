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
package org.zkoss.ganttz.util;

import java.util.List;

import org.zkoss.ganttz.data.constraint.Constraint;
import org.zkoss.ganttz.data.constraint.Constraint.IConstraintViolationListener;
import org.zkoss.ganttz.util.WeakReferencedListeners.IListenerNotification;

/**
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 *
 */
public class ConstraintViolationNotificator<T> {

    public static <T> ConstraintViolationNotificator<T> create() {
        return new ConstraintViolationNotificator<T>();
    }

    private WeakReferencedListeners<IConstraintViolationListener<T>> constraintViolationListeners = WeakReferencedListeners
            .create();

    private IConstraintViolationListener<T> bridge = new IConstraintViolationListener<T>() {

        @Override
        public void constraintViolated(Constraint<T> constraint, T value) {
            fireConstraintViolated(constraint, value);
        }
    };

    public List<Constraint<T>> withListener(List<Constraint<T>> constraints) {
        for (Constraint<T> each : constraints) {
            withListener(each);
        }
        return constraints;
    }

    public Constraint<T> withListener(Constraint<T> constraint) {
        constraint.addConstraintViolationListener(bridge);
        return constraint;
    }

    private void fireConstraintViolated(final Constraint<T> constraint,
            final T value) {
        constraintViolationListeners
                .fireEvent(new IListenerNotification<IConstraintViolationListener<T>>() {

                    @Override
                    public void doNotify(
                            IConstraintViolationListener<T> listener) {
                        listener.constraintViolated(constraint, value);
                    }
                });
    }

    public void addConstraintViolationListener(
            IConstraintViolationListener<T> listener) {
        constraintViolationListeners.addListener(listener);
    }


}
