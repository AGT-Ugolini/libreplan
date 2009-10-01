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

package org.navalplanner.business.test.resources.daos;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.navalplanner.business.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_FILE;
import static org.navalplanner.business.test.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_TEST_FILE;

import java.util.Calendar;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.navalplanner.business.common.exceptions.InstanceNotFoundException;
import org.navalplanner.business.resources.daos.ICriterionDAO;
import org.navalplanner.business.resources.daos.ICriterionSatisfactionDAO;
import org.navalplanner.business.resources.daos.ICriterionTypeDAO;
import org.navalplanner.business.resources.daos.IWorkerDAO;
import org.navalplanner.business.resources.entities.Criterion;
import org.navalplanner.business.resources.entities.CriterionSatisfaction;
import org.navalplanner.business.resources.entities.CriterionType;
import org.navalplanner.business.resources.entities.Worker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Description goes here. <br />
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { BUSINESS_SPRING_CONFIG_FILE,
        BUSINESS_SPRING_CONFIG_TEST_FILE })
@Transactional
public class CriterionSatisfactionDAOTest {

    @Autowired
    private ICriterionSatisfactionDAO satisfactionDAO;

    @Autowired
    private ICriterionDAO criterionDAO;

    @Autowired
    private ICriterionTypeDAO criterionTypeDAO;

    @Autowired
    private IWorkerDAO workerDAO;

    @Test
    public void testSaveCriterions() throws Exception {
        CriterionSatisfaction criterionSatisfaction = createValidCriterionSatisfaction(2007);
        satisfactionDAO.save(criterionSatisfaction);
        assertNotNull(criterionSatisfaction.getId());
        assertTrue(satisfactionDAO.exists(criterionSatisfaction.getId()));
    }

    private CriterionSatisfaction createValidCriterionSatisfaction(int year) {
        Criterion criterion = CriterionDAOTest.createValidCriterion();
        saveCriterionType(criterion);
        criterionDAO.save(criterion);
        Worker worker = Worker.create("firstname", "surname", "nif");
        workerDAO.save(worker);
        CriterionSatisfaction criterionSatisfaction = CriterionSatisfaction.create(year(year), criterion, worker);
        return criterionSatisfaction;
    }

    private void saveCriterionType(Criterion criterion) {
        CriterionType criterionType = criterion.getType();
        if (criterionTypeDAO.existsByName(criterionType)) {
            try {
                criterionType = criterionTypeDAO.findUniqueByName(criterionType);
            } catch (InstanceNotFoundException ex) {
            }
        } else {
            criterionTypeDAO.save(criterionType);
        }
        criterion.setType(criterionType);
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void testNotSaveWithTransientCriterionAndWorker() {
        Criterion criterion = CriterionDAOTest.createValidCriterion();
        saveCriterionType(criterion);
        Worker worker = Worker.create("firstname", "surname", "nif");
        CriterionSatisfaction criterionSatisfaction = CriterionSatisfaction.create(year(2007), criterion, worker);
        satisfactionDAO.save(criterionSatisfaction);
    }

    public static Date year(int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        return calendar.getTime();
    }

    @Test
    public void testRemove() throws InstanceNotFoundException {
        CriterionSatisfaction satisfaction = createValidCriterionSatisfaction(2008);
        satisfactionDAO.save(satisfaction);
        assertTrue(satisfactionDAO.exists(satisfaction.getId()));
        satisfactionDAO.remove(satisfaction.getId());
        assertFalse(satisfactionDAO.exists(satisfaction.getId()));
    }

    @Test
    public void testList() {
        int previous = satisfactionDAO.list(CriterionSatisfaction.class).size();
        CriterionSatisfaction satisfaction1 = createValidCriterionSatisfaction(2007);
        CriterionSatisfaction satisfaction2 = createValidCriterionSatisfaction(2008);
        satisfactionDAO.save(satisfaction1);
        satisfactionDAO.save(satisfaction2);
        assertEquals(previous + 2, satisfactionDAO.list(
                CriterionSatisfaction.class).size());
    }
}
