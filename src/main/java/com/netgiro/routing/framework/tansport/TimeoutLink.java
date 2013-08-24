package com.netgiro.routing.framework.tansport;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netgiro.routing.framework.exception.SystemDownException;
import com.netgiro.utils.application.AppInfo;

/**
 * This class wrapps a single datalink (DataPipe) with timeout 
 * fuctionality. 
 * 
 * You get an instance by calling "getMyCurrentLink" from Router, then call "timeoutCall"
 *
 */
public class TimeoutLink {

    DataPipe iPipe;
    AtomicBoolean isOk = new AtomicBoolean(false);
    Object iRouterMutex;
    private final int iPriority; 
    private int iPingTimeoutCont; //TODO
    private int iLocalSwitchAfterNbrTimeouts; //TODO
    private int iFailedPings;

    private static Logger cLogger = LoggerFactory.getLogger(TimeoutLink.class);

    //TODO this should not really be visible...
    public TimeoutLink( DataPipe aPipe, Object mutex, int prioity ) {
        iPipe = aPipe;
        iRouterMutex = mutex;
        iPriority = prioity;
    }

    /**
     * Sorry for the looong method. Needs to be broken apart
     * 
     * @param aDataPayload the data to send
     * @param aMsTimeout the max service time
     * @return a response (an xml perhaps? DataPipe decides, 
     * you created me you know what i return)
     * @throws TimeoutException we ran out of time...
     * @throws Exception forwarded from DataPipe
     */
    public Object makeTimeoutCall(Object aDataPayload, long aMsTimeout) throws TimeoutException, Exception {
        DataSenderThread task = new DataSenderThread(aDataPayload);
        long startTime = System.currentTimeMillis();
        Thread t = new Thread(task);
        t.start();
        try {
            synchronized (aDataPayload) {
                aDataPayload.wait(aMsTimeout);
            }
            //back from wait... how did we do?
            long timeSpent = System.currentTimeMillis()-startTime;
            cLogger.debug("payload notified! back from wait "+t+" finished in: "+timeSpent+" timeout: "+aMsTimeout );
            if (task.response != null && task.problem == null ) { //OK!
                cLogger.debug("tx "+aDataPayload+" ok! back from wait "+t+" finished in: "+timeSpent+" timeout: "+aMsTimeout );
                iPipe.setPipeOk(true);
                return task.response;
            }
            if ( task.problem == null ) { // timeout, let the caller worry about it
                cLogger.warn("tx TIMEOUT! "+t+" did not finish on time: "+timeSpent+" timeout: "+aMsTimeout );
                throw new TimeoutException("Time's up. Spent: "+timeSpent
                        +"[ms]  timeout was: "+aMsTimeout);
            } 
            cLogger.error("tx ERROR! "+t+" threw exception: "+task.problem+" spent "+timeSpent+" ms timeout was: "+aMsTimeout );
            iPipe.setPipeOk(false);
            synchronized (iRouterMutex) {
                iRouterMutex.notify();
            }
            throw task.problem;
        } catch ( InterruptedException e ) {
            cLogger.error("TimeoutLink was interrupted during call!",e);
            return task.response; //general panic...
        }
    }

    public String getPipeInfo() {
        return iPipe.toString();
    }

    public int getPriority() {
        return iPriority;
    }

    class DataSenderThread implements Runnable {

        final Object iPayload;
        Exception problem = null;
        Object response = null;

        DataSenderThread(Object aPayload) {
            iPayload = aPayload;
        }

        public void run() {
            try {
                response = iPipe.send( iPayload );
                if ( !iPipe.isPipeOk() ) // call done. is it ok?
                    problem = new SystemDownException("Afo response signal: system down!");
                synchronized ( iPayload ) {
                    cLogger.debug("send done notifying: "+iPayload);
                    iPayload.notify();
                }
            } catch (Exception e) {
                problem = e;
                synchronized ( iPayload ) {
                    iPayload.notify();
                }
            }
        }
    }

    public boolean isOk() {
        return iPipe.isPipeOk();
    }
    public int getNbrFailedPing() {
        return iFailedPings;
    }

    public void ping( AppInfo aAppInfo, long aMsTimeout ) {
        try {
            makeTimeoutCall(iPipe.createPingMsg( aAppInfo, aMsTimeout ),
                    aMsTimeout);
            iFailedPings = 0;
        } catch ( TimeoutException e ) {
            iFailedPings++;
        } catch (Exception e) {
            //problems have already been reported to link... Just log
            cLogger.warn("Ping call experienced problems: ",e);
        }
    }

    @Override
    public String toString() {
        return "Pipe: "+iPipe.toString();
    }
}
