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

package org.navalplanner.ws.unittypes.impl;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.navalplanner.business.common.daos.IIntegrationEntityDAO;
import org.navalplanner.business.common.exceptions.ValidationException;
import org.navalplanner.business.materials.daos.IUnitTypeDAO;
import org.navalplanner.business.materials.entities.UnitType;
import org.navalplanner.ws.common.api.InstanceConstraintViolationsListDTO;
import org.navalplanner.ws.common.impl.GenericRESTService;
import org.navalplanner.ws.unittypes.api.IUnitTypeService;
import org.navalplanner.ws.unittypes.api.UnitTypeDTO;
import org.navalplanner.ws.unittypes.api.UnitTypeListDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * REST-based implementation of <code>IUnitTypeService</code>.
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */
@Path("/unittypes/")
@Produces("application/xml")
@Service("unitTypeServiceREST")
public class UnitTypeServiceREST extends
        GenericRESTService<UnitType, UnitTypeDTO> implements IUnitTypeService {

    @Autowired
    private IUnitTypeDAO unitTypeDAO;

    @Override
    @GET
    @Transactional(readOnly = true)
    public UnitTypeListDTO getUnitTypes() {
        return new UnitTypeListDTO(findAll());
    }

    @Override
    @POST
    @Consumes("application/xml")
    public InstanceConstraintViolationsListDTO addUnitTypes(
            UnitTypeListDTO unitTypeListDTO) {
        return save(unitTypeListDTO.unitTypeDTOs);
    }

    @Override
    protected UnitType toEntity(UnitTypeDTO entityDTO) {
        return UnitTypeConverter.toEntity(entityDTO);
    }

    @Override
    protected UnitTypeDTO toDTO(UnitType entity) {
        return UnitTypeConverter.toDTO(entity);
    }

    @Override
    protected IIntegrationEntityDAO<UnitType> getIntegrationEntityDAO() {
        return unitTypeDAO;
    }

    @Override
    protected void updateEntity(UnitType entity, UnitTypeDTO entityDTO)
            throws ValidationException {

        UnitTypeConverter.updateUnitType(entity, entityDTO);

    }

}