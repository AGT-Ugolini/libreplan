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

package org.navalplanner.business.users.entities;

/**
 * Entity for modeling a order authorization related with a {@link User}.
 *
 * @author Jacobo Aragunde Perez <jaragunde@igalia.com>
 */
public class UserOrderAuthorization extends OrderAuthorization {

    /**
     * Necessary for Hibernate.
     */
    public UserOrderAuthorization() {}

    public UserOrderAuthorization(OrderAuthorizationType type) {
        setAuthorizationType(type);
    }

    public static UserOrderAuthorization create() {
        return create(new UserOrderAuthorization());
    }

    public static UserOrderAuthorization create(OrderAuthorizationType type) {
        return create(new UserOrderAuthorization(type));
    }
}
