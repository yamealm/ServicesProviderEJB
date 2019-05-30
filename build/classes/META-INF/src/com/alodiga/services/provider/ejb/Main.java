/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.alodiga.services.provider.ejb;

import com.alodiga.services.provider.commons.models.Product;
import com.alodiga.ws.salesrecord.services.WsInvoiceListResponse;
import com.alodiga.ws.salesrecord.services.WsLoginResponse;
import com.alodiga.ws.salesrecord.services.WsOrderListResponse;
import com.alodiga.ws.salesrecord.services.WsSalesRecordProxy;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lromero
 */

public class Main {

     private static String loadTokenOrdenInvoive() {
        String token = null;
        try {
            WsSalesRecordProxy salesRecordProxy = new WsSalesRecordProxy();
            WsLoginResponse loginResponse = salesRecordProxy.loginSalesRecord("123456", "28e1cbc2e786eb013d66c9a3334961d0");
            System.out.println("codeLoginOrder"+loginResponse.getCode());
            if (loginResponse.getCode().equals("0")) {
                token = loginResponse.getToken();
            }
        } catch (Exception ex1) {
            ex1.printStackTrace();
        }
        return token;
    }

     public static void main(String[] args) {

//      WsSalesRecordProxy salesRecordProxy = new WsSalesRecordProxy();
//        try {
//            String token = loadTokenOrdenInvoive();
//           WsInvoiceListResponse response = salesRecordProxy.getInvoicesByUser(token, "13");
//           System.out.println("code:"+response.getCode());
//           WsOrderListResponse response1 = salesRecordProxy.getOrdersByUser(token, "13");
//           System.out.println("code:"+response1.getCode());
//        } catch (RemoteException ex) {
//            ex.printStackTrace();
//        }

        Float realAmount = 24f - (((24f * 22.92f) / 100)*(97.5f/100));
        Product p = new Product();
        p.setId(new Long(3));
        if ( p.getId().longValue() == Product.TOP_UP_PRODUCT_ID.longValue()){
            System.out.println("1"+p.getId().toString());
            System.out.println("2"+Product.TOP_UP_PRODUCT_ID.toString());
             System.out.println("son iguakes");
         }
        System.out.println("realAmount"+realAmount);
     }

}


