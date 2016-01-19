/**
 * Micheal Crotty, Pristine.
 * Copyright (c) 2015, Waterford Institute of Technology.
 */
package rina.apps.manager.server;

import rina.messages.CDAP.CDAPMessage;

/**
 * An interface abstracting the differences between the native implementation and the pure java one.
 * @author mcrotty@tssg.org
 *
 */
public interface CDAPConnection {
	
	/**
	 * Return the locally unique id for this connection
	 * @return String
	 */
	String getId();
	
	/**
	 * Terminate processing on this thread
	 */
	void cancel();
	
	/**
	 * Send a message on this connection
	 * @param encMessage
	 */
	//void send(byte[] encMessage);

	/**
	 * Send a message on this connection
	 * @param encMessage
	 */
	void wrapped_send(CDAPMessage.Builder message);
	
}
