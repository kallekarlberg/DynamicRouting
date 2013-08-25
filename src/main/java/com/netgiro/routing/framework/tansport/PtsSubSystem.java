package com.netgiro.routing.framework.tansport;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;


public class PtsSubSystem implements SubSystem {

	private final URL iUrl;
	private final String iType;
	private final long iConnectTimeout;
	private final int iOrder;

	public PtsSubSystem(URL url, String type, int order, long connectTimeout) {
		iUrl = url;
		iType = type;
		iOrder = order;
		iConnectTimeout = connectTimeout;
		tryConnect();
	}

	private void tryConnect() {
		final String endpointAddress = iUrl.toExternalForm();

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
		httpClientPolicy.setConnectionTimeout(iConnectTimeout);
		cLogger.debug("Setting connect/receive timeout to {}/{} ms...", connectTimeOut, receiveTimeOut);
		http.setClient(httpClientPolicy);
	}

	public String getInfo() {
		return "PTS@"+iUrl;
	}

	@Override
	public int compareTo(SubSystem o) {
		return this.iOrder - o.getOrder();
	}


	@Override
	public int getOrder() {
		return iOrder;
	}
	@Override
	public Object send(Object data, long timeout) {
		// TODO Auto-generated method stub
		return null;
	}
}
