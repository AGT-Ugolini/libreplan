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

import static org.navalplanner.web.I18nHelper._;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.hibernate.validator.InvalidValue;
import org.navalplanner.business.calendars.entities.BaseCalendar;
import org.navalplanner.business.common.exceptions.InstanceNotFoundException;
import org.navalplanner.business.common.exceptions.ValidationException;
import org.navalplanner.business.orders.daos.IOrderDAO;
import org.navalplanner.business.orders.daos.IOrderElementDAO;
import org.navalplanner.business.orders.entities.Order;
import org.navalplanner.business.scenarios.IScenarioManager;
import org.navalplanner.business.scenarios.bootstrap.PredefinedScenarios;
import org.navalplanner.business.scenarios.daos.IOrderVersionDAO;
import org.navalplanner.business.scenarios.daos.IScenarioDAO;
import org.navalplanner.business.scenarios.entities.OrderVersion;
import org.navalplanner.business.scenarios.entities.Scenario;
import org.navalplanner.business.users.daos.IUserDAO;
import org.navalplanner.business.users.entities.User;
import org.navalplanner.web.common.concurrentdetection.OnConcurrentModification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Model for UI operations related to {@link Scenario}.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Qualifier("main")
@OnConcurrentModification(goToPage = "/scenarios/scenarios.zul")
public class ScenarioModel implements IScenarioModel {

    /**
     * Conversation state
     */
    private Scenario scenario;

    @Autowired
    private IScenarioDAO scenarioDAO;

    @Autowired
    private IOrderDAO orderDAO;

    @Autowired
    private IUserDAO userDAO;

    @Autowired
    private IOrderElementDAO orderElementDAO;

    @Autowired
    private IOrderVersionDAO orderVersionDAO;

    @Autowired
    private IScenarioManager scenarioManager;

    /*
     * Non conversational steps
     */
    @Override
    @Transactional(readOnly = true)
    public List<Scenario> getScenarios() {
        return scenarioDAO.getAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Scenario> getDerivedScenarios(Scenario scenario) {
        return scenarioDAO.getDerivedScenarios(scenario);
    }

    @Override
    @Transactional
    public void remove(Scenario scenario) {
        remove(scenario, true);
    }

    @Override
    @Transactional
    public void remove(Scenario scenario, boolean forceLoad) {
        if (forceLoad) {
            forceLoad(scenario);
        }

        boolean isMainScenario = PredefinedScenarios.MASTER.getScenario().getId().equals(scenario.getId());
        if (isMainScenario) {
            throw new IllegalArgumentException(
                    _("You can not remove the default scenario called \"{0}\"", PredefinedScenarios.MASTER.getName()));
        }

        Scenario currentScenario = scenarioManager.getCurrent();
        boolean isCurrentScenario = currentScenario.getId().equals(
                scenario.getId());
        if (isCurrentScenario) {
            throw new IllegalArgumentException(
                    _("You can not remove the current scenario"));
        }

        List<Scenario> derivedScenarios = getDerivedScenarios(scenario);
        if (!derivedScenarios.isEmpty()) {
            throw new IllegalArgumentException(
                    _("You can not remove a scenario with derived scenarios"));
        }

        List<User> users = userDAO.findByLastConnectedScenario(scenario);
        for (User user : users) {
            user.setLastConnectedScenario(PredefinedScenarios.MASTER
                    .getScenario());
            userDAO.save(user);
        }

        for (Order order : scenario.getOrders().keySet()) {
            if (order.getScenarios().size() == 1) {
                if (!orderElementDAO
                        .isAlreadyInUseThisOrAnyOfItsChildren(order)) {
                    try {
                        orderDAO.remove(order.getId());
                    } catch (InstanceNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            } else {
                order.disassociateFrom(scenario);
                orderDAO.save(order);
            }
        }

        for (OrderVersion orderVersion : orderVersionDAO
                .getOrderVersionByOwnerScenario(scenario)) {
            try {
                orderVersionDAO.remove(orderVersion.getId());
            } catch (InstanceNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            scenarioDAO.remove(scenario.getId());
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * Initial conversation steps
     */
    @Override
    @Transactional(readOnly = true)
    public void initEdit(Scenario scenario) {
        Validate.notNull(scenario);
        forceLoad(scenario);

        this.scenario = scenario;
    }

    private void forceLoad(Scenario scenario) {
        scenarioDAO.reattach(scenario);
        Set<Order> orders = scenario.getOrders().keySet();
        for (Order order : orders) {
            orderDAO.reattach(order);
            order.getName();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void initCreateDerived(Scenario scenario) {
        Validate.notNull(scenario);
        forceLoad(scenario);

        this.scenario = scenario.newDerivedScenario();
    }

    /*
     * Intermediate conversation steps
     */
    @Override
    public Scenario getScenario() {
        return scenario;
    }

    /*
     * Final conversation steps
     */

    @Override
    @Transactional
    public void confirmSave() throws ValidationException {
        if (scenarioDAO.thereIsOtherWithSameName(scenario)) {
            InvalidValue[] invalidValues = { new InvalidValue(_(
                    "{0} already exists", scenario.getName()),
                    BaseCalendar.class, "name", scenario.getName(), scenario) };
            throw new ValidationException(invalidValues,
                    _("Could not save the scenario"));
        }

        scenarioDAO.save(scenario);
    }

    @Override
    public void cancel() {
        resetState();
    }

    private void resetState() {
        scenario = null;
    }

}
