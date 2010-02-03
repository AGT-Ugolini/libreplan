/*
 * This file is part of ###PROJECT_NAME###
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

package org.navalplanner.web.test.ws.common;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.navalplanner.ws.common.api.InstanceConstraintViolationsDTO;
import org.navalplanner.ws.common.api.InstanceConstraintViolationsListDTO;

/**
 * Utilities class related with web service tests.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 * @author Fernando Bellas Permuy <fbellas@udc.es>
 */
public class Util {

    public static Matcher<String> mustEnd(final String property) {
        return new BaseMatcher<String>() {

            @Override
            public boolean matches(Object object) {
                if (object instanceof String) {
                    String s = (String) object;
                    return s.endsWith(property);
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("must end with " + property);
            }
        };
    }

    public static String getUniqueName() {
        return UUID.randomUUID().toString();
    }

    public static void assertNoConstraintViolations(
        InstanceConstraintViolationsListDTO
        instanceConstraintViolationsListDTO) {

        assertTrue(
            instanceConstraintViolationsListDTO.
            instanceConstraintViolationsList.toString(),
            instanceConstraintViolationsListDTO.
            instanceConstraintViolationsList.size() == 0);

    }

    public static void assertOneConstraintViolation(
        InstanceConstraintViolationsListDTO
        instanceConstraintViolationsListDTO) {

        List<InstanceConstraintViolationsDTO> instanceConstraintViolationsList =
            instanceConstraintViolationsListDTO.
                instanceConstraintViolationsList;

        assertTrue(
            instanceConstraintViolationsList.toString(),
            instanceConstraintViolationsList.size() == 1);
        assertTrue(
            instanceConstraintViolationsList.get(0).
            constraintViolations.toString(),
            instanceConstraintViolationsList.get(0).
            constraintViolations.size() == 1);

    }

    public static void assertOneConstraintViolationPerInstance(
        InstanceConstraintViolationsListDTO
        instanceConstraintViolationsListDTO, int numberOfInstances) {

        List<InstanceConstraintViolationsDTO> instanceConstraintViolationsList =
            instanceConstraintViolationsListDTO.
                instanceConstraintViolationsList;

         assertTrue(
             instanceConstraintViolationsList.toString(),
             instanceConstraintViolationsList.size() == numberOfInstances);

         for (InstanceConstraintViolationsDTO i :
             instanceConstraintViolationsList) {
             assertTrue(
                 i.constraintViolations.toString(),
                 i.constraintViolations.size() == 1);
         }

    }

}
