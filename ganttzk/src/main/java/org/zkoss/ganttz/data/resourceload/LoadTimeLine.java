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

package org.zkoss.ganttz.data.resourceload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.joda.time.LocalDate;
import org.zkoss.ganttz.util.Interval;

public class LoadTimeLine {

    private final String conceptName;
    private final List<LoadPeriod> loadPeriods;

    private final String type;

    private final List<LoadTimeLine> children;

    public LoadTimeLine(String conceptName, List<LoadPeriod> loadPeriods) {
        Validate.notEmpty(conceptName);
        Validate.notNull(loadPeriods);
        this.loadPeriods = LoadPeriod.sort(loadPeriods);
        this.conceptName = conceptName;
        this.type = "";
        this.children = Collections
        .unmodifiableList(new ArrayList<LoadTimeLine>());
    }

    public LoadTimeLine(String conceptName, List<LoadPeriod> loadPeriods,
            String type) {
        Validate.notEmpty(conceptName);
        Validate.notNull(loadPeriods);
        this.loadPeriods = LoadPeriod.sort(loadPeriods);
        this.conceptName = conceptName;
        this.type = type;
        this.children = Collections
                .unmodifiableList(new ArrayList<LoadTimeLine>());
    }

    public LoadTimeLine(LoadTimeLine principal, List<LoadTimeLine> children) {
        Validate.notEmpty(principal.getConceptName());
        Validate.notNull(principal.getLoadPeriods());
        this.loadPeriods = LoadPeriod.sort(principal.getLoadPeriods());
        this.conceptName = principal.getConceptName();
        this.type = principal.getType();
        Validate.notNull(children);
        allChildrenAreNotEmpty(children);
        this.children = Collections
                .unmodifiableList(new ArrayList<LoadTimeLine>(children));

    }

    public List<LoadPeriod> getLoadPeriods() {
        return loadPeriods;
    }

    public String getConceptName() {
        return conceptName;
    }

    private LoadPeriod getFirst() {
        return loadPeriods.get(0);
    }

    private LoadPeriod getLast() {
        return loadPeriods.get(loadPeriods.size() - 1);
    }

    public LocalDate getStartPeriod() {
        if (isEmpty()) {
            return null;
        }
        return getFirst().getStart();
    }

    public boolean isEmpty() {
        return loadPeriods.isEmpty();
    }

    public LocalDate getEndPeriod() {
        if (isEmpty()) {
            return null;
        }
        return getLast().getEnd();
    }

    public String getType() {
        return this.type;
    }

    public static Interval getIntervalFrom(List<LoadTimeLine> timeLines) {
        Validate.notEmpty(timeLines);
        LocalDate start = null;
        LocalDate end = null;
        for (LoadTimeLine loadTimeLine : timeLines) {
            Validate.notNull(loadTimeLine.getStart());
            start = min(start, loadTimeLine.getStart());
            Validate.notNull(loadTimeLine.getEnd());
            end = max(end, loadTimeLine.getEnd());
        }
        return new Interval(toDate(start), toDate(end));
    }

    private static Date toDate(LocalDate localDate) {
        return localDate.toDateTimeAtStartOfDay().toDate();
    }

    private static LocalDate max(LocalDate one, LocalDate other) {
        if (one == null) {
            return other;
        }
        if (other == null) {
            return one;
        }
        return one.compareTo(other) > 0 ? one : other;
    }

    private static LocalDate min(LocalDate one, LocalDate other) {
        if (one == null) {
            return other;
        }
        if (other == null) {
            return one;
        }
        return one.compareTo(other) < 0 ? one : other;
    }

    private static void allChildrenAreNotEmpty(List<LoadTimeLine> lines) {
        for (LoadTimeLine l : lines) {
            if (l.isEmpty()) {
                throw new IllegalArgumentException(l + " is empty");
            }
            if (l.hasChildren()) {
                allChildrenAreNotEmpty(l.getChildren());
            }
        }
    }

    public boolean hasChildren() {
        return (!children.isEmpty());
    }

    public List<LoadTimeLine> getChildren() {
        return children;
    }

    public List<LoadTimeLine> getAllChildren() {
        List<LoadTimeLine> result = new ArrayList<LoadTimeLine>();
        for (LoadTimeLine child : children) {
            result.addAll(child.getAllChildren());
            result.add(child);
        }
        return result;
    }

    public LocalDate getStart() {
        LocalDate result = getStartPeriod();
        for (LoadTimeLine loadTimeLine : getChildren()) {
            LocalDate start = loadTimeLine.getStart();
            if (start != null) {
            result = result == null || result.compareTo(start) > 0 ? start
                    : result;
            }
        }
        return result;
    }

    public LocalDate getEnd() {
        LocalDate result = getEndPeriod();
        for (LoadTimeLine loadTimeLine : getChildren()) {
            LocalDate end = loadTimeLine.getEnd();
            if (end != null) {
            result = result == null || result.compareTo(end) < 0 ? end : result;
            }
        }
        return result;
    }

}
