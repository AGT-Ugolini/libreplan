/*
 * This file is part of ###PROJECT_NAME###
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
package org.navalplanner.web.planner.tabs;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.navalplanner.business.orders.entities.Order;

/**
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 *
 */
public class Mode {

    public static Mode initial() {
        return new Mode(new GlobalCase());
    }

    public static Mode forOrder(Order order) {
        return new Mode(new OrderCase(order));
    }

    public interface ModeTypeChangedListener {
        public void typeChanged(ModeType oldType, ModeType newType);
    }

    private List<ModeTypeChangedListener> listeners = new ArrayList<ModeTypeChangedListener>();

    public void addListener(ModeTypeChangedListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(ModeTypeChangedListener listener) {
        this.listeners.remove(listener);
    }

    private ModeCase current;

    Mode(ModeCase current) {
        Validate.notNull(current);
        this.current = current;
    }

    public Order getOrder() throws UnsupportedOperationException {
        return current.getOrder();
    }

    public void goToOrderMode(Order order) {
        changeTo(current.createOrderMode(order));
    }

    private void changeTo(ModeCase newCase) {
        if (current == newCase) {
            return;
        }
        ModeType previousType = current.getModeType();
        current = newCase;
        ModeType newType = current.getModeType();
        if (previousType != newType) {
            fireModeTypeChanged(previousType);
        }
    }

    private void fireModeTypeChanged(ModeType previousType) {
        Validate.notNull(previousType);
        ModeType newType = this.getType();
        Validate.notNull(newType);
        for (ModeTypeChangedListener listener : listeners) {
            listener.typeChanged(previousType, newType);
        }
    }

    public boolean isOf(ModeType type) {
        return current.getModeType() == type;
    }

    public void up() {
        changeTo(current.up());
    }

    public ModeType getType() {
        return current.getModeType();
    }

}

abstract class ModeCase {

    abstract Order getOrder() throws UnsupportedOperationException;

    abstract ModeType getModeType();

    abstract ModeCase up();

    abstract ModeCase createOrderMode(Order order);
}

class GlobalCase extends ModeCase {

    @Override
    ModeCase createOrderMode(Order order) {
        return new OrderCase(order);
    }

    @Override
    Order getOrder() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    ModeCase up() {
        return this;
    }

    @Override
    ModeType getModeType() {
        return ModeType.GLOBAL;
    }

}

class OrderCase extends ModeCase {

    private final Order order;

    public OrderCase(Order order) {
        Validate.notNull(order);
        this.order = order;
    }

    @Override
    ModeCase createOrderMode(Order order) {
        return new OrderCase(order);
    }

    @Override
    Order getOrder() throws UnsupportedOperationException {
        return order;
    }

    @Override
    public ModeType getModeType() {
        return ModeType.ORDER;
    }

    @Override
    ModeCase up() {
        return new GlobalCase();
    }

}
