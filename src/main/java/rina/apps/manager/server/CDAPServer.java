/**
 * 
 */
package rina.apps.manager.server;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import eu.irati.librina.ApplicationProcessNamingInformation;
import eu.irati.librina.FlowDeallocatedEvent;
import eu.irati.librina.FlowInformation;
import eu.irati.librina.FlowRequestEvent;
import eu.irati.librina.IPCManagerSingleton;
import eu.irati.librina.rina;

/**
 * This is an extension of the basic server to support CDAP.
 * 
 * This relies on creating a thread pool to process the incoming messages on each connection.
 * Or its a thread per CDAP connection.
 * 
 * @author mcrotty
 */
public class CDAPServer extends BasicServer {

	// Pool of threads to handle the connections.
	private static ExecutorService executorService = Executors.newCachedThreadPool();

	// Pool of concurrent CDAP connections.
	private HashMap<Integer,CDAPConnection> connections = new HashMap<Integer,CDAPConnection>();
	
	
	/**
	 * @param difName
	 * @param serverAni
	 */
	public CDAPServer(String difName,
			ApplicationProcessNamingInformation serverAni) {
		super(difName, serverAni);
	}

	@Override
	protected void newFlowRequest(FlowRequestEvent fre) {
		
		// First accept the flow request
		IPCManagerSingleton ipc = rina.getIpcManager();
		FlowInformation info = ipc.allocateFlowResponse(fre, 0, true);
		
		// Create a task to process incoming messages
		CDAPConnection connection = new CDAPConnection(info);
		
		// Register it
		CDAPConnection c = connections.put(new Integer(info.getPortId()),connection);
		if (c != null) {
			err("Duplicate Flow [port_id = " + info.getPortId() + "], leaving existing connection intact.");
			// Repair, old connection info
			connections.put(new Integer(info.getPortId()),c); 
		} else {
			info("(there are now " + connections.size() +  " connections active)");
			
			// Leave if do its work
			executorService.execute(connection);			
		}
	}

	@Override
	protected void destroyFlow(FlowDeallocatedEvent fde) {
		
		// Look up the flow port id to find the correct CDAPConnection.
		Integer i = new Integer(fde.getPortId());
		CDAPConnection c = connections.remove(i);
		if (c != null) {
			info("Stopping processing on Flow [port_id = " + fde.getPortId() + "]");
			c.cancel();
		} else {
			warn("Ignoring flow deallocation event, as no matching flow could be found.");			
		}
		info("(there are " + connections.size() +  " connections active)");
	}
	
}
