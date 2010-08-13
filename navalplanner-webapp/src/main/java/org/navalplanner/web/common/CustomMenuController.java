/*
 * This file is part of NavalPlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                         Desenvolvemento Tecnolóxico de Galicia
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

import static org.navalplanner.web.I18nHelper._;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.navalplanner.business.users.entities.UserRole;
import org.navalplanner.web.security.SecurityUtils;
import org.zkoss.ganttz.util.IMenuItemsRegister;
import org.zkoss.ganttz.util.OnZKDesktopRegistry;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Vbox;

/**
 * Controller for customMenu <br />
 * @author Lorenzo Tilve Álvaro <ltilve@igalia.com>
 * @author Fernando Bellas Permuy <fbellas@udc.es>
 */
public class CustomMenuController extends Div implements IMenuItemsRegister {

    private List<CustomMenuItem> firstLevel;

    public static class CustomMenuItem {

        private final String name;
        private final String unencodedURL;
        private final String encodedURL;
        private final List<CustomMenuItem> children;
        private boolean activeParent;
        private String helpLink;
        private boolean disabled;

        public String getName() {
            return name;
        }

        public String getUrl() {
            return unencodedURL;
        }

        public String getEncodedUrl() {
            return encodedURL;
        }

        public List<CustomMenuItem> getChildren() {
            return children;
        }

        public boolean getHasChildren() {
            return !children.isEmpty();
        }

        public boolean getHasNotChildren() {
            return children.isEmpty();
        }

        public CustomMenuItem(String name, String url) {
            this(name, url, new ArrayList<CustomMenuItem>());
        }

        public CustomMenuItem(String name, String url, String helpLink) {
            this(name, url, new ArrayList<CustomMenuItem>());
            this.helpLink = helpLink;
        }

        public CustomMenuItem(String name, String url, boolean disabled) {
            this(name, url, new ArrayList<CustomMenuItem>());
            this.disabled = disabled;
        }

        public CustomMenuItem(String name, String url,
                List<CustomMenuItem> children) {
            this.name = name;
            this.unencodedURL = url;
            this.encodedURL = Executions.getCurrent().encodeURL(url);
            this.children = children;
            this.disabled = false;
            this.helpLink = "";
        }

        public void appendChildren(CustomMenuItem newChildren) {
            this.children.add(newChildren);
        }

        public boolean isActiveParent() {
            return activeParent;
        }

        public boolean isDisabled() {
            return disabled;
        }

        public boolean contains(String requestPath) {
            for (CustomMenuItem item : thisAndChildren()) {
                if (requestContains(requestPath, item.unencodedURL)) {
                    return true;
                }
            }
            return false;
        }

        private List<CustomMenuItem> thisAndChildren() {
            List<CustomMenuItem> items = new ArrayList<CustomMenuItem>();
            items.add(this);
            items.addAll(children);
            for (CustomMenuItem child : children) {
                items.addAll(child.children);
            }
            return items;
        }

        private static boolean requestContains(String requestPath, String url) {
            return requestPath.startsWith(url);
        }

        public void setActive(boolean activeParent) {
            this.activeParent = activeParent;
        }

        public void setHelpLink(String helpLink) {
            this.helpLink = helpLink;
        }

    }

    public CustomMenuController() {
        this.firstLevel = new ArrayList<CustomMenuItem>();
        initializeMenu();
        activateCurrentOne();
        getLocator().store(this);
    }

    private void activateCurrentOne() {
        String requestPath = Executions.getCurrent().getDesktop()
                .getRequestPath();
        for (CustomMenuItem ci : this.firstLevel) {
            if (ci.contains(requestPath)) {
                ci.setActive(true);
                for (CustomMenuItem child : ci.children) {
                    if (child.contains(requestPath)) {
                        child.setActive(true);
                        List<CustomMenuItem> test = child.children;
                        for (CustomMenuItem c : child.children) {
                            if (c.contains(requestPath)) {
                                c.setActive(true);
                                break;
                            }
                        }
                        break;
                    }
                }
                break;
            }
        }
    }

    private OnZKDesktopRegistry<IMenuItemsRegister> getLocator() {
        return OnZKDesktopRegistry.getLocatorFor(IMenuItemsRegister.class);
    }

    private CustomMenuController topItem(String name, String url,
            String helpUri,
            CustomMenuItem... items) {
        return topItem(name, url, helpUri, false, items);
    }

    private CustomMenuController topItem(String name, String url,
            String helpLink,
            boolean disabled, CustomMenuItem... items) {
        CustomMenuItem parent = new CustomMenuItem(name, url, disabled);
        parent.setHelpLink(helpLink);
        this.firstLevel.add(parent);
        for (CustomMenuItem child : items) {
            parent.appendChildren(child);
        }
        return this;
    }

    private CustomMenuItem subItem(String name, String url, String helpLink) {
        return new CustomMenuItem(name, url, helpLink);
    }

    private CustomMenuItem subItem(String name, String url, String helpLink,
            CustomMenuItem... children) {
        CustomMenuItem parent = subItem(name, url, helpLink);
        for (CustomMenuItem child : children) {
            parent.appendChildren(child);
        }
        return parent;
    }

    public void initializeMenu() {
        topItem(_("Scenarios"), "/scenarios/scenarios.zul", "",
            subItem(_("Scenarios Management"), "/scenarios/scenarios.zul",""),
            subItem(_("Transfer Projects Between Scenarios"), "/scenarios/transferOrders.zul", ""));

        topItem(_("Scheduling"), "/planner/index.zul", "",
            subItem(_("Projects Planning"), "/planner/index.zul;company_scheduling","01-introducion.html"),
            subItem(_("Resource Usage"),"/planner/index.zul;company_load","01-introducion.html#id1"),
            subItem(_("Projects"), "/planner/index.zul;orders_list","01-introducion.html#id2"),
            subItem(_("Limiting Resources Planning"),"/planner/index.zul;limiting_resources","01-introducion.html"),
            subItem(_("Project Templates"), "/templates/templates.zul", ""));

        topItem(_("Resources"), "/resources/worker/worker.zul", "",
            subItem(_("Workers"), "/resources/worker/worker.zul","05-recursos.html#xesti-n-de-traballadores"),
            subItem(_("Machines"), "/resources/machine/machines.zul","05-recursos.html#xesti-n-de-m-quinas"),
            subItem(_("Virtual Workers Groups"),"/resources/worker/virtualWorkers.zul","05-recursos.html#xesti-n-de-traballadores"),
            subItem(_("Work Reports"), "/workreports/workReport.zul", "09-partes.html#id3"),
            subItem(_("Companies"), "/externalcompanies/externalcompanies.zul",""),
            subItem(_("Subcontracting"), "/subcontract/subcontractedTasks.zul", "",
                subItem(_("Subcontracted Tasks"), "/subcontract/subcontractedTasks.zul", ""),
                subItem(_("Advance Report"), "/subcontract/reportAdvances.zul", "")));

        if (SecurityUtils.isUserInRole(UserRole.ROLE_ADMINISTRATION)) {
            topItem(_("Administration / Management"), "/advance/advanceTypes.zul", "",
                subItem(_("NavalPlan Configuration"), "/common/configuration.zul","03-calendarios.html#calendario-por-defecto"),
                subItem(_("Users"), "/users/users.zul","13-usuarios.html#administraci-n-de-usuarios",
                    subItem(_("Accounts"), "/users/users.zul","13-usuarios.html#administraci-n-de-usuarios"),
                    subItem(_("Profiles"), "/users/profiles.zul","13-usuarios.html#administraci-n-de-perfiles")),
                subItem(_("Calendars"),"/calendars/calendars.zul", "03-calendarios.html"),
                subItem(_("Materials"), "/materials/materials.zul", "11-materiales.html#administraci-n-de-materiais"),
                subItem(_("Quality Forms"),"/qualityforms/qualityForms.zul","12-formularios-calidad.html#administraci-n-de-formularios-de-calidade"),
                subItem(_("Cost Categories"),"/costcategories/costCategory.zul","14-custos.html#categor-as-de-custo"),
                subItem(_("Data Types"),"/advance/advanceTypes.zul", "04-avances.html#id1",
                    subItem(_("Advances"),"/advance/advanceTypes.zul", "04-avances.html#id1"),
                    subItem(_("Criteria"),"/resources/criterions/criterions-V2.zul","02-criterios.html#id1"),
                    subItem(_("Labels"), "/labels/labelTypes.zul","10-etiquetas.html"),
                    subItem(_("Units"), "/materials/unitTypes.zul", "11-materiales.html#administraci-n-de-materiais"),
                    subItem(_("Work Hours"),"/costcategories/typeOfWorkHours.zul","14-custos.html#administraci-n-de-horas-traballadas"),
                    subItem(_("Work Reports"),"/workreports/workReportTypes.zul","09-partes.html#id2")));
            }

        topItem(_("Reports"), "/reports/hoursWorkedPerWorkerReport.zul", "",
            subItem(_("Work Reports"), "/workreports/workReportQuery.zul", "09-partes.html#id4"),
            subItem(_("Hours Worked Per Resource"),"/reports/hoursWorkedPerWorkerReport.zul","15-informes.html"),
            subItem(_("Work And Progress Per Project"),"/reports/schedulingProgressPerOrderReport.zul", "15-informes.html"),
            subItem(_("Work And Progress Per Task"),"/reports/workingProgressPerTaskReport.zul", "15-informes.html"),
            subItem(_("Estimated/Planned Hours Per Task"),"/reports/completedEstimatedHoursPerTask.zul", "15-informes.html"),
            subItem(_("Project Costs Per Resource"),"/reports/orderCostsPerResource.zul", "15-informes.html"),
            subItem(_("Task Scheduling Status In Project"),"/reports/workingArrangementsPerOrderReport.zul","15-informes.html"),
            subItem(_("Materials Needs At Date"),"/reports/timeLineMaterialReport.zul","15-informes.html"));
    }

    private Vbox getRegisteredItemsInsertionPoint() {
        return (Vbox) getPage().getFellow("registeredItemsInsertionPoint");
    }

    public List<CustomMenuItem> getCustomMenuItems() {
        return this.firstLevel;
    }

    public List<CustomMenuItem> getBreadcrumbsPath() {
        List<CustomMenuItem> breadcrumbsPath = new ArrayList<CustomMenuItem>();
        for (CustomMenuItem ci : this.firstLevel) {
            if (ci.isActiveParent()) {
                if ((ci.name != null) && (ci.name != _("Scheduling"))) {
                    breadcrumbsPath.add(ci);
                    for (CustomMenuItem child : ci.children) {
                        if (child.isActiveParent()) {
                            breadcrumbsPath.add(child);
                            for (CustomMenuItem c : child.children) {
                                if (c.isActiveParent()) {
                                    breadcrumbsPath.add(c);
                                }
                            }
                        }
                    }
                }
            }
        }
        return breadcrumbsPath;
    }

    public String getHelpLink() {
        String helpLink = "index.html";
        for (CustomMenuItem ci : this.firstLevel) {
            if (ci.isActiveParent()) {
                if ((ci.name != null)) {
                    for (CustomMenuItem child : ci.children) {
                        if (child.isActiveParent()
                                && !child.helpLink.equals("")) {
                            helpLink = child.helpLink;
                        }
                    }
                }
            }
        }
        return helpLink;
    }

    public List<CustomMenuItem> getCustomMenuSecondaryItems() {
        for (CustomMenuItem ci : this.firstLevel) {
            if (ci.isActiveParent()) {
                return ci.getChildren();
            }
        }
        return Collections.<CustomMenuItem> emptyList();

    }

    private Button currentOne = null;

    @Override
    public Object addMenuItem(String name, String cssClass,
            org.zkoss.zk.ui.event.EventListener eventListener) {
        Vbox insertionPoint = getRegisteredItemsInsertionPoint();
        Button button = new Button();
        button.setLabel(_(name));
        if (cssClass != null) {
            toggleDomainCssClass(cssClass, button);
        }
        setDeselectedClass(button);
        button.addEventListener(Events.ON_CLICK, doNotCallTwice(button,
                eventListener));
        insertionPoint.appendChild(button);
        insertionPoint.appendChild(separator());
        return button;
    }

    @Override
    public void activateMenuItem(Object key) {
        switchCurrentButtonTo((Button) key);
    }

    @Override
    public void renameMenuItem(Object key, String name, String cssClass) {
        Button button = (Button) key;
        button.setLabel(name);
        if (cssClass != null) {
            toggleDomainCssClass(cssClass, button);
        }
    }

    private void toggleDomainCssClass(String cssClass, Button button) {
        Matcher matcher = perspectiveCssClass
                .matcher(button.getSclass() == null ? "" : button.getSclass());
        String previousPerspectiveClass;
        if (matcher.find()) {
            previousPerspectiveClass = matcher.group();
        } else {
            previousPerspectiveClass = "";
        }
        button.setSclass(previousPerspectiveClass + " " + cssClass);
    }

    @Override
    public void toggleVisibilityTo(Object key, boolean visible) {
        Button button = (Button) key;
        button.setVisible(visible);
        button.getNextSibling().setVisible(visible);
    }

    private void setSelectClass(final Button button) {
        togglePerspectiveClassTo(button, "perspective-active");
    }

    private void setDeselectedClass(Button button) {
        togglePerspectiveClassTo(button, "perspective");
    }

    private static final Pattern perspectiveCssClass = Pattern
    .compile("\\bperspective(-\\w+)?\\b");

    private void togglePerspectiveClassTo(final Button button,
            String newPerspectiveClass) {
        button
                .setSclass(togglePerspectiveCssClass(newPerspectiveClass,
                        button));
    }

    private String togglePerspectiveCssClass(String newPerspectiveClass,
            Button button) {
        String sclass = button.getSclass();
        if (!perspectiveCssClass.matcher(sclass).find()) {
            return newPerspectiveClass + " " + sclass;
        } else {
            Matcher matcher = perspectiveCssClass.matcher(sclass);
            return matcher.replaceAll(newPerspectiveClass);
        }
    }

    private EventListener doNotCallTwice(final Button button,
            final org.zkoss.zk.ui.event.EventListener originalListener) {
        return new EventListener() {

            @Override
            public void onEvent(Event event) throws Exception {
                if (currentOne == button) {
                    return;
                }
                switchCurrentButtonTo(button);
                originalListener.onEvent(event);
            }
        };
    }

    private Component separator() {
        Div div = new Div();
        div.setSclass("vertical-separator");
        return div;
    }

    public String getContextPath() {
        return Executions.getCurrent().getContextPath();
    }

    private void switchCurrentButtonTo(final Button button) {
        if (currentOne == button) {
            return;
        }
        if (currentOne != null) {
            setDeselectedClass(currentOne);
        }
        setSelectClass(button);
        currentOne = button;
    }


}
