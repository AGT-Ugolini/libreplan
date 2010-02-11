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

import java.util.List;

import org.navalplanner.business.orders.entities.OrderElement;
import org.navalplanner.business.workreports.entities.WorkReportLine;
import org.navalplanner.web.common.Util;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Vbox;

/**
 * Controller for show the asigned hours of the selected order element<br />
 * @author Susana Montes Pedreria <smontes@wirelessgalicia.com>
 */
public class AsignedHoursToOrderElementController extends
        GenericForwardComposer {

    private IAsignedHoursToOrderElementModel asignedHoursToOrderElementModel;

    private Vbox orderElementHours;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        comp.setVariable("asignedHoursToOrderElementController", this, true);
    }

    public List<WorkReportLine> getWorkReportLines() {
        return asignedHoursToOrderElementModel.getWorkReportLines();
    }

    public int getTotalAsignedDirectHours() {
        return asignedHoursToOrderElementModel.getAsignedDirectHours();
    }

    public int getTotalAsignedHours() {
        return asignedHoursToOrderElementModel.getTotalAsignedHours();
    }

    public int getHoursChildren() {
        return asignedHoursToOrderElementModel.getAsignedDirectHoursChildren();
    }

    public int getEstimatedHours() {
        return asignedHoursToOrderElementModel.getEstimatedHours();
    }

    public int getProgressWork() {
        return asignedHoursToOrderElementModel.getProgressWork();
    }

    private IOrderElementModel orderElementModel;

    public void openWindow(IOrderElementModel orderElementModel) {
        setOrderElementModel(orderElementModel);
        asignedHoursToOrderElementModel.initOrderElement(getOrderElement());

        if (orderElementHours != null) {
            Util.createBindingsFor(orderElementHours);
            Util.reloadBindings(orderElementHours);
        }

        viewPercentage();
    }

    public void setOrderElementModel(IOrderElementModel orderElementModel) {
        this.orderElementModel = orderElementModel;
    }

    private OrderElement getOrderElement() {
        return orderElementModel.getOrderElement();
    }

    private Progressmeter hoursProgressBar;

    private Progressmeter exceedHoursProgressBar;

    /**
     * This method shows the percentage of the imputed hours with respect to the
     * estimated hours.If the hours imputed is greater that the hours estimated
     * then show the exceed percentage of hours.
     */
    private void viewPercentage() {
        if (this.getProgressWork() > 100) {
            hoursProgressBar.setValue(100);
            exceedHoursProgressBar.setVisible(true);
            exceedHoursProgressBar.setValue(0);
            String exceedValue = String.valueOf(getProgressWork() - 100);
            exceedHoursProgressBar.setWidth(exceedValue + "px");
            exceedHoursProgressBar.setLeft("left");
            exceedHoursProgressBar
                    .setStyle("background:red ; border:1px solid red");
        } else {
            hoursProgressBar.setValue(getProgressWork());
            exceedHoursProgressBar.setVisible(false);
        }
    }

}
