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

package org.navalplanner.ws.materials.impl;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.navalplanner.business.common.daos.IIntegrationEntityDAO;
import org.navalplanner.business.common.exceptions.ValidationException;
import org.navalplanner.business.materials.daos.IMaterialCategoryDAO;
import org.navalplanner.business.materials.entities.MaterialCategory;
import org.navalplanner.ws.common.api.InstanceConstraintViolationsListDTO;
import org.navalplanner.ws.common.impl.GenericRESTService;
import org.navalplanner.ws.materials.api.IMaterialService;
import org.navalplanner.ws.materials.api.MaterialCategoryDTO;
import org.navalplanner.ws.materials.api.MaterialCategoryListDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * REST-based implementation of <code>IMaterialService</code>.
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */
@Path("/materialcategories/")
@Produces("application/xml")
@Service("materialServiceREST")
public class MaterialServiceREST extends
        GenericRESTService<MaterialCategory, MaterialCategoryDTO> implements
        IMaterialService {

    @Autowired
    private IMaterialCategoryDAO materialCategoryDAO;

    @Override
    @GET
    @Transactional(readOnly = true)
    public MaterialCategoryListDTO getMaterials() {
        return new MaterialCategoryListDTO(findAll());
    }

    @Override
    @POST
    @Consumes("application/xml")
    public InstanceConstraintViolationsListDTO addMaterials(
            MaterialCategoryListDTO materialCategoryListDTO) {
        return save(materialCategoryListDTO.materialCategoryDTOs);
    }

    @Override
    protected MaterialCategory toEntity(MaterialCategoryDTO entityDTO) {
        return MaterialConverter.toEntity(entityDTO);
    }

    @Override
    protected MaterialCategoryDTO toDTO(MaterialCategory entity) {
        return MaterialConverter.toDTO(entity);
    }

    @Override
    protected IIntegrationEntityDAO<MaterialCategory> getIntegrationEntityDAO() {
        return materialCategoryDAO;
    }

    @Override
    protected void updateEntity(MaterialCategory entity,
            MaterialCategoryDTO entityDTO)
            throws ValidationException {

        MaterialConverter.updateMaterialCategory(entity, entityDTO);

    }

}
