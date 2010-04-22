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

package org.navalplanner.web.materials;

import java.util.Collection;
import java.util.List;

import org.navalplanner.business.common.exceptions.ValidationException;
import org.navalplanner.business.materials.entities.Material;
import org.navalplanner.business.materials.entities.MaterialCategory;
import org.navalplanner.business.materials.entities.UnitType;
import org.zkoss.ganttz.util.MutableTreeModel;

/**
 *
 * @author Diego Pino Garcia <dpino@igalia.com>
 *
 */
public interface IMaterialsModel {

    void addMaterialCategory(MaterialCategory parent, String categoryName) throws ValidationException;

    void addMaterialToMaterialCategory(MaterialCategory materialCategory);

    void confirmRemoveMaterialCategory(MaterialCategory materialCategory);

    void confirmSave() throws ValidationException;

    MutableTreeModel<MaterialCategory> getMaterialCategories();

    Collection<? extends Material> getMaterials();

    List<Material> getMaterials(MaterialCategory materialCategory);

    void reloadMaterialCategories();

    void removeMaterial(Material material);

    List<UnitType> getUnitTypes();

    void loadUnitTypes();

    boolean canRemoveMaterial(Material material);
}
