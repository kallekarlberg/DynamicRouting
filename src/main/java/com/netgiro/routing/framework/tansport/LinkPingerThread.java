package com.netgiro.routing.framework.tansport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LinkPingerThread implements Runnable {

	private static final Logger cLogger = LoggerFactory.getLogger(LinkPingerThread.class);

	final Object iPayload;
	Object response = null;
	private final DataPipe iPesPipe;

	LinkPingerThread( Object pingMsg, DataPipe pesPipe ) {
		iPayload = pingMsg;
		iPesPipe = pesPipe;
	}

	public void run() {
		try {
			response = iPesPipe.send( iPayload );
		} catch (Exception e) {
			response = null;
			cLogger.warn("Failed ping, reason: "+e.getMessage(),e);
		}
		synchronized ( iPayload ) {
			iPayload.notify();
		}
	}
}
