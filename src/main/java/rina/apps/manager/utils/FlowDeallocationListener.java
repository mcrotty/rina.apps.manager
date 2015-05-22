package rina.apps.manager.utils;

import eu.irati.librina.FlowDeallocatedEvent;

public interface FlowDeallocationListener {
	public void dispatchFlowDeallocatedEvent(FlowDeallocatedEvent event);
}
