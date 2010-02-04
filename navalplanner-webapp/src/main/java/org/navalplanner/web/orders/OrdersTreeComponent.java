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
package org.navalplanner.web.orders;

import static org.navalplanner.web.I18nHelper._;

import java.util.ArrayList;
import java.util.List;

import org.navalplanner.business.orders.entities.OrderElement;
import org.navalplanner.business.trees.ITreeNode;
import org.navalplanner.web.orders.OrderElementTreeController.OrderElementTreeitemRenderer;
import org.navalplanner.web.tree.TreeComponent;
import org.navalplanner.web.tree.TreeController;
import org.zkoss.zul.Treeitem;

/**
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 *
 */
public class OrdersTreeComponent extends TreeComponent {

    abstract class OrdersTreeColumn extends Column {
        OrdersTreeColumn(String label, String cssClass, String tooltip) {
            super(label, cssClass, tooltip);
        }

        OrdersTreeColumn(String label, String cssClass) {
            super(label, cssClass);
        }

        @Override
        public <T extends ITreeNode<T>> void doCell(
                TreeController<T>.Renderer renderer,
                Treeitem item, T currentElement) {
            OrderElementTreeitemRenderer treeRenderer = OrderElementTreeitemRenderer.class
                    .cast(renderer);
            doCell(treeRenderer, OrderElement.class.cast(currentElement));
        }

        protected abstract void doCell(
                OrderElementTreeitemRenderer treeRenderer,
                OrderElement currentElement);

    }

    public List<Column> getColumns() {
        List<Column> columns = new ArrayList<Column>();
        columns.add(schedulingStateColumn);
        columns.add(codeColumn);
        columns.add(new OrdersTreeColumn(_("Hours"), "hours",
                _("Total order element hours")) {

            @Override
            protected void doCell(OrderElementTreeitemRenderer treeRenderer,
                    OrderElement currentElement) {
                treeRenderer.addHoursCell(currentElement);
            }

        });
        columns.add(nameAndDescriptionColumn);
        columns.add(new OrdersTreeColumn(_("Must start after"),
                "estimated_init") {

            @Override
            protected void doCell(OrderElementTreeitemRenderer treeRenderer,
                    OrderElement currentElement) {
                treeRenderer.addInitDateCell(currentElement);
            }

        });
        columns.add(new OrdersTreeColumn(_("Deadline"), "estimated_end") {

            @Override
            protected void doCell(OrderElementTreeitemRenderer treeRenderer,
                    OrderElement currentElement) {
                treeRenderer.addEndDateCell(currentElement);
            }
        });
        columns.add(operationsColumn);
        return columns;
    }

    @Override
    public boolean isCreateFromTemplateEnabled() {
        return true;
    }
}
