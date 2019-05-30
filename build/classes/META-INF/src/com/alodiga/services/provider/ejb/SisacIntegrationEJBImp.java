package com.alodiga.services.provider.ejb;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.interceptor.Interceptors;
import org.apache.log4j.Logger;
import com.alodiga.services.provider.commons.ejbs.SisacIntegrationEJB;
import com.alodiga.services.provider.commons.ejbs.SisacIntegrationEJBLocal;
import com.alodiga.services.provider.commons.services.models.CancelProvisionResponse;
import com.alodiga.services.provider.commons.services.models.ExtendedPinDataResponse;
import com.alodiga.services.provider.commons.services.models.OrderRecharge;
import com.alodiga.services.provider.commons.services.models.ProvisionPinResponse;
import com.alodiga.services.provider.commons.services.models.PurchaseBalanceAccount;
import com.alodiga.services.provider.commons.services.models.WSConstants;
import com.alodiga.services.provider.commons.exceptions.AmountConsumedException;
import com.alodiga.services.provider.commons.exceptions.GeneralException;
import com.alodiga.services.provider.commons.exceptions.InvalidCreditCardException;
import com.alodiga.services.provider.commons.exceptions.NullParameterException;
import com.alodiga.services.provider.commons.exceptions.DisabledPinException;
import com.alodiga.services.provider.commons.exceptions.PinFreeNotfoundException;
import com.alodiga.services.provider.commons.exceptions.PinProvisionException;
import com.alodiga.services.provider.commons.exceptions.PurchaseCanceledException;
import com.alodiga.services.provider.commons.genericEJB.AbstractSPEJB;
import com.alodiga.services.provider.commons.genericEJB.SPContextInterceptor;
import com.alodiga.services.provider.commons.genericEJB.SPLoggerInterceptor;
import com.alodiga.services.provider.commons.genericEJB.EJBRequest;
import com.alodiga.services.provider.commons.models.PinFreeResponse;
import com.alodiga.services.provider.commons.models.ResponseAddress;
import com.alodiga.services.provider.commons.models.ResponseCustomer;
import com.alodiga.services.provider.commons.utils.EjbConstants;
import com.sg123.exception.InvalidAmountException;
import com.sg123.model.platformprepay.PinFree;
import com.sg123.model.platformprepay.PinStatus;
import com.sg123.model.utils.OrderStatus;

@Interceptors({SPLoggerInterceptor.class, SPContextInterceptor.class})
@Stateless(name = EjbConstants.SISAC_INTEGRATION_EJB, mappedName = EjbConstants.SISAC_INTEGRATION_EJB)
@TransactionManagement(TransactionManagementType.BEAN)
public class SisacIntegrationEJBImp extends AbstractSPEJB implements SisacIntegrationEJB, SisacIntegrationEJBLocal {

    private static final Logger logger = Logger.getLogger(SisacIntegrationEJBImp.class);
    public static String userToken;

    public ExtendedPinDataResponse getPinfreeByAni(Long languageId, String ani) throws NullParameterException, GeneralException, PinFreeNotfoundException,DisabledPinException {

        if (languageId == null) {
            throw new NullParameterException( sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_LANGUAGE_ID), null);
        }

        if (ani == null || ani.equals("")) {
            throw new NullParameterException( sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_ANI), null);
        }
        ExtendedPinDataResponse extendedPinDataResponse = new ExtendedPinDataResponse();
        com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);

        try {
            Long serial = pinEJB.getPinFreeByAni(Long.parseLong(ani)).getPin().getSerial();
            com.sg123.model.platformprepay.Pin pin = pinEJB.loadPin(serial);
            if(!pin.getPinStatus().getId().equals(PinStatus.PIN_AVAILABLE_STATE)){
                throw new DisabledPinException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), null);
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
            	rCustomer.setBalance( pin.getCurrentBalance().toString());
            }
            rCustomer.setAddress(rAddrees);
            rCustomer.setPin(serial.toString());

            List<PinFreeResponse> rPinFrees = new ArrayList<PinFreeResponse>();

            for (com.sg123.model.platformprepay.PinFree pinFree : pin.getPinsFree()) {
                PinFreeResponse rPinFree = new PinFreeResponse();
                rPinFree.setCodeId(pinFree.getCode().getId().toString());
                rPinFree.setSerial(pinFree.getPin().getSerial().toString());
                rPinFree.setAni(pinFree.getAni().toString());
                rPinFree.setPinFreeId(pinFree.getId().toString());
                rPinFrees.add(rPinFree);
            }
            rCustomer.setPinFrees(rPinFrees);
            extendedPinDataResponse.setCustomer(rCustomer);
        } catch (com.sg123.exception.PinFreeNotFoundException e) {
            throw new PinFreeNotfoundException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), e);
        } catch (DisabledPinException e) {
            e.printStackTrace();
            throw new DisabledPinException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), null);
        } catch (Exception e) {
            e.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), null);
        }
        return extendedPinDataResponse;
    }

    public ProvisionPinResponse purchasePin(EJBRequest request) throws NullParameterException, GeneralException, PinProvisionException {

        Map<String, Object> params = request.getParams();

        if (!params.containsKey(WSConstants.PARAM_LANGUAGE_ID)) {
            throw new NullParameterException( sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_LANGUAGE_ID), null);
        }
        if (!params.containsKey(WSConstants.PARAM_PIN_BALANCE)) {
            throw new NullParameterException( sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_PIN_BALANCE), null);
        }
        if (!params.containsKey(WSConstants.PARAM_ANI_LIST)) {
            throw new NullParameterException( sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_ANI_LIST), null);
        }
        if (!params.containsKey(WSConstants.PARAM_DISTRIBUTOR)) {
            throw new NullParameterException( sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_DISTRIBUTOR), null);
        }
        List<String> aniList = (List<String>) params.get(WSConstants.PARAM_ANI_LIST);
        String aniString = "";
        for (String ani : aniList) {
            aniString += ani + ",";
        }

        aniString = aniString.substring(0, aniString.length() - 1);
        String account = params.get(WSConstants.PARAM_DISTRIBUTOR).toString();
        String transactionData = "Compra Pin Electronico [monto)" + params.get(WSConstants.PARAM_PIN_BALANCE) + "(monto] - [anis)" + aniString+"(anis]" + "[tienda)" + account+"(tienda]";
        Long languageId = Long.parseLong(params.get(WSConstants.PARAM_LANGUAGE_ID).toString());
        Long enterpriseId = Long.parseLong(params.get(WSConstants.PARAM_ENTERPRISE_ID).toString());
        String login = params.get(WSConstants.PARAM_CUSTOMER_LOGIN).toString();
        String password = params.get(WSConstants.PARAM_PASSWORD).toString();
        String firstName = params.get(WSConstants.PARAM_CUSTOMER_FIRSTNAME).toString();
        String lastName = params.get(WSConstants.PARAM_CUSTOMER_LASTNAME).toString();
        String email = params.get(WSConstants.PARAM_CUSTOMER_EMAIL).toString();
        Long customServiceId = WSConstants.CUSTOM_SERVICE_ID;
        Float pinBalance = Float.parseFloat(params.get(WSConstants.PARAM_PIN_BALANCE).toString());

        Long countryId = Long.parseLong(params.get(WSConstants.PARAM_COUNTRY_ID).toString());
        String customerAddress = params.get(WSConstants.PARAM_ADDRESS).toString();

        ProvisionPinResponse pinResponse = new ProvisionPinResponse();

        com.sg123.ejb.ContractEJB contractEJB = (com.sg123.ejb.ContractEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CONTRACT_EJB);
        com.sg123.ejb.UserEJB userEJB = (com.sg123.ejb.UserEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.USER_EJB);
        com.sg123.ejb.PromotionsManagementEJB promotionsManagementEJB = (com.sg123.ejb.PromotionsManagementEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PROMOTIONSMANAGEMENTEJB);
        com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);
        com.interax.telephony.service.remote.callingcard.CallingCardPinEJB callingCardPinEJB = (com.interax.telephony.service.remote.callingcard.CallingCardPinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CALLING_CARD_PIN_EJB);

        com.sg123.model.WebUser webUser = null;
        com.sg123.model.Language language = new com.sg123.model.Language();
        com.sg123.model.contract.Address address = new com.sg123.model.contract.Address();
        com.sg123.model.utils.Country country = new com.sg123.model.utils.Country();
        com.sg123.model.utils.Enterprise enterprise = new com.sg123.model.utils.Enterprise();
        List<String> errors = new ArrayList<String>();

        Date now = new Date((new java.util.Date()).getTime());
        Timestamp nowTimestamp = new Timestamp(now.getTime());
        language.setId(languageId);
        enterprise.setId(enterpriseId);
        country.setId(countryId);
        address.setAddress1(customerAddress);
        address.setCountry(country);
        try {
            webUser = userEJB.getWebUserByLogin(login, enterprise.getId());
        } catch (com.sg123.exception.WebUserNotFoundException e) {
            // Si no existe el usuario se crea uno nuevo
            com.sg123.model.contract.Customer customer = new com.sg123.model.contract.Customer();
            customer.setEnterprise(enterprise);
            customer.setName(firstName);
            customer.setLastName(lastName);
            customer.setPhone(login);
            customer.setEmail(email);
            customer.setCreated(nowTimestamp);
            com.sg123.model.utils.TinType tinType = new com.sg123.model.utils.TinType();
            tinType.setId(com.sg123.model.utils.TinType.GENERADO);
            customer.setTinType(tinType);
            customer.setTin(now.getTime() + "");
            customer.setBalance(0F);
            customer.setAddress(address);
            webUser = new com.sg123.model.WebUser();
            webUser.setLogin(login);
            webUser.setPassword(password);
            webUser.setConfirmDate(nowTimestamp);
            webUser.setEnterprise(enterprise);
            webUser.setCreationDate(nowTimestamp);
            webUser.setEnabled(1);
            webUser.setIsAdmin(0);
            webUser.setLanguage(language);
            webUser.setPrincipalCustomer(customer);
            com.sg123.model.utils.SalesChannel salesChannel = new com.sg123.model.utils.SalesChannel();
            salesChannel.setId(com.sg123.model.utils.SalesChannel.SISAC);
            webUser.setSalesChannel(salesChannel);

            List<com.sg123.model.contract.relation.CustomerHasWebUser> customerWebUsers = new ArrayList<com.sg123.model.contract.relation.CustomerHasWebUser>();
            com.sg123.model.contract.relation.CustomerHasWebUser customerHasWebUser = new com.sg123.model.contract.relation.CustomerHasWebUser();
            customerHasWebUser.setCustomer(customer);
            customerHasWebUser.setBeginningDate(new Timestamp(now.getTime()));
            customerHasWebUser.setIsPrincipal(1L);
            customerHasWebUser.setWebUser(webUser);
            customerHasWebUser.setEndingDate(null);
            customerWebUsers.add(customerHasWebUser);
            webUser.setCustomerWebUsers(customerWebUsers);
            try {
                webUser = userEJB.registerAndConfirm(webUser, enterprise.getId());
            } catch (com.sg123.exception.GeneralException ex) {
                throw new PinProvisionException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), null);
            }

        } catch (com.sg123.exception.WebUserIsDisabledException e) {
            throw new PinProvisionException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), e);
        } catch (com.sg123.exception.GeneralException e) {
            errors.add(e.getMessage());
            throw new PinProvisionException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), e);
        }

        try {
            Map pinData = contractEJB.provisionDistributionPinPurchase(customServiceId, webUser.getId(), pinBalance, transactionData);
            com.sg123.model.platformprepay.Pin pin = (com.sg123.model.platformprepay.Pin) pinData.get("PIN");
            com.sg123.model.ecommerce.Order order = (com.sg123.model.ecommerce.Order) pinData.get("ORDER");
            List<com.sg123.model.promotion.PromotionItem> promotionItems = null;
            try {
                promotionItems = promotionsManagementEJB.getPromotionItemsByPin(pin.getSerial());
            } catch (Exception e) {
                e.printStackTrace();
            }
            pinResponse.setSerial(pin.getSerial().toString());
            pinResponse.setSecret(pin.getSecret().toString());
            pinResponse.setSisacOrder(order.getId().toString());
            pinResponse.setMesssage("Transaccion exitosa");
            //pinResponse
            if (promotionItems != null && !promotionItems.isEmpty()) {
                //pinResponse.setPromotions(new ArrayList<String>());
                List<String> promotions = new ArrayList<String>();
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
                if (!promotions.isEmpty()) {
                    pinResponse.setPromotions(promotions);
                }
                //xmlReturn.append("</promotions>");
            }

            String[] stringAnis = aniString.split(",");
            Long[] anis = new Long[stringAnis.length];
            String ani = stringAnis[0];

            for (int i = 0; i
                    < stringAnis.length; i++) {
                anis[i] = new Long(stringAnis[i]);
            }
            pinEJB.savePinFree(pin.getSerial(), anis);
            List<String> accessNumbers = new ArrayList<String>();
            Long quantity = 1L;
            try {
                accessNumbers = callingCardPinEJB.getAccessNumbersByAni(customServiceId, ani, quantity);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            pinResponse.setAccessNumbers(accessNumbers);
        } catch (Exception e) {
            throw new PinProvisionException(logger, sysError.format(EjbConstants.ERR_NOT_PIN_PROVISION, this.getClass(), getMethodName(), null), e);
        }
        return pinResponse;
    }
//COMENTADO - LEONEL
//    public PurchaseBalanceAccount purchaseBalanceAccount(EJBRequest request) throws NullParameterException, GeneralException, InvalidCreditCardException {
//        Map<String, Object> params = request.getParams();
//        PurchaseBalanceAccount purchaseBalanceAccount = new PurchaseBalanceAccount();
//        if (!params.containsKey(WSConstants.PARAM_LANGUAGE_ID)) {
//            throw new NullParameterException( sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_LANGUAGE_ID), null);
//        }
//
//        if (!params.containsKey(QueryConstants.PARAM_TRANSACTION)) {
//            throw new NullParameterException( sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), QueryConstants.PARAM_TRANSACTION), null);
//        }
//        Transaction transaction = (Transaction) params.get(QueryConstants.PARAM_TRANSACTION);
//        com.sg123.ejb.UserEJB userEJB = (com.sg123.ejb.UserEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.USER_EJB);
//        com.interax.telephony.service.remote.balancerecharge.BalanceRechargeEJB balanceRechargeEJB = (com.interax.telephony.service.remote.balancerecharge.BalanceRechargeEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.BALANCE_RECHARGE_EJB);
//
//        PaymentInfo transactionPayment = transaction.getPaymentInfo();
//        Address address = transactionPayment.getAddress();
//        com.sg123.model.contract.Contract contract = null;
//        com.interax.telephony.service.balancerecharge.data.BalanceRechargePaymentInfo paymentInfo = null;
//        Long enterpriseId = WSConstants.ENTERPRISE_ID;
//        String currenceSymbol = "$";
//
//        try {
//            paymentInfo = new com.interax.telephony.service.balancerecharge.data.BalanceRechargePaymentInfo();
//            //paymentInfo.setId(transaction.getPaymentInfo().getId());//OJO CON ESTO
//            com.sg123.model.WebUser webUser = userEJB.loadWebUserById(WSConstants.WEB_USER_ID_SISAC);
//            com.sg123.model.contract.Customer customer = webUser.getPrincipalCustomer();
//
//            String cvv = transactionPayment.getCreditCardCvv();
//            String month = transactionPayment.getCreditCardDate().getMonth() + 1 + "";
//            String year = transactionPayment.getCreditCardDate().getYear() + 1900 + "";
//            String credictCardNumber = transactionPayment.getCreditCardNumber();
//            String credictCardType = transactionPayment.getCreditcardType().getName();
//            String zipCode = address.getZipCode();
//            Long countryId = new Long(address.getCountry().getId());
//            Long stateId = address.getState() != null ? address.getState().getId() : null;
//            String stateName = address.getStateName();
//            Long countyId = address.getCounty() != null ? address.getCounty().getId() : null;
//            String countyName = address.getCountyName();
//            Long cityId = address.getCity() != null ? address.getCity().getId() : null;
//            String cityName = address.getCityName();
//            String customerAddress = address.getAddress();
//            String creditCardName = transactionPayment.getCreditCardName();
//            //OJO CABLEADO
//            if (year.length() == 2) {
//                year = "20" + year;
//            }
//
//            paymentInfo.setCreditCardNumber(credictCardNumber);
//            paymentInfo.setExpirationMonth(month);
//            paymentInfo.setExpirationYear(year);
//            paymentInfo.setCvv(cvv);
//            paymentInfo.setCreditCardType(credictCardType);
//
//            com.interax.telephony.service.balancerecharge.data.BalanceRechargeAddress paymentInfoAddress = new com.interax.telephony.service.balancerecharge.data.BalanceRechargeAddress();
//            paymentInfoAddress.setCountryId(countryId);
//            paymentInfoAddress.setStateId(stateId);
//            paymentInfoAddress.setStateName(stateName);
//            paymentInfoAddress.setCountyId(countyId);
//            paymentInfoAddress.setCountyName(countyName);
//            paymentInfoAddress.setCityId(cityId);
//            paymentInfoAddress.setCityName(cityName);
//            paymentInfoAddress.setAddress1(customerAddress);
//            paymentInfoAddress.setZipCode(zipCode);
//
//            if (paymentInfo.getId() == null) {
//                try {
//                    balanceRechargeEJB.validateCreditCard(paymentInfo.getCreditCardNumber());
//                    validateCreditCardMonth(paymentInfo);
//                    validateCreditCardYear(paymentInfo);
//                } catch (InvalidCreditCardException e) {
//                    e.printStackTrace();
//                    throw new InvalidCreditCardException("El numero de tarjeta de credito es invalido. Razon:" + e.getMessage());
//                }catch (Exception e) {
//                    e.printStackTrace();
//                    throw new Exception("El numero de tarjeta de credito es invalido. Razon:" + e.getMessage());
//                }
//                paymentInfo.setCustomerId(customer.getId());
//                try {
//                    if (contract == null) {
//                        paymentInfo = balanceRechargeEJB.saveSisacPaymentInfoDistribution(paymentInfo, null, paymentInfoAddress, creditCardName);
//                    }
//                    if (contract != null && contract.getCustomer().getId() != null) {
//                        paymentInfo.setCustomerId(contract.getCustomer().getId());
//                        paymentInfo = balanceRechargeEJB.saveSisacPaymentInfoDistribution(paymentInfo, contract.getId(), paymentInfoAddress, creditCardName);
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    throw new Exception("Error al salvar el nuevo PaymentInfo:" + e.getMessage());
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            purchaseBalanceAccount.setMessages("Error al procesar Pago. Integracion ESP - SISAC" + e.getMessage());
//            return purchaseBalanceAccount;
//        }
//
//        com.interax.telephony.service.balancerecharge.data.RechargeOrder order = null;
//        try {
//            String rechargeSource = com.interax.telephony.service.balancerecharge.data.RechargePreOrder.RECHARGE_DISTRIBUTION_SRC;
//            Float amount = transaction.getTotalAmount();
//            com.interax.telephony.service.balancerecharge.data.RechargeOption rechargeOption = new com.interax.telephony.service.balancerecharge.data.RechargeOption();
//            com.interax.telephony.service.balancerecharge.data.BalanceRechargeCurrency currency = new com.interax.telephony.service.balancerecharge.data.BalanceRechargeCurrency();
//            currency.setId(2L);
//            currency.setAbbreviation("USD");
//            rechargeOption.setCurrency(currency);
//            com.interax.telephony.service.balancerecharge.data.RechargePreOrder preOrder = buildRechargePreOrder(contract, rechargeOption, paymentInfo, enterpriseId);
//            if (rechargeSource != null) {
//                preOrder.setRechargeSource(rechargeSource);
//                preOrder.setItemDescription("Compra de saldo [monto)"+amount+"(monto]"+ "[tienda)" +transaction.getCustomer().getLogin()+ " "+ transaction.getCustomer().getFirstName()+"(tienda]");
//            }
//            order = balanceRechargeEJB.getOrder(preOrder);
//
//            com.interax.telephony.service.balancerecharge.data.RechargeInvoice invoice = null;
//            invoice = balanceRechargeEJB.processOrder(order);
//
//            InvoiceAccount invoiceAccount = new InvoiceAccount();
//            invoiceAccount.setTransactionNumber(invoice.getTransactionNumber());
//            invoiceAccount.setRecharge(format(invoice.getRechargedBalance()));
//            invoiceAccount.setTotal(format(invoice.getRechargedBalance() + invoice.getTaxChargesTotal()));
//            invoiceAccount.setTaxChargesTotal(format(invoice.getTaxChargesTotal()));
//            invoiceAccount.setCurrency(currenceSymbol);
//            invoiceAccount.setOrderId(invoice.getOrderId().toString());
//            purchaseBalanceAccount.setInvoice(invoiceAccount);
//        } catch (Exception e) {
//            e.printStackTrace();
//            purchaseBalanceAccount.setMessages("Error procesando orden " + e.getMessage());
//            return purchaseBalanceAccount;
//        }
//
//        return purchaseBalanceAccount;
//    }

    public static String format(Float amount) {
        DecimalFormat formatter = new DecimalFormat("#########0.00");
        return formatter.format(amount);
    }

    private com.interax.telephony.service.balancerecharge.data.RechargePreOrder buildRechargePreOrder(com.sg123.model.contract.Contract contract, com.interax.telephony.service.balancerecharge.data.RechargeOption rechargeOption, com.interax.telephony.service.balancerecharge.data.BalanceRechargePaymentInfo paymentInfo, Long enterpriseId) {

        com.interax.telephony.service.balancerecharge.data.RechargePreOrder preOrder = new com.interax.telephony.service.balancerecharge.data.RechargePreOrder();
        preOrder.setBalanceToRecharge(rechargeOption.getAmount());
        if (contract == null) {
            preOrder.setContractId(null);
        } else if (contract.getId() != null) {
            preOrder.setContractId(contract.getId());
        } else if (contract != null) {
            if (contract.getId() != null) {
                preOrder.setContractId(contract.getId());
            } else {
                preOrder.setSisacPinSerial(contract.getId());
            }
        }
        preOrder.setCurrency(rechargeOption.getCurrency());
        preOrder.setEnterpriseId(enterpriseId);
        preOrder.setPaymentInfo(paymentInfo);

        return preOrder;
    }

    private void validateCreditCardMonth(com.interax.telephony.service.balancerecharge.data.BalanceRechargePaymentInfo paymentInfo) throws Exception {

        Integer month = Integer.parseInt(paymentInfo.getExpirationMonth());
        if (!(month > 0 && month < 13)) {
            throw new Exception("El mes de vencimiento no se encuentra en el rango correcto");
        }
    }

    private void validateCreditCardYear(com.interax.telephony.service.balancerecharge.data.BalanceRechargePaymentInfo paymentInfo) throws Exception {

        Calendar now = Calendar.getInstance();
        int intCreditCardMonth = (new Integer(paymentInfo.getExpirationMonth())).intValue();
        int intCreditCardYear = (new Integer(paymentInfo.getExpirationYear())).intValue();
        int currentMonth = now.get(Calendar.MONTH) + 1;
        int currentYear = now.get(Calendar.YEAR);
        //Validación de fecha
        if ((intCreditCardYear > currentYear) || (intCreditCardYear == currentYear && intCreditCardMonth >= currentMonth)) {
            paymentInfo.setExpirationYear("" + intCreditCardYear);
        } else {
            throw new Exception("El año de vencimiento es incorrecto");
        }

    }
//COMENTADO - LEONEL
//    public OrderRecharge rechargePinByCustomer(EJBRequest request) throws NullParameterException, GeneralException {
//        Map<String, Object> params = request.getParams();
//        OrderRecharge orderRecharge = new OrderRecharge();
//
//        if (!params.containsKey(WSConstants.PARAM_LANGUAGE_ID)) {
//            throw new NullParameterException( sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_LANGUAGE_ID), null);
//        }
//        if (!params.containsKey(WSConstants.PARAM_SERIAL)) {
//            throw new NullParameterException( sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_SERIAL), null);
//        }
//        if (!params.containsKey(QueryConstants.PARAM_TRANSACTION) && !(params.get(QueryConstants.PARAM_TRANSACTION) instanceof Transaction)) {
//            throw new NullParameterException( sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), QueryConstants.PARAM_TRANSACTION), null);
//        }
//
//        Transaction transaction = (Transaction) params.get(QueryConstants.PARAM_TRANSACTION);
//        Long customServiceId = WSConstants.CUSTOM_SERVICE_ID;
//        Long webUserId = Long.parseLong((WSConstants.WEB_USER_ID_SISAC).toString());
//        Long serial = Long.parseLong((params.get(WSConstants.PARAM_SERIAL)).toString());
//        float amount = transaction.getTotalAmount();
//
//        com.sg123.ejb.ContractEJB contractEJB = (com.sg123.ejb.ContractEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CONTRACT_EJB);
//        com.sg123.ejb.UserEJB userEJB = (com.sg123.ejb.UserEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.USER_EJB);
//        com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);
//        com.sg123.ejb.ContentEJB contentEJB = (com.sg123.ejb.ContentEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CONTENT_EJB);
//        com.sg123.ecommerce.ShoppingCart shoppingCart = new com.sg123.ecommerce.ShoppingCart();
//        Date now = new Date((new java.util.Date()).getTime());
//
//        com.sg123.serviceconfig.RechargeConfig rechargeConfig = new com.sg123.serviceconfig.RechargeConfig();
//
//        com.sg123.model.payment.PaymentInfo paymentInfo = null;
//        float newBalance = 0f;
//
//        try {
//            com.sg123.model.ecommerce.CustomService customService = null;
//            com.sg123.model.WebUser webUser = null;
//            com.sg123.model.content.Segment segment = null;
//
//            try {
//                customService = contentEJB.loadCustomService(customServiceId);
//            } catch (com.sg123.exception.CustomServiceNotFoundException e) {
//                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), e);
//            }
//
//            segment = customService.getService().getSegment();
//            webUser = userEJB.loadWebUserById(webUserId);
//            com.sg123.model.platformprepay.Pin pin = null;
//            try {
//                pin = pinEJB.loadPin(serial);
//                newBalance = pin.getCurrentBalance() + amount;
//            } catch (com.sg123.exception.PinNotFoundException e) {
//                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), e);
//            }
//
//            shoppingCart.setWebUser(webUser);
//            shoppingCart.setSegmentId(segment.getId());
//            shoppingCart.setLastUpdate(now);
//            rechargeConfig.setPinSerial(serial);
//            rechargeConfig.setAmount(amount);
//            rechargeConfig.setTaxIncluded(customService.isTaxInclude() ? 1 : 0);
//            com.sg123.model.ecommerce.Item item = new com.sg123.model.ecommerce.Item(rechargeConfig, 1L, amount);
//            item.setImage(null);
//            item.setCustomService(null);
//            String transactionData = "Recarga Pin Electronico [monto)" + amount + "(monto] - [serial)" + serial+"(serial]" + "[tienda)" + transaction.getCustomer().getLogin() + " "+transaction.getCustomer().getFirstName() +"(tienda]";
//            item.setName(transactionData);
//            item.setTaxAmount(0F);
//            // TODO PREGUNTAR
//            item.setUnitAmount(0F);
//
//            item.setTotalAmount(0F);
//
//            String description = "Pin serial: " + serial;
//            description += "<br/>" + " Amount: " + amount + customService.getCurrency().getSymbol();
//            item.setDescription(description);
//            shoppingCart.addItem(item);
//            shoppingCart.setPaymentInfo(paymentInfo);
//            try {
//                com.sg123.model.ecommerce.Order order = contractEJB.processShoppingCart(shoppingCart);
//                order.setSubtotal(0F);
//                order.setTaxTotal(0F);
//                order.setTotal(0F);
//                com.sg123.model.WebUserSession webUserSession = userEJB.loadWebUserSessionByWebUser(webUser.getId());
//                userEJB.updateWebUserSessionOrder(webUserSession.getId(), order);
//                order = contractEJB.rechargeDistributionPinPurchase(customServiceId, webUserId, order);
//                //lo retorna el webservice
//                orderRecharge.setMessage("Transaccion Exitosa");
//                orderRecharge.setBalance(String.valueOf(newBalance));
//                orderRecharge.setSisacOrder(String.valueOf(order.getId()));
//
//            } catch (com.sg123.exception.InvalidPaymentInfoException e) {
//
//                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), e);
//            } catch (com.sg123.exception.PaymentDeclinedException e) {
//                throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), e);
//            }
//        } catch (com.sg123.exception.NullParameterException e) {
//            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), e);
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), e);
//        }
//
//        return orderRecharge;
//    }

    public void deletePinFree(EJBRequest request) throws NullParameterException, GeneralException, AmountConsumedException {
    	CancelProvisionResponse  cancelProvisionResponse = new CancelProvisionResponse();
    	Map<String, Object> params = request.getParams();

         EJBRequest requestDelete = new EJBRequest();
         Map<String, Object> requestParams = new HashMap<String, Object>();

         if (!params.containsKey(WSConstants.PARAM_LANGUAGE_ID)) {
             throw new NullParameterException( sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_LANGUAGE_ID), null);
         }

         if (!params.containsKey(WSConstants.PARAM_SERIAL)) {
             throw new NullParameterException( sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_SERIAL), null);
         }

         if (!params.containsKey(WSConstants.PARAM_ANI_LIST)) {
             throw new NullParameterException( sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_ANI_LIST), null);
         }

         String anisList = params.get(WSConstants.PARAM_ANI_LIST).toString();
         Long orderId =  Long.parseLong(params.get(WSConstants.PARAM_ORDER_ID).toString());
         Long serial = Long.parseLong(params.get(WSConstants.PARAM_SERIAL).toString());

//         requestParams.put(WSConstants.PARAM_LANGUAGE_ID, params.get(WSConstants.PARAM_LANGUAGE_ID).toString());
//         requestParams.put(WSConstants.PARAM_SERIAL, params.get(WSConstants.PARAM_SERIAL).toString());
//         requestParams.put(WSConstants.PARAM_ANI_LIST, params.get(WSConstants.PARAM_ANI_LIST).toString());
//         requestParams.put(WSConstants.PARAM_ORDER_ID, params.get(WSConstants.PARAM_ORDER_ID).toString());

         com.sg123.ejb.PinEJB pinEJB = ( com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);
         com.sg123.ejb.ContractEJB contractEJB = (com.sg123.ejb.ContractEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CONTRACT_EJB);

         String[] stringAnis = anisList.split(",");
         try {
             for (int i = 0; i < stringAnis.length; i++) {
                 try
                 {
                 	if(!contractEJB.authorizeCancelDistributionOrder(orderId))
                 	{
                 		throw new InvalidAmountException("Amount consumed");
                 	}
                     PinFree pinFree;
                     pinFree = pinEJB.getPinFreeByAni(new Long(stringAnis[i]));
                     pinEJB.deletePinFree(pinFree.getId());
                 } catch ( NumberFormatException e) {
                	 e.printStackTrace();
                	 throw new NumberFormatException(e.getMessage());
                 } catch (com.sg123.exception.PinFreeNotFoundException e) {
                     e.printStackTrace();
                 } catch (InvalidAmountException e) {
 					e.printStackTrace();
 					throw new AmountConsumedException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
 				}
             }
             pinEJB.deletePin(serial);
             contractEJB.changeOrderStatus(orderId, OrderStatus.DEVUELTA);
             contractEJB.changeOrderStatus(orderId, OrderStatus.DEVUELTA);
         } catch (com.sg123.exception.NullParameterException e) {
        	 throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
         } catch (com.sg123.exception.GeneralException e) {
        	 e.printStackTrace();
        	 throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
         }
    }

     public void deletePinFree(String ani) throws NullParameterException, GeneralException {
        if (ani == null) {
            throw new NullParameterException( sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "ani"), null);
        }
        com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);

        try {
            PinFree pinFree;
            pinFree = pinEJB.getPinFreeByAni(new Long(ani));
            pinEJB.deletePinFree(pinFree.getId());
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
    }


    public void cancelPurchaseBalance(EJBRequest request) throws NullParameterException, GeneralException, PurchaseCanceledException,AmountConsumedException{

    	Map<String, Object> params = request.getParams();

    	if (!params.containsKey(WSConstants.PARAM_LANGUAGE_ID)) {
    		throw new NullParameterException( sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_LANGUAGE_ID), null);
    	}
    	if (!params.containsKey(WSConstants.PARAM_ORDER_ID)) {
    		throw new NullParameterException( sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_ORDER_ID), null);
    	}

    	com.sg123.ejb.ContractEJB contractEJB = ( com.sg123.ejb.ContractEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.CONTRACT_EJB);
    	Long orderId = Long.parseLong(params.get(WSConstants.PARAM_ORDER_ID).toString());
    	try {
    		if(!contractEJB.authorizeCancelDistributionOrder(orderId))
    		{
    			throw new InvalidAmountException("Amount consumed");
    		}
    		contractEJB.cancelOrder(orderId);
    	}catch(InvalidAmountException e){
    		e.printStackTrace();
    		throw new AmountConsumedException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
    	}catch (Exception e) {
    		e.printStackTrace();
    		throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), e.getMessage()), null);
    	}

    }

    public Boolean cancelPurchaseAuthorizeBalance(Long orderId) throws NullParameterException, GeneralException{
        Boolean isProcessOrderVoiding = false;
        try {
            if (orderId == null) {
                throw new NullParameterException( sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_ORDER_ID), null);
            }
            com.sg123.ejb.InvoiceEJB invoiceEJB = (com.sg123.ejb.InvoiceEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.INVOICE_EJB);
            isProcessOrderVoiding = invoiceEJB.processOrderVoiding(orderId);

        } catch (com.sg123.exception.NullParameterException ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        } catch (com.sg123.exception.GeneralException ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_GENERAL_EXCEPTION, this.getClass(), getMethodName(), ex.getMessage()), ex);
        }
        return isProcessOrderVoiding;
    }

 public void savePinFree(String serial, String pinFree) throws NullParameterException, GeneralException {
        try {
            if (serial == null) {
                throw new NullParameterException( sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "serial"), null);
            } else if (pinFree == null) {
                throw new NullParameterException( sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "pinFree"), null);
            }
            com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);
            Long[] anis = new Long[1];
            anis[0] = new Long(pinFree);
            pinEJB.savePinFree(new Long(serial), anis);
        } catch (com.sg123.exception.NullParameterException ex) {
            throw new NullParameterException( sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "serial"));
        } catch (Exception ex) {
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), "serial"), ex);
        }
    }

        public ExtendedPinDataResponse getPinBySerial(Long languageId, String serial) throws NullParameterException, GeneralException, PinFreeNotfoundException, DisabledPinException {

        if (languageId == null) {
            throw new NullParameterException( sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_LANGUAGE_ID), null);
        }

        if (serial == null || serial.equals("")) {
            throw new NullParameterException( sysError.format(EjbConstants.ERR_NULL_PARAMETER, this.getClass(), getMethodName(), WSConstants.PARAM_SERIAL), null);
        }
        ExtendedPinDataResponse extendedPinDataResponse = new ExtendedPinDataResponse();
        com.sg123.ejb.PinEJB pinEJB = (com.sg123.ejb.PinEJB) com.sg123.ejb.utils.EJBServiceLocator.getInstance().get(com.sg123.ejb.utils.EjbConstants.PIN_EJB);

        try {
            com.sg123.model.platformprepay.Pin pin = pinEJB.loadPin(new Long(serial));
            if (!pin.getPinStatus().getId().equals(PinStatus.PIN_AVAILABLE_STATE)) {
                throw new DisabledPinException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), null);
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

            List<PinFreeResponse> rPinFrees = new ArrayList<PinFreeResponse>();

            for (com.sg123.model.platformprepay.PinFree pinFree : pin.getPinsFree()) {
                PinFreeResponse rPinFree = new PinFreeResponse();
                rPinFree.setCodeId(pinFree.getCode().getId().toString());
                rPinFree.setSerial(pinFree.getPin().getSerial().toString());
                rPinFree.setAni(pinFree.getAni().toString());
                rPinFrees.add(rPinFree);
            }
            rCustomer.setPinFrees(rPinFrees);
            extendedPinDataResponse.setCustomer(rCustomer);
        } catch (DisabledPinException e) {
            e.printStackTrace();
            throw new DisabledPinException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), null);
        } catch (Exception e) {
            e.printStackTrace();
            throw new GeneralException(logger, sysError.format(EjbConstants.ERR_ERROR_RECHARGE, this.getClass(), getMethodName(), null), null);
        }
        return extendedPinDataResponse;
    }

    public PurchaseBalanceAccount purchaseBalanceAccount(EJBRequest request) throws NullParameterException, GeneralException, InvalidCreditCardException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OrderRecharge rechargePinByCustomer(EJBRequest request) throws NullParameterException, GeneralException {
        throw new UnsupportedOperationException("Not supported yet.");
    }


}
