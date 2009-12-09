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

import static org.navalplanner.web.I18nHelper._;
import static org.navalplanner.web.planner.tabs.MultipleTabsPlannerController.BREADCRUMBS_SEPARATOR;

import java.util.HashMap;
import java.util.Map;

import org.navalplanner.web.common.Util;
import org.navalplanner.web.orders.OrderCRUDController;
import org.navalplanner.web.planner.order.IOrderPlanningGate;
import org.navalplanner.web.planner.tabs.CreatedOnDemandTab.IComponentCreator;
import org.zkoss.ganttz.extensions.ITab;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;

/**
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 *
 */
public class OrdersTabCreator {

    private static final String ORDERS_VIEW = _("Orders List");

    private static final String ORDER_ORDERS_VIEW = _("Order Details");

    public static ITab create(Mode mode,
            OrderCRUDController orderCRUDController, Component breadcrumbs,
            IOrderPlanningGate orderPlanningGate) {
        return new OrdersTabCreator(mode, orderCRUDController, breadcrumbs,
                orderPlanningGate)
                .build();
    }

    private IComponentCreator ordersTabCreator = new IComponentCreator() {

        private org.zkoss.zk.ui.Component result;

        @Override
        public org.zkoss.zk.ui.Component create(org.zkoss.zk.ui.Component parent) {
            if (result != null) {
                return result;
            }
            Map<String, Object> args = new HashMap<String, Object>();
            args.put("orderController", setupOrderCrudController());
            result = Executions.createComponents("/orders/_ordersTab.zul",
                    parent, args);
            Util.createBindingsFor(result);
            Util.reloadBindings(result);
            return result;
        }

    };

    private final Mode mode;

    private final OrderCRUDController orderCRUDController;

    private final Component breadcrumbs;

    private final IOrderPlanningGate orderPlanningGate;

    private OrdersTabCreator(Mode mode,
            OrderCRUDController orderCRUDController, Component breadcrumbs,
            IOrderPlanningGate orderPlanningGate) {
        this.mode = mode;
        this.orderCRUDController = orderCRUDController;
        this.breadcrumbs = breadcrumbs;
        this.orderPlanningGate = orderPlanningGate;
    }

    private ITab build() {
        return TabOnModeType.forMode(mode).forType(ModeType.GLOBAL,
                createGlobalOrdersTab()).forType(ModeType.ORDER,
                createOrderOrdersTab()).create();
    }

    private ITab createGlobalOrdersTab() {
        return new CreatedOnDemandTab(ORDERS_VIEW, "orders", ordersTabCreator) {
            @Override
            protected void afterShowAction() {
                orderCRUDController.goToList();
                if (breadcrumbs.getChildren() != null) {
                    breadcrumbs.getChildren().clear();
                }
                breadcrumbs.appendChild(new Image(BREADCRUMBS_SEPARATOR));
                breadcrumbs.appendChild(new Label(PlanningTabCreator.ENTERPRISE_VIEW));
                breadcrumbs.appendChild(new Image(BREADCRUMBS_SEPARATOR));
                breadcrumbs.appendChild(new Label(ORDER_ORDERS_VIEW));
            }
        };
    }

    private OrderCRUDController setupOrderCrudController() {
        orderCRUDController.setPlanningControllerEntryPoints(orderPlanningGate);
        orderCRUDController.setActionOnUp(new Runnable() {
            public void run() {
                mode.up();
                orderCRUDController.goToList();
            }
        });
        return orderCRUDController;
    }

    private ITab createOrderOrdersTab() {
        return new CreatedOnDemandTab(ORDER_ORDERS_VIEW, "order-data",
                ordersTabCreator) {
            @Override
            protected void afterShowAction() {
                breadcrumbs.getChildren().clear();
                breadcrumbs.appendChild(new Image(BREADCRUMBS_SEPARATOR));
                breadcrumbs.appendChild(new Label(ORDER_ORDERS_VIEW));
                breadcrumbs.appendChild(new Image(BREADCRUMBS_SEPARATOR));
                if (mode.isOf(ModeType.ORDER)) {
                    orderCRUDController.goToEditForm(mode.getOrder());
                    breadcrumbs
                            .appendChild(new Label(mode.getOrder().getName()));
                }

            }
        };
    }

}
