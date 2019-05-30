package com.alodiga.services.provider.ejb;

import com.alodiga.services.provider.commons.ejbs.ProductEJB;
import com.alodiga.services.provider.commons.ejbs.ProductEJBLocal;
import com.alodiga.services.provider.commons.ejbs.SisacIntegrationEJBLocal;
import com.alodiga.services.provider.commons.ejbs.WSEJBLocal;
import com.alodiga.services.provider.commons.services.models.AccessNumberResponse;
import com.alodiga.services.provider.commons.services.models.WSConstants;
import com.alodiga.services.provider.commons.exceptions.AccessNumberNotfoundException;
import com.alodiga.services.provider.commons.exceptions.EmptyListException;
import com.alodiga.services.provider.commons.exceptions.GeneralException;
import com.alodiga.services.provider.commons.exceptions.NullParameterException;
import com.alodiga.services.provider.commons.exceptions.RegisterNotFoundException;
import com.alodiga.services.provider.commons.genericEJB.AbstractSPEJB;
import com.alodiga.services.provider.commons.genericEJB.EJBRequest;
import com.alodiga.services.provider.commons.models.Period;
import com.alodiga.services.provider.commons.models.Category;
import com.alodiga.services.provider.commons.models.Pin;
import com.alodiga.services.provider.commons.models.Product;
import com.alodiga.services.provider.commons.models.ProductData;
import com.alodiga.services.provider.commons.models.ProductDenomination;
import com.alodiga.services.provider.commons.models.Provider;
import com.alodiga.services.provider.commons.utils.EjbConstants;
import com.alodiga.services.provider.commons.utils.QueryConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import org.apache.log4j.Logger;

@Stateless(name = EjbConstants.PRODUCT_EJB, mappedName = EjbConstants.PRODUCT_EJB)
@TransactionManagement(TransactionManagementType.BEAN)
public class ProductEJBImp extends AbstractSPEJB implements ProductEJB, ProductEJBLocal {

    private static final Logger logger = Logger.getLogger(ProductEJBImp.class);
    @EJB
    private WSEJBLocal wsEJB;
    @EJB
    private SisacIntegrationEJBLocal sisacIntegrationEJB;

    public Category deleteCategory(EJBRequest request) throws GeneralException, NullParameterException {
        return null;
    }

    public Product deleteProduct(EJBRequest request) throws GeneralException, NullParameterException {
        return null;
    }

    public void deleteProductDenomination(EJBRequest request) throws NullParameterException, GeneralException {
        Object param = request.getParam();
        if (param == null || !(param instanceof Long)) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "productId"), null);
        }
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("productId", (Long) param);
        try {
            executeNameQuery(ProductDenomination.class, QueryConstants.DELETE_PRODUCT_DENOMINATION, map, getMethodName(), logger, "ProductDenomination", null, null);
        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }
    }

    public void deleteProductHasProvider(Long productId) throws NullParameterException, GeneralException {
        if (productId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "productId or providerId"), null);
        }
        try {
            EntityTransaction transaction = entityManager.getTransaction();
            transaction.begin();
            //String sql = "DELETE FROM ProductHasProvider php WHERE php.product.id=" + productId;
            StringBuilder sqlBuilder = new StringBuilder("DELETE FROM ProductHasProvider php WHERE php.product.id=?1");
            Query query = createQuery(sqlBuilder.toString());
            query.setParameter("1", productId);
            query.executeUpdate();
            transaction.commit();
        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }
    }

    public void deletePromotionTypeHasPromotion(EJBRequest request) throws NullParameterException, GeneralException {
        Object param = request.getParam();
        if (param == null || !(param instanceof Long)) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "promotionId"), null);
        }
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("promotionId", (Long) param);
        try {
            //executeNameQuery(PromotionTypeHasPromotion.class, QueryConstants.DELETE_PROMOTION_TYPE_HAS_PROMOTION, map, getMethodName(), logger, "PromotionTypeHasPromotion", null, null);
        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }
    }

    public Provider deleteProvider(EJBRequest request) throws GeneralException, NullParameterException {
        return null;
    }

    public Boolean deletePinFree(EJBRequest request) throws GeneralException, NullParameterException {
        return (Boolean) removeEntity(request, logger, getMethodName());
    }

    public Product enableProduct(EJBRequest request) throws GeneralException, NullParameterException, RegisterNotFoundException {
        return (Product) saveEntity(request, logger, getMethodName());
    }

    public List<Product> filterProducts(EJBRequest request) throws GeneralException, EmptyListException, NullParameterException {
        if (request == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "request"), null);
        }

        Map<String, Object> params = request.getParams();
        if (params == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "params"), null);
        }
        Boolean isFilter = true;
        Map orderField = new HashMap();
        orderField.put(Product.NAME, QueryConstants.ORDER_DESC);
        return (List<Product>) createSearchQuery(Product.class, request, orderField, logger, getMethodName(), "customers", isFilter);
    }

    public AccessNumberResponse getAccesNumberByAni(EJBRequest request) throws GeneralException, NullParameterException, EmptyListException, AccessNumberNotfoundException {
        AccessNumberResponse accessNumberResponse = new AccessNumberResponse();
        Map<String, Object> params = request.getParams();

        EJBRequest requestAccesNumber = new EJBRequest();
        Map<String, Object> requestParams = new HashMap<String, Object>();

        if (!params.containsKey(WSConstants.PARAM_ANI)) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_ANI), null);
        }

        if (!params.containsKey(WSConstants.PARAM_CUSTOM_SERVICE_ID)) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_CUSTOM_SERVICE_ID), null);
        }

        if (!params.containsKey(WSConstants.PARAM_COUNT_ACCESS_NUMBER)) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_COUNT_ACCESS_NUMBER), null);
        }

        requestParams.put(WSConstants.PARAM_ANI, params.get(WSConstants.PARAM_ANI).toString());
        requestParams.put(WSConstants.PARAM_CUSTOM_SERVICE_ID, params.get(WSConstants.PARAM_CUSTOM_SERVICE_ID));
        requestParams.put(WSConstants.PARAM_COUNT_ACCESS_NUMBER, params.get(WSConstants.PARAM_COUNT_ACCESS_NUMBER));
        requestAccesNumber.setParams(requestParams);
        try {
            accessNumberResponse = wsEJB.getAccesNumberByAni(requestAccesNumber);
        } catch (AccessNumberNotfoundException e) {
            throw new AccessNumberNotfoundException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), null);
        }
        return accessNumberResponse;
    }

    public List<Category> getCategories(EJBRequest request) throws GeneralException, EmptyListException, NullParameterException {

        return (List<Category>) listEntities(Category.class, request, logger, getMethodName());
    }

    public List<Float> getDenominationsByCategoryId(EJBRequest request) throws GeneralException, NullParameterException, EmptyListException {
        List<Float> denominations = new ArrayList<Float>();
        Object param = request.getParam();
        if (param == null || !(param instanceof Long)) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "category"), null);
        }
//        String sql = "SELECT DISTINCT pd.amount FROM ProductDenomination pd WHERE pd.product.category.id= " + param + "ORDER BY pd.amount ASC";
        StringBuilder sqlBuilder = new StringBuilder("SELECT DISTINCT pd.amount FROM ProductDenomination pd WHERE pd.product.category.id=?1 ORDER BY pd.amount ASC");
        Query query = null;
        try {
            query = createQuery(sqlBuilder.toString());
            query.setParameter("1", param);
            denominations = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }
        return denominations;
    }

    public List<Product> getProducts(EJBRequest request) throws GeneralException, EmptyListException, NullParameterException {

        return (List<Product>) listEntities(Product.class, request, logger, getMethodName());
    }

    public List<Product> getProductsByEnterprise(Long enterpriseId) throws GeneralException, EmptyListException, NullParameterException {
        List<Product> products = null;

        if (enterpriseId == null || enterpriseId.equals("")) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "enterpriseId"), null);
        }
        //
        Query query = null;
        try {
            query = createQuery("SELECT p FROM Product p WHERE p.enterprise.id = ?1");
            query.setParameter("1", enterpriseId);
            products = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }
        if (products.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return products;
    }

    public List<Provider> getProviders(EJBRequest request) throws GeneralException, EmptyListException, NullParameterException {

        return (List<Provider>) listEntities(Provider.class, request, logger, getMethodName());
    }

    public List<Provider> getSMSProviders(EJBRequest request) throws GeneralException, EmptyListException, NullParameterException {

        List<Provider> providers = null;
        //
        Query query = null;
        try {
            query = createQuery("SELECT p FROM Provider p WHERE p.isSMSProvider=1 AND p.enabled=1");

            if (request.getLimit() != null && request.getLimit() > 0) {
                query.setMaxResults(request.getLimit());
            } else {
                query.setMaxResults(20);
            }

            providers = query.setHint("toplink.refresh", "true").getResultList();

        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }
        if (providers.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return providers;
    }

    public Float getPercentDiscount(Long levelId, Long productId) throws GeneralException, NullParameterException {
        if (levelId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "levelId"), null);
        }
        if (productId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "productId"), null);
        }
        Float discount = 0F;
        try {
            Query query = createQuery("SELECT lhp FROM LevelHasProduct lhp WHERE lhp.endingDate IS NULL AND lhp.level.id=?1 AND lhp.product.id=?2 ");
            query.setParameter("1", levelId);
            query.setParameter("2", productId);
//            discount = ((LevelHasProduct) query.getSingleResult()).getDiscountPercent();
        } catch (Exception ex) {
            ex.getMessage();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        return discount;
    }

    public Category loadCategory(EJBRequest request) throws GeneralException, RegisterNotFoundException, NullParameterException {

        return (Category) loadEntity(Category.class, request, logger, getMethodName());
    }

    public Pin loadPinByAni(String ani) throws RegisterNotFoundException, GeneralException, NullParameterException {
        Pin pin = null;
        if (ani == null || ani.equals("")) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "ani"), null);
        }
        //String sql = "SELECT pFree.pin FROM PinFree pFree WHERE pFree.ani=:ani";
        StringBuilder sqlBuilder = new StringBuilder("SELECT pFree.pin FROM PinFree pFree WHERE pFree.ani=?1");
        Query query = null;
        try {
            query = createQuery(sqlBuilder.toString());
            query.setParameter("1", ani);
            pin = (Pin) query.getSingleResult();
        } catch (NoResultException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        } catch (Exception ex) {
            ex.getMessage();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        return pin;

    }

    public Pin loadPinBySerial(String serial) throws RegisterNotFoundException, GeneralException, NullParameterException {
        Pin pin = null;
        if (serial == null || serial.equals("")) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "serial"), null);
        }
        //String sql = "SELECT p FROM Pin p WHERE p.serial=:serial";
        StringBuilder sqlBuilder = new StringBuilder("SELECT p FROM Pin p WHERE p.serial=?1");
        Query query = null;
        try {
            query = createQuery(sqlBuilder.toString());
            query.setParameter("1", serial);
            pin = (Pin) query.getSingleResult();
        } catch (NoResultException ex) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        } catch (Exception ex) {
            ex.getMessage();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        return pin;

    }

    public Pin loadPinByTransactionItemId(Long transactionItemId) throws RegisterNotFoundException, GeneralException, NullParameterException {

        Pin pin = null;
        if (transactionItemId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "transactionItemId"), null);
        }
        //String sql = "SELECT p FROM Pin p WHERE p.transactionItem.id=?1";
        StringBuilder sqlBuilder = new StringBuilder("SELECT p FROM Pin p WHERE p.transactionItem.id=?1");
        Query query = null;
        try {
            query = createQuery(sqlBuilder.toString());
            query.setParameter("1", transactionItemId);
            pin = (Pin) query.getSingleResult();
        } catch (NoResultException ex) {
            //ex.printStackTrace();
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        return pin;
    }

    public Product loadProduct(EJBRequest request) throws GeneralException, RegisterNotFoundException, NullParameterException {
        return (Product) loadEntity(Product.class, request, logger, getMethodName());
    }

    public Product loadProductById(Long productId) throws GeneralException, RegisterNotFoundException, NullParameterException {
        if (productId == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "productId"), null);
        }
        Product product = new Product();
        try {
            Query query = createQuery("SELECT p FROM Product p WHERE p.id = ?1");
            query.setParameter("1", productId);
            product = (Product) query.getSingleResult();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        if (product == null) {
            throw new RegisterNotFoundException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return product;
    }

    public ProductDenomination loadProductDenominationById(Long id) throws NullParameterException, EmptyListException, GeneralException {
        if (id == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "providerId"), null);
        }
        ProductDenomination productDenomination = new ProductDenomination();
        StringBuilder sqlBuilder = new StringBuilder("SELECT pn FROM ProductDenomination pn WHERE pn.id = ?1");
        Query query = null;
        try {

            query = createQuery(sqlBuilder.toString());
            query.setParameter("1", id);
            productDenomination = (ProductDenomination) query.getSingleResult();
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
        if (productDenomination == null) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return productDenomination;
    }

    public Provider loadProvider(EJBRequest request) throws GeneralException, RegisterNotFoundException, NullParameterException {

        return (Provider) loadEntity(Provider.class, request, logger, getMethodName());
    }

    public Category saveCategory(EJBRequest request) throws GeneralException, NullParameterException {
        return (Category) saveEntity(request, logger, getMethodName());
    }

    public Product saveProduct(EJBRequest request) throws GeneralException, NullParameterException {

        return (Product) saveEntity(request, logger, getMethodName());
    }

    public ProductData saveProductData(EJBRequest request) throws GeneralException, NullParameterException {
        return (ProductData) saveEntity(request, logger, getMethodName());
    }

    public Pin savePin(EJBRequest request) throws GeneralException, NullParameterException {

        return (Pin) saveEntity(request, logger, getMethodName());
    }

    public Provider saveProvider(EJBRequest request) throws GeneralException, NullParameterException {

        return (Provider) saveEntity(request, logger, getMethodName());
    }

    public List<Period> getPeriods(EJBRequest request) throws GeneralException, EmptyListException, NullParameterException {

        return (List<Period>) listEntities(Period.class, request, logger, getMethodName());

    }

    public void deletePinFree(String ani) throws NullParameterException, GeneralException {

        if (ani == null) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "ani"), null);
        }

        try {
            EntityTransaction transaction = entityManager.getTransaction();
            transaction.begin();
            Query query = createQuery("UPDATE PinFree p SET p.enabled= 0 WHERE p.ani = ?1");
            query.setParameter("1", ani);
            query.executeUpdate();
            transaction.commit();
            sisacIntegrationEJB.deletePinFree(ani);
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), null);
        }
    }

    public List<Product> getProductsByAccount(EJBRequest request) throws GeneralException, NullParameterException, EmptyListException {
        List<Product> products = null;

        Map<String, Object> params = request.getParams();
        StringBuilder sqlBuilder = new StringBuilder("SELECT DISTINCT ahp.product FROM AccountProduct ahp WHERE ahp.account.id =?1");
        //String sql = "SELECT DISTINCT ahp.product FROM AccountProduct ahp WHERE ahp.account.id =:accountId";
        if (!params.containsKey(QueryConstants.PARAM_ACCOUNT)) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), params.containsKey(QueryConstants.PARAM_ACCOUNT)), null);
        }
        Query query = null;
        try {
            if (params.containsKey(QueryConstants.PARAM_ENDED) && params.get(QueryConstants.PARAM_ENDED).equals(true)) {
                sqlBuilder.append(" AND ahp.endingDate IS NOT NULL");
                //sql += " AND ahp.endingDate IS NOT NULL";
            }
            if (params.containsKey(QueryConstants.PARAM_ENDED) && params.get(QueryConstants.PARAM_ENDED).equals(false)) {
                sqlBuilder.append(" AND ahp.endingDate IS NULL");
                //sql += " AND ahp.endingDate IS NULL";
            }
            query = createQuery(sqlBuilder.toString());
            query.setParameter("1", params.get(QueryConstants.PARAM_ACCOUNT));
            if (request.getLimit() != null && request.getLimit() > 0) {
                query.setMaxResults(request.getLimit());
            }

            products = query.getResultList();
        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }
        if (products.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return products;
    }

    public List<ProductDenomination> loadProductDenominations(Long productId) throws NullParameterException, EmptyListException, GeneralException {
         List<ProductDenomination> products = null;

        if (productId == null || productId.equals("")) {
            throw new NullParameterException(sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "productId"), null);
        }
        //
        Query query = null;
        try {
            query = createQuery("SELECT p FROM ProductDenomination p WHERE p.product.id = ?1");
            query.setParameter("1", productId);
            products = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }
        if (products.isEmpty()) {
            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
        }
        return products;
    }
}
