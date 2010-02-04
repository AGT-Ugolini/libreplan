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

package org.navalplanner.business.users.daos;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.navalplanner.business.common.daos.GenericDAOHibernate;
import org.navalplanner.business.common.exceptions.InstanceNotFoundException;
import org.navalplanner.business.users.entities.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hibernate DAO for the <code>Profile</code> entity.
 *
 * @author Jacobo Aragunde Perez <jaragunde@igalia.com>
 */
@Repository
public class ProfileDAO extends GenericDAOHibernate<Profile, Long> implements
        IProfileDAO {

    @Override
    public boolean existsByProfileName(String profileName) {
        try {
            findByProfileName(profileName);
            return true;
        }
        catch (InstanceNotFoundException e) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public boolean existsByProfileNameAnotherTransaction(String profileName) {
        return existsByProfileName(profileName);
    }

    @Override
    public Profile findByProfileName(String profileName)
        throws InstanceNotFoundException{

        Criteria c = getSession().createCriteria(Profile.class);
        c.add(Restrictions.eq("profileName", profileName).ignoreCase());
        Profile profile = (Profile) c.uniqueResult();

        if (profile == null) {
            throw new InstanceNotFoundException(profileName,
                Profile.class.getName());
        } else {
            return profile;
        }

    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Profile findByProfileNameAnotherTransaction(String profileName)
            throws InstanceNotFoundException {
        return findByProfileName(profileName);
    }

}
