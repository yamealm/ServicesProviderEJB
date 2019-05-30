package com.alodiga.services.provider.ejb;

import com.alodiga.services.provider.commons.ejbs.BillingsEJB;
import com.alodiga.services.provider.commons.ejbs.BillingsEJBLocal;
import com.alodiga.services.provider.commons.ejbs.ProductEJBLocal;
import com.alodiga.services.provider.commons.exceptions.EmptyListException;
import com.alodiga.services.provider.commons.exceptions.GeneralException;
import com.alodiga.services.provider.commons.exceptions.NullParameterException;
import com.alodiga.services.provider.commons.models.Account;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import org.apache.log4j.Logger;
import com.alodiga.services.provider.commons.ejbs.UserEJBLocal;
import com.alodiga.services.provider.commons.ejbs.UtilsEJBLocal;
import com.alodiga.services.provider.commons.exceptions.RegisterNotFoundException;
import com.alodiga.services.provider.commons.genericEJB.AbstractSPEJB;
import com.alodiga.services.provider.commons.genericEJB.EJBRequest;
import com.alodiga.services.provider.commons.genericEJB.SPContextInterceptor;
import com.alodiga.services.provider.commons.genericEJB.SPLoggerInterceptor;
import com.alodiga.services.provider.commons.managers.ContentManager;
import com.alodiga.services.provider.commons.models.Currency;
import com.alodiga.services.provider.commons.models.Invoice;
import com.alodiga.services.provider.commons.models.InvoiceStatus;
import com.alodiga.services.provider.commons.models.Transaction;
import com.alodiga.services.provider.commons.utils.EJBServiceLocator;
import com.alodiga.services.provider.commons.utils.EjbConstants;
import com.alodiga.services.provider.commons.utils.EjbUtils;
import com.alodiga.services.provider.commons.utils.Mail;
import com.alodiga.services.provider.commons.utils.QueryConstants;
import com.alodiga.services.provider.commons.utils.ServiceMails;
import com.sg123.ejb.InvoiceEJB;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.interceptor.Interceptors;
import javax.transaction.UserTransaction;

@Interceptors({SPLoggerInterceptor.class, SPContextInterceptor.class})
@Stateless(name = EjbConstants.BILLINGS_EJB, mappedName = EjbConstants.BILLINGS_EJB)
@TransactionManagement(TransactionManagementType.BEAN)
public class BillingsEJBImpl extends AbstractSPEJB implements BillingsEJB, BillingsEJBLocal {

    private static final Logger logger = Logger.getLogger(BillingsEJBImpl.class);

    @EJB
    private UserEJBLocal userEJB;
    @EJB
    private ProductEJBLocal productEJB;
    @EJB
    private UtilsEJBLocal utilsEJB;

    /********************************************************/
    private static final int DAYS_AFTER = 2;
    private static final int MAX_ATTEMPTS_NUMBER = 3;
    private String testBillingYesterday; //dd/MM/yyyy
    private String testBillingToday; //dd/MM/yyyy
    
    private int outstandingInvoices;

    private void initTestDates() {
        testBillingToday = "20/03/2013 00:00:00";//dd/MM/yyyy

        testBillingYesterday = "19/03/2013 00:00:00";//dd/MM/yyyy
    }

    private Date getDate(String strDate) {
        Date date = new Date();
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
            date = formatter.parse(strDate);
        } catch (java.text.ParseException ex) {
            java.util.logging.Logger.getLogger(BillingsEJBImpl.class.getName()).log(Level.SEVERE, null, ex);
            logger.error("Exception in method BillingEJB.getDate: ", ex);
        }
        return date;
    }

    protected void statusAccount(Long accountID) {

        Calendar calendar = Calendar.getInstance();


    }

    public static Calendar addDays(Date date, int daysNumber) {

        Calendar calendar = Calendar.getInstance();

        if (date != null) {
            calendar.setTime(date);
        }
        calendar.add(Calendar.DATE, +daysNumber);

        return calendar;
    }

    protected void runMainProcess() {
        Date today = null;
        Date yesterday = null;
        if (EjbConstants.DEVELOPMENT_BILLING) {
            initTestDates();
            yesterday = getDate(testBillingYesterday);
            today = getDate(testBillingToday);
        } else {
            today = new Date();
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(today);
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            calendar.set(Calendar.HOUR, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            yesterday = calendar.getTime();
        }

        Long enterpriseId = null;
        try {
            
            List<Account> accounts = utilsEJB.getAccounts();

            for (Account account : accounts) {
                account.getId();
                account.getEnterprise();

                if (!account.getIsPrePaid() && account.getNexBillingDate() == yesterday) {

                    processBilling(account.getId(), account.getLastBillingDate(), yesterday);

                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in processBilling: ", e);

        }

    }

    private Invoice saveInvoice(Invoice invoice) throws GeneralException {
        try {
            EJBRequest request = new EJBRequest();
            //Dado que se guarda a través del EJB de Facturación desarrollado por José Luis se debe hacer la conversión a sus estructuras
            UserTransaction userTransaction = context.getUserTransaction();
            request.setParam(invoice);
            Invoice invoices = null;
            invoices = userEJB.saveInvoice(request);
            return invoices;
        } catch (NullParameterException ex) {
            java.util.logging.Logger.getLogger(BillingsEJBImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private Account saveAccount(Account account) throws GeneralException {
        try {
            EJBRequest request = new EJBRequest();
            //Dado que se guarda a través del EJB de Facturación desarrollado por José Luis se debe hacer la conversión a sus estructuras
            UserTransaction userTransaction = context.getUserTransaction();
            request.setParam(account);
            Account accounts = null;
            accounts = userEJB.saveAccount(request);
            return accounts;
        } catch (NullParameterException ex) {
            java.util.logging.Logger.getLogger(BillingsEJBImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private Transaction saveTransaction(Transaction transaction) throws GeneralException {
        try {
            EJBRequest request = new EJBRequest();
            //Dado que se guarda a través del EJB de Facturación desarrollado por José Luis se debe hacer la conversión a sus estructuras
            UserTransaction userTransaction = context.getUserTransaction();
            request.setParam(transaction);
            transaction = userEJB.saveTransaction(request);
            return transaction;
        } catch (NullParameterException ex) {
            java.util.logging.Logger.getLogger(BillingsEJBImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public int processCantInvoice(List<Invoice> list) {
        int invoice = 0;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getInvoiceStatus().getName() == null ? "POR PAGAR" == null : list.get(i).getInvoiceStatus().getName().equals("POR PAGAR")) {
                invoice++;
            }
        }
        return invoice;
    }

    public void processUpdateTransacction(List<Transaction> list, Invoice invoice) {
        if (list.size() > 0) {
            for (int k = 0; k < list.size(); k++) {
                try {
                    list.get(k).setInvoice(invoice);
                    saveTransaction(list.get(k));

                } catch (GeneralException ex) {
                    java.util.logging.Logger.getLogger(BillingsEJBImpl.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }

    }

    //METODOS PRIVADOS
    public void processBilling(Long accountId, Timestamp lastBillingDate, Date yesterday) throws GeneralException {
        try {
            

            Account account = utilsEJB.loadAccountbyId(accountId);
            if (account.getId() != null) {
                List<Transaction> transactions = new ArrayList<Transaction>();
                transactions = utilsEJB.loadTransacctionbyAccountId(accountId, lastBillingDate, yesterday);
                Float Tmaunt = utilsEJB.getTotalAmauntbyTransacction(accountId, lastBillingDate, yesterday); /*processTotalAmount(transactions);*/
                Float tTax = utilsEJB.getTotalTaxTransacction(accountId, lastBillingDate, yesterday);
                InvoiceStatus invoiceStatus = ContentManager.getInstance().getInvoiceStatusById(InvoiceStatus.POR_PAGAR);
                Timestamp newYesterday = new Timestamp(yesterday.getTime());
                Invoice invoice = new Invoice();
                invoice.setAccount(account);
                invoice.setBeginningDate(lastBillingDate);
                invoice.setClosingDate(newYesterday);
                invoice.setEmissionDate(newYesterday);
                invoice.setExemptTotal(tTax);
                invoice.setTotal(Tmaunt);
                invoice.setTotalToPay(Tmaunt + tTax);
                invoice.setInvoiceStatus(invoiceStatus);
                invoice.setNumberInvoice(null);
                invoice.setInvoiceStatus(invoiceStatus);
                invoice.setEnterprise(account.getEnterprise());
                Currency currencys = ContentManager.getInstance().getCurrency(account.getEnterprise().getCurrency().getId());
                invoice.setCurrency(currencys);
                invoice.setEmailStatus(0L);
                invoice = saveInvoice(invoice);



                processUpdateTransacction(transactions, invoice);
                //Cambiar a english
                this.analyzeRiskAccounts(account);
                java.sql.Timestamp yester = new Timestamp(yesterday.getTime());
                Calendar cal = addDays(yesterday, account.getPeriod().getDays());
                Date now = new Date(cal.getTimeInMillis());
                Timestamp nexBilling = new Timestamp(now.getTime());
                account.setLastBillingDate(yester);
                account.setNexBillingDate(nexBilling);

                Float balancePrev = account.getBalance();
                Float totalInvoice = invoice.getTotalToPay();

                Float totalNewBalance = balancePrev + (totalInvoice * (-1));
                account.setBalance(totalNewBalance);

                account = saveAccount(account);


            }


        } catch (NullParameterException ex) {
            java.util.logging.Logger.getLogger(BillingsEJBImpl.class.getName()).log(Level.SEVERE, null, ex);
            throw new GeneralException("Exception in method ProcessBilling "+ ex.getMessage(), ex.getStackTrace());

        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(BillingsEJBImpl.class.getName()).log(Level.SEVERE, null, ex);
            throw new GeneralException("Exception in method ProcessBilling "+ ex.getMessage(), ex.getStackTrace());
        }


    }


     public void analyzeActivationAccounts(Account account) throws GeneralException, NullParameterException, EmptyListException{

         List<Invoice> list = new ArrayList();
            Map params = new HashMap();
            params.put(QueryConstants.PARAM_ACCOUNT_ID, account.getId());
            EJBRequest request = new EJBRequest();
            request.setParams(params);
            list = utilsEJB.loadInvoicesbyId(request);

            int outstandingInvoices = processCantInvoice(list);

            if(outstandingInvoices<3){

               
                Timestamp timeServer = new Timestamp(new Date().getTime());
                Date date = new Date(timeServer.getTime());
                Calendar cal = addDays(date, account.getPeriod().getDays());
                Date now = new Date(cal.getTimeInMillis());
                Timestamp nexBilling = new Timestamp(now.getTime());
                account.setLastBillingDate(timeServer);
                account.setNexBillingDate(nexBilling);
                account.setEnabled(true);
                request.setParam(account);
                Account accounts = null;
                accounts = userEJB.saveAccount(request);


                //AccountStatus(account);

            }

     }





    public void analyzeRiskAccounts(Account account) {
        try {

            List<Invoice> list = new ArrayList();
            Map params = new HashMap();
            params.put(QueryConstants.PARAM_ACCOUNT_ID, account.getId());
            EJBRequest request = new EJBRequest();
            request.setParams(params);
            list = utilsEJB.loadInvoicesbyId(request);

            int outstandingInvoices = processCantInvoice(list);

            switch (outstandingInvoices) {
                case 1:
                    try {
                        ArrayList<String> recipents = new ArrayList<String>();
                        recipents.add(account.getEmail());
                        String asunto = "Notificacion de Deuda Acumulada ";
                        String texto = "Estimado usuario pongase en contacto con personal de Alodiga en vista de que posee: 1  factura por cancelar, Factura N°:" + list.get(0) + "";
                        Mail mail = ServiceMails.getNoticeOfSuspensionOfServiceMail(account.getEnterprise(), recipents, asunto, texto);
                        utilsEJB.sendMail(mail);
                        //this.showMessage(Labels.getLabel("sp.common.sent"), false, null);
                    } catch (Exception ex) {
                        ex.printStackTrace();

                        throw new GeneralException("Exception in method analyzeRiskAccounts  line 306"+ ex.getMessage(), ex.getStackTrace());
                       
                    }
                    break;
                case 2:
                    try {
                        ArrayList<String> recipents = new ArrayList<String>();
                        recipents.add(account.getEmail());
                        String asunto = "Notificacion de Deuda Acumulada ";
                        String texto = "Estimado usuario pongase en contacto con personal de Alodiga en vista de que posee: 2  facturas por cancelar, Facturas N°:" + list.get(0) + " y " + list.get(1) + " ";
                        Mail mail = ServiceMails.getNoticeOfSuspensionOfServiceMail(account.getEnterprise(), recipents, asunto, texto);
                        utilsEJB.sendMail(mail);
                        
                    } catch (Exception ex) {
                     ex.printStackTrace();
                        throw new GeneralException("Exception in method analyzeRiskAccounts  line 322"+ ex.getMessage(), ex.getStackTrace());

                    }
                    break;
                case 3:
                    account.setEnabled(Boolean.FALSE);
                    AccountStatus(account);
                    try {
                        ArrayList<String> recipents = new ArrayList<String>();
                        recipents.add(account.getEmail());
                        String asunto = "Notificacion de suspension del servicio ";
                        String texto = "Estimado usuario pongase en contacto con personal de Alodiga en vista que esta suspendido por acumular el maximo numero de: 3  facturas por cancelar, Facturas N°:" + list.get(0) + " , " + list.get(1) + " y " + list.get(2) + " ";
                        Mail mail = ServiceMails.getNoticeOfSuspensionOfServiceMail(account.getEnterprise(), recipents, asunto, texto);
                        utilsEJB.sendMail(mail);
                        
                    } catch (Exception ex) {
                      ex.printStackTrace();
                      new GeneralException("Exception in method analyzeRiskAccounts  line 339"+ ex.getMessage(), ex.getStackTrace());

                    }
                    break;
                default:
                    break;
            }
           } catch (NullParameterException ex) {
                ex.printStackTrace();
              System.out.println("NullParameterException en el analyzeRiskAccounts");

            } catch (GeneralException ex) {
                ex.printStackTrace();
                System.out.println(" GeneralException en el analyzeRiskAccounts");


	   } catch (EmptyListException ex) {
                ex.printStackTrace();
                System.out.println("EmptyListException en el analyzeRiskAccounts");

            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println("Exception en el metodo analyzeRiskAccounts ");

            }


    }

    private void AccountStatus(Account account) throws GeneralException {
        
        UserTransaction userTransaction = context.getUserTransaction();
        try {
            userTransaction.begin();

            if (account.getId() == null) {
                entityManager.persist(account);
            } else {
                entityManager.merge(account);
            }

            userTransaction.commit();
        } catch (Exception e) {
            logger.error("Exception in method saveInvoice: ", e);
            EjbUtils.rollback(userTransaction, logger);
            throw new GeneralException("Exception in method saveInvoice: " + e.getMessage(), e.getStackTrace());
        }


    }

    public int loadDateCalculatorCicles(Account account, Timestamp time1) {
        Timestamp time2 = account.getLastBillingDate();

            long  dif =  (time1.getTime() - time2.getTime());
            long  difDayss = (dif / (24 * 60 * 60 * 1000));
            long  cicles = difDayss / account.getPeriod().getDays();
        if (cicles < 0) {
            cicles = cicles * (-1);
        }
           int cicle = (int) cicles;
        return cicle;

    }

    public void BillingPro(Account account) throws GeneralException {
        Long id = account.getId();
        Timestamp lastBillingDate = account.getLastBillingDate();
        Timestamp nexBillingDate = account.getNexBillingDate();
        java.sql.Date dateNexBillingDate = new java.sql.Date(nexBillingDate.getTime());
        processBilling(id, lastBillingDate, dateNexBillingDate);


    }

    public void filterData(Account account, Timestamp timeServer) {

        if (account.getNexBillingDate().getTime() == timeServer.getTime()) {

            System.out.println("no ha llegado  a la fecha de proceso de corte ");

        } else if (account.getNexBillingDate().getTime() < timeServer.getTime()) {
            try {

                BillingPro(account);

            } catch (GeneralException ex) {
                ex.printStackTrace();
                System.out.println("GeneralException en el analyzeRiskAccounts");
                java.util.logging.Logger.getLogger(BillingsEJBImpl.class.getName()).log(Level.SEVERE, null, ex);
            }


        } else if (account.getNexBillingDate().getTime() > timeServer.getTime()) {
            System.out.println("Se paso la fecha  a la fecha de corte ");
        }

    }

    public void forceBilling(String login) throws GeneralException, EmptyListException {

        List<Account> account = null;
        Account account2 = null;
        Long id = 1L;
        
            if (!login.equals("")) {
                Timestamp timeServer = new Timestamp(new Date().getTime());
                Date date = new Date(timeServer.getTime());
                try {
                    account = userEJB.searchAccounts(id, login, null, null,QueryConstants.STATUS_ACCOUNT_ENABLED);
                } catch (EmptyListException ex) {
                    ex.printStackTrace();
                    java.util.logging.Logger.getLogger(BillingsEJBImpl.class.getName()).log(Level.SEVERE, null, ex);
                } catch (NullParameterException ex) {
                    ex.printStackTrace();
                    java.util.logging.Logger.getLogger(BillingsEJBImpl.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (!account.get(0).getIsPrePaid()) {
                    int cicles = loadDateCalculatorCicles(account.get(0), timeServer);
                    if (cicles != 0) {
                        for (int i = 0; i < cicles; i++) {
                            if (i > 0) {
                            try {
                                account = userEJB.searchAccounts(id, login, null, null, QueryConstants.STATUS_ACCOUNT_ENABLED);
                            } catch (NullParameterException ex) {
                                java.util.logging.Logger.getLogger(BillingsEJBImpl.class.getName()).log(Level.SEVERE, null, ex);
                            }
                               
                            }

                            List<Transaction> transactions = new ArrayList<Transaction>();
                            transactions = utilsEJB.loadTransacctionbyAccountId(account.get(0).getId(), account.get(0).getLastBillingDate(), date);
                            if (transactions.size() > 0) {
                                System.out.println("paso por aquiii");
                                filterData(account.get(0), timeServer);
                            }
                        }
                    } else {
                        System.out.println("no tiene ciclo de facturacion por correr");
                    }
                } else {
                    System.out.println(" Esta cuenta es Pre-Pago");
                }

            } else {
                System.out.println("todos ");
                Timestamp timeServer = new Timestamp(new Date().getTime());
                java.sql.Date date = new java.sql.Date(timeServer.getTime());
                try {
                    account = userEJB.searchAccounts(id, null, null, null,QueryConstants.STATUS_ACCOUNT_ENABLED);
                } catch (EmptyListException ex) {
                    ex.printStackTrace();                 
                } catch (NullParameterException ex) {
                    ex.printStackTrace();                 
                }
                for (int i = 0; i < account.size(); i++) {
                    if (!account.get(i).getIsPrePaid()) {
                        int cicles = loadDateCalculatorCicles(account.get(i), timeServer);
                        if (cicles != 0) {
                            for (int j = 0; j < 3; j++) {

                                if (j > 0) {
                                    try {
                                        account2 = userEJB.loadAccountByLogin(account.get(i).getLogin());
                                    } catch (RegisterNotFoundException ex) {
                                        ex.printStackTrace();
                                        java.util.logging.Logger.getLogger(BillingsEJBImpl.class.getName()).log(Level.SEVERE, null, ex);
                                    } catch (NullParameterException ex) {
                                        ex.printStackTrace();
                                        java.util.logging.Logger.getLogger(BillingsEJBImpl.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }

                                List<Transaction> transactions = new ArrayList<Transaction>();
                                transactions = utilsEJB.loadTransacctionbyAccountId(account.get(i).getId(), account.get(i).getLastBillingDate(), date);
                                if (transactions.size() > 0) {
                                    System.out.println("paso por acaaaa");
                                    if (j > 0) {
                                        filterData(account2, timeServer);
                                    } else {
                                        filterData(account.get(i), timeServer);
                                    }
                                }
                            }
                        } else {
                            System.out.println("no tiene ciclo de facturacion por correr");
                        }
                    }
                }
            }
    }

    public void executeBilling() throws GeneralException {
        System.out.println("executeBilling  STARTED..");
        logger.info("[PROCESS] executeBilling");
        String login = "";
        try {
            forceBilling(login);
        } catch (EmptyListException ex) {
            java.util.logging.Logger.getLogger(BillingsEJBImpl.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("executeBilling DONE..");
    }
}
