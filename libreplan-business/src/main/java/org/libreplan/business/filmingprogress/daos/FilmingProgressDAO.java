/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2012 WirelessGalicia, S.L.
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

package org.libreplan.business.filmingprogress.daos;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.libreplan.business.common.daos.GenericDAOHibernate;
import org.libreplan.business.filmingprogress.entities.FilmingProgress;
import org.libreplan.business.orders.entities.Order;

/**
 * Dao for {@link FilmingProgress}
 *
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */
public class FilmingProgressDAO extends GenericDAOHibernate<FilmingProgress, Long> implements
        IFilmingProgressDAO {

    @Override
    public FilmingProgress findBy(Order order) {
        Criteria c = getSession().createCriteria(FilmingProgress.class).createCriteria("order");
        c.add(Restrictions.idEq(order.getId()));
        return (FilmingProgress) c.uniqueResult();
    }

}
