package org.navalplanner.web.users;

import static org.navalplanner.web.I18nHelper._;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.validator.InvalidValue;
import org.navalplanner.business.common.exceptions.InstanceNotFoundException;
import org.navalplanner.business.common.exceptions.ValidationException;
import org.navalplanner.business.orders.daos.IOrderDAO;
import org.navalplanner.business.orders.entities.Order;
import org.navalplanner.business.users.daos.IOrderAuthorizationDAO;
import org.navalplanner.business.users.daos.IUserDAO;
import org.navalplanner.business.users.entities.OrderAuthorization;
import org.navalplanner.business.users.entities.OrderAuthorizationType;
import org.navalplanner.business.users.entities.Profile;
import org.navalplanner.business.users.entities.ProfileOrderAuthorization;
import org.navalplanner.business.users.entities.User;
import org.navalplanner.business.users.entities.UserOrderAuthorization;
import org.navalplanner.business.users.entities.UserRole;
import org.navalplanner.web.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Model for UI operations related to {@link OrderAuthorization}
 *
 * @author Jacobo Aragunde Perez <jaragunde@igalia.com>
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class OrderAuthorizationModel implements IOrderAuthorizationModel {

    private Order order;

    private List<ProfileOrderAuthorization> profileOrderAuthorizationList;

    private List<UserOrderAuthorization> userOrderAuthorizationList;

    private List<OrderAuthorization> orderAuthorizationRemovalList;

    @Autowired
    private IOrderAuthorizationDAO dao;

    @Autowired
    private IOrderDAO orderDAO;

    @Autowired
    private IUserDAO userDAO;

    @Override
    public List<OrderAuthorizationType> addProfileOrderAuthorization(
            Profile profile, List<OrderAuthorizationType> authorizations) {
        List<OrderAuthorizationType> duplicated =
            new ArrayList<OrderAuthorizationType>();
        List<ProfileOrderAuthorization> existingAuthorizations =
            listAuthorizationsByProfile(profile);
        for(OrderAuthorizationType type : authorizations) {
            if(listContainsAuthorizationType(existingAuthorizations, type)) {
                duplicated.add(type);
            }
            else {
                ProfileOrderAuthorization orderAuthorization =
                    createProfileOrderAuthorization(order, profile);
                orderAuthorization.setAuthorizationType(type);
                profileOrderAuthorizationList.add(orderAuthorization);
            }
        }
        return duplicated.isEmpty()? null : duplicated;
    }

    @Override
    public List<OrderAuthorizationType> addUserOrderAuthorization(
            User user, List<OrderAuthorizationType> authorizations) {
        List<OrderAuthorizationType> duplicated =
            new ArrayList<OrderAuthorizationType>();
        List<UserOrderAuthorization> existingAuthorizations =
            listAuthorizationsByUser(user);
        for(OrderAuthorizationType type : authorizations) {
            if(listContainsAuthorizationType(existingAuthorizations, type)) {
                duplicated.add(type);
            }
            else {
                UserOrderAuthorization orderAuthorization =
                    createUserOrderAuthorization(order, user);
                orderAuthorization.setAuthorizationType(type);
                userOrderAuthorizationList.add(orderAuthorization);
            }
        }
        return duplicated.isEmpty()? null : duplicated;
    }

    @Override
    @Transactional
    public void confirmSave() {
        try {
            if(order.isNewObject()) {
                //if it was new, we reload the order from the DAO
                Order newOrder = orderDAO.find(order.getId());
                replaceOrder(newOrder);
            }
        }catch (InstanceNotFoundException e) {
            InvalidValue invalidValue = new InvalidValue(_("Order does not exist"),
                    OrderAuthorization.class, "order", order, null);
            throw new ValidationException(invalidValue);
        }
        for(OrderAuthorization authorization : profileOrderAuthorizationList) {
            dao.save(authorization);
        }
        for(OrderAuthorization authorization : userOrderAuthorizationList) {
            dao.save(authorization);
        }
        for(OrderAuthorization authorization : orderAuthorizationRemovalList) {
            try {
                dao.remove(authorization.getId());
            }
            catch(InstanceNotFoundException e) {}
        }
    }

    @Override
    public List<ProfileOrderAuthorization> getProfileOrderAuthorizations() {
        return profileOrderAuthorizationList;
    }

    @Override
    public List<UserOrderAuthorization> getUserOrderAuthorizations() {
        return userOrderAuthorizationList;
    }

    @Override
    @Transactional(readOnly = true)
    public void initCreate(Order order) {
        this.order = order;
        initializeLists();
        //add write authorization for current user
        try {
            User user = userDAO.findByLoginName(SecurityUtils.getSessionUserLoginName());
            UserOrderAuthorization orderAuthorization =
                createUserOrderAuthorization(order, user);
            orderAuthorization.setAuthorizationType(OrderAuthorizationType.WRITE_AUTHORIZATION);
            userOrderAuthorizationList.add(orderAuthorization);
        }
        catch(InstanceNotFoundException e) {
            //this case shouldn't happen, because it would mean that there isn't a logged user
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void initEdit(Order order) {
        this.order = order;
        initializeLists();
        //Retrieve the OrderAuthorizations associated with this order
        for(OrderAuthorization authorization : dao.listByOrder(order)) {
            forceLoadEntities(authorization);
            if(authorization instanceof UserOrderAuthorization) {
                userOrderAuthorizationList.add(
                        (UserOrderAuthorization) authorization);
            }
            if(authorization instanceof ProfileOrderAuthorization) {
                profileOrderAuthorizationList.add(
                        (ProfileOrderAuthorization) authorization);
            }
        }
    }

    private void initializeLists() {
        profileOrderAuthorizationList =
            new ArrayList<ProfileOrderAuthorization>();
        userOrderAuthorizationList =
            new ArrayList<UserOrderAuthorization>();
        orderAuthorizationRemovalList =
            new ArrayList<OrderAuthorization>();
    }

    private void forceLoadEntities(OrderAuthorization authorization) {
        authorization.getOrder().getName();
        if(authorization instanceof UserOrderAuthorization) {
            ((UserOrderAuthorization)authorization).getUser().getLoginName();
        }
        if(authorization instanceof ProfileOrderAuthorization) {
            ((ProfileOrderAuthorization)authorization).getProfile().getProfileName();
        }
    }

    @Override
    public void removeOrderAuthorization(OrderAuthorization orderAuthorization) {
        if(orderAuthorization instanceof UserOrderAuthorization) {
            userOrderAuthorizationList.remove(
                    (UserOrderAuthorization) orderAuthorization);
        }
        if(orderAuthorization instanceof ProfileOrderAuthorization) {
            profileOrderAuthorizationList.remove(
                    (ProfileOrderAuthorization) orderAuthorization);
        }
        if(!orderAuthorization.isNewObject()) {
            orderAuthorizationRemovalList.add(orderAuthorization);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userCanWrite(String loginName) {
        if (SecurityUtils.isUserInRole(UserRole.ROLE_EDIT_ALL_ORDERS)) {
            return true;
        }
        else {
            User user;
            try {
                user = userDAO.findByLoginName(loginName);
            }
            catch(InstanceNotFoundException e) {
                return false;
            }
            List<OrderAuthorization> authorizations =
                dao.listByOrderUserAndItsProfiles(order, user);
            for(OrderAuthorization authorization : authorizations) {
                if (authorization.getAuthorizationType() ==
                        OrderAuthorizationType.WRITE_AUTHORIZATION) {
                    return true;
                }
            }
            return false;
        }
    }

    private ProfileOrderAuthorization createProfileOrderAuthorization(
            Order order, Profile profile) {
        ProfileOrderAuthorization orderAuthorization =
            ProfileOrderAuthorization.create();
        orderAuthorization.setOrder(order);
        orderAuthorization.setProfile(profile);
        return orderAuthorization;
    }

    private UserOrderAuthorization createUserOrderAuthorization(
            Order order, User user) {
        UserOrderAuthorization orderAuthorization =
            UserOrderAuthorization.create();
        orderAuthorization.setOrder(order);
        orderAuthorization.setUser(user);
        return orderAuthorization;
    }

    private void replaceOrder(Order newOrder) {
        for(OrderAuthorization authorization : profileOrderAuthorizationList) {
            authorization.setOrder(newOrder);
            dao.save(authorization);
        }
        for(OrderAuthorization authorization : userOrderAuthorizationList) {
            authorization.setOrder(newOrder);
            dao.save(authorization);
        }
        this.order = newOrder;
    }

    private List<UserOrderAuthorization> listAuthorizationsByUser(User user) {
        List<UserOrderAuthorization> list = new ArrayList<UserOrderAuthorization>();
        for(UserOrderAuthorization authorization : userOrderAuthorizationList) {
            if(authorization.getUser().getId().equals(user.getId())) {
                list.add(authorization);
            }
        }
        return list;
    }

    private List<ProfileOrderAuthorization> listAuthorizationsByProfile(Profile profile){
        List<ProfileOrderAuthorization> list = new ArrayList<ProfileOrderAuthorization>();
        for(ProfileOrderAuthorization authorization : profileOrderAuthorizationList) {
            if(authorization.getProfile().getId().equals(profile.getId())) {
                list.add(authorization);
            }
        }
        return list;
    }

    private boolean listContainsAuthorizationType(List<? extends OrderAuthorization> list,
            OrderAuthorizationType authorizationType) {
        for(OrderAuthorization authorization : list) {
            if(authorization.getAuthorizationType().equals(authorizationType)) {
                return true;
            }
        }
        return false;
    }
}
