package com.alodiga.services.provider.ejb;

import com.alodiga.integration.prepaynation.servicemanager.ServiceManager;
import com.alodiga.services.provider.commons.ejbs.TopUpProductEJBLocal;
import com.alodiga.services.provider.commons.ejbs.UtilsEJBLocal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.interceptor.Interceptors;
import org.apache.log4j.Logger;
import org.springframework.http.HttpMethod;
import com.alodiga.services.provider.commons.ejbs.WSEJB;
import com.alodiga.services.provider.commons.ejbs.WSEJBLocal;
import com.alodiga.services.provider.commons.services.models.AccessNumberResponse;
import com.alodiga.services.provider.commons.services.models.ApplicationResponse;
import com.alodiga.services.provider.commons.services.models.CallingCardPinInvoice;
import com.alodiga.services.provider.commons.services.models.CancelProvisionResponse;
import com.alodiga.services.provider.commons.services.models.ExtendedPinDataResponse;
import com.alodiga.services.provider.commons.services.models.LoginResponse;
import com.alodiga.services.provider.commons.services.models.OperationResponse;
import com.alodiga.services.provider.commons.services.models.OrderRecharge;
import com.alodiga.services.provider.commons.services.models.ProcessOrderVoidingResponse;
import com.alodiga.services.provider.commons.services.models.ProcessRechargePinResponse;
import com.alodiga.services.provider.commons.services.models.ProvisionAniResponse;
import com.alodiga.services.provider.commons.services.models.ProvisionPinResponse;
import com.alodiga.services.provider.commons.services.models.RechargePinResponse;
import com.alodiga.services.provider.commons.services.models.WSConstants;
import com.alodiga.soap.integration.easycall.SoapClient;
import com.alodiga.soap.integration.easycall.model.DoTopUpResponse;
import com.alodiga.transferto.integration.connection.RequestManager;
import com.alodiga.transferto.integration.model.TopUpResponse;
import com.alodiga.transferto.integration.model.TopUpWalletResponse;
import com.alodiga.services.provider.commons.exceptions.AccessNumberNotfoundException;
import com.alodiga.services.provider.commons.exceptions.DestinationNotPrepaidException;
import com.alodiga.services.provider.commons.exceptions.EmptyListException;
import com.alodiga.services.provider.commons.exceptions.GeneralException;
import com.alodiga.services.provider.commons.exceptions.InvalidFormatException;
import com.alodiga.services.provider.commons.exceptions.NullParameterException;
import com.alodiga.services.provider.commons.exceptions.PinFreeNotfoundException;
import com.alodiga.services.provider.commons.exceptions.PinFreeProvisionException;
import com.alodiga.services.provider.commons.exceptions.PinProvisionException;
import com.alodiga.services.provider.commons.exceptions.PurchaseCanceledException;
import com.alodiga.services.provider.commons.exceptions.TopUpProductNotAvailableException;
import com.alodiga.services.provider.commons.exceptions.TopUpTransactionException;
import com.alodiga.services.provider.commons.genericEJB.AbstractSPRESTEJB;
import com.alodiga.services.provider.commons.genericEJB.SPContextInterceptor;
import com.alodiga.services.provider.commons.genericEJB.SPLoggerInterceptor;
import com.alodiga.services.provider.commons.genericEJB.EJBRequest;
import com.alodiga.services.provider.commons.models.Account;
import com.alodiga.services.provider.commons.models.Address;
import com.alodiga.services.provider.commons.models.Customer;
import com.alodiga.services.provider.commons.models.Language;
import com.alodiga.services.provider.commons.models.Country;
import com.alodiga.services.provider.commons.models.SMS;
import com.alodiga.services.provider.commons.models.PaymentInfo;
import com.alodiga.services.provider.commons.models.TopUpProduct;
import com.alodiga.services.provider.commons.models.TopUpResponseConstants;
import com.alodiga.services.provider.commons.models.Transaction;
import com.alodiga.services.provider.commons.services.models.PurchaseBalanceAccount;
import com.alodiga.services.provider.commons.utils.DistributionWSMediaType;
import com.alodiga.services.provider.commons.utils.DocumentHandler;
import com.alodiga.services.provider.commons.utils.EjbConstants;
import com.alodiga.services.provider.commons.utils.QueryConstants;
import com.pininteract.OrderResponse;
import java.util.Calendar;
import java.util.Iterator;
import javax.ejb.EJB;

@Interceptors({SPLoggerInterceptor.class, SPContextInterceptor.class})
@Stateless(name = EjbConstants.WS_EJB, mappedName = EjbConstants.WS_EJB)
@TransactionManagement(TransactionManagementType.BEAN)
public class WSEJBImp extends AbstractSPRESTEJB implements WSEJB, WSEJBLocal {

    public String authenticate(EJBRequest request) throws NullParameterException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public CancelProvisionResponse deletePinFree(EJBRequest request) throws NullParameterException, GeneralException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Map<String, String> doEasyCallTopUp(String productId, String senderNumber, String phoneNumber, float amount, String externalId, Account account, Long languageId) throws NullParameterException, GeneralException, TopUpTransactionException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Map<String, String> doTransferToTopUp(TopUpProduct topUpProduct, String senderNumber, String destinationNumber, String destinationSMS, String senderSMS, Account account, Long languageId) throws NullParameterException, GeneralException, TopUpTransactionException, TopUpProductNotAvailableException, InvalidFormatException, DestinationNotPrepaidException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Map<String, String> doPrepayNationTopUp(TopUpProduct topUpProduct, String senderNumber, String destinationNumber, String destinationSMS, String senderSMS, Account account, Long languageId) throws NullParameterException, GeneralException, TopUpTransactionException, InvalidFormatException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public AccessNumberResponse getAccesNumberByAni(EJBRequest request) throws NullParameterException, GeneralException, AccessNumberNotfoundException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<CallingCardPinInvoice> getCallHistory(EJBRequest request) throws NullParameterException, EmptyListException, GeneralException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Map<String, String> getCheckWallet() throws NullParameterException, GeneralException, TopUpTransactionException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ExtendedPinDataResponse getPinfreeByAni(Long languageId, String ani) throws NullParameterException, GeneralException, PinFreeNotfoundException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public RechargePinResponse preprocessRechargePin(EJBRequest request) throws NullParameterException, GeneralException, PurchaseCanceledException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ProcessRechargePinResponse processRechargePin(EJBRequest request) throws NullParameterException, GeneralException, PurchaseCanceledException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public PurchaseBalanceAccount purchaseBalanceAccount(EJBRequest request) throws NullParameterException, GeneralException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ProvisionPinResponse purchasePin(EJBRequest request) throws NullParameterException, GeneralException, PurchaseCanceledException, PinFreeProvisionException, PinProvisionException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OrderRecharge rechargePinByAccount(EJBRequest request) throws NullParameterException, GeneralException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OperationResponse searchOperations(EJBRequest request) throws NullParameterException, GeneralException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ProvisionPinResponse updatePin(EJBRequest request) throws NullParameterException, GeneralException, PurchaseCanceledException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ProcessRechargePinResponse cancelPinRecharge(EJBRequest request) throws NullParameterException, GeneralException, PurchaseCanceledException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ProcessOrderVoidingResponse cancelPurchaseBalance(EJBRequest request) throws NullParameterException, GeneralException, PurchaseCanceledException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OrderRecharge rechargePinByDistributor(EJBRequest request) throws NullParameterException, GeneralException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public PurchaseBalanceAccount purchaseBalanceDistributor(EJBRequest request) throws NullParameterException, GeneralException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

//    private static final Logger logger = Logger.getLogger(WSEJBImp.class);
//    public static String userToken;
//    @EJB
//    private TopUpProductEJBLocal topUpProductEJB;
//    @EJB
//    private UtilsEJBLocal utilsEJB;
//
//    @Override
//    public String authenticate(EJBRequest request) throws NullParameterException {
//        Map<String, Object> params = request.getParams();
//        if (params == null) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "params"), null);
//        }
//
//        if (!params.containsKey(WSConstants.PARAM_LANGUAGE_ID)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_LANGUAGE_ID), null);
//        }
//        params.put(WSConstants.PARAM_LOGIN, WSConstants.WEB_USER_SISAC);
//        params.put(WSConstants.PARAM_PASSWORD, WSConstants.PASSWORD_SISAC);
//        params.put(WSConstants.PARAM_ENTERPRISE_ID, WSConstants.ENTERPRISE_ID.toString());
//        request.setParams(params);
//        request.setMediaType(new DistributionWSMediaType());
//        request.setUrl(WSConstants.URL_VALIDATE_USER_SERVICE);
//        request.setMethod(HttpMethod.POST);
//
//        String xmlResponse = loadData(getClass(), request, logger, getMethodName());
//
//        LoginResponse loginResponse = null;
//        try {
//            loginResponse = (LoginResponse) DocumentHandler.objectFromXml(xmlResponse, LoginResponse.class);
//
//        } catch (Exception ex) {
//            ex.printStackTrace();
//
//        }
//
//        userToken = loginResponse.getUserId();
//
//        return userToken;
//    }
//
//    @Override
//    public CancelProvisionResponse deletePinFree(EJBRequest request) throws NullParameterException, GeneralException {
//        Map<String, Object> params = request.getParams();
//
//        EJBRequest requestDelete = new EJBRequest();
//        Map<String, Object> requestParams = new HashMap<String, Object>();
//
//        if (!params.containsKey(WSConstants.PARAM_LANGUAGE_ID)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_LANGUAGE_ID), null);
//        }
//
//        if (!params.containsKey(WSConstants.PARAM_SERIAL)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_SERIAL), null);
//        }
//
//        if (!params.containsKey(WSConstants.PARAM_ANI_LIST)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_ANI_LIST), null);
//        }
//
//        requestParams.put(WSConstants.PARAM_LANGUAGE_ID, params.get(WSConstants.PARAM_LANGUAGE_ID).toString());
//        requestParams.put(WSConstants.PARAM_SERIAL, params.get(WSConstants.PARAM_SERIAL).toString());
//        requestParams.put(WSConstants.PARAM_ANI_LIST, params.get(WSConstants.PARAM_ANI_LIST).toString());
//        requestParams.put(WSConstants.PARAM_ORDER_ID, params.get(WSConstants.PARAM_ORDER_ID).toString());
//
//        requestDelete.setParams(requestParams);
//        requestDelete.setMediaType(new DistributionWSMediaType());
//        requestDelete.setUrl(WSConstants.URL_DELETE_PIN);
//        requestDelete.setMethod(HttpMethod.POST);
//
//        String response = loadData(getClass(), requestDelete, logger, getMethodName());
//        CancelProvisionResponse cancelProvisionResponse = (CancelProvisionResponse) DocumentHandler.objectFromXml(response, CancelProvisionResponse.class);
//
//        if (cancelProvisionResponse.getErrors() != null && !cancelProvisionResponse.getErrors().isEmpty()) {
//            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PROCESS_RECHARGE, this.getClass(), getMethodName(), null), null);
//        }
//
//
//        return cancelProvisionResponse;
//    }
//
//    @Override
//    public Map<String, String> doEasyCallTopUp(String productId, String senderNumber, String phoneNumber, float amount, String externalId, Account account, Long languageId) throws NullParameterException, GeneralException, TopUpTransactionException {
//        System.out.println("------------------- doEasyCallTopUp ------------------------");
//        if (productId == null || phoneNumber == null || externalId == null) {
//            throw new NullParameterException("productId - phoneNumber - ammount - externalId cannot be null");
//        }
//        Map<String, String> response = new HashMap<String, String>();
//        String error = "";
//        try {
//            String _amount = String.valueOf(amount);
//            DoTopUpResponse doTopUpResponse = SoapClient.doTopUp(productId, phoneNumber, _amount, externalId);
//            response.put(TopUpResponseConstants.CODE, String.valueOf(doTopUpResponse.getCode()));
//            String code = response.get(TopUpResponseConstants.CODE);
//            if (!code.equals("0")) {
//                StringBuilder errorBuilder = new StringBuilder(TopUpResponseConstants.EASY_CALL_CODES.get(code));
//                errorBuilder.append("Integrator = ").append("EasyCall").append(" ProductId = ").append(productId).append(" phoneNumber = ").append(phoneNumber).append(" amount = ").append(amount).append(" externalId = ").append(externalId);
//                error = errorBuilder.toString();
//                System.out.println("Top_up error" + error);
//                throw new TopUpTransactionException(error);
//            }
//            response.put(TopUpResponseConstants.ADDITIONAL_MESSAGE, doTopUpResponse.getAdditionalMessage());
//            response.put(TopUpResponseConstants.MESSAGE, doTopUpResponse.getMessage());
//            response.put(TopUpResponseConstants.RETAILER_ID, String.valueOf(doTopUpResponse.getRetailerId()));
//            response.put(TopUpResponseConstants.TRANSACTION_DATE, doTopUpResponse.getTransationDate());
//            response.put(TopUpResponseConstants.EXTERNAL_ID, doTopUpResponse.getExternalId());
//            response.put(TopUpResponseConstants.INSTRUCTIONS, doTopUpResponse.getInstructions());
//            response.put(TopUpResponseConstants.DISCLAIMER, String.valueOf(doTopUpResponse.getDisclaimer()));
//            response.put(TopUpResponseConstants.ORDER_NUMBER, String.valueOf(doTopUpResponse.getOrderNumber()));
//            response.put(TopUpResponseConstants.DISTRIBUTOR, String.valueOf(doTopUpResponse.getAccount()));
//            response.put(TopUpResponseConstants.AMOUNT, String.valueOf(doTopUpResponse.getAmount()));
//            response.put(TopUpResponseConstants.TRANSACTION_MESSAGE, String.valueOf(doTopUpResponse.getTransactionMessage()));
//            String completeResponse = "";
//            try {
//                Iterator it = response.entrySet().iterator();
//                while (it.hasNext()) {
//                    Map.Entry e = (Map.Entry) it.next();
//                    completeResponse += e.getValue();
//                }
//            } catch (Exception e) {
//                completeResponse += e.getMessage();
//                e.printStackTrace();
//            }
//            String message1 = sendSenderSMS(amount, senderNumber, phoneNumber, null, account, languageId);
//            String message2 = sendDestinationSMS(amount, senderNumber, phoneNumber, null, account, languageId);
//            sendTopUpMail(account, message1, message2);
//            response.put(TopUpResponseConstants.COMPLETE_RESPONSE, completeResponse);
//        } catch (Exception ex) {
//            throw new TopUpTransactionException(ex.getMessage() + error);
//        }
//        return response;
//    }
//
//    @Override
//    public Map<String, String> doTransferToTopUp(TopUpProduct topUpProduct, String senderNumber, String destinationNumber, String destinationSMS, String senderSMS, Account account, Long languageId) throws NullParameterException, GeneralException, TopUpTransactionException, TopUpProductNotAvailableException, InvalidFormatException, DestinationNotPrepaidException {
//        System.out.println("------------------- doTransferToTopUp ------------------------");
//        Map<String, String> response = new HashMap<String, String>();
//        String error = "";
//        try {
//            String referenceCode = topUpProductEJB.getMobileOperatorHasProvider(topUpProduct.getMobileOperator().getId(), topUpProduct.getProvider().getId()).getReferenceCode();
//            String phoneNumber = topUpProduct.getMobileOperator().getCountry().getId().equals(Country.USA) ? "1" : "";
//            phoneNumber += destinationNumber;
//            TopUpResponse topUpResponse = RequestManager.doTopUp(senderNumber, phoneNumber, topUpProduct.getReferenceCode(), Long.parseLong(referenceCode), "Powered by Alodiga", "Powered by Alodiga");
//            String code = topUpResponse.getErrorCode();
//            if (!code.equals("0")) {//Cuando es 0 esta bien...
//                StringBuilder errorBuilder = new StringBuilder(TopUpResponseConstants.TRANSFER_TO_CODES.get(code));
//                errorBuilder.append("Integrator = ").append("TransferTo").append("ProductId = ").append(topUpProduct.getId()).append("phoneNumber = ").append(destinationNumber);
//                error = errorBuilder.toString();
//
//                if (code.equals("301") || topUpResponse.getErrorText().equals("Denomination not available")) {
//                    topUpProductEJB.disableTopUpProduct(topUpProduct);
//                    throw new TopUpProductNotAvailableException(error);
//                } else if (code.equals("101") || topUpResponse.getErrorText().equals("Destination MSISDN out of range")) {
//                    throw new InvalidFormatException(error);
//                } else if (code.equals("204")) {
//                    throw new DestinationNotPrepaidException(error);
//                }
//                throw new TopUpTransactionException(error);
//            }
//
//            String message1 = sendSenderSMS(topUpProduct.getProductDenomination().getAmount(), senderNumber, phoneNumber, topUpResponse, account, languageId);
//            String message2 = sendDestinationSMS(topUpProduct.getProductDenomination().getAmount(), senderNumber, phoneNumber, topUpResponse, account, languageId);
//            sendTopUpMail(account, message1, message2);
//            response.put(TopUpResponseConstants.ERROR_CODE, topUpResponse.getErrorCode());
//            response.put(TopUpResponseConstants.ERROR_TEXT, topUpResponse.getErrorText());
//            response.put(TopUpResponseConstants.AUTHENTICATION_KEY, topUpResponse.getAuthenticationKey());
//            StringBuilder responseBuilder = new StringBuilder();
//
//            try {
//                Iterator it = response.entrySet().iterator();
//                while (it.hasNext()) {
//                    Map.Entry e = (Map.Entry) it.next();
//                    responseBuilder.append(e.getValue());
//                }
//            } catch (Exception e) {
//                responseBuilder.append(e.getMessage());
//                e.printStackTrace();
//            }
//            response.put(TopUpResponseConstants.COMPLETE_RESPONSE, responseBuilder.toString());
//        } catch (DestinationNotPrepaidException ex) {
//            throw (ex);
//        } catch (TopUpTransactionException ex) {
//            throw (ex);
//        } catch (GeneralException ex) {
//            throw (ex);
//        } catch (NullParameterException ex) {
//            throw (ex);
//        } catch (TopUpProductNotAvailableException ex) {
//            throw (ex);
//        } catch (InvalidFormatException ex) {
//            throw (ex);
//        } catch (Exception ex) {
//            throw new GeneralException(ex.getMessage() + error);
//        }
//        return response;
//    }
//
//    public Map<String, String> doPrepayNationTopUp(TopUpProduct topUpProduct, String senderNumber, String destinationNumber, String destinationSMS, String senderSMS, Account account, Long languageId) throws NullParameterException, GeneralException, TopUpTransactionException, InvalidFormatException {
//        System.out.println("------------------- doTransferToTopUp ------------------------");
//        Map<String, String> response = new HashMap<String, String>();
//        String error = "";
//        try {
//            int skuId = Integer.parseInt(topUpProduct.getReferenceCode());
//            String correlativeId = account.getId() + "-" + Calendar.getInstance().getTimeInMillis();
//            OrderResponse orderResponse = ServiceManager.purchaseRtr2(skuId, topUpProduct.getProductDenomination().getAmount(), destinationNumber, correlativeId, null, null);
//            String code = orderResponse.getResponseCode();
//
//            if (!code.equals("000")) {//Cuando es 000 esta bien...
//                StringBuilder errorBuilder = new StringBuilder(TopUpResponseConstants.PREPAY_NATION_CODES.get(code));
//                errorBuilder.append("Integrator= ").append("PrepayNation").append("ProductId = ").append(topUpProduct.getId()).append("phoneNumber = ").append(destinationNumber);
//                error = errorBuilder.toString();
//
//                if (code.equals("033")) {
//                    throw new InvalidFormatException(error);
//                }
//                throw new TopUpTransactionException(error);
//            }
//
//            String message1 = sendSenderSMS(topUpProduct.getProductDenomination().getAmount(), senderNumber, destinationNumber, null, account, languageId);
//            String message2 = sendDestinationSMS(topUpProduct.getProductDenomination().getAmount(), senderNumber, destinationNumber, null, account, languageId);
//            response.put(TopUpResponseConstants.SMS_SENDER, message1);
//            response.put(TopUpResponseConstants.SMS_DESTINATION, message2);
//            response.put(TopUpResponseConstants.ERROR_CODE, orderResponse.getResponseCode());
//            response.put(TopUpResponseConstants.ERROR_TEXT, orderResponse.getResponseMessage());
//            response.put(TopUpResponseConstants.DESTINATION_NUMBER, destinationNumber);
//            response.put(TopUpResponseConstants.SENDER_NUMBER, senderNumber);
//            response.put(TopUpResponseConstants.AUTHENTICATION_KEY, "" + orderResponse.getInvoice().getInvoiceNumber());
//            StringBuilder responseBuilder = new StringBuilder();
//
//            try {
//                Iterator it = response.entrySet().iterator();
//                while (it.hasNext()) {
//                    Map.Entry e = (Map.Entry) it.next();
//                    responseBuilder.append(e.getValue());
//                }
//            } catch (Exception e) {
//                responseBuilder.append(e.getMessage());
//                e.printStackTrace();
//            }
//            response.put(TopUpResponseConstants.COMPLETE_RESPONSE, responseBuilder.toString());
//
//        } catch (TopUpTransactionException ex) {
//            throw (ex);
//        } catch (InvalidFormatException ex) {
//            throw (ex);
//        } catch (Exception ex) {
//            throw new GeneralException(ex.getMessage() + error);
//        }
//        return response;
//    }
//
//    private String sendSenderSMS(float amount, String senderNumber, String destinationNumber, TopUpResponse topUpResponse, Account account, Long languageId) {
//        StringBuilder messageBuilder = new StringBuilder();
//
//        if (languageId.equals(Language.ENGLISH)) {
//            messageBuilder.append("You have done a balance recharge.").append(" Amount: ").append(amount).append(". Destination: ").append(destinationNumber);
//            if (topUpResponse != null && topUpResponse.getPinBased() != null && topUpResponse.getPinBased().equals("yes")) {
//                messageBuilder.append(". Pin code: ").append(topUpResponse.getPinCode());
//                messageBuilder.append(". Pin serial: ").append(topUpResponse.getPinSerial());
//                messageBuilder.append(". IVR: ").append(topUpResponse.getPinIvr());
//            }
//            messageBuilder.append(". Thank you for using Alodiga.");
//        } else {
//            messageBuilder.append("Ha realizado una recarga de saldo.").append(" Monto: ").append(amount).append(". Destinatario:").append(destinationNumber);
//            if (topUpResponse != null && topUpResponse.getPinBased() != null && topUpResponse.getPinBased().equals("yes")) {
//                messageBuilder.append(". Codigo Pin: ").append(topUpResponse.getPinCode());
//                messageBuilder.append(". Serial Pin: ").append(topUpResponse.getPinSerial());
//                messageBuilder.append(". IVR: ").append(topUpResponse.getPinIvr());
//            }
//            messageBuilder.append(". Gracias por usar Alodiga.");
//        }
//        try {
//            SMS sms = new SMS();
//            sms.setAccount(account);
//            sms.setDestination(senderNumber);
//            sms.setuSAPhoneNumber(senderNumber);//Para el caso de MLAT ya que se debe concatenar un 1 al número de destino.
//            sms.setContent(messageBuilder.toString());
//            (new com.alodiga.services.provider.commons.utils.SMSSender(sms)).start();
//
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//        return messageBuilder.toString();
//
//    }
//
//    private String sendDestinationSMS(float amount, String senderNumber, String destinationNumber, TopUpResponse topUpResponse, Account account, Long languageId) {
//        StringBuilder messageBuilder = new StringBuilder();
//        if (languageId.equals(Language.ENGLISH)) {
//            messageBuilder.append("You have received a balance recharge.");
//            messageBuilder.append(" Amount: ").append(amount).append(" From: ").append(senderNumber);
//            if (topUpResponse != null && topUpResponse.getPinBased() != null) {
//                System.out.println("topUpResponse.getPinBased() " + topUpResponse.getPinBased());
//                System.out.println("topUpResponse.getPinBased().equals(yes) " + topUpResponse.getPinBased().equals("yes"));
//            }
//            if (topUpResponse != null && topUpResponse.getPinBased() != null && topUpResponse.getPinBased().equals("yes")) {
//                messageBuilder.append(". Pin code: ").append(topUpResponse.getPinCode());
//                messageBuilder.append(". Pin serial: ").append(topUpResponse.getPinSerial());
//                messageBuilder.append(". IVR: ").append(topUpResponse.getPinIvr());
//
//            }
//            messageBuilder.append(". Thank you for using Alodiga.");
//        } else {
//            messageBuilder.append("Ha recibido una recarga de saldo.");
//            messageBuilder.append(" Monto: ").append(amount).append(" Desde: ").append(senderNumber);
//            if (topUpResponse != null && topUpResponse.getPinBased() != null && topUpResponse.getPinBased().equals("yes")) {
//                messageBuilder.append(". Codigo Pin: ").append(topUpResponse.getPinCode());
//                messageBuilder.append(". Serial Pin: ").append(topUpResponse.getPinSerial());
//                messageBuilder.append(". IVR: ").append(topUpResponse.getPinIvr());
//            }
//            messageBuilder.append(". Gracias por usar Alodiga.");
//        }
//
//        try {
//            SMS sms = new SMS();
//            sms.setAccount(account);
//            sms.setDestination(destinationNumber);
//            sms.setContent(messageBuilder.toString());
//            (new com.alodiga.services.provider.commons.utils.SMSSender(sms)).start();
//
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//        return messageBuilder.toString();
//    }
//
//    @Override
//    public AccessNumberResponse getAccesNumberByAni(EJBRequest request) throws NullParameterException, GeneralException, AccessNumberNotfoundException {
//
//        Map<String, Object> params = request.getParams();
//
//        EJBRequest requestAccesNumber = new EJBRequest();
//        Map<String, Object> requestParams = new HashMap<String, Object>();
//
//        if (!params.containsKey(WSConstants.PARAM_ANI)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_ANI), null);
//        }
//
//        if (!params.containsKey(WSConstants.PARAM_CUSTOM_SERVICE_ID)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_CUSTOM_SERVICE_ID), null);
//        }
//
//        if (!params.containsKey(WSConstants.PARAM_COUNT_ACCESS_NUMBER)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_COUNT_ACCESS_NUMBER), null);
//        }
//
//        requestParams.put(WSConstants.PARAM_ANI, params.get(WSConstants.PARAM_ANI).toString());
//        requestParams.put(WSConstants.PARAM_CUSTOM_SERVICE_ID, params.get(WSConstants.PARAM_CUSTOM_SERVICE_ID).toString());
//        requestParams.put(WSConstants.PARAM_COUNT_ACCESS_NUMBER, params.get(WSConstants.PARAM_COUNT_ACCESS_NUMBER).toString());
//
//        requestAccesNumber.setParams(requestParams);
//        requestAccesNumber.setMediaType(new DistributionWSMediaType());
//        requestAccesNumber.setUrl(WSConstants.URL_GET_ACCESS_NUMBER);
//        requestAccesNumber.setMethod(HttpMethod.POST);
//        String response = loadData(getClass(), requestAccesNumber, logger, getMethodName());
//        AccessNumberResponse accessNumberResponse = (AccessNumberResponse) DocumentHandler.objectFromXml(response, AccessNumberResponse.class);
//        if (accessNumberResponse.getErrors() != null) {
//            throw new AccessNumberNotfoundException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), null);
//        }
//        return accessNumberResponse;
//    }
//
//    @Override
//    public List<CallingCardPinInvoice> getCallHistory(EJBRequest request) throws NullParameterException, EmptyListException, GeneralException {
//        List<CallingCardPinInvoice> calls = new ArrayList<CallingCardPinInvoice>();
//        try {
//            Map<String, Object> params = request.getParams();
//            if (!params.containsKey(WSConstants.PARAM_SERIAL)) {
//                throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_SERIAL), null);
//            }
//            if (!params.containsKey(WSConstants.PARAM_ENTERPRISE_ID)) {
//                throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_ENTERPRISE_ID), null);
//            }
//            if (!params.containsKey(WSConstants.PARAM_LANGUAGE_ID)) {
//                throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_LANGUAGE_ID), null);
//            }
//            if (!params.containsKey(WSConstants.PARAM_BEGGINING_DATE)) {
//                throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_BEGGINING_DATE), null);
//            }
//            if (!params.containsKey(WSConstants.PARAM_ENDING_DATE)) {
//                throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_ENDING_DATE), null);
//            }
//
//            EJBRequest authRequest = new EJBRequest();
//            Map<String, Object> authParam = new HashMap<String, Object>();
//            authParam.put(WSConstants.PARAM_LANGUAGE_ID, params.get(WSConstants.PARAM_LANGUAGE_ID).toString());
//            authRequest.setParams(authParam);
//            String token = authenticate(authRequest);
//            if (token == null) {
//                throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_TOKEN), null);
//            }
//            params.put(WSConstants.PARAM_TOKEN, token);
//            request.setMediaType(new DistributionWSMediaType());
//            request.setUrl(WSConstants.URL_CALL_HISTORY_SERVICE);
//            request.setMethod(HttpMethod.POST);
//
//            ApplicationResponse appResponse = (ApplicationResponse) DocumentHandler.objectFromXml(loadData(getClass(), request, logger, getMethodName()), ApplicationResponse.class);
//
//            if (appResponse.getCard() != null && appResponse.getCard().getCalls() != null) {
//
//                calls = appResponse.getCard().getCalls();
//            } else if (appResponse.getErrors() != null) {
//                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), appResponse.getErrors()), null);
//            }
//        } catch (Exception e) {
//            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
//        }
//        if (calls.isEmpty()) {
//            throw new EmptyListException(logger, sysError.format(EjbConstants.ERR_EMPTY_LIST_EXCEPTION, this.getClass(), getMethodName()), null);
//        }
//        return calls;
//    }
//
//    @Override
//    public Map<String, String> getCheckWallet() throws NullParameterException, GeneralException, TopUpTransactionException {
//        TopUpWalletResponse topUpWalletResponse = new TopUpWalletResponse();
//        Map<String, String> response = new HashMap<String, String>();
//        try {
//            topUpWalletResponse = RequestManager.getWallet();
////            response.put(TopUpWalletResponseConstants.AUTHENTICATION_KEY, topUpWalletResponse.getAuthentication_key());
////            response.put(TopUpWalletResponseConstants.BALANCE, topUpWalletResponse.getBalance());
////            response.put(TopUpWalletResponseConstants.CURRENCY, topUpWalletResponse.getCurrency());
////            response.put(TopUpWalletResponseConstants.ERROR_CODE, topUpWalletResponse.getError_code());
////            response.put(TopUpWalletResponseConstants.ERROR_TEXT, topUpWalletResponse.getError_txt());
////            response.put(TopUpWalletResponseConstants.LOGIN, topUpWalletResponse.getLogin());
////            response.put(TopUpWalletResponseConstants.TYPE, topUpWalletResponse.getType());
////            response.put(TopUpWalletResponseConstants.WALLET, topUpWalletResponse.getWallet());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return response;
//    }
//
//    @Override
//    public ExtendedPinDataResponse getPinfreeByAni(Long languageId, String ani) throws NullParameterException, GeneralException, PinFreeNotfoundException {
//
//        EJBRequest requestPinFre = new EJBRequest();
//        if (languageId == null) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_LANGUAGE_ID), null);
//        }
//
//        if (ani == null || ani.equals("")) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_ANI), null);
//        }
//
//        Map<String, Object> requestParams = new HashMap<String, Object>();
//        requestParams.put(WSConstants.PARAM_LANGUAGE_ID, languageId.toString());
//        requestParams.put(WSConstants.PARAM_ANI, ani);
//
//        requestPinFre.setParams(requestParams);
//        requestPinFre.setMediaType(new DistributionWSMediaType());
//        requestPinFre.setUrl(WSConstants.URL_GET_EXTENDEDPINDATA);
//        requestPinFre.setMethod(HttpMethod.POST);
//
//        String response = loadData(getClass(), requestPinFre, logger, getMethodName());
//        //System.out.println(response);
//
//        ExtendedPinDataResponse extendedPinDataResponse = (ExtendedPinDataResponse) DocumentHandler.objectFromXml(response, ExtendedPinDataResponse.class);
//        if (extendedPinDataResponse.getErrors() != null) {
//            throw new PinFreeNotfoundException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), null);
//        }
//        return extendedPinDataResponse;
//    }
//
//    @Override
//    public RechargePinResponse preprocessRechargePin(EJBRequest request) throws NullParameterException, GeneralException, PurchaseCanceledException {
//        Map<String, Object> params = request.getParams();
//
//        EJBRequest requestProvision = new EJBRequest();
//        Map<String, Object> requestParams = new HashMap<String, Object>();
//
//        if (!params.containsKey(WSConstants.PARAM_LANGUAGE_ID)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_LANGUAGE_ID), null);
//        }
//        if (!params.containsKey(WSConstants.PARAM_SERIAL)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_SERIAL), null);
//        }
//        if (!params.containsKey(QueryConstants.PARAM_TRANSACTION) && !(params.get(QueryConstants.PARAM_TRANSACTION) instanceof Transaction)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), QueryConstants.PARAM_TRANSACTION), null);
//        }
//        if (!params.containsKey(WSConstants.PARAM_HAS_PAYMENT) && !(params.get(WSConstants.PARAM_HAS_PAYMENT) instanceof Boolean)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_HAS_PAYMENT), null);
//        }
//
//        Boolean hasPayment = (Boolean) params.get(WSConstants.PARAM_HAS_PAYMENT);
//        requestParams.put(WSConstants.PARAM_CUSTOM_SERVICE_ID, WSConstants.CUSTOM_SERVICE_ID.toString());
//        requestParams.put(WSConstants.PARAM_LANGUAGE_ID, params.get(WSConstants.PARAM_LANGUAGE_ID).toString());
//        requestParams.put(WSConstants.PARAM_WEB_USER_ID, WSConstants.WEB_USER_ID_SISAC);
//        requestParams.put(WSConstants.PARAM_SERIAL, params.get(WSConstants.PARAM_SERIAL).toString());
//        requestParams.put(WSConstants.PARAM_HAS_PAYMENT, hasPayment.toString());
//
//        // Se obtiene la data de la transaccion
//        Transaction transaction = (Transaction) params.get(QueryConstants.PARAM_TRANSACTION);
//        if (hasPayment) {
//            //COMENTADO - LEONEL
//            PaymentInfo paymentInfo = null;//transaction.getPaymentInfo();
//
//            requestParams.put(WSConstants.PARAM_CREDIT_CARD_NAME, paymentInfo.getCreditCardName());
//            requestParams.put(WSConstants.PARAM_CREDIT_CARD_NUMBER, paymentInfo.getCreditCardNumber());
//            requestParams.put(WSConstants.PARAM_CREDIT_CARD_CVV, paymentInfo.getCreditCardCvv());
//            requestParams.put(WSConstants.PARAM_CREDIT_CARD_TYPE, paymentInfo.getCreditcardType().getName());
//            requestParams.put(WSConstants.PARAM_CREDIT_CARD_DATE, paymentInfo.getCreditCardDate().toString());
//
//            Address address = paymentInfo.getAddress();
//
//            requestParams.put(WSConstants.PARAM_COUNTRY_ID, (address.getCountry() != null) ? address.getCountry().getId().toString() : "-1");
//            requestParams.put(WSConstants.PARAM_STATE_ID, (address.getState() != null && address.getState().getId() != null) ? address.getState().getId().toString() : "-1");
//            requestParams.put(WSConstants.PARAM_COUNTY_ID, (address.getCounty() != null && address.getCounty().getId() != null) ? address.getCounty().getId().toString() : "-1");
//            requestParams.put(WSConstants.PARAM_CITY_ID, (address.getCity() != null && address.getCity().getId() != null) ? address.getCity().getId().toString() : "-1");
//            requestParams.put(WSConstants.PARAM_ADDRESS, address.getAddress());
//            requestParams.put(WSConstants.PARAM_ZIPCODE, address.getZipCode());
//            requestParams.put(WSConstants.PARAM_CITY_NAME, address.getCityName());
//            requestParams.put(WSConstants.PARAM_STATE_NAME, address.getStateName());
//            requestParams.put(WSConstants.PARAM_COUNTY_NAME, address.getCountyName());
//
//            Customer customer = paymentInfo.getCustomer();
//            if (customer == null) {
//                //COMENTADO - LEONEL
//                customer = null;//transaction.getCustomer();
//            }
//            if (customer == null) {
//                throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), QueryConstants.PARAM_TRANSACTION), null);
//            }
//
//            requestParams.put(WSConstants.PARAM_CUSTOMER_LOGIN, customer.getLogin());
//            requestParams.put(WSConstants.PARAM_CUSTOMER_EMAIL, customer.getEmail());
//            requestParams.put(WSConstants.PARAM_CUSTOMER_FIRSTNAME, customer.getFirstName());
//            requestParams.put(WSConstants.PARAM_CUSTOMER_LASTNAME, customer.getLastName());
//        } else {
//            requestParams.put(WSConstants.PARAM_CREDIT_CARD_NAME, " ");
//            requestParams.put(WSConstants.PARAM_CREDIT_CARD_NUMBER, " ");
//            requestParams.put(WSConstants.PARAM_CREDIT_CARD_CVV, " ");
//            requestParams.put(WSConstants.PARAM_CREDIT_CARD_TYPE, " ");
//            requestParams.put(WSConstants.PARAM_CREDIT_CARD_DATE, " ");
//
//            requestParams.put(WSConstants.PARAM_COUNTRY_ID, "-1");
//            requestParams.put(WSConstants.PARAM_STATE_ID, "-1");
//            requestParams.put(WSConstants.PARAM_COUNTY_ID, "-1");
//            requestParams.put(WSConstants.PARAM_CITY_ID, "-1");
//            requestParams.put(WSConstants.PARAM_ADDRESS, " ");
//            requestParams.put(WSConstants.PARAM_ZIPCODE, " ");
//            requestParams.put(WSConstants.PARAM_CITY_NAME, " ");
//            requestParams.put(WSConstants.PARAM_STATE_NAME, " ");
//            requestParams.put(WSConstants.PARAM_COUNTY_NAME, " ");
//
//            requestParams.put(WSConstants.PARAM_CUSTOMER_LOGIN, "-1");
//            requestParams.put(WSConstants.PARAM_CUSTOMER_EMAIL, "-1");
//            requestParams.put(WSConstants.PARAM_CUSTOMER_FIRSTNAME, "-1");
//            requestParams.put(WSConstants.PARAM_CUSTOMER_LASTNAME, "-1");
//        }
//
//        requestProvision.setMediaType(new DistributionWSMediaType());
//        requestProvision.setUrl(WSConstants.URL_RECHARGE_PIN);
//        requestProvision.setMethod(HttpMethod.POST);
//        requestProvision.setParams(requestParams);
//
//        String response = loadData(getClass(), requestProvision, logger, getMethodName());
//
//        RechargePinResponse rechargePinResponse = (RechargePinResponse) DocumentHandler.objectFromXml(response, RechargePinResponse.class);
//
//        if (rechargePinResponse.getErrors() != null && !rechargePinResponse.getErrors().isEmpty()) {
//            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), null);
//        }
//
//        //System.out.println(response);
//
//        return rechargePinResponse;
//    }
//
//    @Override
//    public ProcessRechargePinResponse processRechargePin(EJBRequest request) throws NullParameterException, GeneralException, PurchaseCanceledException {
//        Map<String, Object> params = request.getParams();
//
//        EJBRequest requestProvision = new EJBRequest();
//        Map<String, Object> requestParams = new HashMap<String, Object>();
//
//        if (!params.containsKey(WSConstants.PARAM_LANGUAGE_ID)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_LANGUAGE_ID), null);
//        }
//        if (!params.containsKey(WSConstants.PARAM_USER_SESSION_ID)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_USER_SESSION_ID), null);
//        }
//
//        requestParams.put(WSConstants.PARAM_CUSTOM_SERVICE_ID, WSConstants.CUSTOM_SERVICE_ID.toString());
//        requestParams.put(WSConstants.PARAM_LANGUAGE_ID, params.get(WSConstants.PARAM_LANGUAGE_ID).toString());
//        requestParams.put(WSConstants.PARAM_USER_SESSION_ID, params.get(WSConstants.PARAM_USER_SESSION_ID).toString());
//
//        requestProvision.setMediaType(new DistributionWSMediaType());
//        requestProvision.setUrl(WSConstants.URL_PROCESS_RECHARGE_PIN);
//        requestProvision.setMethod(HttpMethod.POST);
//        requestProvision.setParams(requestParams);
//
//        String response = loadData(getClass(), requestProvision, logger, getMethodName());
//
//        ProcessRechargePinResponse processRechargePinResponse = (ProcessRechargePinResponse) DocumentHandler.objectFromXml(response, ProcessRechargePinResponse.class);
//
//        if (processRechargePinResponse.getErrors() != null && !processRechargePinResponse.getErrors().isEmpty()) {
//            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PROCESS_RECHARGE, this.getClass(), getMethodName(), null), null);
//        }
//
//        //System.out.println(response);
//
//        return processRechargePinResponse;
//    }
//
//    @Override
//    public PurchaseBalanceAccount purchaseBalanceDistributor(EJBRequest request) throws NullParameterException, GeneralException {
//        Map<String, Object> params = request.getParams();
//
//        EJBRequest requestPurchase = new EJBRequest();
//        Map<String, Object> requestParams = new HashMap<String, Object>();
//
//        if (!params.containsKey(WSConstants.PARAM_LANGUAGE_ID)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_LANGUAGE_ID), null);
//        }
//
//        if (!params.containsKey(QueryConstants.PARAM_TRANSACTION)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), QueryConstants.PARAM_TRANSACTION), null);
//        }
//
//        String languageId = String.valueOf(params.get(WSConstants.PARAM_LANGUAGE_ID));
//        Transaction transaction = (Transaction) params.get(QueryConstants.PARAM_TRANSACTION);
//
//        EJBRequest authRequest = new EJBRequest();
//        Map<String, Object> authParam = new HashMap<String, Object>();
//        authParam.put(WSConstants.PARAM_LANGUAGE_ID, languageId.toString());
//        authRequest.setParams(authParam);
//        String token = authenticate(authRequest);
//
//        requestParams.put(WSConstants.PARAM_ENTERPRISE_ID, WSConstants.ENTERPRISE_ID.toString());
//        requestParams.put(WSConstants.PARAM_TOKEN, token);
//        requestParams.put(WSConstants.PARAM_LANGUAGE_ID, languageId.toString());
//        //COMENTADO - LEONEL
//        PaymentInfo paymentInfo = null;//transaction.getPaymentInfo();
//        Address address = null;//transaction.getPaymentInfo().getAddress();
//        requestParams.put(WSConstants.PARAM_CREDIT_CARD_CVV, paymentInfo.getCreditCardCvv());
//        requestParams.put(WSConstants.PARAM_CREDIT_CARD_NUMBER, paymentInfo.getCreditCardNumber());
//        requestParams.put(WSConstants.PARAM_CREDIT_CARD_TYPE, paymentInfo.getCreditcardType().getName());
//        requestParams.put(WSConstants.PARAM_ZIPCODE, address.getZipCode().toString());
//        requestParams.put(WSConstants.PARAM_CREDIT_CARD_NAME, paymentInfo.getCreditCardName());
//        requestParams.put(WSConstants.PARAM_COUNTRY_ID, address.getCountry().getId().toString());
//
//        if (address.getState() != null) {
//            requestParams.put(WSConstants.PARAM_STATE_ID, address.getState().getId().toString());
//        } else {
//            requestParams.put(WSConstants.PARAM_STATE_NAME, address.getStateName());
//        }
//
//        if (address.getCounty() != null) {
//            requestParams.put(WSConstants.PARAM_COUNTY_ID, address.getCounty().getId().toString());
//        } else {
//            requestParams.put(WSConstants.PARAM_COUNTY_NAME, address.getCountyName());
//        }
//        if (address.getCity() != null) {
//            requestParams.put(WSConstants.PARAM_CITY_ID, address.getCity().getId().toString());
//        } else {
//            requestParams.put(WSConstants.PARAM_CITY_NAME, address.getCityName());
//        }
//        requestParams.put(WSConstants.PARAM_ADDRESS, address.getAddress());
//
//        requestParams.put(WSConstants.PARAM_YEAR, paymentInfo.getCreditCardDate().getYear() + 1900 + "");
//        requestParams.put(WSConstants.PARAM_MONTH, paymentInfo.getCreditCardDate().getMonth() + 1 + "");
//
//        requestPurchase.setParams(requestParams);
//        requestPurchase.setMediaType(new DistributionWSMediaType());
//        requestPurchase.setUrl(WSConstants.URL_PURCHASE_BALANCE_DISTRIBUTOR);
//        requestPurchase.setMethod(HttpMethod.POST);
//        String response = loadData(getClass(), requestPurchase, logger, getMethodName());
//        PurchaseBalanceAccount purchaseBalanceDistributor = (PurchaseBalanceAccount) DocumentHandler.objectFromXml(response, PurchaseBalanceAccount.class);
//
//        return purchaseBalanceDistributor;
//    }
//
//    @Override
//    public ProvisionPinResponse purchasePin(EJBRequest request) throws NullParameterException, GeneralException, PurchaseCanceledException, PinFreeProvisionException, PinProvisionException {
//        ProvisionPinResponse provisionPinResponse = new ProvisionPinResponse();
//        Map<String, Object> params = request.getParams();
//        EJBRequest requestProvision = new EJBRequest();
//        Map<String, Object> paramsProvision = new HashMap<String, Object>();
//
//        if (!params.containsKey(WSConstants.PARAM_LANGUAGE_ID)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_LANGUAGE_ID), null);
//        }
//        if (!params.containsKey(WSConstants.PARAM_PIN_BALANCE)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_PIN_BALANCE), null);
//        }
//        if (!params.containsKey(WSConstants.PARAM_ANI_LIST)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_ANI_LIST), null);
//        }
//        ProvisionAniResponse provisionAni = null;
//        try {
//            List<String> aniList = (List<String>) params.get(WSConstants.PARAM_ANI_LIST);
//            String aniString = "";
//            for (String ani : aniList) {
//                aniString += ani + ",";
//            }
//            aniString = aniString.substring(0, aniString.length() - 1);
//
//            String transactionData = "";
//            transactionData = " balance: " + params.get(WSConstants.PARAM_PIN_BALANCE).toString() + " - ANI LIST: " + aniString;
//
//            paramsProvision.put(WSConstants.PARAM_LANGUAGE_ID, params.get(WSConstants.PARAM_LANGUAGE_ID).toString());
//            paramsProvision.put(WSConstants.PARAM_PIN_BALANCE, params.get(WSConstants.PARAM_PIN_BALANCE).toString());
//            paramsProvision.put(WSConstants.PARAM_TRANSACTION_DATA, transactionData);
//            paramsProvision.put(WSConstants.PARAM_CUSTOM_SERVICE_ID, WSConstants.CUSTOM_SERVICE_ID.toString());
//            //
//            paramsProvision.put(WSConstants.PARAM_ADDRESS, params.get(WSConstants.PARAM_ADDRESS).toString());
//            paramsProvision.put(WSConstants.PARAM_CUSTOMER_FIRSTNAME, params.get(WSConstants.PARAM_CUSTOMER_FIRSTNAME).toString());
//            paramsProvision.put(WSConstants.PARAM_CUSTOMER_LASTNAME, params.get(WSConstants.PARAM_CUSTOMER_LASTNAME).toString());
//            paramsProvision.put(WSConstants.PARAM_CUSTOMER_LOGIN, params.get(WSConstants.PARAM_CUSTOMER_LOGIN).toString());
//            paramsProvision.put(WSConstants.PARAM_CUSTOMER_EMAIL, params.get(WSConstants.PARAM_CUSTOMER_EMAIL).toString());
//            paramsProvision.put(WSConstants.PARAM_ENTERPRISE_ID, params.get(WSConstants.PARAM_ENTERPRISE_ID).toString());
//            paramsProvision.put(WSConstants.PARAM_COUNTRY_ID, params.get(WSConstants.PARAM_COUNTRY_ID).toString());
//            paramsProvision.put(WSConstants.PARAM_STATE_ID, params.get(WSConstants.PARAM_STATE_ID).toString());
//            paramsProvision.put(WSConstants.PARAM_COUNTY_ID, params.get(WSConstants.PARAM_COUNTY_ID).toString());
//            paramsProvision.put(WSConstants.PARAM_CITY_ID, params.get(WSConstants.PARAM_CITY_ID).toString());
//            paramsProvision.put(WSConstants.PARAM_PASSWORD, params.get(WSConstants.PARAM_PASSWORD).toString());
//            // Se provisiona el pin
//            requestProvision.setMediaType(new DistributionWSMediaType());
//            requestProvision.setUrl(WSConstants.URL_PROVISION_PIN_WEB_USER);
//            requestProvision.setMethod(HttpMethod.POST);
//            requestProvision.setParams(paramsProvision);
//            String response = loadData(getClass(), requestProvision, logger, getMethodName());
//
//            provisionPinResponse = (ProvisionPinResponse) DocumentHandler.objectFromXml(response, ProvisionPinResponse.class);
//            if (provisionPinResponse.getErrors() != null && !provisionPinResponse.getErrors().isEmpty()) {
//                throw new PinProvisionException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), null);
//            }
//            // Se provisiona los pinfrees
//            requestProvision = new EJBRequest();
//            paramsProvision = new HashMap<String, Object>();
//
//            paramsProvision.put(WSConstants.PARAM_LANGUAGE_ID, params.get(WSConstants.PARAM_LANGUAGE_ID).toString());
//            paramsProvision.put(WSConstants.PARAM_SERIAL, provisionPinResponse.getSerial());
//            paramsProvision.put(WSConstants.PARAM_ANI_LIST, aniString);
//            paramsProvision.put(WSConstants.PARAM_CUSTOM_SERVICE_ID, WSConstants.CUSTOM_SERVICE_ID.toString());
//            paramsProvision.put(WSConstants.PARAM_QUANTITY, String.valueOf(10));
//            requestProvision.setMediaType(new DistributionWSMediaType());
//            requestProvision.setUrl(WSConstants.URL_PROVISION_PINFREE2);
//            requestProvision.setMethod(HttpMethod.POST);
//            requestProvision.setParams(paramsProvision);
//
//            response = loadData(getClass(), requestProvision, logger, getMethodName());
//            //System.out.println("response : " + response);
//            provisionAni = (ProvisionAniResponse) DocumentHandler.objectFromXml(response, ProvisionAniResponse.class);
//            provisionPinResponse.setAccessNumbers(provisionAni.getAccessNumbers());
//        } catch (Exception e) {
//            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
//        }
//        if (provisionAni.getErrors() != null && !provisionAni.getErrors().isEmpty()) {
//            throw new PinFreeProvisionException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION_ANI, this.getClass(), getMethodName(), null), null);
//        }
//        return provisionPinResponse;
//    }
//
//    @Override
//    public OrderRecharge rechargePinByDistributor(EJBRequest request) throws NullParameterException, GeneralException {
//        Map<String, Object> params = request.getParams();
//
//        EJBRequest requestProvision = new EJBRequest();
//        Map<String, Object> requestParams = new HashMap<String, Object>();
//
//        if (!params.containsKey(WSConstants.PARAM_LANGUAGE_ID)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_LANGUAGE_ID), null);
//        }
//        if (!params.containsKey(WSConstants.PARAM_SERIAL)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_SERIAL), null);
//        }
//        if (!params.containsKey(QueryConstants.PARAM_TRANSACTION) && !(params.get(QueryConstants.PARAM_TRANSACTION) instanceof Transaction)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), QueryConstants.PARAM_TRANSACTION), null);
//        }
//
//        requestParams.put(WSConstants.PARAM_CUSTOM_SERVICE_ID, WSConstants.CUSTOM_SERVICE_ID.toString());
//        requestParams.put(WSConstants.PARAM_LANGUAGE_ID, params.get(WSConstants.PARAM_LANGUAGE_ID).toString());
//        requestParams.put(WSConstants.PARAM_WEB_USER_ID, WSConstants.WEB_USER_ID_SISAC);
//        requestParams.put(WSConstants.PARAM_SERIAL, params.get(WSConstants.PARAM_SERIAL).toString());
//
//        // Se obtiene la data de la transaccion
//        Transaction transaction = (Transaction) params.get(QueryConstants.PARAM_TRANSACTION);
//
//        requestProvision.setMediaType(new DistributionWSMediaType());
//        requestProvision.setUrl(WSConstants.URL_RECHARGE_PIN_DISTRIBUTOR);
//        requestProvision.setMethod(HttpMethod.POST);
//        requestProvision.setParams(requestParams);
//        String response = loadData(getClass(), requestProvision, logger, getMethodName());
//
//        OrderRecharge orderRecharge = (OrderRecharge) DocumentHandler.objectFromXml(response, OrderRecharge.class);
//
//        if ((orderRecharge.getErrors() != null && !orderRecharge.getErrors().isEmpty()) || orderRecharge.getSisacOrder() == null) {
//            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), null);
//        }
//
//        //System.out.println(response);
//
//        return orderRecharge;
//    }
//
//    @Override
//    public OperationResponse searchOperations(EJBRequest request) throws NullParameterException, GeneralException {
//        Map<String, Object> params = request.getParams();
//
//        EJBRequest requestWS = new EJBRequest();
//
//
//        if (!params.containsKey(WSConstants.PARAM_LANGUAGE_ID)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_LANGUAGE_ID), null);
//        }
//        if (!params.containsKey(WSConstants.PARAM_BEGGINING_DATE)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_BEGGINING_DATE), null);
//        }
//        if (!params.containsKey(WSConstants.PARAM_ENDING_DATE)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_ENDING_DATE), null);
//        }
//
//
//        requestWS.setMediaType(new DistributionWSMediaType());
//        requestWS.setUrl(WSConstants.URL_PROCESS_OPERATION);
//        requestWS.setMethod(HttpMethod.POST);
//        requestWS.setParams(params);
//
//        String response = loadData(getClass(), requestWS, logger, getMethodName());
//
//        OperationResponse operationResponse = (OperationResponse) DocumentHandler.objectFromXml(response, OperationResponse.class);
//        /*if (operationResponse.getErrors() != null && !operationResponse.getErrors().isEmpty()) {
//        throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PROCESS_RECHARGE, this.getClass(), getMethodName(), null), null);
//        }
//         */
//        //System.out.println(response);
//
//        return operationResponse;
//    }
//
//    @Override
//    public ProvisionPinResponse updatePin(EJBRequest request) throws NullParameterException, GeneralException, PurchaseCanceledException {
//        ProvisionPinResponse provisionPinResponse = null;
//
//        Map<String, Object> params = request.getParams();
//
//        if (!params.containsKey(WSConstants.PARAM_LANGUAGE_ID)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_LANGUAGE_ID), null);
//        }
//        if (!params.containsKey(WSConstants.PARAM_SERIAL)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_SERIAL), null);
//        }
//        if (!params.containsKey(WSConstants.PARAM_ANI_LIST)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_ANI_LIST), null);
//        }
//        try {
//            // Se actualizan los pinfree's
//            request.setMediaType(new DistributionWSMediaType());
//            request.setUrl(WSConstants.URL_UPDATE_PINFREE);
//            request.setMethod(HttpMethod.POST);
//
//            String response = loadData(getClass(), request, logger, getMethodName());
//            ProvisionAniResponse provisionAni = (ProvisionAniResponse) DocumentHandler.objectFromXml(response, ProvisionAniResponse.class);
//
//            if (provisionAni.getErrors() != null && !provisionAni.getErrors().isEmpty()) {
//                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), null);
//            }
//
//        } catch (Exception e) {
//            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
//        }
//        return provisionPinResponse;
//    }
//
//    public ProcessRechargePinResponse cancelPinRecharge(EJBRequest request) throws NullParameterException, GeneralException, PurchaseCanceledException {
//        ProcessRechargePinResponse rechargePinResponse = null;
//
//        Map<String, Object> params = request.getParams();
//
//        if (!params.containsKey(WSConstants.PARAM_LANGUAGE_ID)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_LANGUAGE_ID), null);
//        }
//        if (!params.containsKey(WSConstants.PARAM_ORDER_ID)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_ORDER_ID), null);
//        }
//
//        try {
//            // Se actualizan los pinfree's
//            request.setMediaType(new DistributionWSMediaType());
//            request.setUrl(WSConstants.URL_CANCEL_PIN_RECHARGE);
//            request.setMethod(HttpMethod.POST);
//
//            String response = loadData(getClass(), request, logger, getMethodName());
//            rechargePinResponse = (ProcessRechargePinResponse) DocumentHandler.objectFromXml(response, ProcessRechargePinResponse.class);
//
//            if (rechargePinResponse.getErrors() != null && !rechargePinResponse.getErrors().isEmpty()) {
//                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), null);
//            }
//        } catch (GeneralException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
//        }
//        return rechargePinResponse;
//    }
//
//    public ProcessOrderVoidingResponse cancelPurchaseBalance(EJBRequest request) throws NullParameterException, GeneralException, PurchaseCanceledException {
//        ProcessOrderVoidingResponse orderVoidingResponse = null;
//
//        Map<String, Object> params = request.getParams();
//
//        if (!params.containsKey(WSConstants.PARAM_LANGUAGE_ID)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_LANGUAGE_ID), null);
//        }
//        if (!params.containsKey(WSConstants.PARAM_ORDER_ID)) {
//            throw new NullParameterException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_ORDER_ID), null);
//        }
//        try {
//            request.setMediaType(new DistributionWSMediaType());
//            request.setUrl(WSConstants.URL_PROCESS_VOID);
//            request.setMethod(HttpMethod.POST);
//            String response = loadData(getClass(), request, logger, getMethodName());
//            //System.out.println("response : " + response);
//            orderVoidingResponse = (ProcessOrderVoidingResponse) DocumentHandler.objectFromXml(response, ProcessOrderVoidingResponse.class);
//            if (orderVoidingResponse.getErrors() != null && !orderVoidingResponse.getErrors().isEmpty()) {
//                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), null);
//            }
//        } catch (Exception e) {
//            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
//        }
//        return orderVoidingResponse;
//    }
//
//    private void sendTopUpMail(Account account, String message1, String message2) {
////        Mail mail = CommonMails.getTopUpRechargeMail(customer, message1, message2);
////        mail.setEnterprise(customer.getEnterprise());
////        try {
////            utilsEJB.sendMail(mail);
////        } catch (Exception ex) {
////            ex.printStackTrace();
////            logger.info(ex.getMessage());
////        }
//    }
//
//
//    public PurchaseBalanceAccount purchaseBalanceAccount(EJBRequest request) throws NullParameterException, GeneralException {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//    public OrderRecharge rechargePinByAccount(EJBRequest request) throws NullParameterException, GeneralException {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
}
