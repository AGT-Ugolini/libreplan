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

package org.navalplanner.web.planner.order;

import static org.navalplanner.web.I18nHelper._;

import java.util.Date;
import java.util.List;

import org.navalplanner.business.common.exceptions.ValidationException;
import org.navalplanner.business.externalcompanies.entities.ExternalCompany;
import org.navalplanner.business.planner.entities.SubcontractedTaskData;
import org.navalplanner.business.planner.entities.Task;
import org.navalplanner.business.planner.entities.TaskElement;
import org.navalplanner.web.common.Util;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.zkoss.ganttz.extensions.IContextWithPlannerTask;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.api.Tabpanel;

/**
 * Controller for subcontract a task.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
@org.springframework.stereotype.Component("subcontractController")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SubcontractController extends GenericForwardComposer {

    private Tabpanel tabpanel;

    private ISubcontractModel subcontractModel;

    private IContextWithPlannerTask<TaskElement> context;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        tabpanel = (Tabpanel) comp;
    }

    public void init(Task task,
            IContextWithPlannerTask<TaskElement> context) {
        this.context = context;
        subcontractModel.init(task, context.getTask());
        Util.reloadBindings(tabpanel);
    }

    public void accept() throws ValidationException {
        subcontractModel.confirm();
    }

    public void cancel() {
        subcontractModel.cancel();
    }

    public List<ExternalCompany> getSubcontractorExternalCompanies() {
        return subcontractModel.getSubcontractorExternalCompanies();
    }

    public SubcontractedTaskData getSubcontractedTaskData() {
        return subcontractModel.getSubcontractedTaskData();
    }

    public void setExternalCompany(Comboitem comboitem) {
        if (comboitem != null && comboitem.getValue() != null) {
            ExternalCompany externalCompany = (ExternalCompany) comboitem
                    .getValue();
            subcontractModel.setExternalCompany(externalCompany);
        } else {
            subcontractModel.setExternalCompany(null);
        }
    }

    public Date getEndDate() {
        return subcontractModel.getEndDate();
    }

    public void setEndDate(Date endDate) {
        subcontractModel.setEndDate(endDate);
    }

    public void removeSubcontractedTaskData() {
        subcontractModel.removeSubcontractedTaskData();
    }

}