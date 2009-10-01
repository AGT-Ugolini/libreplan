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

package org.navalplanner.business.common.daos;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StaleObjectStateException;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.navalplanner.business.common.exceptions.InstanceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An implementation of <code>IGenericDao</code> based on Hibernate's native
 * API. Concrete DAOs must extend directly from this class. This constraint is
 * imposed by the constructor of this class that must infer the type of the
 * entity from the declaration of the concrete DAO.
 * <p/>
 * This class autowires a <code>SessionFactory</code> bean and allows to
 * implement DAOs with Hibernate's native API. Subclasses access Hibernate's
 * <code>Session</code> by calling on <code>getSession()</code> method.
 * Operations must be implemented by catching <code>HibernateException</code>
 * and rethrowing it by using <code>convertHibernateAccessException()</code>
 * method. See source code of this class for an example.
 * @author Fernando Bellas Permuy <fbellas@udc.es>
 * @param <E>
 *            Entity class
 * @param <PK>
 *            Primary key class
 */
public class GenericDAOHibernate<E, PK extends Serializable> implements
        IGenericDAO<E, PK> {

    private Class<E> entityClass;

    @Autowired
    private SessionFactory sessionFactory;

    @SuppressWarnings("unchecked")
    public GenericDAOHibernate() {
        this.entityClass = (Class<E>) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0];
    }

    public GenericDAOHibernate(Class<E> entityClass) {
        Validate.notNull(entityClass);
        this.entityClass = entityClass;
    }

    protected Session getSession() {
        return sessionFactory.getCurrentSession();
    }

    public void save(E entity) {
        getSession().saveOrUpdate(entity);
    }

    public void reattachUnmodifiedEntity(E entity) {

        getSession().lock(entity, LockMode.NONE);

    }

    public E merge(E entity) {

        return entityClass.cast(getSession().merge(entity));

    }

    public void checkVersion(E entity) {

        /* Get id and version from entity. */
        Serializable id;
        Long versionValueInMemory;

        try {

            Method getIdMethod = entityClass.getMethod("getId");
            id = (Serializable) getIdMethod.invoke(entity);

            if (id == null) {
                return;
            }

            Method getVersionMethod = entityClass.getMethod("getVersion");
            versionValueInMemory = (Long) getVersionMethod.invoke(entity);

            if (versionValueInMemory == null) {
                return;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        /* Check version. */
        Long versionValueInDB = (Long) getSession().createCriteria(entityClass)
                .add(Restrictions.idEq(id)).setProjection(
                        Projections.property("version")).uniqueResult();

        if (versionValueInDB == null) {
            return;
        }

        if (!versionValueInMemory.equals(versionValueInDB)) {
            throw new StaleObjectStateException(entityClass.getName(), id);
        }

    }

    public void lock(E entity) {

        getSession().lock(entity, LockMode.UPGRADE);

    }

    public void associateToSession(E entity) {
        getSession().lock(entity, LockMode.NONE);
    }

    @SuppressWarnings("unchecked")
    public E find(PK id) throws InstanceNotFoundException {

        E entity = (E) getSession().get(entityClass, id);

        if (entity == null) {
            throw new InstanceNotFoundException(id, entityClass.getName());
        }

        return entity;
    }

    public E findExistingEntity(PK id) {

        try {
            return find(id);
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    public boolean exists(final PK id) {

        return getSession().createCriteria(entityClass).add(
                Restrictions.idEq(id)).setProjection(Projections.id())
                .uniqueResult() != null;

    }

    public void remove(PK id) throws InstanceNotFoundException {
        getSession().delete(find(id));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends E> List<T> list(Class<T> klass) {
        return getSession().createCriteria(klass).list();
    }

    @Override
    public void flush() {
        getSession().flush();
    }

}
