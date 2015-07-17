/**
 * Micheal Crotty, Pristine.
 * Copyright (c) 2015, Waterford Institute of Technology.
 */
package rina.apps.manager.server;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import eu.irati.librina.ApplicationProcessNamingInformation;
import eu.irati.librina.FlowDeallocatedEvent;
import eu.irati.librina.FlowRequestEvent;

/**
 * Just another example of a server.
 * 
 * @author mcrotty
 *
 */
public class CDAPEchoServer extends BasicServer {

	// Pool of threads to handle the connections.
	private static ExecutorService executorService = Executors.newCachedThreadPool();
	
	
	/**
	 * @param difName
	 * @param serverAni
	 */
	public CDAPEchoServer(String difName,
			ApplicationProcessNamingInformation serverAni) {
		super(difName, serverAni, "INFO");
		
		warn("Not implemented fully");
		// TODO 
	}


	@Override
	protected void newFlowRequest(FlowRequestEvent e) {
		// TODO Auto-generated method stub
		super.newFlowRequest(e);
		
		
	}


	@Override
	protected void destroyFlow(FlowDeallocatedEvent fde) {
		// TODO Auto-generated method stub
		super.destroyFlow(fde);
	}	

}
