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

package org.navalplanner.web.orders;

import static org.navalplanner.web.I18nHelper._;

import java.text.SimpleDateFormat;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.logging.LogFactory;
import org.hibernate.validator.InvalidValue;
import org.navalplanner.business.advance.exceptions.DuplicateAdvanceAssignmentForOrderElementException;
import org.navalplanner.business.advance.exceptions.DuplicateValueTrueReportGlobalAdvanceException;
import org.navalplanner.business.calendars.entities.BaseCalendar;
import org.navalplanner.business.common.exceptions.ValidationException;
import org.navalplanner.business.externalcompanies.entities.ExternalCompany;
import org.navalplanner.business.orders.entities.HoursGroup;
import org.navalplanner.business.orders.entities.Order;
import org.navalplanner.business.orders.entities.OrderElement;
import org.navalplanner.business.orders.entities.OrderStatusEnum;
import org.navalplanner.business.templates.entities.OrderTemplate;
import org.navalplanner.business.users.entities.UserRole;
import org.navalplanner.web.common.IMessagesForUser;
import org.navalplanner.web.common.Level;
import org.navalplanner.web.common.MessagesForUser;
import org.navalplanner.web.common.OnlyOneVisible;
import org.navalplanner.web.common.Util;
import org.navalplanner.web.common.components.bandboxsearch.BandboxMultipleSearch;
import org.navalplanner.web.common.components.bandboxsearch.BandboxSearch;
import org.navalplanner.web.common.components.finders.FilterPair;
import org.navalplanner.web.orders.assigntemplates.TemplateFinderPopup;
import org.navalplanner.web.orders.assigntemplates.TemplateFinderPopup.IOnResult;
import org.navalplanner.web.orders.criterionrequirements.AssignedCriterionRequirementToOrderElementController;
import org.navalplanner.web.orders.labels.AssignedLabelsToOrderElementController;
import org.navalplanner.web.orders.labels.LabelsAssignmentToOrderElementComponent;
import org.navalplanner.web.orders.materials.AssignedMaterialsToOrderElementController;
import org.navalplanner.web.orders.materials.OrderElementMaterialAssignmentsComponent;
import org.navalplanner.web.planner.order.IOrderPlanningGate;
import org.navalplanner.web.security.SecurityUtils;
import org.navalplanner.web.templates.IOrderTemplatesControllerEntryPoints;
import org.navalplanner.web.tree.TreeComponent;
import org.navalplanner.web.users.OrderAuthorizationController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.SelectEvent;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.ComboitemRenderer;
import org.zkoss.zul.Constraint;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.api.Listbox;
import org.zkoss.zul.api.Window;

/**
 * Controller for CRUD actions <br />
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 */
@org.springframework.stereotype.Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class OrderCRUDController extends GenericForwardComposer {

    private final class LabelCreatorForInvalidValues implements
            IMessagesForUser.ICustomLabelCreator {
        @Override
        public Component createLabelFor(
                InvalidValue invalidValue) {
            if (invalidValue.getBean() instanceof OrderElement) {
                Label result = new Label();

                String orderElementName;
                if (invalidValue.getBean() instanceof Order) {
                    orderElementName = _("Order");
                } else {
                    orderElementName = ((OrderElement) invalidValue
                            .getBean()).getName();
                }

                result.setValue(orderElementName + " "
                        + invalidValue.getPropertyName() + ": "
                        + invalidValue.getMessage());
                return result;
            } else if (invalidValue.getBean() instanceof HoursGroup) {
                Label result = new Label();
                HoursGroup hoursGroup = (HoursGroup) invalidValue.getBean();
                result.setValue(_("Hours Group at ")
                        + getParentName(hoursGroup) + ". "
                        + invalidValue.getPropertyName() + ": "
                        + invalidValue.getMessage());
                return result;
            }else {
                return MessagesForUser.createLabelFor(invalidValue);
            }
        }

        private String getParentName(HoursGroup hoursGroup) {
            return (hoursGroup.getParentOrderLine() != null) ? hoursGroup
                    .getParentOrderLine().getName() : hoursGroup
                    .getOrderLineTemplate().getName();
        }
    }

    private static final org.apache.commons.logging.Log LOG = LogFactory
            .getLog(OrderCRUDController.class);

    @Autowired
    private IOrderModel orderModel;

    private IMessagesForUser messagesForUser;

    private Component messagesContainer;

    private TemplateFinderPopup templateFinderPopup;

    public void createOrderFromTemplate() {
        templateFinderPopup.openForOrderCreation(createOrderFromTemplateButton,
                "after_start", new IOnResult<OrderTemplate>() {

                    @Override
                    public void found(OrderTemplate template) {
                        showOrderElementFilter();
                        showCreateButtons(false);
                        orderModel.prepareCreationFrom(template);
                        showEditWindow(_("Create order from Template"));
                    }
                });
    }

    @Resource
    private IOrderTemplatesControllerEntryPoints orderTemplates;

    private Window editWindow;

    private Window listWindow;

    private Tab selectedTab;

    private Grid listing;

    private Hbox orderFilter;

    private Vbox orderElementFilter;

    private Button createOrderButton;
    private Button createOrderFromTemplateButton;
    private Button saveOrderAndContinueButton;

    private Datebox filterStartDate;

    private Datebox filterFinishDate;

    private BandboxMultipleSearch bdFilters;

    private Checkbox checkIncludeOrderElements;

    private BandboxSearch bdExternalCompanies;

    private OnlyOneVisible cachedOnlyOneVisible;

    private IOrderPlanningGate planningControllerEntryPoints;

    private BaseCalendarsComboitemRenderer baseCalendarsComboitemRenderer = new BaseCalendarsComboitemRenderer();

    private OrdersRowRenderer ordersRowRenderer = new OrdersRowRenderer();

    private OrderElementTreeController orderElementTreeController;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        messagesForUser = new MessagesForUser(messagesContainer);
        comp.setVariable("controller", this, true);

        if(SecurityUtils.isUserInRole(UserRole.ROLE_CREATE_ORDER)) {
            createOrderButton.setDisabled(false);
            createOrderFromTemplateButton.setDisabled(false);
        }

        // Configuration of the order filter
        Component filterComponent = Executions.createComponents(
                "/orders/_orderFilter.zul", orderFilter,
                new HashMap<String, String>());
        filterComponent.setVariable("orderFilterController", this, true);
        filterStartDate = (Datebox) filterComponent
                .getFellow("filterStartDate");
        filterFinishDate = (Datebox) filterComponent
                .getFellow("filterFinishDate");
        bdFilters = (BandboxMultipleSearch) filterComponent
                .getFellow("bdFilters");
        checkIncludeOrderElements = (Checkbox) filterComponent
                .getFellow("checkIncludeOrderElements");
    }

    private void addEditWindowIfNeeded() {
        if (editWindow != null) {
            return;
        }
        Map<String, Object> editWindowArgs = new HashMap<String, Object>();
        editWindowArgs.put("top_id", editWindowArgs);
        Component parent = listWindow.getParent();
        listWindow.setVisible(false);
        cachedOnlyOneVisible = null;
        editWindow = (Window) Executions.createComponents(
                "/orders/_edition.zul",
                parent, editWindowArgs);
        bindListOrderStatusSelectToOnStatusChange();
        Map<String, Object> editOrderElementArgs = new HashMap<String, Object>();
        editOrderElementArgs.put("top_id", "editOrderElement");
        Component editOrderElement = Executions.createComponents(
                "/orders/_editOrderElement.zul",
                parent, editOrderElementArgs);

        Util.createBindingsFor(editWindow);
        Util.reloadBindings(editWindow);
        Util.createBindingsFor(editOrderElement);
        Util.reloadBindings(editOrderElement);
    }

    private void bindListOrderStatusSelectToOnStatusChange() {
        Listbox listOrderStatus = (Listbox) editWindow
                .getFellow("listOrderStatus");
        listOrderStatus.addEventListener(Events.ON_SELECT, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                updateDisabilitiesOnInterface();
            }
        });
    }

    public void setupOrderElementTreeController() throws Exception {
        if (!confirmLastTab())
            return;
        setCurrentTab();

        if (orderElementTreeController == null) {
            OrderElementController orderElementController = new OrderElementController();
            orderElementController.doAfterCompose(self
                    .getFellow("editOrderElement"));
            orderElementTreeController = new OrderElementTreeController(
                    orderModel, orderElementController, messagesForUser);

            TreeComponent orderElementsTree = (TreeComponent) editWindow
                    .getFellow("orderElementTree");
            orderElementTreeController.setTreeComponent(orderElementsTree);
            orderElementsTree.useController(orderElementTreeController);
            orderElementTreeController.setReadOnly(readOnly);
            redraw(orderElementsTree);
        }
    }

    /*
     * Operations to do before to change the selected tab
     */
    private boolean confirmLastTab() {
        if (getCurrentTab() != null) {
            // Confirm advances tab.
            if (getCurrentTab().getId().equals("tabAdvances")) {
                if (!manageOrderElementAdvancesController.save()) {
                    resetSelectedTab();
                    selectTab("tabAdvances");
                    return false;
                }
            }
        }
        return true;
    }

    private void redraw(Component comp) {
        Util.createBindingsFor(comp);
        Util.reloadBindings(comp);
    }

    private IOrderElementModel getOrderElementModel() {
        final Order order = (Order) orderModel.getOrder();
        return orderModel.getOrderElementModel(order);
    }

    private AssignedHoursToOrderElementController assignedHoursController;

    public void setupAssignedHoursToOrderElementController() throws Exception {
        if (!confirmLastTab())
            return;
        setCurrentTab();

        if (assignedHoursController == null) {
            Component orderElementHours = editWindow
                    .getFellowIfAny("orderElementHours");
            assignedHoursController = (AssignedHoursToOrderElementController) orderElementHours
                    .getVariable("assignedHoursToOrderElementController", true);

            final IOrderElementModel orderElementModel = getOrderElementModel();
            assignedHoursController.openWindow(orderElementModel);
        }
    }

    private ManageOrderElementAdvancesController manageOrderElementAdvancesController;

    public void setupManageOrderElementAdvancesController() throws Exception {
        if (!confirmLastTab())
            return;
        setCurrentTab();

        Component orderElementAdvances = editWindow
                .getFellowIfAny("orderElementAdvances");
        if (manageOrderElementAdvancesController == null) {
            final IOrderElementModel orderElementModel = getOrderElementModel();
            manageOrderElementAdvancesController = (ManageOrderElementAdvancesController) orderElementAdvances
                    .getVariable("manageOrderElementAdvancesController", true);
            manageOrderElementAdvancesController.openWindow(orderElementModel);
        } else {
            manageOrderElementAdvancesController.refreshChangesFromOrderElement();
            manageOrderElementAdvancesController.createAndLoadBindings();
            manageOrderElementAdvancesController.refreshSelectedAdvance();
        }
    }

    private AssignedLabelsToOrderElementController assignedLabelsController;

    public void setupAssignedLabelsToOrderElementController()
    throws Exception {
        if (!confirmLastTab())
            return;
        setCurrentTab();

        if (assignedLabelsController == null) {
            LabelsAssignmentToOrderElementComponent labelsAssignment = (LabelsAssignmentToOrderElementComponent) editWindow
                .getFellow("orderElementLabels");
            assignedLabelsController = labelsAssignment.getController();

            final IOrderElementModel orderElementModel = getOrderElementModel();
            assignedLabelsController.openWindow(orderElementModel);
        }
    }

    private AssignedCriterionRequirementToOrderElementController assignedCriterionRequirementController;

    public void setupAssignedCriterionRequirementsToOrderElementController()
            throws Exception {
        if (!confirmLastTab())
            return;
        setCurrentTab();

        if (assignedCriterionRequirementController == null) {
            Component orderElementCriterionRequirements = editWindow
                .getFellowIfAny("orderElementCriterionRequirements");
            assignedCriterionRequirementController = (AssignedCriterionRequirementToOrderElementController) orderElementCriterionRequirements
                .getVariable("assignedCriterionRequirementController", true);

            final IOrderElementModel orderElementModel = getOrderElementModel();
            assignedCriterionRequirementController
                    .openWindow(orderElementModel);
        } else {
            reloadHoursGroupOrder();
        }
    }

    private AssignedMaterialsToOrderElementController assignedMaterialsController;

    public void setupAssignedMaterialsToOrderElementController()
            throws Exception {
        if (!confirmLastTab())
            return;
        setCurrentTab();

        if (assignedMaterialsController == null) {
            OrderElementMaterialAssignmentsComponent assignmentsComponent = (OrderElementMaterialAssignmentsComponent) editWindow
                .getFellowIfAny("orderElementMaterials");
            assignedMaterialsController = assignmentsComponent.getController();

            final IOrderElementModel orderElementModel = getOrderElementModel();
            assignedMaterialsController.openWindow(orderElementModel
                    .getOrderElement());
        }
    }

    private AssignedTaskQualityFormsToOrderElementController assignedTaskQualityFormController;

    public void setupAssignedTaskQualityFormsToOrderElementController() throws Exception {
        if (!confirmLastTab())
            return;
        setCurrentTab();

        Component orderElementTaskQualityForms = editWindow
                .getFellowIfAny("orderElementTaskQualityForms");
        if (assignedTaskQualityFormController == null) {
            assignedTaskQualityFormController = (AssignedTaskQualityFormsToOrderElementController) orderElementTaskQualityForms
                .getVariable("assignedTaskQualityFormsController", true);
            final IOrderElementModel orderElementModel = getOrderElementModel();
            assignedTaskQualityFormController.openWindow(orderElementModel);
        } else {
            Util.createBindingsFor(orderElementTaskQualityForms);
            Util.reloadBindings(orderElementTaskQualityForms);
        }
    }

    private OrderAuthorizationController orderAuthorizationController;

    public void setupOrderAuthorizationController() {
        if (!confirmLastTab())
            return;
        setCurrentTab();

        Component orderElementAuthorizations = editWindow
                .getFellowIfAny("orderElementAuthorizations");
        if (orderAuthorizationController == null) {
            orderAuthorizationController = (OrderAuthorizationController) orderElementAuthorizations
                    .getVariable("orderAuthorizationController", true);
            orderAuthorizationController
                    .setMessagesForUserComponent(messagesForUser);
            initOrderAuthorizations();
        } else {
            Util.createBindingsFor(orderElementAuthorizations);
            Util.reloadBindings(orderElementAuthorizations);
        }
    }

    private void initOrderAuthorizations() {
        Component orderElementAuthorizations = editWindow
                .getFellowIfAny("orderElementAuthorizations");
        final Order order = (Order) orderModel.getOrder();
        if (order.isNewObject()) {
            orderAuthorizationController.initCreate(order);
        } else {
            orderAuthorizationController.initEdit(order);
        }
        Util.createBindingsFor(orderElementAuthorizations);
        Util.reloadBindings(orderElementAuthorizations);
    }

    private void saveOrderAuthorizations() {
        setupOrderAuthorizationController();
        orderAuthorizationController.save();
    }

    public List<Order> getOrders() {
        return orderModel.getOrders();
    }

    private OnlyOneVisible getVisibility() {
        if (cachedOnlyOneVisible == null) {
            cachedOnlyOneVisible = new OnlyOneVisible(listWindow);
        }
        return cachedOnlyOneVisible;
    }

    public Order getOrder() {
        return (Order) orderModel.getOrder();
    }

    public void saveAndContinue() {
        Order order = (Order) orderModel.getOrder();
        final boolean isNewObject = order.isNewObject();
        setCurrentTab();
        Tab previousTab = getCurrentTab();
        final boolean couldSave = save();
        if (couldSave) {
            if(orderModel.userCanRead(order, SecurityUtils.getSessionUserLoginName())) {
                orderModel.initEdit(order);
                initialStatus = order.getState();
                updateDisabilitiesOnInterface();
                initializeTabs();
                showWindow(editWindow);

                // come back to the current tab after initialize all tabs.
                resetSelectedTab();
                selectTab(previousTab.getId());
                Events.sendEvent(new SelectEvent(Events.ON_SELECT, previousTab,
                        null));

                if (isNewObject) {
                    this.planningControllerEntryPoints.goToOrderDetails(order);
                }

            }
            else {
                try {
                    Messagebox.show(_("You don't have read access to this order"),
                            _("Information"), Messagebox.OK, Messagebox.INFORMATION);
                    goToList();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void saveAndExit() {
        setCurrentTab();
        final boolean couldSave = save();
        if (couldSave) {
            goToList();
        }
    }

    private boolean save() {
        if (manageOrderElementAdvancesController != null) {
            selectTab("tabAdvances");
            if (!manageOrderElementAdvancesController.save()) {
                setCurrentTab();
                return false;
            }
        }
        if (assignedCriterionRequirementController != null) {
            selectTab("tabRequirements");
            if (!assignedCriterionRequirementController.close()) {
                setCurrentTab();
                return false;
            }
        }
        if (assignedTaskQualityFormController != null) {
            selectTab("tabTaskQualityForm");
            if (!assignedTaskQualityFormController.confirm()) {
                setCurrentTab();
                return false;
        }
        }

        createPercentageAdvances();

        // come back to the default tab.
        if (getCurrentTab() != null) {
            selectTab(getCurrentTab().getId());
        }

        try {
            orderModel.save();
            saveOrderAuthorizations();

            try {
                Messagebox.show(_("Order saved"), _("Information"),
                        Messagebox.OK, Messagebox.INFORMATION);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return true;
        } catch (ValidationException e) {
            messagesForUser.showInvalidValues(e, new LabelCreatorForInvalidValues());
        }
        setCurrentTab();
        return false;
    }

    Tab tabGeneralData;

    private void createPercentageAdvances() {

        try {
            if (manageOrderElementAdvancesController == null) {
                Component orderElementAdvances = editWindow
                        .getFellowIfAny("orderElementAdvances");
                manageOrderElementAdvancesController = (ManageOrderElementAdvancesController) orderElementAdvances
                        .getVariable("manageOrderElementAdvancesController",
                                true);
            }
            manageOrderElementAdvancesController
                    .createPercentageAdvances(getOrderElementModel());
        } catch (DuplicateAdvanceAssignmentForOrderElementException e) {
            messagesForUser
                    .showMessage(
                            Level.ERROR,
                            _("cannot include an Advance of the same Advance type twice"));
        } catch (DuplicateValueTrueReportGlobalAdvanceException e) {
            messagesForUser
                    .showMessage(
                            Level.ERROR,
                            _("spread values are not valid, at least one value should be true"));
        } catch (Exception e) {
            messagesForUser
                    .showMessage(
                            Level.ERROR,
                            _("incorrect initialization of the advance assignment controller."));
        }
    }

    private void selectDefaultTab() {
        selectTab("tabGeneralData");
    }

    private void resetSelectedTab() {
        selectedTab = null;
    }

    private void setCurrentTab() {
        Tabbox tabboxOrder = (Tabbox) editWindow.getFellowIfAny("tabboxOrder");
        if (tabboxOrder != null) {
            selectedTab = tabboxOrder.getSelectedTab();
        }
    }

    private Tab getCurrentTab() {
        return selectedTab;
    }

    private void selectTab(String str) {
        Tab tab = (Tab) editWindow.getFellowIfAny(str);
        if (tab != null) {
            tab.setSelected(true);
        }
    }

    public void goToList() {
        loadComponents();
        showWindow(listWindow);
    }

    private void loadComponents() {
        // load the components of the order list
        listing = (Grid) listWindow.getFellow("listing");
        showOrderFilter();
        showCreateButtons(true);
        clearFilterDates();
    }

    public void reloadHoursGroupOrder() {
        if (getCurrentTab().getId().equals("tabRequirements")) {
            assignedCriterionRequirementController.reload();
        }
    }

    private void showWindow(Window window) {
        getVisibility().showOnly(window);
        Util.reloadBindings(window);
    }

    public void cancel() {
        goToList();
    }

    public void up() {
        if (onUp == null) {
            throw new IllegalStateException(
                    "in order to call up onUp action should have been set");
        }
        onUp.run();
    }

    public void confirmRemove(Order order) {
        if(orderModel.userCanWrite(order, SecurityUtils.getSessionUserLoginName())) {
            try {
                int status = Messagebox.show(_("Confirm deleting {0}. Are you sure?", order.getName()),
                        "Delete", Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION);
                if (Messagebox.OK == status) {
                    remove(order);
                }
            } catch (InterruptedException e) {
                messagesForUser.showMessage(
                        Level.ERROR, e.getMessage());
                LOG.error(_("Error on showing removing element: ", order.getId()), e);
            }
        }
        else {
            try {
                Messagebox.show(_("You don't have permissions to edit this order"),
                        _("Information"), Messagebox.OK, Messagebox.INFORMATION);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void remove(Order order) {
        boolean alreadyInUse = orderModel.isAlreadyInUseAndIsOnlyInCurrentScenario(order);
        if (alreadyInUse) {
            messagesForUser
                    .showMessage(
                            Level.ERROR,
                            _(
                                    "You can not remove the order \"{0}\" because of any of its order elements are already in use in some work reports and the order just exists in the current scenario",
                                    order.getName()));
        } else {
            orderModel.remove(order);
            Util.reloadBindings(self);
            messagesForUser.showMessage(Level.INFO, _("Removed {0}", order
                    .getName()));
        }
    }

    public void schedule(Order order) {
        orderModel.useSchedulingDataForCurrentScenario(order);
        if(orderModel.userCanRead(order, SecurityUtils.getSessionUserLoginName())) {
            if (order.isScheduled()) {
                planningControllerEntryPoints.goToScheduleOf(order);
                showCreateButtons(false);
            } else {
                try {
                    Messagebox.show(_("The order has no scheduled elements"),
                            _("Information"), Messagebox.OK, Messagebox.INFORMATION);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        else {
            try {
                Messagebox.show(_("You don't have read access to this order"),
                        _("Information"), Messagebox.OK, Messagebox.INFORMATION);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void createTemplate(Order order) {
        orderTemplates.goToCreateTemplateFrom(order);
    }

    private Runnable onUp;

    public void goToEditForm(Order order) {
        showOrderElementFilter();
        showCreateButtons(false);
        planningControllerEntryPoints.goToOrderDetails(order);
    }

    public void initEdit(Order order) {
        if(orderModel.userCanRead(order, SecurityUtils.getSessionUserLoginName())) {
            orderModel.initEdit(order);
            addEditWindowIfNeeded();
            initialStatus = order.getState();
            updateDisabilitiesOnInterface();
            showEditWindow(_("Edit order"));
        }
        else {
            try {
                Messagebox.show(_("You don't have read access to this order"),
                        _("Information"), Messagebox.OK, Messagebox.INFORMATION);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void showEditWindow(String title) {
        addEditWindowIfNeeded();
        initializeTabs();
        editWindow.setTitle(title);
        showWindow(editWindow);
        selectDefaultTab();
        reloadDefaultTab();
        loadCustomerComponent();
    }

    public void setupOrderDetails() {
        confirmLastTab();
        setCurrentTab();
        reloadDefaultTab();
    }

    public void reloadDefaultTab() {
        Tabpanel tabPanel = (Tabpanel) editWindow
                .getFellow("tabPanelGeneralData");
        Util.createBindingsFor(tabPanel);
        Util.reloadBindings(tabPanel);
    }

    private void initializeTabs() {
        final IOrderElementModel orderElementModel = getOrderElementModel();

        if(orderElementTreeController != null){
            TreeComponent orderElementsTree = (TreeComponent) editWindow
            .getFellow("orderElementTree");
            redraw(orderElementsTree);
        }
        if (assignedHoursController != null) {
            assignedHoursController.openWindow(orderElementModel);
        }
        if (manageOrderElementAdvancesController != null) {
            manageOrderElementAdvancesController.openWindow(orderElementModel);
        }
        if (assignedLabelsController != null) {
            assignedLabelsController.openWindow(orderElementModel);
        }
        if (assignedCriterionRequirementController != null) {
            assignedCriterionRequirementController
                    .openWindow(orderElementModel);
        }
        if (assignedMaterialsController != null) {
            assignedMaterialsController.openWindow(orderElementModel
                    .getOrderElement());
        }
        if (assignedTaskQualityFormController != null) {
            assignedTaskQualityFormController.openWindow(orderElementModel);
        }
        if (orderAuthorizationController != null) {
            initOrderAuthorizations();
        }
    }

    public void goToCreateForm() {
        try {
            showOrderElementFilter();
            showCreateButtons(false);
            orderModel.prepareForCreate();
            updateDisabilitiesOnInterface();
            showEditWindow(_("Create order"));
            checkCreationPermissions();
        } catch (ConcurrentModificationException e) {
            messagesForUser.showMessage(Level.ERROR, e.getMessage());
        }
    }

    private void loadCustomerComponent() {
        bdExternalCompanies = (BandboxSearch) editWindow
                .getFellow("bdExternalCompanies");
        bdExternalCompanies.setListboxEventListener(
                Events.ON_SELECT, new EventListener() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        final Object object = bdExternalCompanies
                                .getSelectedElement();
                        orderModel.setExternalCompany((ExternalCompany) object);
                    }
                });
    }

    public void setPlanningControllerEntryPoints(
            IOrderPlanningGate planningControllerEntryPoints) {
        this.planningControllerEntryPoints = planningControllerEntryPoints;
    }

    public IOrderPlanningGate getPlanningControllerEntryPoints() {
        return this.planningControllerEntryPoints;
    }

    public void setActionOnUp(Runnable onUp) {
        this.onUp = onUp;
    }

    public List<BaseCalendar> getBaseCalendars() {
        return orderModel.getBaseCalendars();
    }

    public BaseCalendarsComboitemRenderer getBaseCalendarsComboitemRenderer() {
        return baseCalendarsComboitemRenderer;
    }

    private class BaseCalendarsComboitemRenderer implements ComboitemRenderer {

        @Override
        public void render(Comboitem item, Object data) throws Exception {
            BaseCalendar calendar = (BaseCalendar) data;
            item.setLabel(calendar.getName());
            item.setValue(calendar);

            BaseCalendar current = orderModel.getCalendar();
            if ((current != null) && calendar.getId().equals(current.getId())) {
                Combobox combobox = (Combobox) item.getParent();
                combobox.setSelectedItem(item);
            }
        }

    }

    public void setBaseCalendar(BaseCalendar calendar) {
        orderModel.setCalendar(calendar);
    }

    public boolean isCodeAutogenerated() {
        return orderModel.isCodeAutogenerated();
    }

    public void setCodeAutogenerated(boolean codeAutogenerated) {
        try {
            orderModel.setCodeAutogenerated(codeAutogenerated);
        } catch (ConcurrentModificationException e) {
            messagesForUser.showMessage(Level.ERROR, e.getMessage());
        }
        Util.reloadBindings(editWindow);
    }

    public OrderStatusEnum[] getOrderStatus() {
        return OrderStatusEnum.values();
    }

    public List<ExternalCompany> getExternalCompaniesAreClient() {
        return orderModel.getExternalCompaniesAreClient();
    }

    public OrdersRowRenderer getOrdersRowRender() {
        return ordersRowRenderer;
    }

    public class OrdersRowRenderer implements RowRenderer {

        @Override
        public void render(Row row, Object data) throws Exception {

            final Order order = (Order) data;
            row.setValue(order);

            appendLabel(row, order.getName());
            appendLabel(row, order.getCode());
            appendDate(row, order.getInitDate());
            appendDate(row, order.getDeadline());
            appendCustomer(row, order.getCustomer());
            appendLabel(row, order.getCustomerReference());
            appendObject(row, order.getTotalBudget());
            appendObject(row, order.getTotalHours());
            appendObject(row, _(order.getState().toString()));
            appendOperations(row, order);

            row.setTooltiptext(getTooltipText(order));
            row.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    goToEditForm(order);
                }
            });
        }
    }

    private void appendObject(final Row row, Object object) {
        String text = new String("");
        if (object != null) {
            text = object.toString();
        }
        appendLabel(row, text);
    }

    private void appendCustomer(final Row row, ExternalCompany externalCompany) {
        String customerName = new String("");
        if (externalCompany != null) {
            customerName = externalCompany.getName();
        }
        appendLabel(row, customerName);
    }

    private void appendLabel(final Row row, String value) {
        Label label = new Label(value);
        row.appendChild(label);
    }

    private void appendDate(final Row row, Date date) {
        String labelDate = new String("");
        if (date != null) {
            labelDate = new SimpleDateFormat("dd/MM/yyyy").format(date);
        }
        appendLabel(row, labelDate);
    }

    private void appendOperations(final Row row,final Order order){
        Hbox hbox = new Hbox();
        appendButtonEdit(hbox,order);
        appendButtonDelete(hbox, order);
        appendButtonPlan(hbox, order);
        appendButtonDerived(hbox, order);
        row.appendChild(hbox);
    }

    private void appendButtonEdit(final Hbox hbox, final Order order) {
        Button buttonEdit = new Button();
        buttonEdit.setSclass("icono");
        buttonEdit.setImage("/common/img/ico_editar1.png");
        buttonEdit.setHoverImage("/common/img/ico_editar.png");
        buttonEdit.setTooltiptext(_("Edit"));
        buttonEdit.addEventListener("onClick",new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                goToEditForm(order);
            }
        });
        hbox.appendChild(buttonEdit);
    }

    private void appendButtonDelete(final Hbox hbox, final Order order) {
        if(orderModel.userCanWrite(order, SecurityUtils.getSessionUserLoginName())) {
            Button buttonDelete = new Button();
            buttonDelete.setSclass("icono");
            buttonDelete.setImage("/common/img/ico_borrar1.png");
            buttonDelete.setHoverImage("/common/img/ico_borrar.png");
            buttonDelete.setTooltiptext(_("Delete"));
            buttonDelete.addEventListener("onClick",new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    confirmRemove(order);
                }
            });
            hbox.appendChild(buttonDelete);
        }
    }

    private void appendButtonPlan(final Hbox hbox, final Order order) {
        Button buttonPlan = new Button();
        buttonPlan.setSclass("icono");
        buttonPlan.setImage("/common/img/ico_planificador1.png");
        buttonPlan.setHoverImage("/common/img/ico_planificador.png");
        buttonPlan.setTooltiptext(_("See scheduling"));
        buttonPlan.addEventListener("onClick",new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                schedule(order);
            }
        });
        hbox.appendChild(buttonPlan);
    }

    private void appendButtonDerived(final Hbox hbox, final Order order) {
        Button buttonDerived = new Button();
        buttonDerived.setSclass("icono");
        buttonDerived.setImage("/common/img/ico_derived1.png");
        buttonDerived.setHoverImage("/common/img/ico_derived.png");
        buttonDerived.setTooltiptext(_("Create Template"));
        buttonDerived.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                createTemplate(order);
            }
        });
        hbox.appendChild(buttonDerived);
    }

    public String getTooltipText(final Order order) {
        return orderModel.gettooltipText(order);
    }

    public void reloadTotalBudget(Label txtTotalBudget) {
        Util.reloadBindings(txtTotalBudget);
    }

    /**
     * Operations to filter the orders by multiple filters
     */

    public Constraint checkConstraintFinishDate() {
        return new Constraint() {
            @Override
            public void validate(Component comp, Object value)
                    throws WrongValueException {
                Date finishDate = (Date) value;
                if ((finishDate != null)
                        && (filterStartDate.getValue() != null)
                        && (finishDate.compareTo(filterStartDate.getValue()) < 0)) {
                    filterFinishDate.setValue(null);
                    throw new WrongValueException(comp,
                            _("must be greater than start date"));
                }
            }
        };
    }

    public Constraint checkConstraintStartDate() {
        return new Constraint() {
            @Override
            public void validate(Component comp, Object value)
                    throws WrongValueException {
                Date startDate = (Date) value;
                if ((startDate != null)
                        && (filterFinishDate.getValue() != null)
                        && (startDate.compareTo(filterFinishDate.getValue()) > 0)) {
                    filterStartDate.setValue(null);
                    throw new WrongValueException(comp,
                            _("must be lower than finish date"));
                }
            }
        };
    }

    public void onApplyFilter() {
        OrderPredicate predicate = createPredicate();
        if (predicate != null) {
            filterByPredicate(predicate);
        } else {
            showAllOrders();
        }
    }

    private OrderPredicate createPredicate() {
        List<FilterPair> listFilters = (List<FilterPair>) bdFilters
                .getSelectedElements();
        Date startDate = filterStartDate.getValue();
        Date finishDate = filterFinishDate.getValue();
        Boolean includeOrderElements = checkIncludeOrderElements.isChecked();

        if (listFilters.isEmpty() && startDate == null && finishDate == null) {
            return null;
        }
        return new OrderPredicate(listFilters, startDate, finishDate,
                includeOrderElements);
    }

    private void filterByPredicate(OrderPredicate predicate) {
        List<Order> filterOrders = orderModel.getFilterOrders(predicate);
        listing.setModel(new SimpleListModel(filterOrders.toArray()));
        listing.invalidate();
    }

    private void clearFilterDates() {
        filterStartDate.setValue(null);
        filterFinishDate.setValue(null);
    }

    public void showAllOrders() {
        listing.setModel(new SimpleListModel(orderModel.getOrders().toArray()));
        listing.invalidate();
    }

    private void showOrderFilter() {
        orderFilter.setVisible(true);
        orderElementFilter.setVisible(false);
    }

    public void showOrderElementFilter() {
        if (orderFilter != null)
            orderFilter.setVisible(false);
        if (orderElementFilter != null)
            orderElementFilter.setVisible(true);
    }

    public void showCreateButtons(boolean showCreate) {
        createOrderButton.setVisible(showCreate);
        createOrderFromTemplateButton.setVisible(showCreate);
        saveOrderAndContinueButton.setVisible(!showCreate);
    }

    public void highLight(OrderElement orderElement) {
        Tab tab = (Tab) editWindow.getFellowIfAny("tabOrderElements");
        if (tab != null) {
            tab.setSelected(true);
            Events.sendEvent(new SelectEvent(Events.ON_SELECT, tab, null));
        }

        if ((!(orderElement instanceof Order))
                && (orderElementTreeController != null)) {
            final Treeitem item = orderElementTreeController
                    .getTreeitemByOrderElement(orderElement);
            if (item != null) {
                orderElementTreeController.showEditionOrderElement(item);
            }
        }
    }

    /**
     * Checks the creation permissions of the current user and enables/disables
     * the save buttons accordingly.
     */
    private void checkCreationPermissions() {
        if(SecurityUtils.isUserInRole(UserRole.ROLE_CREATE_ORDER)) {
            saveOrderAndContinueButton.setDisabled(false);
        }
        else {
            saveOrderAndContinueButton.setDisabled(true);
        }
    }

    private boolean readOnly = true;

    private OrderStatusEnum initialStatus;

    private void updateDisabilitiesOnInterface() {
        Order order = (Order) orderModel.getOrder();

        boolean permissionForWriting = orderModel.userCanWrite(order,
                SecurityUtils.getSessionUserLoginName());
        boolean isInStoredState = order.getState() == OrderStatusEnum.STORED;
        boolean isInitiallyStored = initialStatus == OrderStatusEnum.STORED;

        readOnly = !permissionForWriting || isInStoredState;

        if(orderElementTreeController != null) {
            orderElementTreeController.setReadOnly(readOnly);
        }
        saveOrderAndContinueButton.setDisabled(!permissionForWriting
                || (isInitiallyStored && isInStoredState));
    }

    public void sortOrders() {
        Column columnDateStart = (Column) listWindow
                .getFellow("columnDateStart");
        if (columnDateStart != null) {
            if (columnDateStart.getSortDirection().equals("ascending")) {
                columnDateStart.sort(false, false);
                columnDateStart.setSortDirection("ascending");
            } else if (columnDateStart.getSortDirection().equals("descending")) {
                columnDateStart.sort(true, false);
                columnDateStart.setSortDirection("descending");
            }
        }
    }

}