/**
 * Micheal Crotty, Pristine.
 * Copyright (c) 2015, Waterford Institute of Technology.
 */
package rina.apps.manager.server;

// import java.io.UnsupportedEncodingException;
// 
// import rina.cdap.impl.googleprotobuf.CDAP.CDAPMessage.Builder;
// import eu.irati.librina.ApplicationProcessNamingInformation;
// import eu.irati.librina.CDAPCallbackInterface;
// import eu.irati.librina.CDAPProviderInterface;
// import eu.irati.librina.FlowInformation;
// import eu.irati.librina.IPCManagerSingleton;
// import eu.irati.librina.con_handle_t;
// import eu.irati.librina.flags_t;
// import eu.irati.librina.obj_info_t;
// import eu.irati.librina.res_info_t;
// import eu.irati.librina.rina;
// import eu.irati.librina.ser_obj_t;

/**
 * This class captures the responses from the CDAP connection.
 * 
 * @author mcrotty@tssg.org
 *
 */
// public class CDAPConnectionNative extends CDAPCallbackInterface implements Runnable, CDAPConnection {
	
	// For the moment there is only a single provider.
	// private CDAPProviderInterface provider = null;
	
	// Flow information
	// private FlowInformation info = null;

	// Corresponding flow local port id
	// private int port_id = -1;
	
	// Max buffer size
	// private int max_sdu_size_in_bytes = 4048;

	// Allow the loop to be terminated.
	// private boolean keep_running = true;
	
	// private con_handle_t con_handle = null;
	
	/**
	 * Create a CDAP connection from an accepted flow
	 * @param info Flow information
	 */
	// public CDAPConnectionNative(FlowInformation info) {
	// 	port_id = info.getPortId();
	// 	this.info = info;
	// }

	/*
	 * CDAPConnection interface
	 */

	/**
	 * Cancel the task, and cleanup
	 */
	// @Override
	// public void cancel() {
	// 	keep_running = false;
	// }

	/**
	 * Get an identifier for this connection
	 */
	// @Override
	// public String getId() {
	// 	ApplicationProcessNamingInformation remote = info.getRemoteAppName();
	// 	return (remote.getProcessName() + "-" + remote.getProcessInstance());
	// }

	/**
	 * Send a CDAPMessage encoding it.
	 */
	// @Override
	// public void wrapped_send(Builder message) {
	// 	// Encode the message
	// 	byte[] encMessage = CDAPSerialiser
	// 			.encodeCDAPMessage(message.build());
	// 
	// 	// Send message to MA
	// 	info("Sending to MA:" + getId());
	// 	send(encMessage);	
	// }
	
	/**
	 * Send a CDAP message
	 * @param encMessage   The encoded form of the CDAP message
	 */
	// public void send(byte[] encMessage) {
	// 	
	// 	IPCManagerSingleton ipc = rina.getIpcManager();
	// 	
	// 	try {
	// 		ipc.writeSDU(port_id, encMessage, encMessage.length);
	// 	} catch (Exception e) {
	// 		e.printStackTrace();
	// 	}
	// }

	/*
	 * Runnable interface
	 */
	
	/**
	 * This function registers this class as a callback for the CDAP provider.
	 * If also processes all messages received on the port through the CDAP provider.
	 */
	// @Override
	// public void run() {
	// 	
	// 	rina.init(2000);
	// 	provider = rina.create(false, this);
	// 	if (provider == null) {
	// 		err("CDAP: No CDAP provider created.");
	// 		return; // Bail out
	// 	} else if (port_id == -1) {
	// 		err("CDAP: No Port Id supplied.");
	// 		return; // Bail out
	// 	}
		
	// 	byte[] sdu = new byte[max_sdu_size_in_bytes];
	// 	//ByteBuffer sdu = ByteBuffer.allocate(max_sdu_size_in_bytes);
	// 	
	// 	IPCManagerSingleton ipc = rina.getIpcManager();
	// 	
	// 	while (keep_running) {
	// 	    int bytesRead = ipc.readSDU(port_id, sdu, max_sdu_size_in_bytes);
	// 	    //int bytesRead = ipc.readSDU(port_id, sdu.array(), sdu.limit());
	// 	    //sdu.limit(bytesRead);
	// 	    
	// 	    info("Read " + bytesRead + " bytes into the sdu buffer");
	// 	    
	// 	    
	// 		// Debug, dump the buffer
	// 		int width = 16;
	// 		for (int index = 0; index < bytesRead; index += width) {
	// 			printHex(sdu, index, width);
	// 			printAscii(sdu, index, width);
	// 		}				
	// 		
	// 		ser_obj_t message = new ser_obj_t();
	// 		message.setMessage_(sdu);
	// 		message.setSize_(bytesRead);
	// 			    		    		
	// 		info("Calling process message");
	// 		// Ask CDAP Processor to process the message
	// 		provider.process_message(message, port_id);		    
	// 	}
	// 	
	// 	// TODO: Verify if there is a cleaner way
	// 	// For example, we dont call close_connection?
	// 	rina.destroy(port_id);
	// 	port_id = -1;
	// 	info.delete();
	// 	provider = null;
	// }
	
	
	/**
	 * @return the con_handle
	 */
	// public con_handle_t getCon_handle() {
	// 	return con_handle;
	// }


	//@Override
	// public void open_connection(con_handle_t con, flags_t flags, int message_id) {
	// 	// TODO Auto-generated method stub
	// 	res_info_t	res = new res_info_t();
	// 	res.setResult_(1);
	// 	res.setResult_reason_("OK");
	// 	
	// 	con_handle= con;
	// 	
	// 	info("Open connection requested.");
	// 	provider.open_connection_response(con, res, message_id);
	// 	info("CDAP OPEN: Accepting response issued");
	// }

	// @Override
  //   public void remote_create_result(con_handle_t con, obj_info_t obj,
	// 		res_info_t res) {
	// 	// Log it
	// 	info("CDAP CREATE result:[ port=" + con.getPort_() + ", result=" + res.getResult_() + "]");
	// }

	// @Override
	// public void remote_read_result(con_handle_t con, obj_info_t obj,
	// 		res_info_t res) {
	// 	// Log it
	// 	info("CDAP READ result:[ port=" + con.getPort_() + ", result=" + res.getResult_() + "]");
	// 	// TODO add decoding
	// 	
	// }

	/*
	 * Logging functions
	 */
	
	// private void info(String string) {
	// 	System.out.println("INFO:" + string);
	// }

	// private void err(String string) {
	// 	System.err.println("ERROR:" + string);
	// }


	/**
	 * 
	 * Extra debugging functions
	 * 
	 */
	
	// private static void printHex(byte[] bytes, int offset, int width) {
	// 	for (int index = 0; index < width; index++) {
	// 		if (index + offset < bytes.length) {
	// 			System.out.printf("%02x ", bytes[index + offset]);
	// 		} else {
	// 			System.out.print("	");
	// 		}
	// 	}
	// }

	// private static void printAscii(byte[] bytes, int index, int width) {
	// 	if (index < bytes.length) {
	// 		width = Math.min(width, bytes.length - index);
	// 		try {
	// 			System.out.println(
	// 				":"
	// 					+ new String(bytes, index, width, "UTF-8").replaceAll("\r\n", " ").replaceAll(
	// 						"\n",
	// 						" "));
	// 		} catch (UnsupportedEncodingException e) {
	// 			System.out.println(": <illegal chars>");
	// 		}
	// 	} else {
	// 		System.out.println();
	// 	}
	// }




// }
