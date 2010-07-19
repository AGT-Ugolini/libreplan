/*
 * This file is part of NavalPlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
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
import static org.navalplanner.web.planner.tabs.MultipleTabsPlannerController.PLANNIFICATION;

import java.util.HashMap;
import java.util.Map;

import org.navalplanner.business.orders.entities.Order;
import org.navalplanner.web.limitingresources.LimitingResourcesController;
import org.navalplanner.web.planner.tabs.CreatedOnDemandTab.IComponentCreator;
import org.zkoss.ganttz.extensions.ITab;
import org.zkoss.ganttz.resourceload.ResourcesLoadPanel.IToolbarCommand;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;

/**
 * @author Lorenzo Tilve Álvaro <ltilve@igalia.com>
 */
public class LimitingResourcesTabCreator {

    private static final String LIMITING_RESOURCES_VIEW = _("Limiting resources");

    /* Unnecesary */
    private static final String ORDER_LIMITING_RESOURCES_VIEW = _("Limiting resources (order)");

    public static ITab create(Mode mode,
            LimitingResourcesController LimitingResourcesController,
            IToolbarCommand upCommand,
            LimitingResourcesController LimitingResourcesControllerGlobal,
            Component breadcrumbs) {
        return new LimitingResourcesTabCreator(mode,
                LimitingResourcesController, LimitingResourcesControllerGlobal,
                breadcrumbs)
                .build();
    }

    private final Mode mode;
    private final LimitingResourcesController LimitingResourcesController;

    private final LimitingResourcesController LimitingResourcesControllerGlobal;

    private final Component breadcrumbs;

    private LimitingResourcesTabCreator(Mode mode,
            LimitingResourcesController LimitingResourcesController,
            LimitingResourcesController LimitingResourcesControllerGlobal,
            Component breadcrumbs) {
        this.mode = mode;
        this.LimitingResourcesController = LimitingResourcesController;
        this.LimitingResourcesControllerGlobal = LimitingResourcesControllerGlobal;
        this.breadcrumbs = breadcrumbs;
    }

    private ITab build() {
        return TabOnModeType.forMode(mode)
.forType(ModeType.GLOBAL,
                createGlobalLimitingResourcesTab()).forType(ModeType.ORDER,
                createOrderLimitingResourcesTab())
            .create();
    }

    private ITab createOrderLimitingResourcesTab() {
        IComponentCreator componentCreator = new IComponentCreator() {

            @Override
            /* Should never be called */
            public org.zkoss.zk.ui.Component create(
                    org.zkoss.zk.ui.Component parent) {
                Map<String, Object> arguments = new HashMap<String, Object>();
                // LimitingResourcesController.add(upCommand);
                arguments.put("LimitingResourcesController",
                        LimitingResourcesController);
                return Executions.createComponents(
                        "/limitingresources/_limitingresources.zul", parent,
                        arguments);
            }

        };
        return new CreatedOnDemandTab(ORDER_LIMITING_RESOURCES_VIEW,
                "order-limiting-resources",
                componentCreator) {

            @Override
            protected void afterShowAction() {
                breadcrumbs.getChildren().clear();
                breadcrumbs.appendChild(new Image(BREADCRUMBS_SEPARATOR));
                breadcrumbs.appendChild(new Label(PLANNIFICATION));
                breadcrumbs.appendChild(new Image(BREADCRUMBS_SEPARATOR));
                breadcrumbs
                        .appendChild(new Label(ORDER_LIMITING_RESOURCES_VIEW));
                breadcrumbs.appendChild(new Image(BREADCRUMBS_SEPARATOR));
                Order currentOrder = mode.getOrder();
                LimitingResourcesController.filterBy(currentOrder);
                LimitingResourcesController.reload();
                breadcrumbs.appendChild(new Label(currentOrder.getName()));
            }
        };
    }

    private ITab createGlobalLimitingResourcesTab() {

        final IComponentCreator componentCreator = new IComponentCreator() {

            @Override
            public org.zkoss.zk.ui.Component create(
                    org.zkoss.zk.ui.Component parent) {
                Map<String, Object> arguments = new HashMap<String, Object>();
                arguments.put("LimitingResourcesController",
                        LimitingResourcesControllerGlobal);
                return Executions.createComponents(
                        "/limitingresources/_limitingresources.zul", parent,
                        // "/resourceload/_resourceload.zul", parent,
                        arguments);
            }

        };
        return new CreatedOnDemandTab(LIMITING_RESOURCES_VIEW,
                "limiting-resources",
                componentCreator) {
            @Override
            protected void afterShowAction() {
                LimitingResourcesControllerGlobal.filterBy(null);
                LimitingResourcesControllerGlobal.reload();
                if (breadcrumbs.getChildren() != null) {
                    breadcrumbs.getChildren().clear();
                }
                breadcrumbs.appendChild(new Image(BREADCRUMBS_SEPARATOR));
                breadcrumbs.appendChild(new Label(PLANNIFICATION));
                breadcrumbs.appendChild(new Image(BREADCRUMBS_SEPARATOR));
                breadcrumbs.appendChild(new Label(LIMITING_RESOURCES_VIEW));
            }
        };
    }

}
