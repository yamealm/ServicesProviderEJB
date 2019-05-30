package com.alodiga.services.provider.ejb;

import com.alodiga.services.provider.commons.ejbs.BillPaymentEJBLocal;
import com.alodiga.services.provider.commons.exceptions.*;
import com.alodiga.services.provider.commons.models.*;
import com.alodiga.services.provider.commons.responses.*;
import com.alodiga.services.provider.commons.utils.AccountData;
import com.sg123.ejb.UserEJB;
import com.sg123.exception.CodeNotFoundException;
import com.sg123.exception.ContractNotFoundException;
import com.sg123.exception.CustomerNotFoundException;
import com.sg123.exception.EnterpriseNotFoundException;
import com.sg123.exception.FavoriteCountryNotFoundException;
import com.sg123.exception.InvoiceDetailNotFoundException;
import com.sg123.exception.PaymentPartnerNotFoundException;
import com.sg123.exception.PinNotFoundException;
import com.sg123.exception.PlanNotFoundException;
import com.sg123.exception.TerminationPriceListNotFoundException;
import com.sg123.exception.UnconfirmedEmailException;
import com.alodiga.services.provider.commons.exceptions.WebUserNotFoundException;

import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.interceptor.Interceptors;

import org.apache.log4j.Logger;

import com.alodiga.http.mlat.integration.connection.MLatRequestManager;
//import com.alodiga.local.ws.multilevelchannel.services.LocalWSMultilevelChannelProxy;
//import com.alodiga.local.ws.multilevelchannel.services.LocalWSMultilevelChannelProxy;
import com.alodiga.services.provider.commons.ejbs.CustomerEJBLocal;
import com.alodiga.services.provider.commons.ejbs.ProductEJBLocal;
import com.alodiga.services.provider.commons.ejbs.ServicesEJB;
import com.alodiga.services.provider.commons.ejbs.ServicesEJBLocal;
import com.alodiga.services.provider.commons.ejbs.TransactionEJBLocal;
import com.alodiga.services.provider.commons.ejbs.TopUpProductEJBLocal;
import com.alodiga.services.provider.commons.ejbs.UserEJBLocal;
import com.alodiga.services.provider.commons.ejbs.UtilsEJBLocal;
import com.alodiga.services.provider.commons.genericEJB.AbstractSPEJB;
import com.alodiga.services.provider.commons.genericEJB.EJBRequest;
import com.alodiga.services.provider.commons.utils.EjbConstants;
import com.alodiga.services.provider.commons.genericEJB.SPContextInterceptor;
import com.alodiga.services.provider.commons.genericEJB.SPLoggerInterceptor;
import com.alodiga.services.provider.commons.managers.ContentManager;
import com.alodiga.services.provider.commons.managers.PreferenceManager;
import com.alodiga.services.provider.commons.payment.AuthorizeNet;
import com.alodiga.services.provider.commons.models.billPayment.BillPaymentProduct;
import com.alodiga.services.provider.commons.models.PaymentPatner;
import com.alodiga.services.provider.commons.responses.BillPaymentResponse;
import com.alodiga.services.provider.commons.responses.PinResponse;
import com.alodiga.services.provider.commons.responses.ProvissionPinResponse;
import com.alodiga.services.provider.commons.responses.ResponseAddress;
import com.alodiga.services.provider.commons.responses.ResponseCustomer;
import com.alodiga.services.provider.commons.responses.RechargePinResponse;
import com.alodiga.services.provider.commons.services.models.LoginResponse;
import com.alodiga.services.provider.commons.utils.CommonMails;
import com.alodiga.services.provider.commons.utils.Constants;
import com.alodiga.services.provider.commons.utils.CreditCardUtils;
import com.alodiga.services.provider.commons.utils.DateUtils;
import com.alodiga.services.provider.commons.utils.GeneralUtils;
import com.alodiga.services.provider.commons.utils.Mail;
import com.alodiga.services.provider.commons.utils.QueryConstants;
import com.alodiga.services.provider.commons.utils.SMSSender;
import com.alodiga.services.provider.commons.utils.SendMail;
import com.alodiga.services.provider.commons.utils.ServiceConstans;
import com.alodiga.services.provider.commons.utils.ServiceMailDispatcher;
import com.alodiga.services.provider.commons.utils.ServiceMails;
import com.alodiga.services.provider.commons.utils.ServiceSMSDispatcher;
import com.alodiga.tranferto.nauta.IntegrationNauta;
import com.alodiga.transferto.response.ProcessResponse;
import com.alodiga.ws.payment.services.WSPaymentMethodProxy;
import com.alodiga.ws.payment.services.WsPaymentInfoResponse;
import com.alodiga.ws.salesrecord.services.WsInvoiceResponse;
import com.alodiga.ws.salesrecord.services.WsLoginResponse;
import com.alodiga.ws.salesrecord.services.WsOrderResponse;
import com.alodiga.ws.salesrecord.services.WsSalesRecordProxy;
import com.interax.telephony.service.exception.callingcard.CallingCardRateNotFoundException;
import com.interax.telephony.service.security.Encoder;
import com.sg123.exception.DnNotFoundException;
import com.sg123.exception.InvalidAniException;
import com.sg123.exception.PinFreeNotFoundException;
import com.sg123.exception.RentNotFoundException;
import com.sg123.model.Cycle;
import com.sg123.model.WebUser;
import com.sg123.model.content.Segment;
import com.sg123.model.RUUserHasWebUser;
import com.sg123.model.content.ServiceFamily; 
import com.sg123.model.contract.Contract;
import com.sg123.model.contract.PendingChange;
import com.sg123.model.contract.relation.ContractHasPin;
import com.sg123.model.contract.relation.ContractHasService;
import com.sg123.model.contract.relation.ContractHasWebUser;
import com.sg123.model.contract.relation.ContractHistory;
import com.sg123.model.contract.relation.CustomerHasWebUser;
import com.sg123.model.contract.relation.FavoriteCountryInstance;
import com.sg123.model.ecommerce.CustomService;
import com.sg123.model.invoice.InvoiceMode;
import com.sg123.model.plan.Dn;
import com.sg123.model.plan.FavoriteCountry;
import com.sg123.model.plan.Plan;
import com.sg123.model.plan.Rent;
import com.sg123.model.plan.RentHistory;
import com.sg123.model.plan.TerminationPrice;
import com.sg123.model.platformprepay.FavoriteDestination;
import com.sg123.model.plan.RentHistory;
import com.sg123.model.platformprepay.FavoriteDestinationCombination;
import com.sg123.model.platformprepay.PinStatus;
import com.sg123.model.utils.BillingForm;
import com.sg123.model.utils.ContractAction;
import com.sg123.model.utils.ContractStatus;
import com.sg123.model.utils.OrderStatus;
import com.sg123.model.utils.ProvisionType;
import com.sun.xml.ws.policy.privateutil.PolicyUtils.Collections;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.ejb.EJB;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;

@Interceptors({SPLoggerInterceptor.class, SPContextInterceptor.class})
@Stateless(name = EjbConstants.SERVICES_EJB, mappedName = EjbConstants.SERVICES_EJB)
@TransactionManagement(TransactionManagementType.BEAN)
public class ServicesEJBImp extends AbstractSPEJB implements ServicesEJB,ServicesEJBLocal {

    private static final Logger logger = Logger.getLogger(ServicesEJBImp.class);
    @EJB
    private TransactionEJBLocal transactionEJB;
    @EJB

    private TopUpProductEJBLocal topUpProductEJB;
    @EJB
    private BillPaymentEJBLocal billPaymentEJB;
    @EJB
    private UserEJBLocal userEJB;
    @EJB
    private CustomerEJBLocal customerEJB;
    @EJB
    private UtilsEJBLocal utilsEJB;
    @EJB
    private ProductEJBLocal productEJB;



    private Transaction saveTransaction(Transaction transaction) throws NullParameterException, GeneralException {
        try {
            transaction = (Transaction) saveEntity(transaction);
        } catch (NullParameterException ex) {
            System.out.println("Entro en save(NullParameterException)");
            ex.printStackTrace();
            throw (ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName(), "General Exception. trying to saving transaction.."), ex);
        }
        return transaction;
    }

    public Account validateAccount(AccountData accountData) throws NullParameterException, InvalidAccountException, DisabledAccountException, GeneralException {

        System.out.println("Login y password que estan llegando+" + accountData.getLogin()+accountData.getPassword()+accountData.getIpRemoteAddress());
        boolean isValid = false;
        Long yourAreAmerica = 3L;
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
            ex.printStackTrace();
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
        if (account.getId().equals(yourAreAmerica)){
            isValid = true;
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

    private List<String> getPromotions(Long serial) {
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

    public ProvissionPinResponse provissionPin(AccountData userData, Customer customer, String phoneNumber, Float amount, String smsDestination, String externalId) throws NullParameterException, InvalidAccountException, DisabledAccountException, InvalidPhoneNumberException, PinAlreadyExistException, InvalidAmountException, GeneralException {
        Account account = validateAccount(userData);
        Long serial = null;
        Long orderId = null;
        //validateCustomerData(customer);
        TransactionStatus transactionStatus = new TransactionStatus(TransactionStatus.PROCESSED);
        TransactionType transactionType = new TransactionType(TransactionType.PIN_PURCHASE);
        if (phoneNumber == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "phoneNumber"), null);
        } else if (amount == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "amount"), null);
        }
        //Valida que sea un numero valido para USA.
        if (!GeneralUtils.isValidUSAPhoneNumber(phoneNumber)) {
            throw new InvalidPhoneNumberException("Invalid phoneNumber: " + phoneNumber);
        }
        if (amount <= 0) {
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
        // Si no existe el usuario se crea uno nuevo
        try {

            Map pinData = contractEJB.provisionDistributionPinPurchase(account.getCustumerServiceIdSisac(), sisacWebUser.getId(), amount, transactionData);
            com.sg123.model.platformprepay.Pin pin = (com.sg123.model.platformprepay.Pin) pinData.get("PIN");
            com.sg123.model.ecommerce.Order order = (com.sg123.model.ecommerce.Order) pinData.get("ORDER");
            //Se validan las promociones de SISAC.save
            List<String> promotions = this.getPromotions(pin.getSerial());
            serial = pin.getSerial();
            orderId = order.getId();
            Long[] anis = new Long[1];
            anis[0] = Long.parseLong(phoneNumber);
            pinEJB.savePinFree(pin.getSerial(), anis);
            List<String> accessNumbers = new ArrayList<String>();
            try {
                accessNumbers = callingCardPinEJB.getAccessNumbersByAni(account.getCustumerServiceIdSisac(), phoneNumber, 1L);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_PURCHASE);
            transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.PROCESSED);
            transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, transactionData, account, pin.getSerial(), null, null, transactionData, externalId, null, order.getId().toString(), null, null,null,0f,userData.getIpRemoteAddress());
            pinResponse = new ProvissionPinResponse(pin.getSerial().toString(), pin.getSecret().toString(), order.getId().toString(), "Transaccion exitosa", promotions, accessNumbers);
        } catch (Exception ex1) {
            ex1.printStackTrace();
            try {
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, ex1.getMessage(), account, serial !=null?serial:null, null, null, transactionData, externalId, null, orderId!=null?orderId.toString():null, null, null,null,0f,userData.getIpRemoteAddress());
                cancelPinProvission(userData, null, null,transaction);
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex1);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
        } finally {
            try {
                this.saveTransaction(transaction);
            } catch (Exception ex) {
                 ex.printStackTrace();
                throw new GeneralException("Exception trying saving transaction in method provissionPin. " + ex.getMessage());
            }
        }
        return pinResponse;
    }

    public ProvissionPinResponse provissionPin(AccountData userData, Customer customer, String phoneNumber, Float amount, String smsDestination, String externalId,String registerUniId,String deliveryAddressId,String billingAddressId,String paymentInfoId,String salesChannelId,String ordenSourceId) throws NullParameterException, InvalidAccountException, DisabledAccountException, InvalidPhoneNumberException, PinAlreadyExistException, InvalidAmountException, GeneralException {
        Account account = validateAccount(userData);
        Long serial = null;
        Long orderId = null;
        //validateCustomerData(customer);
        TransactionStatus transactionStatus = new TransactionStatus(TransactionStatus.PROCESSED);
        TransactionType transactionType = new TransactionType(TransactionType.PIN_PURCHASE);
        if (phoneNumber == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "phoneNumber"), null);
        } else if (amount == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "amount"), null);
        }
        //Valida que sea un numero valido para USA.
        if (!GeneralUtils.isValidUSAPhoneNumber(phoneNumber)) {
            throw new InvalidPhoneNumberException("Invalid phoneNumber: " + phoneNumber);
        }
        if (amount <= 0) {
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
        saveRUUserHasWebUser(userData.getUserId(),sisacWebUser);
        // Si no existe el usuario se crea uno nuevo
        try {

            Map pinData = contractEJB.provisionDistributionPinPurchase(account.getCustumerServiceIdSisac(), sisacWebUser.getId(), amount, transactionData);
            com.sg123.model.platformprepay.Pin pin = (com.sg123.model.platformprepay.Pin) pinData.get("PIN");
            com.sg123.model.ecommerce.Order order = (com.sg123.model.ecommerce.Order) pinData.get("ORDER");
            //Se validan las promociones de SISAC.save
            List<String> promotions = this.getPromotions(pin.getSerial());
            serial = pin.getSerial();
            orderId = order.getId();
            Long[] anis = new Long[1];
            anis[0] = Long.parseLong(phoneNumber);
            pinEJB.savePinFree(pin.getSerial(), anis);
            List<String> accessNumbers = new ArrayList<String>();
            try {
                accessNumbers = callingCardPinEJB.getAccessNumbersByAni(account.getCustumerServiceIdSisac(), phoneNumber, 1L);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_PURCHASE);
            transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.PROCESSED);
            transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, transactionData, account, pin.getSerial(), null, null, transactionData, externalId, null, order.getId().toString(), null, null,null,0f,userData.getIpRemoteAddress());
            pinResponse = new ProvissionPinResponse(pin.getSerial().toString(), pin.getSecret().toString(), order.getId().toString(), "Transaccion exitosa", promotions, accessNumbers);
             try {
                Product product = productEJB.loadProductById(Product.ELECTRONIC_PIN_ID);
                String token = loadTokenOrdenInvoive();
                WsSalesRecordProxy salesRecordProxy = new WsSalesRecordProxy();
                WsOrderResponse orderResponse = salesRecordProxy.generateOrder(token, Constants.ORDER_STATUS_PROCESS, null, registerUniId, deliveryAddressId, billingAddressId, paymentInfoId, sisacWebUser.getId().toString(), transaction.getTotalAmount().toString(),  transaction.getTotalTax().toString(), "0", transaction.getTotalAmount().toString(), "1", pin.getCurrentBalance().toString(), salesChannelId, pin.getCurrency().getId().toString(), ordenSourceId, product.getName(),Product.ELECTRONIC_PIN_ID.toString());
                System.out.println("codeOrder"+orderResponse.getCode());
                if (orderResponse.getCode().equals(Constants.TRANSACTION_SUCCESSFUL)) {
                    String responseOrderId = orderResponse.getId();
                    pinResponse.setOrderId(responseOrderId);

                    WsInvoiceResponse invoiceResponse = salesRecordProxy.generateInvoice(token, Enterprise.ALODIGA_USA.toString(), Constants.INVOICE_TYPE_PIN_LINE, Constants.INVOICE_STATUS_PROCESS, "1", transaction.getTotalTax().toString(), String.valueOf(amount), "0", "0", pin.getCurrentBalance().toString(), transaction.getTotalAmount().toString(), "0", "0", pin.getCurrency().getId().toString(), responseOrderId, "0", "0", null, null, registerUniId);
                    System.out.println("codeInvoice"+invoiceResponse.getCode());
                    if (invoiceResponse.getCode().equals(Constants.TRANSACTION_SUCCESSFUL)) {
                        pinResponse.setInvoiceId(invoiceResponse.getId());
                    }
                }
            } catch (Exception ex1) {
                ex1.printStackTrace();
            }
        } catch (Exception ex1) {
            ex1.printStackTrace();
            try {
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, ex1.getMessage(), account, serial !=null?serial:null, null, null, transactionData, externalId, null, orderId!=null?orderId.toString():null, null, null,null,0f,userData.getIpRemoteAddress());
                cancelPinProvission(userData, null, null,transaction);
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex1);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
        } finally {
            try {
                this.saveTransaction(transaction);
            } catch (Exception ex) {
                 ex.printStackTrace();
                throw new GeneralException("Exception trying saving transaction in method provissionPin. " + ex.getMessage());
            }
        }
        return pinResponse;
    }


    public ProvissionPinResponse provissionPinlessPro(AccountData userData, Customer customer, String phoneNumber, Float amount, String smsDestination, Long rentId, Long favoriteCountryId, String externalId,PaymentInfo paymentInfo,String registerUniId,String deliveryAddressId,String billingAddressId,String paymentInfoId,String salesChannelId,String ordenSourceId) throws NullParameterException, InvalidAccountException, DisabledAccountException, InvalidPhoneNumberException, PinAlreadyExistException, InvalidAmountException, GeneralException,PaymentDeclinedException,InvalidPaymentInfoException,InvalidAccountException, DisabledAccountException {
        Account account = validateAccount(userData);
        Long serial = null;
        Long orderId = null;
        //validateCustomerData(customer);
        com.sg123.model.contract.Customer sisacCustomer = null;
        TransactionStatus transactionStatus = new TransactionStatus(TransactionStatus.PROCESSED);
        TransactionType transactionType = new TransactionType(TransactionType.PINLESS_PURCHASE);
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
        com.sg123.ejb.PlanEJB planEJB = (com.sg123.ejb.PlanEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PLAN_EJB);
        com.sg123.ejb.UtilsEJB utilsEJB = (com.sg123.ejb.UtilsEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.UTILS_EJB);
        com.sg123.ejb.ContentEJB contentEJB = (com.sg123.ejb.ContentEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CONTENT_EJB);
        
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
            sisacCustomer = this.fillSisacCustomer(customer, nowTimestamp, sisacEnterprise);
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
        saveRUUserHasWebUser(Long.parseLong(registerUniId),sisacWebUser);
        Recharge recharge = null;
        // Si no existe el usuario se crea uno nuevo
         try {
            recharge = this.processPayment(account, paymentInfo, amount);
            System.out.println("RechargeID"+recharge.getId()!=null?recharge.getId():"no se guardo la recarga");
        } catch (InvalidAmountException e) {
            try {
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PINLESS_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, e.getMessage(), account, null, null, null, "Error trying purchase pinless. Number = " + phoneNumber + " Procesando el pago con Authorize.net. (MONTO INVALIDO)", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
                throw (e);
            } catch (Exception ex2) {
//                ex2.printStackTrace();
            }
        } catch (PaymentDeclinedException e) {
            try {
                ServiceMailDispatcher.sendPinPurchaseErrorMail(account, "Error durante compra : " + transactionData.toString(), "Paso 2: Procesando el pago con Authorize.net. (PAGO DECLINADO)", e);
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PINLESS_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, e.getMessage(), account, null, null, null,  "Error trying purchase pinless. Number = " + phoneNumber + " Procesando el pago con Authorize.net. (PAGO DECLINADO)", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
            throw (e);
        } catch (InvalidPaymentInfoException e) {
            try {
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PINLESS_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, e.getMessage(), account, null, null, null, "Error trying purchase pinless. Number = " + phoneNumber + " Procesando el pago con Authorize.net. (METODO DE PAGO INVALIDO)", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
                ServiceMailDispatcher.sendPinPurchaseErrorMail(account, "Error durante compra : " + transactionData.toString(), "Paso 2: Procesando el pago con Authorize.net. (METODO DE PAGO INVALIDO)", e);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
            throw (e);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PINLESS_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, e.getMessage(), account, null, null, null, "Error trying purchase pinless. Number = " + phoneNumber + " Procesando el pago con Authorize.net. (ERROR GENERAL)", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                ServiceMailDispatcher.sendPinPurchaseErrorMail(account, "Error durante compra : " + transactionData.toString(), "Paso 2: Procesando el pago con Authorize.net. (ERROR GENERAL)", e);
                this.saveTransaction(transaction);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ""), e);
        }
        try {

            Map pinData = contractEJB.provisionDistributionPinPurchase(account.getCustumerServiceIdSisac(), sisacWebUser.getId(), amount, transactionData);
            com.sg123.model.platformprepay.Pin pin = (com.sg123.model.platformprepay.Pin) pinData.get("PIN");
            com.sg123.model.ecommerce.Order order = (com.sg123.model.ecommerce.Order) pinData.get("ORDER");
            //Se validan las promociones de SISAC.save
            List<String> promotions = this.getPromotions(pin.getSerial());
            serial = pin.getSerial();
            orderId = order.getId();
            Long[] anis = new Long[1];
            anis[0] = Long.parseLong(phoneNumber);
            try {
                System.out.println("**************seriali a guardar"+serial);
                 System.out.println("**************Ani a guardar"+phoneNumber);
                pinEJB.savePinFree(pin.getSerial(), anis);
            } catch (com.sg123.exception.PinFreeQuantityExceededException e) {
                e.printStackTrace();
            }catch (com.sg123.exception.PinNotFoundException e) {
                e.printStackTrace();
            }catch (com.sg123.exception.InvalidAniException e) {
                e.printStackTrace();
            }catch (com.sg123.exception.GeneralException e) {
                e.printStackTrace();
            }
            List<String> accessNumbers = new ArrayList<String>();
            try {
                accessNumbers = callingCardPinEJB.getAccessNumbersByAni(account.getCustumerServiceIdSisac(), phoneNumber, 1L);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            Contract contract = new Contract();
            Timestamp currentTime = new Timestamp(pin.getCreationDate().getTime());
            Rent rent = new Rent();
            rent.setId(rentId);
            
            FavoriteCountry favoriteCountry = planEJB.loadFavoriteCountry(favoriteCountryId);
            
            FavoriteCountryInstance favoriteCountryInstance = new FavoriteCountryInstance();
            favoriteCountryInstance.setFavoriteCountry(favoriteCountry);
            List<FavoriteCountryInstance> favoriteCountryInstances = new ArrayList<FavoriteCountryInstance>();
            favoriteCountryInstances.add(favoriteCountryInstance);
            
            //CONTRACT HAS PIN
            List<ContractHasPin> pins = new ArrayList<ContractHasPin>();
            ContractHasPin chp = new ContractHasPin();
            chp.setPin(pin);
            chp.setRent(rent);
            chp.setFavoriteCountryInstances(favoriteCountryInstances);
            chp.setBeginningDate(currentTime);
            
            
            
            //CONTRACT HAS SERVICE
            ContractHasService chs = new ContractHasService();
//            chs.setProvisionType(ProvisionType.);
            chs.setBeginningDate(pin.getCreationDate());
            chs.setAlias(pin.getSerial().toString());
           
            
            com.sg123.model.content.Service service = new com.sg123.model.content.Service();
            service.setId(8l);
            chs.setService(service);
            chs.setContract(contract);
            
            chp.setContractService(chs);
            pins.add(chp); 
            chs.setContractPins(pins);
            
            
            List<ContractHasService> cServices = new ArrayList<ContractHasService>();
            cServices.add(chs);
            
            
            com.sg123.model.utils.Currency c = new com.sg123.model.utils.Currency();
            c.setId(com.sg123.model.utils.Currency.USD);
             
            Segment segment = contentEJB.loadSegment(Segment.WIXTEL_RESIDENTIAL);
            
            
            contract.setAlias(pin.getSerial().toString());
            System.out.println("**************BAlance");
            System.out.println(amount);
            contract.setBalance(amount);
            contract.setBalanceControl(0l);
            contract.setBusinessNumber(0l);
            contract.setBeginningDate(currentTime);
            contract.setCollectingInfo(" ");
            contract.setContractServices(cServices);
            contract.setCreditLimit(0f);
            contract.setIsPrepaid(0);
            contract.setCurrency(c);
            contract.setCreditDays(0l);
            ContractStatus contractStatus = new ContractStatus();
            contractStatus.setId(ContractStatus.ACTIVO);
            contract.setStatus(contractStatus);
            contract.setPendingBilling(0f);
            contract.setThreshold(0f);
            
            com.sg123.model.acl.User user = new com.sg123.model.acl.User();
            user.setId(1l);
            ContractAction contractAction = new ContractAction();
            contractAction.setId(ContractAction.ACTIVAR);
            
            ContractHistory contractHistory = new ContractHistory();
            contractHistory.setAclUser(user);
            
            
            contractHistory.setAction(contractAction);
            contractHistory.setContract(contract);
            contractHistory.setDescription("Contrato de la empresa ALODIGA creado desde la web");
            contractHistory.setModifiedDate(currentTime);
            
            List<ContractHistory> contractHistories = new ArrayList<ContractHistory>();
            contractHistories.add(contractHistory);
            
            contract.setContractHistories(contractHistories);
          
           
            ContractHasWebUser contractHasWebUser = new ContractHasWebUser();
            contractHasWebUser.setContract(contract);
            contractHasWebUser.setWebUser(sisacWebUser);
            contractHasWebUser.setBeginningDate(new java.sql.Date(new Date().getTime()));
            List<ContractHasWebUser> contractHasWebUsers = new  ArrayList<ContractHasWebUser>();
            contractHasWebUsers.add(contractHasWebUser);
            contract.setContractWebUsers(contractHasWebUsers);
            
            BillingForm bf = new BillingForm();
            bf.setId(BillingForm.AUTOMATICA);
            contract.setBillingForm(bf);
            InvoiceMode invoiceMode = new InvoiceMode();
            invoiceMode.setId(InvoiceMode.PDF);
            contract.setInvoiceMode(invoiceMode);
            if(sisacWebUser.getPrincipalCustomer() != null){
            	CustomerHasWebUser customerHasWebUser = new CustomerHasWebUser();
            	customerHasWebUser.setBeginningDate(currentTime);
            	customerHasWebUser.setCustomer(sisacWebUser.getPrincipalCustomer());
            	customerHasWebUser.setWebUser(sisacWebUser);
            	customerHasWebUser.setIsPrincipal(1l);
            	List<CustomerHasWebUser> customerHasWebUsers = new ArrayList<CustomerHasWebUser>();
            	customerHasWebUsers.add(customerHasWebUser);
            	sisacWebUser.getPrincipalCustomer().setCustomerWebUsers(customerHasWebUsers);
            	contract.setCustomer(sisacWebUser.getPrincipalCustomer());
            }
            else if(sisacCustomer != null)
            	contract.setCustomer(sisacWebUser.getPrincipalCustomer());
            contract.setSegment(segment);
//            com.sg123.model.payment.PaymentInfo paymentInfo1 = new com.sg123.model.payment.PaymentInfo();
//            com.sg123.model.contract.Address address = new com.sg123.model.contract.Address();
//            address.setCountry(new com.sg123.model.utils.Country());
//            address.setState(new com.sg123.model.utils.State());
//            address.setCity(new com.sg123.model.utils.City());
//            address.setCounty(new com.sg123.model.utils.County());
//            paymentInfo1.setBillingAddress(address);
//            paymentInfo1.setCreditCardType(new com.sg123.model.utils.CreditCardType());
//            paymentInfo1.setType((com.sg123.model.utils.PaymentType) ejbCache.find(com.sg123.model.utils.PaymentType.class, com.sg123.model.utils.PaymentType.PAGO_POR_TARJETA_DE_CREDITO));
//            paymentInfo1.setBeginningDate(new java.sql.Date(new java.util.Date().getTime()));
//            contract.getPaymentsInfo().add(paymentInfo1);
            contract.setPaymentInfoIdRU(Long.parseLong(paymentInfoId));
            contract = contractEJB.saveContract(contract);
            
            
            
            
            transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PINLESS_PURCHASE);
            transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.PROCESSED);
            transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, transactionData, account, pin.getSerial(), null, null, transactionData, externalId, recharge, order.getId().toString(), null, null, paymentInfo,0f,userData.getIpRemoteAddress());
//            transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, transactionData, account, pin.getSerial(), null, null, transactionData, externalId, null, order.getId().toString(), null, null,null,0f,userData.getIpRemoteAddress());
            pinResponse = new ProvissionPinResponse(pin.getSerial().toString(), pin.getSecret().toString(), order.getId().toString(), "Transaccion exitosa", promotions, accessNumbers);
//            pinResponse.setReferenCode(pin.getPinsFree().get(0).getId().toString());
            pinResponse.setContractBalance(contract.getBalance());
            pinResponse.setContractBeginningDate(contract.getBeginningDate());
            pinResponse.setContractInvoiceDate(""+contract.getCycle().getEmissionDate());
            pinResponse.setContractStatus(contract.getStatus().getName());
            pinResponse.setContractId(contract.getId());
            pinResponse.setAlias(contract.getAlias());
            
            try {
                Product product = productEJB.loadProductById(Product.PINLESS_ID);
                String token = loadTokenOrdenInvoive();
                WsSalesRecordProxy salesRecordProxy = new WsSalesRecordProxy();
                WsOrderResponse orderResponse = salesRecordProxy.generateOrder(token, Constants.ORDER_STATUS_PROCESS, contract.getId().toString(), registerUniId, deliveryAddressId, billingAddressId, paymentInfoId, sisacWebUser.getId().toString(), transaction.getTotalAmount().toString(),  transaction.getTotalTax().toString(), "0", transaction.getTotalAmount().toString(), "0", pin.getCurrentBalance().toString(), salesChannelId, pin.getCurrency().getId().toString(), ordenSourceId, product.getName(),Product.PINLESS_ID.toString());
                System.out.println("codeOrder"+orderResponse.getCode());
                if (orderResponse.getCode().equals(Constants.TRANSACTION_SUCCESSFUL)) {
                    String responseOrderId = orderResponse.getId();
                    pinResponse.setOrderId(responseOrderId);

                    WsInvoiceResponse invoiceResponse = salesRecordProxy.generateInvoice(token, Enterprise.ALODIGA_USA.toString(), Constants.INVOICE_TYPE_PIN_LESS, Constants.INVOICE_STATUS_PROCESS,contract.getId().toString(), transaction.getTotalTax().toString(), String.valueOf(amount), "0", "0", pin.getCurrentBalance().toString(), transaction.getTotalAmount().toString(), "0", "0", pin.getCurrency().getId().toString(), responseOrderId, "0", "0", null, null, registerUniId);
                    System.out.println("codeInvoice"+invoiceResponse.getCode());
                    if (invoiceResponse.getCode().equals(Constants.TRANSACTION_SUCCESSFUL)) {
                        pinResponse.setInvoiceId(invoiceResponse.getId());
                    }
                }
            } catch (Exception ex1) {
                ex1.printStackTrace();
            }
           } catch (Exception ex1) {
        	System.out.println("**************************************************************");
        	System.out.println("Este log no lo veo");
            ex1.printStackTrace();
            try {
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, ex1.getMessage(), account, serial !=null?serial:null, null, null, transactionData, externalId, null, orderId!=null?orderId.toString():null, null, null,null,0f,userData.getIpRemoteAddress());
                cancelPinProvission(userData, transaction.getId(), recharge.getId(),transaction);
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex1);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
        } finally {
            try {
                this.saveTransaction(transaction);
            } catch (Exception ex) {
                 ex.printStackTrace();
                throw new GeneralException("Exception trying saving transaction in method provissionPin. " + ex.getMessage());
            }
        }
        return pinResponse;
    }
    

    public RechargePinResponse rechargePin(AccountData userData, String phoneNumber, String serial, Float amount, String smsDestination, String externalId) throws NullParameterException, InvalidAccountException, DisabledAccountException, InvalidPhoneNumberException, InvalidAmountException, DisabledPinException, GeneralException {
        Account account = validateAccount(userData);
        TransactionStatus transactionStatus = null;
        TransactionType transactionType = null;
        Long orderId = null;
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
            com.sg123.model.platformprepay.Pin pin = null;
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
            String transactionData = "Recarga Pin Electronico [monto)" + amount + "(monto] - [serial)" + serial + "(serial]" + "[cuenta - login)" + userData.getLogin() + "(cuenta - login]";
            item.setName(transactionData);
            item.setTaxAmount(0F);
            // TODO PREGUNTAR
            item.setUnitAmount(0F);
            item.setTotalAmount(0F);

            String description = "Pin serial: " + pin.getSerial();
            description += "Recharge" + " Amount: " + amount + customService.getCurrency().getSymbol();
            item.setDescription(description);
            shoppingCart.addItem(item);
            shoppingCart.setPaymentInfo(paymentInfo);

            com.sg123.model.ecommerce.Order order = contractEJB.processShoppingCart(shoppingCart);
            order.setSubtotal(0F);
            order.setTaxTotal(0F);
            order.setTotal(0F);
            com.sg123.model.WebUserSession webUserSession = userEJB.loadWebUserSessionByWebUser(pin.getWebUser().getId());
            userEJB.updateWebUserSessionOrder(webUserSession.getId(), order);

            order = contractEJB.rechargeDistributionPinPurchase(account.getCustumerServiceIdSisac(), pin.getWebUser().getId(), order);
            orderId= order.getId();
            transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_RECHARGE);
            transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.PROCESSED);
            transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, description, account, pin.getSerial(), null, null, transactionData, externalId, null, order.getId().toString(), null, null,null,0f,userData.getIpRemoteAddress());
            rechargePinResponse = new RechargePinResponse(pin.getSerial().toString(), pin.getSecret(), "Transaccion Exitosa", String.valueOf(newBalance), String.valueOf(order.getId()));
        } catch (Exception ex) {
              ex.printStackTrace();
            try {
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_RECHARGE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, ex.getMessage(), account, null, null, null, "Error trying recharging pin. Number = " + phoneNumber, externalId, null, orderId!=null?orderId.toString():null, null, null,null,0f,userData.getIpRemoteAddress());
                cancelPinRecharge(userData, null, null,transaction);
                ex.printStackTrace();
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), ex);
            } catch (Exception ex1) {
            }
        } finally {
            try {
                this.saveTransaction(transaction);
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new GeneralException("Exception trying saving transaction in method rechargePin. " + ex.getMessage());
            }
        }
        return rechargePinResponse;
    }

    private Recharge processPayment(Account account, PaymentInfo paymentInfo, float amount) throws NullParameterException, InvalidAmountException, GeneralException, PaymentServiceUnavailableException, PaymentDeclinedException, InvalidPaymentInfoException, InvalidCreditCardDateException {

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
            System.out.println("getCreditcardType**"+paymentInfo.getCreditcardType().getId());
            System.out.println("CreditCardNumber**"+paymentInfo.getCreditCardNumber());
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
                if (!eTransaction.isActive())
                    eTransaction.begin();
                entityManagerWrapper.save(recharge);
                eTransaction.commit();
            } catch (Exception e) {
               e.printStackTrace();
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
            ex.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), null, ex.getMessage()), ex);
        }
    }

    public SearchPinResponse searchPin(AccountData userData, String phoneNumber, String serial) throws NullParameterException, InvalidAccountException, DisabledAccountException, InvalidPhoneNumberException, RegisterNotFoundException, GeneralException, PinDisabledException, PinFreeNotfoundException {
        validateAccount(userData);
        if (serial == null && phoneNumber == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "serial - phoneNumber - customerLogin"), null);
        }
        Contract contract = null;
        SearchPinResponse searchPinResponse = new SearchPinResponse();
        com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);
        com.sg123.ejb.ContractEJB contractEJB = (com.sg123.ejb.ContractEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CONTRACT_EJB);
        try {
            com.sg123.model.platformprepay.Pin pin = null;
            
            if (serial != null) {
                pin = pinEJB.loadPin(new Long(serial));
            } else {//phoneNumber
                pin = pinEJB.getPinFreeByAni(new Long(phoneNumber)).getPin();
                pin = pinEJB.loadPin(pin.getSerial());//Hago el cambio aqui para no hacerlo en SisacEJB - Issue: LAZY Relationship
            }
            try {
            	contract = contractEJB.getContractByPin(pin.getSerial());
            } catch (com.sg123.exception.ContractNotFoundException ex) {
            }
            searchPinResponse.setEnabled(pin.getPinStatus().getId().equals(PinStatus.PIN_AVAILABLE_STATE) ? true : false);
            com.sg123.model.contract.Customer customer = pin.getWebUser().getPrincipalCustomer();
            ResponseCustomer rCustomer = new ResponseCustomer();
            rCustomer.setLogin(pin.getWebUser().getLogin());
            rCustomer.setPassword(pin.getWebUser().getPassword());
            rCustomer.setEnterprideId(pin.getWebUser().getEnterprise().getId().toString());
            if(contract != null){
            	rCustomer.setContractId(contract.getId().toString());
                rCustomer.setContractBalance(contract.getBalance().toString());
                if (pin.getContractPins().get(0).getRent() != null) {
                    PlanResponse planResponse = new PlanResponse();
                    planResponse.setPlanId(pin.getContractPins().get(0).getRent().getId().toString());
                    planResponse.setPlanName(pin.getContractPins().get(0).getRent().getName());
                    for (RentHistory rh : pin.getContractPins().get(0).getRent().getRentHistories()) {
                        if (rh.getEndingDate() == null) {
                            planResponse.setPlanAmount(rh.getAmount().toString());
                            break;
                        }
                    }
                    rCustomer.setPlanResponse(planResponse);
                }
            }
            
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
            rCustomer.setPinSecret(pin.getSecret().toString());
            List<PinResponse> rPinFrees = new ArrayList<PinResponse>();

            for (com.sg123.model.platformprepay.PinFree pinFree : pin.getPinsFree()) {
                PinResponse pinResponse = new PinResponse();
                pinResponse.setCodeId(pinFree.getId().toString());
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

    public CancelPinProvissionResponse cancelPinProvission(AccountData userData, Long transactionId, Long rechargeId, Transaction transaction) throws NullParameterException, InvalidAccountException, DisabledAccountException, RegisterNotFoundException, AmountConsumedException, GeneralException {
        System.out.println("cancelPinProvission " + transaction.getDescription());
        validateAccount(userData);
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
                transaction = transactionEJB.loadTransactionById(transactionId);
                recharge = transaction.getRecharge() != null ? transaction.getRecharge() : null;
                amount = transaction.getTotalAmount();
            } else if (rechargeId != null){
                recharge = transactionEJB.loadRechargeById(rechargeId);
            }
            List<Error> errors = null;
            if (recharge != null && recharge.getResponseCode().equals("1")) {
                if (!cancelPayment(userData, recharge, recharge.getTotalAmount())) {
                    errors.add(new Error("Error in the method cancelPayment"));
                    cancelPinProvissionResponse.setErrors(errors);
                    return cancelPinProvissionResponse;
                }
            }
            if (transaction != null && transaction.getPinSerial()!=null && transaction.getReferenceProviderCode()!=null) {
                System.out.println("cancelPinProvission : transaction != null");
                Long serial = transaction.getPinSerial();
                System.out.println("serial : " +serial);
                com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);
                com.sg123.ejb.ContractEJB contractEJB = (com.sg123.ejb.ContractEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CONTRACT_EJB);
                com.sg123.model.platformprepay.Pin pin = pinEJB.loadPin(serial);
                List<com.sg123.model.platformprepay.PinFree> pFrees = pin.getPinsFree();
                Long orderId = Long.parseLong(transaction.getReferenceProviderCode());
                System.out.println("orderId : " + orderId);
                for (com.sg123.model.platformprepay.PinFree pfree : pFrees) {
                    if (!contractEJB.authorizeCancelDistributionOrder(orderId)) {
                        throw new InvalidAmountException("Amount consumed");
                    }
                    System.out.println("pinfree a eliminar" +pfree.getAni().toString());
                    deletePinFree(userData, pfree.getAni().toString());
                }
                System.out.println("pin a eliminar" +serial);
                pinEJB.deletePin(serial);
                contractEJB.changeOrderStatus(orderId, OrderStatus.DEVUELTA);
                cancelPinProvissionResponse = new CancelPinProvissionResponse(Constants.RESPONSE_SUCCESS);
                TransactionStatus transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.CANCELED);
                transaction.setTransactionStatus(transactionStatus);
                try {
                    this.saveTransaction(transaction);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    throw new GeneralException("Exception trying saving transaction in method cancelPinProvission. " + ex.getMessage());
                }
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

    public CancelPinRechargeResponse cancelPinRecharge(AccountData userData, Long transactionId, Long rechargeId,Transaction transaction) throws NullParameterException, InvalidAccountException, DisabledAccountException, RegisterNotFoundException, AmountConsumedException, GeneralException {
        validateAccount(userData);
        CancelPinRechargeResponse cancelPinRechargeResponse = null;
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
            recharge = transaction.getRecharge() != null ? transaction.getRecharge() : null;
            amount = transaction.getTotalAmount();
        } else if (rechargeId != null) {
            recharge = transactionEJB.loadRechargeById(rechargeId);
        }
        List<Error> errors = null;
        if (recharge != null && recharge.getResponseCode().equals("1")) {
            try {
                if (!cancelPayment(userData, recharge, recharge.getTotalAmount())) {
                    errors.add(new Error("Error in the method cancelPayment"));
                    cancelPinRechargeResponse.setErrors(errors);
                    return cancelPinRechargeResponse;
                }
            } catch (CancelPaymentException ex) {
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
            }
        }
        if (transaction != null && transaction.getReferenceProviderCode()!=null) {
            Long orderId = Long.parseLong(transaction.getReferenceProviderCode());
            try {
                if (!contractEJB.authorizeCancelDistributionOrder(orderId)) {
                    throw new InvalidAmountException("Amount consumed");
                }
                contractEJB.cancelOrder(orderId);
                cancelPinRechargeResponse = new CancelPinRechargeResponse(Constants.RESPONSE_SUCCESS);
                TransactionStatus transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.CANCELED);
                transaction.setTransactionStatus(transactionStatus);
                try {
                    this.saveTransaction(transaction);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    throw new GeneralException("Exception trying saving transaction in method cancelPinRecharge. " + ex.getMessage());
                }
            } catch (InvalidAmountException e) {
                e.printStackTrace();
                throw new AmountConsumedException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
            } catch (Exception e) {
                e.printStackTrace();
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
            }
        }
        return cancelPinRechargeResponse;
    }

    public List<Country> getTopUpCountries(AccountData userData) throws NullParameterException, InvalidAccountException, DisabledAccountException, EmptyListException, GeneralException {
        validateAccount(userData);
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

    public List<MobileOperator> getMobileOperatorsByCountryId(AccountData userData, Long countryId) throws NullParameterException, InvalidAccountException, DisabledAccountException, DisabledPinException, EmptyListException, GeneralException {
        validateAccount(userData);
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

    public List<TopUpProduct> getTopUpProductByMobileOperatorId(AccountData userData, Long mobileOperatorId) throws NullParameterException, InvalidAccountException, DisabledAccountException, EmptyListException, GeneralException {
        Account account = validateAccount(userData);
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

    public GeneralTopUpResponse processTopUp(AccountData userData, TopUpProduct topUpProduct, String destinationNumber, String externalId, String senderNumber, Customer customer, Long languageId, boolean sendSMS, String destinationSMS) throws NullParameterException, InvalidAccountException, DisabledAccountException, TopUpTransactionException, GeneralException, TopUpTransactionException, GeneralException, TopUpProductNotAvailableException, InvalidFormatException, NotAvaliableServiceException, DestinationNotPrepaidException, DuplicatedTransactionException, CarrierSystemUnavailableException, InvalidPhoneNumberException, InvalidAmountException, MaxNumberTransactionDailyPerDestinationException, MaxNumberTransactionDailyPerSenderException {
        Account account = validateAccount(userData);
        GeneralTopUpResponse response = new GeneralTopUpResponse();
        Transaction transaction = null;
        Date now = new Date((new java.util.Date()).getTime());
        StringBuilder transactionData = new StringBuilder();
        Timestamp nowTimestamp = new Timestamp(now.getTime());
        try {
            //Se egrego cuando se incorporó distribución.
            if(!(userData.getIpRemoteAddress().equals("192.168.201.64") || userData.getIpRemoteAddress().equals("192.168.201.59")) ){
                validarDestinationNumberAndSenderNumber(destinationNumber, senderNumber);
                System.out.println("entro..................................... a la validacion");
            }
            System.out.println(" NO....entro..................................... a la validacion");
            response = topUpProductEJB.executeTopUp(topUpProduct, destinationNumber, externalId, senderNumber, account, languageId, sendSMS, destinationSMS);
            transactionData.append("TopUp: ").append(topUpProduct.getName()).append("( ").append(topUpProduct.getId()).append(" )").append(" Operadora : ").append(topUpProduct.getMobileOperator().getName()).append(" Denominacion: ").append(topUpProduct.getProductDenomination().getAmount()).append("PhoneNumber: ").append(destinationNumber);
            transaction = new Transaction(new TransactionType(TransactionType.TOP_UP_PURCHASE), new TransactionStatus(TransactionStatus.PROCESSED), topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, transactionData.toString(), account, null, topUpProduct, null, transactionData.toString(), externalId, null, "", destinationNumber, senderNumber,null,0f,userData.getIpRemoteAddress());
        } catch (MaxNumberTransactionDailyPerDestinationException ex) {
            transaction = new Transaction(new TransactionType(TransactionType.TOP_UP_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, null, account, null, topUpProduct, null, null, externalId, null, "", null, null,null,0f,userData.getIpRemoteAddress());
            throw ex;
        } catch (MaxNumberTransactionDailyPerSenderException ex) {
            transaction = new Transaction(new TransactionType(TransactionType.TOP_UP_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, null, account, null, topUpProduct, null, null, externalId, null, "", null, null,null,0f,userData.getIpRemoteAddress());
            throw ex;
        } catch (TopUpTransactionException tupEx) {
            tupEx.printStackTrace();
            transactionData.append("TopUp: ").append(topUpProduct.getName()).append("( ").append(topUpProduct.getId()).append(" )").append(" Operadora : ").append(topUpProduct.getMobileOperator().getName()).append(" Denominacion: ").append(topUpProduct.getProductDenomination().getAmount()).append("PhoneNumber: ").append(destinationNumber);
            try {
                response = topUpProductEJB.executeBackUpTopUp(topUpProduct, destinationNumber, externalId, senderNumber, account, languageId, sendSMS, destinationSMS);
                transaction = new Transaction(new TransactionType(TransactionType.TOP_UP_PURCHASE), new TransactionStatus(TransactionStatus.PROCESSED), topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, transactionData.toString(), account, null, topUpProduct, null, transactionData.toString(), externalId, null, "", destinationNumber, senderNumber, null, 0f, userData.getIpRemoteAddress());
            } catch (Exception ex) {
                ex.printStackTrace();
                transaction = new Transaction(new TransactionType(TransactionType.TOP_UP_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, "TopUp fallido al número "+destinationNumber+" Operadora"+topUpProduct.getMobileOperator().getName() + " Monto: "+topUpProduct.getProductDenomination().getAmount(), account, null, topUpProduct, null, null, externalId, null, "", destinationNumber, senderNumber, null, 0f, userData.getIpRemoteAddress());
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
            }
        } catch (Exception ex) {
            transaction = new Transaction(new TransactionType(TransactionType.TOP_UP_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, "TopUp fallido al número "+destinationNumber+" Operadora"+topUpProduct.getMobileOperator().getName() + " Monto: "+topUpProduct.getProductDenomination().getAmount(), account, null, topUpProduct, null, null, externalId, null, "",destinationNumber, senderNumber, null,0f,userData.getIpRemoteAddress());
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
        } finally {
            try {
                this.saveTransaction(transaction);
            } catch (Exception ex) {
                throw new GeneralException("Exception trying saving transaction in method provissionPin. " + ex.getMessage());
            }
        }
        return response;
    }

     public GeneralTopUpResponse processTopUp(AccountData userData, TopUpProduct topUpProduct, String destinationNumber, String externalId, String senderNumber, Customer customer, Long languageId, boolean sendSMS, String destinationSMS,String registerUniId,String deliveryAddressId,String billingAddressId,String paymentInfoId,String salesChannelId,String ordenSourceId) throws NullParameterException, InvalidAccountException, DisabledAccountException, TopUpTransactionException, GeneralException, TopUpTransactionException, GeneralException, TopUpProductNotAvailableException, InvalidFormatException, NotAvaliableServiceException, DestinationNotPrepaidException, DuplicatedTransactionException, CarrierSystemUnavailableException, InvalidPhoneNumberException, InvalidAmountException, MaxNumberTransactionDailyPerDestinationException, MaxNumberTransactionDailyPerSenderException {
        Account account = validateAccount(userData);
        GeneralTopUpResponse response = new GeneralTopUpResponse();
        Transaction transaction = null;
        Date now = new Date((new java.util.Date()).getTime());
        StringBuilder transactionData = new StringBuilder();
        Timestamp nowTimestamp = new Timestamp(now.getTime());
        try {
            //Se egrego cuando se incorporó distribución.
            if(!(userData.getIpRemoteAddress().equals("192.168.201.64") || userData.getIpRemoteAddress().equals("192.168.201.59")) ){
                validarDestinationNumberAndSenderNumber(destinationNumber, senderNumber);
                System.out.println("entro..................................... a la validacion");
            }
            System.out.println(" NO....entro..................................... a la validacion");
            response = topUpProductEJB.executeTopUp(topUpProduct, destinationNumber, externalId, senderNumber, account, languageId, sendSMS, destinationSMS);
            transactionData.append("TopUp: ").append(topUpProduct.getName()).append("( ").append(topUpProduct.getId()).append(" )").append(" Operadora : ").append(topUpProduct.getMobileOperator().getName()).append(" Denominacion: ").append(topUpProduct.getProductDenomination().getAmount()).append("PhoneNumber: ").append(destinationNumber);
            transaction = new Transaction(new TransactionType(TransactionType.TOP_UP_PURCHASE), new TransactionStatus(TransactionStatus.PROCESSED), topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, transactionData.toString(), account, null, topUpProduct, null, transactionData.toString(), externalId, null, "", destinationNumber, senderNumber,null,0f,userData.getIpRemoteAddress());
        } catch (MaxNumberTransactionDailyPerDestinationException ex) {
            transaction = new Transaction(new TransactionType(TransactionType.TOP_UP_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, null, account, null, topUpProduct, null, null, externalId, null, "", null, null,null,0f,userData.getIpRemoteAddress());
            throw ex;
        } catch (MaxNumberTransactionDailyPerSenderException ex) {
            transaction = new Transaction(new TransactionType(TransactionType.TOP_UP_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, null, account, null, topUpProduct, null, null, externalId, null, "", null, null,null,0f,userData.getIpRemoteAddress());
            throw ex;
        } catch (TopUpTransactionException tupEx) {
            tupEx.printStackTrace();
            transactionData.append("TopUp: ").append(topUpProduct.getName()).append("( ").append(topUpProduct.getId()).append(" )").append(" Operadora : ").append(topUpProduct.getMobileOperator().getName()).append(" Denominacion: ").append(topUpProduct.getProductDenomination().getAmount()).append("PhoneNumber: ").append(destinationNumber);
            try {
                response = topUpProductEJB.executeBackUpTopUp(topUpProduct, destinationNumber, externalId, senderNumber, account, languageId, sendSMS, destinationSMS);
                transaction = new Transaction(new TransactionType(TransactionType.TOP_UP_PURCHASE), new TransactionStatus(TransactionStatus.PROCESSED), topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, transactionData.toString(), account, null, topUpProduct, null, transactionData.toString(), externalId, null, "", destinationNumber, senderNumber, null, 0f, userData.getIpRemoteAddress());
                try {
                    Product product = productEJB.loadProductById(Product.TOP_UP_PRODUCT_ID);
                    String token = loadTokenOrdenInvoive();
                    WsSalesRecordProxy salesRecordProxy = new WsSalesRecordProxy();
                    WsOrderResponse orderResponse = salesRecordProxy.generateOrder(token, Constants.ORDER_STATUS_PROCESS, null, registerUniId, deliveryAddressId, billingAddressId, paymentInfoId, registerUniId, transaction.getTotalAmount().toString(), transaction.getTotalTax().toString(), "0", transaction.getTotalAmount().toString(), "1", null, salesChannelId, topUpProduct.getProductDenomination().getCurrency().getId().toString(), ordenSourceId, Product.ELECTRONIC_PIN_ID.toString(), product.getName());
                    if (orderResponse.getCode().equals(Constants.TRANSACTION_SUCCESSFUL)) {
                        String responseOrderId = orderResponse.getId();
                        response.setOrderId(responseOrderId);
                        WsInvoiceResponse invoiceResponse = salesRecordProxy.generateInvoice(token, Enterprise.ALODIGA_USA.toString(), Constants.INVOICE_TYPE_PIN_LINE, Constants.INVOICE_STATUS_PROCESS, null, transaction.getTotalTax().toString(), String.valueOf(topUpProduct.getProductDenomination().getAmount()), "0", "0", null, transaction.getTotalAmount().toString(), "0", "0", topUpProduct.getProductDenomination().getCurrency().getId().toString(), responseOrderId, null, null, null, null, registerUniId);
                        System.out.println("codeInvoice"+invoiceResponse.getCode());
                        if (invoiceResponse.getCode().equals(Constants.TRANSACTION_SUCCESSFUL)) {
                            response.setInvoiceId(invoiceResponse.getId());
                        }
                    }
                } catch (Exception ex1) {
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                transaction = new Transaction(new TransactionType(TransactionType.TOP_UP_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, "TopUp fallido al número "+destinationNumber+" Operadora"+topUpProduct.getMobileOperator().getName() + " Monto: "+topUpProduct.getProductDenomination().getAmount(), account, null, topUpProduct, null, null, externalId, null, "", destinationNumber, senderNumber, null, 0f, userData.getIpRemoteAddress());
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
            }
        } catch (Exception ex) {
            transaction = new Transaction(new TransactionType(TransactionType.TOP_UP_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, "TopUp fallido al número "+destinationNumber+" Operadora"+topUpProduct.getMobileOperator().getName() + " Monto: "+topUpProduct.getProductDenomination().getAmount(), account, null, topUpProduct, null, null, externalId, null, "",destinationNumber, senderNumber, null,0f,userData.getIpRemoteAddress());
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
        } finally {
            try {
                this.saveTransaction(transaction);
            } catch (Exception ex) {
                throw new GeneralException("Exception trying saving transaction in method provissionPin. " + ex.getMessage());
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


    private void validarDestinationNumberAndSenderNumber(String destinationNumber, String senderNumber) throws MaxNumberTransactionDailyPerDestinationException, MaxNumberTransactionDailyPerSenderException {
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
        //numberTransaccionPerDestinationNumber = (Long) entityManager.createNativeQuery(sql).getSingleResult();
        if (numberTransaccionPerDestinationNumber >= 7) {
            throw new MaxNumberTransactionDailyPerDestinationException("MaxNumberTransactionDailyPerDestinationException");
        }
        sql = "SELECT COUNT(t.id) FROM services.transaction t WHERE t.topUpSender =" + senderNumber + " AND t.transactionTypeId=3 AND t.transactionstatusId=1 AND t.creationDate BETWEEN DATE('" + nowBeginningDate + "') AND ('" + nowEndingDate + "')";
        result = new ArrayList();
        result = (List) entityManager.createNativeQuery(sql).getSingleResult();
        numberTransaccionPerSenderNumber = (Long) result.get(0);
        //numberTransaccionPerSenderNumber = (Long) entityManager.createNativeQuery(sql).getSingleResult();

        if (numberTransaccionPerSenderNumber >= 7) {
            throw new MaxNumberTransactionDailyPerSenderException("MaxNumberTransactionDailyPerSenderException");
        }

    }

    public GeneralTopUpResponse processTopUp(AccountData userData, PaymentInfo paymentInfo, TopUpProduct topUpProduct, String destinationNumber, String externalId, String senderNumber, Customer customer, Long languageId, boolean sendSMS, String destinationSMS) throws NullParameterException, InvalidAccountException, DisabledAccountException, PaymentDeclinedException, InvalidPaymentInfoException, TopUpTransactionException, GeneralException, TopUpTransactionException, GeneralException, TopUpProductNotAvailableException, InvalidFormatException, NotAvaliableServiceException, DestinationNotPrepaidException, DuplicatedTransactionException, CarrierSystemUnavailableException, InvalidPhoneNumberException, InvalidAmountException, MaxNumberTransactionDailyPerDestinationException, MaxNumberTransactionDailyPerSenderException {
        System.out.println("processTopUp");
        Account account = validateAccount(userData);
        float numberOfTransaction = 0f;
//        float numberTransaction = 0f;
        GeneralTopUpResponse response = new GeneralTopUpResponse();
        Transaction transaction = null;
        Date now = new Date((new java.util.Date()).getTime());
        StringBuilder transactionData = new StringBuilder();
        Timestamp nowTimestamp = new Timestamp(now.getTime());
        Recharge recharge = null;
        try {
            validarDestinationNumberAndSenderNumber(destinationNumber, senderNumber);
        } catch (MaxNumberTransactionDailyPerDestinationException ex) {
            throw ex;
        } catch (MaxNumberTransactionDailyPerSenderException ex) {
            throw ex;
        }
        TransactionType transactionType = null;
        TransactionStatus transactionStatus = null;
        try {
            recharge = this.processPayment(account, paymentInfo, topUpProduct.getProductDenomination().getAmount());
        } catch (InvalidAmountException ex) {
            try {
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.TOP_UP_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, ex.getMessage(), account, null, null, null, "Paso 1: Error ejecutando el TopUp: " + destinationNumber + "Procesando el pago con Authorize.net (Monto Invalido).", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
            throw (ex);
        } catch (PaymentDeclinedException ex) {
            try {
                ServiceMailDispatcher.sendTopUpErrorMail(account, destinationNumber, senderNumber, topUpProduct, "Paso 1: Procesando el pago con Authorize.net (PAGO DECLINADO).", ex);
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.TOP_UP_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, ex.getMessage(), account, null, null, null, "Paso 1: Error ejecutando el TopUp: " + destinationNumber + "Procesando el pago con Authorize.net (PAGO DECLINADO).", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
            throw (ex);
        } catch (InvalidPaymentInfoException ex) {
            try {
                ServiceMailDispatcher.sendTopUpErrorMail(account, destinationNumber, senderNumber, topUpProduct, "Paso 1: Procesando el pago con Authorize.net (FORMATO DE PAGO INVALIDO).", ex);
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.TOP_UP_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, ex.getMessage(), account, null, null, null, "Paso 1: Error ejecutando el TopUp: " + destinationNumber + "Procesando el pago con Authorize.net (FORMATO DE PAGO INVALIDO).", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
            throw (ex);
        } catch (Exception ex) {
            try {
                ServiceMailDispatcher.sendTopUpErrorMail(account, destinationNumber, senderNumber, topUpProduct, "Paso 1: Procesando el pago con Authorize.net (ERROR GENERAL).", ex);
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.TOP_UP_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, ex.getMessage(), account, null, null, null, "Paso 1: Error ejecutando el TopUp: " + destinationNumber + "Procesando el pago con Authorize.net (ERROR GENERAL).", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
            } catch (Exception ex2) {
                ex2.printStackTrace();
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ""), ex);
            }
             throw new GeneralException("Exception trying saving transaction in method provissionPin. " + ex.getMessage());
        }
        try {
            response = topUpProductEJB.executeTopUp(topUpProduct, destinationNumber, externalId, senderNumber, account, languageId, sendSMS, destinationSMS);
            transactionData.append("TopUp: ").append(topUpProduct.getName()).append("( ").append(topUpProduct.getId()).append(" )").append(" Operadora : ").append(topUpProduct.getMobileOperator().getName()).append(" Denominacion: ").append(topUpProduct.getProductDenomination().getAmount()).append("PhoneNumber: ").append(destinationNumber);
            transaction = new Transaction(new TransactionType(TransactionType.TOP_UP_PURCHASE), new TransactionStatus(TransactionStatus.PROCESSED), topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, transactionData.toString(), account, null, topUpProduct, null, transactionData.toString(), externalId, recharge, "", destinationNumber, senderNumber, paymentInfo,0f,userData.getIpRemoteAddress());
        } catch (TopUpTransactionException tupEx) {
            tupEx.printStackTrace();
            transactionData.append("TopUp: ").append(topUpProduct.getName()).append("( ").append(topUpProduct.getId()).append(" )").append(" Operadora : ").append(topUpProduct.getMobileOperator().getName()).append(" Denominacion: ").append(topUpProduct.getProductDenomination().getAmount()).append("PhoneNumber: ").append(destinationNumber);
            response = topUpProductEJB.executeBackUpTopUp(topUpProduct, destinationNumber, externalId, senderNumber, account, languageId, sendSMS, destinationSMS);
            transaction = new Transaction(new TransactionType(TransactionType.TOP_UP_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, "Paso 2: Error tratando de tratando de ejecutar el TopUp"+destinationNumber+" (ERROR GENERAL).", account, null, topUpProduct, null, transactionData.toString(), externalId, recharge, "", destinationNumber, senderNumber, paymentInfo,0f,userData.getIpRemoteAddress());
            ServiceMailDispatcher.sendTopUpErrorMail(account, destinationNumber, senderNumber, topUpProduct, "Paso 1: Error en la compra de TopUp.", tupEx);
        } catch (Exception ex) {
            ex.printStackTrace();
            transaction = new Transaction(new TransactionType(TransactionType.TOP_UP_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, null, account, null, topUpProduct, null, null, externalId, null, "", destinationNumber, senderNumber, paymentInfo,0f,userData.getIpRemoteAddress());
            try {
                if (recharge != null && recharge.getResponseCode().equals("1")) {
                    cancelPayment(userData, transaction);
                }
            } catch (Exception e) {
                ServiceMailDispatcher.sendTopUpErrorMail(account, destinationNumber, senderNumber, topUpProduct, "Paso 2: Cancelando el pago con Authorize.net por error en compra de TopUp.", ex);
                throw new GeneralException("Exception tratando de cancelar una transacción erronea de TopUp. " + e.getMessage());
            }
            ServiceMailDispatcher.sendTopUpErrorMail(account, destinationNumber, senderNumber, topUpProduct, "Paso 2: Error Procesando la transaccion de TopUp.", ex);
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
        } finally {
            try {
                this.saveTransaction(transaction);
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new GeneralException("Exception trying saving transaction in method provissionPin. " + ex.getMessage());
            }
            numberOfTransaction = this.numberOfTransaction(account.getId(), topUpProduct.getId());
            System.out.println("numberOfTransaction++++++"+numberOfTransaction);
            if (numberOfTransaction % Constants.MAX_NUMBER_TRANSACTION == 0) {
                ServiceMailDispatcher.sendAlertMail(account, topUpProduct, numberOfTransaction);
                ServiceSMSDispatcher.sendAlertTopUpSMS(account, topUpProduct, numberOfTransaction);
            }
        }
        return response;
    }

     public GeneralTopUpResponse processTopUp(AccountData userData, PaymentInfo paymentInfo, TopUpProduct topUpProduct, String destinationNumber, String externalId, String senderNumber, Customer customer, Long languageId, boolean sendSMS, String destinationSMS,String registerUniId,String deliveryAddressId,String billingAddressId,String paymentInfoId,String salesChannelId,String ordenSourceId) throws NullParameterException, InvalidAccountException, DisabledAccountException, PaymentDeclinedException, InvalidPaymentInfoException, TopUpTransactionException, GeneralException, TopUpTransactionException, GeneralException, TopUpProductNotAvailableException, InvalidFormatException, NotAvaliableServiceException, DestinationNotPrepaidException, DuplicatedTransactionException, CarrierSystemUnavailableException, InvalidPhoneNumberException, InvalidAmountException, MaxNumberTransactionDailyPerDestinationException, MaxNumberTransactionDailyPerSenderException {
        System.out.println("processTopUp");
        Account account = validateAccount(userData);
        float numberOfTransaction = 0f;
//        float numberTransaction = 0f;
        GeneralTopUpResponse response = new GeneralTopUpResponse();
        Transaction transaction = null;
        Date now = new Date((new java.util.Date()).getTime());
        StringBuilder transactionData = new StringBuilder();
        Timestamp nowTimestamp = new Timestamp(now.getTime());
        Recharge recharge = null;
        try {
            validarDestinationNumberAndSenderNumber(destinationNumber, senderNumber);
        } catch (MaxNumberTransactionDailyPerDestinationException ex) {
            throw ex;
        } catch (MaxNumberTransactionDailyPerSenderException ex) {
            throw ex;
        }
        TransactionType transactionType = null;
        TransactionStatus transactionStatus = null;
        try {
            recharge = this.processPayment(account, paymentInfo, topUpProduct.getProductDenomination().getAmount());
        } catch (InvalidAmountException ex) {
            try {
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.TOP_UP_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, ex.getMessage(), account, null, null, null, "Paso 1: Error ejecutando el TopUp: " + destinationNumber + "Procesando el pago con Authorize.net (Monto Invalido).", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
            throw (ex);
        } catch (PaymentDeclinedException ex) {
            try {
                ServiceMailDispatcher.sendTopUpErrorMail(account, destinationNumber, senderNumber, topUpProduct, "Paso 1: Procesando el pago con Authorize.net (PAGO DECLINADO).", ex);
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.TOP_UP_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, ex.getMessage(), account, null, null, null, "Paso 1: Error ejecutando el TopUp: " + destinationNumber + "Procesando el pago con Authorize.net (PAGO DECLINADO).", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
            throw (ex);
        } catch (InvalidPaymentInfoException ex) {
            try {
                ServiceMailDispatcher.sendTopUpErrorMail(account, destinationNumber, senderNumber, topUpProduct, "Paso 1: Procesando el pago con Authorize.net (FORMATO DE PAGO INVALIDO).", ex);
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.TOP_UP_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, ex.getMessage(), account, null, null, null, "Paso 1: Error ejecutando el TopUp: " + destinationNumber + "Procesando el pago con Authorize.net (FORMATO DE PAGO INVALIDO).", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
            throw (ex);
        } catch (Exception ex) {
            try {
                ServiceMailDispatcher.sendTopUpErrorMail(account, destinationNumber, senderNumber, topUpProduct, "Paso 1: Procesando el pago con Authorize.net (ERROR GENERAL).", ex);
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.TOP_UP_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, ex.getMessage(), account, null, null, null, "Paso 1: Error ejecutando el TopUp: " + destinationNumber + "Procesando el pago con Authorize.net (ERROR GENERAL).", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
            } catch (Exception ex2) {
                ex2.printStackTrace();
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ""), ex);
            }
             throw new GeneralException("Exception trying saving transaction in method provissionPin. " + ex.getMessage());
        }
        try {
            response = topUpProductEJB.executeTopUp(topUpProduct, destinationNumber, externalId, senderNumber, account, languageId, sendSMS, destinationSMS);
            transactionData.append("TopUp: ").append(topUpProduct.getName()).append("( ").append(topUpProduct.getId()).append(" )").append(" Operadora : ").append(topUpProduct.getMobileOperator().getName()).append(" Denominacion: ").append(topUpProduct.getProductDenomination().getAmount()).append("PhoneNumber: ").append(destinationNumber);
            transaction = new Transaction(new TransactionType(TransactionType.TOP_UP_PURCHASE), new TransactionStatus(TransactionStatus.PROCESSED), topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, transactionData.toString(), account, null, topUpProduct, null, transactionData.toString(), externalId, recharge, "", destinationNumber, senderNumber, paymentInfo,0f,userData.getIpRemoteAddress());
            try {
                Product product = productEJB.loadProductById(Product.TOP_UP_PRODUCT_ID);
                String token = loadTokenOrdenInvoive();
                WsSalesRecordProxy salesRecordProxy = new WsSalesRecordProxy();
                WsOrderResponse orderResponse = salesRecordProxy.generateOrder(token, Constants.ORDER_STATUS_PROCESS, "0", registerUniId, deliveryAddressId, billingAddressId, paymentInfoId, registerUniId, transaction.getTotalAmount().toString(), transaction.getTotalTax().toString(), "0", transaction.getTotalAmount().toString(), "1", "0", salesChannelId, topUpProduct.getProductDenomination().getCurrency().getId().toString(), ordenSourceId,product.getName(), Product.TOP_UP_PRODUCT_ID.toString());
                System.out.println("codeOrder" + orderResponse.getCode());
                if (orderResponse.getCode().equals(Constants.TRANSACTION_SUCCESSFUL)) {
                    String responseOrderId = orderResponse.getId();
                    response.setOrderId(responseOrderId);
                    WsInvoiceResponse invoiceResponse = salesRecordProxy.generateInvoice(token, Enterprise.ALODIGA_USA.toString(), Constants.INVOICE_TYPE_PIN_LINE, Constants.INVOICE_STATUS_PROCESS, "0", transaction.getTotalTax().toString(), String.valueOf(topUpProduct.getProductDenomination().getAmount()), "0", "0", "0", transaction.getTotalAmount().toString(), "0", "0", topUpProduct.getProductDenomination().getCurrency().getId().toString(), responseOrderId, null, null, null, null, registerUniId);
                    System.out.println("codeInvoice" + invoiceResponse.getCode());
                    if (invoiceResponse.getCode().equals(Constants.TRANSACTION_SUCCESSFUL)) {
                        response.setInvoiceId(invoiceResponse.getId());
                    }
                }
            } catch (Exception ex1) {
            }
        } catch (TopUpTransactionException tupEx) {
            tupEx.printStackTrace();
            transactionData.append("TopUp: ").append(topUpProduct.getName()).append("( ").append(topUpProduct.getId()).append(" )").append(" Operadora : ").append(topUpProduct.getMobileOperator().getName()).append(" Denominacion: ").append(topUpProduct.getProductDenomination().getAmount()).append("PhoneNumber: ").append(destinationNumber);
            response = topUpProductEJB.executeBackUpTopUp(topUpProduct, destinationNumber, externalId, senderNumber, account, languageId, sendSMS, destinationSMS);
            transaction = new Transaction(new TransactionType(TransactionType.TOP_UP_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, "Paso 2: Error tratando de tratando de ejecutar el TopUp"+destinationNumber+" (ERROR GENERAL).", account, null, topUpProduct, null, transactionData.toString(), externalId, recharge, "", destinationNumber, senderNumber, paymentInfo,0f,userData.getIpRemoteAddress());
            ServiceMailDispatcher.sendTopUpErrorMail(account, destinationNumber, senderNumber, topUpProduct, "Paso 1: Error en la compra de TopUp.", tupEx);
        } catch (Exception ex) {
            ex.printStackTrace();
            transaction = new Transaction(new TransactionType(TransactionType.TOP_UP_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), topUpProduct.getProductDenomination().getAmount(), 0f, 0f, nowTimestamp, null, account, null, topUpProduct, null, null, externalId, null, "", destinationNumber, senderNumber, paymentInfo,0f,userData.getIpRemoteAddress());
            try {
                if (recharge != null && recharge.getResponseCode().equals("1")) {
                    cancelPayment(userData, transaction);
                }
            } catch (Exception e) {
                ServiceMailDispatcher.sendTopUpErrorMail(account, destinationNumber, senderNumber, topUpProduct, "Paso 2: Cancelando el pago con Authorize.net por error en compra de TopUp.", ex);
                throw new GeneralException("Exception tratando de cancelar una transacción erronea de TopUp. " + e.getMessage());
            }
            ServiceMailDispatcher.sendTopUpErrorMail(account, destinationNumber, senderNumber, topUpProduct, "Paso 2: Error Procesando la transaccion de TopUp.", ex);
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
        } finally {
            try {
                this.saveTransaction(transaction);
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new GeneralException("Exception trying saving transaction in method provissionPin. " + ex.getMessage());
            }
            numberOfTransaction = this.numberOfTransaction(account.getId(), topUpProduct.getId());
            System.out.println("numberOfTransaction++++++"+numberOfTransaction);
            if (numberOfTransaction % Constants.MAX_NUMBER_TRANSACTION == 0) {
                ServiceMailDispatcher.sendAlertMail(account, topUpProduct, numberOfTransaction);
                ServiceSMSDispatcher.sendAlertTopUpSMS(account, topUpProduct, numberOfTransaction);
            }
        }
        return response;
    }

    public List<Country> getBillPaymentCountries(AccountData userData) throws NullParameterException, InvalidAccountException, DisabledAccountException, EmptyListException, GeneralException {
        validateAccount(userData);
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

    public List<BillPaymentProduct> getBillPaymentProductsByCountryId(AccountData userData, Long countryId) throws NullParameterException, InvalidAccountException, DisabledAccountException, EmptyListException, GeneralException {
        Account account = validateAccount(userData);
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
//            commission = billPaymentEJB.getBillPaymentCalculationByAccountId(account.getId());
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

    public List<CreditcardType> getCreditcardTypes(AccountData userData) throws NullParameterException, InvalidAccountException, DisabledAccountException, EmptyListException, GeneralException {
        validateAccount(userData);
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

    public Recharge processPayment(AccountData accountData, Customer customer, PaymentInfo paymentInfo, Float amount, String externalId) throws NullParameterException, InvalidAccountException, DisabledAccountException, InvalidAmountException, InvalidCreditCardException, InvalidCreditCardDateException, PaymentServiceUnavailableException, PaymentDeclinedException, GeneralException {
        validateCustomerData(customer);
        Account account = validateAccount(accountData);

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

    public boolean validatePaymentInfo(PaymentInfo info, Long transactionTypeId)throws NullParameterException, GeneralException{
        if (info == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "paymentInfo"), null);
        }
        boolean valid=true;
        info.setPaymentPatner(entityManager.find(PaymentPatner.class, 1l));
        Date now = new Date((new java.util.Date()).getTime());
        Timestamp nowTimestamp = new Timestamp(now.getTime());
        info.setBeginningDate(nowTimestamp);
        info.setPaymentType(entityManager.find(PaymentType.class, PaymentType.CREDIT_CARD));
        EntityTransaction userTransaction = entityManager.getTransaction();
   //     UserTransaction userTransaction = context.getUserTransaction();
        Long numberTransaccionCreditCardNumber = 0L;
        Date nowBeginning = GeneralUtils.getBeginningDate(new Date((new java.util.Date()).getTime()));
        Date nowEnding = GeneralUtils.getEndingDate(new Date((new java.util.Date()).getTime()));
        Timestamp nowBeginningDate = new Timestamp(nowBeginning.getTime());
        Timestamp nowEndingDate = new Timestamp(nowEnding.getTime());
        try {
            if (info.getId() == null) {
                if (!userTransaction.isActive())
                    userTransaction.begin();
                entityManager.persist(info);
                entityManager.flush();
            }
            StringBuilder sqlBuilder = new StringBuilder("SELECT COUNT(t.id) FROM payment_info p1, transaction t ,payment_info p2" +
                    " WHERE p1.creditCardNumber=p2.creditCardNumber AND p1.id=t.paymentInfoId AND t.transactionStatusId="+ TransactionStatus.PROCESSED + " AND p2.id="+info.getId() + " AND t.creationDate BETWEEN DATE('" + nowBeginningDate + "') AND ('" + nowEndingDate + "')");
                if (transactionTypeId != null) {
                    sqlBuilder.append(" AND t.transactionTypeId=").append(transactionTypeId);
                }
             List result = new ArrayList();
             System.out.println(sqlBuilder.toString());
 
            result = (List) entityManager.createNativeQuery(sqlBuilder.toString()).getSingleResult();
            numberTransaccionCreditCardNumber = (Long) result.get(0);
            if (numberTransaccionCreditCardNumber >= 7) {
                System.out.println("TIENE MAS DE DOS TRANSACCIONES");
                valid = false;
            }
            if (info.getId() == null) {
                entityManager.remove(info);
                userTransaction.commit();
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (userTransaction.isActive()) {
                userTransaction.rollback();
            }
            logger.error("Exception in method loadPaymentInfo: ", e);
            throw new GeneralException("Exception in method loadPaymentInfo: " + e.getMessage(), e.getStackTrace());
        }
        return valid;
    }

    public Boolean cancelPayment(AccountData userData, Recharge recharge, Float amount) throws NullParameterException, InvalidAccountException, DisabledAccountException, CancelPaymentException, GeneralException {
        Boolean isProcessOrderVoiding = false;
        validateAccount(userData);
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

    public Boolean cancelPayment(AccountData userData, Transaction transaction) throws NullParameterException, InvalidAccountException, DisabledAccountException, CancelPaymentException, GeneralException {
        Boolean isProcessOrderVoiding = false;
        validateAccount(userData);
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
                TransactionStatus transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.CANCELED);
                transaction.setTransactionStatus(transactionStatus);
                saveTransaction(transaction);
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

    public Customer registerCustomer(AccountData userData, Customer customer) throws NullParameterException, CustomerAlreadyExistException, InvalidAccountException, DisabledAccountException, GeneralException {
        Account account = validateAccount(userData);
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
            try{
            customerEJB.saveCustomer(customer);
            }catch (Exception ex) {
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
                newCustomer.setPhoneNumber(customer.getPhoneNumber());
                newCustomer.setEnterprise(customer.getEnterprise());
                newCustomer.setAddress(customer.getAddress());
                newCustomer.setCreationDate(customer.getCreationDate());
                newCustomer.setEnabled(customer.getEnabled());
                newCustomer.setGender(customer.getGender());
                newCustomer.setId(customer.getId());
                newCustomer.setPassword(customer.getPassword());

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

    public Customer searchCustomer(AccountData userData, String email) throws NullParameterException, RegisterNotFoundException, InvalidAccountException, DisabledAccountException, GeneralException {
        validateAccount(userData);
        com.sg123.ejb.UserEJB sisacUserEJB = (com.sg123.ejb.UserEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.USER_EJB);
        com.sg123.model.WebUser sisacWebUser = null;
        com.sg123.model.utils.Enterprise sisacEnterprise = new com.sg123.model.utils.Enterprise();
        sisacEnterprise.setId(com.sg123.model.utils.Enterprise.WIXTEL_USA);
        Customer newCustomer = new Customer();
        try {
            sisacWebUser = sisacUserEJB.getWebUserByLogin(email, sisacEnterprise.getId());
            newCustomer.setId((sisacWebUser.getPrincipalCustomer().getId()));
            newCustomer.setFirstName(sisacWebUser.getPrincipalCustomer().getName());
            newCustomer.setLastName(sisacWebUser.getPrincipalCustomer().getLastName());
            newCustomer.setEmail(sisacWebUser.getPrincipalCustomer().getEmail());
            newCustomer.setPhoneNumber(sisacWebUser.getPrincipalCustomer().getPhone());
            newCustomer.setPassword(sisacWebUser.getPassword());
        } catch (com.sg123.exception.WebUserNotFoundException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), null), ex);
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), ex);
        }
        return newCustomer;
    }

    public String getUserId(String email) throws NullParameterException, RegisterNotFoundException, GeneralException {
        com.sg123.ejb.UserEJB sisacUserEJB = (com.sg123.ejb.UserEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.USER_EJB);
        com.sg123.model.WebUser sisacWebUser = null;
        com.sg123.model.utils.Enterprise sisacEnterprise = new com.sg123.model.utils.Enterprise();
        sisacEnterprise.setId(com.sg123.model.utils.Enterprise.WIXTEL_USA);
        String userId = null;
        try {
            sisacWebUser = sisacUserEJB.getWebUserByLogin(email, sisacEnterprise.getId());
            userId = sisacWebUser.getId().toString();
        } catch (com.sg123.exception.WebUserNotFoundException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), null), ex);
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), ex);
        }
        return userId;
    }

    public LoginResponse loginCustomer(AccountData userData, String login,String password) throws NullParameterException, RegisterNotFoundException, InvalidAccountException, DisabledAccountException,InvalidPasswordException,WebUserIsDisabledException, GeneralException {
        validateAccount(userData);
        com.sg123.ejb.UserEJB sisacUserEJB = (com.sg123.ejb.UserEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.USER_EJB);
        com.sg123.model.utils.Enterprise sisacEnterprise = new com.sg123.model.utils.Enterprise();
        sisacEnterprise.setId(com.sg123.model.utils.Enterprise.WIXTEL_USA);
        LoginResponse loginResponse = new LoginResponse();
        Long enterprise = Enterprise.ALODIGA_USA;
        try {
            com.sg123.model.WebUser webUser;
                webUser = sisacUserEJB.login(login, password, enterprise);

            if (webUser.getConfirmDate() == null) {
                throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), null), null);
            }
            loginResponse.setLogin(webUser.getLogin());
            loginResponse.setUserId(webUser.getId().toString());
            loginResponse.setFullName(webUser.getCustomerWebUsers().get(0).getCustomer().getFullName());
        } catch (com.sg123.exception.WebUserNotFoundException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), null), ex);
        } catch (UnconfirmedEmailException ex) {
            throw new InvalidPasswordException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), null), ex);
        } catch (com.sg123.exception.InvalidPasswordException ex) {
            throw new InvalidPasswordException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), null), ex);
        } catch (com.sg123.exception.InvalidEmailException ex) {
             throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), ex);
        } catch (com.sg123.exception.GeneralException ex) {
             throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), ex);
        } catch (com.sg123.exception.WebUserIsDisabledException ex) {
            throw new WebUserIsDisabledException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), ex);
        }
        System.out.println("loginResponse"+loginResponse.getLogin());
        System.out.println("loginResponse"+loginResponse.getFullName());
        System.out.println("loginResponse"+loginResponse.getUserId());
        return loginResponse;
    }

    public boolean logoutCustomer(AccountData userData, String userId) throws NullParameterException, RegisterNotFoundException, InvalidAccountException, DisabledAccountException, TokenException, GeneralException {
        validateAccount(userData);
        com.sg123.ejb.UserEJB sisacUserEJB = (com.sg123.ejb.UserEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.USER_EJB);
        com.sg123.model.utils.Enterprise sisacEnterprise = new com.sg123.model.utils.Enterprise();
        sisacEnterprise.setId(com.sg123.model.utils.Enterprise.WIXTEL_USA);
        Long user = Long.parseLong(userId);
        boolean check = false;

        com.sg123.model.WebUser webUser = null;
        try {
            webUser = sisacUserEJB.loadWebUserById(user);
        } catch (com.sg123.exception.WebUserNotFoundException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), null), ex);
        } catch (com.sg123.exception.GeneralException e) {
           throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), e);
        }

        try {
            System.out.println("WebUserId()" + webUser.getId());
            com.sg123.model.WebUserSession webUserSession = sisacUserEJB.loadWebUserSessionByWebUser(webUser.getId());
            if (webUserSession != null) {
                long limitSession = 0;
                check = sisacUserEJB.checkWebUserSession(webUser.getId(), limitSession);
            }
        } catch (com.sg123.exception.NullParameterException npe) {
             throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "providerId"), null);

        } catch (com.sg123.exception.GeneralException ex) {
            ex.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), ex);
        }
        return check;
    }

    public ProvissionPinResponse purchasePin(AccountData userData, Customer customer, PaymentInfo paymentInfo, String phoneNumber, Float amount, String smsDestination, String externalId,String distributionId) throws NullParameterException, InvalidAccountException, DisabledAccountException, InvalidPhoneNumberException, PinAlreadyExistException, InvalidCreditCardException, InvalidAmountException,PaymentDeclinedException, InvalidPaymentInfoException, GeneralException,  EmptyListException, TransactionNotAvailableException, MaxAmountPerTransactionException, MaxAmountDailyException, MaxPromotionTransactionDailyException {
        Account account = validateAccount(userData);
        TransactionType transactionType = null;
        TransactionStatus transactionStatus = null;
        Long serial = null ;
        Long orderId = null;
        if (phoneNumber == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "phoneNumber"), null);
        } else if (amount == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "amount"), null);
        }
        //Valida que sea un numero valido para USA.
        if (!GeneralUtils.isValidUSAPhoneNumber(phoneNumber)) {
            throw new InvalidPhoneNumberException("Invalid phoneNumber: " + phoneNumber);
        }
        if (amount <= 0) {
            throw new InvalidAmountException("Invalid amount for Pin Purchase: " + amount);

        }
        Transaction transaction = null;
        Date now = new Date((new java.util.Date()).getTime());
        Timestamp nowTimestamp = new Timestamp(now.getTime());
        transaction = transaction = new Transaction(null, null, amount, 0f, 0f, nowTimestamp, null, account, null, null, null, null, externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
        EJBRequest request = new EJBRequest();
        request.setParam(transaction);
        try {
            transactionEJB.validateTransaction(request);
        } catch (TransactionNotAvailableException e) {
            throw e;
        } catch (MaxAmountPerTransactionException e) {
            throw e;
        } catch (MaxAmountDailyException e) {
            throw e;
        }
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
        } catch (PinFreeNotFoundException ex) {
        } catch (com.sg123.exception.GeneralException ex) {
            throw new GeneralException("Exception trying provission pin. " + ex.getMessage());
        }
        String transactionData = "Compra Pin Electronico [monto)" + amount + "(monto] - [ani)" + phoneNumber + "(ani]" + "[cuenta - login)" + userData.getLogin() + "(cuenta -login]";
        com.sg123.model.WebUser sisacWebUser = null;
        com.sg123.model.utils.Enterprise sisacEnterprise = new com.sg123.model.utils.Enterprise();
        sisacEnterprise.setId(com.sg123.model.utils.Enterprise.WIXTEL_USA);

        Recharge recharge = null;

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

        saveRUUserHasWebUser(userData.getUserId(),sisacWebUser);
        try {
            recharge = this.processPayment(account, paymentInfo, amount);
            System.out.println("RechargeID"+recharge.getId()!=null?recharge.getId():"no se guardo la recarga");
        } catch (InvalidAmountException e) {
            try {
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, e.getMessage(), account, null, null, null, "Error trying purchase pin. Number = " + phoneNumber + " Procesando el pago con Authorize.net. (MONTO INVALIDO)", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
                throw (e);
            } catch (Exception ex2) {
//                ex2.printStackTrace();
            }
        } catch (PaymentDeclinedException e) {
            try {
                ServiceMailDispatcher.sendPinPurchaseErrorMail(account, "Error durante compra : " + transactionData.toString(), "Paso 2: Procesando el pago con Authorize.net. (PAGO DECLINADO)", e);
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, e.getMessage(), account, null, null, null,  "Error trying purchase pin. Number = " + phoneNumber + " Procesando el pago con Authorize.net. (PAGO DECLINADO)", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
            throw (e);
        } catch (InvalidPaymentInfoException e) {
            try {
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, e.getMessage(), account, null, null, null, "Error trying purchase pin. Number = " + phoneNumber + " Procesando el pago con Authorize.net. (METODO DE PAGO INVALIDO)", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
                ServiceMailDispatcher.sendPinPurchaseErrorMail(account, "Error durante compra : " + transactionData.toString(), "Paso 2: Procesando el pago con Authorize.net. (METODO DE PAGO INVALIDO)", e);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
            throw (e);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, e.getMessage(), account, null, null, null, "Error trying purchase pin. Number = " + phoneNumber + " Procesando el pago con Authorize.net. (ERROR GENERAL)", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                ServiceMailDispatcher.sendPinPurchaseErrorMail(account, "Error durante compra : " + transactionData.toString(), "Paso 2: Procesando el pago con Authorize.net. (ERROR GENERAL)", e);
                this.saveTransaction(transaction);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ""), e);
        }
        try {
            Map pinData = contractEJB.provisionDistributionPinPurchase(account.getCustumerServiceIdSisac(), sisacWebUser.getId(), amount, transactionData);
            com.sg123.model.platformprepay.Pin pin = (com.sg123.model.platformprepay.Pin) pinData.get("PIN");
            com.sg123.model.ecommerce.Order order = (com.sg123.model.ecommerce.Order) pinData.get("ORDER");
            //Se validan las promociones de SISAC.
            List<String> promotions = this.getPromotions(pin.getSerial());
            Long[] anis = new Long[1];
            anis[0] = Long.parseLong(phoneNumber);
            pinEJB.savePinFree(pin.getSerial(), anis);
            serial = pin.getSerial();
            orderId = order.getId();
            List<String> accessNumbers = new ArrayList<String>();
            try {
                accessNumbers = callingCardPinEJB.getAccessNumbersByAni(account.getCustumerServiceIdSisac(), phoneNumber, 1L);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_PURCHASE);
            transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.PROCESSED);
            transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, transactionData, account, pin.getSerial(), null, null, transactionData, externalId, recharge, order.getId().toString(), null, null, paymentInfo,0f,userData.getIpRemoteAddress());
            pinResponse = new ProvissionPinResponse(pin.getSerial().toString(), pin.getSecret().toString(), order.getId().toString(), "Transaccion exitosa", promotions, accessNumbers);
            try {
                if (orderId != null && paymentInfo.getPaymentInfoIdSISAC() != null) {
//                    contractEJB.addPaymentInfoOrder(orderId, Long.parseLong(paymentInfo.getPaymentInfoIdSISAC()));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (distributionId != null ) {
                if (!distributionId.equals("")) {
//                    LocalWSMultilevelChannelProxy proxy = new LocalWSMultilevelChannelProxy();
//                    try {
//                        proxy.assignComission(Long.parseLong(transactionType.PIN_PURCHASE_REFERENCES.toString()), Long.parseLong(distributionId), amount, serial.toString());
//                    } catch (Exception ex) {
//                        ex.printStackTrace();
//                    }
                }
            }

        } catch (Exception ex1) {
            ex1.printStackTrace();
            try {
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, ex1.getMessage(), account,  serial !=null?serial:null, null, null, "Error trying purchase pin. Number = " + phoneNumber, externalId, null, orderId!=null?orderId.toString():null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                if (recharge != null && recharge.getResponseCode().equals("1")) {
                    cancelPinProvission(userData, null, recharge.getId(),transaction);
                }
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex1);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }


        } finally {
            try {
                this.saveTransaction(transaction);

            } catch (Exception ex) {
                ex.printStackTrace();
                throw new GeneralException("Exception trying saving transaction in method provissionPin. " + ex.getMessage());
            }
        }
        return pinResponse;
//        }
    }

     public ProvissionPinResponse purchasePin(AccountData userData, Customer customer, PaymentInfo paymentInfo,String phoneNumber, Float amount, String smsDestination, String externalId,String distributionId,String registerUniId,String deliveryAddressId,String billingAddressId,String paymentInfoId,String salesChannelId,String ordenSourceId) throws NullParameterException, InvalidAccountException, DisabledAccountException, InvalidPhoneNumberException, PinAlreadyExistException, InvalidCreditCardException, InvalidAmountException, PaymentDeclinedException, InvalidPaymentInfoException, GeneralException, EmptyListException, TransactionNotAvailableException, MaxAmountPerTransactionException, MaxAmountDailyException, MaxPromotionTransactionDailyException{
        Account account = validateAccount(userData);
        TransactionType transactionType = null;
        TransactionStatus transactionStatus = null;
        Long serial = null ;
        Long orderId = null;
        if (phoneNumber == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "phoneNumber"), null);
        } else if (amount == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "amount"), null);
        }
        //Valida que sea un numero valido para USA.
        if (!GeneralUtils.isValidUSAPhoneNumber(phoneNumber)) {
            throw new InvalidPhoneNumberException("Invalid phoneNumber: " + phoneNumber);
        }
        if (amount <= 0) {
            throw new InvalidAmountException("Invalid amount for Pin Purchase: " + amount);

        }
        Transaction transaction = null;
        Date now = new Date((new java.util.Date()).getTime());
        Timestamp nowTimestamp = new Timestamp(now.getTime());
        transaction = transaction = new Transaction(null, null, amount, 0f, 0f, nowTimestamp, null, account, null, null, null, null, externalId, null, null, null, null, null,0f,userData.getIpRemoteAddress());
//         transaction = transaction = new Transaction(null, null, amount, 0f, 0f, nowTimestamp, null, account, null, null, null, null, externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
        EJBRequest request = new EJBRequest();
        request.setParam(transaction);
        try {
            transactionEJB.validateTransaction(request);
        } catch (TransactionNotAvailableException e) {
            throw e;
        } catch (MaxAmountPerTransactionException e) {
            throw e;
        } catch (MaxAmountDailyException e) {
            throw e;
        }
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
        } catch (PinFreeNotFoundException ex) {
        } catch (com.sg123.exception.GeneralException ex) {
            throw new GeneralException("Exception trying provission pin. " + ex.getMessage());
        }
        String transactionData = "Compra Pin Electronico [monto)" + amount + "(monto] - [ani)" + phoneNumber + "(ani]" + "[cuenta - login)" + userData.getLogin() + "(cuenta -login]";
        com.sg123.model.WebUser sisacWebUser = null;
        com.sg123.model.utils.Enterprise sisacEnterprise = new com.sg123.model.utils.Enterprise();
        sisacEnterprise.setId(com.sg123.model.utils.Enterprise.WIXTEL_USA);

        Recharge recharge = null;

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

        saveRUUserHasWebUser(userData.getUserId(),sisacWebUser);
        try {
            recharge = this.processPayment(account, paymentInfo, amount);
            System.out.println("RechargeID"+recharge.getId()!=null?recharge.getId():"no se guardo la recarga");
        } catch (InvalidAmountException e) {
            try {
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, e.getMessage(), account, null, null, null, "Error trying purchase pin. Number = " + phoneNumber + " Procesando el pago con Authorize.net. (MONTO INVALIDO)", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
                throw (e);
            } catch (Exception ex2) {
//                ex2.printStackTrace();
            }
        } catch (PaymentDeclinedException e) {
            try {
                ServiceMailDispatcher.sendPinPurchaseErrorMail(account, "Error durante compra : " + transactionData.toString(), "Paso 2: Procesando el pago con Authorize.net. (PAGO DECLINADO)", e);
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, e.getMessage(), account, null, null, null,  "Error trying purchase pin. Number = " + phoneNumber + " Procesando el pago con Authorize.net. (PAGO DECLINADO)", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
            throw (e);
        } catch (InvalidPaymentInfoException e) {
            try {
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, e.getMessage(), account, null, null, null, "Error trying purchase pin. Number = " + phoneNumber + " Procesando el pago con Authorize.net. (METODO DE PAGO INVALIDO)", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
                ServiceMailDispatcher.sendPinPurchaseErrorMail(account, "Error durante compra : " + transactionData.toString(), "Paso 2: Procesando el pago con Authorize.net. (METODO DE PAGO INVALIDO)", e);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
            throw (e);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, e.getMessage(), account, null, null, null, "Error trying purchase pin. Number = " + phoneNumber + " Procesando el pago con Authorize.net. (ERROR GENERAL)", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                ServiceMailDispatcher.sendPinPurchaseErrorMail(account, "Error durante compra : " + transactionData.toString(), "Paso 2: Procesando el pago con Authorize.net. (ERROR GENERAL)", e);
                this.saveTransaction(transaction);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ""), e);
        }
        try {
            Map pinData = contractEJB.provisionDistributionPinPurchase(account.getCustumerServiceIdSisac(), sisacWebUser.getId(), amount, transactionData);
            com.sg123.model.platformprepay.Pin pin = (com.sg123.model.platformprepay.Pin) pinData.get("PIN");
            com.sg123.model.ecommerce.Order order = (com.sg123.model.ecommerce.Order) pinData.get("ORDER");
            //Se validan las promociones de SISAC.
            List<String> promotions = this.getPromotions(pin.getSerial());
            Long[] anis = new Long[1];
            anis[0] = Long.parseLong(phoneNumber);
            pinEJB.savePinFree(pin.getSerial(), anis);
            serial = pin.getSerial();
            orderId = order.getId();
            List<String> accessNumbers = new ArrayList<String>();
            try {
                accessNumbers = callingCardPinEJB.getAccessNumbersByAni(account.getCustumerServiceIdSisac(), phoneNumber, 1L);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_PURCHASE);
            transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.PROCESSED);
            transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, transactionData, account, pin.getSerial(), null, null, transactionData, externalId, recharge, order.getId().toString(), null, null, paymentInfo,0f,userData.getIpRemoteAddress());
            pinResponse = new ProvissionPinResponse(pin.getSerial().toString(), pin.getSecret().toString(), order.getId().toString(), "Transaccion exitosa", promotions, accessNumbers);
            try {
                if (orderId != null && paymentInfo.getPaymentInfoIdSISAC() != null) {
//                    contractEJB.addPaymentInfoOrder(orderId, Long.parseLong(paymentInfo.getPaymentInfoIdSISAC()));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
             if (distributionId != null ) {
                if (!distributionId.equals("")) {
//                    LocalWSMultilevelChannelProxy proxy = new LocalWSMultilevelChannelProxy();
//                    try {
//                        proxy.assignComission(Long.parseLong(transactionType.PIN_PURCHASE_REFERENCES.toString()), Long.parseLong(distributionId), amount, serial.toString());
//                    } catch (Exception ex) {
//                        ex.printStackTrace();
//                    }
                }
            }
           
            try {
                Product product = productEJB.loadProductById(Product.ELECTRONIC_PIN_ID);
                String token = loadTokenOrdenInvoive();
                WsSalesRecordProxy salesRecordProxy = new WsSalesRecordProxy();
                WsOrderResponse orderResponse = salesRecordProxy.generateOrder(token, Constants.ORDER_STATUS_PROCESS, null, registerUniId, deliveryAddressId, billingAddressId, paymentInfoId, sisacWebUser.getId().toString(), transaction.getTotalAmount().toString(),  transaction.getTotalTax().toString(), "0", transaction.getTotalAmount().toString(), "1", pin.getCurrentBalance().toString(), salesChannelId, pin.getCurrency().getId().toString(), ordenSourceId, product.getName(),Product.ELECTRONIC_PIN_ID.toString());
                System.out.println("codeOrder"+orderResponse.getCode());
                if (orderResponse.getCode().equals(Constants.TRANSACTION_SUCCESSFUL)) {
                    String responseOrderId = orderResponse.getId();
                    pinResponse.setOrderId(responseOrderId);

                    WsInvoiceResponse invoiceResponse = salesRecordProxy.generateInvoice(token, Enterprise.ALODIGA_USA.toString(), Constants.INVOICE_TYPE_PIN_LINE, Constants.INVOICE_STATUS_PROCESS, "1", transaction.getTotalTax().toString(), String.valueOf(amount), "0", "0", pin.getCurrentBalance().toString(), transaction.getTotalAmount().toString(), "0", "0", pin.getCurrency().getId().toString(), responseOrderId, "0", "0", null, null, registerUniId);
                    System.out.println("codeInvoice"+invoiceResponse.getCode());
                    if (invoiceResponse.getCode().equals(Constants.TRANSACTION_SUCCESSFUL)) {
                        pinResponse.setInvoiceId(invoiceResponse.getId());
                    }
                }
            } catch (Exception ex1) {
                ex1.printStackTrace();
            }

        } catch (Exception ex1) {
            ex1.printStackTrace();
            try {
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, ex1.getMessage(), account,  serial !=null?serial:null, null, null, "Error trying purchase pin. Number = " + phoneNumber, externalId, null, orderId!=null?orderId.toString():null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                if (recharge != null && recharge.getResponseCode().equals("1")) {
                    cancelPinProvission(userData, null, recharge.getId(),transaction);
                }
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex1);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }


        } finally {
            try {
                this.saveTransaction(transaction);

            } catch (Exception ex) {
                ex.printStackTrace();
                throw new GeneralException("Exception trying saving transaction in method provissionPin. " + ex.getMessage());
            }
        }
        return pinResponse;
//        }
    }

    public RechargePinResponse rechargePin(AccountData userData, Customer customer, PaymentInfo paymentInfo, String phoneNumber, String serial, Float amount, String smsDestination, String externalId,String distributionId) throws NullParameterException, InvalidAccountException, DisabledAccountException, InvalidPhoneNumberException, DisabledPinException, InvalidCreditCardException, InvalidAmountException,PaymentDeclinedException, InvalidPaymentInfoException,GeneralException, TransactionNotAvailableException, MaxAmountPerTransactionException, MaxAmountDailyException, MaxPromotionTransactionDailyException {
        Account account = validateAccount(userData);
        TransactionStatus transactionStatus = null;
        TransactionType transactionType = null;
        Long orderId = null;
        if (phoneNumber == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "phoneNumber"), null);
        } else if (serial == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "serial"), null);
        } else if (amount == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "amount"), null);
        }
        //Valida que sea un numero valido para USA.
        if (!GeneralUtils.isValidUSAPhoneNumber(phoneNumber)) {
            throw new InvalidPhoneNumberException("Invalid phoneNumber: " + phoneNumber);
        }
        if (amount <= 0) {
            throw new InvalidAmountException("Invalid amount for Pin Purchase: " + amount);

        }
        Transaction transaction = null;
        Date now = new Date((new java.util.Date()).getTime());
        Timestamp nowTimestamp = new Timestamp(now.getTime());
        transaction = transaction = new Transaction(null, null, amount, 0f, 0f, nowTimestamp, null, account, null, null, null, null, externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
        EJBRequest request = new EJBRequest();
        request.setParam(transaction);
        try {
            transactionEJB.validateTransaction(request);
        } catch (TransactionNotAvailableException e) {
            throw e;
        } catch (MaxAmountPerTransactionException e) {
            throw e;
        } catch (MaxAmountDailyException e) {
            throw e;
        } catch (EmptyListException e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), e);
        } catch (MaxPromotionTransactionDailyException e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), e);
        }
        RechargePinResponse rechargePinResponse = null;

        com.sg123.ejb.ContractEJB contractEJB = (com.sg123.ejb.ContractEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CONTRACT_EJB);
        com.sg123.ejb.UserEJB userEJB = (com.sg123.ejb.UserEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.USER_EJB);
        com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);
        com.sg123.ejb.ContentEJB contentEJB = (com.sg123.ejb.ContentEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CONTENT_EJB);
        com.sg123.ecommerce.ShoppingCart shoppingCart = new com.sg123.ecommerce.ShoppingCart();
        com.sg123.serviceconfig.RechargeConfig rechargeConfig = new com.sg123.serviceconfig.RechargeConfig();

        float newBalance = 0f;

        Recharge recharge = null;
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
            description += "<br/>" + " Amount: " + amount + customService.getCurrency().getSymbol();
            item.setDescription(description);
            shoppingCart.addItem(item);
            shoppingCart.setPaymentInfo(null);

            order = contractEJB.processShoppingCart(shoppingCart);
            order.setSubtotal(0F);
            order.setTaxTotal(0F);
            order.setTotal(0F);
            com.sg123.model.WebUserSession webUserSession = userEJB.loadWebUserSessionByWebUser(pin.getWebUser().getId());
            userEJB.updateWebUserSessionOrder(webUserSession.getId(), order);
        } catch (Exception ex1) {
             throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ""), ex1);
        }

        try {
            recharge = this.processPayment(account, paymentInfo, amount);
        } catch (InvalidAmountException e) {
            try {
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_RECHARGE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, e.getMessage(), account, null, null, null, "Error trying recharging pin. Number = " + phoneNumber + " Procesando el pago con Authorize.net. (MONTO INVALIDO)", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
            } catch (Exception ex1) {
            }
            throw (e);
        } catch (PaymentDeclinedException e) {
            try {
                ServiceMailDispatcher.sendPinRechargeErrorMail(account, "Error durante la recarga : " + transactionData.toString(), "Paso 2: Procesando el pago con Authorize.net. (PAGO DECLINADO)", e);
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, e.getMessage(), account, null, null, null, "Error trying recharging pin. Number = " + phoneNumber + " Procesando el pago con Authorize.net. (PAGO DECLINADO)", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
            } catch (Exception ex1) {
            }
            throw (e);
        } catch (InvalidPaymentInfoException e) {
            try {
                ServiceMailDispatcher.sendPinRechargeErrorMail(account, "Error durante la recarga : " + transactionData.toString(), "Paso 2: Procesando el pago con Authorize.net. (METODO DE PAGO INVALIDO)", e);
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, e.getMessage(), account, null, null, null, "Error trying recharging pin. Number = " + phoneNumber + " Procesando el pago con Authorize.net. (METODO DE PAGO INVALIDO)", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
            } catch (Exception ex1) {
            }
            throw (e);
        } catch (Exception e) {
            try {
                ServiceMailDispatcher.sendPinRechargeErrorMail(account, "Error durante la recarga : " + transactionData.toString(), "Paso 2: Procesando el pago con Authorize.net. (ERROR GENERAL)", e);
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, e.getMessage(), account, null, null, null, "Error trying recharging pin. Number = " + phoneNumber + " Procesando el pago con Authorize.net. (ERROR GENERAL)", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
            } catch (Exception ex1) {
            }
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ""), e);
        }
        try {
            order = contractEJB.rechargeDistributionPinPurchase(account.getCustumerServiceIdSisac(), pin.getWebUser().getId(), order);
            orderId = order.getId();
            transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_RECHARGE);
            transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.PROCESSED);
            transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, description, account, pin.getSerial(), null, null, transactionData, externalId, recharge, order.getId().toString(), null, null,paymentInfo,0f,userData.getIpRemoteAddress());
            rechargePinResponse = new RechargePinResponse(pin.getSerial().toString(), pin.getSecret(), "Transaccion Exitosa", String.valueOf(newBalance), String.valueOf(order.getId()));
            try {
                if (orderId != null && paymentInfo.getPaymentInfoIdSISAC() != null) {
//                    contractEJB.addPaymentInfoOrder(orderId, Long.parseLong(paymentInfo.getPaymentInfoIdSISAC()));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }


            if (distributionId != null ) {
                if (!distributionId.equals("")) {

//               com.alodiga.multilevelchannel.commons.ejbs.TransactionEJB  transactionEJB = (com.alodiga.multilevelchannel.commons.ejbs.TransactionEJB) com.alodiga.multilevelchannel.commons.utils.SimpleEJBLocator.getInstance().get(com.alodiga.multilevelchannel.commons.utils.EjbConstants.TRANSACTION_EJB);
//               LocalWSMultilevelChannelProxy proxy = new LocalWSMultilevelChannelProxy();
//               try {
//                   proxy.assignComission(Long.parseLong(transactionType.PIN_RECHARGE_REFERENCES.toString()),Long.parseLong(distrubutionId),amount,serial);
//               } catch (Exception ex) {
//                   ex.printStackTrace();
//               }
                }

            }
        } catch (Exception ex) {
            try {
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_RECHARGE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, ex.getMessage(), account, null, null, null, "Error trying recharging pin. Number = " + phoneNumber, externalId, null, orderId!=null?orderId.toString():null, null, null,paymentInfo,0f,userData.getIpRemoteAddress());
                if (recharge != null && recharge.getResponseCode().equals("1")) {
                    cancelPinRecharge(userData, null, recharge.getId(),transaction);
                }
                ex.printStackTrace();
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), ex);
            } catch (Exception ex1) {
            }
        } finally {
            try {
                this.saveTransaction(transaction);
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new GeneralException("Exception trying saving transaction in method rechargePin. " + ex.getMessage());
            }
        }        
        return rechargePinResponse;
//        }
    }

    public RechargePinResponse rechargePin(AccountData userData, Customer customer, PaymentInfo paymentInfo, String phoneNumber, String serial, Float amount, String smsDestination, String externalId,String distributionId,String registerUniId,String deliveryAddressId,String billingAddressId,String paymentInfoId,String salesChannelId,String ordenSourceId) throws NullParameterException, InvalidAccountException, DisabledAccountException, InvalidPhoneNumberException, DisabledPinException, InvalidCreditCardException, InvalidAmountException, PaymentDeclinedException, InvalidPaymentInfoException, GeneralException, TransactionNotAvailableException, MaxAmountPerTransactionException, MaxAmountDailyException, MaxPromotionTransactionDailyException {
        Account account = validateAccount(userData);
        TransactionStatus transactionStatus = null;
        TransactionType transactionType = null;
        Long orderId = null;
        if (phoneNumber == null && serial ==null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "phoneNumber or serial"), null);
        }  else if (amount == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "amount"), null);
        }
        //Valida que sea un numero valido para USA.
        if (!GeneralUtils.isValidUSAPhoneNumber(phoneNumber)) {
            throw new InvalidPhoneNumberException("Invalid phoneNumber: " + phoneNumber);
        }
        if (amount <= 0) {
            throw new InvalidAmountException("Invalid amount for Pin Purchase: " + amount);

        }
        Transaction transaction = null;
        Date now = new Date((new java.util.Date()).getTime());
        Timestamp nowTimestamp = new Timestamp(now.getTime());
        transaction = transaction = new Transaction(null, null, amount, 0f, 0f, nowTimestamp, null, account, null, null, null, null, externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
        EJBRequest request = new EJBRequest();
        request.setParam(transaction);
        try {
            transactionEJB.validateTransaction(request);
        } catch (TransactionNotAvailableException e) {
            throw e;
        } catch (MaxAmountPerTransactionException e) {
            throw e;
        } catch (MaxAmountDailyException e) {
            throw e;
        } catch (EmptyListException e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), e);
        } catch (MaxPromotionTransactionDailyException e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), e);
        }
        RechargePinResponse rechargePinResponse = null;

        com.sg123.ejb.ContractEJB contractEJB = (com.sg123.ejb.ContractEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CONTRACT_EJB);
        com.sg123.ejb.UserEJB userEJB = (com.sg123.ejb.UserEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.USER_EJB);
        com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);
        com.sg123.ejb.ContentEJB contentEJB = (com.sg123.ejb.ContentEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CONTENT_EJB);
        com.sg123.ecommerce.ShoppingCart shoppingCart = new com.sg123.ecommerce.ShoppingCart();
        com.sg123.serviceconfig.RechargeConfig rechargeConfig = new com.sg123.serviceconfig.RechargeConfig();

        float newBalance = 0f;

        Recharge recharge = null;
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
            description += "<br/>" + " Amount: " + amount + customService.getCurrency().getSymbol();
            item.setDescription(description);
            shoppingCart.addItem(item);
            shoppingCart.setPaymentInfo(null);

            order = contractEJB.processShoppingCart(shoppingCart);
            order.setSubtotal(0F);
            order.setTaxTotal(0F);
            order.setTotal(0F);
            com.sg123.model.WebUserSession webUserSession = userEJB.loadWebUserSessionByWebUser(pin.getWebUser().getId());
            userEJB.updateWebUserSessionOrder(webUserSession.getId(), order);
        } catch (Exception ex1) {
             throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ""), ex1);
        }
//        saveRUUserHasWebUser(userData.getUserId(),pin.getWebUser());
        try {
            recharge = this.processPayment(account, paymentInfo, amount);
        } catch (InvalidAmountException e) {
            try {
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_RECHARGE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, e.getMessage(), account, null, null, null, "Error trying recharging pin. Number = " + phoneNumber + " Procesando el pago con Authorize.net. (MONTO INVALIDO)", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
            } catch (Exception ex1) {
            }
            throw (e);
        } catch (PaymentDeclinedException e) {
            e.printStackTrace();
            try {
                ServiceMailDispatcher.sendPinRechargeErrorMail(account, "Error durante la recarga : " + transactionData.toString(), "Paso 2: Procesando el pago con Authorize.net. (PAGO DECLINADO)", e);
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, e.getMessage(), account, null, null, null, "Error trying recharging pin. Number = " + phoneNumber + " Procesando el pago con Authorize.net. (PAGO DECLINADO)", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
            } catch (Exception ex1) {
            }
            throw (e);
        } catch (InvalidPaymentInfoException e) {
            e.printStackTrace();
            try {
                ServiceMailDispatcher.sendPinRechargeErrorMail(account, "Error durante la recarga : " + transactionData.toString(), "Paso 2: Procesando el pago con Authorize.net. (METODO DE PAGO INVALIDO)", e);
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, e.getMessage(), account, null, null, null, "Error trying recharging pin. Number = " + phoneNumber + " Procesando el pago con Authorize.net. (METODO DE PAGO INVALIDO)", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
            } catch (Exception ex1) {
            }
            throw (e);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                ServiceMailDispatcher.sendPinRechargeErrorMail(account, "Error durante la recarga : " + transactionData.toString(), "Paso 2: Procesando el pago con Authorize.net. (ERROR GENERAL)", e);
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, e.getMessage(), account, null, null, null, "Error trying recharging pin. Number = " + phoneNumber + " Procesando el pago con Authorize.net. (ERROR GENERAL)", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
            } catch (Exception ex1) {
            }
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ""), e);
        }
        try {
            order = contractEJB.rechargeDistributionPinPurchase(account.getCustumerServiceIdSisac(), pin.getWebUser().getId(), order);
            orderId = order.getId();
            transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_RECHARGE);
            transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.PROCESSED);
            transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, description, account, pin.getSerial(), null, null, transactionData, externalId, recharge, order.getId().toString(), null, null,paymentInfo,0f,userData.getIpRemoteAddress());
            rechargePinResponse = new RechargePinResponse(pin.getSerial().toString(), pin.getSecret(), "Transaccion Exitosa", String.valueOf(newBalance), String.valueOf(order.getId()));
            try {
                if (orderId != null && paymentInfo.getPaymentInfoIdSISAC() != null) {
//                    contractEJB.addPaymentInfoOrder(orderId, Long.parseLong(paymentInfo.getPaymentInfoIdSISAC()));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }


            if (distributionId!=null){

//               com.alodiga.multilevelchannel.commons.ejbs.TransactionEJB  transactionEJB = (com.alodiga.multilevelchannel.commons.ejbs.TransactionEJB) com.alodiga.multilevelchannel.commons.utils.SimpleEJBLocator.getInstance().get(com.alodiga.multilevelchannel.commons.utils.EjbConstants.TRANSACTION_EJB);
//               LocalWSMultilevelChannelProxy proxy = new LocalWSMultilevelChannelProxy();
//               try {
//                   proxy.assignComission(Long.parseLong(transactionType.PIN_RECHARGE_REFERENCES.toString()),Long.parseLong(distrubutionId),amount,serial);
//               } catch (Exception ex) {
//                   ex.printStackTrace();
//               }

            }
             try {
                Product product = productEJB.loadProductById(Product.ELECTRONIC_PIN_ID);
                String token = loadTokenOrdenInvoive();
                WsSalesRecordProxy salesRecordProxy = new WsSalesRecordProxy();
                WsOrderResponse orderResponse = salesRecordProxy.generateOrder(token, Constants.ORDER_STATUS_PROCESS, null, registerUniId, deliveryAddressId, billingAddressId, paymentInfoId, pin.getWebUser().getId().toString(), transaction.getTotalAmount().toString(),  transaction.getTotalTax().toString(), "0", transaction.getTotalAmount().toString(), "1", pin.getCurrentBalance().toString(), salesChannelId, pin.getCurrency().getId().toString(), ordenSourceId, product.getName(),Product.ELECTRONIC_PIN_ID.toString());
                System.out.println("codeOrder"+orderResponse.getCode());
                if (orderResponse.getCode().equals(Constants.TRANSACTION_SUCCESSFUL)) {
                    String responseOrderId = orderResponse.getId();
                    rechargePinResponse.setOrderId(responseOrderId);

                    WsInvoiceResponse invoiceResponse = salesRecordProxy.generateInvoice(token, Enterprise.ALODIGA_USA.toString(), Constants.INVOICE_TYPE_PIN_LINE, Constants.INVOICE_STATUS_PROCESS, "1", transaction.getTotalTax().toString(), String.valueOf(amount), "0", "0", pin.getCurrentBalance().toString(), transaction.getTotalAmount().toString(), "0", "0", pin.getCurrency().getId().toString(), responseOrderId, "0", "0", null, null, registerUniId);
                    System.out.println("codeInvoice"+invoiceResponse.getCode());
                    if (invoiceResponse.getCode().equals(Constants.TRANSACTION_SUCCESSFUL)) {
                        rechargePinResponse.setInvoiceId(invoiceResponse.getId());
                    }
                }
            } catch (Exception ex1) {
                ex1.printStackTrace();
            }
        } catch (Exception ex) {
            try {
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_RECHARGE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, ex.getMessage(), account, null, null, null, "Error trying recharging pin. Number = " + phoneNumber, externalId, null, orderId!=null?orderId.toString():null, null, null,paymentInfo,0f,userData.getIpRemoteAddress());
                if (recharge != null && recharge.getResponseCode().equals("1")) {
                    cancelPinRecharge(userData, null, recharge.getId(),transaction);
                }
                ex.printStackTrace();
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), ex);
            } catch (Exception ex1) {
            }
        } finally {
            try {
                this.saveTransaction(transaction);
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new GeneralException("Exception trying saving transaction in method rechargePin. " + ex.getMessage());
            }
        }
        return rechargePinResponse;
//        }
    }

    public void deletePinFree(AccountData userData, String phoneNumber) throws NullParameterException, DisabledPinException, InvalidAccountException, DisabledAccountException, RegisterNotFoundException, GeneralException {
        validateAccount(userData);
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

    public void associatePinFree(AccountData userData, String serial, String phoneNumber) throws NullParameterException, InvalidAccountException, DisabledAccountException, RegisterNotFoundException, InvalidPhoneNumberException, PinFreeQuantityExceededException, GeneralException {

        validateAccount(userData);
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

    public SearchPinResponse searchPin2(AccountData userData, String phoneNumber, String serial) throws NullParameterException, InvalidAccountException, DisabledAccountException, InvalidPhoneNumberException, RegisterNotFoundException, GeneralException {
        validateAccount(userData);

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
    public List<Product> getProducts(AccountData userData) throws NullParameterException, InvalidAccountException, DisabledAccountException, EmptyListException, GeneralException {
        validateAccount(userData);
        List<Product> products = new ArrayList<Product>();
        try {
            Query query = createQuery("SELECT p FROM Product p ");
            products = query.setHint("toplink.refresh", "true").getResultList();

        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }

        if (products.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        products.size();
        return products;
    }
    
    
    

    //TODO: Se debe modificar para cambiar la comision y agregarle un cobro extra para los proveedores externos.
    public List<TopUpProduct> getTopUpProductByMobileOperatorId2(AccountData userData, Long mobileOperatorId) throws NullParameterException, InvalidAccountException, DisabledAccountException, EmptyListException, GeneralException {
        validateAccount(userData);
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

    public Account getAccount(String login, String password,String ipRemoteAddress) throws NullParameterException, InvalidAccountException, DisabledAccountException, GeneralException {
        return this.validateAccount(new AccountData(login, password,ipRemoteAddress));
        
    }

    public Pin procesPin(String login, String password,String ipRemoteAddress, String distributionId, String amount,String externalId) throws NullParameterException, InvalidAccountException, DisabledAccountException, GeneralException {
        if (login == null) {
            throw new NullParameterException("Parameter login cannot be null");
        }else if (password == null) {
            throw new NullParameterException("Parameter password cannot be null");
        }else if (distributionId == null) {
            throw new NullParameterException("Parameter distributionId cannot be null");
        }else if (amount == null) {
            throw new NullParameterException("Parameter amount cannot be null");
        }else if (externalId == null) {
            throw new NullParameterException("Parameter externalId cannot be null");
        }
        Map<Integer,String> map = new HashMap<Integer,String>();
        Pin pin = new Pin();

        com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);
        com.sg123.ejb.UtilsEJB utilsEJB = (com.sg123.ejb.UtilsEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.UTILS_EJB);
        com.sg123.model.utils.Enterprise sisacEnterprise = new com.sg123.model.utils.Enterprise();
        Date now = new Date((new java.util.Date()).getTime());
        Timestamp nowTimestamp = new Timestamp(now.getTime());
        sisacEnterprise.setId(com.sg123.model.utils.Enterprise.WIXTEL_USA);

        try {
            //creacion del Pin en servise asociado al pin de SISAC

           com.sg123.model.utils.Enterprise enterprise = utilsEJB.loadEnterprise(Enterprise.ALODIGA_USA);
           map= pinEJB.generatePinByReferentAndRewardProgram(enterprise,amount);
           String Serial = map.get(1);
           String Secret = map.get(2);

            pin.setCreationDate(new Timestamp(now.getTime()));
            pin.setSecret(Secret);
            pin.setSerial(Serial);
            pin.setEnabled(Boolean.FALSE);
            pin.setCustomer(null);
            pin.setPinFrees(null);
            pin = transactionEJB.savePin(pin);
            //creacion de la transaccion asociada al ping creado
            TransactionType transactionType = new TransactionType();
            transactionType.setId(transactionType.PIN_PURCHASE);

            TransactionStatus transactionStatus = new TransactionStatus();
            transactionStatus.setId(transactionStatus.PROCESSED);

            Transaction transaction = new Transaction();
            transaction.setTransactionType(transactionType);
            transaction.setAccount(getAccount(login,password,ipRemoteAddress));
            transaction.setInvoice(null);
            transaction.setPaymentInfo(null);
            transaction.setTotalTax(0F);
            transaction.setPromotionAmount(0F);
            transaction.setTotalAmount(Float.valueOf(amount));
            transaction.setCreationDate(nowTimestamp);
            transaction.setTransactionStatus(transactionStatus);
            transaction.setPinSerial(Long.valueOf(pin.getSerial()));
            transaction.setDescription(EjbConstants.PIN_FACEBOOK_REFERENCE);
            transaction.setExternalID(externalId);
            transaction.setDistributionId(Long.valueOf(distributionId));

            EJBRequest request = new EJBRequest();
            request.setParam(transaction);
            transaction = transactionEJB.saveTransaction(request);

            if(!(transaction.equals(null) && pin.equals(null))){
                System.out.println("Se realiso efectiva la tranmsaccion y el pin es "+pin.getSerial());
            }
        } catch (EmptyListException ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), null);
        } catch (TransactionNotAvailableException ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), null);
        } catch (MaxAmountPerTransactionException ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), null);
        } catch (MaxAmountDailyException ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), null);
        } catch (MaxPromotionTransactionDailyException ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), null);
        } catch (EnterpriseNotFoundException ex) {
           throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), null);
        } catch (com.sg123.exception.NullParameterException ex) {
             throw new NullParameterException("Parameter enterprise or amount cannot be null");
        } catch (com.sg123.exception.GeneralException ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), null);
        }
        return pin;
    }

    public BillPaymentResponse processBillPayment(AccountData userData, BillPaymentProduct billPaymentProduct, Float amount, String accountNumber, String senderName, String senderNumber, String externalId, Long languageId, boolean sendSMS, String destinationSMS) throws NullParameterException, InvalidAccountException, DisabledAccountException, CarrierSystemUnavailableException, InvalidSubscriberNumberException, SubscriberWillExceedLimitException, SubscriberAccountException, GeneralException {

        Account account = validateAccount(userData);
        BillPaymentResponse response = new BillPaymentResponse();
        Transaction transaction = null;
        Date now = new Date((new java.util.Date()).getTime());
        StringBuilder transactionData = new StringBuilder();
        Timestamp nowTimestamp = new Timestamp(now.getTime());

//        try {
//            response = billPaymentEJB.executeBillPayment(billPaymentProduct, amount, accountNumber, senderName, senderNumber, account, sendSMS, destinationSMS, languageId);
//            transactionData.append("BillPayment: ").append(billPaymentProduct.getName()).append("( ").append(billPaymentProduct.getId()).append(" )").append(" Monto: ").append(amount).append("AccountNumber: ").append(accountNumber);
//            transaction = new Transaction(new TransactionType(TransactionType.BILL_PAYMENT_PURCHASE), new TransactionStatus(TransactionStatus.PROCESSED), amount, 0f, 0f, nowTimestamp, transactionData.toString(), account, null, null, billPaymentProduct, transactionData.toString(), externalId, null, "", null, null,null,0f);
//
//        } catch (CarrierSystemUnavailableException ex) {
//            transaction = new Transaction(new TransactionType(TransactionType.BILL_PAYMENT_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), amount, 0f, 0f, nowTimestamp, null, account, null, null, billPaymentProduct, null, externalId, null, "", null, null,null,0f);
//            throw new CarrierSystemUnavailableException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
//        } catch (InvalidSubscriberNumberException ex) {
//            transaction = new Transaction(new TransactionType(TransactionType.BILL_PAYMENT_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), amount, 0f, 0f, nowTimestamp, null, account, null, null, billPaymentProduct, null, externalId, null, "", null, null,null,0f);
//            throw new InvalidSubscriberNumberException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
//        } catch (SubscriberWillExceedLimitException ex) {
//            transaction = new Transaction(new TransactionType(TransactionType.BILL_PAYMENT_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), amount, 0f, 0f, nowTimestamp, null, account, null, null, billPaymentProduct, null, externalId, null, "", null, null,null,0f);
//            throw new SubscriberWillExceedLimitException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
//        } catch (SubscriberAccountException ex) {
//            transaction = new Transaction(new TransactionType(TransactionType.BILL_PAYMENT_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), amount, 0f, 0f, nowTimestamp, null, account, null, null, billPaymentProduct, null, externalId, null, "", null, null,null,0f);
//            throw new SubscriberAccountException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
//        } catch (Exception ex) {
//            transaction = new Transaction(new TransactionType(TransactionType.BILL_PAYMENT_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), amount, 0f, 0f, nowTimestamp, null, account, null, null, billPaymentProduct, null, externalId, null, "", null, null,null,0f);
//            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
//        } finally {
//            try {
//                transaction = this.saveTransaction(transaction);
//                response.setTransaction(transaction);
//            } catch (Exception ex) {
//                throw new GeneralException("Exception trying saving transaction in method processBillPayment. " + ex.getMessage());
//            }
//        }
        return response;
    }

    public BillPaymentResponse processBillPayment(AccountData userData, PaymentInfo paymentInfo, BillPaymentProduct billPaymentProduct, Float amount, Float exchangedAmount, String accountNumber, String senderName, String senderNumber, String externalId, Long languageId, boolean sendSMS, String destinationSMS) throws NullParameterException, InvalidAccountException, DisabledAccountException, InvalidAmountException, PaymentDeclinedException, InvalidPaymentInfoException, CarrierSystemUnavailableException, InvalidSubscriberNumberException, SubscriberAccountException, SubscriberWillExceedLimitException, GeneralException {

        Account account = validateAccount(userData);
        if (paymentInfo == null) {
            throw new NullParameterException("Parameter paymentInfo cannot be null");
        } else if (billPaymentProduct == null) {
            throw new NullParameterException("Parameter billPaymentProduct cannot be null");
        }
        BillPaymentResponse response = new BillPaymentResponse();
        Transaction transaction = null;
        Date now = new Date((new java.util.Date()).getTime());
        StringBuilder transactionData = new StringBuilder();
        Timestamp nowTimestamp = new Timestamp(now.getTime());

        Recharge recharge = null;
        try {
            if(exchangedAmount!=0){
                recharge = this.processPayment(account, paymentInfo, exchangedAmount);
                System.out.println("El monto que se pago con la tarjeta fue de:"+exchangedAmount);
            }else{
                recharge = this.processPayment(account, paymentInfo, amount);
                System.out.println("El monto que se pago con la tarjeta fue de:"+amount);
            }
        } catch (InvalidAmountException ex) {
            throw (ex);
        } catch (PaymentDeclinedException ex) {
            ServiceMailDispatcher.sendBillPayErrorMail(billPaymentProduct, amount, account, accountNumber, senderName, senderNumber, "Paso 1: Procesando el pago con Authorize.net (FORMATO DE PAGO DECLINADO).", ex);
            throw (ex);
        } catch (InvalidPaymentInfoException ex) {
            ServiceMailDispatcher.sendBillPayErrorMail(billPaymentProduct, amount, account, accountNumber, senderName, senderNumber, "Paso 1: Procesando el pago con Authorize.net (FORMATO DE PAGO INVALIDO).", ex);
            throw (ex);
        } catch (Exception ex) {
            ServiceMailDispatcher.sendBillPayErrorMail(billPaymentProduct, amount, account, accountNumber, senderName, senderNumber, "Paso 1: Procesando el pago con Authorize.net (ERROR GENERAL).", ex);
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ""), ex);
        }
//        try {
//            response = billPaymentEJB.executeBillPayment(billPaymentProduct, amount, accountNumber, senderName, senderNumber, account, sendSMS, destinationSMS, languageId);
//            transactionData.append("BillPayment: ").append(billPaymentProduct.getName()).append("( ").append(billPaymentProduct.getId()).append(" )").append(" Monto: ").append(amount).append("AccountNumber: ").append(accountNumber);
//            transaction = new Transaction(new TransactionType(TransactionType.BILL_PAYMENT_PURCHASE), new TransactionStatus(TransactionStatus.PROCESSED), amount, 0f, 0f, nowTimestamp, transactionData.toString(), account, null, null, billPaymentProduct, transactionData.toString(), externalId, recharge, "", null, null,paymentInfo,0f);
//        } catch (CarrierSystemUnavailableException ex) {
//            transaction = new Transaction(new TransactionType(TransactionType.BILL_PAYMENT_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), amount, 0f, 0f, nowTimestamp, null, account, null, null, billPaymentProduct, null, externalId, null, "", null, null,paymentInfo,0f);
//            try {
//                if (recharge != null && recharge.getResponseCode().equals("1")) {
//                    cancelPayment(userData, transaction);
//                }
//            } catch (Exception e) {
//                ServiceMailDispatcher.sendBillPayErrorMail(billPaymentProduct, amount, account, accountNumber, senderName, senderNumber, "Paso 3: Error tratando de cancelar el pago por error al procesar la transccion. (ERROR GENERAL).", ex);
//                throw new GeneralException("Paso 3: Error tratando de cancelar el pago por error al procesar la transaccion de BillPayment. (ERROR GENERAL) " + e.getMessage());
//            }
//            throw new CarrierSystemUnavailableException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
//        } catch (InvalidSubscriberNumberException ex) {
//            transaction = new Transaction(new TransactionType(TransactionType.BILL_PAYMENT_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), amount, 0f, 0f, nowTimestamp, null, account, null, null, billPaymentProduct, null, externalId, null, "", null, null,paymentInfo,0f);
//            try {
//                if (recharge != null && recharge.getResponseCode().equals("1")) {
//                    cancelPayment(userData, transaction);
//                }
//            } catch (Exception e) {
//                ServiceMailDispatcher.sendBillPayErrorMail(billPaymentProduct, amount, account, accountNumber, senderName, senderNumber, "Paso 3: Error tratando de cancelar el pago por error al procesar la transccion. (ERROR GENERAL).", ex);
//                throw new GeneralException("Paso 3: Error tratando de cancelar el pago por error al procesar la transaccion de BillPayment. (ERROR GENERAL) " + e.getMessage());
//            }
//            throw new InvalidSubscriberNumberException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
//        } catch (SubscriberWillExceedLimitException ex) {
//            transaction = new Transaction(new TransactionType(TransactionType.BILL_PAYMENT_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), amount, 0f, 0f, nowTimestamp, null, account, null, null, billPaymentProduct, null, externalId, null, "", null, null,paymentInfo,0f);
//            try {
//                if (recharge != null && recharge.getResponseCode().equals("1")) {
//                    cancelPayment(userData, transaction);
//                }
//            } catch (Exception e) {
//                ServiceMailDispatcher.sendBillPayErrorMail(billPaymentProduct, amount, account, accountNumber, senderName, senderNumber, "Paso 3: Error tratando de cancelar el pago por error al procesar la transccion. (ERROR GENERAL).", ex);
//                throw new GeneralException("Paso 3: Error tratando de cancelar el pago por error al procesar la transaccion de BillPayment. (ERROR GENERAL) " + e.getMessage());
//            }
//            throw new SubscriberWillExceedLimitException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
//        } catch (SubscriberAccountException ex) {
//            transaction = new Transaction(new TransactionType(TransactionType.BILL_PAYMENT_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), amount, 0f, 0f, nowTimestamp, null, account, null, null, billPaymentProduct, null, externalId, null, "", null, null,paymentInfo,0f);
//            try {
//                if (recharge != null && recharge.getResponseCode().equals("1")) {
//                    cancelPayment(userData, transaction);
//                }
//            } catch (Exception e) {
//                ServiceMailDispatcher.sendBillPayErrorMail(billPaymentProduct, amount, account, accountNumber, senderName, senderNumber, "Paso 3: Error tratando de cancelar el pago por error al procesar la transccion. (ERROR GENERAL).", ex);
//                throw new GeneralException("Paso 3: Error tratando de cancelar el pago por error al procesar la transaccion de BillPayment. (ERROR GENERAL) " + e.getMessage());
//            }
//            throw new SubscriberAccountException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
//        } catch (Exception ex) {
//            transaction = new Transaction(new TransactionType(TransactionType.BILL_PAYMENT_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), amount, 0f, 0f, nowTimestamp, null, account, null, null, billPaymentProduct, null, externalId, null, "", null, null,paymentInfo,0f);
//            try {
//                if (recharge != null && recharge.getResponseCode().equals("1")) {
//                    cancelPayment(userData, transaction);
//                }
//            } catch (Exception e) {
//                ServiceMailDispatcher.sendBillPayErrorMail(billPaymentProduct, amount, account, accountNumber, senderName, senderNumber, "Paso 3: Error tratando de cancelar el pago por error al procesar la transccion. (ERROR GENERAL).", ex);
//                throw new GeneralException("Paso 3: Error tratando de cancelar el pago por error al procesar la transaccion de BillPayment. (ERROR GENERAL) " + e.getMessage());
//            }
//            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
//        } finally {
//            try {
//                transaction = this.saveTransaction(transaction);
//            } catch (Exception ex) {
//               ex.printStackTrace();
//               ServiceMailDispatcher.sendBillPayErrorMail(billPaymentProduct, amount, account, accountNumber, senderName, senderNumber, "Paso 3: Error tratando de persistir la transaccion BillPayment luego de haber procesado el pago. (ERROR GENERAL).", ex);
//               throw new GeneralException("Exception trying saving transaction in method provissionPin. " + ex.getMessage());
//            }
//        }
        return response;
    }

    public List<Country> getCountriesForBillPayment(AccountData userData) throws NullParameterException, InvalidAccountException, DisabledAccountException, GeneralException, EmptyListException {
        Account account = validateAccount(userData);
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

    public SMS sendSMS(SMS sms, Long enterpriseId) throws GeneralException, SMSFailureException, NullParameterException {
        PreferenceManager pm = null;
        try {
            pm = PreferenceManager.getInstance();
        } catch (Exception ex) {
            throw new GeneralException(ex.getMessage());
        }
        boolean isMlat = false;
        boolean isTelintel = false;
        boolean isTwilio = false;
        try {
            //sms.setDestination(ServiceConstans.TEST_MODE ? sms.getDestination() : EjbConstants.TEST_PHONE_NUMBER);
            sms.setDestination(sms.getDestination());
            String smsProvider = pm.getPreferencesValueByEnterpriseAndPreferenceId(enterpriseId, PreferenceFieldEnum.DEFAULT_SMS_PROVIDER.getId());
            isMlat = smsProvider.equals(String.valueOf(Provider.MLAT));
            isTelintel = smsProvider.equals(String.valueOf(Provider.TELINTEL));
            isTwilio =  smsProvider.equals(String.valueOf(Provider.TWILIO));
            if (isMlat) {
                sms = sendMLatSMS(sms);
            } else if (isTelintel) {
                sms = sendTelintelSMS(sms);
            } else if (isTwilio) {
                sms = sendTwilioSMS(sms);
            }
        } catch (Exception ex) {
            sms.setAdditional(ex.getMessage());
            if (!isMlat) {
                sms = sendMLatSMS(sms);
            } else if (!isTelintel) {
                sms = sendTelintelSMS(sms);
            } else if (!isTwilio) {
                sms = sendTwilioSMS(sms);
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

     public SMS sendTwilioSMS(SMS sms) throws SMSFailureException, NullParameterException, GeneralException {

        EJBRequest request = new EJBRequest();
        try {
            System.out.println("Trying to sent TWILIO sms...");
            sms.setIntegratorName("TWILIO");
            sms.setStatus(SMS.APPROVED);
            sms.setCreationDate(new Timestamp(new java.util.Date().getTime()));
            new SMSSender().sendTwilioSMS(sms);
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

    public Recharge processBanking(AccountData userData, PaymentInfo paymentInfo, Float amount, String externalId) throws NullParameterException, InvalidAccountException, DisabledAccountException, InvalidPhoneNumberException, InvalidCreditCardException, InvalidCreditCardDateException, InvalidAmountException, PaymentDeclinedException, InvalidPaymentInfoException, GeneralException {
        Map<String, Object> response = new HashMap<String, Object>();
        Account account = validateAccount(userData);
        if (amount <= 0) {
            throw new InvalidAmountException("Invalid amount for process payment: " + amount);
        }
        Transaction transaction = null;
        StringBuffer transactionData = new StringBuffer();
        Date now = new Date((new java.util.Date()).getTime());
        Timestamp nowTimestamp = new Timestamp(now.getTime());
        Recharge recharge = null;
        try {
            recharge = this.processPayment(account, paymentInfo, amount);
        } catch (InvalidAmountException e) {
            throw (e);
        } catch (PaymentDeclinedException e) {
            ServiceMailDispatcher.sendPurchaseBalanceErrorMail(account,transaction, "Error en el proceso de cobro : " + transactionData.toString(), "Paso 2: Procesando el pago con Authorize.net. (PAGO DECLINADO)", e);
            throw (e);
        } catch (InvalidCreditCardDateException e) {
            ServiceMailDispatcher.sendPurchaseBalanceErrorMail(account,transaction, "Error en el proceso de cobro : " + transactionData.toString(), "Paso 2: Procesando el pago con Authorize.net. Error Fecha de Expiracion(PAGO DECLINADO)", e);
            throw (e);
        } catch (InvalidPaymentInfoException e) {
            ServiceMailDispatcher.sendPurchaseBalanceErrorMail(account,transaction, "Error en el proceso de cobro : " + transactionData.toString(), "Paso 2: Procesando el pago con Authorize.net. (METODO DE PAGO INVALIDO)", e);
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
            transaction = new Transaction(transactionType, transactionStatus, recharge.getTotalAmount(), 0f, 0f, nowTimestamp, transactionData.toString(), account, null, null, null, transactionData.toString(), externalId, recharge, "", null, null,paymentInfo,0f,userData.getIpRemoteAddress());
        } catch (Exception ex) {
            try {
                ex.printStackTrace();
                transactionData.append("Cobro de dinero contra procesador bancario [monto)" + amount);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.PROCESSED);
                transaction = new Transaction(transactionType, transactionStatus, recharge.getTotalAmount(), 0f, 0f, nowTimestamp, transactionData.toString(), account, null, null, null, null, externalId, null, "", null, null,paymentInfo,0f,userData.getIpRemoteAddress());
                throw new GeneralException(ex.getMessage());
            } catch (Exception ex1) {
                throw new GeneralException(ex.getMessage());
            }
        } finally {
            try {
                EJBRequest request = new EJBRequest();
                List<BalanceHistory> histories = new ArrayList<BalanceHistory>();

                    BalanceHistory balanceHistory = new BalanceHistory();
                    balanceHistory.setAccount(account);
                    balanceHistory.setDate(transaction.getCreationDate());
                    balanceHistory.setTransaction(transaction);
                    Float currentAmount = 0F;
                    try {
                        currentAmount = ((BalanceHistory) transactionEJB.loadLastBalanceHistoryByAccount(account.getId())).getCurrentAmount();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    balanceHistory.setOldAmount(currentAmount);
                    balanceHistory.setCurrentAmount(currentAmount + transaction.getTotalAmount());
                    histories.add(balanceHistory);

                    transaction.setBalanceHistories(histories);
                    request = new EJBRequest();
                    request.setParam(transaction);
                    this.saveTransaction(transaction);
                    response.put(QueryConstants.PARAM_TRANSACTION, transaction);

                    account.setBalance(balanceHistory.getCurrentAmount());
                    request = new EJBRequest();
                    request.setParam(account);
                    userEJB.saveAccount(request);
                    //this.saveTransaction(transaction);
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new GeneralException("Exception trying saving transaction in method processBanking. " + ex.getMessage());
            }
        }
        return recharge;
    }

    public boolean hasAvailabeSMS(String customerEmail) throws NullParameterException, GeneralException {
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

    public Boolean procesPinActivation(String serial, String login,String ipRemoteAddress, String password, String externalId, String distributionId) throws NullParameterException, InvalidAccountException, DisabledAccountException, GeneralException {

        Pin pin = new Pin();
        Boolean active = false;
        com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);
        Date now = new Date((new java.util.Date()).getTime());
        Timestamp nowTimestamp = new Timestamp(now.getTime());
        try {
            if (pinEJB.activationPinByReferentAndRewardProgram(pinEJB.loadPin(Long.valueOf(serial)))) {
                pin = transactionEJB.loadPinBySerial(Long.valueOf(serial));
                pin.setEnabled(Boolean.TRUE);
                pin = transactionEJB.savePin(pin);
                //creacion de la transaccion asociada al ping creado
                TransactionType transactionType = new TransactionType();
                transactionType.setId(transactionType.PIN_PURCHASE);
                TransactionStatus transactionStatus = new TransactionStatus();
                transactionStatus.setId(transactionStatus.PROCESSED);
                Transaction transaction = new Transaction();
                transaction.setTransactionType(transactionType);
                transaction.setAccount(getAccount(login, password,ipRemoteAddress));
                transaction.setInvoice(null);
                transaction.setPaymentInfo(null);
                transaction.setTotalTax(0F);
                transaction.setPromotionAmount(0F);
                transaction.setTotalAmount(0F);
                transaction.setCreationDate(nowTimestamp);
                transaction.setTransactionStatus(transactionStatus);
                transaction.setPinSerial(Long.valueOf(pin.getSerial()));
                transaction.setDescription(EjbConstants.ACTIVE_PIN_FACEBOOK_REFERENCE);
                transaction.setExternalID(null);
                transaction.setDistributionId(Long.valueOf(distributionId));
                EJBRequest request = new EJBRequest();
                request.setParam(transaction);
                transaction = transactionEJB.saveTransaction(request);
                if (!(transaction == null && pin == null)) {
                    System.out.println("Se realiso efectiva la tranmsaccion y el pin es " + pin.getSerial());
                    active = true;
                }
            }
        } catch (PinNotFoundException ex) {
            java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (com.sg123.exception.NullParameterException ex) {
            java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (com.sg123.exception.GeneralException ex) {
            java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (EmptyListException ex) {
            java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransactionNotAvailableException ex) {
            java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MaxAmountPerTransactionException ex) {
            java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MaxAmountDailyException ex) {
            java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MaxPromotionTransactionDailyException ex) {
            java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RegisterNotFoundException ex) {
            java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
        }
        return active;
    }





    /* public Boolean listCustomer()   {
        //List<Customer> customer = new ArrayList<Customer>();
         Boolean estado = false;

         String constan = "Sin especificar";
          String sms;
        List<com.sg123.model.contract.Customer> customerisac = new ArrayList<com.sg123.model.contract.Customer>();
        com.sg123.model.contract.Customer customers;
           Map<Integer, String> map = new HashMap<Integer, String>();


        Pin pin = new Pin();
        Boolean active = false;
        com.sg123.ejb.ContractEJB contractEJB = (com.sg123.ejb.ContractEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CONTRACT_EJB);
        com.sg123.ejb.UserEJB sisacUserEJB = (com.sg123.ejb.UserEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.USER_EJB);
        com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);
        com.interax.telephony.service.remote.callingcard.CallingCardPinEJB callingCardPinEJB = (com.interax.telephony.service.remote.callingcard.CallingCardPinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CALLING_CARD_PIN_EJB);
        com.sg123.ejb.UtilsEJB utilsEJB = (com.sg123.ejb.UtilsEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.UTILS_EJB);


        try {

         customerisac = contractEJB.getCustomer();

          for (int j = 0; j < customerisac.size(); j++) {

              Date now = new Date((new java.util.Date()).getTime());
              Timestamp nowTimestamp = new Timestamp(now.getTime());

               customers = customerisac.get(j);

               if(customers.getName().equals(constan) || customers.getLastName().equals(constan)){
                   String Menssage ="";
                   sms = Menssage;
                        sendSmsService(Menssage,customers.getMobile(),customers.getPhone());
               }else{
                   String Menssage ="";
                        sendSmsService(Menssage,customers.getMobile(),customers.getPhone());
                        sms = Menssage;
               }

               //aca se salva en una tabla de auditoria de sms los datos de los mensajes enviados

               AuditSms auditSms = new AuditSms();
               auditSms.setId(null);
               auditSms.setCreationDate(nowTimestamp);
               auditSms.setCustomerId(customers.getId());
               auditSms.setPhoneDestination(customers.getMobile());
               auditSms.setMessage(sms);
               auditSms.setLengths(Long.valueOf(sms.length()));
               auditSms.setIntegrator("MLAT");
               auditSms.setName(customers.getName());
               auditSms.setLastName(customers.getLastName());
               EJBRequest request = new EJBRequest();
               request.setParam(auditSms);
               auditSms = transactionEJB.saveAuditSms(request);

                if (!(auditSms == null)) {
                    System.out.println("Se guardo el mensaje enviado  del customerID: " + auditSms.getCustomerId()+"cuyo Id de tabla es :"+auditSms.getId());

                }

          }
          estado = true;


        } catch (GeneralException ex) {
            java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NullParameterException ex) {
            java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (EmptyListException ex) {
            java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransactionNotAvailableException ex) {
            java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MaxAmountPerTransactionException ex) {
            java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MaxAmountDailyException ex) {
            java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MaxPromotionTransactionDailyException ex) {
            java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (com.sg123.exception.GeneralException ex) {
            java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ContractNotFoundException ex) {
            java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (com.sg123.exception.NullParameterException ex) {
            java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
        }
        return estado;
    }
*/


      public void sendSmsService(String text, String numberPhoneDest, String numberPhoneOri)  {
        try {

            SMS sms = new SMS();
            sms.setSender(numberPhoneOri);
            sms.setDestination(numberPhoneDest);
            sms.setContent(text);

            new SMSSender().sendSMS(sms);

        } catch (GeneralException ex) {
            java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SMSFailureException ex) {
            java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NullParameterException ex) {
            java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
        }


      }

    public ResponseCustomer loadCustomerByIdSisac(Long customerId) throws NullParameterException, RegisterNotFoundException, GeneralException {
        ResponseCustomer rCustomer = null;
        if (customerId == null) {
            throw new NullParameterException("Parameter customerId cant be null in method loadCustomerByIdSisac");
        }
        try {
            com.sg123.ejb.ContractEJB contractEJB = (com.sg123.ejb.ContractEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CONTRACT_EJB);
            com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);
            com.sg123.model.contract.Customer customer = contractEJB.loadCustomer(customerId);
            rCustomer = new ResponseCustomer();
            rCustomer.setLogin(customer.getCustomerWebUsers().get(0).getWebUser().getLogin());
            rCustomer.setPassword(customer.getCustomerWebUsers().get(0).getWebUser().getPassword());
            rCustomer.setEnterprideId(customer.getCustomerWebUsers().get(0).getWebUser().getEnterprise().getId().toString());
            com.sg123.model.platformprepay.Pin pin = null;
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
                rCustomer.setBalance(customer.getBalance().toString());
            }
            rCustomer.setAddress(rAddrees);
            try {
                pin = pinEJB.getPinFreeByAni(new Long(customer.getPhone())).getPin();
                pin = pinEJB.loadPin(pin.getSerial());//Hago el cambio aqui para no hacerlo en SisacEJB - Issue: LAZY Relationship
                rCustomer.setPin(pin.getSerial().toString());
                List<PinResponse> rPinFrees = new ArrayList<PinResponse>();
                if (!pin.getPinsFree().isEmpty()) {
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
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        } catch (CustomerNotFoundException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), null), null);
        } catch (com.sg123.exception.NullParameterException ex) {
           throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "customerId"), null);
        } catch (com.sg123.exception.GeneralException ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName(), "General Exception"), ex);
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName(), "General Exception"), ex);
        }
        return rCustomer;
    }

    public ResponseCustomer saveCustomerSisac(ResponseCustomer responseCustomer,Long customerId) throws NullParameterException, RegisterNotFoundException, GeneralException {
     
    	com.sg123.ejb.UserEJB userEJB = (com.sg123.ejb.UserEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.USER_EJB);
    	if (customerId == null) {
            throw new NullParameterException("Parameter customerId cant be null in method loadCustomerByIdSisac");
        }else if (responseCustomer == null) {
            throw new NullParameterException("Parameter responseCustomer cant be null in method loadCustomerByIdSisac");
        }
        try {
            com.sg123.ejb.ContractEJB contractEJB = (com.sg123.ejb.ContractEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CONTRACT_EJB);
            com.sg123.model.contract.Customer customer = contractEJB.loadCustomer(customerId);
            customer.setName(responseCustomer.getFirstName());
            customer.setLastName(responseCustomer.getLastName());
            customer.setEmail(responseCustomer.getEmail());
            customer.setPhone(responseCustomer.getPhoneNumber());
            if(responseCustomer.getPhoneNumber() != null && responseCustomer.getPhoneNumber() != ""){
            	WebUser wuser = userEJB.loadWebUserById(new Long(getUserId(responseCustomer.getPhoneNumber().substring(1))));
                wuser.setPassword(Encoder.MD5(responseCustomer.getPassword()));
                wuser.setLogin(responseCustomer.getEmail());
                userEJB.updateWebUser(wuser);
            }
            contractEJB.saveCustomer(customer);
            return responseCustomer;
        } catch (CustomerNotFoundException ex) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "customerId"), null);
        } catch (com.sg123.exception.NullParameterException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), null), null);
        } catch (com.sg123.exception.GeneralException ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName(), "General Exception"), ex);
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName(), "General Exception"), ex);
        }
    }



    public Customer registerCustomerByPromotion(AccountData userData, Customer customer, Long languajeId) throws NullParameterException, CustomerAlreadyExistException, InvalidAccountException, DisabledAccountException, GeneralException {
        Mail mail;
        Account account = validateAccount(userData);
        com.sg123.ejb.UserEJB sisacUserEJB = (com.sg123.ejb.UserEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.USER_EJB);
        com.sg123.ejb.UtilsEJB sisacUtilsEJB = (com.sg123.ejb.UtilsEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.UTILS_EJB);
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
                customer = customerEJB.saveCustomer(customer);
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

                com.sg123.ejb.PromotionsManagementEJB promotionsManagementEJB = (com.sg123.ejb.PromotionsManagementEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PROMOTIONSMANAGEMENTEJB);
                promotionsManagementEJB.applyRegistrationPinRewardPromotion(sisacWebUser, com.sg123.model.utils.OrderSource.ALODIGA_USA);
                ArrayList<String> recipents = new ArrayList<String>();
                recipents.add(customer.getEmail());

//                if (languajeId.equals(Language.ENGLISH)) {
//                    mail = ServiceMails.getAlodigaBenefitMailEn(customer.getEnterprise(), recipents, "Notificacion de registro exitoso ", "La presente nota es para notificarle que su registro en alodiga se realizo satisfactoriamente ", sisacWebUser.getPassword(), sisacWebUser.getLogin(), customer.getPassword());
//                } else {
//                    mail = ServiceMails.getAlodigaBenefitMailEs(customer.getEnterprise(), recipents, "Notificacion de registro exitoso ", "La presente nota es para notificarle que su registro en alodiga se realizo satisfactoriamente ", sisacWebUser.getPassword(), sisacWebUser.getLogin(), customer.getPassword());
//                }
//                utilsEJB.sendMail(mail);
            } catch (Exception ex1) {
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex1);
            }
        } catch (CustomerAlreadyExistException ex) {
            throw new CustomerAlreadyExistException(ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
        }
        return newCustomer;
    }
    
    public void registerPromotionalPin(AccountData userData, Customer customer, Long languajeId) throws NullParameterException,  InvalidAccountException, DisabledAccountException, GeneralException, WebUserNotFoundException, WebUserIsDisabledException {
        Mail mail;
        Account account = validateAccount(userData);
        com.sg123.ejb.UserEJB sisacUserEJB = (com.sg123.ejb.UserEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.USER_EJB);
        com.sg123.ejb.UtilsEJB sisacUtilsEJB = (com.sg123.ejb.UtilsEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.UTILS_EJB);
        com.sg123.model.WebUser sisacWebUser = null;
        com.sg123.model.utils.Enterprise sisacEnterprise = new com.sg123.model.utils.Enterprise();
        sisacEnterprise.setId(Enterprise.ALODIGA_USA);
        Date now = new Date((new java.util.Date()).getTime());
        Timestamp nowTimestamp = new Timestamp(now.getTime());
        EJBRequest request = new EJBRequest();
        request.setParam(customer.getEnterprise().getId());
        Enterprise enterprise = null;
		try {
			enterprise = utilsEJB.loadEnterprise(request);
		} catch (RegisterNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        if(enterprise != null){
        	customer.setEnterprise(enterprise);
        }
        if(customer == null){
        	throw new NullParameterException("Customer is null");
        }
        if(customer.getLogin() == null){
        	throw new NullParameterException("Customer has not login");
        }
        if(languajeId == null){
        	throw new NullParameterException("languajeId is null");
        }
        try {
        	sisacWebUser = sisacUserEJB.getWebUserByLogin(customer.getLogin(), sisacEnterprise.getId());
        }catch (com.sg123.exception.WebUserNotFoundException ex) {
        	throw new WebUserNotFoundException("Web user not found exception");
        }  catch (com.sg123.exception.WebUserIsDisabledException e) {
			e.printStackTrace();
			throw new WebUserIsDisabledException("Web user disable");
		}  catch (com.sg123.exception.GeneralException e) {
			e.printStackTrace();
			throw new GeneralException("Unknow error getting webuser pin");
		} 
        
        try{
        
            com.sg123.ejb.PromotionsManagementEJB promotionsManagementEJB = (com.sg123.ejb.PromotionsManagementEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PROMOTIONSMANAGEMENTEJB);
            promotionsManagementEJB.applyRegistrationPinRewardPromotion(sisacWebUser, com.sg123.model.utils.OrderSource.ALODIGA_US_MOBILE);
            ArrayList<String> recipents = new ArrayList<String>();
            recipents.add(customer.getEmail());

//            if (languajeId.equals(Language.ENGLISH)) {
//                mail = ServiceMails.getAlodigaBenefitMailEn(customer.getEnterprise(), recipents, "Notificacion de registro exitoso ", "La presente nota es para notificarle que su registro en alodiga se realizo satisfactoriamente ", sisacWebUser.getPassword(), sisacWebUser.getLogin(), customer.getPassword());
//            } else {
//                mail = ServiceMails.getAlodigaBenefitMailEs(customer.getEnterprise(), recipents, "Notificacion de registro exitoso ", "La presente nota es para notificarle que su registro en alodiga se realizo satisfactoriamente ", sisacWebUser.getPassword(), sisacWebUser.getLogin(), customer.getPassword());
//            }
//            utilsEJB.sendMail(mail);
        }  catch (com.sg123.exception.GeneralException e) {
			e.printStackTrace();
			throw new GeneralException("Unknow error saving promotional pin");
		}  catch (com.sg123.exception.NullParameterException e) {
        	throw new NullParameterException("Unknow parameter is null");
        }
    }



       public List<TerminationDestinationResponse> getTerminationCountryResponse() throws NullParameterException, RegisterNotFoundException, GeneralException {
           List<TerminationDestinationResponse> listTerminationsSingleCountryResponse = new ArrayList<TerminationDestinationResponse>();
           List<com.sg123.model.plan.TerminationPrice> terminationPrices = new ArrayList<com.sg123.model.plan.TerminationPrice>();
           com.sg123.ejb.PlanEJB planEJB = ( com.sg123.ejb.PlanEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PLAN_EJB);
           try {
            try {
                terminationPrices = planEJB.getTerminationPrices(com.alodiga.services.provider.commons.utils.Constants.PINLINE_PLAN);
            } catch (com.sg123.exception.GeneralException ex) {
                java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
            } catch (com.sg123.exception.NullParameterException ex) {
                java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
            }
            } catch (com.sg123.exception.TerminationPriceNotFoundException ex) {
                 throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), null), null);
            }
            for (int i = 0; i < terminationPrices.size(); i++) {
              
                     TerminationDestinationResponse terminationCountry = new TerminationDestinationResponse();

                     com.sg123.model.Code code = terminationPrices.get(i).getCode();
                     terminationCountry.setId(com.sg123.model.Code.getParentCountry(code).getId().toString());
                     terminationCountry.setName(com.sg123.model.Code.getParentCountry(code).getDestination());
                     terminationCountry.setCodeCountry(com.sg123.model.Code.getParentCountry(code).getCountryCode().toString());
                 if(!thisAdded(listTerminationsSingleCountryResponse,terminationCountry)){
                     listTerminationsSingleCountryResponse.add(terminationCountry);
                 }
            }
           return listTerminationsSingleCountryResponse;
     }



         private Boolean thisAdded(List<TerminationDestinationResponse> lists,TerminationDestinationResponse object ){
          for(TerminationDestinationResponse elements : lists){
               if(elements.getCodeCountry().toString().equals(object.getCodeCountry())){
                   return true;
               }
         }
          return false;
       }

  
       public List<TerminationDestinationResponse> getTerminationRegionResponse(Long parentCode) throws NullParameterException, RegisterNotFoundException, GeneralException {
           List<TerminationDestinationResponse> listTerminationsSingleCountryResponse = new ArrayList<TerminationDestinationResponse>();
           List<com.sg123.model.plan.TerminationPrice> terminationPrices = new ArrayList<com.sg123.model.plan.TerminationPrice>();
           com.sg123.ejb.PlanEJB planEJB = ( com.sg123.ejb.PlanEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PLAN_EJB);
           try {
            try {
                terminationPrices = planEJB.getTerminationPrices(com.alodiga.services.provider.commons.utils.Constants.PINLINE_PLAN);
            } catch (com.sg123.exception.GeneralException ex){
                java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
            } catch (com.sg123.exception.NullParameterException ex) {
                java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
            }
            } catch (com.sg123.exception.TerminationPriceNotFoundException ex) {
                 throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), null), null);
            }
            for (int i = 0; i < terminationPrices.size(); i++) {

                if(terminationPrices.get(i).getCode().getParentCode().getId().equals(parentCode)) {
                  TerminationDestinationResponse terminationCountry = new TerminationDestinationResponse();
                  terminationCountry.setId(terminationPrices.get(i).getCode().getId().toString());
                  terminationCountry.setName(terminationPrices.get(i).getCode().getDestination());
                  listTerminationsSingleCountryResponse.add(terminationCountry);
                }
            }
           return listTerminationsSingleCountryResponse;
    }

       public List<TerminationPriceResponse> getTerminationPriceList(Long regionCode) throws NullParameterException, RegisterNotFoundException, GeneralException {

           List<TerminationPriceResponse> listTerminationPriceSingleCountryResponse = new ArrayList<TerminationPriceResponse>();
           List<com.sg123.model.plan.TerminationPrice> terminationPrices = new ArrayList<com.sg123.model.plan.TerminationPrice>();
           com.sg123.ejb.PlanEJB planEJB = ( com.sg123.ejb.PlanEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PLAN_EJB);
           try {
            try {
                terminationPrices = planEJB.getTerminationPrices(com.alodiga.services.provider.commons.utils.Constants.PINLINE_PLAN);
            } catch (com.sg123.exception.GeneralException ex){
                java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
            } catch (com.sg123.exception.NullParameterException ex) {
                java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
            }
            } catch (com.sg123.exception.TerminationPriceNotFoundException ex) {
                 throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), null), null);
            }
            for (int i = 0; i < terminationPrices.size(); i++) {
                if(terminationPrices.get(i).getCode().getId().equals(regionCode)) {
                  TerminationPriceResponse terminationPrice = new TerminationPriceResponse();
                  terminationPrice.setDestination(terminationPrices.get(i).getCode().getFullDestination());
                  terminationPrice.setPrice( getCorrectAmount(Constants.USA_CURRENCY,terminationPrices.get(i).getDisplayPrice(), 4));
                  terminationPrice.setUnitName(terminationPrices.get(i).getTerminationPriceList().getService().getSegment().getEnterprise().getDisplayUnit().getName().toString());
                  listTerminationPriceSingleCountryResponse.add(terminationPrice);
                }
            }
           return listTerminationPriceSingleCountryResponse;
    }
       
       public List<TerminationPriceResponse> getTerminationPriceListByDni(Long dni) throws NullParameterException, RegisterNotFoundException, GeneralException {

    	   List<TerminationPriceResponse> listTerminationPriceSingleCountryResponse = new ArrayList<TerminationPriceResponse>();
    	   List<com.sg123.model.plan.TerminationPrice> terminationPrices = new ArrayList<com.sg123.model.plan.TerminationPrice>();
    	   TerminationPrice tp = new TerminationPrice();
    	   com.sg123.ejb.PlanEJB planEJB = ( com.sg123.ejb.PlanEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PLAN_EJB);
    	   String dniS = "" + dni;
    		   
    			   try {
//    				   terminationPrices =  planEJB.getTerminationPrices(com.alodiga.services.provider.commons.utils.Constants.PINLINE_PLAN, countryCode);
    				   tp =  planEJB.getTerminationPriceByAni(com.alodiga.services.provider.commons.utils.Constants.PINLINE_PLAN, dni);
    				   if(tp != null)
    					   terminationPrices.add(tp);
    				   
    			   } catch (com.sg123.exception.GeneralException ex){
    				   java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
    			   } catch (com.sg123.exception.NullParameterException ex) {
    				   java.util.logging.Logger.getLogger(ServicesEJBImp.class.getName()).log(Level.SEVERE, null, ex);
    			   } catch (com.sg123.exception.TerminationPriceNotFoundException ex) {
    				   throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), null), null);
    			   } catch (CallingCardRateNotFoundException e) {
    				   throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), null), null);
    			   } catch (CodeNotFoundException e) {
    				   throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), null), null);
    			   } catch (TerminationPriceListNotFoundException e) {
    				   throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), null), null);
    			   }
    			   if(terminationPrices != null && !terminationPrices.isEmpty())
    			   {
//    				   outerloop:
//    					   for(int j = 0 ; j < dniS.length(); j++){
//    						   Long countryCode = new Long(dniS.substring(0, dniS.length() - j));
//    						   System.out.println("*************Procesing :" + countryCode);
    						   for (int i = 0; i < terminationPrices.size(); i++) {
    							   TerminationPrice terminationPriceS =  terminationPrices.get(i);
//    							   Long code = terminationPriceS.getCode().getCountryCode();
//    							   System.out.println("*************Comparing code :" + code);
//    							   if(code.equals(countryCode)){
    								   TerminationPriceResponse terminationPrice = new TerminationPriceResponse();
    								   terminationPrice.setDestination(terminationPriceS.getCode().getFullDestination());
    								   terminationPrice.setPrice( getCorrectAmount(Constants.USA_CURRENCY,terminationPriceS.getDisplayPrice(), 4));
    								   terminationPrice.setUnitName(terminationPriceS.getUnit().getName().toString());
    								   listTerminationPriceSingleCountryResponse.add(terminationPrice);
//    								   break outerloop;
//    							   }
    						   }
//    					   }
    			   
    		   }

    	   return listTerminationPriceSingleCountryResponse;
    }   
    

    public List<DenominationResponse> getDenominationList() throws NullParameterException,RegisterNotFoundException, GeneralException {
        List<DenominationResponse> responses = new ArrayList<DenominationResponse>();
        List<ProductDenomination> productDenominations = new ArrayList<ProductDenomination>();
        try {
            productDenominations = productEJB.loadProductDenominations(Product.ELECTRONIC_PIN_ID);
            for (ProductDenomination denomination : productDenominations) {
                DenominationResponse denominationResponse = new DenominationResponse();
                denominationResponse.setAmount(denomination.getAmount().toString());
                denominationResponse.setCurrency(denomination.getCurrency().getSymbol());
                responses.add(denominationResponse);
            }
        } catch (EmptyListException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), null), null);
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName(), null), ex);
        }
        return responses;
    }

    public CardResponse getCallsList(String serial,String beginning, String ending,String languageId) throws NullParameterException,EmptyListException,PinFreeNotfoundException, GeneralException {
        CardResponse responses = new CardResponse();
        List<CallsResponse> callsResponses = new ArrayList<CallsResponse>();
        com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);
        com.sg123.model.platformprepay.Pin pin = null;
        try {
            pin = pinEJB.loadPin(Long.parseLong(serial));
            if (pin == null) {
                throw new PinFreeNotfoundException("Pin not fount");
            }
        } catch (com.sg123.exception.NullParameterException ex) {
            throw new NullParameterException("Parameter cannot be null getCallsList");
        } catch (com.sg123.exception.GeneralException ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName(), "General Exception"), ex);
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), "General Exception"), ex);
        }
        List<com.sg123.model.invoice.InvoiceDetail> invoiceDetails = null;
        try {
            invoiceDetails = findCallDetailsFromPin(beginning, ending, pin);
        } catch (NullParameterException e) {
             throw new NullParameterException("Parameter cannot be null (findCallDetailsFromPin)");
        } catch (EmptyListException e) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName(), null), e);
        } catch (GeneralException e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), e);
        }

        if (invoiceDetails != null && !invoiceDetails.isEmpty()) {
            responses.setBalance(serial);
            responses.setBalance(pin.getBalance().toString());
            for (com.sg123.model.invoice.InvoiceDetail invoice : invoiceDetails) {
                SimpleDateFormat formatter = new SimpleDateFormat(dateFormatTemplate(Integer.valueOf(languageId).intValue()));
                String amount = com.sg123.utils.EjbUtils.getCorrectAmount(invoice.getCurrency().getSymbol(), invoice.getTotal(), 2);
                CallsResponse callsResponse = new CallsResponse();
                callsResponse.setId(invoice.getId().toString());
                callsResponse.setStartTime(formatter.format(new Date(invoice.getStartTime().getTime())));
                callsResponse.setDn(invoice.getDn().toString());
                callsResponse.setOrigin(invoice.getOrigin());
                callsResponse.setAni(invoice.getAni().toString());
                callsResponse.setDestination(invoice.getDestination());
                callsResponse.setDni(invoice.getDni());
                callsResponse.setDuration(invoice.getRatedDuration().toString());
                callsResponse.setDirectionName(invoice.getCallDirection().getName());
                callsResponse.setAmount(amount);
                callsResponses.add(callsResponse);
            }
            responses.setCallsResponses(callsResponses);
        } else {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName(), null), null);
        }
        return responses;
    }

    public UserResponse getCardListByUserId(AccountData userData,String userId) throws NullParameterException, PinNotfoundException, InvalidAccountException,DisabledAccountException,InvalidPhoneNumberException,GeneralException {
        UserResponse responses = new UserResponse();
        com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);
        Long user = Long.parseLong(userId);
        List<com.sg123.model.platformprepay.Pin> clientPinList = new ArrayList<com.sg123.model.platformprepay.Pin>();
        try {
            clientPinList = pinEJB.getPinsByWebUserAndServiceFamily(user, ServiceFamily.CC_PIN_LINE);
            
            //Para retornar el que posee mayor saldo en la lista de pines
            
            if(clientPinList != null && !clientPinList.isEmpty()){
            	
            	 java.util.Collections.sort(clientPinList, new Comparator<com.sg123.model.platformprepay.Pin>() {
     				@Override
     				public int compare(com.sg123.model.platformprepay.Pin pin1,
     						com.sg123.model.platformprepay.Pin pin2) {
     					
     					if(pin1.getCurrentBalance() > pin2.getCurrentBalance()){
     						return -1;
     					} else if(pin1.getCurrentBalance() < pin2.getCurrentBalance()){
     						return 1;
     					}else{
     						return 0;
     					}
     				}
     			});
            }
           
        } catch (com.sg123.exception.NullParameterException e) {
            e.printStackTrace();
            throw new NullParameterException("Parameter cannot be null (getCardListByUserId)");
        } catch (com.sg123.exception.PinNotFoundException e) {
             e.printStackTrace();
            throw new PinNotfoundException("Pin not fount");
        } catch (com.sg123.exception.GeneralException e) {
             e.printStackTrace();
           throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), e);
        }
        List<PinLineResponse> pinLineResponses =new ArrayList<PinLineResponse>();
        responses.setUserId(userId);
        if (clientPinList.size() > 0) {

            for (com.sg123.model.platformprepay.Pin pin : clientPinList) {
                try {
                    System.out.println("PinStatus"+pin.getPinStatus().getId());
                    if (pin.getPinStatus().getId().equals(PinStatus.PIN_AVAILABLE_STATE)) {
                        PinLineResponse lineResponse = new PinLineResponse();
                        lineResponse.setBalance(pin.getCurrency().getSymbol() + pin.getCurrentBalance());
                        lineResponse.setSerial(pin.getSerial().toString());
                        lineResponse.setSecret(pin.getPassword());
                        System.out.println("pinfree size" + pin.getPinsFree().size());
                        String phoneNumber = pin.getPinsFree() != null && pin.getPinsFree().size() > 0 ? pin.getPinsFree().get(0).toString() : null;
                        System.out.println("phoneNumber" + phoneNumber);
                        if (phoneNumber != null) {
                            List<String> accessNumbers = getAccessNumberByPhoneNumber(userData, phoneNumber);
                            lineResponse.setAccessNumber(accessNumbers.get(0));
                        }
                        pinLineResponses.add(lineResponse);
                    }
                } catch (InvalidAccountException ex) {
                    throw new InvalidAccountException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName(), "Account - Login - Password"), ex);
                } catch (DisabledAccountException ex) {
                    throw new DisabledAccountException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName(), "Disabled Account"), null);
                } catch (InvalidPhoneNumberException ex) {
                    throw new InvalidPhoneNumberException(ex.getMessage());
                } catch (GeneralException e) {
                     throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), e);
                }
            }
            responses.setPinLineResponses(pinLineResponses);
        } else {
           throw new PinNotfoundException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName(), null), null);
        }
        return responses;
    }

    private static String getCorrectAmount(String currencySymbol, float amount, int roundTo) {
        String format="0.0";
        for(int i=1; i<roundTo; i++)
            format+="0";
        DecimalFormat decimalFormat = new DecimalFormat(format);
        if (amount >= 0) {
            return currencySymbol + decimalFormat.format(round(amount, roundTo));
        } else {
            return "-" + currencySymbol + decimalFormat.format(round(amount * -1, roundTo));
        }
    }

    private static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Double.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    public List<Dn> getAccessNumberByAniBody(AccountData userData,String phoneNumber) throws com.sg123.exception.GeneralException, com.sg123.exception.NullParameterException, Exception
    {
    	Account account = validateAccount(userData);
    	List<Dn>  dns = null;
    	com.sg123.ejb.PlanEJB planEJB = (com.sg123.ejb.PlanEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PLAN_EJB);
    	//1000L es un simple cable para que devuelva todos los numeros de acceso.
    	dns  =   planEJB.getAccessNumberByAni(ServiceConstans.SERVICE_ID,phoneNumber, null);
    	
    	return dns;
    }
    
    
    
    public List<String> getAccessNumberByPhoneNumber(AccountData userData,String phoneNumber) throws NullParameterException,InvalidAccountException, DisabledAccountException, InvalidPhoneNumberException, GeneralException {
        Account account = validateAccount(userData);
        List<String> accessNumbers = new ArrayList<String>();
        if (phoneNumber == null) {
            throw new NullParameterException("Parameter phoneNumber cant be null in method getAccessNumberByPhoneNumber");
        }
        com.interax.telephony.service.remote.callingcard.CallingCardPinEJB callingCardPinEJB = (com.interax.telephony.service.remote.callingcard.CallingCardPinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CALLING_CARD_PIN_EJB);
         //Valida que sea un numero valido para USA.
//        if (!GeneralUtils.isValidUSAPhoneNumber(phoneNumber)) {
//            throw new InvalidPhoneNumberException("Invalid phoneNumber: " + phoneNumber);
//        }
        try {
            accessNumbers = callingCardPinEJB.getAccessNumbersByAni(ServiceConstans.SERVICE_ID, phoneNumber, 1L);
            System.out.println("*****accessNumber****"+ accessNumbers.get(0)!=null?accessNumbers.get(0): "sin numero");
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        if (accessNumbers.isEmpty()){
            accessNumbers.add("7865223261");
        }
        return accessNumbers;
    }

    public List<com.sg123.model.Code> orderCodeList(List<com.sg123.model.Code> codes) {
        for (int i = 0; i < codes.size() - 1; i++) {
            for (int j = i; j < codes.size(); j++) {
                if (codes.get(i).getDestination().compareTo(codes.get(j).getDestination()) > 0) {
                    com.sg123.model.Code aux = codes.get(j);
                    codes.set(j, codes.get(i));
                    codes.set(i, aux);
                }

            }
        }
        return codes;
    }

    private boolean IsACodeParent(com.sg123.model.Code code, Long idParent) {
        if (code == null) {
            return false;
        }
        if (code.getId().equals(idParent)) {
            return true;
        }
        return IsACodeParent(code.getParentCode(), idParent);
    }

    
    
    
    
    

    public float pinBalance(AccountData userData, String phoneNumber, String serial) throws NullParameterException, InvalidAccountException, DisabledAccountException, InvalidPhoneNumberException, RegisterNotFoundException, GeneralException, PinDisabledException, PinFreeNotfoundException{
        float pinBalance;
        validateAccount(userData);
        if (serial == null && phoneNumber == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "serial - phoneNumber - customerLogin"), null);
        }
        com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);
        try {
            com.sg123.model.platformprepay.Pin pin = null;

            if (serial != null) {
                pin = pinEJB.loadPin(new Long(serial));
            } else {//phoneNumber
                pin = pinEJB.getPinFreeByAni(new Long(phoneNumber)).getPin();
                pin = pinEJB.loadPin(pin.getSerial());//Hago el cambio aqui para no hacerlo en SisacEJB - Issue: LAZY Relationship
            }
            if(pin.getCurrentBalance().equals(Float.MAX_VALUE)){
            	pinBalance = 0;
            }
            else{
            	pinBalance = pin.getCurrentBalance();
            }
            
        } catch (PinNotFoundException e) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), null);
        } catch (PinFreeNotFoundException e) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), null);

        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), null);
        }
        return pinBalance;

    }

    private List<com.sg123.model.invoice.InvoiceDetail> findCallDetailsFromPin(String beginning, String ending, com.sg123.model.platformprepay.Pin pin) throws NullParameterException, EmptyListException,GeneralException {
        List<com.sg123.model.invoice.InvoiceDetail> invoiceDetails = new ArrayList<com.sg123.model.invoice.InvoiceDetail>();
        com.sg123.ejb.InvoiceEJB invoiceEJB = (com.sg123.ejb.InvoiceEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.INVOICE_EJB);
        String format = "dd/MM/yyyy";

        Calendar begin = DateUtils.calendarFromString(beginning, format);
        begin.set(Calendar.HOUR, 0);
        begin.set(Calendar.MINUTE, 0);
        begin.set(Calendar.SECOND, 0);

        Calendar end = DateUtils.calendarFromString(ending, format);
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        try {
            invoiceDetails = invoiceEJB.getInvoiceDetailsByPin(pin.getSerial(), new java.sql.Timestamp(begin.getTime().getTime()), new java.sql.Timestamp(end.getTime().getTime()));
        } catch (com.sg123.exception.NullParameterException ex) {
            throw new NullParameterException("Parameter serial or beginning or ending cant be null in method findCallDetailsFromPin");
        } catch (InvoiceDetailNotFoundException ex) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName(), null), null);
        } catch (com.sg123.exception.GeneralException ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), null);
        }
        return invoiceDetails;
    }

    private String dateFormatTemplate(int languageId) {
        switch (languageId) {
            case 1:
                return "dd-MM-yyyy HH:mm:ss";
            default:
                return "yyyy-MM-dd HH:mm:ss";
        }
    }

    public void updateWebUserByPin(AccountData userData, String email, String phoneNumber) throws NullParameterException, InvalidAccountException, DisabledAccountException, RegisterNotFoundException, GeneralException {
        com.sg123.ejb.UserEJB sisacUserEJB = (com.sg123.ejb.UserEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.USER_EJB);
        com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);
        com.sg123.model.WebUser sisacWebUser = null;
        com.sg123.model.WebUser sisacWebUser1 = null;
        com.sg123.model.utils.Enterprise sisacEnterprise = new com.sg123.model.utils.Enterprise();
        sisacEnterprise.setId(com.sg123.model.utils.Enterprise.WIXTEL_USA);
        boolean found = false;
        try {
            sisacWebUser = sisacUserEJB.getWebUserByLogin(email, sisacEnterprise.getId());
            found = true;
        } catch (com.sg123.exception.WebUserNotFoundException ex) {
            found = false;
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), ex);
        }
        try {
            sisacWebUser1 = sisacUserEJB.getWebUserByLogin(phoneNumber, sisacEnterprise.getId());
            if (found) {
                System.out.println("encontro");
                List<com.sg123.model.platformprepay.Pin> clientPinList = new ArrayList<com.sg123.model.platformprepay.Pin>();
                try {
                    clientPinList = pinEJB.getPinsByWebUserAndServiceFamily(sisacWebUser1.getId(), ServiceFamily.CC_PIN_LINE);
                } catch (com.sg123.exception.NullParameterException e) {
                    e.printStackTrace();
                    throw new NullParameterException("Parameter cannot be null (getCardListByUserId)");
                } catch (com.sg123.exception.PinNotFoundException e) {
                    e.printStackTrace();
                    throw new PinNotfoundException("Pin not fount");
                } catch (com.sg123.exception.GeneralException e) {
                    e.printStackTrace();
                    throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), e);
                }
                if (clientPinList.size() > 0) {
                    for (com.sg123.model.platformprepay.Pin pin : clientPinList) {
                        try {
                            System.out.println("PinStatus" + pin.getPinStatus().getId());
                            if (pin.getPinStatus().getId().equals(PinStatus.PIN_AVAILABLE_STATE)) {
                               System.out.println("cambiar web user "+ pin.getWebUser().getId() + " del pin "+pin.getSerial()+" por "+ sisacWebUser.getId());
//                               pinEJB.updateWebUserByPin(pin.getSerial(),sisacWebUser);
                            }
                        }  catch (Exception e) {
                            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), e);
                        }
                    }
                }
            }else{
                System.out.println("no encontro");
                System.out.println("Cambiar el login del webUser "+sisacWebUser1.getId()+" anterior "+sisacWebUser1.getLogin()+"por "+email);
                sisacWebUser1.setLogin(email);
                sisacUserEJB.updateWebUser(sisacWebUser1);
                System.out.println("Login cambiado");
            }
        } catch (PinNotfoundException ex) {
        } catch (com.sg123.exception.WebUserNotFoundException ex) {
            if (!found) {
                throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), null), ex);
            }
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), ex);
        }

    }

    //Este método se hizo de esta manera por no tocar el SISAC

    public List<com.sg123.model.payment.PaymentInfo> listPaymentInfo(AccountData accountData, Long webUserId) throws NullParameterException,InvalidAccountException, DisabledAccountException, PaymentInfoNotFoundException, GeneralException{
    
    	if (webUserId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "webUserId"), null);
        }
    	com.sg123.ejb.UserEJB sisacUserEJB = (com.sg123.ejb.UserEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.USER_EJB);
    	com.sg123.ejb.ContractEJB contractEJB = (com.sg123.ejb.ContractEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CONTRACT_EJB);
    	List<com.sg123.model.payment.PaymentInfo> paymentOptions = new ArrayList<com.sg123.model.payment.PaymentInfo>() ;
    	List<com.sg123.model.payment.PaymentInfo> paymentOptionsByWebUser = null;
    	List<com.sg123.model.payment.PaymentInfo> paymentOptionsByCustomer = null;
        com.sg123.model.WebUser webUser = null;
		
	      try
	      {
	          if (webUserId != null)
	          {
	         //BUSCO LOS PAYMENT INFO QUE TENGAN ORDENES ASOCIADAS EN SISAC
	        	  paymentOptionsByWebUser = contractEJB.getPaymentsInfoByWebUser(webUserId);
	          }
	      }
          catch (com.sg123.exception.PaymentInfoNotFoundException e)
          {
        	 //NO SE HACE NADA DEBIDO A QUE Falta buscar por customer
          }
          catch (Exception e)
          {
        	 throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), e);
          }
          try
          {
        	//BUSCO LOS PAYMENT INFO QUE TENGA EL CUSTOMER SIN ORDENES ASOCIADAS EN SISAC
      		  webUser = sisacUserEJB.loadWebUserById(webUserId);
              if (webUser != null)
              {
                  com.sg123.model.contract.Customer customer = webUser.getPrincipalCustomer();
                  paymentOptionsByCustomer = contractEJB.getPaymentInfosByCustomer(customer.getId(), false);
               
              }
          }
          catch (com.sg123.exception.PaymentInfoNotFoundException e)
          {
        	  if(paymentOptionsByWebUser == null || paymentOptionsByWebUser.isEmpty()){
        		  throw new PaymentInfoNotFoundException(logger, sysError.format(EjbConstants.ERR_PAYMENTINFO_NOT_FOUND, this.getClass(), getMethodName(), null), e);
        	  }
          }
          catch (Exception e)
          {
        	 throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), e);
          }
          if(paymentOptionsByWebUser!= null && !paymentOptionsByWebUser.isEmpty()){
        	  
        	  //AGREGO LOS PAYMENT INFO DE LAS ORDENES
        	  paymentOptions.addAll(paymentOptionsByWebUser);
          }
          if(paymentOptionsByCustomer!= null && !paymentOptionsByCustomer.isEmpty()){
        	  for(com.sg123.model.payment.PaymentInfo pi : paymentOptionsByCustomer){
        		//AGREGO LOS PAYMENT INFO DEL CUSTOMER SIN ORDEN Y QUE NO SE ENCUENTREN EN LAS AGREGADAS CON ORDENES
        		//ESTE FILTRO SE PUEDE REALIZAR DIRECTAMENTE DESDE EL SISAC EXCLUYENDO ID YA EXISTENTES DEL QUERY
        		  if(!listContainPaymemtInfo(paymentOptions, pi))  paymentOptions.add(pi);
      		  }
        	  
          }
          return paymentOptions;
    }

    private boolean listContainPaymemtInfo(List<com.sg123.model.payment.PaymentInfo> paymentInfos, com.sg123.model.payment.PaymentInfo paymentInfo){
    	for(com.sg123.model.payment.PaymentInfo pi : paymentInfos){
			  if(pi.getId() == paymentInfo.getId()) return true;
		}
    	return false;
    } 
    
      public int numberPaymentInfoByWebUserId(Long webUserId) throws NullParameterException, GeneralException{
        if (webUserId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "webUserId"), null);
        }
        int numberPaymentInfo = 0;
    	List<com.sg123.model.payment.PaymentInfo> paymentOptions = null;
          try
          {
              com.sg123.ejb.ContractEJB contractEJB = (com.sg123.ejb.ContractEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CONTRACT_EJB);
              if (webUserId != null)
              {
                  System.out.println("******WebUser En Service :" + webUserId);
                  System.out.println("******ContractEJB En Service :" + contractEJB);
                  paymentOptions = contractEJB.getPaymentsInfoByWebUser(webUserId);
                  numberPaymentInfo = paymentOptions.size();
              }
          }
          catch (com.sg123.exception.PaymentInfoNotFoundException e)
          {
        	  numberPaymentInfo = 0;

          }
          catch (Exception e)
          {
        	 throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), e);
          }
    	return numberPaymentInfo;
    }

    public com.sg123.model.payment.PaymentInfo validatePaymentInfoSisac(boolean newPaymentInfo, Long paymentInfoId, com.sg123.model.payment.PaymentInfo paymentInfo, Long webUserId) throws NullParameterException, PaymentInfoNotFoundException, InvalidCreditCardDateException, InvalidPaymentInfoException, GeneralException {
        com.sg123.ejb.ContractEJB contractEJB = (com.sg123.ejb.ContractEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CONTRACT_EJB);
        com.sg123.ejb.CardFraudEJB cardFraudEJB = (com.sg123.ejb.CardFraudEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CARDFRAUD_EJB);
        com.sg123.ejb.UserEJB userEJB = (com.sg123.ejb.UserEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.USER_EJB);
        com.interax.telephony.service.remote.balancerecharge.BalanceRechargeEJB balanceRechargeEJB = (com.interax.telephony.service.remote.balancerecharge.BalanceRechargeEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.BALANCE_RECHARGE_EJB);
        String comment = null;
         com.sg123.model.WebUser webUser = null;
        try {
            webUser = userEJB.loadWebUserById(webUserId);
            paymentInfo.setCustomer(webUser.getPrincipalCustomer());
        } catch (com.sg123.exception.WebUserNotFoundException ex) {
            ex.printStackTrace();
        } catch (com.sg123.exception.GeneralException ex) {
            ex.printStackTrace();
        }
        if (paymentInfoId != null) {
            try {
                System.out.println("Buscar paymentInfo" + paymentInfoId);
                paymentInfo = contractEJB.loadPaymentInfo(paymentInfoId);
                System.out.println("Encontro paymentInfo" + paymentInfoId);
            } catch (com.sg123.exception.GeneralException e) {
                e.printStackTrace();
            } catch (com.sg123.exception.PaymentInfoNotFoundException e) {
                e.printStackTrace();
            } catch (com.sg123.exception.NullParameterException e) {
                e.printStackTrace();
            }
        }else{
            try {
                paymentInfo.setPaymentPartner(contractEJB.getDefaultPaymentPartnerByEnterprise(webUser.getEnterprise().getId()));
            } catch (com.sg123.exception.NullParameterException ex) {
                 ex.printStackTrace();
            } catch (PaymentPartnerNotFoundException ex) {
                 ex.printStackTrace();
            } catch (com.sg123.exception.GeneralException ex) {
                 ex.printStackTrace();
            }
        }
        if (paymentInfo.getType().getId().equals(PaymentType.CREDIT_CARD)) {
            //            if (!paymentInfo.getCreditCardType().getId().equals(CreditCardUtils.getCardID(paymentInfo.getCreditCardNumber())) || !CreditCardUtils.validate(paymentInfo.getCreditCardNumber())) {
            //                System.out.println("Tarjeta Invalida1");
            //                logger.error("Invalid credit card number");
            //                throw new InvalidPaymentInfoException("Invalid credit card number");
            //            }
            try {
                balanceRechargeEJB.validateCreditCard(paymentInfo.getCreditCardNumber());
            } catch (Exception e) {
                System.out.println("Tarjeta Invalida1");
                throw new InvalidPaymentInfoException("" + " paymentType");
            }
        } else {
            System.out.println("Tarjeta Invalida2");
            throw new InvalidPaymentInfoException("" + " paymentType");
        }
        Timestamp ts_now = new Timestamp(paymentInfo.getCreditCardDate().getTime());
        if (!CreditCardUtils.validateCreditCardDate(ts_now)) {
            throw new InvalidCreditCardDateException("Invalid creditcard date.");
        }
        String cardUsed = paymentInfo.getCreditCardNumber();
        System.out.println("cardUsed******"+cardUsed);
        com.sg123.model.utils.BannedCard bannedCard = new com.sg123.model.utils.BannedCard();
        bannedCard.setNumber(cardUsed);
        boolean bannedCardUsed = false;
        try {
            System.out.println("CardFraudEJB******"+cardFraudEJB);
            bannedCardUsed = cardFraudEJB.existBannedCard(bannedCard);
        } catch (com.sg123.exception.GeneralException e1) {
            e1.printStackTrace();
        } catch (com.sg123.exception.NullParameterException e1) {
            e1.printStackTrace();
        }
        comment = "La tarjeta de credito " + cardUsed + " ya estaba registrada con otro usuario.";
        if (bannedCardUsed) {
            try {
                System.out.println("Se desabilitara el webUser:" + webUserId);
                if (!ServiceConstans.TEST_MODE) {
                    userEJB.completelyDisableWebUser(webUserId, comment);
                }
                throw new InvalidPaymentInfoException("" + " bannedCardUsed");
            } catch (com.sg123.exception.NullParameterException e) {
                e.printStackTrace();
            } catch (com.sg123.exception.GeneralException e) {
                e.printStackTrace();
            }
        }
        try {
            //paymentInfo.getCreditCardNumber() != "5190902874125897" && paymentInfo.getCreditCardNumber() != "4941591947216257"
            if (newPaymentInfo && !userEJB.paymentInfoIsUnique(paymentInfo) && !ServiceConstans.TEST_MODE)
            {
                comment = "";
                System.out.println("Se desabilitara el webUser:" + webUserId + " el numero de tarjeta no es unico");
                userEJB.completelyDisableWebUser(webUserId, comment);
                bannedCard.setObservations("");
                bannedCard.setBank(null);
                com.sg123.model.utils.BannedCardType bannedCardType = null;
                try {
                    bannedCardType = cardFraudEJB.getBannedCardType(paymentInfo.getCreditCardType().getId());
                } catch (com.sg123.exception.BannedCardTypeNotFoundException e) {
                    e.printStackTrace();
                }
                bannedCard.setCardType(bannedCardType);
                try {
                    cardFraudEJB.saveBannedCard(bannedCard);
                } catch (com.sg123.exception.BannedCardNotValid e) {
                    //        			log.error("El número de tarjeta no es válido",e);
                    e.printStackTrace();
                } catch (com.sg123.exception.BannedCardAllReadyExist e) {
                    //        			log.error("La tarjeta ya ha sido registrada",e);
                    e.printStackTrace();
                } catch (com.sg123.exception.GeneralException e) {
                    //        			log.error("Ocurrió un Error GeneralException al guardar la tarjeta frauduleta",e);
                    e.printStackTrace();
                } catch (com.sg123.exception.NullParameterException e) {
                    //        			log.error("Ocurrió un Error NullParameterException al guardar la la tarjeta fraudulenta", e);
                    e.printStackTrace();
                } catch (Exception e) {
                    //        			log.error("Ocurrió un Error Exception al guardar la tarjeta fraudulenta ", e);
                    e.printStackTrace();
                }
                throw new InvalidPaymentInfoException("" + " bannedCardUsed");
            }
        } catch (com.sg123.exception.NullParameterException e) {
            e.printStackTrace();
        } catch (com.sg123.exception.GeneralException e) {
            e.printStackTrace();
        }
        try {
            if (paymentInfo.getId() == null && newPaymentInfo) {
            paymentInfo = contractEJB.savePaymentInfo(paymentInfo);
            }
        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), e);
        }
        return paymentInfo;
    }

    public void deleteSisacPaymentInfo(Long paymentInfoId) throws NullParameterException, GeneralException {
        if (paymentInfoId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "paymentInfoId"), null);
        }
        try {
            com.sg123.ejb.ContractEJB contractEJB = (com.sg123.ejb.ContractEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CONTRACT_EJB);
            contractEJB.deletePaymentInfo(paymentInfoId);
        } catch (Exception e) {
            e.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), e);
        }
    }

    @Override
	public List<FavoriteDestinationCombination> getFavoriteDestinationCombination(Long pinfreeId)throws NullParameterException, GeneralException,FavoriteDestinationNotFoundException {
		if (pinfreeId == null) {
			throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(),getMethodName(), "pinfreeId"), null);
		}
		List<FavoriteDestinationCombination> favoriteDestinationCombinations = null;
		try {
			com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);
			favoriteDestinationCombinations = pinEJB.getFavoriteDestinationCombination(pinfreeId);
		} catch (com.sg123.exception.FavoriteDestinationNotFoundException e) {
			e.printStackTrace();
			throw new FavoriteDestinationNotFoundException("FavoriteDestinationNotFoundException");
		} catch (Exception e) {
			e.printStackTrace();
			throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(),	getMethodName(), null), e);
		}
		return favoriteDestinationCombinations;
	}

	@Override
	public FavoriteDestinationCombination saveFavoriteDestinationCombination(Long pinfreeId,int dtmf, Long accessNumberId, Long dni, String description)throws NullParameterException, GeneralException,	FavoriteDestinationNotFoundException,RegisterNotFoundException {
		if (pinfreeId == null) {
			throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(),getMethodName(), "pinfreeId"), null);
		} else if (accessNumberId == null) {
			throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(),getMethodName(), "accessNumberId"), null);
		} else if (dni == null) {
			throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(),getMethodName(), "dni"), null);
		}
		com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);
		com.sg123.ejb.PlanEJB planEJB = (com.sg123.ejb.PlanEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PLAN_EJB);
//		FavoriteDestination favoriteDestination = new FavoriteDestination();
		com.sg123.model.platformprepay.PinFree pinFree = null;
		try {

			pinFree = pinEJB.getPinFreeByAni(pinfreeId);
		} catch (com.sg123.exception.PinFreeNotFoundException e1) {
			throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(),getMethodName(), null), null);
		} catch (com.sg123.exception.GeneralException e1) {
			throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(),getMethodName(), null), e1);
		}
//		favoriteDestination.setPinFree(pinFree);

		FavoriteDestinationCombination favoriteDestinationCombination = new FavoriteDestinationCombination();
		favoriteDestinationCombination.setDni(dni);
		favoriteDestinationCombination.setDtmf(dtmf);
		favoriteDestinationCombination.setDescription(description);
		favoriteDestinationCombination.setPinFree(pinFree);
		Dn dn = null;
		try {
			dn = planEJB.loadDn(accessNumberId);
		} catch (com.sg123.exception.GeneralException e1) {
			e1.printStackTrace();
			throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(),getMethodName(), null), e1);
		} catch (DnNotFoundException e1) {
			e1.printStackTrace();
			throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(),getMethodName(), null), null);
		} catch (com.sg123.exception.NullParameterException e1) {
			e1.printStackTrace();
			throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(),getMethodName(), "accessNumberId"), null);
		}
		favoriteDestinationCombination.setDn(dn);
		try {
			favoriteDestinationCombination = pinEJB.saveFavoriteDestinationCombination(favoriteDestinationCombination);
		} 
		catch (Exception e) {
			e.printStackTrace();
			throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(),getMethodName(), null), e);
		}
		return favoriteDestinationCombination;
	}

	@Override
	public FavoriteDestinationCombination updateFavoriteDestinationCombination(Long favoriteDestinationCombinationId,Long pinfreeId, Long accessNumberId, Long dni, String description, Integer dtmf )throws NullParameterException, RegisterNotFoundException,GeneralException {
		if (pinfreeId == null) {
			throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(),getMethodName(), "pinfreeId"), null);
		} else if (accessNumberId == null) {
			throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(),getMethodName(), "accessNumberId"), null);
		} else if (dni == null) {
			throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(),getMethodName(), "dni"), null);
		} else if (dtmf == null) {
			throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(),getMethodName(), "dtmf"), null);
		}
		com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);
		com.sg123.ejb.PlanEJB planEJB = (com.sg123.ejb.PlanEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PLAN_EJB);
		com.sg123.model.platformprepay.PinFree pinFree = null;
		try {
			pinFree = pinEJB.getPinFreeByAni(pinfreeId);
		} catch (PinFreeNotFoundException e1) {
			throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(),getMethodName(), null), null);
		} catch (com.sg123.exception.GeneralException e1) {
			throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(),getMethodName(), null), e1);
		}
		
		FavoriteDestinationCombination favoriteDestinationCombination = new FavoriteDestinationCombination();
		favoriteDestinationCombination.setId(favoriteDestinationCombinationId);
		favoriteDestinationCombination.setDni(dni);
		favoriteDestinationCombination.setPinFree(pinFree);
		favoriteDestinationCombination.setDescription(description);
		favoriteDestinationCombination.setDtmf(dtmf);
		Dn dn = null;
		try {
			dn = planEJB.loadDn(accessNumberId);
		} catch (com.sg123.exception.GeneralException e1) {
			e1.printStackTrace();
			throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(),getMethodName(), null), e1);
		} catch (DnNotFoundException e1) {
			e1.printStackTrace();
			throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(),getMethodName(), null), null);
		} catch (com.sg123.exception.NullParameterException e1) {
			e1.printStackTrace();
			throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(),getMethodName(), "accessNumberId"), null);
		}
		favoriteDestinationCombination.setDn(dn);
		try {
			favoriteDestinationCombination = pinEJB.saveFavoriteDestinationCombination(favoriteDestinationCombination);
		} catch (Exception e) {
			e.printStackTrace();
			throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(),getMethodName(), null), e);
		}
		return favoriteDestinationCombination;
	}

	@Override
	public void deleteFavoriteDestinationCombination(Long favoriteDestinationCombinationId)	throws NullParameterException, GeneralException {
		if (favoriteDestinationCombinationId == null) {
			throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(),getMethodName(), "favoriteDestinationCombinationId"), null);
		}
		com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);
		try {
			pinEJB.deleteFavoriteDestinationCombination(favoriteDestinationCombinationId);
		} catch (Exception e) {
			e.printStackTrace();
			throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(),getMethodName(), null), e);
		}
	}

	@Override
	public FavoriteDestinationCombination getFavoriteDestinationCombination(Long pinfreeId, Long accessNumberId) throws NullParameterException, GeneralException,	FavoriteDestinationCombinationNotFoundException {
		if (pinfreeId == null) {
			throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(),getMethodName(), "pinfreeId"), null);
		} else if (accessNumberId == null) {
			throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(),getMethodName(), "accessNumberId"), null);
		}
		com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);
		FavoriteDestinationCombination destinationCombination = null;
		try {
			destinationCombination = pinEJB.getFavoriteDestinationCombination(pinfreeId, accessNumberId);
		} catch (com.sg123.exception.FavoriteDestinationCombinationNotFoundException e) {
			e.printStackTrace();
			throw new FavoriteDestinationCombinationNotFoundException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(),getMethodName(), null), e);
		} catch (Exception e) {
			e.printStackTrace();
			throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(),getMethodName(), null), e);
		}
		return destinationCombination;
	}

	  @Override
	    public void changePassword(String login, String oldPassword, String newPassword, Long enterpriseId) throws NullParameterException,InvalidPasswordException,
	            InvalidAccountException, DisabledAccountException, GeneralException {
	        if (login == null) {
	            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "login"), null);
	        }
	        if (oldPassword == null) {
	            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "oldPassword"), null);
	        }
	        if (newPassword == null) {
	            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "newPassword"), null);
	        }
	        if (enterpriseId == null) {
	            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "enterpriseId"), null);
	        }

	        com.sg123.ejb.UserEJB userEJB = (com.sg123.ejb.UserEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.USER_EJB);

	        try {
	            userEJB.changePassword(login, oldPassword, newPassword, enterpriseId);
	        } catch (com.sg123.exception.InvalidPasswordException e) {
	            throw new InvalidPasswordException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), null), e);
	        } catch (com.sg123.exception.GeneralException e) {
	           throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(),getMethodName(), null), e);
	        }
	    }
	  
	  public List<com.sg123.model.plan.Dn> getAccesNumber(String ani) throws NullParameterException, GeneralException {
          if (ani == null) {
			throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(),getMethodName(), "ani"), null);
                 // Constants.pinlineServiceCode = 12;
          } 
          List<com.sg123.model.plan.Dn> dns = new ArrayList<com.sg123.model.plan.Dn>();
          com.sg123.ejb.PlanEJB planEJB;
          planEJB = (com.sg123.ejb.PlanEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PLAN_EJB);   
      try {
          dns = planEJB.getAccessNumberByAni(Constants.PINLINE_SERVICE_CODE, ani, 10L); 
      } catch (com.sg123.exception.GeneralException ex) {
          throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(),getMethodName(), null), ex);
      } catch (com.sg123.exception.NullParameterException ex) {
          throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(),getMethodName(), "ani"), null);
      }   catch (Exception e) {
          e.printStackTrace();
          throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(),getMethodName(), null), e);
	}
          return dns;
	}

       private void saveRUUserHasWebUser(Long userId, com.sg123.model.WebUser webUser) throws NullParameterException, GeneralException {
        try {
           RUUserHasWebUser userHasWebUser = new RUUserHasWebUser();
           com.sg123.ejb.UserEJB sisacUserEJB = (com.sg123.ejb.UserEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.USER_EJB);
           userHasWebUser.setWebUser(webUser);
           userHasWebUser.setUserId(userId);
           if (!sisacUserEJB.searchWebUserIdByRUUserId(webUser.getId(), userId))
             sisacUserEJB.registerUserHasWebUser(userHasWebUser);
        } catch(Exception e) {
            e.printStackTrace();
        }
       }

      public Long loadWebUserIdByRUUserId(Long userId) {
        Long webUserId = 0L;
        try {
            com.sg123.ejb.UserEJB sisacUserEJB = (com.sg123.ejb.UserEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.USER_EJB);
            webUserId = sisacUserEJB.loadWebUserIdByRUUserId(userId);
            System.out.println("webUserId:" + webUserId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return webUserId;
    }

      private String loadTokenOrdenInvoive() {
        String token = null;
        try {
            WsSalesRecordProxy salesRecordProxy = new WsSalesRecordProxy();
            WsLoginResponse loginResponse = salesRecordProxy.loginSalesRecord(ServiceConstans.USER_WS_ORDEN_INVOICE, ServiceConstans.PASSWORD_WS_ORDEN_INVOICE);
            System.out.println("codeLoginOrder"+loginResponse.getCode());
            if (loginResponse.getCode().equals(Constants.TRANSACTION_SUCCESSFUL)) {
                token = loginResponse.getToken();
            }
        } catch (Exception ex1) {
            ex1.printStackTrace();
        }
        return token;
    }

      private String loadTokenWSPayment() {
        String token = null;
        try {
             WSPaymentMethodProxy paymentMethodProxy = new WSPaymentMethodProxy();
//            com.alodiga.ws.payment.services.WsLoginResponse loginResponse = paymentMethodProxy.loginWS("alodiga", "f0a0712c5d4de397a87d639313161b88");
            com.alodiga.ws.payment.services.WsLoginResponse loginResponse = paymentMethodProxy.loginWS("yalmea", "123456");
            System.out.println("codelogin"+loginResponse.getCode());
            System.out.println("TOKENNNNNNNNNNNN"+ loginResponse.getToken());
            if (loginResponse.getCode().equals(Constants.TRANSACTION_SUCCESSFUL)) {
                token = loginResponse.getToken();
            }
        } catch (Exception ex1) {
            ex1.printStackTrace();
        }
        return token;
    }

        public PaymentInfo createPaymentInfo(Account account, Customer customer, String PaymentInfoIdRU) throws NullParameterException, GeneralException {
        PaymentInfo paymentInfo = new PaymentInfo();
        try {
            System.out.println("createPaymentInfo");
            WSPaymentMethodProxy paymentMethodProxy = new WSPaymentMethodProxy();
            String token = loadTokenWSPayment();

            WsPaymentInfoResponse wsPaymentInfoResponse = paymentMethodProxy.getPaymentInfoByIdComplete(token, PaymentInfoIdRU);
            System.out.println("BuscarPaymentInfo code"+wsPaymentInfoResponse.getCode());
            if (wsPaymentInfoResponse.getCode().equals(Constants.TRANSACTION_SUCCESSFUL)) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                System.out.println("creditCardDate***" + wsPaymentInfoResponse.getPaymentInfoModel().getCreditCardDate());
                Date ccdate = format.parse(wsPaymentInfoResponse.getPaymentInfoModel().getCreditCardDate());
                Timestamp ccTimestamp = new Timestamp(ccdate.getTime());
                paymentInfo.setCreditCardDate(ccTimestamp);
                paymentInfo.setCreditCardNumber(wsPaymentInfoResponse.getPaymentInfoModel().getCreditCardNumber());
                paymentInfo.setCreditCardName(wsPaymentInfoResponse.getPaymentInfoModel().getCreditCardName());
                paymentInfo.setCreditCardCvv(wsPaymentInfoResponse.getPaymentInfoModel().getCreditCardCVV());
                paymentInfo.setBeginningDate(new Timestamp((new Date()).getTime()));
                CreditcardType creditcardType = new CreditcardType();
                creditcardType.setId(Long.parseLong(wsPaymentInfoResponse.getPaymentInfoModel().getCardTypeModel().getId()));
                paymentInfo.setCreditcardType(creditcardType);
                PaymentType paymentType = new PaymentType();
                paymentType.setId(PaymentType.CREDIT_CARD);
                paymentInfo.setPaymentType(paymentType);
                PaymentPatner patner = new PaymentPatner();
                patner.setId(PaymentPatner.AUTHORIZE);
                paymentInfo.setPaymentPatner(patner);
                paymentInfo.setAccount(account);
                paymentInfo.setCustomer(customer);
                paymentInfo.setPaymentInfoIdRU(PaymentInfoIdRU);
                Address newAddress = new Address();
                newAddress.setAddress(wsPaymentInfoResponse.getPaymentInfoModel().getAddressModel().getAddress1());
                newAddress.setZipCode(wsPaymentInfoResponse.getPaymentInfoModel().getAddressModel().getZipcode());
                Country country = new Country();
                country.setId(Long.parseLong(wsPaymentInfoResponse.getPaymentInfoModel().getAddressModel().getCountryCode()));
                newAddress.setCountry(country);
                State state = new State();
                state.setId(Long.parseLong(wsPaymentInfoResponse.getPaymentInfoModel().getAddressModel().getStateCode()));
                newAddress.setState(state);
                City city = new City();
                city.setId(Long.parseLong(wsPaymentInfoResponse.getPaymentInfoModel().getAddressModel().getCityCode()));
                newAddress.setCity(city);
                try {
                    if (wsPaymentInfoResponse.getPaymentInfoModel().getAddressModel().getCountyCode() != null || wsPaymentInfoResponse.getPaymentInfoModel().getAddressModel().getCountyCode().equals("")) {
                        County county = new County();
                        county.setId(Long.parseLong(wsPaymentInfoResponse.getPaymentInfoModel().getAddressModel().getCountyCode()));
                        newAddress.setCounty(county);
                    }
                } catch (Exception e) {
                }
                newAddress = customerEJB.saveAddress(newAddress);
                paymentInfo.setAddress(newAddress);
                paymentInfo = transactionEJB.savePaymentInfo(paymentInfo);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return paymentInfo;
    }
	  
        public List<Rent> getRentsByService(AccountData userData,Long serviceId) throws GeneralException, NullParameterException, RegisterNotFoundException,InvalidAccountException, DisabledAccountException {
            com.sg123.ejb.PlanEJB planEJB = (com.sg123.ejb.PlanEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PLAN_EJB);
            List<Rent> plans = null;
            validateAccount(userData);
            if (serviceId == null) {
                throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "serviceId"), null);
            }
            try {
                plans = planEJB.getRentList(serviceId);
            } catch (com.sg123.exception.NullParameterException e) {
                e.printStackTrace();
                throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "serviceId"), null);

            } catch (RentNotFoundException e) {
                e.printStackTrace();
                throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), null), e);
            } catch (com.sg123.exception.GeneralException e) {
                e.printStackTrace();
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), e);

            }
            if (plans != null && !plans.isEmpty()) {
                return plans;
            }
            return null;
        }

        public com.sg123.model.plan.Plan loadPlan(Long planId) throws NullParameterException, RegisterNotFoundException, GeneralException {
            com.sg123.ejb.PlanEJB planEJB = (com.sg123.ejb.PlanEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PLAN_EJB);
            Plan plan = null;
            try {
                plan = planEJB.loadPlan(planId);
            } catch (com.sg123.exception.NullParameterException e) {
                e.printStackTrace();
                throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "serviceId"), null);

            } catch (PlanNotFoundException e) {
                e.printStackTrace();
                throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), null), e);
            } catch (com.sg123.exception.GeneralException e) {
                e.printStackTrace();
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), e);

            }
            if (plan != null) {
                return plan;
            }
            return null;
        }

        public List<com.sg123.model.plan.FavoriteCountry> getFavoriteCountries(AccountData userData,Long serviceId) throws GeneralException, NullParameterException, RegisterNotFoundException,InvalidAccountException, DisabledAccountException {
            com.sg123.ejb.PlanEJB planEJB = (com.sg123.ejb.PlanEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PLAN_EJB);
            List<com.sg123.model.plan.FavoriteCountry> favoriteCountries = null;
            validateAccount(userData);
            if (serviceId == null) {
                throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "serviceId"), null);
            }
            try {
                favoriteCountries = planEJB.getFavoriteCountries(serviceId, 1);
            } catch (com.sg123.exception.GeneralException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), e);
            } catch (FavoriteCountryNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), null), e);
            }
            if (favoriteCountries != null && !favoriteCountries.isEmpty()) {
                return favoriteCountries;
            }
            return null;
        }

        public com.sg123.model.plan.FavoriteCountry loadFavoriteCountry(Long favoriteCountryId) throws GeneralException, RegisterNotFoundException, NullParameterException {
            com.sg123.ejb.PlanEJB planEJB = (com.sg123.ejb.PlanEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PLAN_EJB);
            FavoriteCountry fCountry = null;
            try {
                fCountry = planEJB.loadFavoriteCountry(favoriteCountryId);

            } catch (com.sg123.exception.GeneralException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), e);
            } catch (FavoriteCountryNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), null), e);

            } catch (com.sg123.exception.NullParameterException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "favoriteCountryId"), null);
            }

            return fCountry;
        }

        public com.sg123.model.contract.Contract loadContract(Long contractId) {


            return null;
        }

        public List<ContractResponse> getContractByRU(AccountData userData, Long registerUnifiedId) throws RegisterNotFoundException, NullParameterException, GeneralException, WebUserNotFoundException {
            com.sg123.ejb.ContractEJB contractEJB = (com.sg123.ejb.ContractEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CONTRACT_EJB);
            com.sg123.ejb.UserEJB userEJB = (com.sg123.ejb.UserEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.USER_EJB);
            com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);
            List<ContractResponse> contractRs = new ArrayList<ContractResponse>();

            try {
                Long webUserId = null;
                webUserId = userEJB.loadWebUserIdByRUUserId(registerUnifiedId);
                System.out.println("webUserId:"+webUserId+" ruID:"+registerUnifiedId);
                List<com.sg123.model.contract.Contract> contracts = contractEJB.getContractsByWebUser2(webUserId);
                if (contracts == null || contracts.isEmpty()) {
                    throw new ContractNotFoundException("Not contract found");
                }
                for (Contract contract : contracts) {
                    List<ContractHasService> contractServices = contract.getContractServices();
                    for (ContractHasService contractService : contractServices) {
                        try {
                            ContractHasService contractService1 = contractEJB.getContractHasService(contractService.getId());
                            List<ContractHasPin> contractPins = contractService1.getContractPins();
                            if (contractService1.getContractPins() != null && !contractService1.getContractPins().isEmpty()) {
                                ContractResponse contractResponse = new ContractResponse();
                                contractResponse.setAlias(contract.getAlias());
                                contractResponse.setId(contract.getId());
                                contractResponse.setBalance(contract.getBalance());
                                contractResponse.setIsPrepaid(0);
                                contractResponse.setBeginningDate(contract.getBeginningDate().toString());
                                contractResponse.setEndingDate(contract.getEndingDate() != null ? contract.getEndingDate().toString() : null);
                                contractResponse.setLastBillingDate(contract.getLastBillingDate() != null ? contract.getLastBillingDate().toString() : null);
                                List<PinLineResponse> pines = new ArrayList<PinLineResponse>();

                                for (ContractHasPin contractPin : contractPins) {
                                    com.sg123.model.platformprepay.Pin pin = contractPin.getPin();
                                    PinLineResponse pinResponse = new PinLineResponse();
                                    String balance ="0.0";
                                    if (pin.getBalance()!=Float.MAX_VALUE)
                                        balance = pin.getBalance().toString();
                                    pinResponse.setBalance(balance);
                                    pinResponse.setSerial(pin.getSerial().toString());
                                    pinResponse.setSecret(pin.getSecret());
                                    pines.add(pinResponse);
                                }
                                contractResponse.setPins(pines);
                                contractRs.add(contractResponse);
                            }
                        } catch (com.sg123.exception.ContractHasServiceNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
           }catch (ContractNotFoundException e) {
                e.printStackTrace();
                throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), null), e);
            } catch (com.sg123.exception.GeneralException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), e);
            } catch (com.sg123.exception.NullParameterException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "webUserId"), null);
            } catch (com.sg123.exception.WebUserNotFoundException e) {
                throw new WebUserNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), null), e);
            }
            return contractRs;
        }

        public com.sg123.model.contract.Contract saveContract(Contract contract) throws GeneralException, NullParameterException {
            com.sg123.ejb.ContractEJB contractEJB = (com.sg123.ejb.ContractEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PLAN_EJB);
            try {
                contract = contractEJB.saveContract(contract);
            } catch (com.sg123.exception.GeneralException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), e);
            } catch (com.sg123.exception.NullParameterException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "contractId"), null);
            }
            return contract;
        }

        public RechargePinResponse rechargePinless(AccountData userData, PaymentInfo paymentInfo, String phoneNumber, String serial, Float amount, String smsDestination, String externalId, Object object, String registerUniId, String deliveryAddressId, String billingAddressId, String paymentInfoId, String salesChannelId, String ordenSourceId) throws GeneralException, NullParameterException, InvalidAccountException, DisabledAccountException, InvalidPhoneNumberException, InvalidAmountException, InvalidAccountException, DisabledAccountException {
            Account account = validateAccount(userData);
            com.sg123.model.platformprepay.Pin pin = null;
            TransactionStatus transactionStatus = null;
            TransactionType transactionType = null;
            Long orderId = null;
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

//            com.sg123.model.payment.PaymentInfo paymentInfo = null;
            float newBalance = 0f;

            try {
                com.sg123.model.ecommerce.CustomService customService = null;
                //com.sg123.model.WebUser webUser = null;
                com.sg123.model.content.Segment segment = null;

                Long contractSerial = Long.valueOf(serial);
                if (serial != null) {
                    pin = pinEJB.loadPin(new Long(serial));
                } else {//phoneNumber
                    pin = pinEJB.getPinFreeByAni(new Long(phoneNumber)).getPin();
                    pin = pinEJB.loadPin(pin.getSerial());//Hago el cambio aqui para no hacerlo en SisacEJB - Issue: LAZY Relationship
                }
                com.sg123.model.WebUserSession webUserSession = userEJB.loadWebUserSessionByWebUser(pin.getWebUser().getId());
                Contract contract = contractEJB.getContractByPin(contractSerial);
                String transactionData = "Recarga Contrato " + contract.getId() + " de pinless electronico [monto)" + amount + "(monto] - [serial)" + serial + "(serial]" + "[cuenta - login)" + userData.getLogin();
                Recharge recharge = null;
                try {
                    recharge = this.processPayment(account, paymentInfo, amount);
                } catch (InvalidAmountException e) {
                    try {
                        transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_RECHARGE);
                        transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                        transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, e.getMessage(), account, null, null, null, "Error trying recharging pin. Number = " + phoneNumber + " Procesando el pago con Authorize.net. (MONTO INVALIDO)", externalId, null, null, null, null, paymentInfo, 0f, userData.getIpRemoteAddress());
                        this.saveTransaction(transaction);
                    } catch (Exception ex1) {
                    }
                    throw (e);
                } catch (PaymentDeclinedException e) {
                    try {
                        ServiceMailDispatcher.sendPinRechargeErrorMail(account, "Error durante la recarga : " + transactionData.toString(), "Paso 2: Procesando el pago con Authorize.net. (PAGO DECLINADO)", e);
                        transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_PURCHASE);
                        transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                        transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, e.getMessage(), account, null, null, null, "Error trying recharging pin. Number = " + phoneNumber + " Procesando el pago con Authorize.net. (PAGO DECLINADO)", externalId, null, null, null, null, paymentInfo, 0f, userData.getIpRemoteAddress());
                        this.saveTransaction(transaction);
                    } catch (Exception ex1) {
                    }
                    throw (e);
                } catch (InvalidPaymentInfoException e) {
                    try {
                        ServiceMailDispatcher.sendPinRechargeErrorMail(account, "Error durante la recarga : " + transactionData.toString(), "Paso 2: Procesando el pago con Authorize.net. (METODO DE PAGO INVALIDO)", e);
                        transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_PURCHASE);
                        transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                        transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, e.getMessage(), account, null, null, null, "Error trying recharging pin. Number = " + phoneNumber + " Procesando el pago con Authorize.net. (METODO DE PAGO INVALIDO)", externalId, null, null, null, null, paymentInfo, 0f, userData.getIpRemoteAddress());
                        this.saveTransaction(transaction);
                    } catch (Exception ex1) {
                    }
                    throw (e);
                } catch (Exception e) {
                    try {
                        ServiceMailDispatcher.sendPinRechargeErrorMail(account, "Error durante la recarga : " + transactionData.toString(), "Paso 2: Procesando el pago con Authorize.net. (ERROR GENERAL)", e);
                        transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_PURCHASE);
                        transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                        transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, e.getMessage(), account, null, null, null, "Error trying recharging pin. Number = " + phoneNumber + " Procesando el pago con Authorize.net. (ERROR GENERAL)", externalId, null, null, null, null, paymentInfo, 0f, userData.getIpRemoteAddress());
                        this.saveTransaction(transaction);
                    } catch (Exception ex1) {
                    }
                    throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ""), e);
                }

            com.sg123.model.contract.Customer customer = contract.getCustomer();
            newBalance = contract.getBalance() + amount;
            contractEJB.rechargeContract(contract.getId(), amount, "Contract recharge from customer selección");
            if (contract.getPaymentInfoIdRU()==null){
                contractEJB.updateContractByPaymentInfoRUID(contract.getId(), Long.parseLong(paymentInfoId));
            }
            String description = "Pin serial: " + pin.getSerial();
//            description += "<br/>" + " Amount: " + amount + customService.getCurrency().getSymbol();
            description += " " + " Amount: " + amount;
            transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_RECHARGE);
            transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.PROCESSED);
            transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, description, account, pin.getSerial(), null, null, transactionData, externalId, null,null, null, null,null,0f,userData.getIpRemoteAddress());

                //FIXME ELIMINAR CABLE DEL ORDEN ID
                rechargePinResponse = new RechargePinResponse(pin.getSerial().toString(), pin.getSecret(), "Transaccion Exitosa", String.valueOf(newBalance), pin.getPinsFree()!=null?pin.getPinsFree().get(0).getId().toString():null);

                rechargePinResponse.setContractId(contract.getId());
                rechargePinResponse.setContractBalance(newBalance);
                rechargePinResponse.setContractBeginningDate(contract.getBeginningDate());
                rechargePinResponse.setContractInvoiceDate(""+contract.getCycle().getEmissionDate());
                rechargePinResponse.setContractStatus(contract.getStatus().getName());
                rechargePinResponse.setAlias(contract.getAlias());
                try {
                    Product product = productEJB.loadProductById(Product.PINLESS_ID);
                    String token = loadTokenOrdenInvoive();
                    WsSalesRecordProxy salesRecordProxy = new WsSalesRecordProxy();
                    WsOrderResponse orderResponse = salesRecordProxy.generateOrder(token, Constants.ORDER_STATUS_PROCESS, null, registerUniId, deliveryAddressId, billingAddressId, paymentInfoId, pin.getWebUser().getId().toString(), transaction.getTotalAmount().toString(),  transaction.getTotalTax().toString(), "0", transaction.getTotalAmount().toString(), "1", pin.getCurrentBalance().toString(), salesChannelId, pin.getCurrency().getId().toString(), ordenSourceId, product.getName(),Product.PINLESS_ID.toString());
                    System.out.println("codeOrder"+orderResponse.getCode());
                    if (orderResponse.getCode().equals(Constants.TRANSACTION_SUCCESSFUL)) {
                        String responseOrderId = orderResponse.getId();
                        rechargePinResponse.setOrderId(responseOrderId);

                        WsInvoiceResponse invoiceResponse = salesRecordProxy.generateInvoice(token, Enterprise.ALODIGA_USA.toString(), Constants.INVOICE_TYPE_PIN_LESS, Constants.INVOICE_STATUS_PROCESS, "1", transaction.getTotalTax().toString(), String.valueOf(amount), "0", "0", pin.getCurrentBalance().toString(), transaction.getTotalAmount().toString(), "0", "0", pin.getCurrency().getId().toString(), responseOrderId, "0", "0", null, null, registerUniId);
                        System.out.println("codeInvoice"+invoiceResponse.getCode());
                        if (invoiceResponse.getCode().equals(Constants.TRANSACTION_SUCCESSFUL)) {
                            rechargePinResponse.setInvoiceId(invoiceResponse.getId());
                        }
                    }
                } catch (Exception ex1) {
                    ex1.printStackTrace();
                }

            } catch (ContractNotFoundException ex) {
                ex.printStackTrace();
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), ex);
            } catch (Exception ex) {
                ex.printStackTrace();
                try {
                    transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.PIN_RECHARGE);
                    transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                    transaction = new Transaction(transactionType, transactionStatus, amount, 0f, 0f, nowTimestamp, ex.getMessage(), account, null, null, null, "Error trying recharging pin. Number = " + phoneNumber, externalId, null, orderId != null ? orderId.toString() : null, null, null, null, 0f, userData.getIpRemoteAddress());
                    //     cancelPinRecharge(userData, null, null,transaction);
                    ex.printStackTrace();
                    throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), ex);
                } catch (Exception ex1) {
                }
            } finally {
                try {
                    this.saveTransaction(transaction);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    throw new GeneralException("Exception trying saving transaction in method rechargePin. " + ex.getMessage());
                }
            }
            return rechargePinResponse;
        }

     public ProcessResponse processNauta(AccountData userData,String productId, String senderFirstName, String senderLastName,String senderEmail,String senderMobile, String recipientFirstName, String recipientLastName,  String recipientEmail,String recipientMobile) throws NullParameterException, InvalidAccountException, DisabledAccountException, TopUpTransactionException, GeneralException, TopUpTransactionException, GeneralException, TopUpProductNotAvailableException, InvalidFormatException, NotAvaliableServiceException, DestinationNotPrepaidException, DuplicatedTransactionException, CarrierSystemUnavailableException, InvalidPhoneNumberException, InvalidAmountException, MaxNumberTransactionDailyPerDestinationException, MaxNumberTransactionDailyPerSenderException, NautaInsufficientBalanceAccountException,NautaInsufficientRetailerAccountException,NautaAccountNumberIncorrectException,NautaTransactionAmountLimitExceededException,NautaTransactionAlreadyPaidException,NautaTransactionRepeatedException,NautaTransactionRejectedException,NautaTransactiontimeoutException,NautaRecipientReachedMaxTransactionNumberException,NautaExternalIdAlreadyUsedAccountException  {
        Account account = validateAccount(userData);
        ProcessResponse presponse  = new ProcessResponse();

        Transaction transaction = null;
        Date now = new Date((new java.util.Date()).getTime());
        StringBuilder transactionData = new StringBuilder();
        Timestamp nowTimestamp = new Timestamp(now.getTime());
        try {
            //Se egrego cuando se incorporó distribución.
            if(!(userData.getIpRemoteAddress().equals("192.168.201.64") || userData.getIpRemoteAddress().equals("192.168.201.59")) ){
                validarDestinationNumberAndSenderNumber(recipientMobile, senderMobile);
                System.out.println("entro..................................... a la validacion");
            }
            //Lena el response
              presponse = IntegrationNauta.executeNauta(productId, senderFirstName, senderLastName, senderEmail, senderMobile, recipientFirstName, recipientLastName, recipientEmail, recipientMobile);

               if(presponse.getStatusCode().equals("0")){
                System.out.println("el codigo de respuesta es 0");
                transactionData.append("Nauta: ").append(presponse.getStatusMessage()).append("( ").append(presponse.getProduct_id()).append(" )").append(" Operadora : ").append("Nauta Etecsa").append(" Denominacion: ").append(presponse.getRetailPrice()).append("PhoneNumber: ").append(recipientMobile);
                transaction = new Transaction(new TransactionType(TransactionType.NAUTA_RECHARGE), new TransactionStatus(TransactionStatus.PROCESSED), Float.valueOf(presponse.getWholesale_price()), 0f, 0f, nowTimestamp, transactionData.toString(), account, null, null, null, transactionData.toString(), presponse.getExternalId(), null, "", recipientMobile, senderMobile,null,0f,userData.getIpRemoteAddress());
                 presponse.setStatusCode(presponse.getStatusCode());
                this.saveTransaction(transaction);

             }else{
                   presponse.setStatusCode(presponse.getStatusCode().substring(presponse.getStatusCode().length()-3, presponse.getStatusCode().length()));
             }


             

             
        } catch (MaxNumberTransactionDailyPerDestinationException ex) {
            System.out.println("MaxNumberTransactionDailyPerDestinationException");
            transaction = new Transaction(new TransactionType(TransactionType.NAUTA_RECHARGE), new TransactionStatus(TransactionStatus.FAILED), Float.valueOf(0f), 0f, 0f, nowTimestamp, transactionData.toString(), account, null, null, null, transactionData.toString(), "0", null, "", recipientMobile, senderMobile,null,0f,userData.getIpRemoteAddress());
            throw ex;
        } catch (MaxNumberTransactionDailyPerSenderException ex) {
            System.out.println("MaxNumberTransactionDailyPerSenderException");
            transaction = new Transaction(new TransactionType(TransactionType.NAUTA_RECHARGE), new TransactionStatus(TransactionStatus.FAILED), Float.valueOf(0f), 0f, 0f, nowTimestamp, transactionData.toString(), account, null, null, null, transactionData.toString(), "0", null, "", recipientMobile, senderMobile,null,0f,userData.getIpRemoteAddress());
            throw ex;
        }  catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("");
            System.out.println("Exception ex"+ex.getMessage());
            transaction = new Transaction(new TransactionType(TransactionType.NAUTA_RECHARGE), new TransactionStatus(TransactionStatus.FAILED), Float.valueOf(0f), 0f, 0f, nowTimestamp, transactionData.toString(), account, null, null, null, transactionData.toString(), "0", null, "", recipientMobile, senderMobile,null,0f,userData.getIpRemoteAddress());
            this.saveTransaction(transaction);
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), ex);
        }


         System.out.println("el codigo es ak127+" + presponse.getStatusCode()+presponse.getStatusMessage());
             if(presponse.getStatusCode().equals("777")){
                 System.out.println("Insufficient balance in your master account");
                 transaction = new Transaction(new TransactionType(TransactionType.NAUTA_RECHARGE), new TransactionStatus(TransactionStatus.FAILED), Float.valueOf(0f), 0f, 0f, nowTimestamp, transactionData.toString(), account, null, null, null, transactionData.toString(), "0", null, "", recipientMobile, senderMobile,null,0f,userData.getIpRemoteAddress());
                 transactionData.append("Nauta: ").append(presponse.getStatusMessage()).append("( ").append(presponse.getProduct_id()).append(" )").append(" Operadora : ").append("Nauta Etecsa").append(" Denominacion: ").append(presponse.getRetailPrice()).append("PhoneNumber: ").append(recipientMobile);
                 this.saveTransaction(transaction);
                 throw new NautaInsufficientBalanceAccountException("");
             }if(presponse.getStatusCode().equals("888")){
                 System.out.println("Insufficient balance in your retailer account");
                 transaction = new Transaction(new TransactionType(TransactionType.NAUTA_RECHARGE), new TransactionStatus(TransactionStatus.FAILED), Float.valueOf(0f), 0f, 0f, nowTimestamp, transactionData.toString(), account, null, null, null, transactionData.toString(), "0", null, "", recipientMobile, senderMobile,null,0f,userData.getIpRemoteAddress());
                 transactionData.append("Nauta: ").append(presponse.getStatusMessage()).append("( ").append(presponse.getProduct_id()).append(" )").append(" Operadora : ").append("Nauta Etecsa").append(" Denominacion: ").append(presponse.getRetailPrice()).append("PhoneNumber: ").append(recipientMobile);
                 throw new NautaInsufficientRetailerAccountException("");
             }if(presponse.getStatusCode().trim().equals("204")){
                 System.out.println("Account number incorrect in");
                 transaction = new Transaction(new TransactionType(TransactionType.NAUTA_RECHARGE), new TransactionStatus(TransactionStatus.FAILED), Float.valueOf(0f), 0f, 0f, nowTimestamp, transactionData.toString(), account, null, null, null, transactionData.toString(), "0", null, "", recipientMobile, senderMobile,null,0f,userData.getIpRemoteAddress());
                 transactionData.append("Nauta: ").append(presponse.getStatusMessage()).append("( ").append(presponse.getProduct_id()).append(" )").append(" Operadora : ").append("Nauta Etecsa").append(" Denominacion: ").append(presponse.getRetailPrice()).append("PhoneNumber: ").append(recipientMobile);
                 System.out.println("termina Account number incorrect in");
                 this.saveTransaction(transaction);
                 throw new NautaAccountNumberIncorrectException("");
             }if(presponse.getStatusCode().equals("207")){
                 System.out.println("Transaction amount limit exceeded");
                 transaction = new Transaction(new TransactionType(TransactionType.NAUTA_RECHARGE), new TransactionStatus(TransactionStatus.FAILED), Float.valueOf(0f), 0f, 0f, nowTimestamp, transactionData.toString(), account, null, null, null, transactionData.toString(), "0", null, "", recipientMobile, senderMobile,null,0f,userData.getIpRemoteAddress());
                 transactionData.append("Nauta: ").append(presponse.getStatusMessage()).append("( ").append(presponse.getProduct_id()).append(" )").append(" Operadora : ").append("Nauta Etecsa").append(" Denominacion: ").append(presponse.getRetailPrice()).append("PhoneNumber: ").append(recipientMobile);
                 this.saveTransaction(transaction);
                 throw new NautaTransactionAmountLimitExceededException("");
             }if(presponse.getStatusCode().equals("212")){
                 System.out.println("Transaction already paid");
                 transaction = new Transaction(new TransactionType(TransactionType.NAUTA_RECHARGE), new TransactionStatus(TransactionStatus.FAILED), Float.valueOf(0f), 0f, 0f, nowTimestamp, transactionData.toString(), account, null, null, null, transactionData.toString(), "0", null, "", recipientMobile, senderMobile,null,0f,userData.getIpRemoteAddress());
                 transactionData.append("Nauta: ").append(presponse.getStatusMessage()).append("( ").append(presponse.getProduct_id()).append(" )").append(" Operadora : ").append("Nauta Etecsa").append(" Denominacion: ").append(presponse.getRetailPrice()).append("PhoneNumber: ").append(recipientMobile);
                 this.saveTransaction(transaction);
                 throw new NautaTransactionAlreadyPaidException("");
             }if(presponse.getStatusCode().equals("213")){
                 System.out.println("Transaction repeated");
                 transaction = new Transaction(new TransactionType(TransactionType.NAUTA_RECHARGE), new TransactionStatus(TransactionStatus.FAILED), Float.valueOf(0f), 0f, 0f, nowTimestamp, transactionData.toString(), account, null, null, null, transactionData.toString(), "0", null, "", recipientMobile, senderMobile,null,0f,userData.getIpRemoteAddress());
                 transactionData.append("Nauta: ").append(presponse.getStatusMessage()).append("( ").append(presponse.getProduct_id()).append(" )").append(" Operadora : ").append("Nauta Etecsa").append(" Denominacion: ").append(presponse.getRetailPrice()).append("PhoneNumber: ").append(recipientMobile);
                 this.saveTransaction(transaction);
                 throw new NautaTransactionRepeatedException("");
             }if(presponse.getStatusCode().equals("214")){
                 System.out.println("Transaction rejected");
                 transaction = new Transaction(new TransactionType(TransactionType.NAUTA_RECHARGE), new TransactionStatus(TransactionStatus.FAILED), Float.valueOf(0f), 0f, 0f, nowTimestamp, transactionData.toString(), account, null, null, null, transactionData.toString(), "0", null, "", recipientMobile, senderMobile,null,0f,userData.getIpRemoteAddress());
                 transactionData.append("Nauta: ").append(presponse.getStatusMessage()).append("( ").append(presponse.getProduct_id()).append(" )").append(" Operadora : ").append("Nauta Etecsa").append(" Denominacion: ").append(presponse.getRetailPrice()).append("PhoneNumber: ").append(recipientMobile);
                 this.saveTransaction(transaction);
                 throw new NautaTransactionRejectedException("");
             }if(presponse.getStatusCode().equals("218")){
                 System.out.println("Transaction timeout");
                 transaction = new Transaction(new TransactionType(TransactionType.NAUTA_RECHARGE), new TransactionStatus(TransactionStatus.FAILED), Float.valueOf(0f), 0f, 0f, nowTimestamp, transactionData.toString(), account, null, null, null, transactionData.toString(), "0", null, "", recipientMobile, senderMobile,null,0f,userData.getIpRemoteAddress());
                 transactionData.append("Nauta: ").append(presponse.getStatusMessage()).append("( ").append(presponse.getProduct_id()).append(" )").append(" Operadora : ").append("Nauta Etecsa").append(" Denominacion: ").append(presponse.getRetailPrice()).append("PhoneNumber: ").append(recipientMobile);
                 this.saveTransaction(transaction);
                 throw new NautaTransactiontimeoutException("");
             }if(presponse.getStatusCode().equals("230")){
                 System.out.println("Recipient reached maximum transaction number");
                 transaction = new Transaction(new TransactionType(TransactionType.NAUTA_RECHARGE), new TransactionStatus(TransactionStatus.FAILED), Float.valueOf(0f), 0f, 0f, nowTimestamp, transactionData.toString(), account, null, null, null, transactionData.toString(), "0", null, "", recipientMobile, senderMobile,null,0f,userData.getIpRemoteAddress());
                 transactionData.append("Nauta: ").append(presponse.getStatusMessage()).append("( ").append(presponse.getProduct_id()).append(" )").append(" Operadora : ").append("Nauta Etecsa").append(" Denominacion: ").append(presponse.getRetailPrice()).append("PhoneNumber: ").append(recipientMobile);
                 this.saveTransaction(transaction);
                 throw new NautaRecipientReachedMaxTransactionNumberException("");
             }if(presponse.getStatusCode().equals("990")){
                 System.out.println("External id already used");
                 transaction = new Transaction(new TransactionType(TransactionType.NAUTA_RECHARGE), new TransactionStatus(TransactionStatus.FAILED), Float.valueOf(0f), 0f, 0f, nowTimestamp, transactionData.toString(), account, null, null, null, transactionData.toString(), "0", null, "", recipientMobile, senderMobile,null,0f,userData.getIpRemoteAddress());
                 transactionData.append("Nauta: ").append(presponse.getStatusMessage()).append("( ").append(presponse.getProduct_id()).append(" )").append(" Operadora : ").append("Nauta Etecsa").append(" Denominacion: ").append(presponse.getRetailPrice()).append("PhoneNumber: ").append(recipientMobile);
                 this.saveTransaction(transaction);
                 throw new NautaExternalIdAlreadyUsedAccountException("");
             }

             else{
                   transaction = new Transaction(new TransactionType(TransactionType.NAUTA_RECHARGE), new TransactionStatus(TransactionStatus.FAILED), Float.valueOf(presponse.getWholesale_price()), 0f, 0f, nowTimestamp, transactionData.toString(), account, null, null, null, transactionData.toString(), presponse.getExternalId(), null, "", recipientMobile, senderMobile,null,0f,userData.getIpRemoteAddress());
             }

        return presponse;
    }


    }

