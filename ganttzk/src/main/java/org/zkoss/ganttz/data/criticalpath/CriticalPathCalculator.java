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

package org.zkoss.ganttz.data.criticalpath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.zkoss.ganttz.data.DependencyType;
import org.zkoss.ganttz.data.GanttDate;
import org.zkoss.ganttz.data.IDependency;
import org.zkoss.ganttz.data.ITaskFundamentalProperties;
import org.zkoss.ganttz.data.constraint.Constraint;

/**
 * Class that calculates the critical path of a Gantt diagram graph.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
public class CriticalPathCalculator<T extends ITaskFundamentalProperties> {

    private ICriticalPathCalculable<T> graph;

    private LocalDate initDate;

    private Map<T, Node<T>> nodes;

    private InitialNode<T> bop;
    private LastNode<T> eop;

    public List<T> calculateCriticalPath(ICriticalPathCalculable<T> graph) {
        this.graph = graph;

        initDate = calculateInitDate();

        bop = createBeginningOfProjectNode();
        eop = createEndOfProjectNode();

        nodes = createGraphNodes();

        forward(bop, null);
        eop.updateLatestValues();

        backward(eop, null);

        return getTasksOnCriticalPath();
    }

    private LocalDate calculateInitDate() {
        List<T> initialTasks = graph.getInitialTasks();
        if (initialTasks.isEmpty()) {
            return null;
        }
        GanttDate ganttDate = Collections.min(getStartDates());
        return LocalDate.fromDateFields(ganttDate.toDateApproximation());
    }

    private List<GanttDate> getStartDates() {
        List<GanttDate> result = new ArrayList<GanttDate>();
        for (T task : graph.getInitialTasks()) {
            result.add(task.getBeginDate());
        }
        return result;
    }

    private InitialNode<T> createBeginningOfProjectNode() {
        return new InitialNode<T>(new HashSet<T>(graph.getInitialTasks()));
    }

    private LastNode<T> createEndOfProjectNode() {
        return new LastNode<T>(new HashSet<T>(graph.getLatestTasks()));
    }

    private Map<T, Node<T>> createGraphNodes() {
        Map<T, Node<T>> result = new HashMap<T, Node<T>>();

        for (T task : graph.getTasks()) {
            Node<T> node = new Node<T>(task, graph.getIncomingTasksFor(task),
                    graph.getOutgoingTasksFor(task));
            result.put(task, node);
        }

        return result;
    }

    private DependencyType getDependencyTypeEndStartByDefault(T from, T to) {
        if ((from != null) && (to != null)) {
            IDependency<T> dependency = graph.getDependencyFrom(from, to);
            if (dependency != null) {
                return dependency.getType();
            }
        }
        return DependencyType.END_START;
    }

    private void forward(Node<T> currentNode, T previousTask) {
        T currentTask = currentNode.getTask();
        int earliestStart = currentNode.getEarliestStart();
        int earliestFinish = currentNode.getEarliestFinish();

        Set<T> nextTasks = currentNode.getNextTasks();
        if (nextTasks.isEmpty()) {
            eop.setEarliestStart(earliestFinish);
        } else {
            int countStartStart = 0;

            for (T task : nextTasks) {
                if (graph.isContainer(currentTask)) {
                    if (graph.contains(currentTask, previousTask)) {
                        if (graph.contains(currentTask, task)) {
                            continue;
                        }
                    }
                }

                Node<T> node = nodes.get(task);
                DependencyType dependencyType = getDependencyTypeEndStartByDefault(
                        currentTask, task);
                Constraint<GanttDate> constraint = getDateConstraint(task);

                switch (dependencyType) {
                case START_START:
                    setEarliestStart(node, earliestStart, constraint);
                    countStartStart++;
                    break;
                case END_END:
                    setEarliestStart(node, earliestFinish - node.getDuration(),
                            constraint);
                    break;
                case END_START:
                default:
                    setEarliestStart(node, earliestFinish, constraint);
                    break;
                }

                forward(node, currentTask);
            }

            if (nextTasks.size() == countStartStart) {
                eop.setEarliestStart(earliestFinish);
            }
        }
    }

    private void setEarliestStart(Node<T> node, int earliestStart,
            Constraint<GanttDate> constraint) {
        if (constraint != null) {
            GanttDate date = GanttDate.createFrom(initDate
                    .plusDays(earliestStart));
            date = constraint.applyTo(date);
            earliestStart = Days.daysBetween(initDate,
                    LocalDate.fromDateFields(date.toDateApproximation()))
                    .getDays();
        }
        node.setEarliestStart(earliestStart);
    }

    private Constraint<GanttDate> getDateConstraint(T task) {
        if (task == null) {
            return null;
        }

        List<Constraint<GanttDate>> constraints = task.getStartConstraints();
        if (constraints == null) {
            return null;
        }
        return Constraint.coalesce(constraints);
    }

    private void backward(Node<T> currentNode, T nextTask) {
        T currentTask = currentNode.getTask();
        int latestStart = currentNode.getLatestStart();
        int latestFinish = currentNode.getLatestFinish();

        Set<T> previousTasks = currentNode.getPreviousTasks();
        if (previousTasks.isEmpty()) {
            bop.setLatestFinish(latestStart);
        } else {
            int countEndEnd = 0;

            for (T task : previousTasks) {
                if (graph.isContainer(currentTask)) {
                    if (graph.contains(currentTask, nextTask)) {
                        if (graph.contains(currentTask, task)) {
                            continue;
                        }
                    }
                }

                Node<T> node = nodes.get(task);
                DependencyType dependencyType = getDependencyTypeEndStartByDefault(
                        task, currentTask);
                Constraint<GanttDate> constraint = getDateConstraint(task);

                switch (dependencyType) {
                case START_START:
                    setLatestFinish(node, latestStart + node.getDuration(),
                            constraint);
                    break;
                case END_END:
                    setLatestFinish(node, latestFinish, constraint);
                    countEndEnd++;
                    break;
                case END_START:
                default:
                    setLatestFinish(node, latestStart, constraint);
                    break;
                }

                backward(node, currentTask);
            }

            if (previousTasks.size() == countEndEnd) {
                bop.setLatestFinish(latestStart);
            }
        }
    }

    private void setLatestFinish(Node<T> node, int latestFinish,
            Constraint<GanttDate> constraint) {
        if (constraint != null) {
            int duration = node.getDuration();
            GanttDate date = GanttDate.createFrom(initDate.plusDays(latestFinish - duration));
            date = constraint.applyTo(date);
            int daysBetween = Days.daysBetween(initDate,
                    LocalDate.fromDateFields(date.toDateApproximation()))
                    .getDays();
            latestFinish = daysBetween + duration;
        }
        node.setLatestFinish(latestFinish);
    }

    private List<T> getTasksOnCriticalPath() {
        List<T> result = new ArrayList<T>();

        for (Node<T> node : nodes.values()) {
            if (node.getLatestStart() == node.getEarliestStart()) {
                result.add(node.getTask());
            }
        }

        return result;
    }

}
