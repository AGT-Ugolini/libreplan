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

import java.util.List;
import java.util.Set;

import org.zkoss.ganttz.data.Dependency;
import org.zkoss.ganttz.data.IDependency;
import org.zkoss.ganttz.data.Task;

/**
 * Basic needed methods to calculate the critical path method over a graph of
 * {@link Task} joined by {@link Dependency}.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
public interface ICriticalPathCalculable<T> {

    List<T> getInitialTasks();

    List<T> getLatestTasks();

    Set<T> getIncomingTasksFor(T task);

    Set<T> getOutgoingTasksFor(T task);

    IDependency<T> getDependencyFrom(T from, T to);

    List<T> getTasks();

    boolean isContainer(T task);

    boolean contains(T container, T task);

}
