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

package org.navalplanner.web.common;

import static org.navalplanner.web.I18nHelper._;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.navalplanner.business.calendars.daos.IBaseCalendarDAO;
import org.navalplanner.business.calendars.entities.BaseCalendar;
import org.navalplanner.business.common.daos.IConfigurationDAO;
import org.navalplanner.business.common.daos.IEntitySequenceDAO;
import org.navalplanner.business.common.entities.Configuration;
import org.navalplanner.business.common.entities.EntityNameEnum;
import org.navalplanner.business.common.entities.EntitySequence;
import org.navalplanner.business.common.exceptions.InstanceNotFoundException;
import org.navalplanner.business.common.exceptions.ValidationException;
import org.navalplanner.web.common.concurrentdetection.OnConcurrentModification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.orm.hibernate3.HibernateOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@OnConcurrentModification(goToPage = "/common/configuration.zul")
public class ConfigurationModel implements IConfigurationModel {

    /**
     * Conversation state
     */
    private Configuration configuration;

    private Map<EntityNameEnum, List<EntitySequence>> entitySequences = new HashMap<EntityNameEnum, List<EntitySequence>>();

    @Autowired
    private IConfigurationDAO configurationDAO;

    @Autowired
    private IBaseCalendarDAO baseCalendarDAO;

    @Autowired
    private IEntitySequenceDAO entitySequenceDAO;

    @Override
    @Transactional(readOnly = true)
    public List<BaseCalendar> getCalendars() {
        return baseCalendarDAO.getBaseCalendars();
    }

    @Override
    public BaseCalendar getDefaultCalendar() {
        if (configuration == null) {
            return null;
        }
        return configuration.getDefaultCalendar();
    }

    @Override
    @Transactional(readOnly = true)
    public void init() {
        this.configuration = getCurrentConfiguration();
        initEntitySequences();
    }

    private void initEntitySequences() {
        this.entitySequences.clear();
        for (EntityNameEnum entityName : EntityNameEnum.values()) {
            entitySequences.put(entityName, new ArrayList<EntitySequence>());
        }
        for (EntitySequence entitySequence : entitySequenceDAO.getAll()) {
            entitySequences.get(entitySequence.getEntityName()).add(
                    entitySequence);
        }
    }

    private Configuration getCurrentConfiguration() {
        Configuration configuration = configurationDAO.getConfiguration();
        if (configuration == null) {
            configuration = Configuration.create();
        }
        forceLoad(configuration);
        return configuration;
    }

    private void forceLoad(Configuration configuration) {
        forceLoad(configuration.getDefaultCalendar());
    }

    private void forceLoad(BaseCalendar calendar) {
        if (calendar != null) {
            calendar.getName();
        }
    }

    @Override
    public void setDefaultCalendar(BaseCalendar calendar) {
        if (configuration != null) {
            configuration.setDefaultCalendar(calendar);
        }
    }

    @Override
    @Transactional
    public void confirm() {

        checkEntitySequences();
        try {
            configurationDAO.save(configuration);
            storeAndRemoveEntitySequences();
        } catch (HibernateOptimisticLockingFailureException e) {
            throw new ConcurrentModificationException(
                    _("Some entity sequence was created during the configuration process, it is impossible to update entity sequence table. Please, try again later"));
        }
    }

    private void checkEntitySequences() {
        // check if exist at least one sequence for each entity
        for (EntityNameEnum entityName : EntityNameEnum.values()) {
            String entity = entityName.getDescription();
            List<EntitySequence> sequences = entitySequences.get(entityName);
            if (sequences.isEmpty()) {
                throw new ValidationException(_(
                        "At least one {0} sequence is needed", entity));
            }

            if (!isAnyActive(sequences)) {
                throw new ValidationException(_(
                        "At least one {0} sequence must be active", entity));
            }
            if (!checkConstraintPrefixNotRepeated(sequences)) {
                throw new ValidationException(_(
                        "The {0} sequence prefixes can not be repeated",
                        entityName.getDescription()));
            }
        }
    }

    private boolean checkConstraintPrefixNotRepeated(
            List<EntitySequence> sequences) {
        Set<String> prefixes = new HashSet<String>();
        for (EntitySequence sequence : sequences) {
            String prefix = sequence.getPrefix();
            if (prefixes.contains(prefix)) {
                return false;
            }
            prefixes.add(prefix);
        }
        return true;
    }

    private boolean isAnyActive(List<EntitySequence> sequences) {
        for (EntitySequence entitySequence : sequences) {
            if (entitySequence.isActive()) {
                return true;
            }
        }
        return false;
    }

    private void storeAndRemoveEntitySequences() {
        Collection<List<EntitySequence>> col_sequences = entitySequences
                .values();
        List<EntitySequence> sequences = new ArrayList<EntitySequence>();
        for (List<EntitySequence> list : col_sequences) {
            sequences.addAll(list);
        }
        removeEntitySequences(sequences);
        storeEntitySequences(sequences);
    }

    public void removeEntitySequences(final List<EntitySequence> sequences) {
        // first one is necessary to remove the deleted sequences.
        List<EntitySequence> toRemove = entitySequenceDAO
                .findEntitySquencesNotIn(sequences);
        for (final EntitySequence entitySequence : toRemove) {
            try {
                entitySequenceDAO.remove(entitySequence);
            } catch (InstanceNotFoundException e) {
                throw new ValidationException(
                        _("Some sequences to remove not existed"));
            } catch (IllegalArgumentException e) {
                throw new ValidationException(e.getMessage());
            }
        }
    }

    public void storeEntitySequences(List<EntitySequence> sequences) {
        // it updates the sequences that are not active first
        List<EntitySequence> toSaveAfter = new ArrayList<EntitySequence>();
        for (EntitySequence entitySequence : sequences) {
            if (entitySequence.isActive()) {
                toSaveAfter.add(entitySequence);
            } else {
                entitySequenceDAO.save(entitySequence);
            }
        }
        for (EntitySequence entitySequence : toSaveAfter) {
            entitySequenceDAO.save(entitySequence);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void cancel() {
        init();
    }

    @Override
    public String getCompanyCode() {
        if (configuration == null) {
            return null;
        }
        return configuration.getCompanyCode();
    }

    @Override
    public void setCompanyCode(String companyCode) {
        if (configuration != null) {
            configuration.setCompanyCode(companyCode);
        }
    }

    @Override
    public Boolean getGenerateCodeForCriterion() {
        if (configuration == null) {
            return null;
        }
        return configuration.getGenerateCodeForCriterion();
    }

    @Override
    public void setGenerateCodeForCriterion(Boolean generateCodeForCriterion) {
        if (configuration != null) {
            configuration.setGenerateCodeForCriterion(generateCodeForCriterion);
        }
    }

    @Override
    public Boolean getGenerateCodeForWorkReportType() {
        if (configuration == null) {
            return null;
        }
        return configuration.getGenerateCodeForWorkReportType();
    }

    @Override
    public void setGenerateCodeForWorkReportType(
            Boolean generateCodeForWorkReportType) {
        if (configuration != null) {
            configuration
                    .setGenerateCodeForWorkReportType(generateCodeForWorkReportType);
        }
    }

    @Override
    public Boolean getGenerateCodeForCalendarExceptionType() {
        if (configuration == null) {
            return null;
        }
        return configuration.getGenerateCodeForCalendarExceptionType();
    }

    @Override
    public void setGenerateCodeForCalendarExceptionType(
            Boolean generateCodeForCalendarExceptionType) {
        if (configuration != null) {
            configuration
                    .setGenerateCodeForCalendarExceptionType(generateCodeForCalendarExceptionType);
        }
    }

    @Override
    public void setGenerateCodeForCostCategory(
            Boolean generateCodeForCostCategory) {
        if (configuration != null) {
            configuration
                    .setGenerateCodeForCostCategory(generateCodeForCostCategory);
        }
    }

    @Override
    public Boolean getGenerateCodeForCostCategory() {
        if (configuration == null) {
            return null;
        }
        return configuration.getGenerateCodeForCostCategory();
    }

    @Override
    public Boolean getGenerateCodeForLabel() {
        if (configuration == null) {
            return null;
        }
        return configuration.getGenerateCodeForLabel();
    }

    @Override
    public void setGenerateCodeForLabel(Boolean generateCodeForLabel) {
        if (configuration != null) {
            configuration.setGenerateCodeForLabel(generateCodeForLabel);
        }
    }

    @Override
    public Boolean getGenerateCodeForWorkReport() {
        if (configuration == null) {
            return null;
        }
        return configuration.getGenerateCodeForWorkReport();
    }

    @Override
    public void setGenerateCodeForWorkReport(Boolean generateCodeForWorkReport) {
        if (configuration != null) {
            configuration.setGenerateCodeForWorkReport(generateCodeForWorkReport);
        }
    }

    @Override
    public Boolean getGenerateCodeForResources() {
        if (configuration == null) {
            return null;
        }
        return configuration.getGenerateCodeForResources();
    }

    @Override
    public void setGenerateCodeForResources(Boolean generateCodeForResources) {
        if (configuration != null) {
            configuration.setGenerateCodeForResources(generateCodeForResources);
        }
    }

    @Override
    public Boolean getGenerateCodeForTypesOfWorkHours() {
        if (configuration == null) {
            return null;
        }
        return configuration.getGenerateCodeForTypesOfWorkHours();
    }

    @Override
    public void setGenerateCodeForTypesOfWorkHours(
            Boolean generateCodeForTypesOfWorkHours) {
        if (configuration != null) {
            configuration.setGenerateCodeForTypesOfWorkHours(
                    generateCodeForTypesOfWorkHours);
        }
    }

    @Override
    public Boolean getGenerateCodeForMaterialCategories() {
        if (configuration == null) {
            return null;
        }
        return configuration.getGenerateCodeForMaterialCategories();
    }

    @Override
    public void setGenerateCodeForMaterialCategories(
            Boolean generateCodeForMaterialCategories) {
        if (configuration != null) {
            configuration.setGenerateCodeForMaterialCategories(
                    generateCodeForMaterialCategories);
        }
    }

    @Override
    public Boolean getGenerateCodeForUnitTypes() {
        if (configuration == null) {
            return null;
        }
        return configuration.getGenerateCodeForUnitTypes();
    }

    @Override
    public void setGenerateCodeForBaseCalendars(
            Boolean generateCodeForBaseCalendars) {
        if (configuration != null) {
            configuration
                    .setGenerateCodeForBaseCalendars(generateCodeForBaseCalendars);
        }
    }

    @Override
    public Boolean getGenerateCodeForBaseCalendars() {
        if (configuration == null) {
            return null;
        }
        return configuration.getGenerateCodeForBaseCalendars();
    }

    @Override
    public void setGenerateCodeForUnitTypes(Boolean generateCodeForUnitTypes) {
        if (configuration != null) {
            configuration.setGenerateCodeForUnitTypes(generateCodeForUnitTypes);
        }
    }

    @Override
    public void setExpandCompanyPlanningViewCharts(
            Boolean expandCompanyPlanningViewCharts) {
        if (configuration != null) {
            configuration
                    .setExpandCompanyPlanningViewCharts(expandCompanyPlanningViewCharts);
        }
    }

    @Override
    public Boolean isMonteCarloMethodTabVisible() {
        if (configuration == null) {
            return null;
        }
        return configuration.isMonteCarloMethodTabVisible();
    }

    @Override
    public void setMonteCarloMethodTabVisible(
            Boolean visible) {
        if (configuration != null) {
            configuration
                    .setMonteCarloMethodTabVisible(visible);
        }
    }

    @Override
    public Boolean isExpandCompanyPlanningViewCharts() {
        if (configuration == null) {
            return null;
        }
        return configuration.isExpandCompanyPlanningViewCharts();
    }

    @Override
    public void setExpandOrderPlanningViewCharts(
            Boolean expandOrderPlanningViewCharts) {
        if (configuration != null) {
            configuration
                .setExpandOrderPlanningViewCharts(expandOrderPlanningViewCharts);
        }
    }

    @Override
    public Boolean isExpandOrderPlanningViewCharts() {
        if (configuration == null) {
            return null;
        }
        return configuration.isExpandOrderPlanningViewCharts();
    }

    @Override
    public void setExpandResourceLoadViewCharts(
            Boolean expandResourceLoadViewCharts) {
        if (configuration != null) {
            configuration
                    .setExpandResourceLoadViewCharts(expandResourceLoadViewCharts);
        }
    }

    @Override
    public Boolean isExpandResourceLoadViewCharts() {
        if (configuration == null) {
            return null;
        }
        return configuration.isExpandResourceLoadViewCharts();
    }

    public List<EntitySequence> getEntitySequences(EntityNameEnum entityName) {
        return entitySequences.get(entityName);
    }

    public void addEntitySequence(EntityNameEnum entityName) {
        List<EntitySequence> sequences = entitySequences.get(entityName);
        EntitySequence entitySequence = EntitySequence.create("", entityName);
        if (sequences.isEmpty()) {
            entitySequence.setActive(true);
        }
        sequences.add(entitySequence);
    }

    public void removeEntitySequence(EntitySequence entitySequence)
            throws IllegalArgumentException {
        entitySequences.get(entitySequence.getEntityName()).remove(
                entitySequence);
    }

}