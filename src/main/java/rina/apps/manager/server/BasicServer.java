/**
 * 
 */
package rina.apps.manager.server;

import eu.irati.librina.ApplicationProcessNamingInformation;
import eu.irati.librina.ApplicationRegistrationInformation;
import eu.irati.librina.ApplicationRegistrationType;
import eu.irati.librina.DeallocateFlowResponseEvent;
import eu.irati.librina.FlowDeallocatedEvent;
import eu.irati.librina.FlowRequestEvent;
import eu.irati.librina.IPCEvent;
import eu.irati.librina.IPCEventProducerSingleton;
import eu.irati.librina.RegisterApplicationResponseEvent;
import eu.irati.librina.UnregisterApplicationResponseEvent;
import eu.irati.librina.rina;


/**
 * A Basic RINA flow accepting server.
 * 
 * @author mcrotty@tssg.org
 */
public class BasicServer {

	// DIF to register at
	private String dif_name = null;
	private ApplicationProcessNamingInformation local_ani = null;

	// Flag to indicate orderly shutdown
	private boolean keep_running = true;

	// Sanity checks
	private boolean registered = false;
	
	/**
	 * Create a new Flow server
	 */
	public BasicServer(String difName,
			ApplicationProcessNamingInformation serverAni) {
		this.dif_name = difName;
		this.local_ani = serverAni;
	}

	// Do what needs to be done.
	public void execute() {

		rina.initialize("INFO", "");
		
		applicationRegister();

		// Main event loop
		IPCEventProducerSingleton producer = rina.getIpcEventProducer();
		IPCEvent event = null;
		while (keep_running) {
			// Block for at most a second.
			event = producer.eventTimedWait(1, 0);
			if (event != null) {
				processEvent(event);
			}

			// Process non RINA events.
			// TODO: Add more here
		}
		
		// TODO: Fix application unregistering.
	}

	/**
	 * Internal method to sort the IPC event.
	 * 
	 * @param event
	 */
	protected void processEvent(IPCEvent event) {
		// Depending of the event
		switch (event.getEventType()) {

		case REGISTER_APPLICATION_RESPONSE_EVENT:
			if (!registered) {
				RegisterApplicationResponseEvent resp = (RegisterApplicationResponseEvent)event;
				// Update IPC manager of our intentions state
				if (resp.getResult() == 0) {
					rina.getIpcManager().commitPendingRegistration(resp.getSequenceNumber(), resp.getDIFName());
					info("Application registered.");
				} else {
					info("CDAP register [status=" + resp.getResult() + "]");
					rina.getIpcManager().withdrawPendingRegistration(resp.getSequenceNumber());
					fail("Failed to register application [" + resp.getApplicationName() + "] at DIF [" + resp.getDIFName() + "]");
				}
			} else {
				warn("Application is already registered. Ignoring");				
			}
			break;
		
		case UNREGISTER_APPLICATION_RESPONSE_EVENT:
			if (registered) {
				UnregisterApplicationResponseEvent resp = (UnregisterApplicationResponseEvent)event;
				// Update IPC manager of our intentions state
				rina.getIpcManager().appUnregistrationResult(resp.getSequenceNumber(), (resp.getResult() ==0));
				info("Application unregistered.");
			} else {
				warn("Application is not registered yet. Ignoring");				
			}
			break;

		// Response to flow deallocation sent
		case DEALLOCATE_FLOW_RESPONSE_EVENT:			
			DeallocateFlowResponseEvent dfr = (DeallocateFlowResponseEvent)event;
			// Update IPC manager of our intentions
			rina.getIpcManager().flowDeallocationResult(dfr.getPortId(), (dfr.getResult()==0) );
			break;

		// MA has requested a flow initiation	
		case FLOW_ALLOCATION_REQUESTED_EVENT:
			//Flow
			FlowRequestEvent fre = (FlowRequestEvent) event;
	        info("New flow allocated [port-id = " + fre.getPortId() + "]");
	        // Delegate to correct method
	        newFlowRequest(fre);
			break;
		
		// Connection was terminated	
		case FLOW_DEALLOCATED_EVENT:
			FlowDeallocatedEvent fde = (FlowDeallocatedEvent)event;
			// Update IPC manager of our intentions
			rina.getIpcManager().flowDeallocated(fde.getPortId());
	        info("Flow torn down remotely [port-id = " + fde.getPortId() + "]");
	        // Delegate to correct method
	        destroyFlow(fde);
			break;
			
		//  Other events
		default:
			warn("Ignoring unknown event : " + IPCEvent.eventTypeToString(event.getEventType()));
			break;
			
		} // switch

	}



	/*
	 * Register the application with the data supplied.
	 */
	protected void applicationRegister() {
		ApplicationRegistrationInformation ari = new ApplicationRegistrationInformation();

		ari.setIpcProcessId(0); // This is an application not an IPC process
		ari.setAppName(local_ani); // Use the local app name and instance

		// Was there a dif name given ?
		if ((dif_name == null) || ("".equals(dif_name))) {
			ari.setApplicationRegistrationType(ApplicationRegistrationType.APPLICATION_REGISTRATION_ANY_DIF);
		} else {
			ari.setApplicationRegistrationType(ApplicationRegistrationType.APPLICATION_REGISTRATION_SINGLE_DIF);
			ari.setDifName(new ApplicationProcessNamingInformation(dif_name, ""));
		}

		info("Registering:" + ari.toString());
		// Get access to the IPC Manager
		rina.getIpcManager().requestApplicationRegistration(ari);

		// Bottom half initialisation is done via the event loop.
	}

	/*
	 * Methods to overload.
	 */

	protected void newFlowRequest(FlowRequestEvent e) {
		// TODO: Add specific mechanisms here.
	}

	protected void destroyFlow(FlowDeallocatedEvent fde) {
		// TODO Add specific mechanisms 	
	}


	/*
	 * Handy logging functions
	 */
	
	protected void warn(String string) {
		System.err.println("WARNING: " + string);		
	}

	protected void err(String string) {
		System.err.println("ERROR: " + string);				
	}

	protected void info(String string) {
		System.out.println("INFO:" + string);
	}
	
	private void fail(String string) {
		System.err.println("FATAL: " + string);		
		System.exit(1);
	}

}
