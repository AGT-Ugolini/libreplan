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

package org.navalplanner.business.reports.dtos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;
import org.navalplanner.business.advance.entities.AdvanceType;
import org.navalplanner.business.advance.entities.DirectAdvanceAssignment;
import org.navalplanner.business.common.Registry;
import org.navalplanner.business.orders.entities.Order;
import org.navalplanner.business.orders.entities.OrderElement;
import org.navalplanner.business.planner.entities.DayAssignment;
import org.navalplanner.business.planner.entities.Task;
import org.navalplanner.business.planner.entities.TaskElement;
import org.navalplanner.business.workreports.daos.IWorkReportLineDAO;
import org.navalplanner.business.workreports.entities.WorkReportLine;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Diego Pino Garcia <dpino@igalia.com>
 *
 */
public class WorkingArrangementsPerOrderDTO {

    private IWorkReportLineDAO workReportLineDAO;

    private String orderName;

    private Integer estimatedHours;

    private Integer totalPlannedHours;

    private Integer partialPlannedHours;

    private Integer realHours;

    private BigDecimal averageProgress;

    private Double imputedProgress;

    private Double plannedProgress;

    private BigDecimal costDifference;

    private BigDecimal planningDifference;

    private BigDecimal ratioCostDifference;

    private BigDecimal ratioPlanningDifference;

    private Boolean advanceTypeDoesNotApply = Boolean.FALSE;

    private WorkingArrangementsPerOrderDTO() {
        workReportLineDAO = Registry.getWorkReportLineDAO();
    }

    public WorkingArrangementsPerOrderDTO(Order order, AdvanceType advanceType, LocalDate date) {
        this();
        this.orderName = order.getName();

        // Get average progress
        BigDecimal averageProgress;
        if (advanceType != null) {
            averageProgress = order.getAdvancePercentage(advanceType, date);
        } else {
            DirectAdvanceAssignment directAdvanceAssignment = order.getReportGlobalAdvanceAssignment();
            averageProgress = directAdvanceAssignment.getAdvancePercentage(date);
        }

        if (averageProgress == null) {
            advanceTypeDoesNotApply = true;
            return;
        }

        // Fill DTO

        // Total hours calculations
        final List<Task> tasks = getTasks(order);
        this.estimatedHours = getHoursSpecifiedAtOrder(tasks);
        this.totalPlannedHours = calculatePlannedHours(tasks, null);

        // Hours on time calculations
        this.partialPlannedHours = calculatePlannedHours(tasks, date);
        this.realHours = calculateRealHours(tasks, date);

        // Progress calculations
        this.averageProgress = averageProgress;
        this.imputedProgress = (totalPlannedHours != 0) ? new Double(realHours
                / totalPlannedHours.doubleValue()) : new Double(0);
        this.plannedProgress = (totalPlannedHours != 0) ? new Double(
                partialPlannedHours / totalPlannedHours.doubleValue())
                : new Double(0);

        // Differences calculations
        this.costDifference = calculateCostDifference(averageProgress,
                new BigDecimal(totalPlannedHours), new BigDecimal(realHours));
        this.planningDifference = calculatePlanningDifference(averageProgress,
                new BigDecimal(totalPlannedHours), new BigDecimal(
                        partialPlannedHours));
        this.ratioCostDifference = calculateRatioCostDifference(
                averageProgress, imputedProgress);
        this.ratioPlanningDifference = calculateRatioPlanningDifference(
                averageProgress, plannedProgress);
    }

    private List<Task> getTasks(Order order) {
        List<Task> result = new ArrayList<Task>();

        for (OrderElement orderElement: order.getOrderElements()) {
            for (TaskElement task: orderElement.getTaskElements()) {
                if (task instanceof Task) {
                    result.add((Task) task);
                }
            }
        }
        return result;
    }

    private Integer getHoursSpecifiedAtOrder(List<Task> tasks) {
        Integer result = new Integer(0);

        for (Task each: tasks) {
            result += each.getHoursSpecifiedAtOrder();
        }
        return result;
    }

    public Integer calculatePlannedHours(List<Task> tasks, LocalDate date) {
        Integer result = new Integer(0);

        for (Task each: tasks) {
            result += calculatePlannedHours(each, date);
        }
        return result;
    }

    public Integer calculatePlannedHours(Task task, LocalDate date) {
        Integer result = new Integer(0);

        final List<DayAssignment> dayAssignments = task.getDayAssignments();
        if (dayAssignments.isEmpty()) {
            return result;
        }

        for (DayAssignment dayAssignment : dayAssignments) {
            if (date == null || dayAssignment.getDay().compareTo(date) <= 0) {
                result += dayAssignment.getHours();
            }
        }
        return result;
    }

    public Integer calculateRealHours(List<Task> tasks, LocalDate date) {
        Integer result = new Integer(0);

        for (Task each: tasks) {
            result += calculateRealHours(each, date);
        }
        return result;
    }

    public Integer calculateRealHours(Task task, LocalDate date) {
        Integer result = new Integer(0);

        final List<WorkReportLine> workReportLines = workReportLineDAO
                .findByOrderElementAndChildren(task.getOrderElement());
        if (workReportLines.isEmpty()) {
            return result;
        }

        for (WorkReportLine workReportLine : workReportLines) {
            final LocalDate workReportLineDate = new LocalDate(workReportLine.getDate());
            if (date == null || workReportLineDate.compareTo(date) <= 0) {
                result += workReportLine.getNumHours();
            }
        }
        return result;
    }

    public Integer getEstimatedHours() {
        return estimatedHours;
    }

    public Integer getTotalPlannedHours() {
        return totalPlannedHours;
    }

    public Integer getPartialPlannedHours() {
        return partialPlannedHours;
    }

    public Integer getRealHours() {
        return realHours;
    }

    public BigDecimal getAverageProgress() {
        return averageProgress;
    }

    public Double getImputedProgress() {
        return imputedProgress;
    }

    public Double getPlannedProgress() {
        return plannedProgress;
    }

    public String getOrderName() {
        return orderName;
    }

    public BigDecimal calculateCostDifference(BigDecimal averageProgress,
            BigDecimal totalPlannedHours, BigDecimal realHours) {
        BigDecimal result = averageProgress;
        result = result.multiply(totalPlannedHours);
        return result.subtract(realHours);
    }

    public BigDecimal calculatePlanningDifference(BigDecimal averageProgress,
            BigDecimal totalPlannedHours, BigDecimal partialPlannedHours) {
        BigDecimal result = averageProgress;
        result = result.multiply(totalPlannedHours);
        return result.subtract(partialPlannedHours);
    }

    public BigDecimal calculateRatioCostDifference(BigDecimal averageProgress, Double imputedProgress) {
        if (imputedProgress.doubleValue() == 0) {
            return new BigDecimal(0);
        }
        return averageProgress.divide(new BigDecimal(imputedProgress), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateRatioPlanningDifference(BigDecimal averageProgress, Double plannedProgress) {
        if (plannedProgress.doubleValue() == 0) {
            return new BigDecimal(0);
        }
        return averageProgress.divide(new BigDecimal(plannedProgress), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getCostDifference() {
        return costDifference;
    }

    public BigDecimal getPlanningDifference() {
        return planningDifference;
    }

    public BigDecimal getRatioCostDifference() {
        return ratioCostDifference;
    }

    public BigDecimal getRatioPlanningDifference() {
        return ratioPlanningDifference;
    }

    public Boolean getAdvanceTypeDoesNotApply() {
        return advanceTypeDoesNotApply;
    }

}
