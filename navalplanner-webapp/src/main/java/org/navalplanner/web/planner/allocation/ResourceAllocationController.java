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

package org.navalplanner.web.planner.allocation;

import static org.navalplanner.web.I18nHelper._;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.navalplanner.business.orders.entities.AggregatedHoursGroup;
import org.navalplanner.business.planner.entities.CalculatedValue;
import org.navalplanner.business.planner.entities.DerivedAllocation;
import org.navalplanner.business.planner.entities.ResourceAllocation;
import org.navalplanner.business.planner.entities.TaskElement;
import org.navalplanner.business.resources.entities.ResourceEnum;
import org.navalplanner.web.I18nHelper;
import org.navalplanner.web.common.IMessagesForUser;
import org.navalplanner.web.common.Util;
import org.navalplanner.web.common.components.NewAllocationSelector;
import org.navalplanner.web.planner.order.PlanningState;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.zkoss.ganttz.extensions.IContextWithPlannerTask;
import org.zkoss.ganttz.timetracker.ICellForDetailItemRenderer;
import org.zkoss.ganttz.timetracker.IConvertibleToColumn;
import org.zkoss.ganttz.timetracker.OnColumnsRowRenderer;
import org.zkoss.ganttz.util.OnZKDesktopRegistry;
import org.zkoss.ganttz.util.script.IScriptsRegister;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Window;

/**
 * Controller for {@link ResourceAllocation} view.
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 * @author Diego Pino Garcia <dpino@igalia.com>
 */
@org.springframework.stereotype.Component("resourceAllocationController")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ResourceAllocationController extends GenericForwardComposer {

    private static final Log LOG = LogFactory
            .getLog(ResourceAllocationController.class);

    private IResourceAllocationModel resourceAllocationModel;

    private Grid orderElementHoursGrid;

    private ResourceAllocationRenderer resourceAllocationRenderer = new ResourceAllocationRenderer();

    private Grid allocationsGrid;

    private FormBinder formBinder;

    private AllocationRowsHandler allocationRows;

    private Intbox assignedHoursComponent;

    private Grid calculationTypesGrid;

    private Radiogroup calculationTypeSelector;

    private Checkbox recommendedAllocationCheckbox;

    private Checkbox extendedViewCheckbox;

    private Datebox taskEndDate;

    private Decimalbox allResourcesPerDay;

    private Label allOriginalHours;
    private Label allTotalHours;
    private Label allConsolidatedHours;

    private Label allTotalResourcesPerDay;
    private Label allConsolidatedResourcesPerDay;

    private Button applyButton;

    private NewAllocationSelector newAllocationSelector;

    private Tab tbResourceAllocation;

    private Tab workerSearchTab;

    private Window editTaskWindow;

    public static void registerNeededScripts() {
        getScriptsRegister()
                .register(ScriptsRequiredByAdvancedAllocation.class);
    }

    private static IScriptsRegister getScriptsRegister() {
        return OnZKDesktopRegistry.getLocatorFor(IScriptsRegister.class)
                .retrieve();
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        taskEndDate = new Datebox();
        allResourcesPerDay = new Decimalbox();
        allResourcesPerDay.setWidth("80px");
        newAllocationSelector.setLimitingResourceFilter(false);
        initAllocationLabels();
        makeReadyInputsForCalculationTypes();
        prepareCalculationTypesGrid();
    }

    private void initAllocationLabels() {
        allOriginalHours = new Label();
        allTotalHours = new Label();
        allConsolidatedHours = new Label();

        allTotalResourcesPerDay = new Label();
        allConsolidatedResourcesPerDay = new Label();
    }

    private void makeReadyInputsForCalculationTypes() {
        final String width = "90px";
        taskEndDate.setWidth(width);
        assignedHoursComponent = new Intbox();
        assignedHoursComponent.setWidth("80px");
    }

    private void prepareCalculationTypesGrid() {
        calculationTypesGrid.setModel(new ListModelList(Arrays
                .asList(CalculationTypeRadio.values())));
        calculationTypesGrid.setRowRenderer(OnColumnsRowRenderer.create(
                calculationTypesRenderer(), Arrays.asList(0)));
    }

    private ICellForDetailItemRenderer<Integer, CalculationTypeRadio> calculationTypesRenderer() {
        return new ICellForDetailItemRenderer<Integer, CalculationTypeRadio>() {

            @Override
            public Component cellFor(Integer column, CalculationTypeRadio data) {
                return data.createComponent(getController(),
                        getCalculationTypeRadio());
            }
        };
    }

    private Datebox getTaskEndDate() {
        return taskEndDate;
    }

    private CalculationTypeRadio getCalculationTypeRadio() {
        if (formBinder != null) {
            return CalculationTypeRadio.from(formBinder.getCalculatedValue());
        }
        return null;
    }

    public ResourceAllocationController getController() {
        return this;
    }

    /**
     * Shows Resource Allocation window
     * @param task
     * @param ganttTask
     * @param planningState
     */
    public void init(IContextWithPlannerTask<TaskElement> context,
            org.navalplanner.business.planner.entities.Task task,
            PlanningState planningState, IMessagesForUser messagesForUser) {
        try {
            if (formBinder != null) {
                formBinder.detach();
            }
            allocationRows = resourceAllocationModel.initAllocationsFor(task,
                    context, planningState);
            formBinder = allocationRows.createFormBinder(planningState
                    .getCurrentScenario(), resourceAllocationModel);
            formBinder.setAllOriginalHours(allOriginalHours);
            formBinder.setAllTotalHours(allTotalHours);
            formBinder.setAllConsolidatedHours(allConsolidatedHours);
            formBinder.setAssignedHoursComponent(assignedHoursComponent);

            formBinder.setAllTotalResourcesPerDay(allTotalResourcesPerDay);
            formBinder
                    .setAllConsolidatedResourcesPerDay(allConsolidatedResourcesPerDay);
            formBinder.setAllResourcesPerDay(allResourcesPerDay);

            formBinder.setEndDate(taskEndDate);
            formBinder.setApplyButton(applyButton);
            formBinder.setAllocationsGrid(allocationsGrid);
            formBinder.setMessagesForUser(messagesForUser);
            formBinder.setWorkerSearchTab(workerSearchTab);
            formBinder.setCheckbox(recommendedAllocationCheckbox);

            CalculationTypeRadio calculationTypeRadio = CalculationTypeRadio
                    .from(formBinder.getCalculatedValue());
            calculationTypeRadio.doTheSelectionOn(calculationTypeSelector);

            tbResourceAllocation.setSelected(true);
            orderElementHoursGrid.setModel(new ListModelList(
                    resourceAllocationModel.getHoursAggregatedByCriterions()));
            orderElementHoursGrid.setRowRenderer(createOrderElementHoursRenderer());
            newAllocationSelector.setAllocationsAdder(resourceAllocationModel);
        } catch (WrongValueException e) {
            LOG.error("there was a WrongValueException initializing window", e);
            throw e;
        }
    }

    public Date getTaskStart() {
        return resourceAllocationModel.getTaskStart();
    }

    public enum HoursRendererColumn {


        CRITERIONS {
            @Override
            public Component cell(HoursRendererColumn column,
                    AggregatedHoursGroup data) {
                return new Label(data.getCriterionsJoinedByComma());
            }
        },
        RESOURCE_TYPE{

            @Override
            public Component cell(HoursRendererColumn column,
                    AggregatedHoursGroup data) {
                return new Label(asString(data.getResourceType()));
            }
        },
        HOURS {
            @Override
            public Component cell(HoursRendererColumn column,
                    AggregatedHoursGroup data) {
                Label result = new Label(Integer.toString(data.getHours()));
                return result;
            }
        };

        private static String asString(ResourceEnum resourceType) {
            switch (resourceType) {
            case RESOURCE:
            case MACHINE:
            case WORKER:
                return _(resourceType.getDisplayName());
            default:
                LOG.warn("no i18n for " + resourceType.name());
                return resourceType.name();
            }
        }

        public abstract Component cell(HoursRendererColumn column,
                AggregatedHoursGroup data);
    }

    private static final ICellForDetailItemRenderer<HoursRendererColumn, AggregatedHoursGroup> hoursCellRenderer = new ICellForDetailItemRenderer<HoursRendererColumn, AggregatedHoursGroup>() {

        @Override
        public Component cellFor(
                HoursRendererColumn column,
                AggregatedHoursGroup data) {
            return column.cell(column, data);
        }
    };

    private RowRenderer createOrderElementHoursRenderer() {
        return OnColumnsRowRenderer
                .create(
                        hoursCellRenderer, Arrays.asList(HoursRendererColumn.values()));
    }

    /**
     * Pick resources selected from {@link NewAllocationSelector} and add them to
     * resource allocation list
     *
     * @param e
     */
    public void onSelectWorkers(Event e) {
        try {
            newAllocationSelector.addChoosen();
        } finally {
            tbResourceAllocation.setSelected(true);
            newAllocationSelector.clearAll();
            Util.reloadBindings(allocationsGrid);
        }
    }

    /**
     * Shows the extended view of the resources allocations
     */
    public void onCheckExtendedView() {
        if (isExtendedView()) {
            editTaskWindow.setWidth("970px");
        } else {
            editTaskWindow.setWidth("870px");
        }
        editTaskWindow.invalidate();
        Util.reloadBindings(allocationsGrid);
    }

    public boolean isExtendedView() {
        return extendedViewCheckbox.isChecked();
    }

    public int getColspanHours() {
        if (isExtendedView()) {
            return 4;
        }
        return 1;
    }

    public int getColspanResources() {
        if (isExtendedView()) {
            return 3;
        }
        return 1;
    }
    /**
     * Close search worker in worker search tab
     * @param e
     */
    public void onCloseSelectWorkers() {
        tbResourceAllocation.setSelected(true);
        newAllocationSelector.clearAll();
    }

    public enum CalculationTypeRadio {

        NUMBER_OF_HOURS(CalculatedValue.NUMBER_OF_HOURS) {
            @Override
            public String getName() {
                return _("Calculate Number of Hours");
            }

            @Override
            public Component input(
                    ResourceAllocationController resourceAllocationController) {
                return resourceAllocationController.assignedHoursComponent;
            }
        },
        END_DATE(CalculatedValue.END_DATE) {
            @Override
            public String getName() {
                return _("Calculate End Date");
            }

            @Override
            public Component input(
                    ResourceAllocationController resourceAllocationController) {
                return resourceAllocationController.taskEndDate;
            }
        },
        RESOURCES_PER_DAY(CalculatedValue.RESOURCES_PER_DAY) {

            @Override
            public String getName() {
                return _("Calculate Resources per Day");
            }

            @Override
            public Component input(
                    ResourceAllocationController resourceAllocationController) {
                return resourceAllocationController.allResourcesPerDay;
            }
        };

        public static CalculationTypeRadio from(CalculatedValue calculatedValue) {
            Validate.notNull(calculatedValue);
            for (CalculationTypeRadio calculationTypeRadio : CalculationTypeRadio
                    .values()) {
                if (calculationTypeRadio.getCalculatedValue() == calculatedValue) {
                    return calculationTypeRadio;
                }
            }
            throw new RuntimeException("not found "
                    + CalculationTypeRadio.class.getSimpleName() + " for "
                    + calculatedValue);
        }

        public abstract Component input(
                ResourceAllocationController resourceAllocationController);

        public Component createComponent(
                ResourceAllocationController resourceAllocationController,
                CalculationTypeRadio calculationTypeRadio) {
            if (this.equals(END_DATE)) {
                return createHbox(resourceAllocationController.taskEndDate,
                        calculationTypeRadio);
            } else {
                return createRadio(calculationTypeRadio);
            }
        }

        public Radio createRadio(CalculationTypeRadio calculationTypeRadio) {
            Radio result = new Radio();
            result.setLabel(getName());
            result.setValue(toString());
            result.setChecked(isSameCalculationTypeRadio(result,
                    calculationTypeRadio));
            return result;
        }

        public Hbox createHbox(Datebox datebox,
                CalculationTypeRadio calculationTypeRadio) {
            Hbox hbox = new Hbox();
            hbox.setSpacing("65px");
            Radio radio = createRadio(calculationTypeRadio);

            hbox.appendChild(radio);
            hbox.appendChild(datebox);
            return hbox;
        }

        public boolean isSameCalculationTypeRadio(Radio radio,
                CalculationTypeRadio calculationTypeRadio) {
            if (calculationTypeRadio != null) {
                return name().equals(calculationTypeRadio.name());
            }
            return false;
        }

        public void doTheSelectionOn(final Radiogroup radiogroup) {
                for (int i = 0; i < radiogroup.getItemCount(); i++) {
                    Radio radio = radiogroup.getItemAtIndex(i);
                    if (name().equals(radio.getValue())) {
                        radiogroup.setSelectedIndex(i);
                        break;
                    }
                }
        }

        private final CalculatedValue calculatedValue;

        private CalculationTypeRadio(CalculatedValue calculatedValue) {
            this.calculatedValue = calculatedValue;

        }

        public abstract String getName();

        public CalculatedValue getCalculatedValue() {
            return calculatedValue;
        }
    }

    public enum DerivedAllocationColumn implements IConvertibleToColumn {
        NAME(_("Name")) {
            @Override
            public Component cellFor(DerivedAllocation data) {
                return new Label(data.getName());
            }
        },
        ALPHA(_("Alpha")) {
            @Override
            public Component cellFor(DerivedAllocation data) {
                return new Label(String.format("%3.2f", data.getAlpha()));
            }
        },
        HOURS(_("Total Hours")) {
            @Override
            public Component cellFor(DerivedAllocation data) {
                return new Label(data.getHours() + "");
            }
        };

        /**
         * Forces to mark the string as needing translation
         */
        private static String _(String string) {
            return string;
        }

        private final String name;

        private DerivedAllocationColumn(String name) {
            this.name = name;
        }

        public String getName() {
            return I18nHelper._(name);
        }

        @Override
        public org.zkoss.zul.api.Column toColumn() {
            return new Column(getName());
        }

        public static void appendColumnsTo(Grid grid) {
            Columns columns = new Columns();
            grid.appendChild(columns);
            for (DerivedAllocationColumn each : values()) {
                columns.appendChild(each.toColumn());
            }
        }

        public static RowRenderer createRenderer() {
            return OnColumnsRowRenderer.create(cellRenderer, Arrays
                    .asList(DerivedAllocationColumn.values()));
        }

        private static final ICellForDetailItemRenderer<DerivedAllocationColumn, DerivedAllocation> cellRenderer= new ICellForDetailItemRenderer<DerivedAllocationColumn, DerivedAllocation>() {

                                @Override
                                public Component cellFor(
                                        DerivedAllocationColumn column,
                                        DerivedAllocation data) {
                                    return column.cellFor(data);
                                }
                            };

        abstract Component cellFor(DerivedAllocation data);
    }

    public List<CalculationTypeRadio> getCalculationTypes() {
        return Arrays.asList(CalculationTypeRadio.values());
    }

    public void setCalculationTypeSelected(String enumName) {
        CalculationTypeRadio calculationTypeRadio = CalculationTypeRadio
                .valueOf(enumName);
        formBinder
                .setCalculatedValue(calculationTypeRadio.getCalculatedValue());
    }

    public Integer getOrderHours() {
        return resourceAllocationModel.getOrderHours();
    }

    public List<? extends Object> getResourceAllocations() {
        return formBinder != null ? plusAggregatingRow(formBinder
                .getCurrentRows()) : Collections
                .<AllocationRow> emptyList();
    }

    private List<Object> plusAggregatingRow(List<AllocationRow> currentRows) {
        List<Object> result = new ArrayList<Object>(currentRows);
        result.add(null);
        return result;
    }

    public ResourceAllocationRenderer getResourceAllocationRenderer() {
        return resourceAllocationRenderer;
    }

    // Triggered when closable button is clicked
    public void onClose(Event event) {
        cancel();
        event.stopPropagation();
    }

    public void cancel() {
        clear();
        resourceAllocationModel.cancel();
    }

    public void clear() {
        newAllocationSelector.clearAll();
        allocationsGrid.setModel(new SimpleListModel(Collections.emptyList()));
    }

    public void accept() {
        resourceAllocationModel.accept();
        clear();
    }

    private class ResourceAllocationRenderer implements RowRenderer {

        @Override
        public void render(Row item, Object data) throws Exception {
            if (data instanceof AllocationRow) {
                AllocationRow row = (AllocationRow) data;
                renderResourceAllocation(item, row);
            } else {
                renderAggregatingRow(item);
            }
        }

        private void renderResourceAllocation(Row row, final AllocationRow data)
                throws Exception {
            row.setValue(data);
            append(row, data.createDetail());
            append(row, new Label(data.getName()));
            append(row, new Label(Integer.toString(data.getOriginalHours())));
            append(row, new Label(Integer.toString(data.getTotalHours())));
            append(row,
                    new Label(Integer.toString(data.getConsolidatedHours())));
            append(row, data.getHoursInput());
            append(row, new Label(data.getTotalResourcesPerDay().getAmount()
                    .toString()));
            append(row, new Label(data.getConsolidatedResourcesPerDay()
                    .getAmount().toString()));
            append(row, data.getResourcesPerDayInput());
            // On click delete button
            Button deleteButton = appendDeleteButton(row);
            formBinder.setDeleteButtonFor(data, deleteButton);
            deleteButton.addEventListener("onClick", new EventListener() {

                @Override
                public void onEvent(Event event) throws Exception {
                    removeAllocation(data);
                }
            });

            if (!data.isSatisfied()) {
                row.setSclass("allocation-not-satisfied");
            } else {
                row.setSclass("allocation-satisfied");
            }
        }

        private void renderAggregatingRow(Row row) {
            ResourceAllocationController controller = ResourceAllocationController.this;
            append(row, new Label());
            append(row, new Label(_("Sum of all rows")));
            append(row, allOriginalHours);
            append(row, allTotalHours);
            append(row, allConsolidatedHours);
            append(row, CalculationTypeRadio.NUMBER_OF_HOURS
                        .input(controller));
            append(row, allTotalResourcesPerDay);
            append(row, allConsolidatedResourcesPerDay);
            append(row, CalculationTypeRadio.RESOURCES_PER_DAY
                        .input(controller));
            append(row, new Label());
        }

        private void removeAllocation(AllocationRow row) {
            allocationRows.remove(row);
            Util.reloadBindings(allocationsGrid);
        }

        private Button appendDeleteButton(Row row) {
            Button button = new Button();
            button.setSclass("icono");
            button.setImage("/common/img/ico_borrar1.png");
            button.setHoverImage("/common/img/ico_borrar.png");
            button.setTooltiptext(_("Delete"));
            return append(row, button);
        }

        private <T extends Component> T append(Row row, T component) {
            row.appendChild(component);
            return component;
        }
    }

    public FormBinder getFormBinder() {
        return formBinder;
    }

    public void accept(AllocationResult allocation) {
        resourceAllocationModel.accept(allocation);
    }

    public void setStartDate(Date date) {
        resourceAllocationModel.setStartDate(date);
    }

    public boolean hasResourceAllocations() {
        return ((getResourceAllocations().size() > 1));
    }

}
