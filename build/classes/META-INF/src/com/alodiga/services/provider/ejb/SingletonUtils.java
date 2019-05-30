/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.alodiga.services.provider.ejb;

import com.alodiga.services.provider.commons.exceptions.GeneralException;
import com.alodiga.services.provider.commons.models.Invoice;
import javax.persistence.EntityManager;
import javax.persistence.Query;

/**
 *
 * @author christiang
 */

class SingletonUtils {

       private static SingletonUtils theInstance;

    public static SingletonUtils getInstance() {
        if (theInstance == null) {
            theInstance = new SingletonUtils();
        }

        return theInstance;
    }

 

    private SingletonUtils() {
        super();
    }



     public synchronized Invoice saveInvoice(Invoice invoice, EntityManager entityManager) throws GeneralException {
        try {
            if (invoice.getId() == null) {
                Long enterpriseId = invoice.getAccount().getEnterprise().getId();
                Long invoiceId = 0L;
                try {
                    Query query = entityManager.createQuery("SELECT MAX(i.invoice.id) FROM Invoice i WHERE i.invoice.id.enterpriseId=" + enterpriseId);
                    query.setMaxResults(1);
                    invoiceId = (Long) query.getSingleResult();
                    if(invoiceId==null){
                    	invoiceId= new Long(0);
                    }
                } catch (Exception e) {
                    //TODO: No encuentra resultados porque no hay invoice en la tabla correspondiente
                    System.out.println("No se encontraron facturas, emitiendo primera factura");
                }
                invoice.setId(invoiceId + 1);
                entityManager.persist(invoice);
            } else {
                entityManager.merge(invoice);
            }
            //Hace merge de los InvoiceDetail

            return invoice;
        } catch (Exception e) {
            throw new GeneralException(e.getMessage(), e.getStackTrace());
        }
    }

}
