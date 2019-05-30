package com.alodiga.services.provider.ejb;

import com.alodiga.services.provider.commons.ejbs.BillPaymentEJBLocal;
import com.alodiga.services.provider.commons.ejbs.BillPaymentTimerEJB;
import com.alodiga.services.provider.commons.ejbs.BillPaymentTimerEJBLocal;
import com.alodiga.services.provider.commons.genericEJB.AbstractSPEJB;
import com.alodiga.services.provider.commons.genericEJB.SPContextInterceptor;
import com.alodiga.services.provider.commons.genericEJB.SPLoggerInterceptor;
import com.alodiga.services.provider.commons.utils.EjbConstants;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.interceptor.Interceptors;
import org.apache.log4j.Logger;

@Interceptors({SPLoggerInterceptor.class, SPContextInterceptor.class})
@Stateless(name = EjbConstants.BILL_PAYMENT_UPDATE_TIMER_EJB, mappedName = EjbConstants.BILL_PAYMENT_UPDATE_TIMER_EJB)
@TransactionManagement(TransactionManagementType.BEAN)
public class BillPaymentTimerEJBImp extends AbstractSPEJB implements BillPaymentTimerEJB, BillPaymentTimerEJBLocal {

    private static final Logger logger = Logger.getLogger(BillPaymentTimerEJBImp.class);
    @EJB
    private BillPaymentEJBLocal billPaymentEJB;
    @Resource
    private SessionContext ctx;
    Calendar initialExpiration;
    private Long timeoutInterval=0L;

    private void cancelTimers() {
        try {
            if (ctx.getTimerService() != null) {
                Collection<Timer> timers = ctx.getTimerService().getTimers();
                if (timers != null) {
                    for (Timer timer : timers) {
                        timer.cancel();
                    }
                }
            }
        } catch (Exception e) {
            //
        }
    }

    private void createTimer() {
        ctx.getTimerService().createTimer(initialExpiration.getTime(), timeoutInterval, EjbConstants.COMMISSION_TIMER_EJB);
    }

    @Timeout
    public void execute(Timer timer) {
        try {
            logger.info("[BillPaymentTimerEJB] Ejecutando");
            //System.out.println("[BillPaymentTimerEJB] Ejecutando");
            executeClosure();
            stop();
            start();
        } catch (Exception e) {
            logger.error("Error", e);
        }
    }

    private void executeClosure() throws Exception {
        try {
            try {
                billPaymentEJB.executePPNBillPaymentUpdate();
            } catch (Exception e) {
            }
            
            logger.info("[BillPaymentTimerEJB] Ejecutado cambio de comisiones");
            //System.out.println("[BillPaymentTimerEJB] Ejecutado cambio de comisiones");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void forceExecution() throws Exception {
        logger.info("Ejecutó forceExecution!!!!!!!!");
        //System.out.println("Ejecutó forceExecution!!!!!!!!");
    }

    public void forceTimeout() throws Exception {
        logger.info("[BillPaymentTimerEJB] Forzando timeout para dentro de 1 minuto");
        //System.out.println("[BillPaymentTimerEJB] Forzando timeout para dentro de 1 minuto");
        cancelTimers();
        setTimeoutInterval();
        initialExpiration = Calendar.getInstance();
        initialExpiration.add(Calendar.MINUTE, 2);
        createTimer();
    }

    public Date getNextExecutionDate() {
        if (ctx.getTimerService() != null) {
            Collection<Timer> timers = ctx.getTimerService().getTimers();
            if (timers != null) {
                for (Timer timer : timers) {
                    return timer.getNextTimeout();
                }
            }
        }

        return null;
    }

    public void restart() throws Exception {
        stop();
        start();
        logger.info("[BillPaymentTimerEJB] Reiniciado");
        //System.out.println("[BillPaymentTimerEJB] Reiniciado");
    }


    private void setTimeoutInterval() throws Exception {
        initialExpiration = Calendar.getInstance();
        initialExpiration.set(Calendar.HOUR, 4);//Media entre zona horaria de California Y Florida - EN CA 12 am en FL seria las 4 am.
        initialExpiration.set(Calendar.MINUTE, 20);
        initialExpiration.set(Calendar.SECOND, 0);
        initialExpiration.set(Calendar.MILLISECOND, 0);
        initialExpiration.set(Calendar.AM_PM, Calendar.AM);
        Long secondsInDay = 86400L;
        initialExpiration.add(Calendar.DAY_OF_MONTH, 1);
        timeoutInterval = secondsInDay * 1000L;
    }

    @SuppressWarnings("unchecked")
    public void start() throws Exception {
        setTimeoutInterval();
        createTimer();
        logger.info("[BillPaymentTimerEJB] Iniciado");
        //System.out.println("BillPaymentTimerEJB] Iniciado");
    }

    @SuppressWarnings("unchecked")
    public void stop() throws Exception {
        cancelTimers();
        logger.info("[BillPaymentTimerEJB] Detenido");
        //System.out.println("[BillPaymentTimerEJB] Detenido");
    }

    public Long getTimeoutInterval() {
        return timeoutInterval;
    }
}
