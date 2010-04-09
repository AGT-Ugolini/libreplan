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

package org.navalplanner.business.materials.bootstrap;

import org.navalplanner.business.IDataBootstrap;
import org.navalplanner.business.materials.daos.IUnitTypeDAO;
import org.navalplanner.business.materials.entities.UnitType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the bootstrap of the predefined {@link UnitType}.
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */

@Component
@Scope("singleton")
public class UnitTypeBootstrap implements IDataBootstrap {

    @Autowired
    private IUnitTypeDAO unitTypeDAO;

    @Transactional
    @Override
    public void loadRequiredData() {
        for (PredefinedUnitTypes predefinedUnitType : PredefinedUnitTypes
                .values()) {
            if (!unitTypeDAO
                    .existsUnitTypeByNameInAnotherTransaction(predefinedUnitType
                    .getMeasure())) {
                unitTypeDAO.save(predefinedUnitType.createUnitType());
            }
        }
    }

}
