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

package org.navalplanner.web.orders.labels;

import org.navalplanner.business.orders.entities.OrderElement;
import org.navalplanner.web.orders.IOrderElementModel;
import org.navalplanner.web.orders.IOrderModel;

/**
 * Controller for showing OrderElement assigned labels
 *
 * @author Diego Pino García <dpino@igalia.com>
 *
 */
public class AssignedLabelsToOrderElementController extends
        AssignedLabelsController<OrderElement, IOrderElementModel> {

    private IAssignedLabelsToOrderElementModel assignedLabelsToOrderElementModel;


    IOrderElementModel orderElementModel;

    @Override
    protected IAssignedLabelsToOrderElementModel getModel() {
        return assignedLabelsToOrderElementModel;
    }

    @Override
    protected void setOuterModel(IOrderElementModel orderElementModel) {
        this.orderElementModel = orderElementModel;
        setOrderModel(orderElementModel.getOrderModel());

    }

    private void setOrderModel(IOrderModel orderModel) {
        if (assignedLabelsToOrderElementModel != null) {
            assignedLabelsToOrderElementModel.setOrderModel(orderModel);
        }
    }

    public OrderElement getOrderElement() {
        return orderElementModel.getOrderElement();
    }

    @Override
    protected OrderElement getElement() {
        return getOrderElement();
    }
}
