package com.netgiro.routing.framework.tansport;

import com.netgiro.utils.application.AppInfo;

public interface DataPipe {
    Object send( Object aPayload );
    Object createPingMsg( AppInfo aAppInfo, long timeOut );
    boolean isPipeOk();
    void setPipeOk(boolean isOk);
    int getNbrFailedPing();
    int getPriority();
    String getPipeInfo();
    boolean ping(long pingTimeOut);
}
