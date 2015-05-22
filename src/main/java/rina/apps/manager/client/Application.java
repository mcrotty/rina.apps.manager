/**
 * 
 */
package rina.apps.manager.client;

import eu.irati.librina.ApplicationProcessNamingInformation;
import eu.irati.librina.ApplicationRegistrationException;
import eu.irati.librina.ApplicationRegistrationInformation;
import eu.irati.librina.ApplicationRegistrationType;
import eu.irati.librina.CDAPCallbackInterface;
import eu.irati.librina.IPCEvent;
import eu.irati.librina.IPCEventProducer;
import eu.irati.librina.IPCEventProducerSingleton;
import eu.irati.librina.IPCEventType;
import eu.irati.librina.IPCManager;
import eu.irati.librina.IPCManagerSingleton;
import eu.irati.librina.RegisterApplicationResponseEvent;
import eu.irati.librina.rina;

/**
 * Reworking of Application class.
 * 
 * @author mcrotty
 *
 */
public class Application extends CDAPCallbackInterface {

	// Registration information
	private String dif_name;
	private String app_name;
	private String app_instance;
	
	// max buffer size
	protected int max_buffer_size = (1 << 18);
	
	/**
	 * Create a new application
	 */
	public Application(String difName, String appName, String appInstance) {
		// Todo add some checking of parameters
		dif_name=difName;
		app_name=appName;
		app_instance= appInstance;
	}
	
	/**
	 * Register the application with the RINA stack
	 */
	protected void applicationRegister() {
		ApplicationRegistrationInformation ari = new ApplicationRegistrationInformation();
		
		ari.setIpcProcessId(0); // This is an application not an IPC process
		ari.setAppName(new ApplicationProcessNamingInformation(app_name, app_instance));
		
		// Was there a dif name given ?
		if ((dif_name == null) || ("".equals(dif_name))) {
			ari.setApplicationRegistrationType(ApplicationRegistrationType.APPLICATION_REGISTRATION_ANY_DIF);
		} else {
			ari.setApplicationRegistrationType(ApplicationRegistrationType.APPLICATION_REGISTRATION_SINGLE_DIF);
			ari.setDifName(new ApplicationProcessNamingInformation(dif_name, ""));
		}
		
		// Get access to the IPC Manager
		long seqnum = rina.getIpcManager().requestApplicationRegistration(ari);
		
		
		// For the moment we follow the blocking style of the example,
		//
		// TODO: Fix this into the main event loop.
		IPCEventProducerSingleton producer =  rina.getIpcEventProducer();
		IPCEvent event = null;
		while (true) {
			event = producer.eventWait();
			if ((event != null ) 
			  && (event.getEventType() == IPCEventType.REGISTER_APPLICATION_RESPONSE_EVENT) 
			  && (event.getSequenceNumber() == seqnum)) {
			  	// We got the event we are after
			  	break;
			}
		}
		// End bit to fix, the code below should be in an event handler.
		
		RegisterApplicationResponseEvent resp = (RegisterApplicationResponseEvent)event;
		// Update librina state
		if (resp.getResult() == 0) {
			rina.getIpcManager().commitPendingRegistration(seqnum, resp.getDIFName());
		} else {
			rina.getIpcManager().withdrawPendingRegistration(seqnum);
			System.out.println("Failed to register application at DIF:" + resp.getDIFName());
			System.exit(1);
			// FIXME Problem with Exceptions are not throwable.
			//throw new ApplicationRegistrationException("Failed to register application at DIF:" + resp.getDIFName()));
		}
	}
	
}
