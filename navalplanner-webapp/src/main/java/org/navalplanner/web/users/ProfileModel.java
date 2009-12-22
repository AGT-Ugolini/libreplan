/*
 * This file is part of ###PROJECT_NAME###
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

package org.navalplanner.web.users;

import java.util.ArrayList;
import java.util.List;

import org.navalplanner.business.common.exceptions.InstanceNotFoundException;
import org.navalplanner.business.common.exceptions.ValidationException;
import org.navalplanner.business.users.daos.IProfileDAO;
import org.navalplanner.business.users.entities.Profile;
import org.navalplanner.business.users.entities.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Model for UI operations related to {@link Profile}
 *
 * @author Jacobo Aragunde Perez <jaragunde@igalia.com>
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ProfileModel implements IProfileModel {

    private Profile profile;

    @Autowired
    private IProfileDAO profileDAO;

    @Override
    @Transactional
    public void confirmSave() throws ValidationException {
        profileDAO.save(profile);
    }

    @Override
    @Transactional(readOnly=true)
    public List<Profile> getProfiles() {
        return profileDAO.list(Profile.class);
    }

    @Override
    public Profile getProfile() {
        return profile;
    }

    @Override
    @Transactional(readOnly = true)
    public void initCreate() {
        profile = Profile.create();
    }

    @Override
    @Transactional(readOnly = true)
    public void initEdit(Profile profile) {
        try {
            this.profile = profileDAO.find(profile.getId());
            forceLoadEntities(this.profile);
        }
        catch (InstanceNotFoundException e) {
            initCreate();
        }
    }

    @Transactional(readOnly = true)
    private void forceLoadEntities(Profile profile) {
        profile.getProfileName();
        for(UserRole role : profile.getRoles()) {
            role.name();
        }
    }

    @Override
    public List<UserRole> getAllRoles() {
        List<UserRole> list = new ArrayList<UserRole>();
        for(UserRole role : UserRole.values()) {
            list.add(role);
        }
        return list;
    }

    @Override
    public void addRole(UserRole role) {
        profile.addRole(role);
    }

    @Override
    public void removeRole(UserRole role) {
        profile.removeRole(role);
    }

    @Override
    public boolean roleBelongs(UserRole role) {
        if (profile == null) {
            return false;
        }
        return profile.getRoles().contains(role);
    }

    @Override
    @Transactional
    public void confirmRemove(Profile profile)
        throws InstanceNotFoundException {
        profileDAO.remove(profile.getId());
    }

    @Override
    public List<UserRole> getRoles() {
        List<UserRole> list = new ArrayList<UserRole>();
        if (profile != null) {
            list.addAll(profile.getRoles());
        }
        return list;
    }

}
