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

package org.navalplanner.business.planner.daos;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.joda.time.LocalDate;
import org.navalplanner.business.common.daos.IGenericDAO;
import org.navalplanner.business.planner.entities.GenericResourceAllocation;
import org.navalplanner.business.planner.entities.ResourceAllocation;
import org.navalplanner.business.planner.entities.SpecificDayAssignment;
import org.navalplanner.business.planner.entities.Task;
import org.navalplanner.business.resources.entities.Criterion;
import org.navalplanner.business.resources.entities.Resource;

/**
 * DAO interface for {@link ResourceAllocation}
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
public interface IResourceAllocationDAO extends
        IGenericDAO<ResourceAllocation, Long> {

    List<ResourceAllocation<?>> findAllocationsRelatedToAnyOf(
            List<Resource> resources);

    List<ResourceAllocation<?>> findAllocationsRelatedToAnyOf(
            List<Resource> resources, Date intervalFilterStartDate, Date intervalFilterEndDate);

    List<ResourceAllocation<?>> findAllocationsRelatedTo(Resource resource);

    List<ResourceAllocation<?>> findAllocationsRelatedTo(Resource resource,
            Date intervalFilterStartDate, Date intervalFilterEndDate);

    Map<Criterion, List<GenericResourceAllocation>> findGenericAllocationsByCriterion();

    Map<Criterion, List<GenericResourceAllocation>> findGenericAllocationsByCriterion(
            Date intervalFilterStartDate, Date intervalFilterEndDate);

    List<SpecificDayAssignment> getSpecificAssignmentsBetween(
            Collection<Resource> relatedToOne,
            LocalDate start, LocalDate end);

    Map<Criterion, List<GenericResourceAllocation>> findGenericAllocationsByCriterionFor(
            List<Task> task);

    Map<Criterion, List<GenericResourceAllocation>> findGenericAllocationsBySomeCriterion(
            List<Criterion> criterions);

}