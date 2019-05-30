package com.alodiga.services.provider.ejb;

import com.alodiga.services.provider.commons.exceptions.CancelPaymentException;
import com.alodiga.services.provider.commons.exceptions.DuplicatedExternalIdException;
import com.alodiga.services.provider.commons.models.AccountHasIpAddress;
import com.alodiga.services.provider.commons.models.TransactionStatus;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.interceptor.Interceptors;
import org.apache.log4j.Logger;
import com.alodiga.services.provider.commons.ejbs.CustomerEJBLocal;
import com.alodiga.services.provider.commons.ejbs.ProductEJBLocal;
import com.alodiga.services.provider.commons.ejbs.ServicesEJBLocal;
import com.alodiga.services.provider.commons.ejbs.TopUpProductEJBLocal;
import com.alodiga.services.provider.commons.ejbs.TransactionEJB;
import com.alodiga.services.provider.commons.ejbs.TransactionEJBLocal;
import com.alodiga.services.provider.commons.ejbs.UserEJBLocal;
import com.alodiga.services.provider.commons.ejbs.UtilsEJBLocal;
import com.alodiga.services.provider.commons.ejbs.WSEJBLocal;
import com.alodiga.services.provider.commons.exceptions.EmptyListException;
import com.alodiga.services.provider.commons.exceptions.GeneralException;
import com.alodiga.services.provider.commons.exceptions.InvalidFormatException;
import com.alodiga.services.provider.commons.exceptions.MaxAmountBalanceException;
import com.alodiga.services.provider.commons.exceptions.MaxAmountDailyException;
import com.alodiga.services.provider.commons.exceptions.MaxAmountPerTransactionException;
import com.alodiga.services.provider.commons.exceptions.MaxPromotionTransactionDailyException;
import com.alodiga.services.provider.commons.exceptions.MinAmountBalanceException;
import com.alodiga.services.provider.commons.exceptions.NegativeBalanceException;
import com.alodiga.services.provider.commons.exceptions.NullParameterException;
import com.alodiga.services.provider.commons.exceptions.PinFreeProvisionException;
import com.alodiga.services.provider.commons.exceptions.PinProvisionException;
import com.alodiga.services.provider.commons.exceptions.RegisterNotFoundException;
import com.alodiga.services.provider.commons.exceptions.TransactionCanceledException;
import com.alodiga.services.provider.commons.exceptions.InvalidPaymentInfoException;
import com.alodiga.services.provider.commons.exceptions.PaymentDeclinedException;
import com.alodiga.services.provider.commons.exceptions.PaymentServiceUnavailableException;
import com.alodiga.services.provider.commons.exceptions.DisabledPinException;
//import com.alodiga.services.provider.commons.exceptions.MaxLimitCreditCardException;
//import com.alodiga.services.provider.commons.exceptions.MaxLimitPurchaseException;
//import com.alodiga.services.provider.commons.exceptions.MaxLimitRechageException;
import com.alodiga.services.provider.commons.exceptions.PurchaseCanceledException;
import com.alodiga.services.provider.commons.exceptions.TransactionNotAvailableException;
import com.alodiga.services.provider.commons.genericEJB.AbstractSPEJB;
import com.alodiga.services.provider.commons.genericEJB.AbstractSPEntity;
import com.alodiga.services.provider.commons.genericEJB.SPContextInterceptor;
import com.alodiga.services.provider.commons.genericEJB.SPLoggerInterceptor;
import com.alodiga.services.provider.commons.genericEJB.EJBRequest;
import com.alodiga.services.provider.commons.managers.ContentManager;
import com.alodiga.services.provider.commons.managers.PreferenceManager;
import com.alodiga.services.provider.commons.models.Account;
import com.alodiga.services.provider.commons.models.AccountBalance;
import com.alodiga.services.provider.commons.models.AccountLimitTopUp;
import com.alodiga.services.provider.commons.models.AuditSms;
import com.alodiga.services.provider.commons.models.AccountProduct;
import com.alodiga.services.provider.commons.models.BalanceHistory;
import com.alodiga.services.provider.commons.models.Customer;
import com.alodiga.services.provider.commons.models.Enterprise;
import com.alodiga.services.provider.commons.models.CreditcardType;
import com.alodiga.services.provider.commons.models.Currency;
import com.alodiga.services.provider.commons.models.Invoice;
import com.alodiga.services.provider.commons.models.InvoiceStatus;
import com.alodiga.services.provider.commons.models.Payment;
import com.alodiga.services.provider.commons.models.PaymentInfo;
import com.alodiga.services.provider.commons.models.PaymentIntegrationType;
import com.alodiga.services.provider.commons.models.PaymentType;
import com.alodiga.services.provider.commons.models.Transaction;
import com.alodiga.services.provider.commons.models.TransactionType;
import com.alodiga.services.provider.commons.utils.EjbConstants;
import com.alodiga.services.provider.commons.models.Pin;
import com.alodiga.services.provider.commons.models.PreferenceFieldEnum;
import com.alodiga.services.provider.commons.models.Product;
import com.alodiga.services.provider.commons.models.Recharge;
import com.alodiga.services.provider.commons.models.RechargeStatus;
import com.alodiga.services.provider.commons.models.RechargeType;
import com.alodiga.services.provider.commons.models.TopUpProduct;
import com.alodiga.services.provider.commons.models.TransactionSource;
import com.alodiga.services.provider.commons.services.models.ValidatePinFreeByAniResponse;
import com.alodiga.services.provider.commons.services.models.WSConstants;
import com.alodiga.services.provider.commons.utils.AccountData;
import com.alodiga.services.provider.commons.utils.CommonMails;
import com.alodiga.services.provider.commons.utils.Constants;
import com.alodiga.services.provider.commons.utils.EjbUtils;
import com.alodiga.services.provider.commons.utils.GeneralUtils;
import com.alodiga.services.provider.commons.utils.Mail;
import com.alodiga.services.provider.commons.utils.QueryConstants;
import com.alodiga.services.provider.commons.utils.QueryParam;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.Calendar;
import java.util.HashMap;
import java.util.logging.Level;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

@Interceptors({SPLoggerInterceptor.class, SPContextInterceptor.class})
@Stateless(name = EjbConstants.TRANSACTION_EJB, mappedName = EjbConstants.TRANSACTION_EJB)
@TransactionManagement(TransactionManagementType.BEAN)
public class TransactionEJBImp extends AbstractSPEJB implements TransactionEJB, TransactionEJBLocal {

    private static final Logger logger = Logger.getLogger(TransactionEJBImp.class);
    @EJB
    private UtilsEJBLocal utilsEJB;
    @EJB
    private UserEJBLocal userEJB;
    @EJB
    private WSEJBLocal wsEJB;
    @EJB
    private ProductEJBLocal productEJB;
    @EJB
    private CustomerEJBLocal customerEJB;
    @EJB
    private TopUpProductEJBLocal topUpProductEJB;
    @EJB
    private ServicesEJBLocal servicesEJB;
//    private TopUpEJB topUpEJB;//MLAT
//    private PreferenceEJB preferenceEJB;//MLAT
    private Enterprise enterprise;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static int MAX_LEVELS_ROYALTY = 3;//NUMERO MAXIMO AL CUAL SE APLICAN LAS REGALIAS

    public List<CreditcardType> getCreditcardTypes(EJBRequest request) throws GeneralException, RegisterNotFoundException, NullParameterException, EmptyListException {
        //System.out.println("---------------getCreditcardTypes-----------");
        return (List<CreditcardType>) listEntities(CreditcardType.class, request, logger, getMethodName());
    }

    public Recharge loadRechargeById(Long rechargeId) throws GeneralException, RegisterNotFoundException, NullParameterException {
        Recharge recharge = new Recharge();
        try {
            if (rechargeId == null) {
                throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "referenceCode"), null);
            }
            Query query = createQuery("SELECT r FROM Recharge r WHERE r.id = ?1");
            query.setParameter("1", rechargeId);
            recharge = (Recharge) query.setHint("toplink.refresh", "true").getSingleResult();
        } catch (NoResultException ex) {
            System.out.println("RechargeId: " + rechargeId);
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, Recharge.class.getSimpleName(), "loadRechargeById", Recharge.class.getSimpleName(), null), ex);
        } catch (Exception ex) {
            System.out.println("RechargeId: " + rechargeId);
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        return recharge;
    }

    public List<PaymentType> getPaymentTypes(EJBRequest request) throws GeneralException, RegisterNotFoundException, NullParameterException, EmptyListException {
        //System.out.println("---------------getPaymentTypes-----------");
        return (List<PaymentType>) listEntities(PaymentType.class, request, logger, getMethodName());
    }

    public List<Transaction> getTransactionByCondition(EJBRequest request) throws NullParameterException, EmptyListException, GeneralException {
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

    public List<TransactionType> getTransactionTypes() throws GeneralException, NullParameterException, EmptyListException {
        List<TransactionType> transactionTypes = new ArrayList<TransactionType>();
        StringBuilder sqlBuilder = new StringBuilder("SELECT t FROM TransactionType t");
        Query query = null;
        try {
            query = createQuery(sqlBuilder.toString());
            transactionTypes = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return transactionTypes;
    }

    @Override
    public List<TransactionType> getTransactionTypes(EJBRequest request) throws GeneralException, NullParameterException, EmptyListException {
        List<TransactionType> transactionTypes = (List<TransactionType>) listEntities(TransactionType.class, request, logger, getMethodName());

        return transactionTypes;
    }

    public List<TransactionType> getTransactionTypesByDistributor(EJBRequest request) throws GeneralException, NullParameterException, EmptyListException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public CreditcardType loadCreditcardType(EJBRequest request) throws GeneralException, RegisterNotFoundException, NullParameterException {
        //System.out.println("---------------loadCreditcardType-----------");
        return (CreditcardType) loadEntity(CreditcardType.class, request, logger, getMethodName());
    }

    public PaymentInfo loadPaymentInfo(EJBRequest request) throws GeneralException, RegisterNotFoundException, NullParameterException {
        //System.out.println("---------------loadPaymentInfo-----------");
        return (PaymentInfo) loadEntity(PaymentInfo.class, request, logger, getMethodName());
    }

    public PaymentIntegrationType loadPaymentIntegrationType(EJBRequest request) throws GeneralException, RegisterNotFoundException, NullParameterException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Transaction loadTransaction(EJBRequest request) throws GeneralException, RegisterNotFoundException, NullParameterException {
        return (Transaction) loadEntity(Transaction.class, request, logger, getMethodName());
    }

    public Map<String, Object> purchaseBalance(Transaction transaction) throws GeneralException, NullParameterException, TransactionCanceledException, MaxAmountBalanceException, MinAmountBalanceException, TransactionNotAvailableException, MaxAmountPerTransactionException, MaxAmountDailyException, CancelPaymentException, PinFreeProvisionException, PinProvisionException, MaxPromotionTransactionDailyException, InvalidFormatException, PaymentDeclinedException, InvalidPaymentInfoException {
        Map<String, Object> response = new HashMap<String, Object>();
        Account account = transaction.getAccount();
        try {
            if (transaction == null) {
                throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "transaction"), null);
            }
            EJBRequest request = new EJBRequest();
            request.setParam(transaction);
            validateTransaction(request);
            // Se verifica el payment info de existir
            if (transaction.getPaymentInfo() != null && transaction.getPaymentInfo().getId() == null) {
                EJBRequest requestPaymentInfo = new EJBRequest();
                requestPaymentInfo.setParam(transaction.getPaymentInfo());
                transaction.setPaymentInfo(savePaymentInfo(requestPaymentInfo));
            }

            boolean operationSuccess = false;

            if (transaction.getTransactionType().getId().equals(TransactionType.PURCHASE_BALANCE)) {

                Recharge recharge = null;
                try {
                    AccountData accountData = new AccountData();
                    accountData.setLogin(account.getLogin());
                    accountData.setPassword(account.getPassword());
                    List<AccountHasIpAddress> accounts = account.getAccountHasIpAddress();
                    accountData.setIpRemoteAddress(accounts.get(0)!=null ? accounts.get(0).getIpAddress().getIp() : Constants.IP_ADDRESS_SP_EJB);
                    String externalId = account.getId().toString() + Calendar.getInstance().getTimeInMillis();
                    recharge = servicesEJB.processBanking(accountData, transaction.getPaymentInfo(), transaction.getTotalAmount(), externalId);
                    transaction.setRecharge(recharge);
                    operationSuccess = true;
                } catch (PaymentDeclinedException ex) {
                    ex.printStackTrace();
                    sendPurchaseBalanceErrorMail(account, transaction, "Paso 2: Procesando el pago con Authorize.net. (PAGO DECLINADO)", ex);
                    throw (ex);
                } catch (InvalidPaymentInfoException ex) {
                    ex.printStackTrace();
                    sendPurchaseBalanceErrorMail(account, transaction, "Paso 2: Procesando el pago con Authorize.net. (METODO DE PAGO INVALIDO)", ex);
                    throw (ex);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    sendPurchaseBalanceErrorMail(account, transaction, "Paso 2: Procesando el pago con Authorize.net. (ERROR GENERAL)", ex);
                    throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ""), ex);
                }


//                try {
////                    List<BalanceHistory> histories = new ArrayList<BalanceHistory>();
////
////                    BalanceHistory balanceHistory = new BalanceHistory();
////                    balanceHistory.setAccount(account);
////                    balanceHistory.setDate(transaction.getCreationDate());
////                    balanceHistory.setTransaction(transaction);
////                    Float currentAmount = 0F;
////                    try {
////                        currentAmount = ((BalanceHistory) loadLastBalanceHistoryByAccount(account.getId())).getCurrentAmount();
////                    } catch (Exception ex) {
////                        ex.printStackTrace();
////                    }
////                    balanceHistory.setOldAmount(currentAmount);
////                    balanceHistory.setCurrentAmount(currentAmount + transaction.getTotalAmount());
////                    histories.add(balanceHistory);
////
////                    transaction.setBalanceHistories(histories);

                        transaction = loadTransactionByRechargeId(recharge.getId());
                    request = new EJBRequest();
                    request.setParam(transaction);

                    response.put(QueryConstants.PARAM_TRANSACTION, transaction);
////
////                    account.setBalance(balanceHistory.getCurrentAmount());
////                    request = new EJBRequest();
////                    request.setParam(account);
////                    userEJB.saveAccount(request);
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                    sendPurchaseBalanceErrorMail(account, transaction, "Paso 4: Almacenando el BalanceHistory ", ex);
//                }


                try {
                    sendPurchaseBalanceSuccesfulMail(transaction);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } catch (PaymentDeclinedException ex) {
            throw (ex);
        } catch (InvalidPaymentInfoException ex) {
            throw (ex);
        } catch (TransactionNotAvailableException e) {
            throw new TransactionNotAvailableException(logger, sysError.format(EjbConstants.ERR_TRANSACTION_NOT_AVAILABLE, this.getClass(), getMethodName(), "param"), e);
        } catch (MaxAmountPerTransactionException e) {
            throw e;
        } catch (MaxAmountDailyException e) {
            throw e;
        } catch (MaxPromotionTransactionDailyException m) {
            throw new MaxPromotionTransactionDailyException(logger, sysError.format(EjbConstants.ERR_MAX_PROMOTION_TRANSACTION_DAILY, this.getClass(), getMethodName(), "param"), m);
        } catch (Exception e) {
            throw new GeneralException(e.getMessage());
        }
        return response;
    }

    public BalanceHistory loadLastBalanceHistoryByAccount(Long accountId) throws GeneralException, RegisterNotFoundException, NullParameterException {
        //System.out.println("---------------loadLastBalanceHistoryByDistributor-----------");
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

    public Map<String, Object> purchasePins(EJBRequest request) throws GeneralException, NullParameterException, TransactionCanceledException, MaxAmountBalanceException, MinAmountBalanceException, TransactionNotAvailableException, MaxAmountPerTransactionException, MaxAmountDailyException, CancelPaymentException, PinFreeProvisionException, PinProvisionException, MaxPromotionTransactionDailyException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

     private BalanceHistory createBalanceHistory(Account account, float transferAmount, int transferType, boolean isBalanceTranference) throws GeneralException, EmptyListException, NullParameterException, NegativeBalanceException, MaxAmountBalanceException, MinAmountBalanceException, RegisterNotFoundException {

        BalanceHistory currentBalanceHistory = loadLastBalanceHistoryByAccount(account.getId());
        float currentAmount = currentBalanceHistory != null ? currentBalanceHistory.getCurrentAmount().floatValue() : 0f;
        BalanceHistory balanceHistory = new BalanceHistory();
        balanceHistory.setAccount(account);
        balanceHistory.setDate(new Timestamp(new Date().getTime()));
        balanceHistory.setOldAmount(currentAmount);
        EJBRequest request = new EJBRequest();
        float newCurrentAmount = 0.0f;
        switch (transferType) {
            case 1:
                newCurrentAmount = currentAmount - transferAmount;
                break;
            case 2:
                newCurrentAmount = currentAmount + transferAmount;//SUMO AL MONTO ACTUAL (EL DESTINO)
                break;
        }
        if (newCurrentAmount < 0) {
            throw new NegativeBalanceException("Current amount can not be negative");
        }
        balanceHistory.setCurrentAmount(newCurrentAmount);
        return balanceHistory;
    }

    public boolean validateBalance(BalanceHistory currentBalanceHistory, float amount, int transferType, boolean isBalanceTransference) throws MaxAmountBalanceException, GeneralException, EmptyListException, NullParameterException, MinAmountBalanceException {
        Map orderMap = new HashMap();
        orderMap.put("id", "desc");
        EJBRequest request = new EJBRequest();
        Map acSrcParams = new HashMap();
        acSrcParams.put("accountId", currentBalanceHistory.getAccount().getId());
        request.setParams(acSrcParams);
        List<AccountBalance> listAccountBalance = null;
        listAccountBalance = (List<AccountBalance>) createSearchQuery(AccountBalance.class, request, orderMap, logger, getMethodName(), "AccountBalance", true);
        AccountBalance accountBalance = listAccountBalance.get(0);

        if (transferType == 1) {
            if ((currentBalanceHistory.getCurrentAmount() - amount) < 0) {
                throw new MinAmountBalanceException(logger, sysError.format(EjbConstants.ERR_MIN_AMOUNT_BALANCE, this.getClass(), getMethodName(), "param"), null);
            }
        } else {
            if ((!isBalanceTransference && (currentBalanceHistory.getCurrentAmount() + amount) > accountBalance.getBalance().floatValue())) {
                throw new MaxAmountBalanceException(logger, sysError.format(EjbConstants.ERR_MAX_AMOUNT_BALANCE, this.getClass(), getMethodName(), "param"), null);
            }
        }
        return true;
    }

    private void sendSuccessBalancePurchase(Account account, Transaction transaction) {
        try {
            Mail mail = CommonMails.getSuccessfulBalancePurchaseMail(account, transaction);
            //utilsEJB.sendMail(mail);
            (new com.alodiga.services.provider.commons.utils.MailSender(mail)).start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void sendBalancePurchaseError(Account account, Transaction transaction, String step, Exception ex) {
        try {
            Mail mail = CommonMails.getPurchaseBalanceProccessError(account, transaction, step, ex);
            //utilsEJB.sendMail(mail);
            (new com.alodiga.services.provider.commons.utils.MailSender(mail)).start();
        } catch (Exception ex1) {
            ex1.printStackTrace();
        }
    }

    public Transaction saveTransaction(EJBRequest request) throws GeneralException, NullParameterException, EmptyListException, TransactionNotAvailableException, MaxAmountPerTransactionException, MaxAmountDailyException, MaxPromotionTransactionDailyException {
        if (validateTransaction(request)) {
            //Transaction transaction = executeAlopoints((Transaction) request.getParam());
            return (Transaction) saveEntity(request, logger, getMethodName());
        } else {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ""), null);
        }
    }

    public List<Transaction> searchTransaction(EJBRequest request) throws GeneralException, NullParameterException, EmptyListException {
        List<Transaction> transactions = new ArrayList<Transaction>();
        Map<String, Object> params = request.getParams();

        StringBuilder sqlBuilder = new StringBuilder("SELECT t FROM Transaction t WHERE t.creationDate BETWEEN ?1 AND ?2");
        if (!params.containsKey(QueryConstants.PARAM_BEGINNING_DATE) || !params.containsKey(QueryConstants.PARAM_ENDING_DATE)) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "beginningDate & endingDate"), null);
        }
        if (params.containsKey(QueryConstants.PARAM_CUSTOMER_ID)) {
            sqlBuilder.append(" AND t.customer.id=").append(params.get(QueryConstants.PARAM_CUSTOMER_ID));
        }
        if (params.containsKey(QueryConstants.PARAM_ACCOUNT_ID)) {
            sqlBuilder.append(" AND t.account.id=").append(params.get(QueryConstants.PARAM_ACCOUNT_ID));
        }
        if (params.containsKey(QueryConstants.PARAM_TRANSACTION_TYPE_ID)) {
            sqlBuilder.append(" AND t.transactionType.id=").append(params.get(QueryConstants.PARAM_TRANSACTION_TYPE_ID));
        }
        if (params.containsKey(QueryConstants.PARAM_STATUS)) {
            sqlBuilder.append(" AND t.transactionStatus.id=").append(params.get(QueryConstants.PARAM_STATUS));
        }
        Query query = null;
        try {
            System.out.println("query:********"+sqlBuilder.toString());
            query = createQuery(sqlBuilder.toString());
            query.setParameter("1", EjbUtils.getBeginningDate((Date) params.get(QueryConstants.PARAM_BEGINNING_DATE)));
            query.setParameter("2", EjbUtils.getEndingDate((Date) params.get(QueryConstants.PARAM_ENDING_DATE)));
            if (request.getLimit() != null && request.getLimit() > 0) {
                query.setMaxResults(request.getLimit());
            }
            transactions = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }
        if (transactions.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return transactions;
    }

    public List<Transaction> searchLastTransaction(EJBRequest request) throws GeneralException, NullParameterException, EmptyListException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ValidatePinFreeByAniResponse validatePinFreeByAni(Long languageId, String ani) throws GeneralException, NullParameterException, DisabledPinException {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    public boolean validateTransaction(EJBRequest request) throws GeneralException, NullParameterException, EmptyListException, TransactionNotAvailableException, MaxAmountPerTransactionException, MaxAmountDailyException, MaxPromotionTransactionDailyException {
        boolean transactionAproved = true;
        if (request.getParam() == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), QueryConstants.PARAM_TRANSACTION), null);
        }
        boolean isPromotional = false;

        Transaction transaction = (Transaction) request.getParam();
        EJBRequest validateRequest = new EJBRequest();

        validateRequest.setAuditData(request.getAuditData());
        validateRequest.setFirst(request.getFirst());
        validateRequest.setLimit(request.getLimit());
        validateRequest.setMediaType(request.getMediaType());
        validateRequest.setMethod(request.getMethod());
        validateRequest.setUrl(request.getUrl());

        String clientTypeParam;
        Long clientId;
        Long enterpriseId;
//        if (t.getAccount() == null) {
//            clientTypeParam = QueryConstants.PARAM_CUSTOMER_ID;
//            clientId = t.getCustomer().getId();
//            enterpriseId = t.getCustomer().getEnterprise().getId();
//        } else {
        clientTypeParam = QueryConstants.PARAM_ACCOUNT_ID;
        clientId = transaction.getAccount().getId();
        enterpriseId = transaction.getAccount().getEnterprise().getId();
//        }
        EJBRequest prefRequest = new EJBRequest();
        prefRequest.setParams(new HashMap<String, Object>());
        prefRequest.getParams().put(QueryConstants.PARAM_ENTERPRISE_ID, enterpriseId);

        Map<Long, String> preferences = new HashMap<Long, String>();
        PreferenceManager pManager = null;
        try {
            pManager = PreferenceManager.getInstance();
            preferences = pManager.getPreferences();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        Float maxAmountPerTransaction = Float.parseFloat(pManager.getPreferencesValueByEnterpriseAndPreferenceId(enterpriseId, PreferenceFieldEnum.MAX_TRANSACTION_AMOUNT_LIMIT.getId()));
        Float maxAmountDaily = Float.parseFloat(pManager.getPreferencesValueByEnterpriseAndPreferenceId(enterpriseId, PreferenceFieldEnum.MAX_TRANSACTION_AMOUNT_DAILY_LIMIT.getId()));
        //Float maxAmountPerTransaction = Float.parseFloat(preferences.get(PreferenceFieldEnum.MAX_TRANSACTION_AMOUNT_LIMIT.getId()));
        //  Float maxAmountDaily = Float.parseFloat(preferences.get(PreferenceFieldEnum.MAX_TRANSACTION_AMOUNT_DAILY_LIMIT.getId()));

        //  int maxpromotionTransaction = 0;
//        try {
//            maxpromotionTransaction = Integer.parseInt(preferences.get(PreferenceFieldEnum.MAX_PROMOTION_TRANSACTION_DAILY_LIMIT.getId()));
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }

        boolean transactionAvailable = pManager.getPreferencesValueByEnterpriseAndPreferenceId(enterpriseId, PreferenceFieldEnum.DISABLED_TRANSACTION.getId()).equals("1");
        if (!transactionAvailable) {
            throw new TransactionNotAvailableException(logger, sysError.format(EjbConstants.ERR_TRANSACTION_NOT_AVAILABLE, this.getClass(), getMethodName()), null);
        }
        Float transactionAmount = transaction.getTotalAmount();
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
//        validateRequest.setParams(params);
//        validateRequest.setParam(true);
//        List<Transaction> transactions;
//        int promotionalTransactions = 0;
        float amountDaily = 0f;
        amountDaily = getCurrentTransactionAmountByAccount(clientId, 0);
        System.out.println("AmountDaily" + amountDaily);
        System.out.println("TransactionAmount" + transactionAmount);
        System.out.println("MaxAmountDaily" + maxAmountDaily);
        if ((amountDaily + transactionAmount) > maxAmountDaily) {
            throw new MaxAmountDailyException(logger, sysError.format(EjbConstants.ERR_MAX_AMOUNT_DAILY, this.getClass(), getMethodName(), maxAmountDaily + ""), null);
       } /*else if (isPromotional && promotionalTransactions >= maxpromotionTransaction) {
        throw new MaxPromotionTransactionDailyException(logger, sysError.format(EjbConstants.ERR_MAX_PROMOTION_TRANSACTION_DAILY, this.getClass(), getMethodName(), promotionalTransactions + ""), null);

        }*/
        //System.out.println("END ------------------validateTransaction " + sdf.format(new Date()));
        return transactionAproved;
    }

    public ValidatePinFreeByAniResponse validateSisacPin(String ani, String serial) throws GeneralException, NullParameterException, DisabledPinException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Recharge processPayment(PaymentInfo paymentInfo, Customer customer, float amount) throws NullParameterException, InvalidFormatException, GeneralException, PaymentServiceUnavailableException, PaymentDeclinedException, InvalidPaymentInfoException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<RechargeType> getRechargeTypes() throws GeneralException, EmptyListException {
        List<RechargeType> rechargeTypes = new ArrayList<RechargeType>();
        try {
            Query query = createQuery("SELECT r FROM RechargeType r");
            rechargeTypes = (List<RechargeType>) query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }
        if (rechargeTypes.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return rechargeTypes;
    }

    public List<RechargeStatus> getRechargeStatus() throws GeneralException, EmptyListException {
        List<RechargeStatus> rechargeStatus = new ArrayList<RechargeStatus>();
        try {
            Query query = createQuery("SELECT r FROM RechargeStatus r");
            rechargeStatus = (List<RechargeStatus>) query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }
        if (rechargeStatus.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return rechargeStatus;
    }


    public Recharge saveRecharge(Recharge recharge) throws GeneralException, NullParameterException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Pin associatePinFree(Pin pin, String ani) throws GeneralException, NullParameterException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<TransactionStatus> getTransactionStatus() throws GeneralException, NullParameterException, EmptyListException {
        List<TransactionStatus> transactionStatuses = new ArrayList<TransactionStatus>();
        try {
            Query query = createQuery("SELECT t FROM TransactionStatus t");
            transactionStatuses = (List<TransactionStatus>) query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }
        if (transactionStatuses.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return transactionStatuses;
    }

    public List<InvoiceStatus> getInvoiceStatus() throws GeneralException, NullParameterException, EmptyListException {
        List<InvoiceStatus> invoiceStatus = new ArrayList<InvoiceStatus>();
        try {
            Query query = createQuery("SELECT t FROM InvoiceStatus t");
            invoiceStatus = (List<InvoiceStatus>) query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }
        if (invoiceStatus.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return invoiceStatus;
    }

    public Invoice getInvoiceById(Long invoiceId) throws EmptyListException, GeneralException, NullParameterException, RegisterNotFoundException {
       Invoice invoice  = null;
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

    public List<Transaction> getTransactionByExternalId(String externalId) throws NullParameterException, EmptyListException, GeneralException {
        List<Transaction> transactions = new ArrayList<Transaction>();
        if (externalId == null) {
            throw new NullParameterException("Parameter externalId cannot be null in method getTransactionByExternalId");
        }
        try {
            Query query = createQuery("SELECT t FROM Transaction t WHERE t.externalID = ?1 ORDER BY t.id DESC");
            query.setParameter("1", externalId);
            transactions = query.setHint("toplink.refresh", "true").getResultList();

        } catch (Exception ex) {
            throw new GeneralException(ex.getMessage());
        }
        if (transactions.isEmpty()) {
            throw new EmptyListException("No transaction found in method getTransactionByExternalId.");
        }
        return transactions;
    }

    public boolean checkDuplicatedExternalId(String externalId) throws GeneralException, NullParameterException, DuplicatedExternalIdException {
        List<Transaction> list = new ArrayList<Transaction>();
        try {
            if (externalId == null) {
                throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "externalId"), null);
            }
            Query query = createQuery("SELECT t FROM Transaction t WHERE t.externalID = ?1");
            query.setParameter("1", externalId);
            list = (List<Transaction>) query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        if (!list.isEmpty()) {
            throw new DuplicatedExternalIdException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return true;
    }

    private void sendPurchaseBalanceSuccesfulMail(Transaction transaction) {
        System.out.println("-------------- sendPurchaseBalanceSuccesfulMail -------------- ");
        try {
            Mail mail = CommonMails.getBalancePurchaseSuccessful(transaction.getAccount(), transaction);
            utilsEJB.sendMail(mail);
        } catch (Exception ex1) {
            ex1.printStackTrace();
            java.util.logging.Logger.getLogger(TransactionEJBImp.class.getName()).log(Level.SEVERE, null, ex1);
        }

    }

    public BalanceHistory saveBalanceHistory(EJBRequest request) throws GeneralException, NullParameterException {
        return (BalanceHistory) saveEntity(request, logger, getMethodName());
    }

    public Payment savePayment(EJBRequest request) throws GeneralException, NullParameterException {
        return (Payment) saveEntity(request, logger, getMethodName());
    }

    private void sendPurchaseBalanceErrorMail(Account account, Transaction transaction, String step, Exception ex) {
        System.out.println("-------------- sendPurchaseBalanceErrorMail -------------- " + step);
        try {

            Mail mail = CommonMails.getPurchaseBalanceProccessError(account, transaction, step, ex);
            utilsEJB.sendMail(mail);

        } catch (Exception ex1) {
            ex1.printStackTrace();
            java.util.logging.Logger.getLogger(TransactionEJBImp.class.getName()).log(Level.SEVERE, null, ex1);
        }
    }


    public List<Float> getEntireSalesAmountByAccount(Long accountId) throws NullParameterException, GeneralException {
        List<Float> sales = new ArrayList<Float>();
        sales.add(getCurrentTransactionAmountByAccount(accountId, 0));
        sales.add(getCurrentTransactionAmountByAccount(accountId, 1));
        sales.add(getCurrentTransactionAmountByAccount(accountId, 7));
        sales.add(getCurrentTransactionAmountByAccount(accountId, 15));
        return sales;
    }

    public Float getCurrentTransactionAmountByAccount(Long accountId, int previousDays) throws NullParameterException, GeneralException {
        if (accountId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "accountId"), null);
        }
        Float transactionAmount = null;
        Double transactionResult = null;
        try {
            EJBRequest request = new EJBRequest();
            request.setParam(accountId);
            Account account = userEJB.loadAccount(request);
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

            StringBuilder sqlBuilder = new StringBuilder("SELECT SUM(t.totalAmount) FROM transaction t WHERE t.creationDate BETWEEN ?1 AND ?2 AND t.accountId = ?3");
            sqlBuilder.append(" AND t.transactionTypeId IN (").append(TransactionType.TOP_UP_PURCHASE).append(",");
            sqlBuilder.append(TransactionType.PIN_PURCHASE).append(",").append(TransactionType.PIN_RECHARGE).append(") ");
            sqlBuilder.append(" AND t.transactionStatusId=").append("'").append(TransactionStatus.PROCESSED).append("'");
            Query query = entityManager.createNativeQuery(sqlBuilder.toString());
            query.setParameter("1", new Date(todaysMidnite.getTimeInMillis()));
            query.setParameter("2", new Date(tomorrowsMidnite.getTimeInMillis()));
            query.setParameter("3", accountId);

//            transactionResult = (Double) query.getSingleResult();
              List result = (List) query.getSingleResult();
              transactionResult = result.get(0) != null ? (Double) result.get(0) : 0f;
            transactionAmount = transactionResult != null ? transactionResult.floatValue() : 0f;

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        return transactionAmount;
    }

    public PaymentInfo savePaymentInfo(EJBRequest request) throws GeneralException, NullParameterException {
        return (PaymentInfo) saveEntity(request, logger, getMethodName());
    }

    public TransactionSource loadTransactionSource(EJBRequest request) throws GeneralException, RegisterNotFoundException, NullParameterException {
        //System.out.println("---------------loadTransactionSource-----------");
        return (TransactionSource) loadEntity(TransactionSource.class, request, logger, getMethodName());
    }

    public List<PaymentInfo> getPaymentInfoByAccountId(Long accountId, Boolean enabled) throws GeneralException, NullParameterException, EmptyListException {

        if (accountId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "accountId"), null);
        }
        List<PaymentInfo> payments = new ArrayList<PaymentInfo>();

        StringBuilder sqlBuilder = new StringBuilder("SELECT p FROM PaymentInfo p WHERE p.account.id= ?1");
        if (enabled != null) {
            sqlBuilder.append(" AND p.endingDate IS ").append(enabled ? "NULL" : "NOT NULL");
        }
        try {
            Query query = createQuery(sqlBuilder.toString());
            query.setParameter("1", accountId);

            payments = query.getResultList();
            if (payments.isEmpty()) {
                throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return payments;

    }

    public Float getCurrentBalanceByAccount(Long accountId) throws NullParameterException, GeneralException {

        //System.out.println("---------------getCurrentBalanceByAccount-----------");
        if (accountId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "accountId"), null);
        }
        Float currentBalance = 0F;

        try {
            StringBuilder sqlDateBuilder = new StringBuilder("SELECT MAX(b.date) FROM BalanceHistory b WHERE b.account.id = ?1");
            Query queryDate = entityManager.createQuery(sqlDateBuilder.toString());
            queryDate.setParameter("1", accountId);
            Timestamp maxDate = (Timestamp) queryDate.getSingleResult();

            StringBuilder sqlBuilder = new StringBuilder("SELECT b.currentAmount FROM BalanceHistory b WHERE b.date = ?1 AND b.account.id = ?2");
            Query query = entityManager.createQuery(sqlBuilder.toString());
            query.setParameter("1", maxDate);
            query.setParameter("2", accountId);
            List result = (List) query.setHint("toplink.refresh", "true").getResultList();

            currentBalance = result != null && result.size() > 0 ? ((Float) result.get(0)) : 0f;
        } catch (NoResultException ex) {
            //
        } catch (Exception e) {
            e.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), "BalanceHistory"), null);
        }
        return currentBalance;

    }

    public Float getMaxBalanceByAccount(Long accountId) throws NullParameterException, GeneralException {
        //System.out.println("---------------getMaxBalanceByAccount-----------");
        if (accountId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "accountId"), null);
        }
        Float currentBalance = 0F;
        try {
            Query query = entityManager.createQuery("SELECT ab FROM AccountBalance ab WHERE ab.endingDate IS NULL AND ab.account.id = " + accountId);
            currentBalance = ((AccountBalance) query.getSingleResult()).getBalance();
        } catch (NoResultException ex) {
            //
        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), "BalanceHistory"), null);
        }
        return currentBalance;
    }

     public Float getlimitTopUpByAccount(Long accountId) throws NullParameterException, GeneralException {
        //System.out.println("---------------getMaxBalanceByAccount-----------");
        if (accountId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "accountId"), null);
        }
        Float currenttLimitTopUp = 0F;
        try {
            Query query = entityManager.createQuery("SELECT al FROM AccountLimitTopUp al WHERE al.endingDate IS NULL AND al.account.id = " + accountId);
            currenttLimitTopUp = ((AccountLimitTopUp) query.getSingleResult()).getCountTopUp();
        } catch (NoResultException ex) {
            //
        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), "getlimitTopUpByAccount"), null);
        }
        return currenttLimitTopUp;
    }

    public TransactionType loadTransactionTypebyId(Long id) throws NullParameterException, RegisterNotFoundException, GeneralException {
        if (id == null) {
            throw new NullParameterException(" parameter id cannot be null in loadTransactionTypebyId.");
        }
        TransactionType transactionType = null;
        try {
            Query query = createQuery("SELECT tt FROM TransactionType tt WHERE tt.id = ?1");
            query.setParameter("1", id);
            transactionType = (TransactionType) query.setHint("toplink.refresh", "true").getSingleResult();
        } catch (NoResultException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, Recharge.class.getSimpleName(), "loadTransactionTypebyId", Recharge.class.getSimpleName(), null), ex);
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        return transactionType;
    }

    public TransactionStatus loadTransactionStatusbyId(Long id) throws NullParameterException, RegisterNotFoundException, GeneralException {
        if (id == null) {
            throw new NullParameterException(" parameter id cannot be null in loadTransactionStatusbyId.");
        }
        TransactionStatus transactionStatus = null;
        try {
            Query query = createQuery("SELECT ts FROM TransactionStatus ts WHERE ts.id = ?1");
            query.setParameter("1", id);
            transactionStatus = (TransactionStatus) query.setHint("toplink.refresh", "true").getSingleResult();
        } catch (NoResultException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, Recharge.class.getSimpleName(), "loadTransactionStatusbyId", Recharge.class.getSimpleName(), null), ex);
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        return transactionStatus;
    }

    public Transaction loadTransactionById(Long id) throws GeneralException, RegisterNotFoundException, NullParameterException {
        if (id == null) {
            throw new NullParameterException(" parameter id cannot be null in loadTransactionById.");
        }
        Transaction transaction = null;
        try {
            Query query = createQuery("SELECT t FROM Transaction t WHERE t.id = ?1");
            query.setParameter("1", id);
            transaction = (Transaction) query.setHint("toplink.refresh", "true").getSingleResult();
        } catch (NoResultException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, Recharge.class.getSimpleName(), "loadTransactionById", Recharge.class.getSimpleName(), null), ex);
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        return transaction;
    }

    public PaymentInfo savePaymentInfo(PaymentInfo paymentInfo) throws NullParameterException, GeneralException {
        if (paymentInfo == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "paymentInfo"), null);
        }
        return (PaymentInfo) saveEntity(paymentInfo);
    }

    public List<Transaction> loadTransactionByInvoice(String invoiceId) throws NullParameterException, EmptyListException, GeneralException {
        List<Transaction> transactions = new ArrayList<Transaction>();
        if (invoiceId == null) {
            throw new NullParameterException("Parameter externalId cannot be null in method getTransactionByExternalId");
        }
        try {
            Query query = createQuery("SELECT t FROM Transaction t WHERE t.invoice.id = ?1 ORDER BY t.id DESC");
            query.setParameter("1", Long.parseLong(invoiceId));
            transactions = query.setHint("toplink.refresh", "true").getResultList();

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GeneralException(ex.getMessage());
        }
        if (transactions.isEmpty()) {
            throw new EmptyListException("No transaction found in method getTransactionByExternalId.");
        }
        return transactions;
    }

    public Payment savePayment(Payment payment) throws GeneralException, NullParameterException {
        return (Payment) saveEntity(payment, logger, getMethodName());
    }

    public boolean persistListObject(List data) {
        //System.out.println("---------------persistListObject-----------");
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

    public List<Float> getTotalMonthlyTransactions(Long accountId, Long transactionTypeId, Date date) throws NullParameterException, GeneralException {
        List<Float> response = new ArrayList<Float>();

        try {
            if (accountId == null) {
                throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "accountId"), null);
            }

            StringBuilder sqlBuilder1 = new StringBuilder();
            if (transactionTypeId.equals(TransactionType.PURCHASE_BALANCE)) {
                sqlBuilder1.append("SELECT SUM(t.totalAmount) FROM services.transaction t WHERE  t.creationDate BETWEEN ?2  AND ?3 AND t.transactionStatus = 'PROCESSED'").append(" AND t.accountId= ?1").append(" AND  t.transactionTypeId IN (").append(TransactionType.PIN_PURCHASE).append(",").append(TransactionType.PIN_RECHARGE).append(")");
            } else {
                sqlBuilder1.append("SELECT SUM(t.totalAmount) FROM services.transaction t WHERE t.creationDate BETWEEN ?2 AND ?3 AND t.transactionStatus = 'PROCESSED'").append("AND  t.accountId= ?1").append(" AND t.transactionTypeId = ").append(transactionTypeId);
            }
            StringBuilder sqlBuilder2 = new StringBuilder();
            if (transactionTypeId.equals(TransactionType.PURCHASE_BALANCE)) {
                sqlBuilder2.append("SELECT SUM(t.totalAmount) FROM services.transaction t WHERE  t.creationDate BETWEEN ?2  AND ?3 AND t.transactionStatus = 'PROCESSED'").append(" AND t.accountId= ?1").append(" AND  t.transactionTypeId IN (").append(TransactionType.PIN_PURCHASE).append(",").append(TransactionType.PIN_RECHARGE).append(")");
            } else {
                sqlBuilder2.append("SELECT SUM(t.totalAmount) FROM services.transaction t WHERE t.creationDate BETWEEN ?2 AND ?3 AND t.transactionStatus = 'PROCESSED'").append("AND  t.accountId= ?1").append(" AND t.transactionTypeId = ").append(transactionTypeId);
            }

            Query query1 = entityManager.createNativeQuery(sqlBuilder1.toString());
            query1.setParameter("1", accountId);
            query1.setParameter("2", GeneralUtils.getFirstDateOfMonth(date));
            query1.setParameter("3", GeneralUtils.getLastDateOfMonth(date));
            //Double value1 = (Double) query1.setHint("toplink.refresh", "true").getSingleResult();//Glassfish 3.1.2
            List result = (List) query1.setHint("toplink.refresh", "true").getSingleResult();//Glassfish 2.1
            Double value1 = result.get(0) != null ? (Double) result.get(0) : 0f;//Glassfish 2.1
            response.add(value1 != null ? value1.floatValue() : 0f);

            Query query2 = entityManager.createNativeQuery(sqlBuilder2.toString());
            query2.setParameter("1", accountId);
            query2.setParameter("2", GeneralUtils.getFirstDateOfMonth(date));
            query2.setParameter("3", GeneralUtils.getLastDateOfMonth(date));
            //Double value2 = (Double) query2.setHint("toplink.refresh", "true").getSingleResult();//Glassfish 3.1.2
            result = (List) query2.setHint("toplink.refresh", "true").getSingleResult();//Glassfish 2.1
            Double value2 = result.get(0) != null ? (Double) result.get(0) : 0f;//Glassfish 2.1

            response.add(value2 != null ? value2.floatValue() : 0f);

            return response;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
    }

    public void cancelPurchaseBalanceAccount(EJBRequest request) throws NullParameterException, GeneralException, PurchaseCanceledException, EmptyListException, TransactionNotAvailableException, MaxAmountPerTransactionException, MaxAmountDailyException, MaxPromotionTransactionDailyException, NegativeBalanceException, MaxAmountBalanceException, MinAmountBalanceException {
        Map<String, Object> params = request.getParams();
//        Account account = transaction.getAccount();
        Transaction transaction = (Transaction) params.get(WSConstants.PARAM_TRANSACTION_DATA);
        Long languageId = (Long) params.get(WSConstants.PARAM_LANGUAGE_ID);
            if (transaction.getTransactionType().getId().equals(TransactionType.PURCHASE_BALANCE)) {
                try {
                    AccountData accountData = new AccountData();
                    accountData.setLogin(transaction.getAccount().getLogin());
                    accountData.setPassword(transaction.getAccount().getPassword());
                    BalanceHistory balanceHistory = createBalanceHistory(transaction.getAccount(), transaction.getTotalAmount(), 1, false);
                    if (servicesEJB.cancelPayment(accountData, transaction)) {
                        TransactionStatus transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.CANCELED);
                        transaction.setTransactionStatus(transactionStatus);
                        EJBRequest _request = new EJBRequest();
                        _request.setParam(transaction);
                        transaction = saveTransaction(_request);
                        _request = new EJBRequest();
                        _request.setParam(balanceHistory);
                        saveBalanceHistory(_request);
    }
                } catch (RegisterNotFoundException e1) {
                    e1.printStackTrace();
                } catch (NullParameterException e) {
                    e.printStackTrace();
                    throw e;
                } catch (GeneralException e) {
                    e.printStackTrace();
                    throw e;
                } catch (PurchaseCanceledException e) {
                    e.printStackTrace();
                    throw e;
                } catch (EmptyListException e) {
                    e.printStackTrace();
                    throw e;
                } catch (TransactionNotAvailableException e) {
                    e.printStackTrace();
                    throw e;
                } catch (MaxAmountPerTransactionException e) {
                    e.printStackTrace();
                    throw e;
                } catch (MaxAmountDailyException e) {
                    e.printStackTrace();
                    throw e;
                } catch (MaxPromotionTransactionDailyException e) {
                    e.printStackTrace();
                    throw e;
                } catch (MinAmountBalanceException e) {
                    e.printStackTrace();
                    throw e;
                } catch (NegativeBalanceException e) {
                    e.printStackTrace();
                    throw e;
                }catch (Exception e) {
                    e.printStackTrace();
                }

        }
    }

    public List<Currency> getCurrency() throws GeneralException, NullParameterException, EmptyListException {
        List<Currency> currency = new ArrayList<Currency>();
        try {
            Query query = createQuery("SELECT c FROM Currency c");
            currency = (List<Currency>) query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }
        if (currency.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return currency;
    }


    public Pin savePin(Pin pin) throws NullParameterException, GeneralException {
        if (pin == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "paymentInfo"), null);
        }
        return (Pin) saveEntity(pin);
    }


    public Pin loadPinBySerial(Long serial) throws GeneralException, RegisterNotFoundException, NullParameterException {
        if (serial == null) {
            throw new NullParameterException(" parameter id cannot be null in loadTransactionById.");
        }
        Pin pin =new Pin();
        try {
            String SQL ="SELECT p FROM Pin p WHERE p.serial ="+serial;
            Query query = createQuery(SQL);
//            query.setParameter("1", serial);
            System.out.println(query);
           pin = (Pin) query.setHint("toplink.refresh", "true").getSingleResult();
        } catch (NoResultException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, Recharge.class.getSimpleName(), "loadTransactionById", Recharge.class.getSimpleName(), null), ex);
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        return pin;
    }



        public AuditSms saveAuditSms(EJBRequest request) throws GeneralException, NullParameterException, EmptyListException, TransactionNotAvailableException, MaxAmountPerTransactionException, MaxAmountDailyException, MaxPromotionTransactionDailyException {
        if (validateTransaction(request)) {
            //Transaction transaction = executeAlopoints((Transaction) request.getParam());
            return (AuditSms) saveEntity(request, logger, getMethodName());
        } else {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ""), null);
        }
    }



        public List<AuditSms> getAuditSms() throws GeneralException, NullParameterException, EmptyListException {
        List<AuditSms> auditSms = new ArrayList<AuditSms>();
        StringBuilder sqlBuilder = new StringBuilder("SELECT a FROM AuditSms a");
        Query query = null;
        try {
            query = createQuery(sqlBuilder.toString());
            query.setMaxResults(60);
            auditSms = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception e) {
            //e.printStackTrace();
        }
        return auditSms;
    }


         public List<AuditSms> getAuditSmsByTime(Timestamp beginingDate, Timestamp endingDate) throws GeneralException, NullParameterException, EmptyListException {
        List<AuditSms> auditSms = new ArrayList<AuditSms>();
        StringBuilder sqlBuilder = new StringBuilder("SELECT a FROM AuditSms a where a.id > 0");
        if (beginingDate == null || endingDate == null) {

            System.out.println("error al momento uno de los parametros llego al metodo en null");
            System.out.println("error when one of the parameters to null method came to");

        } else {
            sqlBuilder.append("AND a.creationDate BETWEEN " + beginingDate + " AND " + endingDate + "");
        }
        Query query = null;
        try {
            query = createQuery(sqlBuilder.toString());
            query.setMaxResults(60);
            auditSms = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception e) {
            //e.printStackTrace();
        }
        return auditSms;
    }

   
    public float getRealValueByProdut(Account account, Float amount, Product product,TopUpProduct topUpProduct) throws GeneralException, NullParameterException {
        System.out.println("---------------getRealValueByProdut-----------");
        float realAmount = 0f;
        List<AccountProduct> accountProducts = new ArrayList<AccountProduct>();
        Float commissionValue = 0f;
        accountProducts = account.getAccountProducts();
        for (AccountProduct acp : accountProducts) {
            if (acp.getProduct().getId().equals(product.getId()) && acp.getEndingDate() == null) {
                commissionValue = acp.getCommission();
                break;
            }
        }
        if ( product.getId().longValue() == Product.TOP_UP_PRODUCT_ID.longValue())
            realAmount = amount - (((amount * topUpProduct.getCommissionPercent()) / 100)*(commissionValue/100));
        else
            realAmount = (amount - ((amount * commissionValue) / 100));
        return realAmount;
    }

    public Transaction loadTransactionByRechargeId(Long rechargeId) throws GeneralException, RegisterNotFoundException, NullParameterException {
        //System.out.println("---------------loadLastBalanceHistoryByDistributor-----------");
        if (rechargeId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "accountId"), null);
        }
        Transaction transaction = null;
        try {
            //Timestamp maxDate = (Timestamp) entityManager.createQuery("SELECT t FROM Transaction t WHERE t.recharge.id = " + rechargeId).getSingleResult();
            Query query = entityManager.createQuery("SELECT t FROM Transaction t WHERE t.recharge.id = " + rechargeId);
            transaction = (Transaction) query.setHint("toplink.refresh", "true").getSingleResult();
            //List result = (List) query.setHint("toplink.refresh", "true").getSingleResult();

//            if (!transaction.isEmpty()) {
//                transaction = ((Transaction) transaction.get(0));
//            }
        } catch (NoResultException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName(), "BalanceHistory"), null);
        } catch (Exception e) {
            e.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), "BalanceHistory"), null);
        }
        return transaction;
    }
    
    
//    //Metodo para validar el maximo monto de recarga y compra de pin
//    public boolean validateMaxAmount(BalanceHistory currentBalanceHistory, float amount, int transferType, boolean isBalanceTransference) throws MaxLimitPurchaseException, GeneralException, EmptyListException, NullParameterException, MaxLimitRechageException {
//        Map orderMap = new HashMap();
//        orderMap.put("id", "desc");
//        EJBRequest request = new EJBRequest();
//        Map acSrcParams = new HashMap();
//        acSrcParams.put("accountId", currentBalanceHistory.getAccount().getId());
//        request.setParams(acSrcParams);
//        List<AccountBalance> listAccountBalance = null;
//        listAccountBalance = (List<AccountBalance>) createSearchQuery(AccountBalance.class, request, orderMap, logger, getMethodName(), "AccountBalance", true);
//        AccountBalance accountBalance = listAccountBalance.get(0);
//
//        if (transferType == 1) {
//            if ((currentBalanceHistory.getCurrentAmount() + amount) > 100) {
//                throw new MaxLimitPurchaseException(logger, sysError.format(EjbConstants.ERR_MAX_AMOUNT_PURCHASE, this.getClass(), getMethodName(), "param"), null);
//            }
//        } else if (transferType == 2) {
//            if ((currentBalanceHistory.getCurrentAmount() + amount) > 100) {
//                throw new MaxLimitRechageException(logger, sysError.format(EjbConstants.ERR_MAX_AMOUNT_RECHARGE, this.getClass(), getMethodName(), "param"), null);
//            }
//        }
//        return true;
//    }
//
//    //Metodo que valida el maximo de tarjetas por customer
//     public boolean validateMaxCreditCard(Long accountId, Boolean enabled) throws MaxLimitCreditCardException, GeneralException, EmptyListException, NullParameterException {
//
//        if (getPaymentInfoByAccountId(accountId, true).size() > 2){
//           throw  new MaxLimitCreditCardException(logger, sysError.format(EjbConstants.ERR_MAX_CREDIT_CARD, this.getClass(), getMethodName(), "param"), null);
//
//        }
//         return true;
//
//     }
         
     
    
    
    

}
