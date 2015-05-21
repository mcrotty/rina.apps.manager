/**
 * 
 */
package rina.utils.apps.echo.server;

import eu.irati.librina.CDAPCallbackInterface;
import eu.irati.librina.CDAPProviderInterface;
import eu.irati.librina.FlowInformation;
import eu.irati.librina.IPCManagerSingleton;
import eu.irati.librina.con_handle_t;
import eu.irati.librina.flags_t;
import eu.irati.librina.obj_info_t;
import eu.irati.librina.res_info_t;
import eu.irati.librina.rina;
import eu.irati.librina.ser_obj_t;

/**
 * This class captures the responses from the CDAP connection.
 * 
 * @author mcrotty
 *
 */
public class CDAPConnection extends CDAPCallbackInterface implements Runnable {
	
	// For the moment there is only a single provider.
	private CDAPProviderInterface provider = null;
	
	// Corresponding flow local port id
	private int port_id = -1;
	
	// Max buffer size
	private int max_sdu_size_in_bytes = 2000;

	// Allow the loop to be terminated.
	private boolean keep_running = true;
	
	
	/**
	 * Create a CDAP connection from an accepted flow
	 * @param info Flow information
	 */
	public CDAPConnection(FlowInformation info) {
		port_id = info.getPortId();
	}


	/*
	 * Cancel the task, and cleanup
	 */
	public void cancel() {
		keep_running = false;
	}
	
	/**
	 * This function registers this class as a callback for the CDAP provider.
	 * If also processes all messages received on the port through the CDAP provider.
	 */
	@Override
	public void run() {
		
		provider = rina.create(false, this);
		if (provider == null) {
			err("CDAP: No CDAP provider created.");
			return; // Bail out
		} else if (port_id == -1) {
			err("CDAP: No Port Id supplied.");
			return; // Bail out
		}
		
		byte[] sdu = new byte[max_sdu_size_in_bytes];
		IPCManagerSingleton ipc = rina.getIpcManager();
		
		while (keep_running) {
		    int bytesRead = ipc.readSDU(port_id, sdu, max_sdu_size_in_bytes);
		    
			ser_obj_t message = new ser_obj_t();
			message.setMessage_(sdu);
			message.setSize_(bytesRead);
			// Ask CDAP Processor to process the message
			provider.process_message(message, port_id);		    
		}
		
		// TODO: Verify if there is a cleaner way
		// For example, we dont call close_connection?
		rina.destroy(port_id);
		port_id = -1;
		provider = null;
	}
	
	
	@Override
	public void open_connection(con_handle_t con, flags_t flags, int message_id) {
		// TODO Auto-generated method stub
		res_info_t	res = new res_info_t();
		res.setResult_(1);
		res.setResult_reason_("OK");
		
		info("Open connection requested.");
		provider.open_connection_response(con, res, message_id);
		info("CDAP OPEN: Accepting response issued");
	}

	@Override
    public void remote_create_result(con_handle_t con, obj_info_t obj,
			res_info_t res) {
		// Log it
		info("CDAP CREATE result:[ port=" + con.getPort_() + ", result=" + res.getResult_() + "]");
	}

	@Override
	public void remote_read_result(con_handle_t con, obj_info_t obj,
			res_info_t res) {
		// Log it
		info("CDAP READ result:[ port=" + con.getPort_() + ", result=" + res.getResult_() + "]");
		// TODO add decoding
		
	}

	/*
	 * Logging functions
	 */
	
	private void info(String string) {
		System.out.println("INFO:" + string);
	}

	private void err(String string) {
		System.err.println("ERROR:" + string);
	}


	
}
