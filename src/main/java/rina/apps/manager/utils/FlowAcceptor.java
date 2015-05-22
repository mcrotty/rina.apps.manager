package rina.apps.manager.utils;

import eu.irati.librina.FlowRequestEvent;

public interface FlowAcceptor {
	public void dispatchFlowRequestEvent(FlowRequestEvent event);
}
