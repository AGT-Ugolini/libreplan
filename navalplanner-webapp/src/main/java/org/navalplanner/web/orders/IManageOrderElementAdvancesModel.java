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

package org.navalplanner.web.orders;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.navalplanner.business.advance.entities.AdvanceAssignment;
import org.navalplanner.business.advance.entities.AdvanceMeasurement;
import org.navalplanner.business.advance.entities.AdvanceType;
import org.navalplanner.business.advance.entities.DirectAdvanceAssignment;
import org.navalplanner.business.advance.entities.IndirectAdvanceAssignment;
import org.navalplanner.business.advance.exceptions.DuplicateAdvanceAssignmentForOrderElementException;
import org.navalplanner.business.advance.exceptions.DuplicateValueTrueReportGlobalAdvanceException;
import org.navalplanner.business.common.exceptions.InstanceNotFoundException;
import org.navalplanner.business.orders.entities.OrderElement;
import org.zkoss.zul.XYModel;

/**
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */
public interface IManageOrderElementAdvancesModel {

    public void prepareEditAdvanceMeasurements(AdvanceAssignment advanceAssignment);

    public List<AdvanceMeasurement> getAdvanceMeasurements();

    public List<AdvanceAssignment> getAdvanceAssignments();

    public void initEdit(OrderElement orderElement);

    public void addNewLineAdvaceAssignment();

    public void addNewLineAdvaceMeasurement();

    public void removeLineAdvanceAssignment(AdvanceAssignment advance);

    public void removeLineAdvanceMeasurement(AdvanceMeasurement advance);

    public List<AdvanceType> getPossibleAdvanceTypes(
            DirectAdvanceAssignment directAdvanceAssignment);

    public boolean isReadOnlyAdvanceMeasurements();

    public void cleanAdvance();

    public boolean isPrecisionValid(BigDecimal value);

    public boolean greatThanMaxValue(BigDecimal value);

    public boolean isDistinctValidDate(Date value,
            AdvanceMeasurement newAdvanceMeasurement);

    public BigDecimal getUnitPrecision();

    public AdvanceMeasurement getLastAdvanceMeasurement(
            DirectAdvanceAssignment advanceAssignment);

    public void sortListAdvanceMeasurement();

    public String getInfoAdvanceAssignment();

    public void confirmSave()throws InstanceNotFoundException,
            DuplicateAdvanceAssignmentForOrderElementException,
            DuplicateValueTrueReportGlobalAdvanceException;

    public BigDecimal getPercentageAdvanceMeasurement(
            AdvanceMeasurement advanceMeasurement);

    public DirectAdvanceAssignment calculateFakeDirectAdvanceAssignment(
            IndirectAdvanceAssignment indirectAdvanceAssignment);

    public BigDecimal getAdvancePercentageChildren();

    public XYModel getChartData(Set<AdvanceAssignment> selectedAdvances);

    public void refreshChangesFromOrderElement();

    public boolean isQualityForm(AdvanceAssignment advance);

}
