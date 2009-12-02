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

package org.navalplanner.business.test.costcategories.daos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.navalplanner.business.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_FILE;
import static org.navalplanner.business.test.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_TEST_FILE;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.navalplanner.business.common.exceptions.InstanceNotFoundException;
import org.navalplanner.business.costcategories.daos.IHourCostDAO;
import org.navalplanner.business.costcategories.entities.HourCost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { BUSINESS_SPRING_CONFIG_FILE,
        BUSINESS_SPRING_CONFIG_TEST_FILE })
/**
 * Test for {@HourCostDAO}
 *
 * @author Jacobo Aragunde Perez <jaragunde@igalia.com>
 *
 */
@Transactional
public class HourCostDAOTest {

    @Autowired
    IHourCostDAO hourCostDAO;

    @Test
    public void testInSpringContainer() {
        assertNotNull(hourCostDAO);
    }

    private HourCost createValidHourCost() {
        HourCost hourCost = HourCost.create(BigDecimal.ONE, new Date());
        return hourCost;
    }

    @Test
    public void testSaveHourCost() {
        HourCost hourCost = createValidHourCost();
        hourCostDAO.save(hourCost);
        assertTrue(hourCost.getId() != null);
    }

    @Test
    public void testRemoveHourCost() throws InstanceNotFoundException {
        HourCost hourCost = createValidHourCost();
        hourCostDAO.save(hourCost);
        hourCostDAO.remove(hourCost.getId());
        assertFalse(hourCostDAO.exists(hourCost.getId()));
    }

    @Test
    public void testListHourCost() {
        int previous = hourCostDAO.list(HourCost.class).size();
        HourCost hourCost = createValidHourCost();
        hourCostDAO.save(hourCost);
        List<HourCost> list = hourCostDAO.list(HourCost.class);
        assertEquals(previous + 1, list.size());
    }
}
