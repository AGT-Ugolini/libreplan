/*
 * This file is part of NavalPlan
 *
 * Copyright (C) 2011 ComtecSF, S.L.
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
package org.navalplanner.web.users.settings;

import org.navalplanner.business.common.exceptions.InstanceNotFoundException;
import org.navalplanner.business.common.exceptions.ValidationException;
import org.navalplanner.business.users.daos.IUserDAO;
import org.navalplanner.business.users.entities.Language;
import org.navalplanner.business.users.entities.Profile;
import org.navalplanner.business.users.entities.User;
import org.navalplanner.business.users.entities.UserRole;
import org.navalplanner.web.common.concurrentdetection.OnConcurrentModification;
import org.navalplanner.web.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Model for UI operations related to user settings
 *
 * @author Cristina Alvarino Perez <cristina.alvarino@comtecsf.es>
 * @author Ignacio Diaz Teijido <ignacio.diaz@comtecsf.es>
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@OnConcurrentModification(goToPage = "/settings/settings.zul")
public class SettingsModel implements ISettingsModel {

    @Autowired
    private IUserDAO userDAO;

    private User user;

    @Override
    public Language getApplicationLanguage() {
        return user.getApplicationLanguage();
    }

    @Override
    public void setApplicationLanguage(Language applicationLanguage) {
        this.user.setApplicationLanguage(applicationLanguage);
    }

    private User findByLoginUser(String login) {
        try {
            return user = userDAO.findByLoginName(login);
        } catch (InstanceNotFoundException e) {
               throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void initEditLoggedUser() {
        User user = findByLoginUser(SecurityUtils.getSessionUserLoginName());
        this.user = getFromDB(user);
    }

    @Transactional(readOnly = true)
    private User getFromDB(User user) {
        return getFromDB(user.getId());
    }

    private User getFromDB(Long id) {
        try {
            User result = userDAO.find(id);
            forceLoadEntities(result);
            return result;
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void forceLoadEntities(User user) {
        user.getLoginName();
        for (UserRole each : user.getRoles()) {
            each.name();
        }
        for (Profile each : user.getProfiles()) {
            each.getProfileName();
        }
    }

    @Override
    @Transactional
    public void confirmSave() throws ValidationException {
        userDAO.save(user);
    }


    @Override
    public boolean isExpandCompanyPlanningViewCharts() {
        return user.isExpandCompanyPlanningViewCharts();
    }

    @Override
    public void setExpandOrderPlanningViewCharts(
            boolean expandOrderPlanningViewCharts) {
        if (user != null) {
            user.setExpandOrderPlanningViewCharts(expandOrderPlanningViewCharts);
        }
    }

    @Override
    public boolean isExpandOrderPlanningViewCharts() {
        return user.isExpandOrderPlanningViewCharts();
    }

    @Override
    public void setExpandResourceLoadViewCharts(
            boolean expandResourceLoadViewCharts) {
        if (user != null) {
            user.setExpandResourceLoadViewCharts(expandResourceLoadViewCharts);
        }
    }

    @Override
    public boolean isExpandResourceLoadViewCharts() {
        return user.isExpandResourceLoadViewCharts();
    }

    @Override
    public void setExpandCompanyPlanningViewCharts(
            boolean expandCompanyPlanningViewCharts) {
        if (user != null) {
            user.setExpandCompanyPlanningViewCharts(expandCompanyPlanningViewCharts);
        }
    }

    @Override
    public String getFirstName() {
        return user.getFirstName();
    }

    @Override
    public void setFirstName(String firstName) {
        if (user != null) {
            user.setFirstName(firstName);
        }
    }

    @Override
    public String getLastName() {
        return user.getLastName();
    }

    @Override
    public void setLastName(String lastName) {
        if (user != null) {
            user.setLastName(lastName);
        }
    }

}
