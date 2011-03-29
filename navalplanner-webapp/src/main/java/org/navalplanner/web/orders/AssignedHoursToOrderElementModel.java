/*
 * This file is part of NavalPlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                         Desenvolvemento Tecnolóxico de Galicia
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.joda.time.LocalDate;
import org.navalplanner.business.orders.daos.IOrderElementDAO;
import org.navalplanner.business.orders.entities.OrderElement;
import org.navalplanner.business.reports.dtos.WorkReportLineDTO;
import org.navalplanner.business.workreports.daos.IWorkReportLineDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to show the asigned hours of a selected order element
 *
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class AssignedHoursToOrderElementModel implements
        IAssignedHoursToOrderElementModel {

    @Autowired
    private final IWorkReportLineDAO workReportLineDAO;

    @Autowired
    private IOrderElementDAO orderElementDAO;

    private int assignedDirectHours;

    private OrderElement orderElement;

    private List<WorkReportLineDTO> listWRL;

    @Autowired
    public AssignedHoursToOrderElementModel(IWorkReportLineDAO workReportLineDAO) {
        Validate.notNull(workReportLineDAO);
        this.workReportLineDAO = workReportLineDAO;
        this.assignedDirectHours = 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkReportLineDTO> getWorkReportLines() {
        if (orderElement == null) {
            return new ArrayList<WorkReportLineDTO>();
        }
        orderElementDAO.reattach(orderElement);
        this.assignedDirectHours = 0;
        this.listWRL = workReportLineDAO
                .findByOrderElementGroupByResourceAndHourTypeAndDate(orderElement);

        this.listWRL = groupByDate(listWRL);
        Iterator<WorkReportLineDTO> iterador = listWRL.iterator();
        while (iterador.hasNext()) {
            WorkReportLineDTO w = iterador.next();
            w.getResource().getShortDescription();
            w.getTypeOfWorkHours().getName();
            this.assignedDirectHours = this.assignedDirectHours
                    + w.getSumHours();
        }
        return sortByDate(listWRL);
    }

    private List<WorkReportLineDTO> sortByDate(List<WorkReportLineDTO> listWRL) {
        Collections.sort(listWRL, new Comparator<WorkReportLineDTO>() {
            public int compare(WorkReportLineDTO arg0, WorkReportLineDTO arg1) {
            if (arg0.getDate() == null) {
                return -1;
            }
            if (arg1.getDate() == null) {
                return 1;
            }
                return arg0.getDate().compareTo(arg1.getDate());
            }
        });
        return listWRL;
    }

    private List<WorkReportLineDTO> groupByDate(
            List<WorkReportLineDTO> listWRL) {
        List<WorkReportLineDTO> groupedByDateList = new ArrayList<WorkReportLineDTO>();

        if (!listWRL.isEmpty()) {
            Iterator<WorkReportLineDTO> iterador = listWRL.iterator();
            WorkReportLineDTO currentWRL = iterador.next();
            groupedByDateList.add(currentWRL);

            while (iterador.hasNext()) {
                WorkReportLineDTO nextWRL = iterador.next();

                LocalDate currentDate = currentWRL.getLocalDate();
                LocalDate nextDate = nextWRL.getLocalDate();

                if ((currentWRL.getResource().getId().equals(nextWRL
                        .getResource().getId()))
                        && (currentWRL.getTypeOfWorkHours().getId()
                                .equals(nextWRL.getTypeOfWorkHours().getId()))
                        && (currentDate.compareTo(nextDate) == 0)) {
                    // sum the number of hours to the next WorkReportLineDTO
                    currentWRL.setSumHours(currentWRL.getSumHours()
                            + nextWRL.getSumHours());
                } else {
                    groupedByDateList.add(nextWRL);
                    currentWRL = nextWRL;
                }
            }
        }
        return groupedByDateList;
    }

    @Override
    public int getAssignedDirectHours() {
        if (orderElement == null) {
            return 0;
        }
        return this.assignedDirectHours;
    }

    @Override
    public int getTotalAssignedHours() {
        if (orderElement == null) {
            return 0;
        }
        return this.orderElement.getSumChargedHours().getTotalChargedHours();
    }

    @Override
    @Transactional(readOnly = true)
    public int getAssignedDirectHoursChildren() {
        if (orderElement == null) {
            return 0;
        }
        if (orderElement.getChildren().isEmpty()) {
            return 0;
        }
        int assignedDirectChildren = getTotalAssignedHours()
                - this.assignedDirectHours;
        return assignedDirectChildren;
    }

    @Override
    @Transactional(readOnly = true)
    public void initOrderElement(OrderElement orderElement) {
        this.orderElement = orderElement;
    }

    @Override
    @Transactional(readOnly = true)
    public int getEstimatedHours() {
        if (orderElement == null) {
            return 0;
        }
        return orderElement.getWorkHours();
    }

    @Override
    @Transactional(readOnly = true)
    public int getProgressWork() {
        if (orderElement == null) {
            return 0;
        }
        return orderElementDAO.getHoursAdvancePercentage(orderElement)
                .multiply(new BigDecimal(100)).intValue();
    }

}
