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

package org.navalplanner.web.labels;

import java.util.List;

import org.navalplanner.business.common.exceptions.ValidationException;
import org.navalplanner.business.labels.entities.Label;
import org.navalplanner.business.labels.entities.LabelType;

/**
 * Interface for {@link LabelTypeModel}
 *
 * @author Diego Pino Garcia <dpino@igalia.com>
 */
public interface ILabelTypeModel {

    /**
     * Removes {@link LabelType}
     *
     * @param labelType
     */
    void confirmDelete(LabelType labelType);

    /**
     * Ends conversation saving current {@link LabelType}
     */
    void confirmSave() throws ValidationException;

    /**
     * Returns {@link LabelType}
     *
     * @return
     */
    LabelType getLabelType();

    /**
     * Returns all {@link LabelType}
     *
     * @return
     */
    List<LabelType> getLabelTypes();

    /**
     * Starts conversation creating new {@link LabelType}
     */
    void initCreate();

    /**
     * Starts conversation editing {@link LabelType}
     */
    void initEdit(LabelType labelType);

    /**
     * Returns all {@link Label} for current {@link LabelType}
     *
     * @return
     */
    List<Label> getLabels();

    /**
     * Add {@link Label} to {@link LabelType}
     */
    void addLabel(String value);

    /**
     *
     * @param label
     */
    void confirmDeleteLabel(Label label);

    /**
     * Check is {@link Label} name is unique
     *
     * @param value
     */
    boolean labelNameIsUnique(String value);

    /**
     * Checks if the autogeneration of codes for Labels is active
     * @return
     */
    boolean isCodeGenerated();

    /**
     * Sets the state of the autogeneration of codes for Labels
     */
    void setCodeGenerated(boolean codeGenerated);

}
