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
package org.navalplanner.web.orders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.navalplanner.business.qualityforms.daos.IQualityFormDAO;
import org.navalplanner.business.qualityforms.entities.QualityForm;
import org.navalplanner.business.qualityforms.entities.QualityFormItem;

/**
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 *
 */
public class QualityFormsOnConversation {

    private final IQualityFormDAO qualityFormDAO;

    private Set<QualityForm> qualityForms = new HashSet<QualityForm>();

    public QualityFormsOnConversation(IQualityFormDAO qualityFormDAO) {
        this.qualityFormDAO = qualityFormDAO;
    }

    public List<QualityForm> getQualityForms() {
        return new ArrayList<QualityForm>(qualityForms);
    }

    public void initialize() {
        qualityForms = new HashSet<QualityForm>(qualityFormDAO.getAll());
        intialize(qualityForms);
    }

    private void intialize(Collection<QualityForm> qualityForms) {
        for (QualityForm each : qualityForms) {
            initialize(each);
        }
    }

    private void initialize(QualityForm qualityForm) {
        qualityForm.getName();
        qualityForm.getQualityFormType();
        for (QualityFormItem qualityFormItem : qualityForm
                .getQualityFormItems()) {
            qualityFormItem.getName();
        }
    }

    public void reattach() {
        for (QualityForm each : qualityForms) {
            qualityFormDAO.reattach(each);
        }
    }

}
