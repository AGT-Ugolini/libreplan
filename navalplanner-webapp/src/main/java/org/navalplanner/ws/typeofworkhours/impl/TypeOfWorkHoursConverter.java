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

package org.navalplanner.ws.typeofworkhours.impl;

import org.apache.commons.lang.StringUtils;
import org.navalplanner.business.common.exceptions.ValidationException;
import org.navalplanner.business.costcategories.entities.TypeOfWorkHours;
import org.navalplanner.ws.typeofworkhours.api.TypeOfWorkHoursDTO;

/**
 * Converter from/to Type-Of-Work-Hours-related entities to/from DTOs.
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */
public class TypeOfWorkHoursConverter {

    private TypeOfWorkHoursConverter() {
    }

    public final static TypeOfWorkHoursDTO toDTO(TypeOfWorkHours typeOfWorkHours) {
        return new TypeOfWorkHoursDTO(typeOfWorkHours.getCode(),
                typeOfWorkHours.getName(), typeOfWorkHours.getEnabled(),
                typeOfWorkHours.getDefaultPrice());

    }

    public final static TypeOfWorkHours toEntity(
            TypeOfWorkHoursDTO typeOfWorkHoursDTO) {

        TypeOfWorkHours typeOfWorkHours = TypeOfWorkHours.createUnvalidated(
                StringUtils.trim(typeOfWorkHoursDTO.code), StringUtils
                        .trim(typeOfWorkHoursDTO.name),
                typeOfWorkHoursDTO.enabled, typeOfWorkHoursDTO.defaultPrice);

        return typeOfWorkHours;

    }

    public final static void updateTypeOfWorkHours(
            TypeOfWorkHours typeOfWorkHours,
            TypeOfWorkHoursDTO typeOfWorkHoursDTO) throws ValidationException {
        /* 1: Update typeOfWorkHours basic properties. */
        typeOfWorkHours.updateUnvalidated(StringUtils
                .trim(typeOfWorkHoursDTO.name), typeOfWorkHoursDTO.enabled,
                typeOfWorkHoursDTO.defaultPrice);

    }
}
