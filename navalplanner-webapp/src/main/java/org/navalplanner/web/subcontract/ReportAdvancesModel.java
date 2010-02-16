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
package org.navalplanner.web.subcontract;

import static org.navalplanner.web.I18nHelper._;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.navalplanner.business.advance.bootstrap.PredefinedAdvancedTypes;
import org.navalplanner.business.advance.entities.AdvanceMeasurement;
import org.navalplanner.business.advance.entities.AdvanceType;
import org.navalplanner.business.advance.entities.DirectAdvanceAssignment;
import org.navalplanner.business.common.daos.IConfigurationDAO;
import org.navalplanner.business.externalcompanies.entities.ExternalCompany;
import org.navalplanner.business.orders.daos.IOrderElementDAO;
import org.navalplanner.business.orders.entities.Order;
import org.navalplanner.business.orders.entities.OrderElement;
import org.navalplanner.web.subcontract.exceptions.ConnectionProblemsException;
import org.navalplanner.web.subcontract.exceptions.UnrecoverableErrorServiceException;
import org.navalplanner.ws.cert.NaiveTrustProvider;
import org.navalplanner.ws.common.api.AdvanceMeasurementDTO;
import org.navalplanner.ws.common.api.ConstraintViolationDTO;
import org.navalplanner.ws.common.api.InstanceConstraintViolationsDTO;
import org.navalplanner.ws.common.api.InstanceConstraintViolationsListDTO;
import org.navalplanner.ws.common.impl.OrderElementConverter;
import org.navalplanner.ws.common.impl.Util;
import org.navalplanner.ws.subcontract.api.OrderElementWithAdvanceMeasurementsDTO;
import org.navalplanner.ws.subcontract.api.OrderElementWithAdvanceMeasurementsListDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Model for operations related with report advances.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ReportAdvancesModel implements IReportAdvancesModel {

    private static Log LOG = LogFactory.getLog(ReportAdvancesModel.class);

    @Autowired
    private IOrderElementDAO orderElementDAO;

    @Autowired
    private IConfigurationDAO configurationDAO;

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersWithExternalCodeInAnyOrderElement() {
        List<OrderElement> orderElements = orderElementDAO
                .findOrderElementsWithExternalCode();

        Map<Long, Order> ordersMap = new HashMap<Long, Order>();
        for (OrderElement orderElement : orderElements) {
            Order order = orderElementDAO
                    .loadOrderAvoidingProxyFor(orderElement);
            if (ordersMap.get(order.getId()) == null) {
                ordersMap.put(order.getId(), order);
                forceLoadHoursGroups(order);
                forceLoadAdvanceAssignments(order);
            }
        }

        return new ArrayList<Order>(ordersMap.values());
    }

    private void forceLoadHoursGroups(OrderElement orderElement) {
        orderElement.getHoursGroups().size();
        for (OrderElement child : orderElement.getChildren()) {
            forceLoadHoursGroups(child);
        }
    }

    private void forceLoadAdvanceAssignments(Order order) {
        AdvanceType advanceType = PredefinedAdvancedTypes.SUBCONTRACTOR
                .getType();
        for (DirectAdvanceAssignment directAdvanceAssignment : order
                .getAllDirectAdvanceAssignments(advanceType)) {
            directAdvanceAssignment.getAdvanceMeasurements().size();
        }
        order.getAllIndirectAdvanceAssignments(advanceType).size();
    }

    @Override
    @Transactional(readOnly = true)
    public AdvanceMeasurement getLastAdvanceMeasurement(
            Set<DirectAdvanceAssignment> allDirectAdvanceAssignments) {
        if (allDirectAdvanceAssignments.isEmpty()) {
            return null;
        }

        Iterator<DirectAdvanceAssignment> iterator = allDirectAdvanceAssignments
                .iterator();
        DirectAdvanceAssignment advanceAssignment = iterator.next();

        AdvanceMeasurement lastAdvanceMeasurement = advanceAssignment
                .getLastAdvanceMeasurement();
        while (iterator.hasNext()) {
            advanceAssignment = iterator.next();
            AdvanceMeasurement advanceMeasurement = advanceAssignment
                    .getLastAdvanceMeasurement();
            if (advanceMeasurement.getDate().compareTo(
                    lastAdvanceMeasurement.getDate()) > 0) {
                lastAdvanceMeasurement = advanceMeasurement;
            }
        }

        return lastAdvanceMeasurement;
    }

    @Override
    @Transactional(readOnly = true)
    public AdvanceMeasurement getLastAdvanceMeasurementReported(
            Set<DirectAdvanceAssignment> allDirectAdvanceAssignments) {
        if (allDirectAdvanceAssignments.isEmpty()) {
            return null;
        }

        AdvanceMeasurement lastAdvanceMeasurementReported = null;

        for (DirectAdvanceAssignment advanceAssignment : allDirectAdvanceAssignments) {
            for (AdvanceMeasurement advanceMeasurement : advanceAssignment.getAdvanceMeasurements()) {
                if (advanceMeasurement.getCommunicationDate() != null) {
                    if (lastAdvanceMeasurementReported == null) {
                        lastAdvanceMeasurementReported = advanceMeasurement;
                    } else {
                        if (advanceMeasurement.getCommunicationDate()
                                .compareTo(
                                        lastAdvanceMeasurementReported
                                                .getCommunicationDate()) > 0) {
                            lastAdvanceMeasurementReported = advanceMeasurement;
                        }
                    }
                }
            }
        }

        return lastAdvanceMeasurementReported;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAnyAdvanceMeasurementNotReported(
            Set<DirectAdvanceAssignment> allDirectAdvanceAssignments) {
        if (allDirectAdvanceAssignments.isEmpty()) {
            return false;
        }

        for (DirectAdvanceAssignment advanceAssignment : allDirectAdvanceAssignments) {
            for (AdvanceMeasurement advanceMeasurement : advanceAssignment.getAdvanceMeasurements()) {
                if (advanceMeasurement.getCommunicationDate() == null) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    @Transactional(rollbackFor = { ConnectionProblemsException.class,
            UnrecoverableErrorServiceException.class })
    public void sendAdvanceMeasurements(Order order)
            throws UnrecoverableErrorServiceException,
            ConnectionProblemsException {
        orderElementDAO.save(order);

        OrderElementWithAdvanceMeasurementsListDTO orderElementWithAdvanceMeasurementsListDTO = getOrderElementWithAdvanceMeasurementsListDTO(order);
        ExternalCompany externalCompany = order.getCustomer();

        NaiveTrustProvider.setAlwaysTrust(true);

        WebClient client = WebClient.create(externalCompany.getAppURI());

        client.path("ws/rest/reportadvances");

        Util.addAuthorizationHeader(client, externalCompany
                .getOurCompanyLogin(), externalCompany.getOurCompanyPassword());

        try {
            InstanceConstraintViolationsListDTO instanceConstraintViolationsListDTO = client
                    .post(orderElementWithAdvanceMeasurementsListDTO,
                            InstanceConstraintViolationsListDTO.class);

            List<InstanceConstraintViolationsDTO> instanceConstraintViolationsList = instanceConstraintViolationsListDTO.instanceConstraintViolationsList;
            if ((instanceConstraintViolationsList != null)
                    && (!instanceConstraintViolationsList.isEmpty())) {
                String message = "";

                for (ConstraintViolationDTO constraintViolationDTO : instanceConstraintViolationsList
                        .get(0).constraintViolations) {
                    message += constraintViolationDTO.toString() + "\n";
                }

                throw new UnrecoverableErrorServiceException(message);
            }
        } catch (WebApplicationException e) {
            LOG.error("Problems connecting with client web service", e);

            String message = _("Problems connecting with client web service");
            if (e.getMessage() != null) {
                message += ". " + _("Error: {0}", e.getMessage());
            }

            throw new ConnectionProblemsException(message, e);
        }
    }

    private OrderElementWithAdvanceMeasurementsListDTO getOrderElementWithAdvanceMeasurementsListDTO(
            Order order) {
        List<OrderElementWithAdvanceMeasurementsDTO> orderElementWithAdvanceMeasurementsDTOs = new ArrayList<OrderElementWithAdvanceMeasurementsDTO>();

        Set<DirectAdvanceAssignment> directAdvanceAssignments = order
                .getDirectAdvanceAssignmentsOfSubcontractedOrderElements();

        for (DirectAdvanceAssignment advanceAssignment : directAdvanceAssignments) {
            Set<AdvanceMeasurementDTO> advanceMeasurementDTOs = new HashSet<AdvanceMeasurementDTO>();

            for (AdvanceMeasurement advanceMeasurement : advanceAssignment
                    .getAdvanceMeasurements()) {
                if (advanceMeasurement.getCommunicationDate() == null) {
                    AdvanceMeasurementDTO advanceMeasurementDTO = OrderElementConverter
                            .toDTO(advanceMeasurement);
                    advanceMeasurement.updateCommunicationDate(new Date());
                    advanceMeasurementDTOs.add(advanceMeasurementDTO);
                }

            }

            if (!advanceMeasurementDTOs.isEmpty()) {
                OrderElementWithAdvanceMeasurementsDTO orderElementWithAdvanceMeasurementsDTO = new OrderElementWithAdvanceMeasurementsDTO(
                        advanceAssignment.getOrderElement().getExternalCode(),
                        advanceMeasurementDTOs);
                orderElementWithAdvanceMeasurementsDTOs
                        .add(orderElementWithAdvanceMeasurementsDTO);
            }
        }

        return new OrderElementWithAdvanceMeasurementsListDTO(getCompanyCode(),
                orderElementWithAdvanceMeasurementsDTOs);
    }

    private String getCompanyCode() {
        return configurationDAO.getConfiguration().getCompanyCode();
    }

    @Override
    @Transactional(readOnly = true)
    public String exportXML(Order order) {
        OrderElementWithAdvanceMeasurementsListDTO orderElementWithAdvanceMeasurementsListDTO = getOrderElementWithAdvanceMeasurementsListDTO(order);

        StringWriter xml = new StringWriter();
        try {
            JAXBContext jaxbContext = JAXBContext
                    .newInstance(OrderElementWithAdvanceMeasurementsListDTO.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.marshal(orderElementWithAdvanceMeasurementsListDTO, xml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return xml.toString();
    }

}
