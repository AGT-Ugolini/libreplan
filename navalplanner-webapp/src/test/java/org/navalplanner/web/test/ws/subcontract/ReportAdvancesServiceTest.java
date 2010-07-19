/*
 * This file is part of NavalPlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                         Desenvolvemento Tecnolóxico de Galicia
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

package org.navalplanner.web.test.ws.subcontract;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.navalplanner.business.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_FILE;
import static org.navalplanner.web.WebappGlobalNames.WEBAPP_SPRING_CONFIG_FILE;
import static org.navalplanner.web.WebappGlobalNames.WEBAPP_SPRING_SECURITY_CONFIG_FILE;
import static org.navalplanner.web.test.WebappGlobalNames.WEBAPP_SPRING_CONFIG_TEST_FILE;
import static org.navalplanner.web.test.WebappGlobalNames.WEBAPP_SPRING_SECURITY_CONFIG_TEST_FILE;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;

import org.hibernate.SessionFactory;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.navalplanner.business.IDataBootstrap;
import org.navalplanner.business.advance.entities.AdvanceMeasurement;
import org.navalplanner.business.advance.entities.DirectAdvanceAssignment;
import org.navalplanner.business.common.IAdHocTransactionService;
import org.navalplanner.business.common.IOnTransaction;
import org.navalplanner.business.common.daos.IConfigurationDAO;
import org.navalplanner.business.externalcompanies.daos.IExternalCompanyDAO;
import org.navalplanner.business.externalcompanies.entities.ExternalCompany;
import org.navalplanner.business.orders.daos.IOrderDAO;
import org.navalplanner.business.orders.entities.Order;
import org.navalplanner.business.orders.entities.OrderElement;
import org.navalplanner.business.orders.entities.OrderLine;
import org.navalplanner.business.scenarios.IScenarioManager;
import org.navalplanner.business.scenarios.entities.OrderVersion;
import org.navalplanner.web.orders.OrderModelTest;
import org.navalplanner.ws.common.api.AdvanceMeasurementDTO;
import org.navalplanner.ws.common.impl.DateConverter;
import org.navalplanner.ws.subcontract.api.IReportAdvancesService;
import org.navalplanner.ws.subcontract.api.OrderElementWithAdvanceMeasurementsDTO;
import org.navalplanner.ws.subcontract.api.OrderElementWithAdvanceMeasurementsListDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests for {@link IReportAdvancesService}.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { BUSINESS_SPRING_CONFIG_FILE,
        WEBAPP_SPRING_CONFIG_FILE, WEBAPP_SPRING_CONFIG_TEST_FILE,
        WEBAPP_SPRING_SECURITY_CONFIG_FILE,
        WEBAPP_SPRING_SECURITY_CONFIG_TEST_FILE })
@Transactional
public class ReportAdvancesServiceTest {

    @Autowired
    private IAdHocTransactionService transactionService;

    @Resource
    private IDataBootstrap defaultAdvanceTypesBootstrapListener;

    @Resource
    private IDataBootstrap configurationBootstrap;

    @Resource
    private IDataBootstrap scenariosBootstrap;

    @Before
    public void loadRequiredaData() {

        IOnTransaction<Void> load = new IOnTransaction<Void>() {

            @Override
            public Void execute() {
                defaultAdvanceTypesBootstrapListener.loadRequiredData();
                configurationBootstrap.loadRequiredData();
		scenariosBootstrap.loadRequiredData();
                return null;
            }
        };

        transactionService.runOnAnotherTransaction(load);
    }

    @Autowired
    private IReportAdvancesService reportAdvancesService;

    @Autowired
    private IOrderDAO orderDAO;

    @Autowired
    private IConfigurationDAO configurationDAO;

    @Autowired
    private IExternalCompanyDAO externalCompanyDAO;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private IScenarioManager scenarioManager;

    private ExternalCompany getSubcontractorExternalCompanySaved(String name,
            String nif) {
        ExternalCompany externalCompany = ExternalCompany.create(name, nif);
        externalCompany.setSubcontractor(true);

        externalCompanyDAO.save(externalCompany);
        externalCompanyDAO.flush();
        sessionFactory.getCurrentSession().evict(externalCompany);

        externalCompany.dontPoseAsTransientObjectAnymore();

        return externalCompany;
    }

    @Test
    public void validAdvancesReport() {
        Order order = givenValidOrderAlreadyStored();
        String orderElementCode = order.getChildren().get(0).getCode();

        Date date = new Date();
        BigDecimal value = new BigDecimal(20);
        OrderElementWithAdvanceMeasurementsListDTO orderElementWithAdvanceMeasurementsListDTO = givenOrderElementWithAdvanceMeasurementsListDTO(
                orderElementCode, date, value);
        reportAdvancesService
                .updateAdvances(orderElementWithAdvanceMeasurementsListDTO);

        Order foundOrder = orderDAO.findExistingEntity(order.getId());
        assertNotNull(foundOrder);
        assertThat(foundOrder.getChildren().size(), equalTo(1));

        OrderElement orderElement = foundOrder.getChildren().get(0);
        assertNotNull(orderElement);

        DirectAdvanceAssignment directAdvanceAssignmentSubcontractor = orderElement
                .getDirectAdvanceAssignmentSubcontractor();
        assertNotNull(directAdvanceAssignmentSubcontractor);
        assertTrue(directAdvanceAssignmentSubcontractor
                .getReportGlobalAdvance());
        assertThat(directAdvanceAssignmentSubcontractor
                .getAdvanceMeasurements().size(), equalTo(1));

        AdvanceMeasurement advanceMeasurement = directAdvanceAssignmentSubcontractor
                .getAdvanceMeasurements().first();
        assertThat(advanceMeasurement.getDate(), equalTo(LocalDate
                .fromDateFields(date)));
        assertThat(advanceMeasurement.getValue(), equalTo(value));
    }

    private OrderElementWithAdvanceMeasurementsListDTO givenOrderElementWithAdvanceMeasurementsListDTO(
            String orderElementCode, Date date, BigDecimal value) {
        OrderElementWithAdvanceMeasurementsDTO orderElementWithAdvanceMeasurementsDTO = new OrderElementWithAdvanceMeasurementsDTO();
        orderElementWithAdvanceMeasurementsDTO.code = orderElementCode;

        Set<AdvanceMeasurementDTO> advanceMeasurementDTOs = new HashSet<AdvanceMeasurementDTO>();
        advanceMeasurementDTOs.add(new AdvanceMeasurementDTO(DateConverter
                .toXMLGregorianCalendar(date), value));
        orderElementWithAdvanceMeasurementsDTO.advanceMeasurements = advanceMeasurementDTOs;

        ExternalCompany externalCompany = getSubcontractorExternalCompanySaved(
                "Company", "company-nif");

        return new OrderElementWithAdvanceMeasurementsListDTO(externalCompany
                .getNif(), Arrays
                .asList(orderElementWithAdvanceMeasurementsDTO));
    }

    private Order givenValidOrderAlreadyStored() {
        Order order = Order.create();
        order.setCode(UUID.randomUUID().toString());
        order.setName("Order name");
        order.setInitDate(new Date());
        order.setCalendar(configurationDAO.getConfiguration()
                .getDefaultCalendar());
        OrderVersion version = OrderModelTest.setupVersionUsing(
                scenarioManager, order);
        order.useSchedulingDataFor(version);
        OrderLine orderLine = OrderLine
                .createOrderLineWithUnfixedPercentage(1000);
        order.add(orderLine);
        orderLine.setCode(UUID.randomUUID().toString());
        orderLine.setName("Order line name");

        orderDAO.save(order);

        return order;
    }

}
