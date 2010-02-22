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
package org.navalplanner.business.templates.entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.validator.Valid;
import org.navalplanner.business.i18n.I18nHelper;
import org.navalplanner.business.orders.entities.CriterionRequirementOrderElementHandler;
import org.navalplanner.business.orders.entities.HoursGroup;
import org.navalplanner.business.orders.entities.OrderElement;
import org.navalplanner.business.orders.entities.OrderLineGroup;
import org.navalplanner.business.orders.entities.SchedulingState;
import org.navalplanner.business.orders.entities.TreeNodeOnListWithSchedulingState;
import org.navalplanner.business.requirements.entities.DirectCriterionRequirement;
import org.navalplanner.business.trees.ITreeParentNode;

/**
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 *
 */
public class OrderLineGroupTemplate extends OrderElementTemplate implements
        ITreeParentNode<OrderElementTemplate> {

    private final CriterionRequirementTemplateHandler criterionRequirementTemplateHandler = CriterionRequirementTemplateHandler
            .getInstance();

    private final class ChildrenManipulator extends
            TreeNodeOnListWithSchedulingState<OrderElementTemplate> {

        ChildrenManipulator(List<OrderElementTemplate> templates) {
            super(templates);
        }

        @Override
        protected void setParentIfRequired(OrderElementTemplate newChild) {
            newChild.setParent(OrderLineGroupTemplate.this);
        }

        @Override
        public ITreeParentNode<OrderElementTemplate> getParent() {
            return OrderLineGroupTemplate.this.getParent();
        }

        @Override
        public OrderElementTemplate getThis() {
            return OrderLineGroupTemplate.this;
        }

        @Override
        public ITreeParentNode<OrderElementTemplate> toContainer() {
            return OrderLineGroupTemplate.this.toContainer();
        }

        @Override
        public OrderElementTemplate toLeaf() {
            return OrderLineGroupTemplate.this.toLeaf();
        }

        @Override
        protected SchedulingState getSchedulingStateFrom(
                OrderElementTemplate node) {
            return node.getSchedulingState();
        }

        @Override
        protected void updateWithNewChild(SchedulingState newChildState) {
            getThis().getSchedulingState().add(newChildState);
        }

    }

    public static OrderLineGroupTemplate createNew() {
        return create(new OrderLineGroupTemplate());
    }

    public static OrderLineGroupTemplate create(OrderLineGroup group) {
        return create(new OrderLineGroupTemplate(), group);
    }

    protected static <T extends OrderLineGroupTemplate> T create(T beingBuilt,
            OrderLineGroup group) {
        OrderElementTemplate.create(beingBuilt, group);
        List<OrderElementTemplate> result = buildChildrenTemplates(beingBuilt,
                group.getChildren());
        beingBuilt.children = result;
        beingBuilt.copyIndirectRequirementsFromOriginalDirectRequirements();
        return beingBuilt;
    }

    public void copyIndirectRequirementsFromOriginalDirectRequirements() {
        for (DirectCriterionRequirement each: getDirectCriterionRequirements()) {
            criterionRequirementTemplateHandler
                    .copyIndirectCriterionRequirementFromOriginalToOrderLineGroupChildren(this, each);
        }
    }

    private static List<OrderElementTemplate> buildChildrenTemplates(
            OrderLineGroupTemplate parent, List<OrderElement> children) {
        List<OrderElementTemplate> result = new ArrayList<OrderElementTemplate>();
        for (OrderElement each : children) {
            OrderElementTemplate template = each.createTemplate();
            template.setParent(parent);
            result.add(template);
        }
        return result;
    }

    private List<OrderElementTemplate> children = new ArrayList<OrderElementTemplate>();

    @Override
    public List<OrderElementTemplate> getChildrenTemplates() {
        return Collections.unmodifiableList(children);
    }

    private ChildrenManipulator getManipulator() {
        return new ChildrenManipulator(children);
    }

    @Override
    public void add(OrderElementTemplate newChild) {
        getManipulator().add(newChild);
    }

    @Override
    public void add(int position, OrderElementTemplate newChild) {
        getManipulator().add(position, newChild);
    }

    @Override
    public void down(OrderElementTemplate existentChild) {
        getManipulator().down(existentChild);
    }

    @Override
    public void remove(OrderElementTemplate existentChild) {
        getManipulator().remove(existentChild);
    }

    @Override
    public void replace(OrderElementTemplate previousChild,
            OrderElementTemplate newChild) {
        getManipulator().replace(previousChild, newChild);
    }

    @Override
    public void up(OrderElementTemplate existentChild) {
        getManipulator().up(existentChild);
    }

    @Override
    @Valid
    public List<OrderElementTemplate> getChildren() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public ITreeParentNode<OrderElementTemplate> toContainer() {
        return this;
    }

    @Override
    public OrderElementTemplate toLeaf() {
        OrderLineTemplate result = OrderLineTemplate.createNew();
        copyTo(result);
        return result;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    private CriterionRequirementOrderElementHandler criterionRequirementOrderElementHandler = CriterionRequirementOrderElementHandler
            .getInstance();

    protected <T extends OrderLineGroup> T setupGroupParts(T group) {
        T parent = setupElementParts(group);
        for (OrderElementTemplate each : children) {
            each.createElement(parent);
        }
        copyIndirectCriterionRequirementFromOriginalToOrderLineGroupChildren(parent);
        return parent;
    }

    private void copyIndirectCriterionRequirementFromOriginalToOrderLineGroupChildren(OrderLineGroup orderElement) {
        for (DirectCriterionRequirement each: orderElement.getDirectCriterionRequirement()) {
            criterionRequirementOrderElementHandler.copyIndirectCriterionRequirementFromOriginalToOrderLineGroupChildren(
                            orderElement, each);
        }
    }

    @Override
    public OrderLineGroup createElement() {
        OrderLineGroup orderLineGroup = OrderLineGroup.create();
        orderLineGroup.initializeTemplate(this);
        return setupGroupParts(setupSchedulingStateType(orderLineGroup));
    }

    @Override
    public OrderElement createElement(OrderLineGroup parent) {
        OrderLineGroup result = setupSchedulingStateType(OrderLineGroup
                .create());
        result.initializeTemplate(this);
        parent.add(result);
        return setupGroupParts(result);
    }

    @Override
    public String getType() {
        return I18nHelper._("Group");
    }

    @Override
    public List<HoursGroup> getHoursGroups() {
        List<HoursGroup> hoursGroups = new ArrayList<HoursGroup>();
        for (OrderElementTemplate orderElement : children) {
            hoursGroups.addAll(orderElement.getHoursGroups());
        }
        return hoursGroups;
    }

    @Override
    public Integer getWorkHours() {
        Integer result = 0;
        for (OrderElementTemplate orderElement : getChildren()) {
            result += orderElement.getWorkHours();
        }
        return result;
    }

}
