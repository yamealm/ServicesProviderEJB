package com.alodiga.services.provider.ejb;

import com.alodiga.csq.topup.integration.CSQManager;
import com.alodiga.csq.topup.integration.exceptions.CSQDuplicatedTransactionException;
import com.alodiga.csq.topup.integration.exceptions.CSQGeneralException;
import com.alodiga.csq.topup.integration.exceptions.CSQInvalidAmountException;
import com.alodiga.csq.topup.integration.exceptions.CSQInvalidPhoneNumberException;
import com.alodiga.csq.topup.integration.exceptions.CSQNullParameterException;
import com.alodiga.csq.topup.integration.exceptions.CSQRejectedByOperatorException;
import com.alodiga.csq.topup.integration.exceptions.CSQTemporarilyUnavailableProductException;
import com.alodiga.csq.topup.integration.responses.CSQTopUpResponse;
import com.alodiga.integration.kddi.web.services.RechargeTopUpWebKDDIProxy;
import com.alodiga.integration.prepaynation.servicemanager.ServiceManager;
import com.alodiga.services.provider.commons.ejbs.ProductEJBLocal;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import org.apache.log4j.Logger;
import com.alodiga.services.provider.commons.ejbs.TopUpProductEJB;
import com.alodiga.services.provider.commons.ejbs.TopUpProductEJBLocal;
import com.alodiga.services.provider.commons.ejbs.UserEJBLocal;
import com.alodiga.services.provider.commons.ejbs.UtilsEJBLocal;
import com.alodiga.services.provider.commons.exceptions.CarrierSystemUnavailableException;
import com.alodiga.services.provider.commons.exceptions.DestinationNotPrepaidException;
import com.alodiga.services.provider.commons.exceptions.DuplicatedTransactionException;
import com.alodiga.transferto.integration.connection.RequestManager;
import com.alodiga.services.provider.commons.exceptions.EmptyListException;
import com.alodiga.services.provider.commons.exceptions.GeneralException;
import com.alodiga.services.provider.commons.exceptions.InvalidAmountException;
import com.alodiga.services.provider.commons.exceptions.InvalidFormatException;
import com.alodiga.services.provider.commons.exceptions.InvalidPhoneNumberException;
import com.alodiga.services.provider.commons.exceptions.NotAvaliableServiceException;
import com.alodiga.services.provider.commons.exceptions.NullParameterException;
import com.alodiga.services.provider.commons.exceptions.RegisterNotFoundException;
import com.alodiga.services.provider.commons.exceptions.SubstringBetweenException;
import com.alodiga.services.provider.commons.exceptions.TopUpProductNotAvailableException;
import com.alodiga.services.provider.commons.exceptions.TopUpTransactionException;
import com.alodiga.services.provider.commons.genericEJB.AbstractSPEJB;
import com.alodiga.services.provider.commons.genericEJB.SPContextInterceptor;
import com.alodiga.services.provider.commons.genericEJB.SPLoggerInterceptor;
import com.alodiga.services.provider.commons.genericEJB.EJBRequest;
import com.alodiga.services.provider.commons.models.Account;
import com.alodiga.services.provider.commons.models.MobileOperator;
import com.alodiga.services.provider.commons.models.MobileOperatorHasProvider;
import com.alodiga.services.provider.commons.models.TopUpCalculation;
import com.alodiga.services.provider.commons.models.TopUpProduct;
import com.alodiga.services.provider.commons.models.User;
import com.alodiga.services.provider.commons.models.Currency;
import com.alodiga.services.provider.commons.models.Enterprise;
import com.alodiga.services.provider.commons.models.Country;
import com.alodiga.services.provider.commons.models.CountryHasProvider;
import com.alodiga.services.provider.commons.models.NautaProduct;
import com.alodiga.services.provider.commons.models.Payment;
import com.alodiga.services.provider.commons.models.Product;
import com.alodiga.services.provider.commons.models.ProductDenomination;
import com.alodiga.services.provider.commons.models.Provider;
import com.alodiga.services.provider.commons.models.TopUpResponseConstants;
import com.alodiga.services.provider.commons.models.UpdatePrepayNationCarriers;
import com.alodiga.services.provider.commons.models.UpdatePrepayNationProduct;
import com.alodiga.services.provider.commons.models.UpdateTransferToProduct;
import com.alodiga.services.provider.commons.responses.GeneralTopUpResponse;
import com.alodiga.services.provider.commons.utils.Constants;
import com.alodiga.services.provider.commons.utils.EjbConstants;
import com.alodiga.services.provider.commons.utils.Mail;
import com.alodiga.services.provider.commons.utils.ServiceConstans;
import com.alodiga.services.provider.commons.utils.ServiceMails;
import com.alodiga.services.provider.commons.utils.ServiceMailDispatcher;
import com.alodiga.services.provider.commons.utils.ServiceSMSDispatcher;
import com.alodiga.soap.integration.easycall.SoapClient;
import com.alodiga.soap.integration.easycall.model.DoTopUpResponse;
import com.alodiga.transferto.integration.model.MSIDN_INFOResponse;
import com.alodiga.transferto.integration.model.ReserveResponse;
import com.alodiga.transferto.integration.model.TopUpResponse;
import com.pininteract.Carrier;
import com.pininteract.OrderResponse;
import com.pininteract.www.Sku;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import javax.ejb.EJB;
import javax.interceptor.Interceptors;
import javax.naming.InitialContext;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;

@Interceptors({SPLoggerInterceptor.class, SPContextInterceptor.class})
@Stateless(name = EjbConstants.TOP_UP_EJB, mappedName = EjbConstants.TOP_UP_EJB)
@TransactionManagement(TransactionManagementType.BEAN)
public class TopUpProductEJBImpl extends AbstractSPEJB implements TopUpProductEJB, TopUpProductEJBLocal {

    private static final Logger logger = Logger.getLogger(TopUpProductEJBImpl.class);
    @EJB
    private UtilsEJBLocal utilsEJB;
    @EJB
    private UserEJBLocal userEJB;
    @EJB
    private ProductEJBLocal productEJB;
  

    public void closePendingTopUpCommissionChangeByProfile(Long profileId) throws NullParameterException, EmptyListException, GeneralException {

        if (profileId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "profileId"), null);
        }
        String sql = "UPDATE TopUpCommissionChange tcc SET tcc.endingDate = CURRENT_DATE WHERE tcc.endingDate IS NULL AND tcc.profile.id=" + profileId;
        try {
            EntityTransaction transaction = entityManager.getTransaction();
            transaction.begin();
            createQuery(sql).executeUpdate();
            transaction.commit();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }

    }

    public MobileOperator deleteOperators(EJBRequest request) {
        //TODO:implementar
        return null;
    }

    public void disableTopUpProduct(TopUpProduct topUpProduct) throws GeneralException, NullParameterException {
        if (topUpProduct == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "topUpProductId"), null);
        }
        try {
            String sql1 = "UPDATE TopUpProduct tup SET tup.enabled = FALSE WHERE tup.id= " + topUpProduct.getId();
            entityManager.getTransaction().begin();
            createQuery(sql1).executeUpdate();
            entityManager.getTransaction().commit();
        } catch (Exception ex) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw new GeneralException(ex.getMessage());
        }
    }

    public List<TopUpProduct> getAlternativeTopUpProducts(TopUpProduct currentProduct) throws GeneralException {

        List<TopUpProduct> alternativeProducts = new ArrayList<TopUpProduct>();
        try {
            String sql = "SELECT t FROM TopUpProduct t WHERE t.mobileOperator.id = " + currentProduct.getMobileOperator().getId()
                    + " AND t.productDenomination.id = " + currentProduct.getProductDenomination().getId() + " AND t.id <> " + currentProduct.getId()
                    + " AND t.enabled = 1 ORDER BY t.commissionPercent DESC";
            System.out.println("sql " + sql);
            alternativeProducts = createQuery(sql).setHint("toplink.refresh", "true").getResultList();
        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }
        System.out.println("alternativeProducts " + alternativeProducts);
        return alternativeProducts;
    }

    public List<Country> getCountries() throws NullParameterException, EmptyListException, GeneralException {
        List<Country> countries = new ArrayList<Country>();
        //String sql = "SELECT DISTINCT m.country FROM MobileOperator m WHERE m.enabled = TRUE ORDER BY m.country.name";
        String sql = "SELECT DISTINCT tp.mobileOperator.country FROM TopUpProduct tp WHERE tp.enabled = TRUE AND tp.mobileOperator.enabled = TRUE ORDER BY tp.mobileOperator.country.name";
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

    public MobileOperatorHasProvider getMobileOperatorHasProvider(Long mobileOperatorId, Long providerId) throws NullParameterException, RegisterNotFoundException, GeneralException {
        if (providerId == null || mobileOperatorId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "providerId - mobileOperatorId"), null);
        }
        MobileOperatorHasProvider mobileOperatorHasProvider = null;
        String sql = "SELECT mohp FROM MobileOperatorHasProvider mohp WHERE mohp.mobileOperator.id=" + mobileOperatorId + " AND mohp.provider.id=" + providerId;
        try {
            mobileOperatorHasProvider = (MobileOperatorHasProvider) createQuery(sql).setHint("toplink.refresh", "true").getSingleResult();
        }  catch (NoResultException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, MobileOperator.class.getSimpleName(), getMethodName(), MobileOperator.class.getSimpleName(), null), ex);
        }catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        return mobileOperatorHasProvider;

    }

    public List<MobileOperator> getMobileOperators(EJBRequest request) throws GeneralException, RegisterNotFoundException, NullParameterException, EmptyListException {
        List<MobileOperator> mobileOperators = new ArrayList<MobileOperator>();
        String sql = "SELECT DISTINCT tp.mobileOperator FROM TopUpProduct tp WHERE tp.enabled = TRUE AND tp.mobileOperator.enabled = TRUE ORDER BY tp.mobileOperator.name";
        Query query = null;
        try {
            query = createQuery(sql);
            mobileOperators = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        if (mobileOperators.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return mobileOperators;
    }

    public List<MobileOperator> getOperatorsByCondition(EJBRequest request) {
        return null;
    }

    public List<MobileOperator> getOperatorsByCountryId(Long countryId) throws NullParameterException, EmptyListException, GeneralException {
        if (countryId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "providerId"), null);
        }
        List<MobileOperator> mobileOperators = new ArrayList<MobileOperator>();

        try {
            Query query = createQuery("SELECT m FROM MobileOperator m WHERE m.country.id = ?1 AND m.enabled = TRUE");
            query.setParameter("1", countryId);
            mobileOperators = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        if (mobileOperators.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return mobileOperators;
    }

    /* Retorna 3 listas:
     * 1 todos TopUps activos.
     * 2 lista con cada uno de los top up por operadora y denominacion que pertencen a un proveedor.
     * 3 los top up repetidos por operadora y denominacion pero con un proveedor distinto.
     */
    public List<List<TopUpProduct>> getSortedTopUpProducts() throws GeneralException, NullParameterException, EmptyListException {
        List<TopUpProduct> entireTopUpProducts = new ArrayList<TopUpProduct>();
        List<TopUpProduct> filteredTopUpProducts = new ArrayList<TopUpProduct>();
        List<TopUpProduct> repeatedTopUpProducts = new ArrayList<TopUpProduct>();
        List<List<TopUpProduct>> list = new ArrayList<List<TopUpProduct>>();

        Query query = null;
        try {
            query = createQuery("SELECT tup FROM TopUpProduct tup WHERE tup.enabled=TRUE ORDER BY tup.mobileOperator.id, tup.productDenomination.id, tup.provider.id");
            entireTopUpProducts = query.setHint("toplink.refresh", "true").getResultList();
            //System.out.println("topUpProducts.size: " + entireTopUpProducts.size());
            String key = "";
            for (TopUpProduct tup : entireTopUpProducts) {
                String auxKey = tup.getMobileOperator().getId() + "-" + tup.getProductDenomination().getId();
                if (!key.equals(auxKey)) {

                    filteredTopUpProducts.add(tup);
                    key = auxKey;
                } else {
                    repeatedTopUpProducts.add(tup);
                    //System.out.println("auxKey: " + auxKey);
                }
            }
            list.add(entireTopUpProducts);
            list.add(filteredTopUpProducts);
            list.add(repeatedTopUpProducts);
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        if (filteredTopUpProducts.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return list;
    }

    public List<TopUpProduct> getTopUpProductByMobileOperatorId(Long mobileOperatorId, boolean getBestChoice) throws NullParameterException, EmptyListException, GeneralException {

        if (mobileOperatorId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "mobileOperatorId"), null);
        }
        List<TopUpProduct> topUpProducts = new ArrayList<TopUpProduct>();
        StringBuilder sqlBuilder = new StringBuilder();
        if (getBestChoice) {
            sqlBuilder.append("SELECT tup1 FROM TopUpProduct tup1 WHERE tup1.provider.enabled= 1 AND tup1.mobileOperator.id = ?1 AND tup1.enabled = 1").append(" AND tup1.commissionPercent = (SELECT MAX(tup2.commissionPercent) FROM TopUpProduct tup2 WHERE tup2.mobileOperator.id = ?1").append(" AND tup2.enabled = 1  AND tup1.productDenomination.id = tup2.productDenomination.id) ORDER BY tup1.productDenomination.amount");
        } else {
            sqlBuilder.append("SELECT tup FROM TopUpProduct tup WHERE tup.provider.enabled=1 AND tup.mobileOperator.id = ?1 ORDER BY tup.productDenomination.amount");
        }
        Query query = null;
        try {
            query = createQuery(sqlBuilder.toString());
            query.setParameter("1", mobileOperatorId);
            topUpProducts = query.setHint("toplink.refresh", "true").getResultList();
            topUpProducts.size();

        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }

        if (topUpProducts.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return topUpProducts;
    }

    public List<TopUpProduct> getTopUpProductByProviderId(Long providerId) throws NullParameterException, EmptyListException, GeneralException {
        if (providerId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "providerId"), null);
        }
        List<TopUpProduct> topUpProducts = new ArrayList<TopUpProduct>();


        try {
            Query query = createQuery("SELECT tup FROM TopUpProduct tup WHERE tup.provider.id = ?1 AND tup.enabled = TRUE");
            query.setParameter("1", providerId);
            topUpProducts = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        if (topUpProducts.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return topUpProducts;
    }

    public List<TopUpProduct> getTopUpProducts(EJBRequest request) throws GeneralException, RegisterNotFoundException, NullParameterException, EmptyListException {
        return (List<TopUpProduct>) listEntities(TopUpProduct.class, request, logger, getMethodName());
    }

    public List<TopUpProduct> getTopUpProductsEnabled() throws GeneralException, EmptyListException {
         List<TopUpProduct> topUpProducts = new ArrayList<TopUpProduct>();
        try {
            Query query = createQuery("SELECT tup FROM TopUpProduct tup WHERE tup.enabled = TRUE");
            topUpProducts = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        if (topUpProducts.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return topUpProducts;
    }

    public List<Provider> getTopUpProvider() throws GeneralException, EmptyListException {
        List<Provider> providers = null;
        try {
            providers = createQuery("SELECT p FROM Provider p").setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        if (providers.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return providers;
    }

    public List<Provider> getTopUpProviders(EJBRequest request) throws GeneralException, NullParameterException, EmptyListException {

        List<Provider> providers = new ArrayList<Provider>();

        try {
            Query query = createQuery("SELECT DISTINCT tup.provider FROM TopUpProduct tup");
            providers = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        if (providers.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return providers;
    }

    public MobileOperator loadMobileOperatorsById(Long id) throws NullParameterException, EmptyListException, GeneralException {
        if (id == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "mobileOperatorId"), null);
        }
        MobileOperator mobileOperator = new MobileOperator();
        //String sql = "SELECT m FROM MobileOperator m WHERE m.id = ?1";
        System.out.println(" ------------ id" + id);
        try {
            Query query = createQuery("SELECT m FROM MobileOperator m WHERE m.id = ?1");
            query.setParameter("1", id);
            mobileOperator = (MobileOperator) query.getSingleResult();
        }  catch (NoResultException ex) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        return mobileOperator;
    }

    public TopUpProduct loadTopUpProductById(EJBRequest request) throws GeneralException, RegisterNotFoundException, NullParameterException {

        return (TopUpProduct) loadEntity(TopUpProduct.class, request, logger, getMethodName());
    }

    public MobileOperator saveMobileOperator(EJBRequest request) throws GeneralException, NullParameterException {
        return (MobileOperator) saveEntity(request, logger, getMethodName());
    }

//    public Payment savePayment(EJBRequest request) throws GeneralException, NullParameterException {
//        return (Payment) saveEntity(request, logger, getMethodName());
//    }

    public TopUpProduct saveTopUpProduct(EJBRequest request) throws GeneralException, NullParameterException {
        return (TopUpProduct) saveEntity(request, logger, getMethodName());
    }

    private static String substringBetween(String source, String beginValue, String endValue) throws SubstringBetweenException {

        int init = source.indexOf(beginValue);
        if (init == -1) {
            throw new SubstringBetweenException("error: beginValue not found." + source + " beginValue: " + beginValue);
            //return "error: beginValue not found.";
        }
        init += beginValue.length();
        int end = source.indexOf(endValue);
        if (end == -1) {
            throw new SubstringBetweenException("error : endValue not found." + source + " endValue: " + endValue);
            //return "error : endValue not found.";
        }
        //end += endValue.length();
        String stringCut = source.substring(init, end);
        return stringCut;
    }
    ///Consulta los topUps disponibles de TransferTo y los guarda en la tabla update_transfer_to_product
    
    public void processTransferTopUps2()throws GeneralException, 
            NullParameterException, IOException {
        try {
            String fileCSV = "/tmp/pricelist_open_range_alodigaor.csv";
            String line = "";
            String cvsSplitBy = ",";
            List<UpdateTransferToProduct> uttps = new ArrayList<UpdateTransferToProduct>();
            BufferedReader br = new BufferedReader(new FileReader(fileCSV));
            while ((line = br.readLine()) != null) {
                String[] row = line.split(cvsSplitBy);
                
                if (row[0].equals("16501"))
                    System.out.println("*****************ESPERAAAAAA*****************");
                String countryName = row[1];
                String operatorName = row[2];
                Long operatorId = Long.valueOf(row[3]);
                String openRange = row[4];
                Float commissionPercent = Float.valueOf(row[5].replace("%", ""));
                String localCurrencyAmount = row[6];
                Float minimumAmount = Float.valueOf(row[6]);
                Float maximumAmount = Float.valueOf(row[7]);
                String countryCurrency = row[9];
                System.out.println(countryName + "," +
                        operatorName + "," +
                        operatorId + "," +
                        openRange + "," +
                        commissionPercent + "," +
                        localCurrencyAmount + "," +
                        minimumAmount + "," +
                        maximumAmount + "," +
                        countryCurrency + ",");
                if (openRange.equals("Open Range")) {
                    System.out.println("el operdor:" + operatorId + " es open range");
                    Float init = 5f;
                    int j = 0;
                    while (init <= maximumAmount) {
                        System.out.println("guardar la denominacion" + init 
                                + " para el operador:" + operatorId);
                        uttps.add(new UpdateTransferToProduct(null, countryName, 
                                null, operatorName, operatorId, countryCurrency, 
                                String.valueOf(init), null, null, commissionPercent, 
                                null, null));
                        j++;
                        if (init==5f)
                            init=0f;
                        init = init + 10;
                    }
                }else {
                    uttps.add(new UpdateTransferToProduct(null, countryName, null, 
                            operatorName, operatorId, countryCurrency, 
                            localCurrencyAmount, Float.parseFloat(localCurrencyAmount), 
                            null, commissionPercent, null, null));
                }
                System.out.println();
            }
            for (UpdateTransferToProduct uttp : uttps) {
                this.saveUpdateTransferToProduct(uttp);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (GeneralException ex) {
            ex.printStackTrace();
            throw new GeneralException(ex.getMessage());
        } catch (NullParameterException ex) {
            throw new NullParameterException(ex.getMessage());
        }
    }

    public void processTransferTopUps() throws GeneralException {
        logger.info("[PROCESS] processTransferTopUps");
        try {
            List<UpdateTransferToProduct> updateTransferToProducts = new ArrayList<UpdateTransferToProduct>();
            String response = RequestManager.getTopUpCountries();

            String[] countryIds = substringBetween(response, "<countryid>", "</countryid>").split(",");
            String[] countryNames = substringBetween(response, "<country>", "</country>").split(",");
            System.out.println(" Begin processTransferTopUps " + Calendar.getInstance().getTime());

            //for (int c = 0; c < countryIds.length; c++) {
            //for (int c = 0; c < 10; c++) {
            for (int c = 0; c < countryIds.length; c++) {

                String countryId = countryIds[c];
                String countryName = countryNames[c];
                String operators = RequestManager.getTopUpOperators(Long.parseLong(countryId));
                String[] operatorIds = substringBetween(operators, "<operatorid>", "</operatorid>").split(",");
                String[] operatorNames = substringBetween(operators, "<operator>", "</operator>").split(",");
                System.out.println("Progress: " + c + " of " + countryIds.length);
                for (int o = 0; o < operatorIds.length; o++) {
                    String operatorId = operatorIds[o];
                    String operatorName = operatorNames[o];
                    String products = RequestManager.getTopUpProducts(Long.parseLong(operatorIds[o]));
                    String[] productIds = null;
                    String[] productRetailsPrices = null;
                    String[] productWholesalePrices = null;
                    String productCurrency = null;
                    String openRange = "0";
                    try {
                        openRange = substringBetween(products, "<open_range>", "</open_range>");
                    } catch (Exception e) {
                    }
                    if (openRange.equals("1")) {
                        System.out.println("el operdor:"+operatorId+" es open range");
                        Float minimumAmount = Float.parseFloat(substringBetween(products, "<open_range_minimum_amount_requested_currency>", "</open_range_minimum_amount_requested_currency>"));
                        Float maximumAmount = Float.parseFloat(substringBetween(products, "<open_range_maximum_amount_requested_currency>", "</open_range_maximum_amount_requested_currency>"));
                        productCurrency = substringBetween(products, "<open_range_requested_currency>", "</open_range_requested_currency>");
                        Float init = minimumAmount + 5;
                        int index =(int) (maximumAmount / 5);
                        productIds = new String[index];
                        int i=0;
                        while (init<=maximumAmount) {
                            System.out.println("guardar la denominacion"+init+" para el operador:"+operatorId);
                            productIds[i] =String.valueOf(init);
                            i++;
                            init= init+5;
                        }
                        productRetailsPrices = productIds;
                        productWholesalePrices = productIds;
                    } else {
                        try {
                            productIds = substringBetween(products, "<product_list>", "</product_list>").split(",");
                            productRetailsPrices = substringBetween(products, "<retail_price_list>", "</retail_price_list>").split(",");
                            productWholesalePrices = substringBetween(products, "<wholesale_price_list>", "</wholesale_price_list>").split(",");
                            productCurrency = substringBetween(products, "<destination_currency>", "</destination_currency>");
                        } catch (SubstringBetweenException sbe) {
                            sbe.printStackTrace();
                            continue;
                        }
                    }
                    for (int p = 0; p < productIds.length; p++) {
                        System.out.println("productIds" + productIds[p]);
                        System.out.println("productRetailsPrices" + productRetailsPrices[p]);
                        System.out.println("productWholesalePrices" + productWholesalePrices[p]);
                        if (productIds[p] != null) {
                            UpdateTransferToProduct toProduct = new UpdateTransferToProduct(null, countryName, Long.parseLong(countryId), operatorName, Long.parseLong(operatorId), productCurrency, productIds[p], Float.valueOf(productRetailsPrices[p]), Float.valueOf(productWholesalePrices[p]), null, null, null);
                            updateTransferToProducts.add(toProduct);
                        }
                    }

                    EntityTransaction transaction = entityManager.getTransaction();
                    transaction.begin();
                    for (int i = 0; i < updateTransferToProducts.size(); i++) {

                        boolean isOK = ((i % 100) == 0 || i == updateTransferToProducts.size() - 1);
                        //Permite hacer commit cada 100 registros
                        entityManager.persist(updateTransferToProducts.get(i));

                        if (isOK) {
                            transaction.commit();
                            if (!(i == updateTransferToProducts.size() - 1)) {
                                transaction = entityManager.getTransaction();
                                transaction.begin();
                            }
                        }
                    }

                }
            }
            System.out.println("End processTransferTopUps " + Calendar.getInstance().getTime());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public UpdateTransferToProduct saveUpdateTransferToProduct(UpdateTransferToProduct updateTransferToProduct) throws GeneralException, NullParameterException {
        return (UpdateTransferToProduct) saveEntity(updateTransferToProduct);
    }
    //Borra toda la información de update_transfer_to_product

    public void deleteUpdateTransferToProducts() throws GeneralException {
        logger.info("[PROCESS] deleteUpdateTransferToProducts");
        try {

            EntityTransaction transaction = entityManager.getTransaction();
            transaction.begin();
            entityManager.createNativeQuery("TRUNCATE TABLE update_transfer_to_product").executeUpdate();
            transaction.commit();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }

    }
    ///Consulta las denominaciones de los nuevos topUp de TransferTo que no existen en la base de datos de distribució y los guarda.

    public void processTransferToDenominations() throws GeneralException {
        System.out.println("processTransferToDenominations");
        logger.info("[PROCESS] processTransferToDenominations");
        List denominations = null;
        try {
            StringBuilder sqlBuilder = new StringBuilder("SELECT DISTINCT(utp.retailPrice) FROM services.update_transfer_to_product utp WHERE utp.retailPrice NOT IN (SELECT p.amount FROM services.product_denomination p WHERE p.productId = ?1)");
            Query query = entityManager.createNativeQuery(sqlBuilder.toString());
            query.setParameter("1", Product.TOP_UP_PRODUCT_ID);
            denominations = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        System.out.println("cantidad de denominaciones"+denominations.size());
        if (denominations != null && denominations.size() > 0) {
            EntityTransaction transaction = entityManager.getTransaction();
            transaction.begin();
            for (int i = 0; i < denominations.size(); i++) {
                Float denominationValue = (Float) ((List) denominations.get(i)).get(0); //Glassfish 2.1
                //Float denominationValue = Float.parseFloat(denominations.get(i).toString());//Glassfish 3.2.1
                ProductDenomination pDenomination = new ProductDenomination();
                pDenomination.setAmount(denominationValue);
                Product product = new Product();
                product.setId(Product.TOP_UP_PRODUCT_ID);
                pDenomination.setProduct(product);
                pDenomination.setCurrency(new Currency(Currency.DOLLAR));
                entityManager.persist(pDenomination);
            }
            transaction.commit();
        }
    }
    ///Deshabilita los topUps que no estan disponibles en distribución en relación a los disponibles en TransferTo.

    public void disableUnAvailableTransferToTopUps() throws GeneralException {
        System.out.println("disableUnAvailableTransferToTopUps");
        logger.info("[PROCESS] disableUnAvailableTransferToTopUps");
        List mobileOperatorIds = null;
        try {
            String sql1 = "SELECT DISTINCT(m.mobileOperatorId) FROM services.mobile_operator_has_provider m WHERE m.providerId = ?1 AND m.referenceCode IN (SELECT DISTINCT (utp.transferToOperatorId) FROM services.update_transfer_to_product utp)";
            Query query = entityManager.createNativeQuery(sql1);
            query.setParameter("1", Provider.TRANSFER_TO);
            mobileOperatorIds = query.setHint("toplink.refresh", "true").getResultList();
            if (mobileOperatorIds != null && mobileOperatorIds.size() > 0) {
                StringBuilder ids = new StringBuilder("");
                for (int i = 0; i < mobileOperatorIds.size(); i++) {
                    String id = ((List) mobileOperatorIds.get(i)).get(0).toString(); //Glassfish 2.1
                    //String id = (mobileOperatorIds.get(i)).toString();//Glassfish 3.2.1
                    ids.append(id).append(",");
                }
                ids.deleteCharAt(ids.length() - 1);

                try {
                    StringBuilder sqlBuilder1 = new StringBuilder("UPDATE services.top_up_product t SET t.enabled = 0 WHERE t.providerId = ?1 AND t.mobileOperatorId NOT IN (SELECT m.mobileOperatorId FROM services.mobile_operator_has_provider m WHERE m.providerId = ?1 AND m.referenceCode IN (" + ids + "))");

                    EntityTransaction transaction = entityManager.getTransaction();
                    transaction.begin();
                    Query query1 = entityManager.createNativeQuery(sqlBuilder1.toString());
                    query1.setParameter("1", Provider.TRANSFER_TO);
                    query1.executeUpdate();
                    transaction.commit();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
                }

            }
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }

    }

    //Actualiza la información necesaria como porcentaje de comisión denominación y operadoras.
    public void processTransferToTopUpInformation() throws GeneralException {
        System.out.println("processTransferToTopUpInformation");
        logger.info("[PROCESS] processTransferToTopUpInformation");
        try {
//            StringBuilder sqlBuilder1 = new StringBuilder("UPDATE services.update_transfer_to_product SET commissionPercent = 100 - ((wholesalePrice * 100) / retailPrice)"); Comentado por Yamelis ya que se agrego directo en base de datos
            StringBuilder sqlBuilder2 = new StringBuilder("UPDATE services.update_transfer_to_product t, services.product_denomination p SET t.denominationId = p.id WHERE t.retailPrice = p.amount AND p.productId = ?1");
            StringBuilder sqlBuilder3 = new StringBuilder("UPDATE services.update_transfer_to_product t, services.mobile_operator_has_provider m SET t.mobileOperatorId = m.mobileOperatorId WHERE t.transferToOperatorId = m.referenceCode AND m.providerId = ?1");

            EntityTransaction transaction = entityManager.getTransaction();
            transaction.begin();
//            Query query1 = entityManager.createNativeQuery(sqlBuilder1.toString()); Comentado por Yamelis
            Query query2 = entityManager.createNativeQuery(sqlBuilder2.toString());
            Query query3 = entityManager.createNativeQuery(sqlBuilder3.toString());
            query2.setParameter("1", Product.TOP_UP_PRODUCT_ID);
            query3.setParameter("1", Provider.TRANSFER_TO);
//            query1.executeUpdate();Comentado por Yamelis
            query2.executeUpdate();
            query3.executeUpdate();
            transaction.commit();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
    }
    
    public void processTransferToMobileOperators2() throws GeneralException {
        System.out.println("processTransferToMobileOperators");
        logger.info("[PROCESS] processTransferToMobileOperators");
        List result = new ArrayList();
        try {
            StringBuilder sqlBuilder = new StringBuilder("SELECT utp.* FROM services.update_transfer_to_product utp WHERE utp.transferToOperatorId NOT IN (SELECT m.referenceCode FROM services.mobile_operator_has_provider m WHERE m.providerId = ?1) GROUP BY utp.operatorName ORDER BY utp.countryName;");
            Query query = entityManager.createNativeQuery(sqlBuilder.toString());
            query.setParameter("1", Provider.TRANSFER_TO);
            result = (List) query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        Provider provider = null;
        try {
            provider = productEJB.loadProvider(new EJBRequest(new Long(Provider.TRANSFER_TO)));
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        if (result != null && result.size() > 0) {
            for (int i = 0; i < result.size(); i++) {
                List object = (List) result.get(i);//Glassfish 2.1
//                Object[] object = (Object[]) result.get(i);//Glassfish 3.2.1
                String operatorName = object.get(3).toString().toUpperCase();//Glassfish 2.1
                //String operatorName = object[3].toString().toUpperCase();//Glassfish 3.2.1
                String referenceCode = object.get(4).toString();//Glassfish 2.1
                //String referenceCode = object[4].toString();//Glassfish 3.2.1
                try {
                    MobileOperatorHasProvider mobileHasProvider = null;
                    MobileOperator mobileOperator = this.loadMobileOperatorByName(operatorName);
                    System.out.println("encontro operatorName"+operatorName);
                    try {
                        mobileHasProvider = this.loadMobileOperatorHasProvider(mobileOperator.getId(), provider.getId());
                    } catch (RegisterNotFoundException rne) {
                        System.out.println("no encontro mobileOperatorHasProvider para"+operatorName);
                        mobileHasProvider = new MobileOperatorHasProvider();
                        mobileHasProvider.setMobileOperator(mobileOperator);
                        mobileHasProvider.setProvider(provider);
                    }
                    //En caso de existir el MobileOperatorHasProvider solo actualizamos el referenceCode;
                    mobileHasProvider.setReferenceCode(referenceCode);
                    this.saveMobileOperatorHasProvider(mobileHasProvider);
                    System.out.println("guardo mobileOperatorHasProvider para"+operatorName);
                } catch (RegisterNotFoundException ex) {
                    System.out.println("no encontro operatorName"+operatorName);
                    String sql = "SELECT c FROM Country c where c.name = '" + object.get(1).toString().toUpperCase() + "'";
                    Country country = null;
                    country = (Country) entityManager.createQuery(sql).setHint("toplink.refresh", "true").getSingleResult();
                    MobileOperator mobileOperator = new MobileOperator();
                    mobileOperator.setName(operatorName);
                    mobileOperator.setAlternativeName1(operatorName);
                    mobileOperator.setCountry(country);
                    mobileOperator.setEnabled(Boolean.TRUE);
                    MobileOperatorHasProvider operatorHasProvider = new MobileOperatorHasProvider();
                    operatorHasProvider.setMobileOperator(mobileOperator);
                    operatorHasProvider.setProvider(provider);
                    operatorHasProvider.setReferenceCode(object.get(4).toString());
                    EJBRequest reques = new EJBRequest();
                    reques.setParam(mobileOperator);
                    EJBRequest request2 = new EJBRequest();
                    request2.setParam(operatorHasProvider);
                    try {
                        this.saveMobileOperator(reques);
                        this.saveMobileOperatorHasProvider(operatorHasProvider);
                    } catch (NullParameterException ex1) {
                        java.util.logging.Logger.getLogger(TopUpProductEJBImpl.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                } catch (Exception ex) {
                    throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
                }
            }
        }
    }
    
    public Country saveCountry(Country country) throws GeneralException, NullParameterException {
        EJBRequest request = new EJBRequest();
        request.setParam(country);
        return (Country) saveEntity(request, logger, getMethodName());
    }

    public List<MobileOperatorHasProvider> processTransferToMobileOperators() throws GeneralException {
        System.out.println("processTransferToMobileOperators");
        logger.info("[PROCESS] processTransferToMobileOperators");
        List result = new ArrayList();
        List<MobileOperatorHasProvider> operatorHasProviders = new ArrayList<MobileOperatorHasProvider>();
        try {
            StringBuilder sqlBuilder = new StringBuilder("SELECT utp.* FROM services.update_transfer_to_product utp WHERE utp.transferToOperatorId NOT IN (SELECT m.referenceCode FROM services.mobile_operator_has_provider m WHERE m.providerId = ?1) GROUP BY utp.operatorName ORDER BY utp.countryName;");
            Query query = entityManager.createNativeQuery(sqlBuilder.toString());
            query.setParameter("1", Provider.TRANSFER_TO);
            result = (List) query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        Provider provider = null;
        try {
            provider = productEJB.loadProvider(new EJBRequest(new Long(Provider.TRANSFER_TO)));
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }

        if (result != null && result.size() > 0) {

            for (int i = 0; i < result.size(); i++) {
                List object = (List) result.get(i);//Glassfish 2.1
//                Object[] object = (Object[]) result.get(i);//Glassfish 3.2.1
                String operatorName = object.get(3).toString().toUpperCase();//Glassfish 2.1
                //String operatorName = object[3].toString().toUpperCase();//Glassfish 3.2.1
                String referenceCode = object.get(4).toString();//Glassfish 2.1
                //String referenceCode = object[4].toString();//Glassfish 3.2.1
                try {
                    MobileOperatorHasProvider mobileHasProvider = null;
                    MobileOperator mobileOperator = this.loadMobileOperatorByName(operatorName);
                    System.out.println("encontro operatorName"+operatorName);
                    try {
                        mobileHasProvider = this.loadMobileOperatorHasProvider(mobileOperator.getId(), provider.getId());
                    } catch (RegisterNotFoundException rne) {
                        System.out.println("no encontro mobileOperatorHasProvider para"+operatorName);
                        mobileHasProvider = new MobileOperatorHasProvider();
                        mobileHasProvider.setMobileOperator(mobileOperator);
                        mobileHasProvider.setProvider(provider);
                    }
                    //En caso de existir el MobileOperatorHasProvider solo actualizamos el referenceCode;
                    mobileHasProvider.setReferenceCode(referenceCode);
                    this.saveMobileOperatorHasProvider(mobileHasProvider);
                    System.out.println("guardo mobileOperatorHasProvider para"+operatorName);
                } catch (RegisterNotFoundException ex) {
                    provider.setName("Transfer To");
                    System.out.println("no encontro operatorName"+operatorName);
                    MobileOperator mobileOperator = new MobileOperator();
                    mobileOperator.setName(operatorName);
                    MobileOperatorHasProvider operatorHasProvider = new MobileOperatorHasProvider();
                    operatorHasProvider.setMobileOperator(mobileOperator);
                    operatorHasProvider.setProvider(provider);
                    operatorHasProviders.add(operatorHasProvider);
                } catch (Exception ex) {
                    throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
                }
            }
        }
        return operatorHasProviders;
    }
    
    public List<CountryHasProvider> processTransferToCountries() throws GeneralException {
        System.out.println("processTransferToCountries");
        logger.info("[PROCESS] processTransferToCountries");
        List result = new ArrayList();
        List<CountryHasProvider> pendingCountries = new ArrayList<CountryHasProvider>();
        try {
            StringBuilder sqlBuilder = new StringBuilder("SELECT utp.* FROM services.update_transfer_to_product utp WHERE utp.transferToCountryId NOT IN (SELECT c.referenceCode FROM services.country_has_provider c WHERE c.providerId = ?1) GROUP BY utp.countryName");
            Query query = entityManager.createNativeQuery(sqlBuilder.toString());
            query.setParameter("1", Provider.TRANSFER_TO);
            result = (List) query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }

        Provider provider = null;
        try {
            provider = productEJB.loadProvider(new EJBRequest(new Long(Provider.TRANSFER_TO)));
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        if (result != null && result.size() > 0) {
            for (int i = 0; i < result.size(); i++) {
                List object = (List) result.get(i);//Glassfish 2.1
                //Object[] object = (Object[]) result.get(i);//Glassfish 3.2.1

                String countryName = object.get(1).toString().toUpperCase();//Glassfish 2.1
                String referenceCode = object.get(2).toString();//Glassfish 2.1
                //String countryName = object[1].toString();//Glassfish 3.2.1
                //String referenceCode = object[2].toString();//Glassfish 3.2.1

                try {
                    Country country = utilsEJB.loadCountryByName(countryName);
                    CountryHasProvider countryHasProvider = new CountryHasProvider();
                    countryHasProvider.setCountry(country);

                    countryHasProvider.setProvider(provider);
                    countryHasProvider.setReferenceCode(referenceCode);
                    utilsEJB.saveCountryHasProvider(countryHasProvider);

                } catch (RegisterNotFoundException ex) {
                    provider.setName("Transfer To");
                    Country country = new Country();
                    country.setName(countryName);
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

    public void processTransferToCountries2() throws GeneralException {
        System.out.println("processTransferToCountries");
        logger.info("[PROCESS] processTransferToCountries");
        List result = new ArrayList();
        List<CountryHasProvider> pendingCountries = new ArrayList<CountryHasProvider>();
        try {
            StringBuilder sqlBuilder = new StringBuilder("SELECT utp.* FROM services.update_transfer_to_product utp WHERE utp.transferToCountryId NOT IN (SELECT c.referenceCode FROM services.country_has_provider c WHERE c.providerId = ?1) GROUP BY utp.countryName");
            Query query = entityManager.createNativeQuery(sqlBuilder.toString());
            query.setParameter("1", Provider.TRANSFER_TO);
            result = (List) query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }

        Provider provider = null;
        try {
            provider = productEJB.loadProvider(new EJBRequest(new Long(Provider.TRANSFER_TO)));
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        if (result != null && result.size() > 0) {
            for (int i = 0; i < result.size(); i++) {
                List object = (List) result.get(i);//Glassfish 2.1
                //Object[] object = (Object[]) result.get(i);//Glassfish 3.2.1

                String countryName = object.get(1).toString().toUpperCase();//Glassfish 2.1
                String referenceCode = object.get(2).toString();//Glassfish 2.1
                //String countryName = object[1].toString();//Glassfish 3.2.1
                //String referenceCode = object[2].toString();//Glassfish 3.2.1

                try {
                    Country country = utilsEJB.loadCountryByName(countryName);
                    CountryHasProvider countryHasProvider = new CountryHasProvider();
                    countryHasProvider.setCountry(country);

                    countryHasProvider.setProvider(provider);
                    countryHasProvider.setReferenceCode(referenceCode);
                    utilsEJB.saveCountryHasProvider(countryHasProvider);

                } catch (RegisterNotFoundException ex) {
                    provider.setName("Transfer To");
                    Country country = new Country();
                    country.setName(countryName);
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
    }

    private MobileOperator loadMobileOperatorByName(String name) throws RegisterNotFoundException, NullParameterException, GeneralException {
        logger.info("[PROCESS] loadMobileOperatorByName");
        MobileOperator mobileOperator = new MobileOperator();
        try {
            if (name == null) {
                throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "name"), null);
            }
            System.out.println("name**************"+name);
            StringBuilder sqlBuilder = new StringBuilder("SELECT mo FROM MobileOperator mo ");
            sqlBuilder.append("WHERE mo.alternativeName1 = '").append(name).append("'").append(" OR mo.name = '").append(name).append("'").append(" OR mo.alternativeName2 = '").append(name).append("'").append(" OR mo.alternativeName3 = '").append(name).append("'");
            mobileOperator = (MobileOperator) entityManager.createQuery(sqlBuilder.toString()).setHint("toplink.refresh", "true").getSingleResult();

        } catch (NoResultException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, MobileOperator.class.getSimpleName(), getMethodName(), MobileOperator.class.getSimpleName(), null), ex);
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }

        return mobileOperator;
    }

    public MobileOperatorHasProvider saveMobileOperatorHasProvider(MobileOperatorHasProvider mobileOperatorHasProvider) throws NullParameterException, GeneralException {
        if (mobileOperatorHasProvider == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "countryHasProvider"), null);
        }
        return (MobileOperatorHasProvider) saveEntity(mobileOperatorHasProvider);
    }

    private void sendPendingDataMail(Enterprise enterprise, List<MobileOperatorHasProvider> mobileOperatorHasProviders, List<CountryHasProvider> countryHasProviders, ArrayList<String> recipients, String processName) throws GeneralException {
        System.out.println("sendPendingDataMail");
        logger.info("[PROCESS] sendPendingDataMail");
        try {
            Mail mail = ServiceMails.getPendingDataMail(enterprise, mobileOperatorHasProviders, countryHasProviders, recipients, processName);
            //utilsEJB.sendMail(mail);
            (new com.alodiga.services.provider.commons.utils.MailSender(mail)).start();
        } catch (Exception ex) {
            throw new GeneralException(ex.getMessage());
        }
    }

    //Envía notificación de que se deben actualizar las comisiones
    public void sendTopUpNotificationMail(Enterprise enterprise, ArrayList<String> recipients, String providerName) throws GeneralException {
        System.out.println("sendTopUpNotificationMail..............");
        logger.info("[PROCESS] sendTopUpNotificationMail");
        try {

            Mail mail = ServiceMails.getUpdateNotificationMail(enterprise, recipients, providerName, "Top Up");
            //utilsEJB.sendMail(mail);
            (new com.alodiga.services.provider.commons.utils.MailSender(mail)).start();
        } catch (Exception ex) {
            throw new GeneralException(ex.getMessage());
        }
    }

    public void disableDuplicatedTransferToTopUps() throws GeneralException {
        System.out.println("disableDuplicatedTransferToTopUps");
        logger.info("[PROCESS] disableDuplicatedTransferToTopUps");
        try {
            StringBuilder sqlBuilder1 = new StringBuilder("UPDATE TopUpProduct tp SET tp.enabled = FALSE  WHERE  tp.id IN (SELECT tup.id  FROM TopUpProduct tup, UpdateTransferToProduct ut WHERE  tup.mobileOperator.id = ut.mobileOperatorId AND  tup.productDenomination.id = ut.denominationId AND tup.providerId = ?1 AND tup.enabled = TRUE)");
            EntityTransaction transaction = entityManager.getTransaction();
            transaction.begin();
            Query query1 = entityManager.createQuery(sqlBuilder1.toString());
            query1.setParameter("1", Provider.TRANSFER_TO);
            query1.executeUpdate();
            transaction.commit();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
    }

    public List<UpdateTransferToProduct> getUpdateTransferToProducts() throws GeneralException, EmptyListException {
        System.out.println("getUpdateTransferToProducts");
        logger.info("[PROCESS] getUpdateTransferToProducts");
        List<UpdateTransferToProduct> updateTransferToProducts = new ArrayList<UpdateTransferToProduct>();
        try {
            StringBuilder sqlBuilder = new StringBuilder("SELECT utp FROM UpdateTransferToProduct utp");
            Query query = entityManager.createQuery(sqlBuilder.toString());
            updateTransferToProducts = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        if (updateTransferToProducts.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return updateTransferToProducts;
    }

    public void disableModifiedTransferToTopUps() throws GeneralException {
        System.out.println("disableModifiedTransferToTopUps");
        logger.info("[PROCESS] disableModifiedTransferToTopUps");
        try {
//            String sql1 = "UPDATE TopUpProduct tp SET tp.enabled = FALSE "
//                    + "WHERE tp.id IN (SELECT t.id FROM TopUpProduct t, UpdateTransferToProduct utp "
//                    + "WHERE t.mobileOperator.id= utp.mobileOperatorId AND t.enabled= TRUE "
//                    + "AND t.productDenomination.id = utp.denominationId AND t.provider.id = ?1 "
//                    + "AND t.commissionPercent <> utp.commissionPercent)";

//            String sql2 = "UPDATE TopUpProduct tp SET tp.enabled = FALSE "
//                    + "WHERE tp.enabled = TRUE AND tp.provider.id = " + Provider.TRANSFER_TO + " AND tp.id NOT IN (SELECT t.id FROM TopUpProduct t, UpdateTransferToProduct upnp "
//                    + "WHERE t.mobileOperator.id= upnp.mobileOperatorId AND t.enabled= TRUE "
//                    + "AND t.productDenomination.id = upnp.denominationId AND t.provider.id = " + Provider.TRANSFER_TO + " )";
            String sql = "UPDATE services.top_up_product AS t , ("
                    + "SELECT t1.id FROM services.top_up_product t1, services.update_transfer_to_product upnp " 
                    + "WHERE t1.enabled = 1 " 
                    + "AND t1.mobileOperatorId = upnp.mobileOperatorId " 
                    + "AND t1.productDenominationId = upnp.denominationId " 
                    + "AND t1.providerId = " + Provider.TRANSFER_TO
                    + ") AS t2 " 
                    + "SET t.enabled = 1 " 
                    + "WHERE t.enabled =0 AND t.providerId = " + Provider.TRANSFER_TO
                    + " AND t.id NOT IN (t2.id)";

            /*TODO: con cada perfil*/
//            entityManager.getTransaction().begin();
//            Query query = createQuery(sql1);
//            Query query2 = createQuery(sql2);
//            query.setParameter("1", Provider.TRANSFER_TO);
//            query2.setParameter("1", Provider.TRANSFER_TO);
//            query.executeUpdate();
//            query2.executeUpdate();\
//            createQuery(sql).executeUpdate();
            entityManager.getTransaction().begin();
            entityManager.createNativeQuery(sql).executeUpdate();
            entityManager.getTransaction().commit();
        } catch (Exception ex) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw new GeneralException(ex.getMessage());
        } finally {
        }

    }
    
    public void includeTransferToTopUps2() throws GeneralException {
        System.out.println("includeTransferToTopUps");
        logger.info("[PROCESS] includeTransferToTopUps");
        disableModifiedTransferToTopUps();
//        disableDuplicatedTransferToTopUps();
        Timestamp now = new Timestamp(new java.util.Date().getTime());
        try {
            List<TopUpProduct> topUpProducts = null;
            try {
                topUpProducts = getTopUpProductByProviderId(Long.parseLong("" + Provider.TRANSFER_TO));
            } catch (EmptyListException ele) {
                ele.printStackTrace();
            }

            Map<String, TopUpProduct> map = new HashMap<String, TopUpProduct>();
            if (topUpProducts != null) {
                for (TopUpProduct topUpProduct : topUpProducts) {
                    map.put(topUpProduct.getMobileOperator().getId() + "-" + topUpProduct.getProductDenomination().getId(), topUpProduct);
                }
            }
            Provider provider = productEJB.loadProvider(new EJBRequest(new Long(Provider.TRANSFER_TO)));
            List<UpdateTransferToProduct> updateTransferToProducts = getUpdateTransferToProducts();
            EntityTransaction transaction = entityManager.getTransaction();
            transaction.begin();

            for (int i = 0; i < updateTransferToProducts.size(); i++) {
                UpdateTransferToProduct uttp = updateTransferToProducts.get(i);
                String key = uttp.getMobileOperatorId() + "-" + uttp.getDenominationId();
                System.out.println("key " + key);
                if (map.containsKey(key)) {
                    TopUpProduct topUpProduct = map.get(key);
                    if (topUpProduct.getCommissionPercent() != uttp.getCommissionPercent()) {
                        //Si el TopUp Existe pero cambio el porcentaje de comision.
                        //Desactivo el TopUp que ya existe y Luego persisto uno nuevo.

                        String sql1 = "UPDATE TopUpProduct tup SET tup.commissionPercent = " 
                                + uttp.getCommissionPercent() + " WHERE tup.id= " + topUpProduct.getId();
                        createQuery(sql1).executeUpdate();
//                        TopUpProduct newTopUpProduct = new TopUpProduct();
//                        newTopUpProduct.setName(uttp.getOperatorName() + " " + uttp.getCountryCurrency() + " " + uttp.getLocalCurrencyAmount());
//                        newTopUpProduct.setDescription(topUpProduct.getName());
//                        newTopUpProduct.setMobileOperator(loadMobileOperatorsById(uttp.getMobileOperatorId()));
//                        newTopUpProduct.setProvider(provider);
//                        newTopUpProduct.setProductDenomination(productEJB.loadProductDenominationById(uttp.getDenominationId()));
//                        newTopUpProduct.setCommissionPercent(uttp.getCommissionPercent());
//                        newTopUpProduct.setReferenceCode(uttp.getLocalCurrencyAmount());
//                        newTopUpProduct.setCreationDate(now);
//                        newTopUpProduct.setEnabled(true);
//                        entityManager.persist(newTopUpProduct);

                    }
                } else {
                    //Si el TopUp no existe, persisto uno nuevo.
                    TopUpProduct topUpProduct = new TopUpProduct();
                    topUpProduct.setName(uttp.getOperatorName() + " " + uttp.getCountryCurrency() + " " + uttp.getLocalCurrencyAmount());
                    topUpProduct.setDescription(topUpProduct.getName());
                    System.out.println("uttp.getId() " + uttp.getId());
                    topUpProduct.setMobileOperator(loadMobileOperatorsById(uttp.getMobileOperatorId()));
                    topUpProduct.setProvider(provider);
                    topUpProduct.setProductDenomination(productEJB.loadProductDenominationById(uttp.getDenominationId()));
                    topUpProduct.setCommissionPercent(uttp.getCommissionPercent());
                    topUpProduct.setReferenceCode(uttp.getLocalCurrencyAmount());
                    topUpProduct.setCreationDate(now);
                    topUpProduct.setEnabled(true);
                    entityManager.persist(topUpProduct);
                }
                boolean isOK = ((i % 100) == 0 || i == updateTransferToProducts.size() - 1);
                if (isOK) {
                    transaction.commit();
                    if (!(i == updateTransferToProducts.size() - 1)) {
                        transaction = entityManager.getTransaction();
                        transaction.begin();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw new GeneralException(ex.getMessage());
        }
        System.out.println("includeTransferToTopUps DONE");
    }

    public void includeTransferToTopUps() throws GeneralException {
        System.out.println("includeTransferToTopUps");
        logger.info("[PROCESS] includeTransferToTopUps");
        disableModifiedTransferToTopUps();
        //disableDuplicatedTransferToTopUps();
        Timestamp now = new Timestamp(new java.util.Date().getTime());
        try {
            List<TopUpProduct> topUpProducts = null;
            try {
                topUpProducts = getTopUpProductByProviderId(Long.parseLong("" + Provider.TRANSFER_TO));
            } catch (EmptyListException ele) {
                ele.printStackTrace();
            }

            Map<String, TopUpProduct> map = new HashMap<String, TopUpProduct>();
            if (topUpProducts != null) {
                for (TopUpProduct topUpProduct : topUpProducts) {
                    map.put(topUpProduct.getMobileOperator().getId() + "-" + topUpProduct.getProductDenomination().getId(), topUpProduct);
                }
            }
            Provider provider = productEJB.loadProvider(new EJBRequest(new Long(Provider.TRANSFER_TO)));
            List<UpdateTransferToProduct> updateTransferToProducts = getUpdateTransferToProducts();
            EntityTransaction transaction = entityManager.getTransaction();
            transaction.begin();

            for (int i = 0; i < updateTransferToProducts.size(); i++) {
                UpdateTransferToProduct uttp = updateTransferToProducts.get(i);
                String key = uttp.getMobileOperatorId() + "-" + uttp.getDenominationId();
                System.out.println("key " + key);
                if (map.containsKey(key)) {
                    TopUpProduct topUpProduct = map.get(key);
                    if (topUpProduct.getCommissionPercent() != uttp.getCommissionPercent()) {
                        //Si el TopUp Existe pero cambio el porcentaje de comision.
                        //Desactivo el TopUp que ya existe y Luego persisto uno nuevo.

                        String sql1 = "UPDATE TopUpProduct tup SET tup.enabled = FALSE WHERE tup.id= " + topUpProduct.getId();
                        createQuery(sql1).executeUpdate();
                        TopUpProduct newTopUpProduct = new TopUpProduct();
                        newTopUpProduct.setName(uttp.getOperatorName() + " " + uttp.getCountryCurrency() + " " + uttp.getLocalCurrencyAmount());
                        newTopUpProduct.setDescription(topUpProduct.getName());
                        newTopUpProduct.setMobileOperator(loadMobileOperatorsById(uttp.getMobileOperatorId()));
                        newTopUpProduct.setProvider(provider);
                        newTopUpProduct.setProductDenomination(productEJB.loadProductDenominationById(uttp.getDenominationId()));
                        newTopUpProduct.setCommissionPercent(uttp.getCommissionPercent());
                        newTopUpProduct.setReferenceCode(uttp.getLocalCurrencyAmount());
                        newTopUpProduct.setCreationDate(now);
                        newTopUpProduct.setEnabled(true);
                        entityManager.persist(newTopUpProduct);

                    }
                } else {
                    //Si el TopUp no existe, persisto uno nuevo.
                    TopUpProduct topUpProduct = new TopUpProduct();
                    topUpProduct.setName(uttp.getOperatorName() + " " + uttp.getCountryCurrency() + " " + uttp.getLocalCurrencyAmount());
                    topUpProduct.setDescription(topUpProduct.getName());
                    System.out.println("uttp.getId() " + uttp.getId());
                    topUpProduct.setMobileOperator(loadMobileOperatorsById(uttp.getMobileOperatorId()));
                    topUpProduct.setProvider(provider);
                    topUpProduct.setProductDenomination(productEJB.loadProductDenominationById(uttp.getDenominationId()));
                    topUpProduct.setCommissionPercent(uttp.getCommissionPercent());
                    topUpProduct.setReferenceCode(uttp.getLocalCurrencyAmount());
                    topUpProduct.setCreationDate(now);
                    topUpProduct.setEnabled(true);
                    entityManager.persist(topUpProduct);
                }
                boolean isOK = ((i % 100) == 0 || i == updateTransferToProducts.size() - 1);
                if (isOK) {
                    transaction.commit();
                    if (!(i == updateTransferToProducts.size() - 1)) {
                        transaction = entityManager.getTransaction();
                        transaction.begin();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw new GeneralException(ex.getMessage());
        }
        System.out.println("includeTransferToTopUps DONE");
    }

    //Elimina los top-Ups que estan deshabilitados y que nunca han sido utilizados en una transacción.
    public void deleteDisabledTransferToProducts() throws GeneralException {
        System.out.println("deleteDisabledTransferToProducts");
        logger.info("[PROCESS] deleteDisabledTransferToProducts");
        try {
            EntityTransaction transaction = entityManager.getTransaction();
            transaction.begin();
            entityManager.createQuery("DELETE FROM TopUpProduct WHERE provider.id = " + Provider.TRANSFER_TO + " AND enabled = 0 AND id NOT IN (SELECT tp.id FROM TopUpProduct tp, Transaction t WHERE t.topUpProduct.id = tp.id)").executeUpdate();
            transaction.commit();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
    }
    
    public void executeTransferToTopUpUpdate2() throws GeneralException{
        System.out.println("executeTransferToTopUpUpdate STARTED..");
        logger.info("[PROCESS] executeTransferToTopUpUpdate");
        Enterprise enterprise = null;
        try {
            EJBRequest request = new EJBRequest();
            request.setParam(Enterprise.ALODIGA_USA);
            enterprise = utilsEJB.loadEnterprise(request);
            ArrayList<String> recipients = new ArrayList<String>();
            recipients.add(enterprise.getEmail());
            
        } catch (Exception ex) {
            sendUpdateTopUpErrorMail(enterprise, "Proceso de TopUp - TransaferTop", "Error General - en metodo executeTransferToTopUpUpdate()", ex);
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        System.out.println("executeTransferToTopUpUpdate DONE..");
    }

    public void executeTransferToTopUpUpdate() throws GeneralException {
        System.out.println("executeTransferToTopUpUpdate STARTED..");
        logger.info("[PROCESS] executeTransferToTopUpUpdate");
        Enterprise enterprise = null;
        try {
            EJBRequest request = new EJBRequest();
            request.setParam(Enterprise.ALODIGA_USA);
            enterprise = utilsEJB.loadEnterprise(request);
            ArrayList<String> recipients = new ArrayList<String>();
            recipients.add(enterprise.getEmail());

//            //1- Elimina el contenido de la tabla update_transfer_to_product.
//            this.deleteUpdateTransferToProducts();//
//            //2- Consulta los topUps disponibles de TransferTo y los guarda en la tabla update_transfer_to_product
//            this.processTransferTopUps();//
//            //3 - Consulta las denominaciones de los nuevos topUp de TransferTo que no existen en la base de datos de distribución y los guarda.
//            this.processTransferToDenominations();
//            //4 - Deshabilita los topUps que no estan disponibles en distribución en relación a los disponibles en TransferTo.
//            this.disableUnAvailableTransferToTopUps();
//
//            //5 - Verifica las Operadoras y Paises que no esten disponibles y que deben ser agregados manualmente.
//            //Si encuentra registros no disponibles envia SMS y correo electronico y detiene el proceso.
//            if (this.processTransferToMobileOperatorsAndCountries(enterprise, recipients)) {
//                return;
//            }
//            //4 - Deshabilita los topUps que no estan disponibles en distribución en relación a los disponibles en TransferTo.
//            this.disableUnAvailableTransferToTopUps();
//            //6 - Actualiza la información necesaria como porcentaje de comisión denominación y operadoras.
//            this.processTransferToTopUpInformation();
//            //7 - Se agregan los nuevos top-Ups y se desactivan los que tengan cambios en comisiones.
//            this.includeTransferToTopUps();
//            //8 - Elimina los top-Ups que estan deshabilitados y que nunca han sido utilizados en una transacción.
//            this.deleteDisabledTransferToProducts();
//            //9 -Se envia la notificación de la actualización de las comisiones por realizar.
//            this.sendTopUpNotificationMail(enterprise, recipients, "Transfer To");


            //1- Elimina el contenido de la tabla update_transfer_to_product.
            this.deleteUpdateTransferToProducts();
            //2- Consulta los topUps disponibles de TransferTo en el archivo CSV y los guarda en la tabla update_transfer_to_product
            this.processTransferTopUps2();
            //3 - Consulta las denominaciones de los nuevos topUp de TransferTo que no existen en la base de datos de distribución y los guarda.
            this.processTransferToDenominations();
            //4 - Deshabilita los topUps que no estan disponibles en distribución en relación a los disponibles en TransferTo.
            this.disableUnAvailableTransferToTopUps();
            //5 - Verifica las Operadoras que no esten disponibles y las agrega automaticamente.
            this.processTransferToMobileOperators2();
            //6 - Actualiza la información necesaria como porcentaje de comisión denominación y operadoras.
            this.processTransferToTopUpInformation();
            //7 - Se agregan los nuevos top-Ups y se desactivan los que tengan cambios en comisiones.
            this.includeTransferToTopUps2();
            //8 - Elimina los top-Ups que estan deshabilitados y que nunca han sido utilizados en una transacción.
            this.deleteDisabledTransferToProducts();
            //9 -Se envia la notificación de la actualización de las comisiones por realizar.
            this.sendTopUpNotificationMail(enterprise, recipients, "Transfer To");

        } catch (Exception ex) {
            sendUpdateTopUpErrorMail(enterprise, "Proceso de TopUp - TransaferTop", "Error General - en metodo executeTransferToTopUpUpdate()", ex);
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        System.out.println("executeTransferToTopUpUpdate DONE..");
    }
    /*
    public void includeCommissions() {
    try {
    List<TopUpProduct> topUpProducts = null;
    try {
    topUpProducts = getActiveTopUpProducts();
    } catch (EmptyListException ele) {
    ele.printStackTrace();
    }

    Map<String, Float> map = new HashMap<String, Float>();
    if (topUpProducts != null) {
    for (TopUpProduct topUpProduct : topUpProducts) {
    map.put(topUpProduct.getMobileOperator().getId() + "-" + topUpProduct.getProductDenomination().getId(), topUpProduct.getCommissionPercent());
    }
    }
    List<MobileOperatorHasDenominationHasProfile> newMohdhps = new ArrayList<MobileOperatorHasDenominationHasProfile>();
    //Map<String, Float> map = new HashMap<String, Float>();
    List<Profile> profiles = getProfiles();
    for (Profile profile : profiles) {
    List<MobileOperatorHasDenominationHasProfile> denominationHasProfiles = getMobileOperatorDenominationProfilesByProfileId(profile.getId());
    if (denominationHasProfiles != null && denominationHasProfiles.size() > 0) {
    for (MobileOperatorHasDenominationHasProfile mohdhp : denominationHasProfiles) {
    MobileOperatorHasDenominationHasProfile mohdhp1 = new MobileOperatorHasDenominationHasProfile();
    String key = mohdhp.getMobileOperator().getId() + "-" + mohdhp.getProductDenomination().getId();
    if (map.containsKey(key)) {
    TopUpProduct product = getHighestTopUpProduct(topUpProducts, mohdhp.getMobileOperator().getId(), mohdhp.getProductDenomination().getId());
    if(product.getCommissionPercent() != mohdhp.getCommission()){
    mohdhp1.setBeginningDate(null);
    //hdhp1.setCommission(commission);
    mohdhp1.setEndingDate(null);
    mohdhp1.setMobileOperator(null);
    mohdhp1.setProductDenomination(null);
    mohdhp1.setProfile(profile);
    newMohdhps.add(mohdhp);
    //Persisto nuevo y dehabilito el anterior...
    }

    } else {
    //persisto un mohdhp;
    }
    }

    }

    }


    } catch (Exception ele) {
    ele.printStackTrace();
    }
    }

    public TopUpProduct getHighestTopUpProduct(List<TopUpProduct> products, Long mobileOperatorId, Long denominationId) {
    //Devuelve el TopUpProduct que tenga mayor porcentaje de comisión.
    TopUpProduct maxTopUpProduct = null;
    float maxCommission = 0F;
    for (TopUpProduct topUpProduct : products) {
    if (topUpProduct.getMobileOperator().getId().equals(mobileOperatorId) && topUpProduct.getProductDenomination().getId().equals(denominationId)) {
    if (topUpProduct.getCommissionPercent() > maxCommission) {
    maxCommission = topUpProduct.getCommissionPercent();
    maxTopUpProduct = topUpProduct;
    }
    }
    }
    return maxTopUpProduct;
    }

    public List<TopUpProduct> getActiveTopUpProducts() throws GeneralException, RegisterNotFoundException, NullParameterException, EmptyListException {

    List<TopUpProduct> topUpProducts = new ArrayList<TopUpProduct>();
    try {
    Query query = createQuery("SELECT tup FROM TopUpProduct tup WHERE tup.enabled = TRUE");
    topUpProducts = query.setHint("toplink.refresh", "true").getResultList();
    } catch (Exception ex) {
    throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
    }
    if (topUpProducts.isEmpty()) {
    throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
    }
    return topUpProducts;
    }

    public List<Profile> getProfiles() throws GeneralException, EmptyListException {
    List<Profile> profiles = new ArrayList<Profile>();
    try {
    Query query = createQuery("SELECT p FROM Profile p WHERE p.enabled = TRUE AND p.isAdmin = FALSE");
    profiles = query.setHint("toplink.refresh", "true").getResultList();
    } catch (Exception ex) {
    throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
    }
    if (profiles.isEmpty()) {
    throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
    }
    return profiles;

    }*/

    public List<TopUpProduct> getTopUpProductsWithFormula() throws EmptyListException, GeneralException {
        List<TopUpProduct> topProducts = new ArrayList<TopUpProduct>();
        try {
            String sql = "SELECT t FROM TopUpProduct t WHERE t.enabled = TRUE AND t.commissionFormula IS NOT NULL";
            topProducts = createQuery(sql).setHint("toplink.refresh", "true").getResultList();
        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }

        if (topProducts.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return topProducts;

    }

    public TopUpCalculation saveTopUpCalculation(TopUpCalculation topUpCalculation) throws GeneralException, NullParameterException {
        if (topUpCalculation == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "topUpCalculation"), null);
        }
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            if (topUpCalculation.getId() == null) {
                entityManager.persist(topUpCalculation);
            } else {
                entityManager.merge(topUpCalculation);
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
        return topUpCalculation;
    }

    public void saveTopUpCalculations(List<TopUpCalculation> topUpCalculations) throws GeneralException, NullParameterException {
        if (topUpCalculations == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "topUpCalculation"), null);
        }
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            for (TopUpCalculation tuc : topUpCalculations) {
                if (tuc.getId() == null) {
                    entityManager.persist(tuc);
                } else {
                    entityManager.merge(tuc);
                }
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
    }

    public void disableTopUpCalculations() throws GeneralException, NullParameterException {
        String sql = "UPDATE TopUpCalculation SET endingDate = CURRENT_DATE WHERE endingDate IS NULL";
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

    public void setTopUpFormula(Long topUpProductId, String formula) throws GeneralException, NullParameterException {
        EntityTransaction transaction = null;
        try {
            String sql = "UPDATE TopUpProduct SET commissionFormula = " + formula + " WHERE id = " + topUpProductId;
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

    public void dropTopUpFormula(Long topUpProductId) throws GeneralException, NullParameterException {
        EntityTransaction transaction = null;
        try {
            String sql = "UPDATE TopUpProduct SET commissionFormula = NULL WHERE id = " + topUpProductId;
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

    public List<TopUpProduct> getTopUpProductsForReport() throws EmptyListException, GeneralException {
        List<TopUpProduct> topUpProducts = new ArrayList<TopUpProduct>();
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT tup1 FROM TopUpProduct tup1 WHERE tup1.enabled = 1 AND tup1.commissionPercent = (SELECT MAX(tup2.commissionPercent) FROM TopUpProduct tup2 WHERE tup2.enabled = 1 AND tup1.productDenomination.id = tup2.productDenomination.id) ORDER BY tup1.productDenomination.amount");
        Query query = null;
        try {
            query = createQuery(sqlBuilder.toString());
            topUpProducts = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }

        if (topUpProducts.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return topUpProducts;
    }

    // Funciones particulares para el integrador Prepay Nation:
    // 1- borrar
    public void deleteUpdatePrepayNationProducts() throws GeneralException {
        logger.info("[PROCESS] deleteUpdatePrepayNationProducts");
        try {

            EntityTransaction transaction = entityManager.getTransaction();
            if (transaction.isActive()) {
                transaction.rollback();
            }
            transaction.begin();
            entityManager.createNativeQuery("TRUNCATE TABLE update_prepay_nation_product").executeUpdate();
            transaction.commit();
            System.out.print("SUCCESS");
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
    }

    public void deleteUpdatePrepayNationCarriers() throws GeneralException {
        logger.info("[PROCESS] deleteUpdatePrepayNationCarriers");
        try {

            EntityTransaction transaction = entityManager.getTransaction();
            if (transaction.isActive()) {
                transaction.rollback();
            }
            transaction.begin();
            entityManager.createNativeQuery("TRUNCATE TABLE update_prepay_nation_carriers").executeUpdate();
            transaction.commit();
            System.out.print("SUCCESS");
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
    }

    //2- Cargar SKU:
    public void processPrepayNationTopUps() throws GeneralException {
        logger.info("[PROCESS] processPrepayNationTopUps");
        try {
            List<Sku> skus = ServiceManager.getSplitedSkus();
            UpdatePrepayNationProduct upnp = null;
            EntityTransaction transaction = entityManager.getTransaction();
            transaction.begin();
            for (int i = 0; i < skus.size(); i++) {
                boolean isOK = ((i % 100) == 0 || i == skus.size() - 1);
                //Permite hacer commit cada 100 registros
                upnp = new UpdatePrepayNationProduct(skus.get(i));
//                if (upnp.getCarrierName().contains("Cubacel - I")) {
//                    System.out.println("upnp.getCarrierName()" + upnp.getCarrierName());
//                    System.out.println("upnp.getCategory()" + upnp.getCategory());
//                    System.out.println("ESTOOOOOOOOOOOOOOOOOOoo");
//                }
                // Esto será movido luego.
                // Buscar el id de la denominacion y setearlo.
                List result = new ArrayList();
                StringBuilder sqlBuilder = new StringBuilder("SELECT pd.id FROM services.product_denomination pd WHERE pd.amount = ?1;");
                Query query = entityManager.createNativeQuery(sqlBuilder.toString());
                query.setParameter("1", upnp.getDenomination().floatValue());
                result = (List) query.setHint("toplink.refresh", "true").getResultList();
                if (!result.isEmpty() && upnp.getDenomination().intValue() != -1) {

                    upnp.setDenominationId(Long.parseLong(((java.util.Vector) result.get(0)).get(0).toString()));//Glassfish 2.1
                    //upnp.setDenominationId(Long.parseLong(result.get(0).toString()));//Glassfish 3.2.1
                }
                // Fin de el código que va en otra parte (borrar comentario).

                // Buscar el nombre del carrier para setear carrierId.
                result = new ArrayList();
                sqlBuilder = new StringBuilder("SELECT upnc.carrierId FROM services.update_prepay_nation_carriers upnc WHERE upnc.carrierName =?1;");
                query = entityManager.createNativeQuery(sqlBuilder.toString());
                query.setParameter("1", upnp.getCarrierName());
                result = (List) query.setHint("toplink.refresh", "true").getResultList();
                if (!result.isEmpty()) {
                    //upnp.setCarrierId(((Integer) (((List) result.get(0)).get(0))).intValue());//Glassfish 2.1
                    upnp.setCarrierId(Long.parseLong(((java.util.Vector) result.get(0)).get(0).toString()));
                  //  upnp.setCarrierId(Long.parseLong(result.get(0).toString()));
                }

                // Buscar el id del operador para setearlo.
                result = new ArrayList();
                upnp.setMobileOperatorId(null);
                System.out.println("upnp " + upnp.getCarrierName());
                if (upnp.getCategory().equalsIgnoreCase("Rtr") && upnp.getDenomination() > 0) {
                    entityManager.persist(upnp);
                }
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
            System.out.println("End processPrepayNationTopUps " + Calendar.getInstance().getTime());
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }

    }

    public void processPrepayNationCarriers() throws GeneralException {
        logger.info("[PROCESS] processPrepayNationCarriers");
        try {
            List<Carrier> carriers = ServiceManager.getCarriers();
            UpdatePrepayNationCarriers upnc = null;
            EntityTransaction transaction = entityManager.getTransaction();
            transaction.begin();
            for (int i = 0; i < carriers.size(); i++) {
                boolean isOK = ((i % 100) == 0 || i == carriers.size() - 1);
                //Permite hacer commit cada 100 registros
                upnc = new UpdatePrepayNationCarriers(carriers.get(i));

                entityManager.persist(upnc);
                if (isOK) {
                    transaction.commit();
                    if (!(i == carriers.size() - 1)) {
                        transaction = entityManager.getTransaction();
                        transaction.begin();
                    }
                }
            }
            if (transaction.isActive()) {
                transaction.rollback();
            }
            System.out.println("End processPrepayNationCarriers " + Calendar.getInstance().getTime());
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }

    }

    public void processPrepayNationDenominations() throws GeneralException {
        logger.info("[PROCESS] processPrepayNationDenominations");
        List denominations = null;
        try {
            StringBuilder sqlBuilder = new StringBuilder("SELECT DISTINCT(upnp.denomination) FROM services.update_prepay_nation_product upnp WHERE upnp.denomination NOT IN (SELECT p.amount FROM services.product_denomination p WHERE p.productId = 3) and upnp.maxAmount > 0");
            Query query = entityManager.createNativeQuery(sqlBuilder.toString());
            query.setParameter("1", Product.TOP_UP_PRODUCT_ID);
            denominations = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }

        if (denominations != null && denominations.size() > 0) {
            EntityTransaction transaction = entityManager.getTransaction();
            transaction.begin();
            for (int i = 0; i < denominations.size(); i++) {
                Float denominationValue = (Float) Float.valueOf(((List) denominations.get(i)).get(0).toString());//Glassfish 2.1
              //  Float denominationValue = Float.parseFloat(denominations.get(i).toString());//Glassfish 3.2.1
                ProductDenomination pDenomination = new ProductDenomination();
                pDenomination.setAmount(denominationValue);
                Product product = new Product();
                product.setId(Product.TOP_UP_PRODUCT_ID);
                pDenomination.setProduct(product);
                pDenomination.setCurrency(new Currency(Currency.DOLLAR));
                entityManager.persist(pDenomination);
            }
            transaction.commit();
        }
    }

    public boolean processTransferToMobileOperatorsAndCountries(Enterprise enterprise, ArrayList<String> recipients) throws GeneralException, RegisterNotFoundException, NullParameterException {
        List<MobileOperatorHasProvider> mobileOperatorHasProviders = this.processTransferToMobileOperators();
        List<CountryHasProvider> countryHasProviders = this.processTransferToCountries();
        try {
            List<User> users = userEJB.getUserTopUpNotification();
            for (User user : users) {
                recipients.add(user.getEmail());
            }
        } catch (EmptyListException ele) {
            recipients.add(ServiceMails.SAC_COORDINADORES_MAIL);
        }

        if ((mobileOperatorHasProviders != null && mobileOperatorHasProviders.size() > 0) || (countryHasProviders != null && countryHasProviders.size() > 0)) {
            this.sendPendingDataMail(enterprise, mobileOperatorHasProviders, countryHasProviders, recipients, "Actualizacion de Productos TopUp Transfer-To");
            return true;
        }
        return false;
    }

    private boolean processPrepayNationMobileOperatorsAndCountries(Enterprise enterprise, ArrayList<String> recipients) throws GeneralException, RegisterNotFoundException, NullParameterException {

        List<MobileOperatorHasProvider> mobileOperatorHasProviders = this.processPrepayNationMobileOperators();
        List<CountryHasProvider> countryHasProviders = this.processPrepayNationCountries();

        try {
            List<User> users = userEJB.getUserTopUpNotification();
            for (User user : users) {
                recipients.add(user.getEmail());
            }
        } catch (EmptyListException ele) {
            recipients.add(ServiceMails.SAC_COORDINADORES_MAIL);
        }

        if ((mobileOperatorHasProviders != null && mobileOperatorHasProviders.size() > 0) || (countryHasProviders != null && countryHasProviders.size() > 0)) {
            this.sendPendingDataMail(enterprise, mobileOperatorHasProviders, countryHasProviders, recipients, "Actualizacion de Productos TopUp Prepay-Nation");
            return true; // Interrumpe la ejecución de la función que lo invoca debe ser true.
        }
        return false; // Hace que no se ejecute la condición del if que lo llama y por lo tanto continua la ejecución normal.
    }

    public List<MobileOperatorHasProvider> processPrepayNationMobileOperators() throws GeneralException {
        System.out.println("processPrepayNationMobileOperators");
        logger.info("[PROCESS] processPrepayNationMobileOperators");
        List result = new ArrayList();
        List<MobileOperatorHasProvider> operatorHasProviders = new ArrayList<MobileOperatorHasProvider>();
        try {
            StringBuilder sqlBuilder = new StringBuilder("SELECT upnp.* FROM services.update_prepay_nation_product upnp WHERE upnp.carrierId NOT IN (SELECT m.referenceCode FROM services.mobile_operator_has_provider m WHERE m.providerId = ?1) GROUP BY upnp.carrierName ORDER BY upnp.countryCode;");
            Query query = entityManager.createNativeQuery(sqlBuilder.toString());
            query.setParameter("1", Provider.PREPAY_NATION);
            result = (List) query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        Provider provider = null;
        try {
            provider = productEJB.loadProvider(new EJBRequest(new Long(Provider.PREPAY_NATION)));
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        if (result != null && result.size() > 0) {
            for (int i = 0; i < result.size(); i++) {
                List object = (List) result.get(i);//Glassfish 2.1
            //    Object[] object = (Object[]) result.get(i);//Glassfish 3.2.1
                String operatorName = object.get(16).toString().toUpperCase();//Glassfish 2.1
           //     String operatorName = object[16].toString().toUpperCase();//Glassfish 3.2.1
                String countryCode = object.get(17).toString().toUpperCase();//Glassfish 2.1
           //     String countryCode = object[17].toString().toUpperCase();//Glassfish 3.2.1
                String referenceCode = object.get(14).toString();//Glassfish 2.1
               // String referenceCode = object[14].toString();//Glassfish 3.2.1

                try {
                      MobileOperatorHasProvider mobileHasProvider = null;
                    MobileOperator mobileOperator = this.loadMobileOperatorByName(operatorName);
                    try {
                        mobileHasProvider = this.loadMobileOperatorHasProvider(mobileOperator.getId(), provider.getId());
                    } catch (RegisterNotFoundException rne) {
                        mobileHasProvider = new MobileOperatorHasProvider();
                        mobileHasProvider.setMobileOperator(mobileOperator);
                        mobileHasProvider.setProvider(provider);
                    }
                    //En caso de existir el MobileOperatorHasProvider solo actualizamos el referenceCode;
                    mobileHasProvider.setReferenceCode(referenceCode);
                    this.saveMobileOperatorHasProvider(mobileHasProvider);


                } catch (RegisterNotFoundException ex) {

                    Provider newProvider = new Provider();
                    newProvider.setName("Prepay Nation. -- Cod Pais (Para referencia): " + countryCode);
                    MobileOperator mobileOperator = new MobileOperator();
                    mobileOperator.setName(operatorName);
                    MobileOperatorHasProvider operatorHasProvider = new MobileOperatorHasProvider();
                    operatorHasProvider.setMobileOperator(mobileOperator);
                    operatorHasProvider.setProvider(newProvider);
                    operatorHasProviders.add(operatorHasProvider);

                } catch (Exception ex) {
                    throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
                }
            }
        }
        return operatorHasProviders;
    }

    public List<CountryHasProvider> processPrepayNationCountries() throws GeneralException {
        System.out.println("processPrepayNationCountries");
        logger.info("[PROCESS] processPrepayNationCountries");
        List result = new ArrayList();
        List<CountryHasProvider> pendingCountries = new ArrayList<CountryHasProvider>();
        try {
            StringBuilder sqlBuilder = new StringBuilder("SELECT upnp.* FROM services.update_prepay_nation_product upnp WHERE upnp.countryCode NOT IN (SELECT c.referenceCode FROM services.country_has_provider c WHERE c.providerId = ?1) GROUP BY upnp.countryCode");
            Query query = entityManager.createNativeQuery(sqlBuilder.toString());
            query.setParameter("1", Provider.PREPAY_NATION);
            result = (List) query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        Provider provider = null;
        try {
            provider = productEJB.loadProvider(new EJBRequest(new Long(Provider.PREPAY_NATION)));
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        if (result != null && result.size() > 0) {
            for (int i = 0; i < result.size(); i++) {
                List object = (List) result.get(i);//Glassfish 2.1
              //  Object[] object = (Object[]) result.get(i);//Glassfish 3.2.1
                String referenceCode = object.get(17).toString(); //Glassfish 2.1
                //String referenceCode = object[17].toString();//Glassfish 3.2.1

                try {
                    Country country = utilsEJB.loadCountryByShortName(referenceCode);
                    CountryHasProvider countryHasProvider = new CountryHasProvider();
                    countryHasProvider.setCountry(country);

                    countryHasProvider.setProvider(provider);
                    countryHasProvider.setReferenceCode(referenceCode);
                    utilsEJB.saveCountryHasProvider(countryHasProvider);

                } catch (RegisterNotFoundException ex) {
                    //provider.setName();
                    Provider newProvider = new Provider();
                    newProvider.setName("Prepay Nation" + " Cod de pais( Para referencia): " + referenceCode);
                    String carrierName = object.get(16).toString(); //Glassfish 2.1
                 //   String carrierName = object[16].toString();//Glassfish 3.2.1
                    Country country = new Country();
                    country.setName("Nombre de operadora: " + carrierName);
                    CountryHasProvider countryHasProvider = new CountryHasProvider();
                    countryHasProvider.setCountry(country);
                    countryHasProvider.setReferenceCode(referenceCode);
                    countryHasProvider.setProvider(newProvider);
                    //countryHasProvider.getProvider().setName("asdasdhuashduia");
                    pendingCountries.add(countryHasProvider);
                } catch (Exception ex) {
                    throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
                }
            }
        }
        return pendingCountries;
    }

    public void disableUnAvailablePrepayNationTopUps() throws GeneralException {
        System.out.println("disableUnAvailablePrepayNationTopUps");
        logger.info("[PROCESS] disableUnAvailablePrepayNationTopUps");
        List mobileOperatorIds = null;
        try {
            String sql1 = "SELECT DISTINCT(m.referenceCode) FROM services.mobile_operator_has_provider m WHERE m.providerId = ?1 AND m.referenceCode IN (SELECT DISTINCT (upnp.carrierId) FROM services.update_prepay_nation_product upnp)";
            Query query = entityManager.createNativeQuery(sql1);
            query.setParameter("1", Provider.PREPAY_NATION);
            mobileOperatorIds = query.setHint("toplink.refresh", "true").getResultList();
            if (mobileOperatorIds != null && mobileOperatorIds.size() > 0) {
                StringBuilder ids = new StringBuilder("");
                for (int i = 0; i < mobileOperatorIds.size(); i++) {
                    String id = ((List) mobileOperatorIds.get(i)).get(0).toString();//Glassfish 2.1
                   // String id = mobileOperatorIds.get(i).toString();
                    ids.append(id).append(",");
                }
                System.out.println("ids " + ids);
                ids.deleteCharAt(ids.length() - 1);

                try {
                    StringBuilder sqlBuilder1 = new StringBuilder("UPDATE services.top_up_product t SET t.enabled = 0 WHERE t.providerId = ?1 AND t.mobileOperatorId NOT IN (SELECT m.mobileOperatorId FROM services.mobile_operator_has_provider m WHERE m.providerId = ?1 AND m.referenceCode IN (" + ids + "))");

                    EntityTransaction transaction = entityManager.getTransaction();
                    transaction.begin();
                    Query query1 = entityManager.createNativeQuery(sqlBuilder1.toString());
                    query1.setParameter("1", Provider.PREPAY_NATION);
                    query1.executeUpdate();
                    transaction.commit();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
    }

    public void processPrepayNationTopUpInformation() throws GeneralException {
        System.out.println("processPrepayNationTopUpInformation");
        logger.info("[PROCESS] processPrepayNationTopUpInformation");
        try {
            StringBuilder sqlBuilder1 = new StringBuilder("UPDATE services.update_prepay_nation_product t, services.product_denomination p SET t.denominationId = p.id WHERE t.denomination = p.amount AND p.productId = ?1");
            StringBuilder sqlBuilder3 = new StringBuilder("UPDATE services.update_prepay_nation_product t, services.mobile_operator_has_provider m SET t.mobileOperatorId = m.mobileOperatorId WHERE t.carrierId = m.referenceCode AND m.providerId = ?1");
            EntityTransaction transaction = entityManager.getTransaction();
            transaction.begin();
            Query query1 = entityManager.createNativeQuery(sqlBuilder1.toString());
            Query query3 = entityManager.createNativeQuery(sqlBuilder3.toString());
            query1.setParameter("1", Product.TOP_UP_PRODUCT_ID);
            query3.setParameter("1", Provider.PREPAY_NATION);
            query1.executeUpdate();
            query3.executeUpdate();
            transaction.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
    }

    public void includePrepayNationTopUps() throws GeneralException {
        System.out.println("includePrepayNationTopUps");
        logger.info("[PROCESS] includePrepayNationTopUps");
        disableModifiedPrepayNationTopUps();
        //disableDuplicatedPrepayNationTopUps();
        Timestamp now = new Timestamp(new java.util.Date().getTime());
        try {
            List<TopUpProduct> topUpProducts = null;
            try {
                topUpProducts = getTopUpProductByProviderId(Long.parseLong(String.valueOf(Provider.PREPAY_NATION)));
            } catch (EmptyListException ele) {
                ele.printStackTrace();
            }

            Map<String, TopUpProduct> map = new HashMap<String, TopUpProduct>();
            if (topUpProducts != null) {
                for (TopUpProduct topUpProduct : topUpProducts) {
                    map.put(topUpProduct.getMobileOperator().getId() + "-" + topUpProduct.getProductDenomination().getId(), topUpProduct);
                }
            }
            Provider provider = productEJB.loadProvider(new EJBRequest(new Long(Provider.PREPAY_NATION)));
            List<UpdatePrepayNationProduct> updatePrepayNationProducts = new ArrayList<UpdatePrepayNationProduct>();
            updatePrepayNationProducts = getUpdatePrepayNationProducts();
            EntityTransaction transaction = entityManager.getTransaction();
            transaction.begin();
            UpdatePrepayNationProduct upnp = null;
            for (int i = 0; i < updatePrepayNationProducts.size(); i++) {
                upnp = updatePrepayNationProducts.get(i);
                //upnp = loadUpdatePrepayNationProduct(updatePrepayNationProducts.get(i).getId()) ;
                String key = upnp.getMobileOperatorId() + "-" + upnp.getDenominationId();
                System.out.println("key " + key);
                if (map.containsKey(key)) {
                    TopUpProduct topUpProduct = map.get(key);
                    if (topUpProduct.getCommissionPercent() != upnp.getDiscount().floatValue()) // <--- MOSCA ¿DESCUESTO ES LO MISMO QUE COMISION?
                    {
                        //Si el TopUp Existe pero cambio el porcentaje de comision.
                        //Desactivo el TopUp que ya existe y Luego persisto uno nuevo.

                        String sql1 = "UPDATE TopUpProduct tup SET tup.enabled = FALSE WHERE tup.id= " + topUpProduct.getId();
                        ///String sql2 = "UPDATE MobileOperatorHasDenominationHasProfile mhdhp SET mhdhp.endingDate = CURRENT_DATE WHERE mhdhp.mobileOperator.id=" + topUpProduct.getMobileOperator().getId() + " AND mhdhp.productDenomination.id=" + topUpProduct.getProductDenomination().getId() + " AND mhdhp.endingDate IS NULL";
                        createQuery(sql1).executeUpdate();
                        //createQuery(sql2).executeUpdate();
                        TopUpProduct newTopUpProduct = new TopUpProduct();
                        newTopUpProduct.setName(upnp.getCarrierName() + " " + upnp.getCurrencyCode() + " " + upnp.getDenomination()); // MOSCA
                        newTopUpProduct.setDescription(topUpProduct.getName());
                        newTopUpProduct.setProductDenomination(productEJB.loadProductDenominationById((long) upnp.getDenominationId()));
                        newTopUpProduct.setMobileOperator(this.loadMobileOperatorsById((long) (upnp.getMobileOperatorId())));
                        newTopUpProduct.setProvider(provider);

                        newTopUpProduct.setCommissionPercent(upnp.getDiscount().floatValue());
                        newTopUpProduct.setReferenceCode(String.valueOf(upnp.getSkuId()));
                        newTopUpProduct.setCreationDate(now);
                        newTopUpProduct.setEnabled(true);
                        entityManager.persist(newTopUpProduct);

                    }
                } else {
                    //Si el TopUp no existe, persisto uno nuevo.
                    if (upnp.getMobileOperatorId() != null) {
                        //Verificar el caso en los que no hace match el nombre del carrier del topUp

                        TopUpProduct topUpProduct = new TopUpProduct();
                        topUpProduct.setName(upnp.getCarrierName() + " " + upnp.getCurrencyCode() + " " + (String.valueOf(upnp.getDenomination() * upnp.getExchangeRate().floatValue())));
                        System.out.println("Insert update_prepay_nation_product"+ topUpProduct.getName());
                        topUpProduct.setDescription(topUpProduct.getName());
                        topUpProduct.setProvider(provider);
                        topUpProduct.setProductDenomination(productEJB.loadProductDenominationById((long) upnp.getDenominationId()));
                        topUpProduct.setMobileOperator(this.loadMobileOperatorsById((long) (upnp.getMobileOperatorId())));
                        topUpProduct.setCommissionPercent(upnp.getDiscount().floatValue());
                        topUpProduct.setReferenceCode(String.valueOf(upnp.getSkuId()));
                        topUpProduct.setCreationDate(now);
                        topUpProduct.setEnabled(true);
                        entityManager.persist(topUpProduct);
                    }
                }
                boolean isOK = ((i % 100) == 0 || i == updatePrepayNationProducts.size() - 1);
                if (isOK) {
                    transaction.commit();
                    if (!(i == updatePrepayNationProducts.size() - 1)) {
                        transaction = entityManager.getTransaction();
                        transaction.begin();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw new GeneralException(ex.getMessage());
        }
    }

    public void disableModifiedPrepayNationTopUps() throws GeneralException {
        System.out.println("disableModifiedPrepayNationTopUps");
        logger.info("[PROCESS] disableModifiedPrepayNationTopUps");
        try {
            String sql1 = "UPDATE TopUpProduct tp SET tp.enabled = FALSE "
                    + "WHERE tp.id IN (SELECT t.id FROM TopUpProduct t, UpdatePrepayNationProduct upnp "
                    + "WHERE t.mobileOperator.id= upnp.mobileOperatorId AND t.enabled= TRUE "
                    + "AND t.productDenomination.id = upnp.denominationId AND t.provider.id = ?1 "
                    + "AND t.commissionPercent <> upnp.discount)";

            String sql2 = "UPDATE TopUpProduct tp SET tp.enabled = FALSE "
                    + "WHERE tp.enabled = TRUE AND tp.provider.id = ?1 AND tp.id NOT IN (SELECT t.id FROM TopUpProduct t, UpdatePrepayNationProduct upnp "
                    + "WHERE t.mobileOperator.id= upnp.mobileOperatorId AND t.enabled= TRUE "
                    + "AND t.productDenomination.id = upnp.denominationId AND t.provider.id = ?1 )";

            /*TODO: con cada perfil*/
            entityManager.getTransaction().begin();
            Query query = createQuery(sql1);
            Query query2 = createQuery(sql2);
            query.setParameter("1", Provider.PREPAY_NATION);
            query2.setParameter("1", Provider.PREPAY_NATION);
            query.executeUpdate();
            query2.executeUpdate();
            entityManager.getTransaction().commit();
        } catch (Exception ex) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw new GeneralException(ex.getMessage());
        }

    }

    public List<UpdatePrepayNationProduct> getUpdatePrepayNationProducts() throws GeneralException, EmptyListException {
        System.out.println("getUpdatePrepayNationProducts");
        logger.info("[PROCESS] getUpdatePrepayNationProducts");
        List<UpdatePrepayNationProduct> updatePrepayNationProducts = new ArrayList<UpdatePrepayNationProduct>();
        try {
            StringBuilder sqlBuilder = new StringBuilder("SELECT upnp FROM UpdatePrepayNationProduct upnp");
            Query query = entityManager.createQuery(sqlBuilder.toString());
            updatePrepayNationProducts = query.setHint("toplink.refresh", "true").getResultList();
            updatePrepayNationProducts.size();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        if (updatePrepayNationProducts.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return updatePrepayNationProducts;
    }

    public void deleteDisabledPrepayNationProducts() throws GeneralException {
        System.out.println("deleteDisabledPrepayNationProducts");
        logger.info("[PROCESS] deleteDisabledPrepayNationProducts");
        try {
//            EntityTransaction transaction = entityManager.getTransaction();
//            transaction.begin();
//            entityManager.createQuery("DELETE FROM TopUpProduct WHERE provider.id = " + Provider.PREPAY_NATION + " AND enabled = 0 AND id NOT IN (SELECT tp.id FROM TopUpProduct tp, TransactionItem ti WHERE ti.topUpProduct.id = tp.id)").executeUpdate();
//            transaction.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
    }

    public void executePrepayNationTopUpUpdate() throws GeneralException {

        logger.info("[PROCESS] executePrepayNationTopUpUpdate");
        Enterprise enterprise = null;
        try {
            EJBRequest request = new EJBRequest();
            request.setParam(Enterprise.ALODIGA_USA);
            enterprise = utilsEJB.loadEnterprise(request);
            ArrayList<String> recipients = new ArrayList<String>();
            recipients.add(enterprise.getEmail());
            // 1.- Borra la tabla con los productos de Prep executeTransferToTopUpUpdate ay Nation (SKU):
            deleteUpdatePrepayNationProducts();
            // 1.- Borra la tabla con los proveedores de Prepay Nation (CARRIERS):
            deleteUpdatePrepayNationCarriers();
            // 2.- Cargar en la tabla todos los productos de Prepay Nation (SKU):
            processPrepayNationCarriers();
            processPrepayNationTopUps();
            // 3.- Consulta las denominaciones de los topUp de Prepay Nation que no existen en la base de datos de distribución y los guarda.
            processPrepayNationDenominations();
            // 5 - Verifica las Operadoras y Paises que no esten disponibles y que deben ser agregados manualmente.
            //Si encuentra registros no disponibles envia SMS y correo electronico y detiene el proceso.

            // --> Tst frm hr:
            if (this.processPrepayNationMobileOperatorsAndCountries(enterprise, recipients)) {
                return;
            }
            //4 - Deshabilita los topUps que no estan disponibles en distribución en relación a los disponibles en Prepay Nation.
//            this.disableUnAvailablePrepayNationTopUps();
            //6 - Actualiza la información necesaria como porcentaje de comisión denominación y operadoras.
            this.processPrepayNationTopUpInformation();
            //7 - Se agregan los nuevos top-Ups y se desactivan los que tengan cambios en comisiones.
            this.includePrepayNationTopUps();

            this.disableUnAvailablePrepayNationTopUps();
            //8 - Elimina los top-Ups que estan deshabilitados y que nunca han sido utilizados en una transacción.

            this.deleteDisabledPrepayNationProducts();
            //9 -Se envia la notificación de la actualización de las comisiones por realizar.
            this.sendTopUpNotificationMail(enterprise, recipients, "Prepay Nation");
        } catch (Exception ex) {
            ex.printStackTrace();
            sendUpdateTopUpErrorMail(enterprise, "Proceso de TopUp - Prepay Nation", "Error General - en metodo executePrepayNationTopUpUpdate()", ex);
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
    }

    //Envía notificación de que se deben actualizar las comisiones
    private void sendUpdateTopUpErrorMail(Enterprise enterprise, String process, String error, Exception ex) throws GeneralException {
        System.out.println("sendUpdateTopUpErrorMail");
        logger.info("[PROCESS] sendUpdateTopUpErrorMail");
        try {

            Mail mail = ServiceMails.getUpdateProcessErrorMail(enterprise, process, error, ex);
            //utilsEJB.sendMail(mail);
            (new com.alodiga.services.provider.commons.utils.MailSender(mail)).start();
        } catch (Exception e) {
            throw new GeneralException(e.getMessage());
        }
    }

    public float getTopUpCommission(Long mobileOperatorId, Long denominationId, Long profileId) throws GeneralException, NullParameterException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public float getTopUpCommissionPercent(Long levelId, Long productId, TopUpProduct topUpProduct) throws GeneralException, NullParameterException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void sendPendingDataMail(Enterprise enterprise, List<MobileOperatorHasProvider> mobileOperatorHasProviders, List<CountryHasProvider> countryHasProviders, ArrayList<String> recipients) throws GeneralException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void sendTopUpNotificationMail(Enterprise enterprise, ArrayList<String> recipients) throws GeneralException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Deprecated
    public List<TopUpCalculation> getTopUpCalculationsEnabled() throws GeneralException, RegisterNotFoundException, NullParameterException, EmptyListException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Deprecated
    public List<TopUpCalculation> getTopUpCalculations() throws GeneralException, RegisterNotFoundException, NullParameterException, EmptyListException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Deprecated
    public TopUpCalculation loadCommissionAdmin(EJBRequest request) throws RegisterNotFoundException, NullParameterException, GeneralException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public UpdatePrepayNationProduct loadUpdatePrepayNationProduct(Long id) throws RegisterNotFoundException, NullParameterException, GeneralException {
        if (id == null || id == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "id"), null);
        }
        UpdatePrepayNationProduct upnp = null;
        String sql = "SELECT upnp FROM UpdatePrepayNationProduct upnp WHERE upnp.id=" + id;
        try {
            upnp = (UpdatePrepayNationProduct) createQuery(sql).setHint("toplink.refresh", "true").getSingleResult();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        if (upnp == null) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, MobileOperatorHasProvider.class.getSimpleName(), "getMobileOperatorHasProvider", MobileOperatorHasProvider.class.getSimpleName(), null), null);
        }
        return upnp;
    }

    public GeneralTopUpResponse doEasyCallTopUp(String productId, String senderNumber, String phoneNumber, float amount, String externalId, Account account, Long languageId, boolean sendSMS, boolean isUSADestination, String externalSMSName) throws NullParameterException, GeneralException, TopUpTransactionException {

        System.out.println("------------------- doEasyCallTopUp ------------------------");
        if (productId == null) {
            throw new NullParameterException("Parameter productId cannot be null");
        } else if (phoneNumber == null) {
            throw new NullParameterException("Parameter phoneNumber cannot be null");
        } else if (externalId == null) {
            throw new NullParameterException("Parameter externalId cannot be null");
        }

        GeneralTopUpResponse response = new GeneralTopUpResponse();
        String error = "";
        try {
            DoTopUpResponse doTopUpResponse = SoapClient.doTopUp(productId, phoneNumber, String.valueOf(amount), externalId);
            String code = String.valueOf(doTopUpResponse.getCode());
            response.setResponseCode(code);
            if (!code.equals("0")) {
                StringBuilder errorBuilder = new StringBuilder(TopUpResponseConstants.EASY_CALL_CODES.get(code));
                errorBuilder.append("Integrator = ").append("EasyCall").append(" ProductId = ").append(productId).append(" phoneNumber = ").append(phoneNumber).append(" amount = ").append(amount).append(" externalId = ").append(externalId);
                error = errorBuilder.toString();
                System.out.println("Top_up error" + error);
                throw new TopUpTransactionException(error);
            }
            response.setAdditionalMessage(doTopUpResponse.getAdditionalMessage());
            response.setMessage(doTopUpResponse.getMessage());
            response.setRetailerId(String.valueOf(doTopUpResponse.getRetailerId()));
            response.setTrasactionDate(doTopUpResponse.getTransationDate());
            response.setExternalId(doTopUpResponse.getExternalId());
            response.setInstructions(doTopUpResponse.getInstructions());
            response.setDisclaimer(doTopUpResponse.getDisclaimer());
            //response.setOrderNumber(doTopUpResponse.getOrderNumber());
            response.setAccount(account);
            response.setAccountName(doTopUpResponse.getAccount());
            response.setAmount(amount);
            response.setDestinationNumber(phoneNumber);
            response.setSenderNumber(senderNumber);
            response.setMessage(doTopUpResponse.getTransactionMessage());

            if (sendSMS) {
                String message1 = ServiceSMSDispatcher.sendSenderSMS(amount, senderNumber, phoneNumber, null, account, languageId, externalSMSName);
                String message2 = ServiceSMSDispatcher.sendDestinationSMS(amount, senderNumber, phoneNumber, null, account, languageId, isUSADestination, externalSMSName);
                response.setSenderSMS(message1);
                response.setDestinationSMS(message2);
            }

        } catch (Exception ex) {
            throw new TopUpTransactionException(ex.getMessage() + error);
        }
        return response;
    }

    @Override
    public GeneralTopUpResponse doTransferToTopUp(TopUpProduct topUpProduct, String senderNumber, String destinationNumber, Account account, Long languageId, boolean sendSMS, String externalSMSName) throws NullParameterException, GeneralException, TopUpTransactionException, TopUpProductNotAvailableException, InvalidFormatException, DestinationNotPrepaidException {
        System.out.println("------------------- doTransferToTopUp ------------------------");

       if (topUpProduct == null) {
            throw new NullParameterException("Parameter topUpProduct cannot be null");
        } else if (senderNumber == null) {
            throw new NullParameterException("Parameter senderNumber cannot be null");
        } else if (destinationNumber == null) {
            throw new NullParameterException("Parameter destinationNumber cannot be null");
        }
        GeneralTopUpResponse response = new GeneralTopUpResponse();
        String error = "";
        try {
//            String referenceCode = this.getMobileOperatorHasProvider(topUpProduct.getMobileOperator().getId(), topUpProduct.getProvider().getId()).getReferenceCode();
//            String phoneNumber = topUpProduct.getMobileOperator().getCountry().getId().equals(Country.USA) ? "1" : "";
//            phoneNumber += destinationNumber;
//            TopUpResponse topUpResponse = RequestManager.doTopUp(senderNumber, phoneNumber, topUpProduct.getReferenceCode(), Long.parseLong(referenceCode), "Powered by Alodiga", "Powered by Alodiga");
//            String code = topUpResponse.getErrorCode();
//            response.setResponseCode(topUpResponse.getErrorCode());
             Float amount = topUpProduct.getProductDenomination().getAmount();
             String phoneNumber = topUpProduct.getMobileOperator().getCountry().getId().equals(Country.USA) ? "1" : "";
             phoneNumber = phoneNumber + destinationNumber;
             response.setAmount(topUpProduct.getProductDenomination().getAmount());
             MSIDN_INFOResponse response1 = RequestManager.getMsisdn_ingo(phoneNumber);
             String skuidId = response1.getSkuid();
             if (response1.getSkuid() == null) {
                System.out.println("response " + response1.getSkuid_list());
                String[] Skuids = response1.getSkuid_list().split(",");
                String[] products = response1.getProduct_list().split(",");
                for (int o = 0; o < products.length; o++) {
//                  System.out.println("products "+o+":"+products[o] +"="+amount);
                  if (Float.parseFloat(products[o])==amount) {
                   System.out.println("es igual");
                    skuidId = Skuids[o];
                  }
                }
             }
             System.out.println("skuidId " + skuidId);
             ReserveResponse response2 = RequestManager.getReserve();
             TopUpResponse topUpResponse = RequestManager.simulationDoTopUp(senderNumber, phoneNumber, amount.toString(), skuidId);
             TopUpResponse topUpResponse1 = RequestManager.newDoTopUp(senderNumber, phoneNumber, amount.toString(), skuidId, response2.getReserved_id());
             String code = topUpResponse1.getErrorCode();
             response.setResponseCode(topUpResponse.getErrorCode());
             if (!code.equals("0")) {//Cuando es 0 esta bien...
                StringBuilder errorBuilder = new StringBuilder(TopUpResponseConstants.TRANSFER_TO_CODES.get(code));
                errorBuilder.append("Integrator = ").append("TransferTo").append("ProductId = ").append(topUpProduct.getId()).append("phoneNumber = ").append(destinationNumber);
                error = errorBuilder.toString();

                if (code.equals("301") || topUpResponse.getErrorText().equals("Denomination not available")) {
                    this.disableTopUpProduct(topUpProduct);
                    throw new TopUpProductNotAvailableException(error);
                } else if (code.equals("101") || topUpResponse.getErrorText().equals("Destination MSISDN out of range")) {
                    throw new InvalidFormatException(error);
                } else if (code.equals("204")) {
                    throw new DestinationNotPrepaidException(error);
                }
                throw new TopUpTransactionException(error);
             }
             try {
                if (topUpResponse != null && topUpResponse.getPinBased() != null && topUpResponse.getPinBased().equals("yes")) {
                    response.setIsPinBased(true);
                    response.setPinCode(topUpResponse.getPinCode());
                    response.setPinIVR(topUpResponse.getPinIvr());
                    response.setPinSerial(topUpResponse.getPinSerial());
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
            response.setErrorCode(topUpResponse.getErrorCode());
            response.setErrorMessage(topUpResponse.getErrorText());
            response.setAuthenticationKey(topUpResponse.getAuthenticationKey());
            response.setDestinationNumber(phoneNumber);
            response.setSenderNumber(senderNumber);
            if (sendSMS) {
                String message1 = ServiceSMSDispatcher.sendSenderSMS(topUpProduct.getProductDenomination().getAmount(), senderNumber, destinationNumber, response, account, languageId, externalSMSName);
                String message2 = ServiceSMSDispatcher.sendDestinationSMS(topUpProduct.getProductDenomination().getAmount(), senderNumber, destinationNumber, response, account, languageId, topUpProduct.getMobileOperator().getCountry().getId().equals(Country.USA), externalSMSName);
                response.setSenderSMS(message1);
                response.setDestinationSMS(message2);

            }
        } catch (DestinationNotPrepaidException ex) {
            throw (ex);
        } catch (GeneralException ex) {
            throw (ex);
        } catch (NullParameterException ex) {
            throw (ex);
        } catch (TopUpProductNotAvailableException ex) {
            throw (ex);
        } catch (InvalidFormatException ex) {
            throw (ex);
        } catch (Exception ex) {
            throw new GeneralException(ex.getMessage() + error);
        }
        return response;
    }
    
    public GeneralTopUpResponse doPrepayNationTopUp(TopUpProduct topUpProduct, String senderNumber, String destinationNumber, Account account, Long languageId, boolean sendSMS, String externalSMSName) throws NullParameterException, GeneralException, TopUpTransactionException, InvalidFormatException {
        System.out.println("------------------- doTransferToTopUp ------------------------");
        if (topUpProduct == null) {
            throw new NullParameterException("Parameter topUpProduct cannot be null");
        } else if (senderNumber == null) {
            throw new NullParameterException("Parameter senderNumber cannot be null");
        } else if (destinationNumber == null) {
            throw new NullParameterException("Parameter destinationNumber cannot be null");
        }
        GeneralTopUpResponse response = new GeneralTopUpResponse();
        String error = "";
        try {
            int skuId = Integer.parseInt(topUpProduct.getReferenceCode());
            String correlativeId = account.getId() + "-" + Calendar.getInstance().getTimeInMillis();
            OrderResponse orderResponse = ServiceManager.purchaseRtr2(skuId, topUpProduct.getProductDenomination().getAmount(), destinationNumber, correlativeId, null, null);
            String code = orderResponse.getResponseCode();

            if (!code.equals("000")) {//Cuando es 000 esta bien...
                StringBuilder errorBuilder = new StringBuilder(TopUpResponseConstants.PREPAY_NATION_CODES.get(code));
                errorBuilder.append("Integrator= ").append("PrepayNation").append("ProductId = ").append(topUpProduct.getId()).append("phoneNumber = ").append(destinationNumber);
                error = errorBuilder.toString();

                if (code.equals("033")) {
                    throw new InvalidFormatException(error);
                }
                throw new TopUpTransactionException(error);
            }
            if (sendSMS) {
                String message1 = ServiceSMSDispatcher.sendSenderSMS(topUpProduct.getProductDenomination().getAmount(), senderNumber, destinationNumber, null, account, languageId, externalSMSName);
                String message2 = ServiceSMSDispatcher.sendDestinationSMS(topUpProduct.getProductDenomination().getAmount(), senderNumber, destinationNumber, null, account, languageId, topUpProduct.getMobileOperator().getCountry().getId().equals(Country.USA), externalSMSName);
                response.setSenderSMS(message1);
                response.setDestinationSMS(message2);
            }
            response.setExternalId(String.valueOf(orderResponse.getInvoice().getInvoiceNumber()));
            response.setResponseCode(code);
            response.setMessage(orderResponse.getResponseMessage());
            response.setDestinationNumber(destinationNumber);
            response.setSenderNumber(senderNumber);
            response.setAuthenticationKey(String.valueOf(orderResponse.getInvoice().getInvoiceNumber()));

        } catch (InvalidFormatException ex) {
            throw (ex);
        } catch (Exception ex) {
            throw new GeneralException(ex.getMessage() + error);
        }
        return response;
    }

    public GeneralTopUpResponse doCSQTopUp(TopUpProduct topUpProduct, String senderNumber, String phoneNumber, Account account, Long languageId, boolean sendSMS, String externalSMSName) throws NullParameterException, InvalidAmountException, InvalidPhoneNumberException, TopUpProductNotAvailableException, DuplicatedTransactionException, CarrierSystemUnavailableException, GeneralException {

        System.out.println("------------------- doCSQTopUp ------------------------");
        if (topUpProduct == null) {
            throw new NullParameterException("Parameter topUpProduct cannot be null");
        } else if (senderNumber == null) {
            throw new NullParameterException("Parameter senderNumber cannot be null");
        } else if (phoneNumber == null) {
            throw new NullParameterException("Parameter phoneNumber cannot be null");
        }
        GeneralTopUpResponse response = new GeneralTopUpResponse();

        try {
            System.out.println("------------------- getReferenceCode ------------------------"+topUpProduct.getReferenceCode());
            System.out.println("------------------- phoneNumber ------------------------"+phoneNumber);
            float amount = topUpProduct.getProductDenomination().getAmount() * 100;
            System.out.println("------------------- Amount ------------------------"+amount);
            CSQTopUpResponse cSQTopUpResponse = CSQManager.executeTopUp(topUpProduct.getReferenceCode(), phoneNumber, amount);//CSQ recibe el importe en centimos.
            String code = cSQTopUpResponse.getCode();
            System.out.println("CSQTopUpResponse Code++++++++++"+code);
            if (sendSMS) {
                String message1 = ServiceSMSDispatcher.sendSenderSMS(topUpProduct.getProductDenomination().getAmount(), senderNumber, phoneNumber, null, account, languageId, externalSMSName);
                String message2 = ServiceSMSDispatcher.sendDestinationSMS(topUpProduct.getProductDenomination().getAmount(), senderNumber, phoneNumber, null, account, languageId, topUpProduct.getMobileOperator().getCountry().getId().equals(Country.USA), externalSMSName);
                response.setSenderSMS(message1);
                response.setDestinationSMS(message2);
            }
            response.setResponseCode(code);
            response.setMessage(cSQTopUpResponse.getMessage());
            response.setDestinationNumber(phoneNumber);
            response.setSenderNumber(senderNumber);
            response.setExternalId("localref: " + cSQTopUpResponse.getLocalReference() + "dateRef: " + cSQTopUpResponse.getDateReference());
            response.setMessage("To make any revert you must sent localref and dateRef");

        } catch (CSQNullParameterException ex) {
            throw new NullParameterException(ex.getMessage());
        } catch (CSQInvalidPhoneNumberException ex) {
            throw new InvalidPhoneNumberException(ex.getMessage());
        } catch (CSQInvalidAmountException ex) {
            throw new InvalidAmountException(ex.getMessage());
        } catch (CSQRejectedByOperatorException ex) {
            throw new CarrierSystemUnavailableException(ex.getMessage());
        } catch (CSQTemporarilyUnavailableProductException ex) {
            throw new TopUpProductNotAvailableException(ex.getMessage());
        } catch (CSQDuplicatedTransactionException ex) {
            throw new DuplicatedTransactionException(ex.getMessage());
        } catch (CSQGeneralException ex) {
            ex.printStackTrace();
            throw new GeneralException(ex.getMessage() + ex.getMessage());
        }
        return response;
    }

    public GeneralTopUpResponse executeTopUp(TopUpProduct topUpProduct, String destinationNumber, String externalId, String senderNumber, Account account, Long languageId, boolean sendSMS, String externalSMSName) throws TopUpTransactionException, GeneralException, TopUpProductNotAvailableException, InvalidFormatException, NotAvaliableServiceException, DestinationNotPrepaidException, DuplicatedTransactionException, CarrierSystemUnavailableException, InvalidPhoneNumberException, InvalidAmountException {

        System.out.println("---------------executeTopUp-----------");
        try {
            GeneralTopUpResponse response = new GeneralTopUpResponse();
            if (ServiceConstans.TEST_MODE) {
                //En caso de prueba retorna una respuesta de prueba.
                return getTopUpTestReponse(topUpProduct, destinationNumber, externalId, senderNumber, account, languageId, sendSMS, externalSMSName);
            }
            switch (topUpProduct.getProvider().getId().intValue()) {
                case Provider.EASY_CALL:
                    response = this.doEasyCallTopUp(topUpProduct.getReferenceCode(), senderNumber, destinationNumber, topUpProduct.getProductDenomination().getAmount(), externalId, account, languageId, sendSMS, topUpProduct.getMobileOperator().getCountry().getId().equals(Country.USA), externalSMSName);
                    break;
                case Provider.TRANSFER_TO:
                    response = this.doTransferToTopUp(topUpProduct, senderNumber, destinationNumber, account, languageId, sendSMS, externalSMSName);
                    break;
                case Provider.PREPAY_NATION:
                    response = this.doPrepayNationTopUp(topUpProduct, senderNumber, destinationNumber, account, languageId, sendSMS, externalSMSName);
                    break;
                case Provider.CSQ:
                    response = this.doCSQTopUp(topUpProduct, senderNumber, destinationNumber, account, languageId, sendSMS, externalSMSName);
                    break;
                case Provider.KDDI:
                    response = this.doKDDITopUp(topUpProduct, senderNumber, destinationNumber, account, languageId, sendSMS, externalSMSName);
                    break;
//                case Provider.MLAT:
//                    String phoneNumberForMlat = topUpProduct.getMobileOperator().getCountry().getId().equals(Country.USA) ? "1" : "";
//                    phoneNumberForMlat += destinationNumber;
//                    topUpEJB = (TopUpEJB) EJBServiceLocator.getInstance().get(EjbConstants.MLAT_TOP_UP_EJB);//TODO:
//                    preferenceEJB = (PreferenceEJB) EJBServiceLocator.getInstance().get(EjbConstants.MLAT_PREFERENCE_EJB);
//                    if (!topUpEJB.isServiceAvailability()) {
//                        throw new NotAvaliableServiceException("Service MLAT Top Up isn't currently available");
//                    }
//                    Float amount;
//                    TopUp topUp = new TopUp();
//                    amount = preferenceEJB.loadExchangeRateByInitialAmount(topUpProduct.getProductDenomination().getAmount());
//                    topUp.setAmount(amount);
//                    topUp.setCustomerEmail(topUpProduct.getProductDenomination().getProduct().getEnterprise().getEmail());
//                    topUp.setPhoneNumber(phoneNumberForMlat);
//                    topUp.setCustomerName("Distibution Recharge");
//                    topUp.setCreationDate(new Timestamp(new Date().getTime()));
//                    topUpEJB.saveTopUp(topUp);
//
//                    response.put(TopUpResponseConstants.COMPLETE_RESPONSE, "successful-MLAT");

                /* case Provider.MOBILE_PIN_INVENTORY:
                String phoneNumberForMPI = topUpProduct.getMobileOperator().getCountry().getId().equals(Country.USA) ? "1" : "";
                phoneNumberForMPI += destinationNumber;
                topUpMPIEJB = (com.alodiga.mobilepininventory.commons.ejb.TopUpEJB) com.alodiga.mobilepininventory.commons.util.EJBServiceLocator.getInstance().get(EjbConstants.MPI_TOP_UP_EJB);//TODO:
                denominationMPIEJB = (com.alodiga.mobilepininventory.commons.ejb.DenominationEJB) com.alodiga.mobilepininventory.commons.util.EJBServiceLocator.getInstance().get(EjbConstants.MPI_PREFERENCE_EJB);
                cardMPIEJB = (CardEJB)com.alodiga.mobilepininventory.commons.util.EJBServiceLocator.getInstance().get(EjbConstants.MPI_CARD_EJB);
                Float amountMPI;
                com.alodiga.mobilepininventory.model.topup.TopUp topUpMPI = new com.alodiga.mobilepininventory.model.topup.TopUp();
                Card card = null;
                try {
                Denomination denomination = denominationMPIEJB.loadDenominationByInitialAmount(topUpProduct.getProductDenomination().getAmount());
                amountMPI = denomination.getFinalAmount();
                card = cardMPIEJB.processTopUpRecharge(new Long(topUpProduct.getReferenceCode()), denomination,new CardStatus(CardStatus.CARD_ACTIVE_STATE));
                topUpMPIEJB.sendSms(amountMPI.toString(), phoneNumberForMPI,card.getSecretCode(), language, card.getMobileOperator().getBalanceRechargeNumber());
                topUpMPI.setAmount(amountMPI);
                topUpMPI.setCustomerEmail(topUpProduct.getProductDenomination().getProduct().getEnterprise().getEmail());
                topUpMPI.setPhoneNumber(phoneNumberForMPI);
                topUpMPI.setCustomerName("Distibution Recharge");
                topUpMPI.setCreationDate(new Timestamp(new Date().getTime()));
                topUpMPI.setMobileOperator(card.getMobileOperator());
                topUpMPI.setCard(card);
                card.setStatus(new CardStatus(CardStatus.CARD_USED_STATE));
                cardMPIEJB.saveCard(card);
                topUpMPIEJB.saveTopUp(topUpMPI);
                } catch (Exception e) {
                if (card!=null){
                card.setStatus(new CardStatus(CardStatus.CARD_ACTIVE_STATE));
                cardMPIEJB.saveCard(card);
                }
                throw new TopUpTransactionException(e.getMessage());
                }
                response.put(TopUpResponseConstants.COMPLETE_RESPONSE, "successful-MOBILE-PIN-INVENTORY");    */
                default:
                    break;
            }
            return response;
        } catch (DestinationNotPrepaidException ex) {
            throw new DestinationNotPrepaidException(ex.getMessage());
        } catch (DuplicatedTransactionException ex) {
            throw new DuplicatedTransactionException(ex.getMessage());
        } catch (CarrierSystemUnavailableException ex) {
            throw new CarrierSystemUnavailableException(ex.getMessage());
        } catch (InvalidPhoneNumberException ex) {
            throw new InvalidPhoneNumberException(ex.getMessage());
        } catch (InvalidAmountException ex) {
            throw new InvalidAmountException(ex.getMessage());
        } catch (InvalidFormatException ex) {
            throw new InvalidFormatException(ex.getMessage());
        } catch (TopUpProductNotAvailableException ex) {
            ServiceMailDispatcher.sendTopUpErrorMail(account, destinationNumber, senderNumber, topUpProduct, "Error en el metodo executeTopUp", ex);
            throw new TopUpProductNotAvailableException(ex.getMessage());
        } catch (TopUpTransactionException ex) {
            ServiceMailDispatcher.sendTopUpErrorMail(account, destinationNumber, senderNumber, topUpProduct, "Error en el metodo executeTopUp", ex);
            throw new TopUpTransactionException(ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            ServiceMailDispatcher.sendTopUpErrorMail(account, destinationNumber, senderNumber, topUpProduct, "Error en el metodo executeTopUp", ex);
            throw new TopUpTransactionException(ex.getMessage());
            //throw new GeneralException(ex.getMessage());
        }
    }
    
    public GeneralTopUpResponse doKDDITopUp(TopUpProduct topUpProduct, String senderNumber, String phoneNumber, Account account, Long languageId, boolean sendSMS, String externalSMSName) throws NullParameterException, InvalidAmountException, InvalidPhoneNumberException, TopUpProductNotAvailableException, DuplicatedTransactionException, CarrierSystemUnavailableException, GeneralException {
        System.out.println("---------------antes executeTopUpKddi-----------");
        GeneralTopUpResponse response = new GeneralTopUpResponse();
        RechargeTopUpWebKDDIProxy kddiProxy = new RechargeTopUpWebKDDIProxy();
        String tucu = (topUpProduct.getReferenceCode().split(","))[1];
        String trc = (topUpProduct.getReferenceCode().split(","))[0];
        String amountProvider = (topUpProduct.getReferenceCode().split(","))[2];
        Long amountRealProvider= Long.valueOf(amountProvider);
        System.out.println("---------------executeTopUpKddi-----------");
        if (topUpProduct == null) {
            throw new NullParameterException("Parameter topUpProduct cannot be null");
        } else if (senderNumber == null) {
            throw new NullParameterException("Parameter senderNumber cannot be null");
        } else if (phoneNumber == null) {
            throw new NullParameterException("Parameter phoneNumber cannot be null");
        }
        System.out.println("Realizando la Invocacion del Web services");
        try {
            String codeKDDI = kddiProxy.recharge(tucu, trc, phoneNumber,amountRealProvider);
            System.out.println("Codigo KDDI" + codeKDDI);
            if (codeKDDI.equals(Constants.TRANSACTION_SUCCESSFUL)) {
                if (sendSMS) {
                    String message1 = ServiceSMSDispatcher.sendSenderSMS(topUpProduct.getProductDenomination().getAmount(), senderNumber, phoneNumber, null, account, languageId, externalSMSName);
                    String message2 = ServiceSMSDispatcher.sendDestinationSMS(topUpProduct.getProductDenomination().getAmount(), senderNumber, phoneNumber, null, account, languageId, topUpProduct.getMobileOperator().getCountry().getId().equals(Country.USA), externalSMSName);
                    response.setSenderSMS(message1);
                    response.setDestinationSMS(message2);
                }
                response.setAmount(topUpProduct.getProductDenomination().getAmount());
                response.setResponseCode(codeKDDI);
                response.setMessage("TRANSACTION SUCCESSFUL");
                response.setDestinationNumber(phoneNumber);
                response.setSenderNumber(senderNumber);
//                response.setExternalId("EXTERNAL_ID, localref: " + kDDITopUpResponse.getLocalReference() + "dateRef: " + kDDITopUpResponse.getDateReference());
                response.setExternalId("EXTERNAL_ID, localref: " + codeKDDI + "dateRef: " + (new java.util.Date()).getTime());
            }else if (codeKDDI.equals(Constants.ERROR_PHONE_NUMBER)) {
                throw new InvalidPhoneNumberException("Invalid Phone Number");
            }else if (codeKDDI.equals(Constants.INVALID_PRODUCT)) {
                throw new TopUpProductNotAvailableException("Invalid Top UP Product");
            }else if (codeKDDI.equals(Constants.TRANSACTION_AMOUNT_INVALID)) {
                 throw new InvalidAmountException("Invalid Amount");
            }else{
                throw new GeneralException("General Exception");
            }
        } catch (RemoteException ex) {
            ex.printStackTrace();
           throw new GeneralException(ex.getMessage() + ex.getMessage());
        }
        return response;
    }
    
    private GeneralTopUpResponse getTopUpTestReponse(TopUpProduct topUpProduct, String destinationNumber, String externalId, String senderNumber, Account account, Long languageId, boolean sendSMS, String externalSMSName) {

        GeneralTopUpResponse response = new GeneralTopUpResponse();
        response.setAccount(account);
        response.setAccountName(account.getFullName());
        response.setAmount(topUpProduct.getProductDenomination().getAmount());
        response.setTrasactionDate(new Date().toString());
        Long millis = (new Date()).getTime();
        response.setAuthenticationKey("TEST : " + millis);
        response.setCompleteResponse("Transacción de Prueba Satisfactoria");
        response.setDestinationNumber(destinationNumber);
        response.setSenderNumber(senderNumber);
        response.setExternalId(externalId);
        response.setSenderSMS(senderNumber);
        response.setDestinationSMS(destinationNumber);
        response.setAdditionalMessage("Mensaje adiconal de prueba");
        return response;
    }


    public GeneralTopUpResponse executeBackUpTopUp(TopUpProduct topUpProduct, String phoneNumber, String externalId, String senderNumber, Account account, Long languageId, boolean sendSMS, String externalSMSName) throws TopUpTransactionException, GeneralException, DestinationNotPrepaidException {
        System.out.println("---------------executeBackUpTopUp-----------");
        List<TopUpProduct> topUpProducts = this.getAlternativeTopUpProducts(topUpProduct);

        if (topUpProducts != null && !topUpProducts.isEmpty()) {
            String error = "";
            int attempt = 0;
            int maxAttempts = topUpProducts.size();
            for (TopUpProduct tup : topUpProducts) {
                attempt++;
                if (attempt <= maxAttempts) {
                    System.out.println("attempt: " + attempt);
                    try {
                        return this.executeTopUp(tup, phoneNumber, externalId, senderNumber, account, languageId, sendSMS, externalSMSName);
                    } catch (Exception ex) {
                        error = ex.getMessage();
                    }
                } else {
                    throw new TopUpTransactionException(error);
                }
            }
        }
        throw new GeneralException("Error - executeBackUpTopUp");
    }

    public TopUpProduct loadTopUpProductById(Long topUpProductId) throws GeneralException, RegisterNotFoundException, NullParameterException {
        if (topUpProductId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "topUpProductId"), null);
        }
        TopUpProduct topUpProduct = null;
        try {
            Query query = createQuery("SELECT t FROM TopUpProduct t WHERE t.id = ?1");
            query.setParameter("1", topUpProductId);
            topUpProduct = (TopUpProduct) query.setHint("toplink.refresh", "true").getSingleResult();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        if (topUpProduct == null) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return topUpProduct;
    }

    public float getTopUpCalculationByAccountId(Long accountId) throws GeneralException, RegisterNotFoundException, NullParameterException {
        if (accountId == null) {
            throw new NullParameterException("Parameter accountId cannot be null");
        }
        TopUpCalculation topUpCalculation = new TopUpCalculation();
        try {
            Query query = createQuery("SELECT t FROM TopUpCalculation t WHERE t.account.id = ?1 AND t.endingDate IS NULL");
            query.setParameter("1", accountId);
            topUpCalculation = (TopUpCalculation) query.getSingleResult();
        } catch (NoResultException ex) {
            return 0f;
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        return topUpCalculation.getPercent();
    }

    public List<TopUpCalculation> getTopUpsCalculationByAccountId(Long accountId) throws GeneralException, NullParameterException, EmptyListException {
        if (accountId == null) {
            throw new NullParameterException("Parameter accountId cannot be null");
        }
        List<TopUpCalculation> topUpCalculations = new ArrayList<TopUpCalculation>();
        String sql = "SELECT t FROM TopUpCalculation t WHERE t.account.id = ?1";
        Query query = null;
        try {
            query = createQuery(sql);
            query.setParameter("1", accountId);
            topUpCalculations = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        if (topUpCalculations.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return topUpCalculations;
    }

    public TopUpCalculation loadTopUpCalculationByAccountId(Long accountId) throws NullParameterException, RegisterNotFoundException, GeneralException{
        if (accountId == null) {
            throw new NullParameterException("Parameter accountId cannot be null");
        }
        TopUpCalculation topUpCalculation = new TopUpCalculation();
        try {
            Query query = createQuery("SELECT t FROM TopUpCalculation t WHERE t.account.id = ?1 AND t.endingDate IS NULL");
            query.setParameter("1", accountId);
            topUpCalculation = (TopUpCalculation) query.getSingleResult();
        } catch (NoResultException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        return topUpCalculation;
    }

    public MobileOperatorHasProvider loadMobileOperatorHasProvider(Long mobileOperatorId, Long providerId) throws NullParameterException, RegisterNotFoundException, GeneralException {
        if (providerId == null) {
            throw new NullParameterException("parameter providerId cannot be null.");
        }else if (mobileOperatorId == null) {
            throw new NullParameterException("parameter mobileOperatorId cannot be null.");
        }
        MobileOperatorHasProvider mobileOperatorHasProvider = null;
        String sql = "SELECT mohp FROM MobileOperatorHasProvider mohp WHERE mohp.mobileOperator.id=" + mobileOperatorId + " AND mohp.provider.id=" + providerId;
        try {
            mobileOperatorHasProvider = (MobileOperatorHasProvider) createQuery(sql).setHint("toplink.refresh", "true").getSingleResult();
        } catch (NoResultException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, MobileOperator.class.getSimpleName(), getMethodName(), MobileOperator.class.getSimpleName(), null), ex);
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        return mobileOperatorHasProvider;

    }

    public NautaProduct saveNautaProduct(EJBRequest request) throws GeneralException, NullParameterException {
        return (NautaProduct) saveEntity(request, logger, getMethodName());
    }

    public NautaProduct loadNautaProductById(Long nautaId) throws GeneralException, RegisterNotFoundException, NullParameterException {
       if (nautaId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "nautaId"), null);
        }
        NautaProduct nautaProduct = new NautaProduct();
        //String sql = "SELECT m FROM MobileOperator m WHERE m.id = ?1";
        System.out.println(" ------------ id" + nautaId);
        try {
            Query query = createQuery("SELECT n FROM NautaProduct n WHERE n.id = ?1");
            query.setParameter("1", nautaId);
            nautaProduct = (NautaProduct) query.getSingleResult();
        }  catch (NoResultException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_REGISTER_NOT_FOUND_EXCEPTION, this.getClass(), getMethodName()), null);
        }catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        return nautaProduct;
    }

    public List<NautaProduct> getNautaProduct() throws NullParameterException, EmptyListException, GeneralException {
        List<NautaProduct> productId = new ArrayList<NautaProduct>();
        //String sql = "SELECT DISTINCT m.country FROM MobileOperator m WHERE m.enabled = TRUE ORDER BY m.country.name";
        String sql = "SELECT np FROM NautaProduct np WHERE np.enabled = TRUE  ORDER BY np.id";
        Query query = null;
        try {
            query = createQuery(sql);
            productId = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        if (productId.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return productId;
    }

	
}
