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
package org.navalplanner.web.scenarios;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createNiceMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.navalplanner.business.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_FILE;
import static org.navalplanner.web.WebappGlobalNames.WEBAPP_SPRING_CONFIG_FILE;
import static org.navalplanner.web.WebappGlobalNames.WEBAPP_SPRING_SECURITY_CONFIG_FILE;
import static org.navalplanner.web.test.WebappGlobalNames.WEBAPP_SPRING_CONFIG_TEST_FILE;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.navalplanner.business.common.Registry;
import org.navalplanner.business.scenarios.IScenarioManager;
import org.navalplanner.business.scenarios.bootstrap.IScenariosBootstrap;
import org.navalplanner.business.scenarios.bootstrap.PredefinedScenarios;
import org.navalplanner.business.scenarios.entities.Scenario;
import org.navalplanner.web.users.services.CustomUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { BUSINESS_SPRING_CONFIG_FILE,
        WEBAPP_SPRING_CONFIG_FILE, WEBAPP_SPRING_CONFIG_TEST_FILE,
        WEBAPP_SPRING_SECURITY_CONFIG_FILE })
@Transactional
public class CurrentUserScenarioAwareManagerTest {

    @Autowired
    private IScenariosBootstrap scenariosBootstrap;

    @Autowired
    private IScenarioManager scenarioManager;

    @Before
    public void loadRequiredData() {
        scenariosBootstrap.loadRequiredData();
    }

    @Test
    public void ifNoUserAuthenticatedMainScenarioIsReturned() {
        Scenario current = scenarioManager.getCurrent();
        assertEquals(PredefinedScenarios.MASTER.getName(), current.getName());
        assertEquals(scenariosBootstrap.getMain(), current);
    }

    @Test
    public void retrievesTheScenarioAssociatedWithTheAuthentication() {
        Scenario customScenario = mockScenario();
        givenUserAuthenticatedWith(customScenario);
        Scenario current = scenarioManager.getCurrent();
        assertEquals(customScenario, current);
    }

    private Scenario mockScenario() {
        Scenario result = createNiceMock(Scenario.class);
        replay(result);
        return result;
    }

    private Scenario givenUserAuthenticatedWith(Scenario customScenario) {
        SecurityContext context = SecurityContextHolder.getContext();
        CustomUser user = createUserWithScenario(customScenario);
        Authentication authentication = stubAuthenticationWithPrincipal(user);
        context.setAuthentication(authentication);
        return customScenario;
    }


    private CustomUser createUserWithScenario(Scenario customScenario) {
        CustomUser result = createNiceMock(CustomUser.class);
        expect(result.getScenario()).andReturn(customScenario).anyTimes();
        replay(result);
        return result;
    }

    private Authentication stubAuthenticationWithPrincipal(CustomUser customUser) {
        Authentication result = createNiceMock(Authentication.class);
        expect(result.getPrincipal()).andReturn(customUser).anyTimes();
        replay(result);
        return result;
    }

    @Test
    public void registryHoldsCurrentUserScenarioAwareTest() {
        IScenarioManager fromRegistry = Registry.getScenarioManager();
        assertEquals(scenarioManager, fromRegistry);
    }

}
