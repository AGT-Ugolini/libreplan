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

package org.zkoss.ganttz.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.HtmlNativeComponent;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.OpenEvent;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.Menupopup;
import org.zkoss.zul.impl.api.XulElement;

public class MenuBuilder<T extends XulElement> {

    public static <T extends XulElement> MenuBuilder<T> on(Page page,
            Collection<T> elements) {
        return new MenuBuilder<T>(page, elements);
    }

    public static <T extends XulElement> MenuBuilder<T> on(Page page,
            T... elements) {
        return on(page, Arrays.asList(elements));
    }

    public static interface ItemAction<T> {

        void onEvent(T choosen, Event event);
    }

    private class Item {
        private final String name;

        private final ItemAction<T> action;

        Item(String name, ItemAction<T> action) {
            this.name = name;
            this.action = action;
        }

        Menuitem createMenuItem() {
            Menuitem result = new Menuitem();
            result.setLabel(name);
            return result;
        }

    }

    private final List<T> elements;

    private final List<Item> items = new ArrayList<Item>();

    private Component root;

    private MenuBuilder(Page page, Collection<? extends T> elements) {
        this.elements = new ArrayList<T>(elements);
        this.root = page.getLastRoot();
    }

    public MenuBuilder<T> item(String name, ItemAction<T> itemAction) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        if (itemAction == null) {
            throw new IllegalArgumentException("itemAction cannot be null");
        }
        items.add(new Item(name, itemAction));
        return this;
    }

    private T referenced;

    public Menupopup createWithoutSettingContext() {
        return create(false);
    }

    public Menupopup create() {
        return create(true);
    }

    private Menupopup create(boolean setContext) {
        Menupopup result = new Menupopup();
        result.addEventListener("onOpen", new EventListener() {

            @Override
            public void onEvent(Event event) throws Exception {
                OpenEvent openEvent = (OpenEvent) event;
                referenced = (T) openEvent.getReference();
            }
        });
        for (final Item item : items) {
            Menuitem menuItem = item.createMenuItem();
            menuItem.addEventListener("onClick", new EventListener() {

                @Override
                public void onEvent(Event event) throws Exception {
                    ItemAction<T> action = item.action;
                    action.onEvent(referenced, event);
                }
            });
            result.appendChild(menuItem);
        }
        insertInRootComponent(result);
        if (setContext) {
            for (T element : elements) {
                element.setContext(result);
            }
        }
        return result;
    }

    private void insertInRootComponent(Menupopup result) {
        ArrayList<Component> children = new ArrayList<Component>(root
                .getChildren());
        Collections.reverse(children);
        // the Menupopup cannot be inserted after a HtmlNativeComponent, so we
        // try to avoid it
        if (children.isEmpty()) {
            root.appendChild(result);
        }
        for (Component child : children) {
            if (!(child instanceof HtmlNativeComponent)) {
                root.insertBefore(result, child);
                return;
            }
        }
        throw new RuntimeException("all children of " + root
                + " are html native");
    }
}
