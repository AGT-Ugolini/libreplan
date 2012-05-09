/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                         Desenvolvemento Tecnolóxico de Galicia
 * Copyright (C) 2010-2011 Igalia, S.L.
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
package org.libreplan.web.templates.budgettemplates;

import static org.libreplan.web.I18nHelper._;

import java.util.HashMap;
import java.util.Map;

import org.libreplan.business.templates.entities.BudgetLineTemplate;
import org.libreplan.business.templates.entities.OrderElementTemplate;
import org.libreplan.web.tree.EntitiesTree;

/**
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 *
 */
public class TemplatesTree extends EntitiesTree<OrderElementTemplate> {

    public TemplatesTree(OrderElementTemplate root) {
        super(OrderElementTemplate.class, root);
    }

    @Override
    protected OrderElementTemplate createNewElement() {
        BudgetLineTemplate result = BudgetLineTemplate.createNew();
        result.setName(_("New template"));
        result.setCode(_("New code"));
        result.setDescription(_("New Description"));
        return result;
    }

    @Override
    protected OrderElementTemplate createNewElement(String name, int hours) {
        BudgetLineTemplate result = BudgetLineTemplate.createNew();
        result.setName(name);
        result.setCode(_("New code"));
        result.setDescription(_("New Description"));
        result.setWorkHours(hours);
        return result;
    }

}
