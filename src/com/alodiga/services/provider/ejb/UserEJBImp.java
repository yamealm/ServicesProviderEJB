package com.alodiga.services.provider.ejb;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJBException;
import javax.ejb.RemoveException;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.interceptor.Interceptors;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.apache.log4j.Logger;

import com.alodiga.services.provider.commons.ejbs.UserEJB;
import com.alodiga.services.provider.commons.ejbs.UserEJBLocal;
import com.alodiga.services.provider.commons.exceptions.EmptyListException;
import com.alodiga.services.provider.commons.exceptions.GeneralException;
import com.alodiga.services.provider.commons.exceptions.NullParameterException;
import com.alodiga.services.provider.commons.exceptions.RegisterNotFoundException;
import com.alodiga.services.provider.commons.genericEJB.AbstractSPEJB;
import com.alodiga.services.provider.commons.genericEJB.EJBRequest;
import com.alodiga.services.provider.commons.genericEJB.SPContextInterceptor;
import com.alodiga.services.provider.commons.genericEJB.SPGenericEntity;
import com.alodiga.services.provider.commons.genericEJB.SPLoggerInterceptor;
import com.alodiga.services.provider.commons.models.Account;
import com.alodiga.services.provider.commons.models.AccountProduct;
import com.alodiga.services.provider.commons.models.Invoice;
import com.alodiga.services.provider.commons.models.Payment;
import com.alodiga.services.provider.commons.models.Permission;
import com.alodiga.services.provider.commons.models.PermissionGroup;
import com.alodiga.services.provider.commons.models.Profile;
import com.alodiga.services.provider.commons.models.User;
import com.alodiga.services.provider.commons.utils.EjbConstants;
import com.alodiga.services.provider.commons.utils.GeneralUtils;
import com.alodiga.services.provider.commons.utils.QueryConstants;

@Interceptors({SPLoggerInterceptor.class, SPContextInterceptor.class})
@Stateless(name = EjbConstants.USER_EJB, mappedName = EjbConstants.USER_EJB)
@TransactionManagement(TransactionManagementType.BEAN)
public class UserEJBImp extends AbstractSPEJB implements UserEJB, UserEJBLocal {

    private static final Logger logger = Logger.getLogger(UserEJBImp.class);

    public List<User> getUsers(EJBRequest request) throws EmptyListException, GeneralException {

        List<User> users = null;
        try {
            users = (List<User>) createQuery("SELECT u FROM User u").setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        if (users.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return users;
    }

    public User loadUser(EJBRequest request) throws RegisterNotFoundException, NullParameterException, GeneralException {
        User user = (User) loadEntity(User.class, request, logger, getMethodName());

        return user;
    }

    public User loadUserByEmail(EJBRequest request) throws RegisterNotFoundException, NullParameterException, GeneralException {

        List<User> users = null;
        Map<String, Object> params = request.getParams();

        if (!params.containsKey(QueryConstants.PARAM_EMAIL)) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), QueryConstants.PARAM_EMAIL), null);
        }

        try {
            users = (List<User>) getNamedQueryResult(User.class, QueryConstants.LOAD_USER_BY_EMAIL, request, getMethodName(), logger, "User");
        } catch (EmptyListException e) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName(), "user"), null);
        }

        return users.get(0);
    }

    public User loadUserByLogin(EJBRequest request) throws RegisterNotFoundException, NullParameterException, GeneralException {
        List<User> users = null;
        Map<String, Object> params = request.getParams();

        if (!params.containsKey(QueryConstants.PARAM_LOGIN)) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), QueryConstants.PARAM_LOGIN), null);
        }
        try {
            users = (List<User>) getNamedQueryResult(User.class, QueryConstants.LOAD_USER_BY_LOGIN, request, getMethodName(), logger, "User");
        } catch (EmptyListException e) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName(), "user"), null);
        }
        return users.get(0);
    }

    public Account loadAccountByLogin(String login) throws RegisterNotFoundException, NullParameterException, GeneralException {
        Account account = null;
        if (login == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "login"), null);
        }
        try {
            Query query = createQuery("SELECT a FROM Account a WHERE a.login =?1");
            query.setParameter("1", login);
            account = (Account) query.getSingleResult();
        } catch (NoResultException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), ex);
        } catch (Exception ex) {
            ex.getMessage();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        return account;

    }

    public User saveUser(EJBRequest request) throws NullParameterException, GeneralException {
        return (User) saveEntity(request, logger, getMethodName());
    }

    public boolean validateExistingUser(EJBRequest request) throws NullParameterException, GeneralException {
        boolean valid = true;
        Map<String, Object> params = request.getParams();
        if (params.containsKey(QueryConstants.PARAM_LOGIN)) {
            try {
                loadUserByLogin(request);
            } catch (RegisterNotFoundException ex) {
                valid = false;
            } catch (NullParameterException ex) {
                throw new NullParameterException(ex.getMessage());
            } catch (GeneralException ex) {
                throw new GeneralException(ex.getMessage());
            }
        } else if (params.containsKey(QueryConstants.PARAM_EMAIL)) {
            try {
                loadUserByEmail(request);
            } catch (RegisterNotFoundException ex) {
                valid = false;
            } catch (NullParameterException ex) {
                throw new NullParameterException(ex.getMessage());
            } catch (GeneralException ex) {
                throw new GeneralException(ex.getMessage());
            }
        } else {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), QueryConstants.PARAM_LOGIN), null);
        }
        return valid;
    }

    public Float getPointsByDistributor(Long distributorId, int previousDays) throws NullParameterException, GeneralException {
        Float points = 0f;
        if (distributorId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "param"), null);
        }
        try {
            Calendar todaysMidnite = Calendar.getInstance();
            todaysMidnite.set(Calendar.HOUR, 0);
            todaysMidnite.set(Calendar.MINUTE, 0);
            todaysMidnite.set(Calendar.SECOND, 0);
            todaysMidnite.set(Calendar.MILLISECOND, 0);
            todaysMidnite.set(Calendar.AM_PM, Calendar.AM);
            Calendar tomorrowsMidnite = (Calendar) todaysMidnite.clone();

            if (previousDays > 0) {
                todaysMidnite.add(Calendar.DAY_OF_YEAR, -1 * previousDays);
            }

            tomorrowsMidnite.add(Calendar.DAY_OF_YEAR, 1);

            StringBuilder sqlBuilder = new StringBuilder("SELECT SUM(points) FROM multilevelchannel.distributor_points dp  WHERE  dp.creationDate BETWEEN ?1 AND ?2 AND dp.distributorId = ?3");
            Query query = entityManager.createNativeQuery(sqlBuilder.toString());
            query.setParameter("1", new Date(todaysMidnite.getTimeInMillis()));
            query.setParameter("2", new Date(tomorrowsMidnite.getTimeInMillis()));
            query.setParameter("3", distributorId);
            List result = query.setHint("toplink.refresh", "true").getResultList();
            if (result.get(0) != null) {
                points = new Float(result.get(0).toString());
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }

        return points;
    }

    public void remove(Object o) throws RemoveException, EJBException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<User> getUserTopUpNotification() throws EmptyListException, GeneralException {
        List<User> users = null;
        try {
            Query query = entityManager.createQuery("SELECT u FROM User u WHERE u.receiveTopUpNotification = TRUE");
            users = query.setHint("toplink.refresh", "true").getResultList();
        } catch (NoResultException ex) {
            throw new EmptyListException("No user found for TopUp Notifications");
        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }
        return users;
    }

    public void updateUserNotifications(String ids) throws NullParameterException, GeneralException {
        if (ids == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "ids"), null);
        }

        try {
            Query queryDisable = entityManager.createQuery("UPDATE User u SET u.receiveTopUpNotification = FALSE");
            EntityTransaction transaction = entityManager.getTransaction();
            try {
                transaction.begin();
                queryDisable.executeUpdate();
                if (!ids.equals("")) {
                    Query queryEnable = entityManager.createQuery("UPDATE User u SET u.receiveTopUpNotification = TRUE WHERE u.id IN (" + ids + ")");
                    queryEnable.executeUpdate();
                }
                transaction.commit();
            } catch (Exception ex) {
                ex.printStackTrace();
                try {
                    if (transaction.isActive()) {
                        transaction.rollback();
                    }
                } catch (Exception ex1) {
                    ex1.printStackTrace();
                }
            }

        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }

    }

    public Float getTotalPointsByDistributor(Long distributorId, Date beginningDate, Date endingDate) throws NullParameterException, GeneralException {

        Float points = 0f;
        if (distributorId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "distributorId"), null);
        }
        StringBuilder sqlBuilder = new StringBuilder("SELECT SUM(dp.points) FROM multilevelchannel.distributor_points dp WHERE dp.distributorId = ?1");
        if (beginningDate != null && beginningDate != null) {
            sqlBuilder.append(" AND dp.creationDate BETWEEN ?2 AND ?3");
        }
        Query query = entityManager.createNativeQuery(sqlBuilder.toString());
        query.setParameter("1", distributorId);
        if (beginningDate != null && beginningDate != null) {
            query.setParameter("2", GeneralUtils.getBeginningDate(beginningDate));
            query.setParameter("3", GeneralUtils.getEndingDate(endingDate));
        }
        List result = (List) query.setHint("toplink.refresh", "true").getSingleResult();
        //Double result = (Double) query.setHint("toplink.refresh", "true").getSingleResult();
        Double value = result != null && result.get(0) != null ? (Double) result.get(0) : 0f;
        points = value.floatValue();
        return points;
    }

    public List<PermissionGroup> getPermissionGroups() throws EmptyListException, NullParameterException, GeneralException {
        List<PermissionGroup> permissionGroups = new ArrayList<PermissionGroup>();
        Query query = null;
        try {
            query = createQuery("SELECT pg FROM PermissionGroup pg");
            permissionGroups = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }
        if (permissionGroups.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return permissionGroups;

    }

    public List<Permission> getPermissions() throws EmptyListException, NullParameterException, GeneralException {
        List<Permission> permissions = new ArrayList<Permission>();
        Query query = null;
        try {
            query = createQuery("SELECT p FROM Permission p WHERE p.enabled =1");
            permissions = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }
        if (permissions.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return permissions;
    }

    public List<Permission> getPermissionByGroupId(Long groupId) throws EmptyListException, NullParameterException, GeneralException {
        if (groupId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "groupId"), null);
        }
        List<Permission> permissions = new ArrayList<Permission>();
        Query query = null;
        try {
            query = createQuery("SELECT p FROM Permission p WHERE p.enabled =1 AND p.permissionGroup.id=?1");
            query.setParameter("1", groupId);
            permissions = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }
        if (permissions.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return permissions;
    }

    public List<Permission> getPermissionByProfileId(Long profileId) throws EmptyListException, NullParameterException, GeneralException {
        if (profileId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "groupId"), null);
        }
        List<Permission> permissions = new ArrayList<Permission>();
        Query query = null;
        try {
            query = createQuery("SELECT php.permission FROM PermissionHasProfile php WHERE php.profile.id = ?1");
            query.setParameter("1", profileId);
            permissions = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }
        if (permissions.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return permissions;
    }

    public Permission loadPermissionById(Long permissionId) throws GeneralException, NullParameterException, RegisterNotFoundException {
        EJBRequest bRequest = new EJBRequest(permissionId);
        Permission permission = (Permission) loadEntity(Permission.class, bRequest, logger, getMethodName());
        return permission;

    }

    public List<Profile> getProfiles() throws EmptyListException, GeneralException {

        List<Profile> profiles = new ArrayList<Profile>();
        Query query = null;
        try {
            query = createQuery("SELECT p FROM Profile p WHERE p.enabled = 1");
            profiles = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }
        if (profiles.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return profiles;
    }

    public Float getCurrentMonthlyPoints(Long distributorId, Date date) throws NullParameterException, GeneralException {
        Float points = 0f;
        if (distributorId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "distributorId"), null);
        } else if (date == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "date"), null);
        }
        StringBuilder sqlBuilder = new StringBuilder("SELECT SUM(dp.points) FROM DistributorPoints dp WHERE dp.distributor.id = ?1 AND dp.creationDate BETWEEN ?2 AND ?3");

        Query query = entityManager.createQuery(sqlBuilder.toString());
        query.setParameter("1", distributorId);
        query.setParameter("2", GeneralUtils.getFirstDateOfMonth(date));
        query.setParameter("3", GeneralUtils.getLastDateOfMonth(date));

        Double value = (Double) query.setHint("toplink.refresh", "true").getSingleResult();
        //Double result = (Double) query.setHint("toplink.refresh", "true").getSingleResult();
        //Double value = result != null && result.get(0) != null ? (Double) result.get(0) : 0f;
        points = value != null ? value.floatValue() : 0F;
        return points;
    }

    public Account saveAccount(EJBRequest request) throws GeneralException, NullParameterException {
        return (Account) saveEntity(request, logger, getMethodName());
    }

   public AccountProduct saveAccountProduct(EJBRequest request) throws GeneralException, NullParameterException {
        return (AccountProduct) saveEntity(request, logger, getMethodName());
    }

    public Invoice saveInvoice(EJBRequest request) throws GeneralException, NullParameterException {
        return (Invoice) saveEntity(request, logger, getMethodName());
    }

    public List<Account> getAccounts(EJBRequest request) throws EmptyListException, GeneralException, NullParameterException {
        List<Account> accounts = (List<Account>) listEntities(Account.class, request, logger, getMethodName());

        return accounts;
    }

    public List<Account> searchAccounts(Long enterpriseId, String login, String fullName, String email,String status) throws EmptyListException, NullParameterException, GeneralException {
        List<Account> accounts = new ArrayList<Account>();
        if (enterpriseId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "enterpriseId"), null);
        }
        StringBuilder sqlBuilder = new StringBuilder("SELECT a FROM Account a WHERE a.enterprise.id = ?1");
        try {
            if (login != null) {
                sqlBuilder.append(" AND a.login like '%").append(login).append("%'");
            }
            if (fullName != null) {
                sqlBuilder.append(" AND a.fullName like '%").append(fullName).append("%'");
            }
            if (email != null) {
                sqlBuilder.append(" AND a.email like '%").append(email).append("%'");
            }
            if (status != null && !status.equals(QueryConstants.STATUS_ACCOUNT_ALL)) {
                sqlBuilder.append(" AND a.enabled='").append(status).append("'");
            }
            Query query = entityManager.createQuery(sqlBuilder.toString());
            query.setParameter("1", enterpriseId);
            accounts = query.setHint("toplink.refresh", "true").getResultList();
        } catch (NoResultException ex) {
//            throw new EmptyListException("No distributions found");
        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }
        return accounts;
    }

    public Account loadAccount(EJBRequest request) throws RegisterNotFoundException, NullParameterException, GeneralException {
        Account account = (Account) loadEntity(Account.class, request, logger, getMethodName());
        return account;
    }

    public User loadUserByLogin(String login) throws RegisterNotFoundException, NullParameterException, GeneralException {
        User user = null;
        if (login == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "login"), null);
        }
        try {

            Query query = createQuery("SELECT u FROM User u WHERE u.login =?1 AND u.enabled=TRUE");
            query.setParameter("1", login);
            user = (User) query.getSingleResult();

        } catch (NoResultException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), ex);
        } catch (Exception ex) {
            ex.getMessage();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        return user;

    }

    public Float CountTransaccionByPaymentInfo(String creditCarNumber) throws RegisterNotFoundException, NullParameterException, GeneralException {
       Float points = 0f;
        if (creditCarNumber == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "creditCarNumber"), null);
        }
        StringBuilder sqlBuilder = new StringBuilder("SELECT SUM(dp.points) FROM DistributorPoints dp WHERE dp.distributor.id = ?1 AND dp.creationDate BETWEEN ?2 AND ?3");

        Query query = entityManager.createQuery(sqlBuilder.toString());
        query.setParameter("1", creditCarNumber);


        Double value = (Double) query.setHint("toplink.refresh", "true").getSingleResult();
        //Double result = (Double) query.setHint("toplink.refresh", "true").getSingleResult();
        //Double value = result != null && result.get(0) != null ? (Double) result.get(0) : 0f;
        points = value != null ? value.floatValue() : 0F;
        return points;
    }


    public Account loadCurrentParentByAccount(Long accountId) throws GeneralException, NullParameterException, RegisterNotFoundException {

        if (accountId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "accountId"), null);
        }

     EJBRequest request = new EJBRequest();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(QueryConstants.PARAM_ACCOUNT, accountId);
        params.put(QueryConstants.PARAM_ENDED, false);
        request.setParams(params);
        try {
            return this.getAccountParentsHasAccount(request).get(0);
        } catch (EmptyListException e) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName(), "Account"), null);
        }
    }

     @Override
    public List<Account> getAccountParentsHasAccount(EJBRequest request) throws GeneralException, NullParameterException, EmptyListException {
        List<Account> accounts = null;
        Map<String, Object> params = request.getParams();
        StringBuilder sqlBuilder = new StringBuilder("SELECT ahp.accountParent FROM AccountHasParent ahp WHERE ahp.accountChild.id = ?1");
        if (!params.containsKey(QueryConstants.PARAM_ACCOUNT)) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), params.containsKey(QueryConstants.PARAM_ACCOUNT)), null);
        }
        Query query = null;
        try {
            if (params.containsKey(QueryConstants.PARAM_ENDED) && params.get(QueryConstants.PARAM_ENDED).equals(true)) {
                sqlBuilder.append("AND ahp.endingDate IS NOT NULL");
            }
            if (params.containsKey(QueryConstants.PARAM_ENDED) && params.get(QueryConstants.PARAM_ENDED).equals(false)) {
                sqlBuilder.append("AND ahp.endingDate IS NULL");
            }

            query = createQuery(sqlBuilder.toString());
            query.setParameter("1", params.get(QueryConstants.PARAM_ACCOUNT));
            if (request.getLimit() != null && request.getLimit() > 0) {
                query.setMaxResults(request.getLimit());
            }

            accounts = query.getResultList();
        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }
        if (accounts.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return accounts;
    }




    public Account searchAccountsByLogin(String login) throws RegisterNotFoundException, NullParameterException, GeneralException {
        Account accounts = new Account();


        try {

            Query query = createQuery("SELECT a FROM Account a  WHERE a.login =?1");
            query.setParameter("1", login);
            accounts = (Account) query.getSingleResult();

        } catch (NoResultException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), ex);
        } catch (Exception ex) {
            ex.getMessage();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        return accounts;
    }

    public Payment savePay(Object o) throws GeneralException, NullParameterException {
        Payment pa = null;
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        if (((SPGenericEntity) o).getPk() != null) {
            //processAuditData(EventTypeEnum.UPDATE, entity, auditData, entityManagerWrapper);
            pa = (Payment) entityManagerWrapper.update(o);
        } else {
            pa = (Payment) entityManagerWrapper.save(o);
            //processAuditData(EventTypeEnum.CREATE, entity, auditData, entityManagerWrapper);
        }
        transaction.commit();
        return pa;
    }

    public Invoice getInvoiceByID(Long invoiceId) throws EmptyListException, GeneralException, NullParameterException, RegisterNotFoundException {

        Invoice invoice = null;

        if (invoiceId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "invoiceId"), null);
        }
        try {

            Query query = createQuery("SELECT i FROM Invoice i WHERE i.id =?1");
            query.setParameter("1", invoiceId);
            invoice = (Invoice) query.getSingleResult();

        } catch (NoResultException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), ex);
        } catch (Exception ex) {
            ex.getMessage();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }

        return invoice;
    }

    public Account loadAccountbyFullName(String fullName) {
        Account account = null;
        account = entityManager.find(Account.class, fullName);
        if (account != null) {
            return account;
        } else {
            account = null;
        }
        return account;
    }

    public Account loadAccountById(String id) throws RegisterNotFoundException, NullParameterException, GeneralException {
        Account account = null;
        Long accountId = Long.valueOf(id);
        if (id == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "login"), null);
        }
        try {
            Query query = createQuery("SELECT a FROM Account a WHERE a.id =?1");
            query.setParameter("1", accountId);
            account = (Account) query.getSingleResult();
        } catch (NoResultException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), ex);
        } catch (Exception ex) {
            ex.getMessage();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        return account;

    }

 

    public float getAccountProductCalculationByAccountId(Long accountId, Long productId) throws GeneralException, RegisterNotFoundException, NullParameterException {
        if (accountId == null) {
            throw new NullParameterException("Parameter accountId cannot be null");
        }
        AccountProduct accountProduct = new AccountProduct();
        try {
            Query query = createQuery("SELECT t FROM AccountProduct t WHERE t.account.id = ?1 AND t.product.id = ?2 AND t.endingDate IS NULL");
            query.setParameter("1", accountId);
            query.setParameter("2", productId);
            accountProduct = (AccountProduct) query.getSingleResult();
        } catch (NoResultException ex) {
            return 0f;
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        return accountProduct.getCommission();
    }

    public List<AccountProduct> loadAccountProductCalculationByAccountId(Long accountId) throws NullParameterException, EmptyListException, GeneralException{
        List<AccountProduct> accountProduct = null;

        if (accountId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "accountId"), null);
        }
        try {

            Query query = createQuery("SELECT i FROM AccountProduct i WHERE i.account.id =?1 AND i.endingDate is null");
            query.setParameter("1", accountId);
            accountProduct = query.getResultList();

        } catch (Exception ex) {
            ex.getMessage();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }

        if(accountProduct.isEmpty()){
            throw new EmptyListException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "accountId"), null);
        }

        return accountProduct;
    }


}
