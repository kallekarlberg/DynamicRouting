package com.netgiro.routing.framework.tansport;

public interface SubSystem extends Comparable<SubSystem>{

	Object send(Object data, long timeout);
	int getOrder();
}
