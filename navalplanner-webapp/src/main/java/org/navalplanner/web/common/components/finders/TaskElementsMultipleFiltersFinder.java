/*
 * This file is part of NavalPlan
 *
 * Copyright (C) 2011 Igalia, S.L.
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

package org.navalplanner.web.common.components.finders;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.commons.lang.StringUtils;
import org.navalplanner.business.hibernate.notification.PredefinedDatabaseSnapshots;
import org.navalplanner.business.labels.entities.Label;
import org.navalplanner.business.labels.entities.LabelType;
import org.navalplanner.business.planner.entities.TaskElement;
import org.navalplanner.business.resources.entities.Criterion;
import org.navalplanner.business.resources.entities.CriterionType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements all the methods needed to search the criteria to filter the
 * {@link TaskElement}. Provides multiples criteria to filter like
 * {@link Criterion} and {@link Label}.
 *
 * @author Manuel Rego Casasnovas <rego@igalia.com>
 */
public class TaskElementsMultipleFiltersFinder extends MultipleFiltersFinder {

    @Autowired
    private PredefinedDatabaseSnapshots databaseSnapshots;

    protected TaskElementsMultipleFiltersFinder() {

    }

    @Override
    public List<FilterPair> getFirstTenFilters() {
        getListMatching().clear();
        fillWithFirstTenFiltersLabels();
        fillWithFirstTenFiltersCriterions();
        addNoneFilter();
        return getListMatching();
    }

    private List<FilterPair> fillWithFirstTenFiltersLabels() {
        Map<LabelType, List<Label>> mapLabels = getLabelsMap();
        Iterator<LabelType> iteratorLabelType = mapLabels.keySet().iterator();
        while (iteratorLabelType.hasNext() && getListMatching().size() < 10) {
            LabelType type = iteratorLabelType.next();
            for (int i = 0; getListMatching().size() < 10
                    && i < mapLabels.get(type).size(); i++) {
                Label label = mapLabels.get(type).get(i);
                addLabel(type, label);
            }
        }
        return getListMatching();
    }

    private Map<LabelType, List<Label>> getLabelsMap() {
        return databaseSnapshots.snapshotLabelsMap();
    }

    private List<FilterPair> fillWithFirstTenFiltersCriterions() {
        SortedMap<CriterionType, List<Criterion>> mapCriterions = getMapCriterions();
        Iterator<CriterionType> iteratorCriterionType = mapCriterions.keySet()
                .iterator();
        while (iteratorCriterionType.hasNext() && getListMatching().size() < 10) {
            CriterionType type = iteratorCriterionType.next();
            for (int i = 0; getListMatching().size() < 10
                    && i < mapCriterions.get(type).size(); i++) {
                Criterion criterion = mapCriterions.get(type).get(i);
                addCriterion(type, criterion);
            }
        }
        return getListMatching();
    }

    private SortedMap<CriterionType, List<Criterion>> getMapCriterions() {
        return databaseSnapshots.snapshotCriterionsMap();
    }

    @Override
    public List<FilterPair> getMatching(String filter) {
        getListMatching().clear();
        if ((filter != null) && (!filter.isEmpty())) {
            filter = StringUtils.deleteWhitespace(filter.toLowerCase());
            searchInCriterionTypes(filter);
            searchInLabelTypes(filter);
        }

        addNoneFilter();
        return getListMatching();
    }

    private void searchInCriterionTypes(String filter) {
        boolean limited = (filter.length() < 3);
        for (CriterionType type : getMapCriterions().keySet()) {
            String name = StringUtils.deleteWhitespace(type.getName()
                    .toLowerCase());
            if (name.contains(filter)) {
                setFilterPairCriterionType(type, limited);
            } else {
                searchInCriterions(type, filter);
            }
        }
    }

    private void searchInCriterions(CriterionType type, String filter) {
        List<Criterion> list = getMapCriterions().get(type);
        if (list == null) {
            return;
        }
        for (Criterion criterion : list) {
            String name = StringUtils.deleteWhitespace(criterion.getName()
                    .toLowerCase());
            if (name.contains(filter)) {
                addCriterion(type, criterion);
                if ((filter.length() < 3) && (getListMatching().size() > 9)) {
                    return;
                }
            }
        }
    }

    private void setFilterPairCriterionType(CriterionType type, boolean limited) {
        List<Criterion> list = getMapCriterions().get(type);
        if (list == null) {
            return;
        }
        for (Criterion criterion : list) {
            addCriterion(type, criterion);
            if ((limited) && (getListMatching().size() > 9)) {
                return;
            }
        }
    }

    private void searchInLabelTypes(String filter) {
        boolean limited = (filter.length() < 3);
        for (LabelType type : getLabelsMap().keySet()) {
            String name = StringUtils.deleteWhitespace(type.getName()
                    .toLowerCase());
            if (name.contains(filter)) {
                setFilterPairLabelType(type, limited);
            } else {
                searchInLabels(type, filter);
            }
        }
    }

    private void searchInLabels(LabelType type, String filter) {
        for (Label label : getLabelsMap().get(type)) {
            String name = StringUtils.deleteWhitespace(label.getName()
                    .toLowerCase());
            if (name.contains(filter)) {
                addLabel(type, label);
                if ((filter.length() < 3) && (getListMatching().size() > 9)) {
                    return;
                }
            }
        }
    }

    private void setFilterPairLabelType(LabelType type, boolean limited) {
        for (Label label : getLabelsMap().get(type)) {
            addLabel(type, label);
            if ((limited) && (getListMatching().size() > 9)) {
                return;
            }
        }
    }

    private void addCriterion(CriterionType type, Criterion criterion) {
        String pattern = criterion.getName() + " ( " + type.getName() + " )";
        getListMatching().add(
                new FilterPair(TaskElementFilterEnum.Criterion, type
                        .getResource().toLowerCase(), pattern, criterion));
    }

    private void addLabel(LabelType type, Label label) {
        String pattern = label.getName() + " ( " + type.getName() + " )";
        getListMatching().add(
                new FilterPair(TaskElementFilterEnum.Label, pattern, label));
    }

}
