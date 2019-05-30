package com.alodiga.services.provider.ejb;

import com.alodiga.services.provider.commons.ejbs.BillPaymentEJBLocal;
import com.alodiga.services.provider.commons.exceptions.*;
import com.alodiga.services.provider.commons.models.*;
import com.alodiga.services.provider.commons.responses.*;
import com.alodiga.services.provider.commons.utils.AccountData;
import com.sg123.exception.PinNotFoundException;
import java.util.List;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.interceptor.Interceptors;
import org.apache.log4j.Logger;
import com.alodiga.http.mlat.integration.connection.MLatRequestManager;
import com.alodiga.services.provider.commons.ejbs.CustomerEJBLocal;
import com.alodiga.services.provider.commons.ejbs.ExternalServicesEJB;
import com.alodiga.services.provider.commons.ejbs.ExternalServicesEJBLocal;
import com.alodiga.services.provider.commons.ejbs.ProductEJBLocal;
import com.alodiga.services.provider.commons.ejbs.TransactionEJBLocal;
import com.alodiga.services.provider.commons.ejbs.TopUpProductEJBLocal;
import com.alodiga.services.provider.commons.genericEJB.AbstractSPEJB;
import com.alodiga.services.provider.commons.genericEJB.AbstractSPEntity;
import com.alodiga.services.provider.commons.genericEJB.EJBRequest;
import com.alodiga.services.provider.commons.utils.EjbConstants;
import com.alodiga.services.provider.commons.genericEJB.SPContextInterceptor;
import com.alodiga.services.provider.commons.genericEJB.SPLoggerInterceptor;
import com.alodiga.services.provider.commons.managers.ContentManager;
import com.alodiga.services.provider.commons.managers.PreferenceManager;
import com.alodiga.services.provider.commons.payment.AuthorizeNet;
import com.alodiga.services.provider.commons.models.billPayment.BillPaymentProduct;
import com.alodiga.services.provider.commons.models.billPayment.BillPaymentResponse;
import com.alodiga.services.provider.commons.models.PaymentPatner;
import com.alodiga.services.provider.commons.responses.PinResponse;
import com.alodiga.services.provider.commons.responses.ResponseAddress;
import com.alodiga.services.provider.commons.responses.ResponseCustomer;
import com.alodiga.services.provider.commons.responses.RechargePinResponse;
import com.alodiga.services.provider.commons.utils.CommonMails;
import com.alodiga.services.provider.commons.utils.Constants;
import com.alodiga.services.provider.commons.utils.CreditCardUtils;
import com.alodiga.services.provider.commons.utils.GeneralUtils;
import com.alodiga.services.provider.commons.utils.Mail;
import com.alodiga.services.provider.commons.utils.QueryConstants;
import com.alodiga.services.provider.commons.utils.QueryParam;
import com.alodiga.services.provider.commons.utils.SendMail;
import com.alodiga.services.provider.commons.utils.ServiceConstans;
import com.alodiga.services.provider.commons.utils.ServiceMailDispatcher;
import com.alodiga.services.provider.commons.utils.ServiceMails;
import com.alodiga.services.provider.commons.utils.ServiceSMSDispatcher;
import com.sg123.exception.EnterpriseNotFoundException;
import com.sg123.exception.PinFreeNotFoundException;
import com.sg123.model.platformprepay.PinStatus;
import com.sg123.model.utils.OrderStatus;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;

@Interceptors({SPLoggerInterceptor.class, SPContextInterceptor.class})
@Stateless(name = EjbConstants.EXTERNAL_SERVICES_EJB, mappedName = EjbConstants.EXTERNAL_SERVICES_EJB)
@TransactionManagement(TransactionManagementType.BEAN)
public class ExternalServicesEJBImp extends AbstractSPEJB implements ExternalServicesEJB, ExternalServicesEJBLocal {

    private static final Logger logger = Logger.getLogger(ExternalServicesEJBImp.class);
    @EJB
    private TransactionEJBLocal transactionEJB;
    @EJB
    private TopUpProductEJBLocal topUpProductEJB;
    @EJB
    private BillPaymentEJBLocal billPaymentEJB;
    @EJB
    private CustomerEJBLocal customerEJB;
    @EJB
    private ProductEJBLocal productEJB;

    private Transaction saveTransaction(Transaction transaction) throws NullParameterException, GeneralException {
        try {
            transaction = (Transaction) saveEntity(transaction);
        } catch (NullParameterException ex) {
            throw (ex);
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName(), "General Exception. trying to saving transaction.."), ex);
        }
        return transaction;
    }

    public Account externalValidateAccount(AccountData accountData) throws NullParameterException, InvalidAccountException, DisabledAccountException, GeneralException {
        boolean isValid = false;
        if (accountData == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "accountData"), null);
        } else if (accountData.getLogin() == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "login"), null);
        } else if (accountData.getPassword() == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "password"), null);
        }
        Account account = null;
        try {
            Query query = null;
            query = createQuery("SELECT a FROM Account a WHERE a.login=?1 AND a.password=?2");
            query.setParameter("1", accountData.getLogin());
            query.setParameter("2", accountData.getPassword());
            account = (Account) query.setHint("toplink.refresh", "true").getSingleResult();

        } catch (NoResultException ex) {
            throw new InvalidAccountException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName(), "Account - Login - Password"), ex);
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName(), "General Exception"), ex);
        }
        if (!account.getEnabled()) {
            throw new DisabledAccountException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName(), "Disabled Account"), null);
        }
        if (account.getAccountHasIpAddress().isEmpty()){
            throw new InvalidAccountException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName(), "Account - Login - Password"), null);
        } else{
            for (AccountHasIpAddress accountHasIpAddress : account.getAccountHasIpAddress()){
                if (accountHasIpAddress.getIpAddress().getIp().equals(accountData.getIpRemoteAddress()))
                    isValid = true;
            }
        }
        if (!isValid)
            throw new InvalidAccountException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName(), "Account - Login - Password"), null);
        return account;
    }

    private void validateCustomerData(Customer customer) throws NullParameterException {
        if (customer == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "customer"), null);
        } else if (customer.getLogin() == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "customer login"), null);
        } else if (customer.getPassword() == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "customer password"), null);
        } else if (customer.getFirstName() == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "customer firstName"), null);
        } else if (customer.getLastName() == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "customer lastName"), null);
        } else if (customer.getAddress() == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "customer address"), null);
        } else if (customer.getAddress().getAddress() == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "customer address"), null);
        } else if (customer.getEmail() == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "customer email"), null);
        }
    }

    private com.sg123.model.contract.Customer fillSisacCustomer(Customer customer, Timestamp nowTimestamp, com.sg123.model.utils.Enterprise sisacEnterprise) {
        com.sg123.model.contract.Customer sisacCustomer = new com.sg123.model.contract.Customer();
        com.sg123.model.utils.Country sisacCountry = new com.sg123.model.utils.Country();
        sisacCustomer.setEnterprise(sisacEnterprise);
        sisacCustomer.setName(customer.getFirstName());
        sisacCustomer.setLastName(customer.getLastName());
        sisacCustomer.setPhone(customer.getPhoneNumber());
        sisacCustomer.setEmail(customer.getEmail());
        sisacCustomer.setCreated(nowTimestamp);
        com.sg123.model.utils.TinType tinType = new com.sg123.model.utils.TinType();
        tinType.setId(com.sg123.model.utils.TinType.GENERADO);
        sisacCustomer.setTinType(tinType);
        sisacCustomer.setTin("1000001");
        sisacCustomer.setBalance(0F);
        com.sg123.model.contract.Address sisacAddress = new com.sg123.model.contract.Address();
        sisacAddress.setAddress1(customer.getAddress().getAddress());
        sisacCountry.setId(customer.getAddress().getCountry().getId());
        sisacAddress.setCountry(sisacCountry);
        sisacCustomer.setAddress(sisacAddress);
        return sisacCustomer;
    }

    private com.sg123.model.WebUser fillSisacWebUser(com.sg123.model.contract.Customer sisacCustomer, Customer customer, Timestamp nowTimestamp, com.sg123.model.utils.Enterprise sisacEnterprise, Long languageId) {
        com.sg123.model.WebUser sisacWebUser = new com.sg123.model.WebUser();
        sisacWebUser = new com.sg123.model.WebUser();

        String login = customer.getLogin().substring(0, 1).equals(String.valueOf(Constants.USA_CODE)) ? customer.getLogin().substring(1, customer.getLogin().length()) : customer.getLogin();
        sisacWebUser.setLogin(login);
        sisacWebUser.setPassword(customer.getPassword());
        sisacWebUser.setConfirmDate(nowTimestamp);
        sisacWebUser.setEnterprise(sisacEnterprise);
        sisacWebUser.setCreationDate(nowTimestamp);
        sisacWebUser.setEnabled(1);
        sisacWebUser.setIsAdmin(0);
        com.sg123.model.Language sisacLanguage = new com.sg123.model.Language();
        sisacLanguage.setId(languageId);
        sisacWebUser.setLanguage(sisacLanguage);
        sisacWebUser.setPrincipalCustomer(sisacCustomer);
        com.sg123.model.utils.SalesChannel salesChannel = new com.sg123.model.utils.SalesChannel();
        salesChannel.setId(com.sg123.model.utils.SalesChannel.SISAC);
        sisacWebUser.setSalesChannel(salesChannel);
        List<com.sg123.model.contract.relation.CustomerHasWebUser> customerWebUsers = new ArrayList<com.sg123.model.contract.relation.CustomerHasWebUser>();
        com.sg123.model.contract.relation.CustomerHasWebUser customerHasWebUser = new com.sg123.model.contract.relation.CustomerHasWebUser();
        customerHasWebUser.setCustomer(sisacCustomer);
        customerHasWebUser.setBeginningDate(nowTimestamp);
        customerHasWebUser.setIsPrincipal(1L);
        customerHasWebUser.setWebUser(sisacWebUser);
        customerHasWebUser.setEndingDate(null);
        customerWebUsers.add(customerHasWebUser);
        sisacWebUser.setCustomerWebUsers(customerWebUsers);
        return sisacWebUser;
    }

    private List<String> externalGetPromotions(Long serial) {
        List<String> promotions = null;
        List<com.sg123.model.promotion.PromotionItem> promotionItems = null;
        try {
            com.sg123.ejb.PromotionsManagementEJB promotionsManagementEJB = (com.sg123.ejb.PromotionsManagementEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PROMOTIONSMANAGEMENTEJB);
            promotionItems = promotionsManagementEJB.getPromotionItemsByPin(serial);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (promotionItems != null && !promotionItems.isEmpty()) {
            promotions = new ArrayList<String>();
            StringBuilder xmlReturn = new StringBuilder();
            //xmlReturn.append("<promotions>");
            for (com.sg123.model.promotion.PromotionItem pi : promotionItems) {
                xmlReturn.append("<promotion>");
                xmlReturn.append("<id>").append(pi.getPromotion().getId()).append("</id>");
                xmlReturn.append("<description>").append(pi.getPromotion().getDescription()).append("</description>");
                xmlReturn.append("<log>").append(pi.getPromotionLog()).append("</log>");
                xmlReturn.append("<promotionalAmount>").append(pi.getPromotionalAmount()).append("</promotionalAmount>");
                xmlReturn.append("</promotion>");
                promotions.add(xmlReturn.toString());
            }
        }
        return promotions;

    }

    public ProvissionPinResponse externalProvissionPin(AccountData userData, Customer customer, String phoneNumber, Float amount, String smsDestination, String externalId) throws NullParameterException, InvalidAccountException, DisabledAccountException, InvalidPhoneNumberException, PinAlreadyExistException, InvalidAmountException,NegativeBalanceException,CreditLimitExcededException, GeneralException {
        Account account = externalValidateAccount(userData);
        TransactionStatus transactionStatus = null;
        TransactionType transactionType = null;
        if (phoneNumber == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "phoneNumber"), null);
        } else if (amount == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "amount"), null);
        }
        //Valida que sea un numero valido para USA.
        if (!GeneralUtils.isValidUSAPhoneNumber(phoneNumber)) {
            throw new InvalidPhoneNumberException("Invalid phoneNumber: " + phoneNumber);
        }
        if (amount < 0) {
            throw new InvalidAmountException("Invalid amount for Pin Purchase: " + amount);

        }
        String login = customer.getLogin();
        try {
            customer = customerEJB.loadCustomerByLogin(login);
        } catch (RegisterNotFoundException ex) {

            customer = new Customer();
            customer.setLogin(login);
            customer.setAddress(account.getAddress());
            String password1 = GeneralUtils.getRamdomNumber(4);
            customer.setPassword(password1);
            customer.setCreationDate(new Timestamp(new Date().getTime()));
            customer.setPhoneNumber(phoneNumber);
            customer.setFirstName("Sin especificar");
            customer.setLastName("Sin especificar");
            customer.setEmail(customer.getEmail() != null ? customer.getEmail() : "Sin especificar");
            customer.setGender(null);
            customer.setBirthDate(new Timestamp(new java.util.Date().getTime()));
            Enterprise enterprise = new Enterprise();
            enterprise.setId(Enterprise.ALODIGA_USA);
            Currency currency = new Currency(Currency.DOLLAR);
            enterprise.setCurrency(currency);
            customer.setEnterprise(enterprise);
            customer.setEnabled(true);


            EJBRequest req = new EJBRequest(customer);
            customerEJB.saveCustomer(req);
        }

        Transaction transaction = null;
        /*Invocación EJB's Remotos de SisacEJB*/
        com.sg123.ejb.ContractEJB contractEJB = (com.sg123.ejb.ContractEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CONTRACT_EJB);
        com.sg123.ejb.UserEJB sisacUserEJB = (com.sg123.ejb.UserEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.USER_EJB);
        com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);
        com.interax.telephony.service.remote.callingcard.CallingCardPinEJB callingCardPinEJB = (com.interax.telephony.service.remote.callingcard.CallingCardPinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CALLING_CARD_PIN_EJB);
        ProvissionPinResponse pinResponse = null;

        try {
            //Validamos que el phoneNumber no este asignado a otro Pin Electrónico.
            pinEJB.getPinFreeByAni(Long.parseLong(phoneNumber));
            throw new PinAlreadyExistException("PhoneNumber: " + phoneNumber + " is already assigned to another pin.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        String transactionData = "Compra Pin Electronico [monto)" + amount + "(monto] - [ani)" + phoneNumber + "(ani]" + "[cuenta login)" + userData.getLogin() + "(cuenta login]";
        com.sg123.model.WebUser sisacWebUser = null;
        com.sg123.model.utils.Enterprise sisacEnterprise = new com.sg123.model.utils.Enterprise();
        Date now = new Date((new java.util.Date()).getTime());
        Timestamp nowTimestamp = new Timestamp(now.getTime());
        sisacEnterprise.setId(com.sg123.model.utils.Enterprise.WIXTEL_USA);

        try {
            sisacWebUser = sisacUserEJB.getWebUserByLogin(customer.getLogin(), sisacEnterprise.getId());
            //throw new GeneralException("Web user already exists. Login = " + customer.getLogin());
        } catch (com.sg123.exception.WebUserNotFoundException ex) {
            com.sg123.model.contract.Customer sisacCustomer = this.fillSisacCustomer(customer, nowTimestamp, sisacEnterprise);
            sisacWebUser = this.fillSisacWebUser(sisacCustomer, customer, nowTimestamp, sisacEnterprise, account.getLanguage().getId());
            /* Registro de webUser y customer en SISAC*/
            try {
                sisacWebUser = sisacUserEJB.registerAndConfirm(sisacWebUser, sisacEnterprise.getId());
            } catch (Exception ex1) {
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex1);
            }
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
        }
        // Vas contra el saldo del cliente
        float realAmount = 0f;
        try {
            Product product = productEJB.loadProductById(Product.ELECTRONIC_PIN_ID);
            realAmount = transactionEJB.getRealValueByProdut(account, amount, product,null);
            System.out.println("realAmount......." + realAmount);
            transaction = new Transaction(amount, realAmount, account);
            ValidateBalance(account, amount);
        } catch (NegativeBalanceException ex) {
            throw ex;
        } catch (CreditLimitExcededException ex) {
            throw ex;
        }  catch (GeneralException ex) {
            throw ex;
        } catch (Exception ex) {
             throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
        }
        try {
            Map pinData = contractEJB.provisionDistributionPinPurchase(account.getCustumerServiceIdSisac(), sisacWebUser.getId(), amount, transactionData);
            com.sg123.model.platformprepay.Pin pin = (com.sg123.model.platformprepay.Pin) pinData.get("PIN");
            com.sg123.model.ecommerce.Order order = (com.sg123.model.ecommerce.Order) pinData.get("ORDER");
            //Se validan las promociones de SISAC.
            List<String> promotions = this.externalGetPromotions(pin.getSerial());
            Long[] anis = new Long[1];
            anis[0] = Long.parseLong(phoneNumber);
            pinEJB.savePinFree(pin.getSerial(), anis);
            List<String> accessNumbers = new ArrayList<String>();
            try {
                accessNumbers = callingCardPinEJB.getAccessNumbersByAni(account.getCustumerServiceIdSisac(), phoneNumber, 1L);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            pinResponse = new ProvissionPinResponse(pin.getSerial().toString(), pin.getSecret().toString(), order.getId().toString(), "Transaccion exitosa", promotions, accessNumbers);
            transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_PURCHASE);
            transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.PROCESSED);
            transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, transactionData, account, pin.getSerial(), null, null, transactionData, externalId, null, order.getId().toString(), null, null, null, realAmount,userData.getIpRemoteAddress());

        } catch (Exception ex1) {
            ex1.printStackTrace();
            try {
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, ex1.getMessage(), account, null, null, null, transactionData, externalId, null, null, null, null, null, 0f,userData.getIpRemoteAddress());
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex1);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
        } finally {
            try {
                this._saveTransaction(transaction);
            } catch (NegativeBalanceException ex) {
                throw ex;
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new GeneralException("Exception trying saving transaction in method provissionPin. " + ex.getMessage());
            }
        }
        return pinResponse;
    }

    public RechargePinResponse externalRechargePin(AccountData userData, String phoneNumber, String serial, Float amount, String smsDestination, String externalId) throws NullParameterException, InvalidAccountException, DisabledAccountException, InvalidPhoneNumberException, InvalidAmountException, DisabledPinException,NegativeBalanceException,CreditLimitExcededException,GeneralException {
        Account account = externalValidateAccount(userData);
        TransactionStatus transactionStatus = null;
        TransactionType transactionType = null;
        if (phoneNumber == null) {
            throw new NullParameterException("Parameter phoneNumber cannot be null");
        } else if (serial == null) {
            throw new NullParameterException("Parameter serial cannot be null");
        } else if (amount == null) {
            throw new NullParameterException("Parameter amount cannot be null");
        }
        //Valida que sea un numero valido para USA.
        if (!GeneralUtils.isValidUSAPhoneNumber(phoneNumber)) {
            throw new InvalidPhoneNumberException("Invalid phoneNumber: " + phoneNumber);
        }
        if (amount <= 0) {
            throw new InvalidAmountException("Invalid amount for Pin Purchase: " + amount);

        }
        Transaction transaction = null;
        RechargePinResponse rechargePinResponse = null;

        com.sg123.ejb.ContractEJB contractEJB = (com.sg123.ejb.ContractEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CONTRACT_EJB);
        com.sg123.ejb.UserEJB userEJB = (com.sg123.ejb.UserEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.USER_EJB);
        com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);
        com.sg123.ejb.ContentEJB contentEJB = (com.sg123.ejb.ContentEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CONTENT_EJB);
        com.sg123.ecommerce.ShoppingCart shoppingCart = new com.sg123.ecommerce.ShoppingCart();
        Date now = new Date((new java.util.Date()).getTime());
        Timestamp nowTimestamp = new Timestamp(now.getTime());
        com.sg123.serviceconfig.RechargeConfig rechargeConfig = new com.sg123.serviceconfig.RechargeConfig();

        com.sg123.model.payment.PaymentInfo paymentInfo = null;
        float newBalance = 0f;
        com.sg123.model.ecommerce.Order order = null;
        String transactionData = "Recarga Pin Electronico [monto)" + amount + "(monto] - [serial)" + serial + "(serial]" + "[cuenta - login )" + userData.getLogin() + "(cuenta - login ]";
        String description = null;
        com.sg123.model.platformprepay.Pin pin = null;
        try {
            com.sg123.model.ecommerce.CustomService customService = null;
            //com.sg123.model.WebUser webUser = null;
            com.sg123.model.content.Segment segment = null;

            try {
                customService = contentEJB.loadCustomService(account.getCustumerServiceIdSisac());
            } catch (com.sg123.exception.CustomServiceNotFoundException e) {
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), e);
            }

            segment = customService.getService().getSegment();
            //webUser = userEJB.loadWebUserById(WSConstants.WEB_USER_ID_SISAC);
            try {
                pin = pinEJB.loadPin(Long.parseLong(serial));
                newBalance = pin.getCurrentBalance() + amount;

            } catch (com.sg123.exception.PinNotFoundException e) {
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), e);
            }

            shoppingCart.setWebUser(pin.getWebUser());
            shoppingCart.setSegmentId(segment.getId());
            shoppingCart.setLastUpdate(now);
            rechargeConfig.setPinSerial(pin.getSerial());
            rechargeConfig.setAmount(amount);
            rechargeConfig.setTaxIncluded(customService.isTaxInclude() ? 1 : 0);
            com.sg123.model.ecommerce.Item item = new com.sg123.model.ecommerce.Item(rechargeConfig, 1L, amount);
            item.setImage(null);
            item.setCustomService(null);
            item.setName(transactionData);
            item.setTaxAmount(0F);
            // TODO PREGUNTAR
            item.setUnitAmount(0F);
            item.setTotalAmount(0F);

            description = "Pin serial: " + pin.getSerial();
            description += "Recharge" + " Amount: " + amount + customService.getCurrency().getSymbol();
            item.setDescription(description);
            shoppingCart.addItem(item);
            shoppingCart.setPaymentInfo(paymentInfo);

            order = contractEJB.processShoppingCart(shoppingCart);
            order.setSubtotal(0F);
            order.setTaxTotal(0F);
            order.setTotal(0F);
            com.sg123.model.WebUserSession webUserSession = userEJB.loadWebUserSessionByWebUser(pin.getWebUser().getId());
            userEJB.updateWebUserSessionOrder(webUserSession.getId(), order);
        } catch (Exception ex1) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ""), ex1);
        }
        // Vas contra el saldo del cliente
        float realAmount = 0f;
        try {
            Product product = productEJB.loadProductById(Product.ELECTRONIC_PIN_ID);
            realAmount = transactionEJB.getRealValueByProdut(account, amount, product,null);
            System.out.println("realAmount......." + realAmount);
            transaction = new Transaction(amount, realAmount, account);
            ValidateBalance(account, amount);
        } catch (NegativeBalanceException ex) {
            throw ex;
        } catch (CreditLimitExcededException ex) {
            throw ex;
        } catch (GeneralException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
        }
        try {
            order = contractEJB.rechargeDistributionPinPurchase(account.getCustumerServiceIdSisac(), pin.getWebUser().getId(), order);
            transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_RECHARGE);
            transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.PROCESSED);
            transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, description, account, pin.getSerial(), null, null, transactionData, externalId, null, order.getId().toString(), null, null, null,realAmount,userData.getIpRemoteAddress());
            rechargePinResponse = new RechargePinResponse(pin.getSerial().toString(), pin.getSecret(), "Transaccion Exitosa", String.valueOf(newBalance), String.valueOf(order.getId()));
        } catch (Exception ex) {
            try {
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_RECHARGE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, ex.getMessage(), account, null, null, null, "Error trying recharging pin. Number = " + phoneNumber, externalId, null, null, null, null, null, 0f,userData.getIpRemoteAddress());
                ex.printStackTrace();
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), ex);
            } catch (Exception ex1) {
            }
        } finally {
            try {
                this._saveTransaction(transaction);
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new GeneralException("Exception trying saving transaction in method externalRechargePin. " + ex.getMessage());
            }
        }
        return rechargePinResponse;
    }

    private Recharge externalProcessPayment(Account account, PaymentInfo paymentInfo, float amount) throws NullParameterException, InvalidAmountException, GeneralException, PaymentServiceUnavailableException, PaymentDeclinedException, InvalidPaymentInfoException, InvalidCreditCardDateException {

        if (paymentInfo == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "paymentInfo"), null);
        } else if (account == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "account"), null);
        } else if (amount <= 0) {
            throw new InvalidAmountException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), QueryConstants.PARAM_ENDING_DATE), null);
        } else if (!CreditCardUtils.validateCreditCardDate(paymentInfo.getCreditCardDate())) {
            throw new InvalidCreditCardDateException("Invalid creditcard date.");
        }
        if (paymentInfo.getPaymentType().getId().equals(PaymentType.CREDIT_CARD)) {
            if (!paymentInfo.getCreditcardType().getId().equals(CreditCardUtils.getCardID(paymentInfo.getCreditCardNumber())) || !CreditCardUtils.validate(paymentInfo.getCreditCardNumber())) {
                logger.error("Invalid credit card number");
                throw new InvalidPaymentInfoException("Invalid credit card number");
            }
        } else {
            throw new InvalidPaymentInfoException(""
                    + " paymentType");
        }
        try {
            AuthorizeNet authorizeNet = new AuthorizeNet();
            Recharge recharge = authorizeNet.processTransaction(paymentInfo, account, amount, entityManager, logger);
            EntityTransaction eTransaction = entityManager.getTransaction();
            try {
                eTransaction.begin();
                entityManagerWrapper.save(recharge);
                eTransaction.commit();
            } catch (Exception e) {
                try {
                    if (eTransaction.isActive()) {
                        eTransaction.rollback();
                    }
                } catch (IllegalStateException e1) {
                    throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), "processPayment", e.getMessage()), e);
                } catch (SecurityException e1) {
                    throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), "processPayment", e.getMessage()), e);
                }

            }
            if (recharge.getStatus().getId().equals(RechargeStatus.RECHAZADO)) {
                throw new PaymentDeclinedException("Payment was declined for Authorize.Net. Response=" + recharge.getHumanReadableResponseCode());
            }

            return recharge;
        } catch (PaymentServiceUnavailableException ex) {
            throw new PaymentServiceUnavailableException(ex.getMessage());
        } catch (PaymentDeclinedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), null, ex.getMessage()), ex);
        }
    }

    public SearchPinResponse externalSearchPin(AccountData userData, String phoneNumber, String serial) throws NullParameterException, InvalidAccountException, DisabledAccountException, InvalidPhoneNumberException, RegisterNotFoundException, GeneralException, PinDisabledException, PinFreeNotfoundException {
        externalValidateAccount(userData);
        if (serial == null && phoneNumber == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "serial - phoneNumber - customerLogin"), null);
        }
        SearchPinResponse searchPinResponse = new SearchPinResponse();
        com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);

        try {
            com.sg123.model.platformprepay.Pin pin = null;

            if (serial != null) {
                pin = pinEJB.loadPin(new Long(serial));
            } else {//phoneNumber
                pin = pinEJB.getPinFreeByAni(new Long(phoneNumber)).getPin();
                pin = pinEJB.loadPin(pin.getSerial());//Hago el cambio aqui para no hacerlo en SisacEJB - Issue: LAZY Relationship
            }
            searchPinResponse.setEnabled(pin.getPinStatus().getId().equals(PinStatus.PIN_AVAILABLE_STATE) ? true : false);
            com.sg123.model.contract.Customer customer = pin.getWebUser().getPrincipalCustomer();
            ResponseCustomer rCustomer = new ResponseCustomer();
            rCustomer.setLogin(pin.getWebUser().getLogin());
            rCustomer.setPassword(pin.getWebUser().getPassword());
            rCustomer.setEnterprideId(pin.getWebUser().getEnterprise().getId().toString());

            if (customer.getName() != null) {
                rCustomer.setFirstName(customer.getName());
            }/*

             */
            if (customer.getLastName() != null) {
                rCustomer.setLastName(customer.getLastName());
            }
            if (customer.getEmail() != null) {
                rCustomer.setEmail(customer.getEmail());
            }
            if (customer.getPhone() != null) {
                rCustomer.setPhoneNumber(customer.getPhone());
            }
            if (customer.getBirthDate() != null) {
                rCustomer.setBirthDate(customer.getBirthDate());
            }
            if (customer.getCivilState() != null) {
                rCustomer.setCivilState(customer.getCivilState());
            }
            if (customer.getGender() != null) {
                rCustomer.setGender(customer.getGender());
            }
            ResponseAddress rAddrees = new ResponseAddress();
            if (customer.getAddress().getCountry() != null) {
                rAddrees.setCountryId(customer.getAddress().getCountry().getId().toString());
            }
            if (customer.getAddress().getState() != null) {
                rAddrees.setStateId(customer.getAddress().getState().getId().toString());
            }
            if (customer.getAddress().getCity() != null) {
                rAddrees.setCityId(customer.getAddress().getCity().getId().toString());
            }
            if (customer.getAddress().getCounty() != null) {
                rAddrees.setCountyId(customer.getAddress().getCounty().getId().toString());
            }
            if (customer.getAddress().getAddress1() != null) {
                rAddrees.setAddress1(customer.getAddress().getAddress1());
            }
            if (customer.getAddress().getZipCode() != null) {
                rAddrees.setZipCode(customer.getAddress().getZipCode());
            }
            if (customer.getAddress().getStateName() != null) {
                rAddrees.setStateName(customer.getAddress().getStateName());
            }
            if (customer.getAddress().getCountyName() != null) {
                rAddrees.setCountyName(customer.getAddress().getCountyName());
            }
            if (customer.getAddress().getCityName() != null) {
                rAddrees.setCityName(customer.getAddress().getCityName());
            }
            if (customer.getBalance() != null) {
                rCustomer.setBalance(pin.getCurrentBalance().toString());
            }
            rCustomer.setAddress(rAddrees);
            rCustomer.setPin(pin.getSerial().toString());

            List<PinResponse> rPinFrees = new ArrayList<PinResponse>();

            for (com.sg123.model.platformprepay.PinFree pinFree : pin.getPinsFree()) {
                PinResponse pinResponse = new PinResponse();
                pinResponse.setCodeId(pinFree.getCode().getId().toString());
                pinResponse.setSerial(pinFree.getPin().getSerial().toString());
                pinResponse.setAni(pinFree.getAni().toString());
                pinResponse.setSecret(pin.getSecret());
                pinResponse.setBalance(pin.getCurrentBalance());
                rPinFrees.add(pinResponse);
                if (pin.getPinStatus().equals(PinStatus.PIN_UNAVAILABLE_STATE)) {
                    throw new PinDisabledException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), null);
                }
            }
            rCustomer.setPinFrees(rPinFrees);
            searchPinResponse.setCustomer(rCustomer);
        } catch (PinNotFoundException e) {
            e.printStackTrace();
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), null);
        } catch (PinFreeNotFoundException e) {
            e.printStackTrace();
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), null);

        } catch (Exception e) {
            e.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), null);
        }
        return searchPinResponse;

    }

    public CancelPinProvissionResponse externalCancelPinProvission(AccountData userData, Long transactionId, Long rechargeId) throws NullParameterException, InvalidAccountException, DisabledAccountException, RegisterNotFoundException, AmountConsumedException, GeneralException {
        externalValidateAccount(userData);
        Transaction transaction = null;
        Recharge recharge = null;
        Float amount = 0f;
        if (userData == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "userData"), null);
        } else if (transactionId == null && rechargeId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "transactionId && rechargeId"), null);
        }

        CancelPinProvissionResponse cancelPinProvissionResponse = null;
        try {
            if (transactionId != null) {
                System.out.println("TransactionId no null");
                transaction = transactionEJB.loadTransactionById(transactionId);
                amount = transaction.getTotalAmount();
            }
            if (transaction != null) {
                System.out.println("Transaction no null");
                Long serial = transaction.getPinSerial();
                com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);
                com.sg123.ejb.ContractEJB contractEJB = (com.sg123.ejb.ContractEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CONTRACT_EJB);
                com.sg123.model.platformprepay.Pin pin = pinEJB.loadPin(serial);
                List<com.sg123.model.platformprepay.PinFree> pFrees = pin.getPinsFree();
                Long orderId = Long.parseLong(transaction.getReferenceProviderCode());
                for (com.sg123.model.platformprepay.PinFree pfree : pFrees) {
                    if (!contractEJB.authorizeCancelDistributionOrder(orderId)) {
                        throw new InvalidAmountException("Amount consumed");
                    }
                    externalDeletePinFree(userData, pfree.getAni().toString());
                }
                pinEJB.deletePin(serial);
                contractEJB.changeOrderStatus(orderId, OrderStatus.DEVUELTA);
                cancelPinProvissionResponse = new CancelPinProvissionResponse(Constants.RESPONSE_SUCCESS);
//                TransactionStatus transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.CANCELED);
//                transaction.setTransactionStatus(transactionStatus);
                List persistedObject = new ArrayList();
                persistedObject.addAll(saveCancelTransaction(transaction));
                persistListObject(persistedObject);


                Account account = transaction.getAccount();
                System.out.println("--------------------Saldo antes:" + account.getBalance());
                account.setBalance(account.getBalance() + amount);
                System.out.println("--------------------Saldo despues:" + account.getBalance());
                entityManager.getTransaction().begin();
                account = entityManager.merge(account);
                entityManager.getTransaction().commit();
                System.out.println("--------------------Saldo despues del merge:" + account.getBalance());


//                try {
//                    this.saveTransaction(transaction);
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                    throw new GeneralException("Exception trying saving transaction in method cancelPinProvission. " + ex.getMessage());
//                }
            }
        } catch (InvalidAmountException e) {
            throw new AmountConsumedException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        } catch (com.sg123.exception.NullParameterException ex) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "cancelPinProvission"), null);
        } catch (PinNotFoundException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), null), ex);
        } catch (com.sg123.exception.GeneralException ex) {
            ex.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        return cancelPinProvissionResponse;
    }

    public CancelPinRechargeResponse externalCancelPinRecharge(AccountData userData, Long transactionId, Long rechargeId) throws NullParameterException, InvalidAccountException, DisabledAccountException, RegisterNotFoundException, AmountConsumedException, GeneralException {
        externalValidateAccount(userData);
        CancelPinRechargeResponse cancelPinRechargeResponse = null;
        Transaction transaction = null;
        Recharge recharge = null;
        Float amount = 0f;
        if (userData == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "userData"), null);
        } else if (transactionId == null && rechargeId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "transactionId && rechargeId"), null);
        }
        com.sg123.ejb.ContractEJB contractEJB = (com.sg123.ejb.ContractEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CONTRACT_EJB);
        if (transactionId != null) {
            transaction = transactionEJB.loadTransactionById(transactionId);
            amount = transaction.getTotalRealValue();
        }
        if (transaction != null) {
                Long orderId = Long.parseLong(transaction.getReferenceProviderCode());
                try {
                    if (!contractEJB.authorizeCancelDistributionOrder(orderId)) {
                        throw new InvalidAmountException("Amount consumed");
                    }
                    
                    contractEJB.cancelOrder(orderId);
                    cancelPinRechargeResponse = new CancelPinRechargeResponse(Constants.RESPONSE_SUCCESS);
                    List persistedObject = new ArrayList();
                    persistedObject.addAll(saveCancelTransaction(transaction));
                    persistListObject(persistedObject);

                    Account account = transaction.getAccount();
                    System.out.println("--------------------Saldo antes:"+account.getBalance());
                    account.setBalance(account.getBalance() + amount);
                    System.out.println("--------------------Saldo despues:"+account.getBalance());
                    entityManager.getTransaction().begin();
                    account = entityManager.merge(account);
                    entityManager.getTransaction().commit();
                    System.out.println("--------------------Saldo despues del merge:"+account.getBalance());
//                    TransactionStatus transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.CANCELED);
//                    transaction.setTransactionStatus(transactionStatus);
//                    try {
//                        this.saveTransaction(transaction);
//                    } catch (Exception ex) {
//                        ex.printStackTrace();
//                        throw new GeneralException("Exception trying saving transaction in method cancelPinRecharge. " + ex.getMessage());
//                    }
                     
                } catch (InvalidAmountException e) {
                    e.printStackTrace();
                    throw new AmountConsumedException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
                } catch (EmptyListException ex) {
                 ex.printStackTrace();
                  throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
                }catch (Exception e) {
                    e.printStackTrace();
                    throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
                }
           
        }
        return cancelPinRechargeResponse;
    }

     private List saveCancelTransaction(Transaction t) throws GeneralException, EmptyListException, NullParameterException {
        System.out.println("---------------saveCancelTransaction-----------");
        List persistedObject = new ArrayList();
        BalanceHistory balHistory = null;
        Transaction reverseTransaction = new Transaction();
        List<BalanceHistory> bHistories = new ArrayList<BalanceHistory>();
        try {
            balHistory = createBalanceHistory(t.getAccount(), t.getTotalRealValue(), 2, false);
            reverseTransaction.setAccount(t.getAccount());
            reverseTransaction.setTotalAmount(t.getTotalAmount());
            reverseTransaction.setTotalRealValue(t.getTotalRealValue());
            reverseTransaction.setCreationDate(new Timestamp(Calendar.getInstance().getTimeInMillis()));
            reverseTransaction.setPaymentInfo(t.getPaymentInfo());
            reverseTransaction.setPinSerial(t.getPinSerial());
            TransactionStatus transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.PROCESSED);
            reverseTransaction.setTransactionStatus(transactionStatus);
            TransactionType tType = new TransactionType();
            tType.setId(TransactionType.REVERSE_TRANSACTION);
            reverseTransaction.setTransactionType(tType);
            reverseTransaction.setTotalTax(0.00f);
            reverseTransaction.setExternalID("" + t.getId());
            balHistory.setTransaction(reverseTransaction);
            bHistories.add(balHistory);
            reverseTransaction.setBalanceHistories(bHistories);
            persistedObject.add(reverseTransaction);
        } catch (RegisterNotFoundException e1) {
            e1.printStackTrace();
        } catch (NegativeBalanceException e1) {
            e1.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return persistedObject;
    }

    public List<Country> externalGetTopUpCountries(AccountData userData) throws NullParameterException, InvalidAccountException, DisabledAccountException, EmptyListException, GeneralException {
        externalValidateAccount(userData);
        List<Country> countries = new ArrayList<Country>();
        String sql = "SELECT DISTINCT tpp.mobileOperator.country FROM TopUpProduct tpp WHERE tpp.enabled = TRUE AND tpp.mobileOperator.enabled = TRUE ORDER BY tpp.mobileOperator.country.name";
        Query query = null;
        try {
            query = createQuery(sql);
            countries = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        if (countries.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return countries;
    }

    public List<MobileOperator> externalGetMobileOperatorsByCountryId(AccountData userData, Long countryId) throws NullParameterException, InvalidAccountException, DisabledAccountException, DisabledPinException, EmptyListException, GeneralException {
        externalValidateAccount(userData);
        if (countryId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "countryId"), null);
        }
        List<MobileOperator> operators = new ArrayList<MobileOperator>();
        try {

            //String sql = "SELECT m FROM MobileOperator m WHERE m.enabled = TRUE AND m.country.id = ?1 ORDER BY m.name";
            String sql = "SELECT DISTINCT tp.mobileOperator FROM TopUpProduct tp WHERE tp.enabled = TRUE AND tp.mobileOperator.enabled = TRUE AND tp.mobileOperator.country.id = ?1 ORDER BY tp.mobileOperator.name";
            Query query = createQuery(sql);

            query.setParameter("1", countryId);
            operators = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        if (operators.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return operators;
    }

    public List<TopUpProduct> externalGetTopUpProductByMobileOperatorId(AccountData userData, Long mobileOperatorId) throws NullParameterException, InvalidAccountException, DisabledAccountException, EmptyListException, GeneralException {
        Account account = externalValidateAccount(userData);
        if (mobileOperatorId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "mobileOperatorId"), null);
        }
        float commission = 0f;
        List<TopUpProduct> topUpProducts = new ArrayList<TopUpProduct>();
        try {
            Query query = createQuery("SELECT tup1 FROM TopUpProduct tup1 WHERE tup1.mobileOperator.id = ?1 AND tup1.enabled = 1 AND tup1.commissionPercent = (SELECT MAX(tup2.commissionPercent) FROM TopUpProduct tup2 WHERE tup2.mobileOperator.id = ?1 AND tup2.enabled = 1  AND tup1.productDenomination.id = tup2.productDenomination.id) ORDER BY tup1.productDenomination.amount");
            query.setParameter("1", mobileOperatorId);
            topUpProducts = query.setHint("toplink.refresh", "true").getResultList();
            commission = topUpProductEJB.getTopUpCalculationByAccountId(account.getId());
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }

        if (topUpProducts.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        for (TopUpProduct topUp : topUpProducts) {
            topUp.setCommissionPercent(topUp.getCommissionPercent() * (commission / 100));
        }
        topUpProducts.size();
        return topUpProducts;
    }

    public GeneralTopUpResponse externalProcessTopUp(AccountData userData, TopUpProduct topUpProduct, String destinationNumber, String externalId, String senderNumber, Customer customer, Long languageId, boolean sendSMS, String destinationSMS) throws NullParameterException, InvalidAccountException, DisabledAccountException, TopUpTransactionException, GeneralException, TopUpTransactionException, GeneralException, TopUpProductNotAvailableException, InvalidFormatException, NotAvaliableServiceException, DestinationNotPrepaidException, DuplicatedTransactionException, CarrierSystemUnavailableException, InvalidPhoneNumberException, InvalidAmountException, MaxNumberTransactionDailyPerDestinationException, MaxNumberTransactionDailyPerSenderException,NegativeBalanceException,CreditLimitExcededException{
        Account account = externalValidateAccount(userData);
        float numberOfTransaction = 0f;
        GeneralTopUpResponse response = new GeneralTopUpResponse();
        Transaction transaction = null;
        Date now = new Date((new java.util.Date()).getTime());
        StringBuilder transactionData = new StringBuilder();
        Timestamp nowTimestamp = new Timestamp(now.getTime());
          // Vas contra el saldo del cliente
        float realAmount = 0f;
        try {
            Product product = productEJB.loadProductById(Product.TOP_UP_PRODUCT_ID);
            realAmount = transactionEJB.getRealValueByProdut(account, topUpProduct.getProductDenomination().getAmount(), product,topUpProduct);
            System.out.println("realAmount......." + realAmount);
            transaction = new Transaction(topUpProduct.getProductDenomination().getAmount(), realAmount, account);
            ValidateBalance(account, realAmount);
        } catch (NegativeBalanceException ex) {
            throw ex;
        } catch (CreditLimitExcededException ex) {
            throw ex;
        } catch (GeneralException ex) {
            throw ex;
        } catch (Exception ex) {
             throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
        }
        try {
            validarDestinationNumberAndSenderNumber(account,destinationNumber, senderNumber);
            response = topUpProductEJB.executeTopUp(topUpProduct, destinationNumber, externalId, senderNumber, account, languageId, sendSMS, destinationSMS);
            transactionData.append("TopUp: ").append(topUpProduct.getName()).append("( ").append(topUpProduct.getId()).append(" )").append(" Operadora : ").append(topUpProduct.getMobileOperator().getName()).append(" Denominacion: ").append(topUpProduct.getProductDenomination().getAmount()).append("PhoneNumber: ").append(destinationNumber);
            transaction = new Transaction(new TransactionType(TransactionType.TOP_UP_PURCHASE), new TransactionStatus(TransactionStatus.PROCESSED), topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, transactionData.toString(), account, null, topUpProduct, null, transactionData.toString(), externalId, null, "", destinationNumber, senderNumber, null,realAmount,userData.getIpRemoteAddress());
        } catch (MaxNumberTransactionDailyPerDestinationException ex) {
            transaction = new Transaction(new TransactionType(TransactionType.TOP_UP_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, null, account, null, topUpProduct, null, null, externalId, null, "", null, null, null,0f,userData.getIpRemoteAddress());
            throw ex;
        } catch (MaxNumberTransactionDailyPerSenderException ex) {
            transaction = new Transaction(new TransactionType(TransactionType.TOP_UP_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, null, account, null, topUpProduct, null, null, externalId, null, "", null, null, null,0f,userData.getIpRemoteAddress());
            throw ex;
        } catch (TopUpTransactionException tupEx) {
            tupEx.printStackTrace();
            transactionData.append("TopUp: ").append(topUpProduct.getName()).append("( ").append(topUpProduct.getId()).append(" )").append(" Operadora : ").append(topUpProduct.getMobileOperator().getName()).append(" Denominacion: ").append(topUpProduct.getProductDenomination().getAmount()).append("PhoneNumber: ").append(destinationNumber);
            response = topUpProductEJB.executeBackUpTopUp(topUpProduct, destinationNumber, externalId, senderNumber, account, languageId, sendSMS, destinationSMS);
            transaction = new Transaction(new TransactionType(TransactionType.TOP_UP_PURCHASE), new TransactionStatus(TransactionStatus.PROCESSED), topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, transactionData.toString(), account, null, topUpProduct, null, transactionData.toString(), externalId, null, "", destinationNumber, senderNumber, null,0f,userData.getIpRemoteAddress());
        } catch (Exception ex) {
            transaction = new Transaction(new TransactionType(TransactionType.TOP_UP_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, null, account, null, topUpProduct, null, null, externalId, null, "", destinationNumber, senderNumber, null,0f,userData.getIpRemoteAddress());
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
        } finally {
            try {
                this._saveTransaction(transaction);
            } catch (Exception ex) {
                throw new GeneralException("Exception trying saving transaction in method provissionPin. " + ex.getMessage());
            }
            if (transaction.getTransactionStatus().getId().equals(1L)) {
                numberOfTransaction = this.numberOfTransaction(account.getId(), topUpProduct.getId());
                System.out.println("numberOfTransaction++++++" + numberOfTransaction);
                if (numberOfTransaction % Constants.MAX_NUMBER_TRANSACTION == 0) {
                    ServiceMailDispatcher.sendAlertMail(account, topUpProduct, numberOfTransaction);
                    ServiceSMSDispatcher.sendAlertTopUpSMS(account, topUpProduct, numberOfTransaction);
                }
            }
        }
        return response;

    }

    private float numberOfTransaction(Long accountId, Long topUpId){
        float number = 0;
        Date nowBeginning = GeneralUtils.getBeginningDate(new Date((new java.util.Date()).getTime()));
        Date nowEnding = GeneralUtils.getEndingDate(new Date((new java.util.Date()).getTime()));
        Timestamp nowBeginningDate = new Timestamp(nowBeginning.getTime());
        Timestamp nowEndingDate = new Timestamp(nowEnding.getTime());
        String sql = "SELECT COUNT(t.id) FROM services.transaction t WHERE t.accountId =" + accountId + " AND t.topUpId= "+ topUpId +" AND t.transactionTypeId=3 AND t.transactionstatusId=1 AND t.creationDate BETWEEN DATE('" + nowBeginningDate + "') AND ('" + nowEndingDate + "')";
        try {
            List result = new ArrayList();
            result = (List) entityManager.createNativeQuery(sql).getSingleResult();
            number = (Long) result.get(0);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return number;
    }

    private void validarDestinationNumberAndSenderNumber(Account account,String destinationNumber, String senderNumber) throws MaxNumberTransactionDailyPerDestinationException, MaxNumberTransactionDailyPerSenderException {
        Long numberTransaccionPerDestinationNumber = 0L;
        Long numberTransaccionPerSenderNumber = 0L;
        Date nowBeginning = GeneralUtils.getBeginningDate(new Date((new java.util.Date()).getTime()));
        Date nowEnding = GeneralUtils.getEndingDate(new Date((new java.util.Date()).getTime()));
        Timestamp nowBeginningDate = new Timestamp(nowBeginning.getTime());
        Timestamp nowEndingDate = new Timestamp(nowEnding.getTime());
        String sql = "SELECT COUNT(t.id) FROM services.transaction t WHERE t.topUpDestination =" + destinationNumber + " AND t.transactionTypeId=3 AND t.transactionstatusId=1 AND t.creationDate BETWEEN DATE('" + nowBeginningDate + "') AND ('" + nowEndingDate + "')";
        List result = new ArrayList();
        result = (List) entityManager.createNativeQuery(sql).getSingleResult();
        numberTransaccionPerDestinationNumber = (Long) result.get(0);
         Float numTopUp = 2f;
        try {
             numTopUp = transactionEJB.getlimitTopUpByAccount(account.getId());
        } catch (NullParameterException ex) {
        } catch (GeneralException ex) {
            numTopUp = 2f;
        }
        System.out.println("cantidad por destino:"+numTopUp);
        if (numberTransaccionPerDestinationNumber >= numTopUp) {
            throw new MaxNumberTransactionDailyPerDestinationException("MaxNumberTransactionDailyPerDestinationException");
        }
        sql = "SELECT COUNT(t.id) FROM services.transaction t WHERE t.topUpSender =" + senderNumber + " AND t.transactionTypeId=3 AND t.transactionstatusId=1 AND t.creationDate BETWEEN DATE('" + nowBeginningDate + "') AND ('" + nowEndingDate + "')";
        result = new ArrayList();
        result = (List) entityManager.createNativeQuery(sql).getSingleResult();
        numberTransaccionPerSenderNumber = (Long) result.get(0);
        //numberTransaccionPerSenderNumber = (Long) entityManager.createNativeQuery(sql).getSingleResult();
        if (numberTransaccionPerSenderNumber >= numTopUp) {
            throw new MaxNumberTransactionDailyPerSenderException("MaxNumberTransactionDailyPerSenderException");
        }

    }

    public List<Country> externalGetBillPaymentCountries(AccountData userData) throws NullParameterException, InvalidAccountException, DisabledAccountException, EmptyListException, GeneralException {
        externalValidateAccount(userData);
        List<Country> countries = new ArrayList<Country>();
        try {
            StringBuilder sqlBuilder = new StringBuilder("SELECT DISTINCT bpp.country FROM BillPaymentProduct bpp WHERE bpp.enabled = 1");
            Query query = entityManager.createQuery(sqlBuilder.toString());
            countries = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        if (countries.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return countries;
    }

    public List<BillPaymentProduct> externalGetBillPaymentProductsByCountryId(AccountData userData, Long countryId) throws NullParameterException, InvalidAccountException, DisabledAccountException, EmptyListException, GeneralException {
        Account account = externalValidateAccount(userData);
        if (countryId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "providerId"), null);
        }
        List<BillPaymentProduct> billPaymentProducts = new ArrayList<BillPaymentProduct>();
        float commission = 0f;
        try {
            StringBuilder sqlBuilder = new StringBuilder("SELECT bpp FROM BillPaymentProduct bpp WHERE bpp.enabled = 1 AND bpp.country.id=?1");
            Query query = entityManager.createQuery(sqlBuilder.toString());
            query.setParameter("1", countryId);
            billPaymentProducts = query.setHint("toplink.refresh", "true").getResultList();
            commission = billPaymentEJB.getBillPaymentCalculationByAccountId(account.getId());
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        if (billPaymentProducts.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        float fee = 0F;
        for (BillPaymentProduct product : billPaymentProducts) {
            fee = product.getProviderFee() != null ? product.getProviderFee() : 0F;
            product.setProviderFee(fee + commission);
        }
        return billPaymentProducts;
    }

    public List<CreditcardType> externalGetCreditcardTypes(AccountData userData) throws NullParameterException, InvalidAccountException, DisabledAccountException, EmptyListException, GeneralException {
        externalValidateAccount(userData);
        List<CreditcardType> creditcardTypes = new ArrayList<CreditcardType>();
        StringBuilder sqlBuilder = new StringBuilder("SELECT t FROM CreditcardType t");
        Query query = null;
        try {
            query = createQuery(sqlBuilder.toString());
            creditcardTypes = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (creditcardTypes.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return creditcardTypes;
    }

    public Recharge externalProcessPayment(AccountData accountData, Customer customer, PaymentInfo paymentInfo, Float amount, String externalId) throws NullParameterException, InvalidAccountException, DisabledAccountException, InvalidAmountException, InvalidCreditCardException, InvalidCreditCardDateException, PaymentServiceUnavailableException, PaymentDeclinedException, GeneralException {
        validateCustomerData(customer);
        Account account = externalValidateAccount(accountData);

        if (paymentInfo == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "paymentInfo"), null);
        } else if (customer == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "customer"), null);
        } else if (amount <= 0) {
            throw new InvalidAmountException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), QueryConstants.PARAM_ENDING_DATE), null);
        }
        if (paymentInfo.getPaymentType().getId().equals(PaymentType.CREDIT_CARD)) {
            if (!paymentInfo.getCreditcardType().getId().equals(CreditCardUtils.getCardID(paymentInfo.getCreditCardNumber())) || !CreditCardUtils.validate(paymentInfo.getCreditCardNumber())) {
                logger.error("Invalid credit card number");
                throw new InvalidCreditCardException("Invalid credit card number");
            }
        } else {
            throw new InvalidCreditCardException("Unavaliable paymentType");
        }
        try {
            AuthorizeNet authorizeNet = new AuthorizeNet();
            Recharge recharge = authorizeNet.processTransaction(paymentInfo, account, amount, entityManager, logger);
            EntityTransaction eTransaction = entityManager.getTransaction();
            try {
                eTransaction.begin();
                entityManagerWrapper.save(recharge);
                eTransaction.commit();
            } catch (Exception e) {
                try {
                    if (eTransaction.isActive()) {
                        eTransaction.rollback();
                    }
                } catch (IllegalStateException e1) {
                    throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), "processPayment", e.getMessage()), e);
                } catch (SecurityException e1) {
                    throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), "processPayment", e.getMessage()), e);
                }

            }
            if (recharge.getStatus().getId().equals(RechargeStatus.RECHAZADO)) {
                throw new PaymentDeclinedException("Payment was declined for Authorize.Net. Response=" + recharge.getHumanReadableResponseCode());
            }
            return recharge;
        } catch (PaymentServiceUnavailableException ex) {
            throw new PaymentServiceUnavailableException(ex.getMessage());
        } catch (PaymentDeclinedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), null, ex.getMessage()), ex);
        }
    }

    public boolean externalValidatePaymentInfo(PaymentInfo info, Long transactionTypeId) throws NullParameterException, GeneralException {
        if (info == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "paymentInfo"), null);
        }
        boolean valid = true;
        info.setPaymentPatner(entityManager.find(PaymentPatner.class, 1l));
        Date now = new Date((new java.util.Date()).getTime());
        Timestamp nowTimestamp = new Timestamp(now.getTime());
        info.setBeginningDate(nowTimestamp);
        info.setPaymentType(entityManager.find(PaymentType.class, PaymentType.CREDIT_CARD));
        EntityTransaction userTransaction = entityManager.getTransaction();
        // 	UserTransaction userTransaction = context.getUserTransaction();
        Long numberTransaccionCreditCardNumber = 0L;
        Date nowBeginning = GeneralUtils.getBeginningDate(new Date((new java.util.Date()).getTime()));
        Date nowEnding = GeneralUtils.getEndingDate(new Date((new java.util.Date()).getTime()));
        Timestamp nowBeginningDate = new Timestamp(nowBeginning.getTime());
        Timestamp nowEndingDate = new Timestamp(nowEnding.getTime());
        try {
            userTransaction.begin();
            entityManager.persist(info);
            entityManager.flush();
            StringBuilder sqlBuilder = new StringBuilder("SELECT COUNT(t.id) FROM payment_info p1, transaction t ,payment_info p2"
                    + " WHERE p1.creditCardNumber=p2.creditCardNumber AND p1.id=t.paymentInfoId AND t.transactionStatusId=" + TransactionStatus.PROCESSED + " AND p2.id=" + info.getId() + " AND t.creationDate BETWEEN DATE('" + nowBeginningDate + "') AND ('" + nowEndingDate + "')");
            if (transactionTypeId != null) {
                sqlBuilder.append(" AND t.transactionTypeId=").append(transactionTypeId);
            }
            List result = new ArrayList();
            result = (List) entityManager.createNativeQuery(sqlBuilder.toString()).getSingleResult();
            numberTransaccionCreditCardNumber = (Long) result.get(0);
            if (numberTransaccionCreditCardNumber >= 2) {
                valid = false;
            }
            entityManager.remove(info);
            userTransaction.commit();

        } catch (Exception e) {
            logger.error("Exception in method loadPaymentInfo: ", e);
            throw new GeneralException("Exception in method loadPaymentInfo: " + e.getMessage(), e.getStackTrace());
        }
        return valid;
    }

    public Boolean externalCancelPayment(AccountData userData, Recharge recharge, Float amount) throws NullParameterException, InvalidAccountException, DisabledAccountException, CancelPaymentException, GeneralException {
        Boolean isProcessOrderVoiding = false;
        externalValidateAccount(userData);
        if (recharge == null) {
            throw new NullParameterException("Parameter recharge cant be null in method cancelPayment");
        } else if (amount == null) {
            throw new NullParameterException("Parameter amount cant be null in method cancelPayment");
        }
        try {
            AuthorizeNet authorizeNet = new AuthorizeNet();
            isProcessOrderVoiding = ServiceConstans.TEST_MODE ? ServiceConstans.TEST_MODE : authorizeNet.processVoid(recharge.getTransactionNumber(), amount, logger);
            EntityTransaction eTransaction = entityManager.getTransaction();
            try {
                eTransaction.begin();
                RechargeStatus rechargeStatus = ContentManager.getInstance().getRechargeStatusById(RechargeStatus.DEVUELTO);
                recharge.setStatus(rechargeStatus);
                entityManagerWrapper.update(recharge);
                eTransaction.commit();
            } catch (Exception e) {
                try {
                    if (eTransaction.isActive()) {
                        eTransaction.rollback();
                    }
                } catch (IllegalStateException e1) {
                    throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), "processPayment", e.getMessage()), e);
                } catch (SecurityException e1) {
                    throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), "processPayment", e.getMessage()), e);
                }
                throw new GeneralException("Exception in method cancelPayment: " + e.getMessage(), e.getStackTrace());

            }
        } catch (Exception e) {
            logger.error("Exception in method cancelPayment: ", e);
            throw new GeneralException("Exception in method cancelPayment: " + e.getMessage(), e.getStackTrace());
        }
        return isProcessOrderVoiding;
    }

    public Boolean externalCancelPayment(AccountData userData, Transaction transaction) throws NullParameterException, InvalidAccountException, DisabledAccountException, CancelPaymentException, GeneralException {
        Boolean isProcessOrderVoiding = false;
        externalValidateAccount(userData);
        if (transaction == null) {
            throw new NullParameterException("Parameter transaction cant be null in method cancelPayment");
        }
        Recharge recharge = transaction.getRecharge();
        try {
            AuthorizeNet authorizeNet = new AuthorizeNet();
            isProcessOrderVoiding = ServiceConstans.TEST_MODE ? ServiceConstans.TEST_MODE : authorizeNet.processVoid(recharge.getTransactionNumber(), transaction.getTotalAmount(), logger);
            EntityTransaction eTransaction = entityManager.getTransaction();
            try {
                eTransaction.begin();
                RechargeStatus rechargeStatus = ContentManager.getInstance().getRechargeStatusById(RechargeStatus.DEVUELTO);
                recharge.setStatus(rechargeStatus);
                entityManagerWrapper.update(recharge);
                eTransaction.commit();
            } catch (Exception e) {
                try {
                    if (eTransaction.isActive()) {
                        eTransaction.rollback();
                    }
                } catch (IllegalStateException e1) {
                    throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), "processPayment", e.getMessage()), e);
                } catch (SecurityException e1) {
                    throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), "processPayment", e.getMessage()), e);
                }
                throw new GeneralException("Exception in method cancelPayment: " + e.getMessage(), e.getStackTrace());

            }
        } catch (Exception e) {
            logger.error("Exception in method cancelPayment: ", e);
            throw new GeneralException("Exception in method cancelPayment: " + e.getMessage(), e.getStackTrace());
        }
        return isProcessOrderVoiding;
    }

    public Customer externalRegisterCustomer(AccountData userData, Customer customer) throws NullParameterException, CustomerAlreadyExistException, InvalidAccountException, DisabledAccountException, GeneralException {
        Account account = externalValidateAccount(userData);
        com.sg123.ejb.UserEJB sisacUserEJB = (com.sg123.ejb.UserEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.USER_EJB);
        com.sg123.model.WebUser sisacWebUser = null;
        com.sg123.model.utils.Enterprise sisacEnterprise = new com.sg123.model.utils.Enterprise();
        Date now = new Date((new java.util.Date()).getTime());
        Timestamp nowTimestamp = new Timestamp(now.getTime());
        sisacEnterprise.setId(com.sg123.model.utils.Enterprise.WIXTEL_USA);
        Customer newCustomer = new Customer();
        try {
            customer = customerEJB.loadCustomerByLogin(customer.getLogin());
            throw new CustomerAlreadyExistException("customer already exists");
        } catch (RegisterNotFoundException rnf) {
            try {
                customerEJB.saveCustomer(customer);
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);

            }
        }

        try {
            sisacWebUser = sisacUserEJB.getWebUserByLogin(customer.getLogin(), sisacEnterprise.getId());
            throw new CustomerAlreadyExistException("Web user already exists. Login = " + customer.getLogin());
        } catch (com.sg123.exception.WebUserNotFoundException ex) {
            com.sg123.model.contract.Customer sisacCustomer = this.fillSisacCustomer(customer, nowTimestamp, sisacEnterprise);
            sisacWebUser = this.fillSisacWebUser(sisacCustomer, customer, nowTimestamp, sisacEnterprise, account.getLanguage().getId());
            /* Registro de webUser y customer en SISAC*/
            try {
                sisacWebUser = sisacUserEJB.registerAndConfirm(sisacWebUser, sisacEnterprise.getId());
                newCustomer.setLogin(customer.getLogin());
                newCustomer.setFirstName(customer.getFirstName());
                newCustomer.setLastName(customer.getLastName());
                newCustomer.setEmail(customer.getEmail());
                newCustomer.setPhoneNumber(newCustomer.getPhoneNumber());

            } catch (Exception ex1) {
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex1);
            }
        } catch (CustomerAlreadyExistException ex) {
            throw new CustomerAlreadyExistException(ex.getMessage());
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
        }
        return newCustomer;
    }

    public Customer externalSearchCustomer(AccountData userData, String email) throws NullParameterException, RegisterNotFoundException, InvalidAccountException, DisabledAccountException, GeneralException {
        externalValidateAccount(userData);
        com.sg123.ejb.UserEJB sisacUserEJB = (com.sg123.ejb.UserEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.USER_EJB);
        com.sg123.model.WebUser sisacWebUser = null;
        com.sg123.model.utils.Enterprise sisacEnterprise = new com.sg123.model.utils.Enterprise();
        sisacEnterprise.setId(com.sg123.model.utils.Enterprise.WIXTEL_USA);
        Customer newCustomer = new Customer();
        try {
            sisacWebUser = sisacUserEJB.getWebUserByLogin(email, sisacEnterprise.getId());
            newCustomer.setFirstName(sisacWebUser.getPrincipalCustomer().getName());
            newCustomer.setLastName(sisacWebUser.getPrincipalCustomer().getLastName());
            newCustomer.setEmail(sisacWebUser.getPrincipalCustomer().getEmail());
            newCustomer.setPhoneNumber(sisacWebUser.getPrincipalCustomer().getPhone());
        } catch (com.sg123.exception.WebUserNotFoundException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), null), ex);
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), ex);
        }
        return newCustomer;
    }

   
    public void externalDeletePinFree(AccountData userData, String phoneNumber) throws NullParameterException, DisabledPinException, InvalidAccountException, DisabledAccountException, RegisterNotFoundException, GeneralException {
        externalValidateAccount(userData);
        if (phoneNumber == null) {
            throw new NullParameterException("Parameter phoneNumber cannot be null");
        }
        com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);
        try {
            com.sg123.model.platformprepay.PinFree pinFree = pinEJB.getPinFreeByAni(new Long(phoneNumber));
            if (!pinFree.getPin().getPinStatus().getId().equals(PinStatus.PIN_AVAILABLE_STATE)) {
                throw new DisabledPinException("Trying to dele pin free..Pin Not available.");
            }
            pinEJB.deletePinFree(pinFree.getId());
        } catch (DisabledPinException ex) {
            ex.printStackTrace();
            throw (ex);
        } catch (PinFreeNotFoundException ex) {
            ex.printStackTrace();
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
    }

    public void externalAsociatePinFree(AccountData userData, String serial, String phoneNumber) throws NullParameterException, InvalidAccountException, DisabledAccountException, RegisterNotFoundException, InvalidPhoneNumberException, PinFreeQuantityExceededException, GeneralException {

        externalValidateAccount(userData);
        if (serial == null) {
            throw new NullParameterException("Parameter serial cannot be null");
        } else if (phoneNumber == null) {
            throw new NullParameterException("Parameter phoneNumber cannot be null");
        }
        try {
            com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);
            Long[] anis = new Long[1];
            anis[0] = new Long(phoneNumber);
            pinEJB.savePinFree(new Long(serial), anis);
        } catch (com.sg123.exception.PinFreeQuantityExceededException ex) {
            ex.printStackTrace();
            throw new PinFreeQuantityExceededException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        } catch (com.sg123.exception.PinNotFoundException ex) {
            ex.printStackTrace();
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        } catch (com.sg123.exception.InvalidAniException ex) {
            ex.printStackTrace();
            throw new InvalidPhoneNumberException(ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
    }

    public SearchPinResponse externalSearchPin2(AccountData userData, String phoneNumber, String serial) throws NullParameterException, InvalidAccountException, DisabledAccountException, InvalidPhoneNumberException, RegisterNotFoundException, GeneralException {
        externalValidateAccount(userData);

        if (serial == null && phoneNumber == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "serial - phoneNumber - customerLogin"), null);
        }
        SearchPinResponse searchPinResponse = new SearchPinResponse();
        com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);

        try {
            com.sg123.model.platformprepay.Pin pin = null;

            if (serial != null) {
                pin = pinEJB.loadPin(new Long(serial));
            } else {//phoneNumber
                pin = pinEJB.getPinFreeByAni(new Long(phoneNumber)).getPin();
            }

            com.sg123.model.contract.Customer customer = pin.getWebUser().getPrincipalCustomer();
            ResponseCustomer rCustomer = new ResponseCustomer();
            rCustomer.setLogin(pin.getWebUser().getLogin());
            rCustomer.setPassword(pin.getWebUser().getPassword());
            rCustomer.setEnterprideId(pin.getWebUser().getEnterprise().getId().toString());

            if (customer.getName() != null) {
                rCustomer.setFirstName(customer.getName());
            }
            if (customer.getLastName() != null) {
                rCustomer.setLastName(customer.getLastName());
            }
            if (customer.getEmail() != null) {
                rCustomer.setEmail(customer.getEmail());
            }
            if (customer.getPhone() != null) {
                rCustomer.setPhoneNumber(customer.getPhone());
            }
            if (customer.getBirthDate() != null) {
                rCustomer.setBirthDate(customer.getBirthDate());
            }
            if (customer.getCivilState() != null) {
                rCustomer.setCivilState(customer.getCivilState());
            }
            if (customer.getGender() != null) {
                rCustomer.setGender(customer.getGender());
            }
            ResponseAddress rAddrees = new ResponseAddress();
            if (customer.getAddress().getCountry() != null) {
                rAddrees.setCountryId(customer.getAddress().getCountry().getId().toString());
            }
            if (customer.getAddress().getState() != null) {
                rAddrees.setStateId(customer.getAddress().getState().getId().toString());
            }
            if (customer.getAddress().getCity() != null) {
                rAddrees.setCityId(customer.getAddress().getCity().getId().toString());
            }
            if (customer.getAddress().getCounty() != null) {
                rAddrees.setCountyId(customer.getAddress().getCounty().getId().toString());
            }
            if (customer.getAddress().getAddress1() != null) {
                rAddrees.setAddress1(customer.getAddress().getAddress1());
            }
            if (customer.getAddress().getZipCode() != null) {
                rAddrees.setZipCode(customer.getAddress().getZipCode());
            }
            if (customer.getAddress().getStateName() != null) {
                rAddrees.setStateName(customer.getAddress().getStateName());
            }
            if (customer.getAddress().getCountyName() != null) {
                rAddrees.setCountyName(customer.getAddress().getCountyName());
            }
            if (customer.getAddress().getCityName() != null) {
                rAddrees.setCityName(customer.getAddress().getCityName());
            }
            if (customer.getBalance() != null) {
                rCustomer.setBalance(pin.getCurrentBalance().toString());
            }
            rCustomer.setAddress(rAddrees);
            rCustomer.setPin(serial.toString());

            List<PinResponse> rPinFrees = new ArrayList<PinResponse>();

            for (com.sg123.model.platformprepay.PinFree pinFree : pin.getPinsFree()) {
                PinResponse pinResponse = new PinResponse();
                pinResponse.setCodeId(pinFree.getCode().getId().toString());
                pinResponse.setSerial(pinFree.getPin().getSerial().toString());
                pinResponse.setAni(pinFree.getAni().toString());
                pinResponse.setBalance(pin.getCurrentBalance());
                //No retorna el secret.
                rPinFrees.add(pinResponse);
            }
            rCustomer.setPinFrees(rPinFrees);
            searchPinResponse.setCustomer(rCustomer);
        } catch (Exception e) {
            e.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), null);
        }
        return searchPinResponse;

    }

    //TODO: Se debe modificar para cambiar la comision y agregarle un cobro extra para los proveedores externos.
    public List<TopUpProduct> externalGetTopUpProductByMobileOperatorId2(AccountData userData, Long mobileOperatorId) throws NullParameterException, InvalidAccountException, DisabledAccountException, EmptyListException, GeneralException {
        externalValidateAccount(userData);
        if (mobileOperatorId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "mobileOperatorId"), null);
        }
        List<TopUpProduct> topUpProducts = new ArrayList<TopUpProduct>();
        try {
            Query query = createQuery("SELECT tup1 FROM TopUpProduct tup1 WHERE tup1.mobileOperator.id = ?1 AND tup1.enabled = 1 AND tup1.commissionPercent = (SELECT MAX(tup2.commissionPercent) FROM TopUpProduct tup2 WHERE tup2.mobileOperator.id = ?1 AND tup2.enabled = 1  AND tup1.productDenomination.id = tup2.productDenomination.id) ORDER BY tup1.productDenomination.amount");
            query.setParameter("1", mobileOperatorId);
            topUpProducts = query.setHint("toplink.refresh", "true").getResultList();

        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }

        if (topUpProducts.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        topUpProducts.size();
        return topUpProducts;
    }

    public Account externalGetAccount(String login, String password, String ipRemoteAddress) throws NullParameterException, InvalidAccountException, DisabledAccountException, GeneralException {
        return this.externalValidateAccount(new AccountData(login, password,ipRemoteAddress));
    }

    public BillPaymentResponse externalProcessBillPayment(AccountData userData, BillPaymentProduct billPaymentProduct, Float amount, String accountNumber, String senderName, String senderNumber, String externalId, Long languageId, boolean sendSMS, String destinationSMS) throws NullParameterException, InvalidAccountException, DisabledAccountException, CarrierSystemUnavailableException, InvalidSubscriberNumberException, SubscriberWillExceedLimitException, SubscriberAccountException,NegativeBalanceException,CreditLimitExcededException, GeneralException {

        Account account = externalValidateAccount(userData);
        BillPaymentResponse response = new BillPaymentResponse();
        Transaction transaction = null;
        Date now = new Date((new java.util.Date()).getTime());
        StringBuilder transactionData = new StringBuilder();
        Timestamp nowTimestamp = new Timestamp(now.getTime());
        // Vas contra el saldo del cliente
        float realAmount = 0f;
        try {
            Product product = productEJB.loadProductById(Product.ELECTRONIC_PIN_ID);
            realAmount = transactionEJB.getRealValueByProdut(account, amount, product,null);
            System.out.println("realAmount......." + realAmount);
            transaction = new Transaction(amount, realAmount, account);
            ValidateBalance(account, realAmount);
        } catch (NegativeBalanceException ex) {
            throw ex;
        } catch (CreditLimitExcededException ex) {
            throw ex;
        } catch (GeneralException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
        }
        try {
            response = billPaymentEJB.executeBillPayment(billPaymentProduct, amount, accountNumber, senderName, senderNumber, account, sendSMS, destinationSMS, languageId);
            transactionData.append("BillPayment: ").append(billPaymentProduct.getName()).append("( ").append(billPaymentProduct.getId()).append(" )").append(" Monto: ").append(amount).append("AccountNumber: ").append(accountNumber);
            transaction = new Transaction(new TransactionType(TransactionType.BILL_PAYMENT_PURCHASE), new TransactionStatus(TransactionStatus.PROCESSED), amount, 0f, 0f, nowTimestamp, transactionData.toString(), account, null, null, billPaymentProduct, transactionData.toString(), externalId, null, "", null, null, null, 0f,userData.getIpRemoteAddress());

        } catch (CarrierSystemUnavailableException ex) {
            transaction = new Transaction(new TransactionType(TransactionType.BILL_PAYMENT_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), amount, 0f, 0f, nowTimestamp, null, account, null, null, billPaymentProduct, null, externalId, null, "", null, null, null, 0f,userData.getIpRemoteAddress());
            throw new CarrierSystemUnavailableException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
        } catch (InvalidSubscriberNumberException ex) {
            transaction = new Transaction(new TransactionType(TransactionType.BILL_PAYMENT_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), amount, 0f, 0f, nowTimestamp, null, account, null, null, billPaymentProduct, null, externalId, null, "", null, null, null, 0f,userData.getIpRemoteAddress());
            throw new InvalidSubscriberNumberException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
        } catch (SubscriberWillExceedLimitException ex) {
            transaction = new Transaction(new TransactionType(TransactionType.BILL_PAYMENT_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), amount, 0f, 0f, nowTimestamp, null, account, null, null, billPaymentProduct, null, externalId, null, "", null, null, null, 0f,userData.getIpRemoteAddress());
            throw new SubscriberWillExceedLimitException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
        } catch (SubscriberAccountException ex) {
            transaction = new Transaction(new TransactionType(TransactionType.BILL_PAYMENT_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), amount, 0f, 0f, nowTimestamp, null, account, null, null, billPaymentProduct, null, externalId, null, "", null, null, null, 0f,userData.getIpRemoteAddress());
            throw new SubscriberAccountException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
        } catch (Exception ex) {
            transaction = new Transaction(new TransactionType(TransactionType.BILL_PAYMENT_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), amount, 0f, 0f, nowTimestamp, null, account, null, null, billPaymentProduct, null, externalId, null, "", null, null, null,0f,userData.getIpRemoteAddress());
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
        } finally {
            try {
                transaction = this.saveTransaction(transaction);
                response.setTransaction(transaction);
            } catch (Exception ex) {
                throw new GeneralException("Exception trying saving transaction in method processBillPayment. " + ex.getMessage());
            }
        }
        return response;
    }

  
    public List<Country> externalGetCountriesForBillPayment(AccountData userData) throws NullParameterException, InvalidAccountException, DisabledAccountException, GeneralException, EmptyListException {
        Account account = externalValidateAccount(userData);
        List<Country> countries = new ArrayList<Country>();
        try {
            StringBuilder sqlBuilder = new StringBuilder("SELECT DISTINCT bpp.country FROM BillPaymentProduct bpp WHERE bpp.enabled = 1");
            Query query = entityManager.createQuery(sqlBuilder.toString());
            countries = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        if (countries.isEmpty()) {
            return null;
        }

        return countries;
    }

    public SMS externalSendSMS(SMS sms, Long enterpriseId) throws GeneralException, SMSFailureException, NullParameterException {
        PreferenceManager pm = null;
        try {
            pm = PreferenceManager.getInstance();
        } catch (Exception ex) {
            throw new GeneralException(ex.getMessage());
        }
        boolean isMlat = false;
        boolean isTelintel = false;
        try {
            //sms.setDestination(ServiceConstans.TEST_MODE ? sms.getDestination() : EjbConstants.TEST_PHONE_NUMBER);
            sms.setDestination(sms.getDestination());
            String smsProvider = pm.getPreferencesValueByEnterpriseAndPreferenceId(enterpriseId, PreferenceFieldEnum.DEFAULT_SMS_PROVIDER.getId());
            isMlat = smsProvider.equals(String.valueOf(Provider.MLAT));
            isTelintel = smsProvider.equals(String.valueOf(Provider.TELINTEL));
            if (isMlat) {
                sms = sendMLatSMS(sms);
            } else if (isTelintel) {
                sms = sendTelintelSMS(sms);
            }
        } catch (Exception ex) {
            sms.setAdditional(ex.getMessage());
            if (!isMlat) {
                sms = sendMLatSMS(sms);
            } else if (!isTelintel) {
                sms = sendTelintelSMS(sms);
            }
        }
        return sms;
    }

    public SMS sendTelintelSMS(SMS sms) throws SMSFailureException, NullParameterException, GeneralException {

        EJBRequest request = new EJBRequest();
        try {
            System.out.println("Trying to sent TELINTEL sms...");
            sms.setIntegratorName("TELINTEL");
            sms.setStatus(SMS.APPROVED);
            sms.setCreationDate(new Timestamp(new java.util.Date().getTime()));
            com.alodiga.services.provider.commons.sms.integration.RequestManager.sendSMSPostRequest(sms.getContent(), sms.getDestination());
            System.out.println("Successfully sms sent ...");
        } catch (Exception ex) {
            ex.printStackTrace();
            sms.setAdditional(ex.getMessage());
            sms.setStatus(SMS.CANCELED);
            sendSMSFailureMail(ex.getMessage());
            throw new SMSFailureException(ex.getMessage());
        } finally {
            request.setParam(sms);
            sms = this.saveSMS(request);
        }
        return sms;
    }

    public SMS sendMLatSMS(SMS sms) throws SMSFailureException, NullParameterException, GeneralException {
        EJBRequest request = new EJBRequest();
        SMS newSms = new SMS();
        newSms.setAccount(sms.getAccount());
        newSms.setAdditional(sms.getAdditional());
        newSms.setContent(sms.getContent());
        newSms.setCustomerEmail(sms.getCustomerEmail());
        newSms.setCreationDate(new Timestamp(new java.util.Date().getTime()));
        newSms.setIntegratorName("MLAT");
        newSms.setSender(sms.getSender());
        newSms.setStatus(SMS.APPROVED);
        newSms.setDestination(sms.getDestination());
        try {
            System.out.println("Trying to sent sms with MLAT...");
            MLatRequestManager.sendSmsGetRequest(newSms.getContent(), newSms.getDestination());
            System.out.println("Successfully sms sent ...");
        } catch (Exception ex) {
            ex.printStackTrace();
            newSms.setAdditional(ex.getMessage());
            newSms.setStatus(SMS.CANCELED);
            sendSMSFailureMail(ex.getMessage());
            throw new SMSFailureException(ex.getMessage());
        } finally {
            request.setParam(newSms);
            newSms = this.saveSMS(request);
        }
        return newSms;
    }

    private void sendSMSFailureMail(String message) {
        try {
            Mail mail = new Mail();
            mail.setSubject("Error al enviar SMS -SmsTelintelIntegration");
            mail.setBody("Error al enviar SMS -SmsTelintelIntegration " + message);
            ArrayList<String> recipients = new ArrayList<String>();
            recipients.add(CommonMails.SAC_COORDINADORES_MAIL);
            mail.setFrom(CommonMails.SAC_COORDINADORES_MAIL);
            mail.setTo(recipients);
            this.sendMail(mail);
        } catch (Exception ex) {
            ex.printStackTrace();
            java.util.logging.Logger.getLogger(UtilsEJBImp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public SMS saveSMS(EJBRequest request) throws NullParameterException, GeneralException {
        return (SMS) saveEntity(request, logger, getMethodName());
    }

    public void sendMail(Mail mail) throws GeneralException, NullParameterException {
        SendMail SendMail = new SendMail();
        try {
            SendMail.sendMail(mail);
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
    }

    public Recharge externalProcessBanking(AccountData userData, PaymentInfo paymentInfo, Float amount, String externalId) throws NullParameterException, InvalidAccountException, DisabledAccountException, InvalidPhoneNumberException, InvalidCreditCardException, InvalidCreditCardDateException, InvalidAmountException, PaymentDeclinedException, InvalidPaymentInfoException, GeneralException {
        Account account = externalValidateAccount(userData);
        if (amount <= 0) {
            throw new InvalidAmountException("Invalid amount for process payment: " + amount);
        }
        Transaction transaction = null;
        StringBuffer transactionData = new StringBuffer();
        Date now = new Date((new java.util.Date()).getTime());
        Timestamp nowTimestamp = new Timestamp(now.getTime());
        Recharge recharge = null;
        try {
            recharge = this.externalProcessPayment(account, paymentInfo, amount);
        } catch (InvalidAmountException e) {
            throw (e);
        } catch (PaymentDeclinedException e) {
            ServiceMailDispatcher.sendPinPurchaseErrorMail(account, "Error en el proceso de cobro : " + transactionData.toString(), "Paso 2: Procesando el pago con Authorize.net. (PAGO DECLINADO)", e);
            throw (e);
        } catch (InvalidCreditCardDateException e) {
            ServiceMailDispatcher.sendPinPurchaseErrorMail(account, "Error en el proceso de cobro : " + transactionData.toString(), "Paso 2: Procesando el pago con Authorize.net. Error Fecha de Expiracion(PAGO DECLINADO)", e);
            throw (e);
        } catch (InvalidPaymentInfoException e) {
            ServiceMailDispatcher.sendPinPurchaseErrorMail(account, "Error en el proceso de cobro : " + transactionData.toString(), "Paso 2: Procesando el pago con Authorize.net. (METODO DE PAGO INVALIDO)", e);
            throw (e);
        } catch (Exception e) {
            ServiceMailDispatcher.sendPinPurchaseErrorMail(account, "Error en el proceso de cobro : " + transactionData.toString(), "Paso 2: Procesando el pago con Authorize.net. (ERROR GENERAL)", e);
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ""), e);
        }
        TransactionType transactionType = null;
        TransactionStatus transactionStatus = null;
        try {
            transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PURCHASE_BALANCE);
            transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.PROCESSED);
            transactionData.append("Cobro de dinero contra procesador bancario [monto)" + amount);
            transaction = new Transaction(transactionType, transactionStatus, recharge.getTotalAmount(), 0f, 0f, nowTimestamp, transactionData.toString(), account, null, null, null, transactionData.toString(), externalId, recharge, "", null, null, paymentInfo, 0f,userData.getIpRemoteAddress());
        } catch (Exception ex) {
            try {
                ex.printStackTrace();
                transactionData.append("Cobro de dinero contra procesador bancario [monto)" + amount);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.PROCESSED);
                transaction = new Transaction(transactionType, transactionStatus, recharge.getTotalAmount(), 0f, 0f, nowTimestamp, transactionData.toString(), account, null, null, null, null, externalId, null, "", null, null, paymentInfo, 0f,userData.getIpRemoteAddress());
                throw new GeneralException(ex.getMessage());
            } catch (Exception ex1) {
                throw new GeneralException(ex.getMessage());
            }
        } finally {
            try {
                this._saveTransaction(transaction);
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new GeneralException("Exception trying saving transaction in method externalProcessBanking. " + ex.getMessage());
            }
        }
        return recharge;
    }

    public boolean externalHasAvailabeSMS(String customerEmail) throws NullParameterException, GeneralException {
        if (customerEmail == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "accountData"), null);
        }
        List<SMS> smss = null;
        try {
            Date now = new Date((new java.util.Date()).getTime());
            Query query = null;
            query = createQuery("SELECT s FROM SMS s WHERE s.customerEmail=?1 AND s.creationDate BETWEEN ?2 AND ?3 AND s.status='APPROVED'");
            query.setParameter("1", customerEmail);
            query.setParameter("2", GeneralUtils.getBeginningDate(now));
            query.setParameter("3", GeneralUtils.getEndingDate(now));
            smss = query.setHint("toplink.refresh", "true").getResultList();
            if (smss.size() >= Constants.MAX_SMS_PER_CUSTOMER) {
                return false;
            }
        } catch (NoResultException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName(), "General Exception"), ex);
        }
        return true;
    }

    public boolean externalValidateTransaction(EJBRequest request) throws GeneralException, NullParameterException, TransactionNotAvailableException, MaxAmountPerTransactionException, MaxAmountDailyException, TransactionNotAvailableException {
        boolean transactionAproved = true;
        if (request.getParam() == null) {
            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), QueryConstants.PARAM_TRANSACTION), null);
        }

        Transaction t = (Transaction) request.getParam();
        EJBRequest validateRequest = new EJBRequest();

        validateRequest.setAuditData(request.getAuditData());
        validateRequest.setFirst(request.getFirst());
        validateRequest.setLimit(request.getLimit());
        validateRequest.setMediaType(request.getMediaType());
        validateRequest.setMethod(request.getMethod());
        validateRequest.setUrl(request.getUrl());

        String clientTypeParam = null;
        Long clientId = null;
        Long enterpriseId = null;
        if (t.getAccount() != null) {
            clientTypeParam = QueryConstants.PARAM_ACCOUNT_ID;
            clientId = t.getAccount().getId();
            enterpriseId = t.getAccount().getEnterprise().getId();
        }
        EJBRequest prefRequest = new EJBRequest();
        prefRequest.setParams(new HashMap<String, Object>());
        prefRequest.getParams().put(QueryConstants.PARAM_ENTERPRISE_ID, enterpriseId);

        Map<Long, String> preferences = new HashMap<Long, String>();
        try {
            PreferenceManager pManager = PreferenceManager.getInstance();
            preferences = pManager.getPreferences();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }

        Float maxAmountPerTransaction = Float.parseFloat(preferences.get(PreferenceFieldEnum.MAX_TRANSACTION_AMOUNT_LIMIT.getId()));
        Float maxAmountDaily = Float.parseFloat(preferences.get(PreferenceFieldEnum.MAX_TRANSACTION_AMOUNT_DAILY_LIMIT.getId()));

        boolean transactionAvailable = preferences.get(PreferenceFieldEnum.DISABLED_TRANSACTION.getId()).equals("1");
        if (!transactionAvailable) {
            throw new TransactionNotAvailableException(logger, sysError.format(EjbConstants.ERR_TRANSACTION_NOT_AVAILABLE, this.getClass(), getMethodName()), null);
        }
        Float transactionAmount = t.getTotalAmount();
        if (transactionAmount == null || (transactionAmount != null && transactionAmount > maxAmountPerTransaction)) {
            throw new MaxAmountPerTransactionException(logger, sysError.format(EjbConstants.ERR_MAX_AMOUNT_PER_TRANSACTION, this.getClass(), getMethodName(), maxAmountPerTransaction + ""), null);
        }
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        today.set(Calendar.AM_PM, Calendar.AM);
        Timestamp todayT = new Timestamp(today.getTimeInMillis());
        QueryParam paramValue = new QueryParam();
        paramValue.setOperator(QueryConstants.GREATER_THAN_OR_EQUAL);
        paramValue.setValue(todayT.toString());
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(QueryConstants.PARAM_CREATION_DATE, paramValue);
        params.put(clientTypeParam, clientId);
        validateRequest.setParams(params);
        validateRequest.setParam(true);
        List<Transaction> transactions;
        int promotionalTransactions = 0;
        try {
            transactions = getTransactionByCondition(validateRequest);
        } catch (EmptyListException e) {
            transactions = new ArrayList<Transaction>();
        }
        float amountDaily = 0f;
        for (Transaction transaction : transactions) {
            amountDaily = +transaction.getTotalAmount().floatValue();
            if (transaction.getPromotionAmount() != null && transaction.getPromotionAmount() > 0 && transaction.getTransactionStatus().equals(Transaction.STATUS_PROCESSED)) {
                promotionalTransactions++;
            }

        }
        if ((amountDaily + transactionAmount) > maxAmountDaily) {
            throw new MaxAmountDailyException(logger, sysError.format(EjbConstants.ERR_MAX_AMOUNT_DAILY, this.getClass(), getMethodName(), maxAmountDaily + ""), null);
        }

        return transactionAproved;
    }

    public List<Transaction> getTransactionByCondition(EJBRequest request) throws NullParameterException, EmptyListException, GeneralException {
        //System.out.println("---------------getTransactionByCondition-----------");
        List<Transaction> transactions = new ArrayList<Transaction>();
        Map orderField = new HashMap();
        orderField.put("id", QueryConstants.ORDER_DESC);
        Boolean isFilter = (Boolean) request.getParam();
        if (isFilter == null || isFilter.equals("null")) {
            isFilter = false;
        }
        createSearchQuery(Transaction.class, request, orderField, logger, getMethodName(), "customers", isFilter);
        transactions = (List<Transaction>) createSearchQuery(Transaction.class, request, orderField, logger, getMethodName(), "customers", isFilter);
        return transactions;
    }

    private synchronized Map<String, Object> _saveTransaction(Transaction transaction) throws GeneralException, NullParameterException, TransactionCanceledException, MaxAmountBalanceException, MinAmountBalanceException, TransactionNotAvailableException, MaxAmountPerTransactionException, MaxAmountDailyException, PurchaseCanceledException, PinFreeProvisionException, PinProvisionException, MaxPromotionTransactionDailyException, CommissionCalculationException,NegativeBalanceException {
        System.out.println("---------------------SAVE TRANSACTION------------------");
        Map<String, Object> response = new HashMap<String, Object>();
        BalanceHistory balanceHistory = new BalanceHistory();
        List<BalanceHistory> histories = new ArrayList<BalanceHistory>();
        List<Transaction> wrongTransactions = new ArrayList<Transaction>();
        float amount = 0f;
        try {
            if (transaction.getTransactionStatus().getId().equals(TransactionStatus.PROCESSED)) {
                amount = transaction.getTotalRealValue();
                if (transaction.getTransactionType().getId().equals(TransactionType.PURCHASE_BALANCE)) {
                    balanceHistory = (BalanceHistory) createBalanceHistory(transaction.getAccount(), amount, 2, false);
                } else {
                    balanceHistory = (BalanceHistory) createBalanceHistory(transaction.getAccount(), amount, 1, false);
                }
                balanceHistory.setTransaction(transaction);
                histories.add(balanceHistory);
                transaction.setBalanceHistories(histories);
            }
            try {
                System.out.println("en el try");
                entityManager.getTransaction().begin();
                Account accountPurchase = transaction.getAccount();
                    if(accountPurchase != null){System.out.println("***************** accountPurchaseId:"+accountPurchase.getId());}
                    else{System.out.println("***************** accountPurchase es nulo");}
                if (accountPurchase != null && accountPurchase.getId() == null) {
                    System.out.println("en el if");
                    Address address = transaction.getAccount().getAddress();
                    accountPurchase.setAddress(null);
                    entityManager.persist(accountPurchase);
                    accountPurchase.setAddress(address);
                    System.out.println(accountPurchase.getBalance() - amount);
                    accountPurchase.setBalance(accountPurchase.getBalance() - amount);
                    accountPurchase = entityManager.merge(accountPurchase);
                    transaction.setAccount(accountPurchase);
                }
                else {
                    System.out.println("en el else");
                    System.out.println(accountPurchase.getBalance() - amount);
                    accountPurchase.setBalance(accountPurchase.getBalance() - amount);
                    accountPurchase = entityManager.merge(accountPurchase);
                    transaction.setAccount(accountPurchase);
                }
                transaction.setId(null);
                transaction = entityManager.merge(transaction);
                if (transaction != null) {
                    entityManager.persist(transaction);
                    entityManager.flush();
                }
                entityManager.getTransaction().commit();
                response.put(Constants.RESPONSE_TRANSACTION, transaction);
            } catch (Exception ex) {
                ex.printStackTrace();
                if (entityManager.getTransaction().isActive()) {
                    entityManager.getTransaction().rollback();
                }
                TransactionStatus transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.CANCELED);
                transaction.setTransactionStatus(transactionStatus);
                wrongTransactions.add(transaction);
                persistListObject(wrongTransactions);
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ""), null);
            }

        } catch (CommissionCalculationException m) {
            throw new CommissionCalculationException(logger, sysError.format(EjbConstants.ERR_MAX_AMOUNT_BALANCE, this.getClass(), getMethodName(), "param"), null);
        } catch (NegativeBalanceException m) {
            throw new NegativeBalanceException(logger, sysError.format(EjbConstants.ERR_MAX_AMOUNT_BALANCE, this.getClass(), getMethodName(), "param"), null);
     /*   } catch (MinAmountBalanceException m) {
            throw new MinAmountBalanceException(logger, sysError.format(EjbConstants.ERR_MIN_AMOUNT_BALANCE, this.getClass(), getMethodName(), "param"), null);*/
        } catch (Exception e) {
            transaction.setBalanceHistories(null);
            transaction.setAccount(null);
            wrongTransactions.add(transaction);
            persistListObject(wrongTransactions);
            e.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ""), null);
        }
        return response;
    }

    public boolean persistListObject(List data) {
        boolean success = false;
        EntityManager em = getEntityManagerWrapper().getEntityManager();
        EntityTransaction et = em.getTransaction();
        try {
            et.begin();
            for (Object entity : data) {
                if (entity instanceof AbstractSPEntity) {
                    if (((AbstractSPEntity) entity).getPk() == null) {
                        em.persist(entity);
                    } else {
                        em.merge(entity);
                    }
                }
            }
            et.commit();
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
            makeRoolback(et);
        }
        return success;
    }

    private void makeRoolback(EntityTransaction transaction) {
        try {
            if (transaction.isActive()) {
                transaction.rollback();
            }
        } catch (Exception ex1) {
            ex1.printStackTrace();

        }
    }

    private BalanceHistory createBalanceHistory(Account account, float transferAmount, int transferType, boolean isBalanceTranference) throws GeneralException, EmptyListException, NullParameterException, NegativeBalanceException, CreditLimitExcededException, RegisterNotFoundException {
        BalanceHistory currentBalanceHistory = loadLastBalanceHistoryByAccount2(account.getId());
        float currentAmount = currentBalanceHistory != null ? currentBalanceHistory.getCurrentAmount().floatValue() : 0f;
        BalanceHistory balanceHistory = new BalanceHistory();
        balanceHistory.setAccount(account);
        balanceHistory.setDate(new Timestamp(new Date().getTime()));
        balanceHistory.setOldAmount(currentAmount);
        float newCurrentAmount = 0.0f;
        switch (transferType) {
            case 1:
                newCurrentAmount = currentAmount - transferAmount;
                break;
            case 2:
                newCurrentAmount = currentAmount + transferAmount;//SUMO AL MONTO ACTUAL (EL DESTINO)
                break;
        }
        if (account.getIsPrePaid() && newCurrentAmount < 0) {
            throw new NegativeBalanceException("Current amount can not be negative");
        }
        if (!account.getIsPrePaid() && account.getCrediLimit() + newCurrentAmount < 0) {
            throw new CreditLimitExcededException("Credit Limit Exceed");
        }
        balanceHistory.setCurrentAmount(newCurrentAmount);
        return balanceHistory;
    }

    private boolean ValidateBalance(Account account, float transferAmount) throws GeneralException, EmptyListException, NullParameterException, NegativeBalanceException, CreditLimitExcededException, RegisterNotFoundException {
        BalanceHistory currentBalanceHistory = loadLastBalanceHistoryByAccount2(account.getId());
        float currentAmount = currentBalanceHistory != null ? currentBalanceHistory.getCurrentAmount().floatValue() : 0f;
        float newCurrentAmount = currentAmount - transferAmount;
        if (account.getIsPrePaid() && newCurrentAmount < 0) {
            throw new NegativeBalanceException("Current amount can not be negative");
        }
        if (!account.getIsPrePaid() && account.getCrediLimit() + newCurrentAmount < 0) {
            throw new CreditLimitExcededException("Credit Limit Exceed");
        }
        return true;
    }

    public BalanceHistory loadLastBalanceHistoryByAccount(Long accountId) throws GeneralException, RegisterNotFoundException, NullParameterException {
        if (accountId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "accountId"), null);
        }
        BalanceHistory balanceHistory = null;
        try {
            Timestamp maxDate = (Timestamp) entityManager.createQuery("SELECT MAX(b.date) FROM BalanceHistory b WHERE b.account.id = " + accountId).getSingleResult();
            Query query = entityManager.createQuery("SELECT b FROM BalanceHistory b WHERE b.date = :maxDate AND b.account.id = " + accountId);
            query.setParameter("maxDate", maxDate);

            //balanceHistory = (BalanceHistory) query.setHint("toplink.refresh", "true").getSingleResult();
            List result = (List) query.setHint("toplink.refresh", "true").getResultList();

            if (!result.isEmpty()) {
                balanceHistory = ((BalanceHistory) result.get(0));
            }
        } catch (NoResultException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), "BalanceHistory"), null);
        } catch (Exception e) {
            e.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), "BalanceHistory"), null);
        }
        return balanceHistory;
    }

    public BalanceHistory loadLastBalanceHistoryByAccount2(Long accountId) throws GeneralException, RegisterNotFoundException, NullParameterException {
        if (accountId == null) {
            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "accountId"), null);
        }
        BalanceHistory balanceHistory = null;
        try {

            Query query = entityManager.createQuery("SELECT b FROM BalanceHistory b WHERE b.account.id = " + accountId+" ORDER BY b.id desc");
            //balanceHistory = (BalanceHistory) query.setHint("toplink.refresh", "true").getSingleResult();
            query.setMaxResults(1);
            List result = (List) query.setHint("toplink.refresh", "true").getResultList();
            balanceHistory = (BalanceHistory) ((BalanceHistory) result.get(0));
        } catch (NoResultException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), "BalanceHistory"), null);
        } catch (Exception e) {
            e.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), "BalanceHistory"), null);
        }
        return balanceHistory;
    }

    private void sendPinPurchaseError(Account account, String sms, String step, Exception ex) {
        try {
            Mail mail = ServiceMails.getPinPurchaseErrorMail(account, sms, step, ex);
            (new com.alodiga.services.provider.commons.utils.MailSender(mail)).start();
        } catch (Exception ex1) {
            ex1.printStackTrace();
        }
    }

    public void externalAssociatePinFree(AccountData userData, String serial, String phoneNumber) throws NullParameterException, InvalidAccountException, DisabledAccountException, RegisterNotFoundException, InvalidPhoneNumberException, PinFreeQuantityExceededException, GeneralException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Float accountBalance(AccountData userData) throws NullParameterException, InvalidAccountException, DisabledAccountException, GeneralException {
        Float accountBalance = 0f;
        Account account = externalValidateAccount(userData);
        try {
            accountBalance = loadLastBalanceHistoryByAccount2(account.getId()).getCurrentAmount();
        } catch (RegisterNotFoundException ex) {
            return accountBalance;
        } catch (Exception ex1) {
            ex1.printStackTrace();
           throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ""), null);
        }
        return accountBalance;
    }

}
