package com.alodiga.services.provider.ejb;

import com.alodiga.services.provider.commons.ejbs.BannerEJB;
import com.alodiga.services.provider.commons.ejbs.BannerEJBLocal;
import com.alodiga.services.provider.commons.genericEJB.AbstractSPEJB;
import com.alodiga.services.provider.commons.genericEJB.SPContextInterceptor;
import com.alodiga.services.provider.commons.genericEJB.SPLoggerInterceptor;
import com.alodiga.services.provider.commons.utils.EjbConstants;
import com.alodiga.services.provider.commons.exceptions.EmptyListException;
import com.alodiga.services.provider.commons.exceptions.GeneralException;
import com.alodiga.services.provider.commons.exceptions.NullParameterException;
import com.alodiga.services.provider.commons.exceptions.RegisterNotFoundException;
import com.alodiga.services.provider.commons.genericEJB.EJBRequest;
import com.alodiga.services.provider.commons.models.Banner;
import com.alodiga.services.provider.commons.models.BannerType;
import com.alodiga.services.provider.commons.utils.QueryConstants;
import java.util.List;
import java.util.Map;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import org.apache.log4j.Logger;
import java.util.ArrayList;
import java.util.HashMap;
import javax.interceptor.Interceptors;
import javax.persistence.Query;

@Interceptors({SPLoggerInterceptor.class, SPContextInterceptor.class})
@Stateless(name = EjbConstants.BANNER_EJB, mappedName = EjbConstants.BANNER_EJB)
@TransactionManagement(TransactionManagementType.BEAN)
public class BannerEJBImp extends AbstractSPEJB implements BannerEJB, BannerEJBLocal {

    private static final Logger logger = Logger.getLogger(BannerEJBImp.class);

    public List<Banner> getBanners(EJBRequest request) throws GeneralException, RegisterNotFoundException, NullParameterException, EmptyListException {
        return (List<Banner>) listEntities(Banner.class, request, logger, getMethodName());
    }
    
    public List<BannerType> getBannerTypes() throws GeneralException, EmptyListException{
        List<BannerType> bannerTypes = new ArrayList<BannerType>();
        try {
            Query query = createQuery("SELECT bt FROM BannerType bt WHERE bt.enabled = TRUE");
            bannerTypes = query.setHint("toplink.refresh", "true").getResultList();
        } catch (Exception e) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
        }
        return bannerTypes;
    }

    public List<Banner> getBannersByPosition(EJBRequest request) throws GeneralException, EmptyListException, NullParameterException {
        List<Banner> banners = null;
        Map<String, Object> params = request.getParams();


        if (!params.containsKey(QueryConstants.PARAM_BANNERS_BY_POSITION)) {
            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "position"), null);
        }
        banners = (List<Banner>) getNamedQueryResult(Banner.class, QueryConstants.BANNERS_BY_POSITION, request, getMethodName(), logger, "banner");
        return banners;
    }

    public List<Banner> getBannersByType(Long bannerTypeId) throws GeneralException, EmptyListException, NullParameterException {
        List<Banner> banners = null;
        if (bannerTypeId == null) {
            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "bannerTypeId"), null);
        }
        EJBRequest request = new EJBRequest();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(QueryConstants.PARAM_BANNER_TYPE_ID, bannerTypeId);
        request.setParams(params);
        banners = (List<Banner>) getNamedQueryResult(Banner.class, QueryConstants.BANNERS_BY_TYPE, request, getMethodName(), logger, "banner");
        return banners;
    }

    public Banner loadBanner(EJBRequest request) throws GeneralException, RegisterNotFoundException, NullParameterException {
        return (Banner) loadEntity(Banner.class, request, logger, getMethodName());
    }

    public Banner saveBanner(EJBRequest request) throws GeneralException, NullParameterException {
        return (Banner) saveEntity(request, logger, getMethodName());
    }
}
