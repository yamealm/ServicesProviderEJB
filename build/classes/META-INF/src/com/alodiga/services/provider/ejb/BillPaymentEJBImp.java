package com.alodiga.services.provider.ejb;

import com.alodiga.integration.prepaynation.servicemanager.ServiceManager;
import com.alodiga.services.provider.commons.models.TransactionStatus;
import com.alodiga.services.provider.commons.ejbs.BillPaymentEJB;
import com.alodiga.services.provider.commons.ejbs.BillPaymentEJBLocal;
import com.alodiga.services.provider.commons.ejbs.ProductEJBLocal;
import com.alodiga.services.provider.commons.ejbs.ServicesEJBLocal;
import com.alodiga.services.provider.commons.ejbs.TransactionEJBLocal;
import com.alodiga.services.provider.commons.ejbs.UserEJBLocal;
import com.alodiga.services.provider.commons.ejbs.UtilsEJBLocal;
import com.alodiga.services.provider.commons.exceptions.BillPayTransactionException;
import com.alodiga.services.provider.commons.exceptions.BillPaymenGeneralErrorException;
import com.alodiga.services.provider.commons.exceptions.BillPaymenProductUnrelatedToTheServiceException;
import com.alodiga.services.provider.commons.exceptions.BillPaymenTransactionNotAppliedException;
import com.alodiga.services.provider.commons.exceptions.BillPaymentAppliedTransactionException;
import com.alodiga.services.provider.commons.exceptions.BillPaymentConnectionErrorException;
import com.alodiga.services.provider.commons.exceptions.BillPaymentCredentialsNotFoundException;
import com.alodiga.services.provider.commons.exceptions.BillPaymentDeviceUnregisteredException;
import com.alodiga.services.provider.commons.exceptions.BillPaymentInvalidBranchException;
import com.alodiga.services.provider.commons.exceptions.BillPaymentInvalidDistributorException;
import com.alodiga.services.provider.commons.exceptions.BillPaymentInvalidFormatLocalDateException;
import com.alodiga.services.provider.commons.exceptions.BillPaymentInvalidFormatNumberPhoneException;
import com.alodiga.services.provider.commons.exceptions.BillPaymentInvalidIdentityException;
import com.alodiga.services.provider.commons.exceptions.BillPaymentInvalidPasswordException;
import com.alodiga.services.provider.commons.exceptions.BillPaymentInvalidProductException;
import com.alodiga.services.provider.commons.exceptions.BillPaymentInvalidServiceException;
import com.alodiga.services.provider.commons.exceptions.BillPaymentPasswordNotFoundException;
import com.alodiga.services.provider.commons.exceptions.BillPaymentPasswordNotGeneratedByAdministratorException;
import com.alodiga.services.provider.commons.exceptions.BillPaymentProductNotFoundException;
import com.alodiga.services.provider.commons.exceptions.BillPaymentInvalidProductException;
import com.alodiga.services.provider.commons.exceptions.BillPaymentNotBalanceException;
import com.alodiga.services.provider.commons.exceptions.CarrierSystemUnavailableException;
import com.alodiga.services.provider.commons.exceptions.DisabledAccountException;
import com.alodiga.services.provider.commons.exceptions.EmptyListException;
import com.alodiga.services.provider.commons.exceptions.GeneralException;
import com.alodiga.services.provider.commons.exceptions.InvalidAccountException;
import com.alodiga.services.provider.commons.exceptions.InvalidAmountException;
import com.alodiga.services.provider.commons.exceptions.InvalidCreditCardDateException;
import com.alodiga.services.provider.commons.exceptions.InvalidFormatException;
import com.alodiga.services.provider.commons.exceptions.InvalidPaymentInfoException;
import com.alodiga.services.provider.commons.exceptions.InvalidSubscriberNumberException;
import com.alodiga.services.provider.commons.exceptions.NullParameterException;
import com.alodiga.services.provider.commons.exceptions.PaymentDeclinedException;
import com.alodiga.services.provider.commons.exceptions.PaymentServiceUnavailableException;
import com.alodiga.services.provider.commons.exceptions.RegisterNotFoundException;
import com.alodiga.services.provider.commons.exceptions.SubscriberAccountException;
import com.alodiga.services.provider.commons.exceptions.SubscriberWillExceedLimitException;
import com.alodiga.services.provider.commons.genericEJB.AbstractSPRESTEJB;
import com.alodiga.services.provider.commons.genericEJB.EJBRequest;
import com.alodiga.services.provider.commons.genericEJB.SPContextInterceptor;
import com.alodiga.services.provider.commons.genericEJB.SPLoggerInterceptor;
import com.alodiga.services.provider.commons.managers.ContentManager;
import com.alodiga.services.provider.commons.models.Account;
import com.alodiga.services.provider.commons.models.BillPaymentCalculation;
import com.alodiga.services.provider.commons.models.Country;
import com.alodiga.services.provider.commons.models.CountryHasProvider;
import com.alodiga.services.provider.commons.models.Enterprise;
import com.alodiga.services.provider.commons.models.PaymentInfo;
import com.alodiga.services.provider.commons.models.PaymentType;
import com.alodiga.services.provider.commons.models.Provider;
import com.alodiga.services.provider.commons.models.Recharge;
import com.alodiga.services.provider.commons.models.RechargeStatus;
import com.alodiga.services.provider.commons.models.Transaction;
import com.alodiga.services.provider.commons.models.TransactionType;
import com.alodiga.services.provider.commons.models.User;
import com.alodiga.services.provider.commons.models.billPayment.BillPayResponseConstants;
import com.alodiga.services.provider.commons.models.billPayment.BillPaymentCatalog;
import com.alodiga.services.provider.commons.models.billPayment.BillPaymentProduct;
import com.alodiga.services.provider.commons.models.billPayment.BillPaymentResponse;
import com.alodiga.services.provider.commons.models.billPayment.BillPaymentServices;
import com.alodiga.services.provider.commons.models.billPayment.UpdateBillPaymentProduct;
import com.alodiga.services.provider.commons.payment.AuthorizeNet;
import com.alodiga.services.provider.commons.utils.AccountData;
import com.alodiga.services.provider.commons.utils.CommonMails;
import com.alodiga.services.provider.commons.utils.CreditCardUtils;
import com.alodiga.services.provider.commons.utils.EjbConstants;
import com.alodiga.services.provider.commons.utils.Mail;
import com.alodiga.services.provider.commons.utils.QueryConstants;
import com.alodiga.services.provider.commons.utils.ServiceConstans;
import com.alodiga.services.provider.commons.utils.ServiceMails;
import com.alodiga.services.provider.commons.utils.ServiceMailDispatcher;
import com.alodiga.services.provider.commons.utils.ServiceSMSDispatcher;
import com.alodiga.teleservicios.access.proxy.TeleserviciosBillpaymentIntegration;
import com.alodiga.teleservicios.models.CatalogService;
import com.alodiga.teleservicios.models.Product;
import com.alodiga.teleservicios.models.Service;
import com.alodiga.teleservicios.responses.ResponseBalance;
import com.alodiga.teleservicios.responses.ResponsePurchaseProduct;
import com.alodiga.ws.salesrecord.services.WsLoginResponse;
import com.alodiga.ws.salesrecord.services.WsSalesRecordProxy;
//import com.alodiga.teleservicios.access.proxy.TeleserviciosBillpaymentIntegration;
//import com.alodiga.teleservicios.models.CatalogService;
//import com.alodiga.teleservicios.models.Product;
//import com.alodiga.teleservicios.models.Service;
//import com.alodiga.teleservicios.responses.ResponseBalance;
//import com.alodiga.teleservicios.responses.ResponsePurchaseProduct;
import com.pininteract.OrderResponse;
import com.alodiga.services.provider.commons.utils.Constants;
import com.alodiga.ws.salesrecord.services.WsInvoiceResponse;
import com.alodiga.ws.salesrecord.services.WsOrderResponse;

import java.util.List;
import java.util.logging.Level;

import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.interceptor.Interceptors;

import org.apache.log4j.Logger;

import com.pininteract.www.Sku;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import javax.ejb.EJB;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;

@Interceptors({SPLoggerInterceptor.class, SPContextInterceptor.class})
@Stateless(name = EjbConstants.BILLPAYMENT_EJB, mappedName = EjbConstants.BILLPAYMENT_EJB)
@TransactionManagement(TransactionManagementType.BEAN)
public class BillPaymentEJBImp extends AbstractSPRESTEJB implements BillPaymentEJB, BillPaymentEJBLocal {

    private static final Logger logger = Logger.getLogger(BillPaymentEJBImp.class);
    @EJB
    private UtilsEJBLocal utilsEJB;
    @EJB
    private ProductEJBLocal productEJB;
    @EJB
    private UserEJBLocal userEJB;
    @EJB
    private ServicesEJBLocal servicesEJBLocal;
    @EJB
    private TransactionEJBLocal transactionEJB;

    public BillPaymentProduct saveBillPaymentProduct(EJBRequest request) throws GeneralException, NullParameterException {
        return (BillPaymentProduct) saveEntity(request, logger, getMethodName());
    }

    public void executePPNBillPaymentUpdate() throws GeneralException, BillPaymentInvalidProductException,BillPaymentNotBalanceException, BillPaymentConnectionErrorException,BillPaymentPasswordNotFoundException,BillPaymentInvalidFormatNumberPhoneException,BillPaymentInvalidServiceException,BillPaymenGeneralErrorException,BillPaymentInvalidDistributorException,BillPaymentInvalidFormatLocalDateException,BillPaymentInvalidBranchException,BillPaymentInvalidIdentityException,BillPaymentInvalidPasswordException,BillPaymentProductNotFoundException,BillPaymentAppliedTransactionException,BillPaymentDeviceUnregisteredException,BillPaymentCredentialsNotFoundException,BillPaymentPasswordNotGeneratedByAdministratorException,BillPaymenProductUnrelatedToTheServiceException,BillPaymenTransactionNotAppliedException{

        List unavailables = null;
//        com.alodiga.teleservicios.responses.ResponseListProducts response = new com.alodiga.teleservicios.responses.ResponseListProducts();
//        response = com.alodiga.teleservicios.access.proxy.TeleserviciosBillpaymentIntegration.getServicesResponse();
//
//        List<Product> productsNot = new ArrayList<Product>();
//        response = TeleserviciosBillpaymentIntegration.getServicesResponse();
//        String whereIn = "(";
//
//        if (response.getResponseCode().equals("01")) {
////            try {
//                for (Service s : response.getServices()) {
//                    for (CatalogService cs : s.getCatalogServices()) {
//                        for (Product p : cs.getProduct()) {
//                            BillPaymentProduct billPaymentProducts = new BillPaymentProduct();
//                            try {
//                                StringBuilder sqlBuilder = new StringBuilder("SELECT bpp FROM BillPaymentProduct bpp WHERE bpp.id =" + p.getIdProducto());
//                                Query query = entityManager.createQuery(sqlBuilder.toString());
//                                billPaymentProducts = (BillPaymentProduct) query.setHint("toplink.refresh", "true").getSingleResult();
//                            } catch (NoResultException ex) {
//                                productsNot.add(p);
//                            } catch (Exception ex) {
//                                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
//                            }
//                        }
//                    }
//                }
//                List<String> values = new ArrayList<String>();
//                if (productsNot.size() > 0) {
//                    for (Product p : productsNot) {
//                        values.add(p.getIdProducto() + "," + p.getNameProduct());
//                    }
//                }
////                Mail mail = ServiceMails.getPendingBillPaymentProduct(unavailables, "Actualizacion Bill Payment");
////                utilsEJB.sendMail(mail);
////            } catch (NullParameterException ex) {
////                throw new GeneralException("Error enviando Email");
////            }
//        } else if (response.getResponseCode().equals("11")) {
//            System.out.println("Error 11. Dispositivo no Identificado");
//            throw new BillPaymentInvalidProductException("");
//        } else if (response.getResponseCode().equals("02")) {
//            System.out.println("Error 02. Saldo insuficiente");
//            throw new BillPaymentNotBalanceException("");
//        } else if (response.getResponseCode().equals("03")) {
//            System.out.println("Error 03. Error en la conexion");
//            throw new BillPaymentConnectionErrorException("");
//        } else if (response.getResponseCode().equals("04")) {
//            System.out.println("Error 04. Error en la conexion");
//            throw new BillPaymentConnectionErrorException("");
//        } else if (response.getResponseCode().equals("41")) {
//            System.out.println("Error 41. Codigo del dispositivo invalido");
//        } else if (response.getResponseCode().equals("42")) {
//            System.out.println("Error 42. Parametro password no fué encontrado o viene en blanco");
//            throw new BillPaymentPasswordNotFoundException("");
//        } else if (response.getResponseCode().equals("43")) {
//            System.out.println("Error 43. El numero de telefono debe ser entre 8 y 10 digitos");
//            throw new BillPaymentInvalidFormatNumberPhoneException("");
//        } else if (response.getResponseCode().equals("44")) {
//            System.out.println("Error 44. Idservicio es incorrecto");
//            throw new BillPaymentInvalidServiceException("");
//        } else if (response.getResponseCode().equals("45")) {
//            System.out.println("Error 45. idProducto Incorrecto ");
//            throw new BillPaymentInvalidProductException("");
//        } else if (response.getResponseCode().equals("46")) {
//            System.out.println("Error 46. IdDistribuidor Incorrecto");
//            throw new BillPaymentInvalidDistributorException("");
//        } else if (response.getResponseCode().equals("47")) {
//            System.out.println("Error 47.Formato de hora local invalido");
//            throw new BillPaymentInvalidFormatLocalDateException("");
//        } else if (response.getResponseCode().equals("48")) {
//            System.out.println("Error 48. idSucursalIncorrecto");
//            throw new BillPaymentInvalidBranchException("");
//        } else if (response.getResponseCode().equals("05")) {
//            System.out.println("Error 05. Identidad Invalida");
//            throw new BillPaymentInvalidIdentityException("");
//        } else if (response.getResponseCode().equals("06")) {
//            System.out.println("Error 06. Transacción aplicada anteriormente(Recientemente)");
//            throw new BillPaymentAppliedTransactionException("");
//        } else if (response.getResponseCode().equals("51")) {
//            System.out.println("Error 51. Dispositivo no registrado");
//            throw new BillPaymentDeviceUnregisteredException("");
//        } else if (response.getResponseCode().equals("52")) {
//            System.out.println("Error 52. Las Credenciales no pertenecen al distribuidor ");
//            throw new BillPaymentCredentialsNotFoundException("");
//        } else if (response.getResponseCode().equals("53")) {
//            System.out.println("Error 53. Password no generado por el administrador ");
//            throw new BillPaymentPasswordNotGeneratedByAdministratorException("");
//        } else if (response.getResponseCode().equals("54")) {
//            System.out.println("Error 54.  Password incorrecto ");
//            throw new BillPaymentInvalidPasswordException("");
//        } else if (response.getResponseCode().equals("60")) {
//            System.out.println("Error 60. Producto inexistente");
//            throw new BillPaymentProductNotFoundException("");
//        } else if (response.getResponseCode().equals("61")) {
//            System.out.println("Error 61. Servicio inexistente");
//        } else if (response.getResponseCode().equals("62")) {
//            System.out.println("Error 62. Producto no relacionado al servicio");
//            throw new BillPaymenProductUnrelatedToTheServiceException("");
//        } else if (response.getResponseCode().equals("70")) {
//            System.out.println("Error 70 . La Transaccion no fue aplicada");
//            throw new BillPaymenTransactionNotAppliedException("");
//        } else if (response.getResponseCode().equals("81")) {
//            System.out.println("Error 81. El sistema esta efectuando un mantenimiento intente mas tarde ");
//            throw new GeneralException("");
//        } else if (response.getResponseCode().equals("82")) {
//            System.out.println("Error 82. ");
//            throw new BillPaymenGeneralErrorException("");
//        }
    }

    private void disableUnAvailablePrepayNationBillPayments() throws GeneralException {
        System.out.println("disableUnAvailablePrepayNationBillPayments");
        logger.info("[PROCESS] disableUnAvailablePrepayNationBillPayments");
        List unavailables = null;
        try {
            String sql1 = "SELECT DISTINCT(b.id) FROM services.bill_payment_product b WHERE b.providerId = ?1 AND b.enabled=1 AND b.referenceCode NOT IN (SELECT DISTINCT (u.referenceCode) FROM services.update_bill_payment_product u)";
            Query query = entityManager.createNativeQuery(sql1);
            query.setParameter("1", Provider.PREPAY_NATION);
            unavailables = query.setHint("toplink.refresh", "true").getResultList();
            if (unavailables != null && unavailables.size() > 0) {
                StringBuilder ids = new StringBuilder("");
                for (int i = 0; i < unavailables.size(); i++) {
                    String id = ((List) unavailables.get(i)).get(0).toString();
                    ids.append(id).append(",");
                }
                System.out.println("ids " + ids);
                ids.deleteCharAt(ids.length() - 1);

                try {
                    StringBuilder sqlBuilder1 = new StringBuilder("UPDATE services.bill_payment_product b SET b.enabled = 0 WHERE b.providerId = ?1 AND b.id IN (" + ids + ")");
                    EntityTransaction transaction = entityManager.getTransaction();
                    transaction.begin();
                    Query query1 = entityManager.createNativeQuery(sqlBuilder1.toString());
                    query1.setParameter("1", Provider.PREPAY_NATION);
                    query1.executeUpdate();
                    transaction.commit();
                } catch (Exception ex) {
                    throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
                }
            }
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
    }

    private void deletePPNBillPaymentProducts() throws GeneralException {
        logger.info("[PROCESS] deleteUpdatePrepayNationProducts");
        try {

            EntityTransaction transaction = entityManager.getTransaction();
            if (transaction.isActive()) {
                transaction.rollback();
            }
            transaction.begin();
            entityManager.createNativeQuery("TRUNCATE TABLE update_bill_payment_product").executeUpdate();
            transaction.commit();
            System.out.print("SUCCESS");
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
    }

    private void processPPNBillPaymentProducts() throws GeneralException {
        logger.info("[PROCESS] processPPNBillPaymentProducts");
        try {
            Timestamp now = new Timestamp(new java.util.Date().getTime());
            List<Sku> skus = ServiceManager.getBillPaymentProducts();
            EJBRequest request = new EJBRequest(new Long(Provider.PREPAY_NATION));
            Provider provider = productEJB.loadProvider(request);
            UpdateBillPaymentProduct ubpp = null;
            EntityTransaction transaction = entityManager.getTransaction();
            transaction.begin();
            for (int i = 0; i < skus.size(); i++) {
                boolean isOK = ((i % 100) == 0 || i == skus.size() - 1);
                //Permite hacer commit cada 100 registros
                ubpp = new UpdateBillPaymentProduct(skus.get(i));
                ubpp.setProvider(provider);
                ubpp.setEnabled(true);
                ubpp.setCreationDate(now);
                entityManager.persist(ubpp);
                if (isOK) {
                    transaction.commit();
                    if (!(i == skus.size() - 1)) {
                        transaction = entityManager.getTransaction();
                        transaction.begin();
                    }
                }
            }
            if (transaction.isActive()) {
                transaction.rollback();
            }
            System.out.println("End processPPNBillPaymentProducts " + Calendar.getInstance().getTime());
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }

    }

    private boolean processBillPaymentCountries(Enterprise enterprise, ArrayList<String> recipients) throws GeneralException, RegisterNotFoundException, NullParameterException {
        List<CountryHasProvider> countryHasProviders = this.processBillPaymentPrepayNationCountries();

        try {
            List<User> users = userEJB.getUserTopUpNotification();
            for (User user : users) {
                recipients.add(user.getEmail());
            }
        } catch (EmptyListException ele) {
            recipients.add(ServiceMails.SAC_COORDINADORES_MAIL);
        }

        if (countryHasProviders != null && countryHasProviders.size() > 0) {
            ServiceMailDispatcher.sendPendingDataMail(enterprise, countryHasProviders, recipients, "Actualizacion de Productos Bill-Payment");
            return true; // Interrumpe la ejecución de la función que lo invoca debe ser true.
        }
        return false; // Hace que no se ejecute la condición del if que lo llama y por lo tanto continua la ejecución normal.
    }

    private List<CountryHasProvider> processBillPaymentPrepayNationCountries() throws GeneralException {
        System.out.println("processBillPaymentPrepayNationCountries");
        logger.info("[PROCESS] processBillPaymentPrepayNationCountries");
        List result = new ArrayList();
        List<CountryHasProvider> pendingCountries = new ArrayList<CountryHasProvider>();
        try {
            StringBuilder sqlBuilder = new StringBuilder("SELECT ubp.* FROM services.update_bill_payment_product ubp WHERE ubp.countryCode NOT IN (SELECT c.referenceCode FROM services.country_has_provider c WHERE c.providerId = ?1) GROUP BY ubp.countryCode");
            Query query = entityManager.createNativeQuery(sqlBuilder.toString());
            query.setParameter("1", Provider.PREPAY_NATION);
            result = (List) query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }

        if (result != null && result.size() > 0) {
            for (int i = 0; i < result.size(); i++) {
                List object = (List) result.get(i);
                String referenceCode = object.get(9).toString(); //
                Provider provider = new Provider();
                provider.setId(new Long(Provider.PREPAY_NATION));
                try {
                    Country country = utilsEJB.loadCountryByShortName(referenceCode);
                    CountryHasProvider countryHasProvider = new CountryHasProvider();
                    countryHasProvider.setCountry(country);
                    countryHasProvider.setProvider(provider);
                    countryHasProvider.setReferenceCode(referenceCode);
                    utilsEJB.saveCountryHasProvider(countryHasProvider);
                } catch (RegisterNotFoundException ex) {
                    provider.setName("Prepay Nation");
                    String name = object.get(1).toString(); //
                    Country country = new Country();
                    country.setName("Nombre de operadora(Referencia) " + name);
                    CountryHasProvider countryHasProvider = new CountryHasProvider();
                    countryHasProvider.setCountry(country);
                    countryHasProvider.setReferenceCode(referenceCode);
                    countryHasProvider.setProvider(provider);
                    pendingCountries.add(countryHasProvider);
                } catch (Exception ex) {
                    throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
                }
            }
        }
        return pendingCountries;
    }

    private void disableModifiedPrepayNationBillPayments() throws GeneralException {
        System.out.println("disableModifiedPrepayNationBillPayments");
        logger.info("[PROCESS] disableModifiedPrepayNationBillPayments");
        try {
            String sql1 = "UPDATE BillPaymentProduct bp SET bp.enabled = FALSE "
                    + "WHERE bp.id IN (SELECT b.id FROM BillPaymentProduct b, UpdateBillPaymentProduct ubpp "
                    + "WHERE b.enabled= TRUE "
                    + "AND b.referenceCode = ubpp.referenceCode AND b.provider.id = ?1 "
                    + "AND (b.minAmount <> ubpp.minAmount OR b.maxAmount <> ubpp.maxAmount OR b.exchangeRate <> ubpp.exchangeRate))";
            /*TODO: con cada perfil*/
            entityManager.getTransaction().begin();
            Query query = createQuery(sql1);
            query.setParameter("1", Provider.PREPAY_NATION);
            query.executeUpdate();
            entityManager.getTransaction().commit();
        } catch (Exception ex) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw new GeneralException(ex.getMessage());
        }

    }

    private void includePrepayNationBillPayments() throws GeneralException {
        System.out.println("includePrepayNationBillPayments");
        logger.info("[PROCESS] includePrepayNationBillPayments");

        Timestamp now = new Timestamp(new java.util.Date().getTime());
        try {

            List<UpdateBillPaymentProduct> updateBillPaymentProducts = getUpdateBillPaymentProducts();
            List<BillPaymentProduct> billPaymentProducts = getActiveBillPaymentProducts();
            EntityTransaction transaction = entityManager.getTransaction();
            transaction.begin();
            int i = 0;
            for (UpdateBillPaymentProduct ubpp : updateBillPaymentProducts) {
                i++;
                if (!existBillPaymentProduct(billPaymentProducts, ubpp.getReferenceCode())) {
                    //Si no existe previamente persisto
                    Country country = utilsEJB.loadCountryByShortName(ubpp.getCountryCode());
                    BillPaymentProduct billPaymentProduct = new BillPaymentProduct();
                    billPaymentProduct.setCategory(ubpp.getCategory());
                    billPaymentProduct.setProviderFee(ubpp.getCommissionPercent());
                    billPaymentProduct.setCountry(country);
                    billPaymentProduct.setCountryCode(ubpp.getCountryCode());
                    billPaymentProduct.setCreationDate(now);
                    billPaymentProduct.setDescription(ubpp.getDescription());
                    billPaymentProduct.setEnabled(true);
                    billPaymentProduct.setExchangeRate(ubpp.getExchangeRate());
                    billPaymentProduct.setLocalPhoneNumberLength(ubpp.getLocalPhoneNumberLength());
                    billPaymentProduct.setMaxAmount(ubpp.getMaxAmount());
                    billPaymentProduct.setMinAmount(ubpp.getMinAmount());
                    billPaymentProduct.setName(ubpp.getName());
                    billPaymentProduct.setProductDenomination(ubpp.getProductDenomination());
                    billPaymentProduct.setProvider(ubpp.getProvider());
                    billPaymentProduct.setReferenceCode(ubpp.getReferenceCode());
                    billPaymentProduct.setCurrencyCode(ubpp.getCurrencyCode());
                    billPaymentProduct.setRequiredDigitsAccount(ubpp.getRequiredDigitsAccount());
                    entityManager.persist(billPaymentProduct);
                }

                boolean isOK = ((i % 100) == 0 || i == updateBillPaymentProducts.size() - 1);
                if (isOK) {
                    transaction.commit();
                    if (!(i == updateBillPaymentProducts.size() - 1)) {
                        transaction = entityManager.getTransaction();
                        transaction.begin();
                    }
                }
            }

        } catch (EmptyListException el) {
            el.printStackTrace();
        } catch (Exception ex) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw new GeneralException(ex.getMessage());
        }
    }

    public List<BillPaymentProduct> getActiveBillPaymentProducts() throws GeneralException, EmptyListException {
        System.out.println("getActiveBillPaymentProducts");
        logger.info("[PROCESS] getActiveBillPaymentProducts");
        List<BillPaymentProduct> billPaymentProducts = new ArrayList<BillPaymentProduct>();
        try {
            StringBuilder sqlBuilder = new StringBuilder("SELECT bpp FROM BillPaymentProduct bpp WHERE bpp.enabled = 1");
            Query query = entityManager.createQuery(sqlBuilder.toString());
            billPaymentProducts = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        if (billPaymentProducts.isEmpty()) {
            return null;
        }
        return billPaymentProducts;
    }

    public List<BillPaymentProduct> getBillPaymentProducts() throws GeneralException, EmptyListException {
        System.out.println("getBillPaymentProducts");
        logger.info("[PROCESS] getBillPaymentProducts");
        List<BillPaymentProduct> billPaymentProducts = new ArrayList<BillPaymentProduct>();
        try {
            StringBuilder sqlBuilder = new StringBuilder("SELECT bpp FROM BillPaymentProduct bpp");
            Query query = entityManager.createQuery(sqlBuilder.toString());
            billPaymentProducts = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        if (billPaymentProducts.isEmpty()) {
            return null;
        }
        return billPaymentProducts;
    }

    private boolean existBillPaymentProduct(List<BillPaymentProduct> billPaymentProducts, String referenceCode) {
        if (billPaymentProducts == null) {
            return false;
        }
        for (BillPaymentProduct bp : billPaymentProducts) {
            if (bp.getReferenceCode().equals(referenceCode)) {
                return true;
            }

        }
        return false;
    }

    public List<Country> getCountriesForBillPayment() throws GeneralException, EmptyListException {
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

    public List<BillPaymentProduct> getBillPaymentProductsByCountryId(Long countryId) throws NullParameterException, GeneralException, EmptyListException {

        if (countryId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "providerId"), null);
        }

        List<BillPaymentProduct> billPaymentProducts = new ArrayList<BillPaymentProduct>();
        try {
            StringBuilder sqlBuilder = new StringBuilder("SELECT bpp FROM BillPaymentProduct bpp WHERE bpp.enabled = 1 AND bpp.country.id=?1");
            Query query = entityManager.createQuery(sqlBuilder.toString());
            query.setParameter("1", countryId);
            billPaymentProducts = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        if (billPaymentProducts.isEmpty()) {
            return null;
        }

        return billPaymentProducts;
    }

    public void disableBillPaymentCalculations() throws GeneralException, NullParameterException {
        String sql = "UPDATE BillPaymentCalculation SET endingDate = CURRENT_DATE WHERE endingDate IS NULL";
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            createQuery(sql).executeUpdate();
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
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
    }

    public Float getTotalprepayNationFee() throws GeneralException {

        Float amount = 0F;
        try {
            //Float retailStoreAmount = getCommissionAmountByProfileId(Profile.RETAIL_STORE);
            Query query = createQuery("SELECT SUM(bpc.amount) FROM BillPaymentCalculation bpc WHERE bpc.endingDate IS NULL");
            Double total = (Double) query.setHint("toplink.refresh", "true").getSingleResult();
            amount = total.floatValue();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        return amount;
    }

    private void sendUpdateTopUpErrorMail(Enterprise enterprise, String process, String error, Exception ex) throws GeneralException {
        System.out.println("sendTopUpNotificationMail");
        logger.info("[PROCESS] sendTopUpNotificationMail");
        try {

            Mail mail = ServiceMails.getUpdateProcessErrorMail(enterprise, process, error, ex);
            //utilsEJB.sendMail(mail);
            (new com.alodiga.services.provider.commons.utils.MailSender(mail)).start();
        } catch (Exception e) {
            throw new GeneralException(e.getMessage());
        }
    }

    private List<UpdateBillPaymentProduct> getUpdateBillPaymentProducts() throws GeneralException, EmptyListException {
        System.out.println("getUpdateBillPaymentProducts");
        logger.info("[PROCESS] getUpdateBillPaymentProducts");
        List<UpdateBillPaymentProduct> billPaymentProducts = new ArrayList<UpdateBillPaymentProduct>();
        try {
            StringBuilder sqlBuilder = new StringBuilder("SELECT ubpp FROM UpdateBillPaymentProduct ubpp");
            Query query = entityManager.createQuery(sqlBuilder.toString());
            billPaymentProducts = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        if (billPaymentProducts.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return billPaymentProducts;
    }

    public Float getCommissionAmountByProfileId(Long profileId) throws NullParameterException, GeneralException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Float getTotalChargesForSales() throws GeneralException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public BillPaymentResponse doPrepayNationBillPay(BillPaymentProduct billPaymentProduct, Float amount, String accountNumber, String senderName, String senderNumber, Account account, boolean sendSMS, String smsToDestination, Long languageId) throws NullParameterException, BillPayTransactionException, CarrierSystemUnavailableException, InvalidFormatException, InvalidSubscriberNumberException, SubscriberWillExceedLimitException, SubscriberAccountException, GeneralException {
        System.out.println("------------------- doPrepayNationBillPay ------------------------");
        BillPaymentResponse response = new BillPaymentResponse();
        String error = "";
        try {
            int skuId = Integer.parseInt(billPaymentProduct.getReferenceCode());
            String externalId = account.getId() + "000" + Calendar.getInstance().getTimeInMillis();
            if (ServiceConstans.TEST_MODE) {
                return getBillPaymentTestResponse(accountNumber, senderNumber, externalId);
            }
            OrderResponse orderResponse = ServiceManager.billPay(skuId, amount, accountNumber, externalId, senderName, senderNumber);
            String code = orderResponse.getResponseCode();
            if (!code.equals("000")) {//Cuando es 000 esta bien...
                StringBuilder errorBuilder = new StringBuilder(BillPayResponseConstants.PREPAY_NATION_CODES.get(code));
                errorBuilder.append("Integrator= ").append("BillPayment - PrepayNation").append("ProductId = ").append(billPaymentProduct.getId()).append(" Amount = ").append(amount).append(" Account number= ").append(accountNumber);
                errorBuilder.append(orderResponse.getResponseMessage());
                error = errorBuilder.toString();
                if (code.equals("033")) {
                    throw new InvalidFormatException(error);
                } else if (code.equals("560") || code.equals("580") || code.equals("590") || code.equals("600")) {
                    throw new CarrierSystemUnavailableException(error);
                } else if (code.equals("501")) {
                    throw new InvalidSubscriberNumberException(error);
                } else if (code.equals("503")) {
                    throw new SubscriberWillExceedLimitException(error);
                } else if (code.equals("500")) {
                    throw new SubscriberAccountException(error);
                }
                throw new BillPayTransactionException(error);
            }

            String message1 = sendSMS ? ServiceSMSDispatcher.sendSenderBillPaySMS(amount, billPaymentProduct.getName(), accountNumber, senderName, senderNumber, account, smsToDestination, languageId) : smsToDestination;
            response.setSenderSMS(message1);
            response.setResponseCode(orderResponse.getResponseCode());
            response.setResponseMessage(orderResponse.getResponseMessage());
            response.setAccountNumber(accountNumber);
            response.setSenderNumber(senderNumber);
            response.setAuthenticationKey(String.valueOf(orderResponse.getInvoice().getInvoiceNumber()));
        } catch (InvalidFormatException ex) {
            throw (ex);
        } catch (CarrierSystemUnavailableException ex) {
            throw (ex);
        } catch (InvalidSubscriberNumberException ex) {
            throw (ex);
        } catch (SubscriberWillExceedLimitException ex) {
            throw (ex);
        } catch (BillPayTransactionException ex) {
            throw (ex);
        } catch (Exception ex) {
            throw new GeneralException(ex.getMessage() + error);
        }
        return response;
    }

    public BillPaymentResponse getBillPaymentTestResponse(String accountNumber, String senderNumber, String externalId) {
        Long time = Calendar.getInstance().getTimeInMillis();
        BillPaymentResponse response = new BillPaymentResponse();
        response.setAuthenticationKey(time.toString());
        response.setAccountNumber(accountNumber);
        response.setAuthenticationKey("TEST" + time);
        response.setResponseCode("000");
        response.setResponseMessage("OK. TEST");
        response.setSenderNumber(senderNumber);
        return response;
    }

    public BillPaymentResponse executeBillPayment(BillPaymentProduct billPaymentProduct, Float amount, String accountNumber, String senderName, String senderNumber, Account account, boolean sendSMS, String smsToDestination, Long languageId) throws BillPayTransactionException, InvalidFormatException, CarrierSystemUnavailableException, InvalidSubscriberNumberException, SubscriberWillExceedLimitException, SubscriberAccountException, GeneralException {
        System.out.println("---------------executeBillPayment-----------");
        try {
            BillPaymentResponse response = new BillPaymentResponse();
            switch (billPaymentProduct.getProvider().getId().intValue()) {
                case Provider.PREPAY_NATION:
                    response = this.doPrepayNationBillPay(billPaymentProduct, amount, accountNumber, senderName, senderNumber, account, sendSMS, smsToDestination, languageId);
                    break;
                default:
                    break;
            }
            return response;

        } catch (CarrierSystemUnavailableException ex) {
            throw new CarrierSystemUnavailableException(ex.getMessage());
        } catch (InvalidFormatException ex) {
            throw new InvalidFormatException(ex.getMessage());
        } catch (InvalidSubscriberNumberException ex) {
            throw new InvalidSubscriberNumberException(ex.getMessage());
        } catch (SubscriberWillExceedLimitException ex) {
            throw new SubscriberWillExceedLimitException(ex.getMessage());
        } catch (BillPayTransactionException ex) {
            ServiceMailDispatcher.sendBillPayErrorMail(billPaymentProduct, amount, account, accountNumber, senderName, senderNumber, "Error en el metodo executeBillPayment. " + ex.getMessage(), ex);
            throw new BillPayTransactionException(ex.getMessage());
        } catch (Exception ex) {
            ServiceMailDispatcher.sendBillPayErrorMail(billPaymentProduct, amount, account, accountNumber, senderName, senderNumber, "Error en el metodo executeBillPayment. " + ex.getMessage(), ex);
            throw new GeneralException(ex.getMessage());
        }
    }

    public BillPaymentProduct loadBillPaymentProductById(Long billPaymentProductId) throws GeneralException, RegisterNotFoundException, NullParameterException {
        if (billPaymentProductId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "billPaymentProductId"), null);
        }
        BillPaymentProduct billPaymentProduct = new BillPaymentProduct();
        try {
            Query query = createQuery("SELECT b FROM BillPaymentProduct b WHERE b.id = ?1");
            query.setParameter("1", billPaymentProductId);
            billPaymentProduct = (BillPaymentProduct) query.getSingleResult();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        if (billPaymentProduct == null) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return billPaymentProduct;
    }

    public float getBillPaymentCalculationByAccountId(Long accountId) throws GeneralException, RegisterNotFoundException, NullParameterException {
        if (accountId == null) {
            throw new NullParameterException("Parameter accountId cannot be null");
        }
        BillPaymentCalculation billPaymentCalculation = new BillPaymentCalculation();
        try {
            Query query = createQuery("SELECT b FROM BillPaymentCalculation b WHERE b.account.id = ?1 AND b.endingDate IS NULL");
            query.setParameter("1", accountId);
            billPaymentCalculation = (BillPaymentCalculation) query.getSingleResult();
        } catch (NoResultException ex) {
            return 0f;
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        return billPaymentCalculation.getFee();
    }

    public List<BillPaymentCalculation> getBillPaymentsCalculationByAccountId(Long accountId) throws GeneralException, NullParameterException, EmptyListException {
        if (accountId == null) {
            throw new NullParameterException("Parameter accountId cannot be null");
        }
        List<BillPaymentCalculation> billPaymentCalculations = new ArrayList<BillPaymentCalculation>();
        String sql = "SELECT b FROM BillPaymentCalculation b WHERE b.account.id = ?1";
        Query query = null;
        try {
            query = createQuery(sql);
            query.setParameter("1", accountId);
            billPaymentCalculations = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        if (billPaymentCalculations.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return billPaymentCalculations;
    }

    public BillPaymentCalculation loadBillPaymentCalculationByAccountId(Long accountId) throws NullParameterException, RegisterNotFoundException, GeneralException {
        if (accountId == null) {
            throw new NullParameterException("Parameter accountId cannot be null");
        }
        BillPaymentCalculation billPaymentCalculation = new BillPaymentCalculation();
        try {
            Query query = createQuery("SELECT b FROM BillPaymentCalculation b WHERE b.account.id = ?1 AND b.endingDate IS NULL");
            query.setParameter("1", accountId);
            billPaymentCalculation = (BillPaymentCalculation) query.getSingleResult();
        } catch (NoResultException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        return billPaymentCalculation;
    }

    public BillPaymentCalculation saveBillPaymentCalculation(BillPaymentCalculation billPaymentCalculation) throws GeneralException, NullParameterException {
        if (billPaymentCalculation == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "billPaymentCalculation"), null);
        }
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            if (billPaymentCalculation.getId() == null) {
                entityManager.persist(billPaymentCalculation);
            } else {
                entityManager.merge(billPaymentCalculation);
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
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        return billPaymentCalculation;
    }

    public BillPaymentCatalog saveBillPaymentCatalog(EJBRequest request) throws GeneralException, NullParameterException {
        return (BillPaymentCatalog) saveEntity(request, logger, getMethodName());
    }

    public BillPaymentServices saveBillPaymentServices(EJBRequest request) throws GeneralException, NullParameterException {
          return (BillPaymentServices) saveEntity(request, logger, getMethodName());
    }

    public List<BillPaymentProduct> getBillPaymentProductsByCatalogId(Long catalogId) throws NullParameterException,GeneralException, EmptyListException {
        if (catalogId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "catalogId"), null);
        }

        List<BillPaymentProduct> billPaymentProducts = new ArrayList<BillPaymentProduct>();
        try {
            StringBuilder sqlBuilder = new StringBuilder("SELECT bpp FROM BillPaymentProduct bpp WHERE bpp.billPaymentCatalog.id = ?1");
            Query query = entityManager.createQuery(sqlBuilder.toString());
            query.setParameter("1", catalogId);
            billPaymentProducts = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        if (billPaymentProducts.isEmpty()) {
            return null;
        }

        return billPaymentProducts;
    }

    public List<BillPaymentCatalog> getBillPaymentCatalogsByServicesId(Long servicesId) throws NullParameterException,GeneralException, EmptyListException {
                if (servicesId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "servicesId"), null);
        }

        List<BillPaymentCatalog> billPaymentCatalogs = new ArrayList<BillPaymentCatalog>();
        try {
            StringBuilder sqlBuilder = new StringBuilder("SELECT bpc FROM BillPaymentCatalog bpc WHERE bpp.billPaymentServices.id = ?1");
            Query query = entityManager.createQuery(sqlBuilder.toString());
            query.setParameter("1", servicesId);
            billPaymentCatalogs = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        if (billPaymentCatalogs.isEmpty()) {
            return null;
        }

        return billPaymentCatalogs;
    }

    public List<BillPaymentServices> getBillPaymentServices() throws GeneralException, EmptyListException {
        System.out.println("getBillPaymentServices");
        logger.info("[PROCESS] getBillPaymentServices");
        List<BillPaymentServices> billPaymentServices = new ArrayList<BillPaymentServices>();
        try {
            StringBuilder sqlBuilder = new StringBuilder("SELECT bps FROM BillPaymentServices bps");
            Query query = entityManager.createQuery(sqlBuilder.toString());
            billPaymentServices = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        if (billPaymentServices.isEmpty()) {
            return null;
        }
        return billPaymentServices;
    }



   public BillPaymentResponse executeTransactionByTypeAirTime(String phoneNumber, String serviceId, String productId) throws GeneralException, NullParameterException {
        ResponsePurchaseProduct responsePurchaseProduct = new ResponsePurchaseProduct();
        BillPaymentResponse billPaymentResponse = new BillPaymentResponse();
        if (phoneNumber == null || serviceId==null || productId==null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "phoneNumber,serviceId,productId"), null);
        }
        try {
            responsePurchaseProduct = TeleserviciosBillpaymentIntegration.executeTransactionByTypeAirTime(phoneNumber, serviceId, productId);
            if (responsePurchaseProduct.getResponseCode().equals("01")) {
                billPaymentResponse.setResponseTransactionNumber(responsePurchaseProduct.getResponseTransactionNumber());
                billPaymentResponse.setResponseAuthorizationNumber(responsePurchaseProduct.getResponseAuthorizationNumber());
                billPaymentResponse.setResponseBalance(responsePurchaseProduct.getResponseBalance());
                billPaymentResponse.setResponseCommission(responsePurchaseProduct.getResponseCommission());
                billPaymentResponse.setResponseBalance_f(responsePurchaseProduct.getResponseBalance_f());
                billPaymentResponse.setResponseCommission_f(responsePurchaseProduct.getResponseCommission_f());
                billPaymentResponse.setResponseDateTransaction(responsePurchaseProduct.getResponseDateTransaction());
                billPaymentResponse.setResponseAmount(responsePurchaseProduct.getResponseAmount());
                billPaymentResponse.setResponseCode(responsePurchaseProduct.getResponseCode());
                billPaymentResponse.setResponseText(responsePurchaseProduct.getResponseText());
            }
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        return billPaymentResponse;
    }

    public BillPaymentResponse executeTransactionByTypePaymentServices(String amount, String referentServices, String phone, String checkDigit, String productId, String serviceId) throws GeneralException, NullParameterException {
        ResponsePurchaseProduct responsePurchaseProduct = new ResponsePurchaseProduct();
        BillPaymentResponse billPaymentResponse = new BillPaymentResponse();
        if (amount == null || referentServices == null || phone == null || serviceId == null || productId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "amount,referentServices, phone,checkDigit,serviceId,productId"), null);
        }
        responsePurchaseProduct = TeleserviciosBillpaymentIntegration.executeTransactionByTypePaymentServices(amount, referentServices, phone, checkDigit, productId, serviceId);
        billPaymentResponse.setResponseCode(responsePurchaseProduct.getResponseCode());
        billPaymentResponse.setResponseText(responsePurchaseProduct.getResponseText());
        if (billPaymentResponse.getResponseCode().equals("01")) {
            billPaymentResponse.setResponseTransactionNumber(responsePurchaseProduct.getResponseTransactionNumber());
            billPaymentResponse.setResponseAuthorizationNumber(responsePurchaseProduct.getResponseAuthorizationNumber());
            billPaymentResponse.setResponseBalance(responsePurchaseProduct.getResponseBalance());
            billPaymentResponse.setResponseCommission(responsePurchaseProduct.getResponseCommission());
            billPaymentResponse.setResponseBalance_f(responsePurchaseProduct.getResponseBalance_f());
            billPaymentResponse.setResponseCommission_f(responsePurchaseProduct.getResponseCommission_f());
            billPaymentResponse.setResponseDateTransaction(responsePurchaseProduct.getResponseDateTransaction());
            billPaymentResponse.setResponseAmount(responsePurchaseProduct.getResponseAmount());
            billPaymentResponse.setResponseText(responsePurchaseProduct.getResponseText());
            return billPaymentResponse;
        }

        return billPaymentResponse;
    }

     public BillPaymentResponse executeTransactionByTypeAirTime(AccountData userData, PaymentInfo paymentInfo,String amount,String phoneNumber, String serviceId, String productId,String externalId,String registerUniId,String deliveryAddressId,String billingAddressId,String paymentInfoId,String salesChannelId,String ordenSourceId) throws GeneralException, NullParameterException, InvalidAccountException, DisabledAccountException, PaymentDeclinedException, InvalidPaymentInfoException,InvalidAmountException {
        ResponsePurchaseProduct responsePurchaseProduct = new ResponsePurchaseProduct();
        BillPaymentResponse billPaymentResponse = new BillPaymentResponse();
        if (phoneNumber == null || serviceId==null || productId==null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "phoneNumber,serviceId,productId"), null);
        }
        Account account = servicesEJBLocal.validateAccount(userData);
        Transaction transaction = null;
        Date now = new Date((new java.util.Date()).getTime());
        StringBuilder transactionData = new StringBuilder();
        Timestamp nowTimestamp = new Timestamp(now.getTime());
        Recharge recharge = null;
        TransactionType transactionType = null;
        TransactionStatus transactionStatus = null;
        float amount2 = Float.parseFloat(amount);
        BillPaymentProduct billPaymentProduct = null;
        try {
            billPaymentProduct = this.loadBillPaymentProductById(111L);
        } catch (RegisterNotFoundException ex) {
            
        }
        try {
            System.out.println("mmmmmmmmmmm"+paymentInfo.getId());
            recharge = this.processPayment(account, paymentInfo, amount2);
            System.out.println("22222222222"+paymentInfo.getId());
        } catch (InvalidAmountException ex) {
            try {
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.TOP_UP_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount2, 0f, 0f, nowTimestamp, ex.getMessage(), account, null, null, null, "Paso 1: Error ejecutando el Billpayment: " + productId + "Procesando el pago con Authorize.net (Monto Invalido).", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
            throw (ex);
        } catch (PaymentDeclinedException ex) {
            try {
                ServiceMailDispatcher.sendBillPayErrorMail(billPaymentProduct, amount2, account, phoneNumber, registerUniId, productId, "Paso 1: Procesando el pago con Authorize.net (FORMATO DE PAGO DECLINADO).", ex);
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.TOP_UP_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount2, 0f, 0f, nowTimestamp, ex.getMessage(), account, null, null, null, "Paso 1: Error ejecutando el TopUp: " + phoneNumber + "Procesando el pago con Authorize.net (PAGO DECLINADO).", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
            throw (ex);
        } catch (InvalidPaymentInfoException ex) {
            try {
                ServiceMailDispatcher.sendBillPayErrorMail(billPaymentProduct, amount2, account, phoneNumber, registerUniId, productId, "Paso 1: Procesando el pago con Authorize.net (FORMATO DE PAGO INVALIDO).", ex);
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.TOP_UP_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount2, 0f, 0f, nowTimestamp, ex.getMessage(), account, null, null, null, "Paso 1: Error ejecutando el TopUp: " + phoneNumber + "Procesando el pago con Authorize.net (FORMATO DE PAGO INVALIDO).", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
            throw (ex);
        } catch (Exception ex) {
            try {
                ServiceMailDispatcher.sendBillPayErrorMail(billPaymentProduct, amount2, account, phoneNumber, registerUniId, productId, "Paso 1: Procesando el pago con Authorize.net (ERROR GENERAL).", ex);
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.TOP_UP_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount2, 0f, 0f, nowTimestamp, ex.getMessage(), account, null, null, null, "Paso 1: Error ejecutando el TopUp: " + phoneNumber + "Procesando el pago con Authorize.net (ERROR GENERAL).", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
            } catch (Exception ex2) {
                ex2.printStackTrace();
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ""), ex);
            }
             throw new GeneralException("Exception trying saving transaction in method executeTransactionByTypePaymentServices. " + ex.getMessage());
        }
        try {
            //responsePurchaseProduct = TeleserviciosBillpaymentIntegration.executeTransactionByTypeAirTime(phoneNumber, serviceId, productId);
            responsePurchaseProduct.setResponseCode("01");
            if (responsePurchaseProduct.getResponseCode().equals("01")) {

                System.out.println("SETEO TODOOOOO");
                Random rand = new Random();
                Integer randomNum = rand.nextInt((10000 - 70) + 1) + 70;
                Integer randomNum2 = rand.nextInt((5000 - 100) + 1) + 100;
                billPaymentResponse.setResponseTransactionNumber(randomNum.toString());
                billPaymentResponse.setResponseAuthorizationNumber(randomNum2.toString());
                billPaymentResponse.setResponseBalance("10");
                billPaymentResponse.setResponseCommission("0.01");
                billPaymentResponse.setResponseBalance_f("8");
                billPaymentResponse.setResponseCommission_f("0.3");
                billPaymentResponse.setResponseDateTransaction(new Timestamp(new Date().getTime()).toString());
                billPaymentResponse.setResponseAmount(amount.toString());
                billPaymentResponse.setResponseText("0");


             // billPaymentResponse.setResponseTransactionNumber(responsePurchaseProduct.getResponseTransactionNumber());
             // billPaymentResponse.setResponseAuthorizationNumber(responsePurchaseProduct.getResponseAuthorizationNumber());
             // billPaymentResponse.setResponseBalance(responsePurchaseProduct.getResponseBalance());
             // billPaymentResponse.setResponseCommission(responsePurchaseProduct.getResponseCommission());
             // billPaymentResponse.setResponseBalance_f(responsePurchaseProduct.getResponseBalance_f());
             // billPaymentResponse.setResponseCommission_f(responsePurchaseProduct.getResponseCommission_f());
             // billPaymentResponse.setResponseDateTransaction(responsePurchaseProduct.getResponseDateTransaction());
             // billPaymentResponse.setResponseAmount(responsePurchaseProduct.getResponseAmount());
             // billPaymentResponse.setResponseCode(responsePurchaseProduct.getResponseCode());
             // billPaymentResponse.setResponseText(responsePurchaseProduct.getResponseText());

                transaction = new Transaction(new TransactionType(TransactionType.BILL_PAYMENT_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), amount2, 0f, 0f, nowTimestamp, null, account, null, null, billPaymentProduct, null, externalId, null, "", null, null, null, 0f, userData.getIpRemoteAddress());
                try {
                    String token = loadTokenOrdenInvoive();
                    System.out.println("Cargo el Token"+token);
                    WsSalesRecordProxy salesRecordProxy = new WsSalesRecordProxy();
                    System.out.println("Cargo el proxy");
                    String userData_ = userData.getUserId().toString();;
                    WsOrderResponse orderResponse = salesRecordProxy.generateOrder(token, Constants.ORDER_STATUS_PROCESS, null, registerUniId, deliveryAddressId, billingAddressId, paymentInfoId, userData_, amount, "0", "0", amount, "1", "0", salesChannelId, "1", ordenSourceId, "123", "4");
                    System.out.println("codeOrder" + orderResponse.getCode());
                    if (orderResponse.getCode().equals(Constants.TRANSACTION_SUCCESSFUL)) {
                        String responseOrderId = orderResponse.getId();
                        System.out.println("Si genero la orden"+ responseOrderId);
                        billPaymentResponse.setOrderId(responseOrderId);

                        WsInvoiceResponse invoiceResponse = salesRecordProxy.generateInvoice(token, Enterprise.ALODIGA_USA.toString(), Constants.INVOICE_TYPE_PIN_LINE, Constants.INVOICE_STATUS_PROCESS, "1", amount, String.valueOf(amount), "0", "0", "0", amount, "0", "0", "1", responseOrderId, "0", "0", null, null, registerUniId);
                        System.out.println("codeInvoice" + invoiceResponse.getCode());
                        if (invoiceResponse.getCode().equals(Constants.TRANSACTION_SUCCESSFUL)) {
                            System.out.println("Si genero la factura"+ invoiceResponse.getId());
                            billPaymentResponse.setInvoiceId(invoiceResponse.getId());
                        }
                    }

      
            } catch (Exception ex1) {
                ex1.printStackTrace();
            }



                
            } else
                throw new GeneralException("Fallo el BillPAyment el código de respuesta es:"+billPaymentResponse.getResponseCode());
         } catch (Exception ex) {
             transaction = new Transaction(new TransactionType(TransactionType.BILL_PAYMENT_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), amount2, 0f, 0f, nowTimestamp, null, account, null, null, billPaymentProduct, null, externalId, null, "", null, null, null, 0f, userData.getIpRemoteAddress());
            try {
                if (recharge != null && recharge.getResponseCode().equals("1")) {
                    servicesEJBLocal.cancelPayment(userData, transaction);
                }
            } catch (Exception e) {
                ServiceMailDispatcher.sendBillPayErrorMail(billPaymentProduct, amount2, account, phoneNumber, serviceId, productId, "Paso 3: Error tratando de cancelar el pago por error al procesar la transccion. (ERROR GENERAL).", ex);
                throw new GeneralException("Paso 3: Error tratando de cancelar el pago por error al procesar la transaccion de BillPayment. (ERROR GENERAL) " + e.getMessage());
            }
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), ex);
         } finally {
             try {
                 transaction = this.saveTransaction(transaction);
                 billPaymentResponse.setTransaction(transaction);
             } catch (Exception ex) {
                 ex.printStackTrace();
                 throw new GeneralException("Exception trying saving transaction in method executeTransactionByTypePaymentServices. " + ex.getMessage());
             }
         }

        return billPaymentResponse;
    }

    public BillPaymentResponse executeTransactionByTypePaymentServices(AccountData userData, PaymentInfo paymentInfo,String amount, String referentServices, String phone, String checkDigit, String productId, String serviceId,String externalId,String registerUniId,String deliveryAddressId,String billingAddressId,String paymentInfoId,String salesChannelId,String ordenSourceId) throws GeneralException, NullParameterException, InvalidAccountException, DisabledAccountException, PaymentDeclinedException, InvalidPaymentInfoException,InvalidAmountException {
        ResponsePurchaseProduct responsePurchaseProduct = new ResponsePurchaseProduct();
        BillPaymentResponse billPaymentResponse = new BillPaymentResponse();
        if (amount == null || referentServices == null || phone == null || serviceId == null || productId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "amount,referentServices, phone,checkDigit,serviceId,productId"), null);
        }
         Account account = servicesEJBLocal.validateAccount(userData);
        Transaction transaction = null;
        Date now = new Date((new java.util.Date()).getTime());
        StringBuilder transactionData = new StringBuilder();
        Timestamp nowTimestamp = new Timestamp(now.getTime());
        Recharge recharge = null;
        TransactionType transactionType = null;
        TransactionStatus transactionStatus = null;
        Float amount2 =  Float.parseFloat(amount);
        BillPaymentProduct billPaymentProduct = null;
        try {
            billPaymentProduct = this.loadBillPaymentProductById(60L);
        } catch (RegisterNotFoundException ex) {

        }
        try {
            recharge = this.processPayment(account, paymentInfo, amount2);
        } catch (InvalidAmountException ex) {
            try {
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.TOP_UP_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount2, 0f, 0f, nowTimestamp, ex.getMessage(), account, null, null, null, "Paso 1: Error ejecutando el BillPayment: " + productId + "Procesando el pago con Authorize.net (Monto Invalido).", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
            throw (ex);
        } catch (PaymentDeclinedException ex) {
            try {
                ServiceMailDispatcher.sendBillPayErrorMail(billPaymentProduct, amount2, account, referentServices, serviceId, phone, "Paso 1: Procesando el pago con Authorize.net (FORMATO DE PAGO DECLINADO).", ex);
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.TOP_UP_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount2, 0f, 0f, nowTimestamp, ex.getMessage(), account, null, null, null, "Paso 1: Error ejecutando el BillPayment: " + productId + "Procesando el pago con Authorize.net (PAGO DECLINADO).", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
            throw (ex);
        } catch (InvalidPaymentInfoException ex) {
            try {
                ServiceMailDispatcher.sendBillPayErrorMail(billPaymentProduct, amount2, account, referentServices, serviceId, phone, "Paso 1: Procesando el pago con Authorize.net (FORMATO DE PAGO INVALIDO).", ex);
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.TOP_UP_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount2, 0f, 0f, nowTimestamp, ex.getMessage(), account, null, null, null, "Paso 1: Error ejecutando el BillPayment: " + productId + "Procesando el pago con Authorize.net (FORMATO DE PAGO INVALIDO).", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
            throw (ex);
        } catch (Exception ex) {
            try {
                ServiceMailDispatcher.sendBillPayErrorMail(billPaymentProduct, amount2, account, referentServices, serviceId, phone, "Paso 1: Procesando el pago con Authorize.net (ERROR GENERAL).", ex);
                transactionType = ContentManager.getInstance().getTransactionTypeById(TransactionType.TOP_UP_PURCHASE);
                transactionStatus = ContentManager.getInstance().getTransactionStatusById(TransactionStatus.FAILED);
                transaction = new Transaction(transactionType, transactionStatus, amount2, 0f, 0f, nowTimestamp, ex.getMessage(), account, null, null, null, "Paso 1: Error ejecutando el BillPayment: " + productId + "Procesando el pago con Authorize.net (ERROR GENERAL).", externalId, null, null, null, null, paymentInfo,0f,userData.getIpRemoteAddress());
                this.saveTransaction(transaction);
            } catch (Exception ex2) {
                ex2.printStackTrace();
                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ""), ex);
            }
             throw new GeneralException("Exception trying saving transaction in method executeTransactionByTypePaymentServices. " + ex.getMessage());
        }
        try {
            //Descablear
            //responsePurchaseProduct = TeleserviciosBillpaymentIntegration.executeTransactionByTypePaymentServices(amount, referentServices, phone, checkDigit, productId, serviceId);
//            billPaymentResponse.setResponseCode(responsePurchaseProduct.getResponseCode());
//            billPaymentResponse.setResponseText(responsePurchaseProduct.getResponseText());
            //Descablear
              billPaymentResponse.setResponseCode("01");
            if (billPaymentResponse.getResponseCode().equals("01")) {
                Random rand = new Random();
                Integer randomNum = rand.nextInt((10000 - 70) + 1) + 70;
                Integer randomNum2 = rand.nextInt((5000 - 100) + 1) + 100;
                billPaymentResponse.setResponseTransactionNumber(randomNum.toString());
                billPaymentResponse.setResponseAuthorizationNumber(randomNum2.toString());
                billPaymentResponse.setResponseBalance("10");
                billPaymentResponse.setResponseCommission("0.01");
                billPaymentResponse.setResponseBalance_f("8");
                billPaymentResponse.setResponseCommission_f("0.3");
                billPaymentResponse.setResponseDateTransaction(new Timestamp(new Date().getTime()).toString());
                billPaymentResponse.setResponseAmount(amount.toString());
                billPaymentResponse.setResponseText("0");
//                billPaymentResponse.setResponseTransactionNumber(responsePurchaseProduct.getResponseTransactionNumber());
//                billPaymentResponse.setResponseAuthorizationNumber(responsePurchaseProduct.getResponseAuthorizationNumber());
//                billPaymentResponse.setResponseBalance(responsePurchaseProduct.getResponseBalance());
//                billPaymentResponse.setResponseCommission(responsePurchaseProduct.getResponseCommission());
//                billPaymentResponse.setResponseBalance_f(responsePurchaseProduct.getResponseBalance_f());
//                billPaymentResponse.setResponseCommission_f(responsePurchaseProduct.getResponseCommission_f());
//                billPaymentResponse.setResponseDateTransaction(responsePurchaseProduct.getResponseDateTransaction());
//                billPaymentResponse.setResponseAmount(responsePurchaseProduct.getResponseAmount());
//                billPaymentResponse.setResponseText(responsePurchaseProduct.getResponseText());
                transaction = new Transaction(new TransactionType(TransactionType.BILL_PAYMENT_PURCHASE), new TransactionStatus(TransactionStatus.PROCESSED), amount2, 0f, 0f, nowTimestamp, transactionData.toString(), account, null, null, billPaymentProduct, transactionData.toString(), externalId, recharge, "", null, null, null, 0f, userData.getIpRemoteAddress());
                try {
                 
                    String token = loadTokenOrdenInvoive();
                    WsSalesRecordProxy salesRecordProxy = new WsSalesRecordProxy();
                    WsOrderResponse orderResponse = salesRecordProxy.generateOrder(token, Constants.ORDER_STATUS_PROCESS, null, registerUniId, deliveryAddressId, billingAddressId, paymentInfoId, userData.getUserId().toString(), transaction.getTotalAmount().toString(), transaction.getTotalTax().toString(), "0", transaction.getTotalAmount().toString(), "1", "0", salesChannelId, "1", ordenSourceId, billPaymentProduct.getName(), "4");
                    System.out.println("codeOrder" + orderResponse.getCode());
                    if (orderResponse.getCode().equals(Constants.TRANSACTION_SUCCESSFUL)) {


                        String responseOrderId = orderResponse.getId();
                        billPaymentResponse.setOrderId(responseOrderId);

                        WsInvoiceResponse invoiceResponse = salesRecordProxy.generateInvoice(token, Enterprise.ALODIGA_USA.toString(), Constants.INVOICE_TYPE_PIN_LINE, Constants.INVOICE_STATUS_PROCESS, "1", transaction.getTotalTax().toString(), String.valueOf(amount), "0", "0", "0", transaction.getTotalAmount().toString(), "0", "0", "1", responseOrderId, "0", "0", null, null, registerUniId);
                        System.out.println("codeInvoice" + invoiceResponse.getCode());
                        if (invoiceResponse.getCode().equals(Constants.TRANSACTION_SUCCESSFUL)) {
                            billPaymentResponse.setInvoiceId(invoiceResponse.getId());
                        }
                    }
            } catch (Exception ex1) {
                ex1.printStackTrace();
            }
            }else
                throw new GeneralException("Fallo el BillPAyment el código de respuesta es:"+billPaymentResponse.getResponseCode());
//            return billPaymentResponse;
        } catch (Exception ex) {
            transaction = new Transaction(new TransactionType(TransactionType.BILL_PAYMENT_PURCHASE), new TransactionStatus(TransactionStatus.FAILED), amount2, 0f, 0f, nowTimestamp, null, account, null, null, billPaymentProduct, null, externalId, null, "", null, null, null, 0f, userData.getIpRemoteAddress());
            try {
                if (recharge != null && recharge.getResponseCode().equals("1")) {
                    servicesEJBLocal.cancelPayment(userData, transaction);
                }
            } catch (Exception e) {
                ServiceMailDispatcher.sendBillPayErrorMail(billPaymentProduct, amount2, account, referentServices, serviceId, phone, "Paso 3: Error tratando de cancelar el pago por error al procesar la transccion. (ERROR GENERAL).", ex);
                throw new GeneralException("Paso 3: Error tratando de cancelar el pago por error al procesar la transaccion de BillPayment. (ERROR GENERAL) " + e.getMessage());
            }
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), null), ex);
        } finally {
            try {
                transaction = this.saveTransaction(transaction);
                billPaymentResponse.setTransaction(transaction);
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new GeneralException("Exception trying saving transaction in method executeTransactionByTypePaymentServices. " + ex.getMessage());
            }
        }

        return billPaymentResponse;
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

    private Transaction saveTransaction(Transaction transaction) throws NullParameterException, GeneralException {
        try {
            transaction = (Transaction) saveEntity(transaction);
        } catch (NullParameterException ex) {
            throw (ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName(), "General Exception. trying to saving transaction.."), ex);
        }
        return transaction;
    }

    public Float executeGetBalanceByFinalCustomer(String phoneNumber, String serviceId, String productId) throws GeneralException, NullParameterException {
        ResponsePurchaseProduct responsePurchaseProduct = new ResponsePurchaseProduct();
        Float balanceCustomer = 0f;
        BillPaymentResponse billPaymentResponse = new BillPaymentResponse();
        if (serviceId == null || productId == null || phoneNumber == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "referentServices, phone,serviceId"), null);
        }
        try {
            responsePurchaseProduct = TeleserviciosBillpaymentIntegration.executeGetBalanceByFinalCustomer(phoneNumber, serviceId, productId);
            if (responsePurchaseProduct.getResponseCode().equals("01")) {
                balanceCustomer = Float.parseFloat(responsePurchaseProduct.getResponseBalanceCustomer());
                System.out.println("El saldo es:"+responsePurchaseProduct.getResponseBalanceCustomer());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        return balanceCustomer;
    }


    public Float getMyBalance() throws GeneralException {

        System.out.println("Entro al método que calcula el saldo de la cuenta");
        Float balance = 0f;

//        try {
//            ResponseBalance responseBalance = new ResponseBalance();
//            System.out.println("Crea el objeto");
//            responseBalance = TeleserviciosBillpaymentIntegration.getMyBalance();
//             System.out.println("Devuelve Respuesta");
//            if (responseBalance.getResponseCode().equals("10")||responseBalance.getResponseCode().equals("01")) {
//
//                balance = Float.parseFloat(responseBalance.getResponseBalance());
//                System.out.println("El saldo es:"+responseBalance.getResponseBalance());
//            }
//        } catch (Exception ex) {
//            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
//        }
        return balance;
    }


    public String getServiceIdByProductId(String productId) throws GeneralException {
        System.out.println("el productId es:"+productId);
        BillPaymentProduct billPaymentProduct = new BillPaymentProduct();
        try {
            StringBuilder sqlBuilder = new StringBuilder("SELECT bps FROM BillPaymentProduct bps WHERE bps.referenceCode =" + productId);
            Query query = entityManager.createQuery(sqlBuilder.toString());
            billPaymentProduct = (BillPaymentProduct) query.setHint("toplink.refresh", "true").getSingleResult();
            if (billPaymentProduct != null) {
                   System.out.println("el productId es:"+productId+"y el servicio al cual pertenece="+billPaymentProduct.getBillPaymentCatalog().getExternalServiceId());
                return billPaymentProduct.getBillPaymentCatalog().getExternalServiceId();
            }
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        return "0";
    }

}
