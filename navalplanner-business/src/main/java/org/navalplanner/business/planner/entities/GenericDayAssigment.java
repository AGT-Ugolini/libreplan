package org.navalplanner.business.planner.entities;

import java.util.HashSet;
import java.util.Set;

import org.joda.time.LocalDate;
import org.navalplanner.business.resources.entities.Criterion;
import org.navalplanner.business.resources.entities.Resource;

/**
 *
 * @author Diego Pino García <dpino@igalia.com>
 *
 */
public class GenericDayAssigment extends DayAssigment {

    private GenericResourceAllocation genericResourceAllocation;

    private Set<Criterion> criterions = new HashSet<Criterion>();

    public static GenericDayAssigment create(LocalDate day, int hours,
            Resource resource) {
        return (GenericDayAssigment) create(new GenericDayAssigment(day, hours,
                resource));
    }

    private GenericDayAssigment(LocalDate day, int hours, Resource resource) {
        super(day, hours, resource);
    }

    /**
     * Constructor for hibernate. DO NOT USE!
     */
    public GenericDayAssigment() {

    }

    public Set<Criterion> getCriterions() {
        return criterions;
    }

    public void setCriterions(Set<Criterion> criterions) {
        this.criterions = criterions;
    }

    public GenericResourceAllocation getGenericResourceAllocation() {
        return genericResourceAllocation;
    }

    public void setGenericResourceAllocation(
            GenericResourceAllocation genericResourceAllocation) {
        this.genericResourceAllocation = genericResourceAllocation;
    }

}
