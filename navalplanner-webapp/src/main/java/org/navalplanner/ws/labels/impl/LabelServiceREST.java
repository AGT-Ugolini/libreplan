/*
 * This file is part of NavalPlan
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

package org.navalplanner.ws.labels.impl;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.navalplanner.business.common.daos.IIntegrationEntityDAO;
import org.navalplanner.business.common.exceptions.ValidationException;
import org.navalplanner.business.labels.daos.ILabelTypeDAO;
import org.navalplanner.business.labels.entities.LabelType;
import org.navalplanner.ws.common.api.InstanceConstraintViolationsListDTO;
import org.navalplanner.ws.common.impl.GenericRESTService;
import org.navalplanner.ws.common.impl.RecoverableErrorException;
import org.navalplanner.ws.labels.api.ILabelService;
import org.navalplanner.ws.labels.api.LabelTypeDTO;
import org.navalplanner.ws.labels.api.LabelTypeListDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * REST-based implementation of {@link ILabelService}.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
@Path("/labels/")
@Produces("application/xml")
@Service("labelServiceREST")
public class LabelServiceREST extends
        GenericRESTService<LabelType, LabelTypeDTO> implements ILabelService {

    @Autowired
    private ILabelTypeDAO labelTypeDAO;

    @Override
    @GET
    @Transactional(readOnly = true)
    public LabelTypeListDTO getLabelTypes() {
        return new LabelTypeListDTO(findAll());
    }

    @Override
    @POST
    @Consumes("application/xml")
    @Transactional
    public InstanceConstraintViolationsListDTO addLabelTypes(
            LabelTypeListDTO labelTypes) {
        return save(labelTypes.labelTypes);
    }

    @Override
    protected IIntegrationEntityDAO<LabelType> getIntegrationEntityDAO() {
        return labelTypeDAO;
    }

    @Override
    protected LabelTypeDTO toDTO(LabelType entity) {
        return LabelConverter.toDTO(entity);
    }

    @Override
    protected LabelType toEntity(LabelTypeDTO entityDTO)
            throws ValidationException, RecoverableErrorException {
        return LabelConverter.toEntity(entityDTO);
    }

    @Override
    protected void updateEntity(LabelType entity, LabelTypeDTO entityDTO)
            throws ValidationException, RecoverableErrorException {
        LabelConverter.updateLabelType(entity, entityDTO);
    }

}
