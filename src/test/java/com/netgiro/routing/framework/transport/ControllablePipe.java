package com.netgiro.routing.framework.transport;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netgiro.routing.framework.tansport.DataPipe;
import com.netgiro.utils.application.AppInfo;

public class ControllablePipe implements DataPipe {

    private static final Logger cLogger = LoggerFactory.getLogger(ControllablePipe.class);

    int iFastServTime;
    int iSlowServTime;
    int iFailAfterNbrMsgs;
    int iSentMsg = 0;
    int iAfoDownAfterNbrMsgs;
    AtomicBoolean isOk = new AtomicBoolean(true);
    String iName;
    int iPrio;

    public ControllablePipe( int fastServTime, int slowServTime, int failEveryNbrMsgs,
            int afoDownEveryNbrMsgs, boolean aIsOk, String name, int prio ) {
        iFastServTime = fastServTime;
        iSlowServTime = slowServTime;
        iFailAfterNbrMsgs = failEveryNbrMsgs;
        iAfoDownAfterNbrMsgs = afoDownEveryNbrMsgs;
        isOk.set(aIsOk);
        iName = name;
        iPrio = prio;
    }

    public Object send(Object aPayload) {
        Random rand = new Random();
        try {
            Thread.sleep( iFastServTime + rand.nextInt(iSlowServTime-iFastServTime));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        iSentMsg ++;
        cLogger.debug("sending msg : "+aPayload+" to "+this.toString());
        if ( iFailAfterNbrMsgs > 0 && iSentMsg >= iFailAfterNbrMsgs ) {
            throw new RuntimeException("im failing every: "+iFailAfterNbrMsgs);
        }
        if ( iAfoDownAfterNbrMsgs > 0 && iSentMsg >= iAfoDownAfterNbrMsgs ) {
            isOk.set(false);
        }
        return aPayload;
    }

    public boolean isPipeOk() {
        return isOk.get();
    }

    public Object createPingMsg(AppInfo aAppInfo, long aTimeOut) {
        return "ping";
    }

    public void setPipeOk(boolean aIsOk) {
        isOk.set(aIsOk);
    }

    @Override
    public String toString() {
        return iName;
    }

    @Override
    public int getNbrFailedPing() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getPriority() {
        return iPrio;
    }

    @Override
    public String getPipeInfo() {
        return toString();
    }

    @Override
    public boolean ping(long pingTimeOut) {
        // TODO Auto-generated method stub
        return false;
    }
}
