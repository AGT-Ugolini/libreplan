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
package org.navalplanner.business.orders.entities;

import org.hibernate.validator.NotEmpty;

/**
 * @author  Óscar González Fernández <ogonzalez@igalia.com>
 */
public class InfoComponent {

    private String code;

    private String name;

    private String description;

    public InfoComponent() {
    }

    public void setCode(String code) {
        this.code = code;
    }

    @NotEmpty(message = "code not specified")
    public String getCode() {
        return code;
    }

    public void setName(String name) {
        this.name = name;
    }

    @NotEmpty(message = "name not specified")
    public String getName() {
        return name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public InfoComponent copy() {
        InfoComponent result = new InfoComponent();
        result.setCode(getCode());
        result.setName(getName());
        result.setDescription(getDescription());
        return result;
    }
}