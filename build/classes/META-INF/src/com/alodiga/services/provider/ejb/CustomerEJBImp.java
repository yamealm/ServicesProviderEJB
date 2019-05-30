package com.alodiga.services.provider.ejb;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import org.apache.log4j.Logger;
import com.alodiga.services.provider.commons.ejbs.CustomerEJB;
import com.alodiga.services.provider.commons.ejbs.CustomerEJBLocal;
import com.alodiga.services.provider.commons.exceptions.EmptyListException;
import com.alodiga.services.provider.commons.exceptions.GeneralException;
import com.alodiga.services.provider.commons.exceptions.NullParameterException;
import com.alodiga.services.provider.commons.exceptions.RegisterNotFoundException;
import com.alodiga.services.provider.commons.genericEJB.AbstractSPEJB;
import com.alodiga.services.provider.commons.genericEJB.SPContextInterceptor;
import com.alodiga.services.provider.commons.genericEJB.SPLoggerInterceptor;
import com.alodiga.services.provider.commons.genericEJB.EJBRequest;
import com.alodiga.services.provider.commons.models.Address;
import com.alodiga.services.provider.commons.models.Customer;
import com.alodiga.services.provider.commons.utils.EjbConstants;
import com.alodiga.services.provider.commons.utils.QueryConstants;
import java.util.ArrayList;
import javax.interceptor.Interceptors;
import javax.persistence.NoResultException;
import javax.persistence.Query;

@Interceptors({SPLoggerInterceptor.class, SPContextInterceptor.class})
@Stateless(name = EjbConstants.CUSTOMER_EJB, mappedName = EjbConstants.CUSTOMER_EJB)
@TransactionManagement(TransactionManagementType.BEAN)
public class CustomerEJBImp extends AbstractSPEJB implements CustomerEJB, CustomerEJBLocal {

    private static final Logger logger = Logger.getLogger(CustomerEJBImp.class);

    public Customer deleteCustomer(EJBRequest request) throws GeneralException, NullParameterException {
        return null;
    }

    public List<Customer> getCustomers(EJBRequest request) throws GeneralException, NullParameterException, EmptyListException {
        return (List<Customer>) listEntities(Customer.class, request, logger, getMethodName());
    }

    public List<Customer> getCustomersByConditions(EJBRequest request) throws GeneralException, EmptyListException, NullParameterException {
        Boolean isFilter = (Boolean) request.getParam();
        if (isFilter == null || isFilter.equals("null")) {
            isFilter = false;
        }
        Map orderField = new HashMap();
        orderField.put("id", QueryConstants.ORDER_DESC);
        return (List<Customer>) createSearchQuery(Customer.class, request, orderField, logger, getMethodName(), "customers", isFilter);
    }

    public Customer loadCustomer(EJBRequest request) throws GeneralException, RegisterNotFoundException, NullParameterException {
        return (Customer) loadEntity(Customer.class, request, logger, getMethodName());
    }

    public Customer loadCustomerByAni(String ani) throws RegisterNotFoundException, GeneralException, NullParameterException {

        Customer customer = null;
        try {
            Query query = createQuery("SELECT pinFree.pin.customer FROM PinFree pinFree WHERE pinFree.ani =?1");
            query.setParameter("1", ani);
            customer = (Customer) query.setHint("toplink.refresh", "true").getSingleResult();
        } catch (NoResultException e) {
            throw new RegisterNotFoundException(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }
        return customer;

    }

    public Customer loadCustomerByEmail(EJBRequest request) throws RegisterNotFoundException, NullParameterException, GeneralException {

        List<Customer> customers = null;
        Map<String, Object> params = request.getParams();

        if (!params.containsKey(QueryConstants.PARAM_EMAIL)) {
            throw new NullParameterException( sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), QueryConstants.PARAM_EMAIL), null);
        }
        try {
            customers = (List<Customer>) getNamedQueryResult(Customer.class, QueryConstants.LOAD_CUSTOMER_BY_EMAIL, request, getMethodName(), logger, "Customer");
        } catch (EmptyListException e) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName(), "Customer"), null);
        }

        return customers.get(0);
    }

    public Customer loadCustomerByLogin(EJBRequest request) throws RegisterNotFoundException, NullParameterException, GeneralException {
        List<Customer> customers = null;
        Map<String, Object> params = request.getParams();

        if (!params.containsKey(QueryConstants.PARAM_LOGIN)) {
            throw new NullParameterException( sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), QueryConstants.PARAM_LOGIN), null);
        }
        try {
            customers = (List<Customer>) getNamedQueryResult(Customer.class, QueryConstants.LOAD_CUSTOMER_BY_LOGIN, request, getMethodName(), logger, "Customer");
        } catch (EmptyListException e) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName(), "Customer"), null);
        }
        return customers.get(0);
    }

    public Customer loadCustomerByLogin(String login) throws GeneralException, RegisterNotFoundException, NullParameterException {
        if (login == null || login.equals("")) {
            throw new NullParameterException( sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), QueryConstants.PARAM_LOGIN), null);
        }
        Customer customer = null;
        String sql = "SELECT c FROM Customer c WHERE c.login = :login";
        try {
            Query query = null;
            query = createQuery(sql);
            query.setParameter("login", login);
            customer = (Customer) query.setHint("toplink.refresh", "true").getSingleResult();
        } catch (NoResultException e) {
            throw new RegisterNotFoundException(e.getMessage());
        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }
        return customer;

    }

    public Customer loadCustomerByProductPinFree(String pinFree) throws GeneralException, NullParameterException {
        Customer customer = null;
        return customer;
    }

    public Customer loadCustomerBySerial(String serial) throws RegisterNotFoundException, GeneralException, NullParameterException {
        return null;
    }

    public Customer saveCustomer(EJBRequest request) throws GeneralException, NullParameterException {
        return (Customer) saveEntity(request, logger, getMethodName());
    }

    public Customer saveCustomer(Customer customer) throws NullParameterException, GeneralException {
        if (customer == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "customer"), null);
        }
        return (Customer) saveEntity(customer);
    }

     public Address saveAddress(Address address) throws NullParameterException, GeneralException {
        if (address == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "address"), null);
        }
        return (Address) saveEntity(address);
    }
    /*
    public CustomerProductDetail saveCustomerProductDetail(EJBRequest request) throws GeneralException, NullParameterException {
    return (CustomerProductDetail) saveEntity(request, logger, getMethodName());
    }*/

    public List<Customer> searchCustomers(Long enterpriseId, String login, String fullName, String email) throws EmptyListException, NullParameterException, GeneralException {
        List<Customer> customers = new ArrayList<Customer>();
        if (enterpriseId == null) {
            throw new NullParameterException( sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "enterpriseId"), null);
        }
        StringBuilder sqlBuilder = new StringBuilder("SELECT c FROM Customer c WHERE c.enterprise.id = ?1 AND c.enabled = 1");
        try {
            if (login != null) {
                sqlBuilder.append(" AND c.login like '%").append(login).append("%'");
            }
            if (fullName != null) {
                sqlBuilder.append(" AND c.firstName like '%").append(fullName).append("%'");
            }
            if (email != null) {
                sqlBuilder.append(" AND c.email like '%").append(email).append("%'");
            }

            Query query = entityManager.createQuery(sqlBuilder.toString());
            query.setParameter("1", enterpriseId);
            customers = query.setHint("toplink.refresh", "true").getResultList();
        } catch (NoResultException ex) {
//            throw new EmptyListException("No distributions found");
        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }
        return customers;

    }


}
