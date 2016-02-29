/**
 * (C) Copyright 2015, Waterford Institute of Technology, PRISTINE.
 */
package rina.apps.manager.server;

import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import rina.messages.CDAP.CDAPMessage;
import rina.messages.CDAP.CDAPMessage.Builder;
import eu.ict_pristine.wp5.dms.es.taxonomy.ESEvent_Header;
import eu.ict_pristine.wp5.dms.es.taxonomy.ES_Event;
import eu.ict_pristine.wp5.dms.es.taxonomy.ES_EventBuilder;
import eu.ict_pristine.wp5.dms.es.taxonomy.TAX_Factory;
import eu.ict_pristine.wp5.dms.es.taxonomy.filters.ES_EventFilter;
import eu.ict_pristine.wp5.dms.es.taxonomy.filters.ES_EventFilterBuilder;
import eu.ict_pristine.wp5.dms.esdsls.cdap.v100.CDAP_Dialects;
import eu.ict_pristine.wp5.dms.esdsls.cdap.v100.CDAP_Lang_1_0_0_Identity;
import eu.ict_pristine.wp5.dms.esdsls.cdap.v100.CDAP_Sources;
import eu.ict_pristine.wp5.dms.esdsls.cdap.v100.CDAP_Types;
import eu.ict_pristine.wp5.dms.esdsls.cdap.v100.CDAP_ValueKeys;

/**
 * Implementation of a websocket client for connecting to the DMS.
 * 
 * @author mcrotty@tssg.org
 *
 */
public class DMSConnection extends WebSocketClient implements CDAPConnection {

	// A way to find the corresponding CDAP Connections
	private ConnectionFinder finder = null;

	// Filter for events
	protected final static ES_EventFilter ANY_CDAP_ACTION_FROM_MANGER = new ES_EventFilterBuilder()
			.withStandardOptions()
			.withKeyMembers("cmd-cdap-actions", "any CDAP command",
					"filter for CDAP commands from the manager").ofType(CDAP_Types.ES_CDAP)
			.inLanguage(CDAP_Lang_1_0_0_Identity.IDENTITY)
			.fromSource(CDAP_Sources.DMS_MANAGER).getMapFilter();

	/*
	 * Constructor
	 */
	public DMSConnection(URI serverURI, ConnectionFinder f) {
		super(serverURI);
		finder = f;
	}

	/*
	 * CDAP connection interface
	 */
	
	/**
	 * Unique identifier
	 */
	@Override
	public String getId() {
		return uri.toString();
	}


	/**
	 * Cancel this connection
	 */
	@Override
	public void cancel() {
		try {
			super.closeBlocking();
		} catch (InterruptedException e) {
			// Nothing to do we wanted to close the connection anyway
		}
	}

	/**
	 * Convert the message to JSON and dispatch to manager.
	 */
	@Override
	public void wrapped_send(Builder message) {
		
		if (message == null) {
			err("JSON encoding of CDAP Message failed as message is empty");
			return;
		}
		
		// Add required DMS ES header
		ESEvent_Header header = TAX_Factory.get.newHeader(CDAP_Types.ES_CDAP,CDAP_Sources.DMS_AGENT,CDAP_Lang_1_0_0_Identity.IDENTITY, CDAP_Dialects.CDAP_RES);			
		
		// Convert to JSON
		String json = CDAPSerialiser.encodeJSONMessage(message.build(), header);
		if (json != null) {
			// Pass on to manager
			logShortMessage(message);
			info(" JSON=" + json);
			super.send(json);
		} else {
			err("JSON encoding of CDAP Message failed");
		}
		
	}

	
	/*
	 * Websocket client interface
	 */

	/**
	 *  Called when a connection to the manager is established. 
	 */
	@Override
	public void onOpen(ServerHandshake handshakedata) {
		info("WS connection opened");
	}

	/**
	 * Primary method to accept JSON events from the manager
	 */
	@Override
	public void onMessage(String json) {

		// / First filter the messages.
		if (ANY_CDAP_ACTION_FROM_MANGER.match(json)) {
			// Got something that is destined for us
			ES_Event peIn = new ES_EventBuilder().setTransformer(
					new CDAPKeyTransformer()).event(json);
			if (peIn != null) {
				// Skb_Console.conInfo("Passed filter");
				String name = peIn.getString(CDAP_ValueKeys.destApName);
				String instance = peIn.getString(CDAP_ValueKeys.destApInst);

				CDAPConnection c = finder.find(name, instance);
				if (c != null) {
					// This is the part that decodes the JSON encodes a CDAP GPB
					CDAPMessage.Builder message = CDAPSerialiser
							.decodeJSONMessage(json);
					if (message != null) {
						info("From manager JSON=" + json);
						// Send message to MA
						c.wrapped_send(message);
					} else {
						warn("Event failed JSON conversion: " + json);
					}
				} else {
					warn("Missing MA for " + name + "-" + instance);
				}
			} else {
				warn("Invalid event: " + json);
			}
		}

	}


	/**
	 * Called when the connection is closed.
	 */
	@Override
	public void onClose(int code, String reason, boolean remote) {
		info("WS connection closed");
	}

	/**
	 * Called when an error occurs
	 */
	@Override
	public void onError(Exception ex) {
		err("Exception: " + ex.getLocalizedMessage());
	}

	
	/*
	 * Handy logging functions
	 */
	/*
	 * Log a short but informative message
	 */
	private void logShortMessage(Builder message) {
		StringBuilder b = new StringBuilder();
		b.append("CDAP ");
		b.append(message.getOpCode());
		switch(message.getOpCode()) {
		// operations on an object
		case M_CREATE:
		case M_DELETE:
		case M_READ:
		case M_WRITE:
		case M_CANCELREAD:
		case M_START:
		case M_STOP:
			b.append(" of ");
			b.append(message.getObjClass());
			// missing break is intentional
		case M_CONNECT:
		case M_RELEASE:
			b.append(". ");
			break;
		// Responses on an object
		case M_CREATE_R:
		case M_DELETE_R:
		case M_READ_R:
		case M_CANCELREAD_R:
		case M_WRITE_R:
		case M_START_R:
		case M_STOP_R:
			b.append(" of ");
			b.append(message.getObjClass());
			// missing break is intentional
		case M_CONNECT_R:
		case M_RELEASE_R:
			if (message.getResult() != 0) {
				b.append(" FAILED ");
				if (message.hasResultReason()) {
					b.append("(");
					b.append(message.getResultReason());
					b.append(") ");
				}
			} else {
				b.append(" SUCCESSFUL ");
			}
			break;
		}
		b.append("Sending to Manager.");
		info(b.toString());
	}
	

	protected void warn(String string) {
		System.err.println("WARNING: " + string);
	}

	protected void err(String string) {
		System.err.println("ERROR: " + string);
	}

	protected void info(String string) {
		System.out.println("INFO:" + string);
	}

	protected void fail(String string) {
		System.err.println("FATAL: " + string);
		System.exit(1);
	}

}
