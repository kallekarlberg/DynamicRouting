package com.netgiro.routing.framework.tansport;

import java.net.URL;

import com.netgiro.paymenteventservice.v1.card.PaymentEventService;
import com.netgiro.paymenteventservice.v1.card.pts.ServiceRequest;
import com.netgiro.paymenteventservice.v1.card.pts.ServiceResponse;
import com.netgiro.paymenteventservice.v1.internal.InternalRequestHeader;
import com.netgiro.paymenteventservice.v1.internal.InternalRequestPayload;
import com.netgiro.utils.application.AppInfo;

public class CardPesPipe extends PesPipe<PaymentEventService, ServiceRequest, ServiceResponse> {

    private static final String NS_URI = "";

    public CardPesPipe( URL aWsdlUrl, long connectTimeOut, long receiveTimeOut, int prio) {
        super(aWsdlUrl, PaymentEventService.class, NS_URI, connectTimeOut, receiveTimeOut, prio);
    }

    @Override
    protected ServiceResponse sendImpl(ServiceRequest aPayload, PaymentEventService client) {
        ServiceResponse resp = client.process( aPayload );
        isOk.set( seemsOk(resp) );
        return resp;
    }

    @Override
    ServiceRequest createPingMsgImpl( AppInfo aAppInfo, long timeout ) {
        ServiceRequest req = new ServiceRequest();
        InternalRequestHeader intHead = createInternalHeader( System.currentTimeMillis(),
                System.currentTimeMillis() + timeout );
        InternalRequestPayload intPay = createInternalPayload( aAppInfo );
        req.setInternalRequestHeader( intHead );
        req.setInternalRequestPayload( intPay );
        return req;
    }

    @Override
    synchronized boolean seemsOkImpl(ServiceResponse aResp) {
        if ( aResp != null && aResp.getInternalResponseHeader() != null
                && aResp.getInternalResponseHeader().getAfoInfo() != null )
            return aResp.getInternalResponseHeader().getAfoInfo().isIsOk() ==
            null ? false : aResp.getInternalResponseHeader().getAfoInfo().isIsOk();
        return false;
    }

    @Override
    AppInfo getAppInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPipeInfo() {
        return "PTS@"+iUrl;
    }
}
