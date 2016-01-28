/**
 * Micheal Crotty, Pristine.
 * Copyright (c) 2015, Waterford Institute of Technology.
 */
package rina.apps.manager.server;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import eu.irati.librina.ApplicationProcessNamingInformation;
import eu.irati.librina.FlowDeallocatedEvent;
import eu.irati.librina.FlowInformation;
import eu.irati.librina.FlowRequestEvent;
import eu.irati.librina.rina;

/**
 * This is an extension of the basic server to support CDAP.
 * 
 * This relies on creating a thread pool to process the incoming messages on
 * each connection. Or its a thread per CDAP connection.
 * 
 * @author mcrotty@tssg.org
 */
public class CDAPServer extends BasicServer implements ConnectionFinder {

	// Pool of threads to handle the connections.
	private static ExecutorService executorService = Executors
			.newCachedThreadPool();

	// Pool of concurrent CDAP connections.
	private HashMap<Integer, CDAPConnection> connections = new HashMap<Integer, CDAPConnection>();
	private HashMap<String, CDAPConnection> connections_index = new HashMap<String, CDAPConnection>();

	// The web server
	private CDAPConnection ws = null;

	// Length to wait before initiating CDAP messages (in secs)
	private int wait = 20;

	/**
	 * @param difName
	 * @param serverAni
	 */
	public CDAPServer(String difName,
			ApplicationProcessNamingInformation serverAni, int waitTime,
			String wsConnection, String loggingLevel) {
		super(difName, serverAni, loggingLevel);
		wait = waitTime;
		if (wsConnection != null) {
			URI uri;
			try {
				uri = new URI(wsConnection);
				ws = new DMSConnection(uri, this);
				info("Spawning ws");

				// Leave if do its work
				executorService.execute((Runnable) ws);
			} catch (URISyntaxException e) {
				warn("Invalid URI: " + e.getLocalizedMessage());
			}
		} else {
			warn("No ws");
		}

	}

	@Override
	protected void newFlowRequest(FlowRequestEvent fre) {

		// First accept the flow request
		// IPCManager ipc = rina.getIpcManager();
		FlowInformation info = rina.getIpcManager().allocateFlowResponse(fre,
				0, true);

		// Create a task to process incoming messages
		CDAPConnection connection = new CDAPConnectionJava(info, this);

		// Register it
		CDAPConnection c = connections.put(new Integer(info.getPortId()),
				connection);
		if (c != null) {
			err("Duplicate Flow [port_id = " + info.getPortId()
					+ "], leaving existing connection intact.");
			// Repair, old connection info
			connections.put(new Integer(info.getPortId()), c);
		} else {
			connections_index.put(connection.getId(), connection);

			info("(there are now " + connections.size()
					+ " connections active)");

			// Leave if do its work
			executorService.execute((Runnable) connection);
		}
	}

	@Override
	protected void destroyFlow(FlowDeallocatedEvent fde) {

		// Look up the flow port id to find the correct CDAPConnectionNative.
		Integer i = new Integer(fde.getPortId());
		CDAPConnection c = connections.remove(i);
		if (c != null) {
			connections_index.remove(c.getId());

			info("Stopping processing on Flow [port_id = " + fde.getPortId()
					+ "]");
			c.cancel();
		} else {
			warn("Ignoring flow deallocation event, as no matching flow could be found.");
		}
		info("(there are " + connections.size() + " connections active)");
	}

	/*
	 * Manage the connections
	 */

	/**
	 * Find a given connection given the remote AP name and instance
	 */
	@Override
	public CDAPConnection find(String name, String instance) {
		return connections_index.get(new String(name + "-" + instance));
	}

	@Override
	public CDAPConnection find(Integer localPort) {
		return connections.get(localPort);
	}

	@Override
	public CDAPConnection findManager() {
		return ws;
	}

	/**
	 * And hook to check status
	 */

	@Override
	protected void tick() {

		// Have we a connection
		if (!connections.isEmpty()) {
			wait = wait - (TICK_TIME);
			if ((wait < 1) && (wait > -1)) {
				info("Attempting a CDAP operation");
				// Start doing it.
				Iterator<CDAPConnection> i = connections.values().iterator();
				CDAPConnection c = i.next();
				if (c != null) {
					// It is safe to sequence these as CDAPconnection has a
					// thread
					// pool
					cacep(c);

					CDAPConnectionJava c2 = (CDAPConnectionJava) c;
					c2.createIPCP2(18000);

					// c2.create_read(18001);
				}

			} else if (wait > 0) {
				info("Wating " + wait + " still.");
			}
		} else {
			info("No connection.");
		} // otherwise ignore
	}

	private void cacep(CDAPConnection c) {
		// Nothing to do here, as the CDAPconnection's loop processes incoming
		// messages

	}

}
