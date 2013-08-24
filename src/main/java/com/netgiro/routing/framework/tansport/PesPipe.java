/* -----------------------------------------------------------------------------
 * Created on 12 nov 2010 by kkarlberg
 * Copyright NetGiro Systems AB
 * Version: $Id: $
 * --------------------------------------------------------------------------- */
package com.netgiro.routing.framework.tansport;

import java.net.URL;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netgiro.paymenteventservice.v1.internal.AfoPingRequest;
import com.netgiro.paymenteventservice.v1.internal.AfoRequest;
import com.netgiro.paymenteventservice.v1.internal.ApplicationInfo;
import com.netgiro.paymenteventservice.v1.internal.InternalRequestHeader;
import com.netgiro.paymenteventservice.v1.internal.InternalRequestPayload;
import com.netgiro.utils.application.AppInfo;
import com.netgiro.utils.exception.NgInternalErrorException;

public abstract class PesPipe<PES_IFC, REQUEST, RESPONSE> implements DataPipe {

    protected static final Logger cLogger = LoggerFactory.getLogger(CardPesPipe.class);

    private static final com.netgiro.paymenteventservice.v1.internal.ObjectFactory INTERNAL_OBJ_FACTORY =
            new com.netgiro.paymenteventservice.v1.internal.ObjectFactory();

    protected final AtomicBoolean isOk = new AtomicBoolean(true);
    protected static final long LOG_TRACE_ID = System.currentTimeMillis(); //hack

    protected PES_IFC iPesWsClient;
    protected final URL iUrl;

    private final int iPriority;
    private int iNbrFailedPings = 0;

    public PesPipe( URL aWsdlUrl, Class<PES_IFC> ifcClass, String nsUri, long connectTimeOut,
            long receiveTimeOut, int prio ) {
        iUrl = aWsdlUrl;
        iPriority = prio;
        final String endpointAddress = aWsdlUrl.toExternalForm();

        cLogger.info("Creating Service using end point address: '"
                + endpointAddress + "', with NS: " + nsUri + "...");
        final QName serviceName = new QName(nsUri, "PesService");
        Service service = Service.create(serviceName);

        final QName portName = new QName(nsUri, "PesServicePort");
        service.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING,
                endpointAddress);

        cLogger.debug("Getting port handle from service...");
        iPesWsClient = service.getPort(portName, ifcClass);

        Client cl = ClientProxy.getClient(iPesWsClient);
        HTTPConduit http = (HTTPConduit) cl.getConduit();
        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        httpClientPolicy.setConnectionTimeout(connectTimeOut);
        cLogger.debug("Setting connect/receive timeout to {}/{} ms...", connectTimeOut, receiveTimeOut);
        httpClientPolicy.setReceiveTimeout(receiveTimeOut);
        http.setClient(httpClientPolicy);
    }

    abstract RESPONSE sendImpl(REQUEST req, PES_IFC client);
    abstract REQUEST createPingMsgImpl( AppInfo aAppInfo, long timeout );
    abstract boolean seemsOkImpl( RESPONSE resp );
    abstract AppInfo getAppInfo();

    @SuppressWarnings("unchecked")
    boolean seemsOk(Object o) {
        return seemsOkImpl((RESPONSE) o);
    }

    public Object createPingMsg( AppInfo aAppInfo, long timeout ) {
        return createPingMsgImpl(aAppInfo, timeout);
    }

    @SuppressWarnings("unchecked")
    public Object send( Object aPayload ) {
        return sendImpl((REQUEST)aPayload, iPesWsClient );
    }

    public boolean ping( long pingTimeOut ) {
        Object pingMsg = createPingMsg(getAppInfo(), pingTimeOut );
        LinkPingerThread task = new LinkPingerThread( pingMsg, this );
        long startTime = System.currentTimeMillis();
        Thread t = new Thread(task);
        t.start();
        try {
            synchronized ( pingMsg ) {
                pingMsg.wait( pingTimeOut );
            }
            //back from wait... how did we do?
            cLogger.debug("Pinger notified! back from wait "+t+" finished in: "+(System.currentTimeMillis()-startTime)+" timeout: "+pingTimeOut );
            boolean ok = seemsOk(task.response);
            if (!ok)
                iNbrFailedPings++;
            else
                iNbrFailedPings = 0;
            return ok;
        } catch ( InterruptedException e ) {
            cLogger.error("TimeoutLink was interrupted during call!",e);
            iNbrFailedPings++;
            return false; //general panic...
        }
    }

    public boolean isPipeOk() {
        return isOk.get();
    }

    public void setPipeOk(boolean aIsOk) {
        isOk.set(aIsOk);
    }

    @Override
    public int getNbrFailedPing() {
        return iNbrFailedPings ;
    }

    @Override
    public int getPriority() {
        return iPriority;
    }

    protected static XMLGregorianCalendar getXmlTime(long ts) {
        try {
            GregorianCalendar calendar = new GregorianCalendar(Locale.ROOT);
            calendar.setTimeInMillis(ts);
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
        }
        catch (DatatypeConfigurationException e) {
            throw new NgInternalErrorException(e);
        }
    }

    protected static InternalRequestPayload createInternalPayload( AppInfo aAppInfo ) {
        InternalRequestPayload pay = INTERNAL_OBJ_FACTORY.createInternalRequestPayload();
        AfoRequest afoReq = INTERNAL_OBJ_FACTORY.createAfoRequest();
        AfoPingRequest ping = INTERNAL_OBJ_FACTORY.createAfoPingRequest();
        ApplicationInfo appInfo = INTERNAL_OBJ_FACTORY.createApplicationInfo();
        appInfo.setApplicationId( aAppInfo.getApplicationId() );
        appInfo.setApplicationType( aAppInfo.getApplicationTypeId() );
        appInfo.setApplicationTypeName( appInfo.getApplicationTypeName() );
        appInfo.setDescription( appInfo.getDescription() );
        appInfo.setTag( appInfo.getTag() );
        ping.setApplicationInfo(appInfo );
        afoReq.setAfoPingRequest( ping );
        pay.setAfoRequest(afoReq );
        return pay;
    }
    protected static InternalRequestHeader createInternalHeader(long startTime, long deadline ) {
        InternalRequestHeader head = INTERNAL_OBJ_FACTORY.createInternalRequestHeader();
        head.setDeadline( getXmlTime(deadline) );
        head.setStartTime( getXmlTime(startTime) );
        head.setSystemLogTraceId( LOG_TRACE_ID );
        return head;
    }
}
