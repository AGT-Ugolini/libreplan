/*
 * This file is part of NavalPlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                         Desenvolvemento Tecnolóxico de Galicia
 * Copyright (C) 2010-2011 Igalia, S.L.
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

package org.navalplanner.business.test.resources.daos;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.navalplanner.business.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_FILE;
import static org.navalplanner.business.test.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_TEST_FILE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.SessionFactory;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.navalplanner.business.calendars.entities.ResourceCalendar;
import org.navalplanner.business.common.IAdHocTransactionService;
import org.navalplanner.business.common.IOnTransaction;
import org.navalplanner.business.common.exceptions.InstanceNotFoundException;
import org.navalplanner.business.resources.daos.ICriterionDAO;
import org.navalplanner.business.resources.daos.ICriterionTypeDAO;
import org.navalplanner.business.resources.daos.IResourceDAO;
import org.navalplanner.business.resources.daos.IResourcesSearcher;
import org.navalplanner.business.resources.entities.Criterion;
import org.navalplanner.business.resources.entities.CriterionSatisfaction;
import org.navalplanner.business.resources.entities.CriterionType;
import org.navalplanner.business.resources.entities.Interval;
import org.navalplanner.business.resources.entities.Resource;
import org.navalplanner.business.resources.entities.Worker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test cases for {@link ResourceDAOTest}
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { BUSINESS_SPRING_CONFIG_FILE,
        BUSINESS_SPRING_CONFIG_TEST_FILE })
@Transactional
public class ResourceDAOTest {

    @Autowired
    private IResourceDAO resourceDAO;

    @Autowired
    private ICriterionDAO criterionDAO;

    @Autowired
    private IResourcesSearcher resourcesSearcher;

    @Autowired
    private IAdHocTransactionService transactionService;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private ICriterionTypeDAO criterionTypeDAO;

    @Test
    public void saveResourceWithCalendar() throws InstanceNotFoundException {
        Resource resource = givenValidWorker();
        ResourceCalendar resourceCalendar = givenValidResourceCalendar();

        resource.setCalendar(resourceCalendar);

        resourceDAO.save(resource);
        resourceDAO.flush();
        sessionFactory.getCurrentSession().evict(resource);

        Resource foundResource = resourceDAO.find(resource.getId());
        assertNotSame(resource, foundResource);
        assertNotNull(foundResource.getCalendar());
        assertThat(foundResource.getCalendar().getId(),
                equalTo(resourceCalendar.getId()));
    }

    private ResourceCalendar givenValidResourceCalendar() {
        ResourceCalendar resourceCalendar = ResourceCalendar.create();
        resourceCalendar.setName("Calendar");
        return resourceCalendar;
    }

    public static Worker givenValidWorker() {
        Worker worker = Worker.create();
        worker.setFirstName("First name");
        worker.setSurname("Surname");
        worker.setNif("NIF" + UUID.randomUUID().toString());
        return worker;
    }

    @Test
    public void testResourceIsRelatedWithAllCriterions() {
        Collection<Criterion> criterions = createCriterions();
        createAndSaveResourceSatisfyingAllCriterions(criterions);
        List<Resource> result = resourcesSearcher.searchBoth()
                .byCriteria(criterions).execute();
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @NotTransactional
    public void theHierarchyOfCriterionsIsConsidered() {
        final Criterion[] parentCriteron = { null };
        Resource worker = transactionService
                .runOnTransaction(new IOnTransaction<Resource>() {
                    @Override
                    public Resource execute() {
                        Worker result = givenValidWorker();
                        CriterionType type = createCriterionType("testType");
                        Criterion parent = createCriterion("parent", type);
                        parentCriteron[0] = parent;
                        Criterion child = createCriterion("child", type);
                        child.setParent(parent);
                        addSatisfactionsOn(result,
                                Interval.from(new LocalDate(1970, 1, 1)), child);
                        return result;
                    }
        });
        final Criterion parent = transactionService
                .runOnReadOnlyTransaction(new IOnTransaction<Criterion>() {

                    @Override
                    public Criterion execute() {
                        return criterionDAO
                                .findExistingEntity(parentCriteron[0].getId());
                    }
                });
        List<Resource> resources = transactionService
                .runOnReadOnlyTransaction(new IOnTransaction<List<Resource>>() {

                    @Override
                    public List<Resource> execute() {
                        return resourcesSearcher.searchBoth()
                                .byCriteria(Collections.singleton(parent))
                                .execute();
                    }
                });
        assertThat(resources.size(), equalTo(1));
        Resource resource = resources.get(0);
        assertThat(resource.getId(), equalTo(worker.getId()));
    }

    private Collection<Criterion> createCriterions() {
        List<Criterion> result = new ArrayList<Criterion>();
        CriterionType type = createCriterionType("criterionTypeTest");
        result.add(createCriterion("criterion1", type));
        result.add(createCriterion("criterion2", type));
        return result;
    }

    private CriterionType createCriterionType(String name) {
        CriterionType result = CriterionType.create(name, "");
        criterionTypeDAO.save(result);
        return result;
    }

    private Criterion createCriterion(String name) {
        return createCriterion(name, createCriterionType(UUID.randomUUID().toString()));
    }

    private Criterion createCriterion(String name, CriterionType type) {
        Criterion result = Criterion.create(name, type);
        criterionDAO.save(result);
        return result;
    }

    private Worker createAndSaveResourceSatisfyingAllCriterions(final Collection<Criterion> criterions) {
        Worker result = givenValidWorker();
        Interval interval = Interval.range(new LocalDate(1970, 1, 1), null);
        addSatisfactionsOn(result, interval,
                criterions.toArray(new Criterion[] {}));
        return result;
    }

    private void addSatisfactionsOn(Worker worker, Interval interval,
            final Criterion... criterions) {
        Set<CriterionSatisfaction> satisfactions = new HashSet<CriterionSatisfaction>();
        for (Criterion each : criterions) {
            satisfactions.add(CriterionSatisfaction.create(each, worker,
                    interval));
        }
        worker.addSatisfactions(satisfactions);
        resourceDAO.save(worker);
    }

    @Test
    public void testResourceIsNotRelatedWithAllCriterions() {
        Collection<Criterion> criterions = createCriterions();
        createAndSaveResourceSatisfyingAllCriterions(criterions);

        // Modify criterions collection
        criterions.add(createCriterion("criterion3"));

        List<Resource> result = resourcesSearcher.searchBoth()
                .byCriteria(criterions).execute();
        assertNotNull(result);
        assertThat(result.size(), not(equalTo(1)));
    }

}
