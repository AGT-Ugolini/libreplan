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

package org.navalplanner.business.resources.entities;

import java.util.ArrayList;
import java.util.List;

/**
 * Predefined working relationships<br />
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 * @author Diego Pino García <dpino@igalia.com>
 */
public enum WorkingRelationship {
    HIRED("hiredResourceWorkingRelationship"),
    FIRED("firedResourceWorkingRelationship");

    public static List<String> getCriterionNames() {
        ArrayList<String> result = new ArrayList<String>();
        for (WorkingRelationship workingRelationship : values()) {
            result.add(workingRelationship.criterionName);
        }
        return result;
    }

    private final String criterionName;

    public Criterion criterion() {
        return Criterion.create(criterionName, CriterionType.asCriterionType(PredefinedCriterionTypes.WORK_RELATIONSHIP));
    }

    public String getCriterionName() {
        return criterionName;
    }

    private WorkingRelationship(String name) {
        this.criterionName = name;
    }
}
