/**
 * 
 */
package rina.utils.apps.echo.client;

import eu.irati.librina.AllocateFlowRequestResultEvent;
import eu.irati.librina.ApplicationProcessNamingInformation;
import eu.irati.librina.ApplicationRegistrationInformation;
import eu.irati.librina.ApplicationRegistrationType;
import eu.irati.librina.CDAPCallbackInterface;
import eu.irati.librina.CDAPProviderInterface;
import eu.irati.librina.DeallocateFlowResponseEvent;
import eu.irati.librina.Flow;
import eu.irati.librina.FlowSpecification;
import eu.irati.librina.IPCEvent;
import eu.irati.librina.IPCEventProducerSingleton;
import eu.irati.librina.IPCEventType;
import eu.irati.librina.IPCManagerSingleton;
import eu.irati.librina.RegisterApplicationResponseEvent;
import eu.irati.librina.SerializedObject;
import eu.irati.librina.auth_info_t;
import eu.irati.librina.auth_info_t.AuthTypes;
import eu.irati.librina.con_handle_t;
import eu.irati.librina.dest_info_t;
import eu.irati.librina.filt_info_t;
import eu.irati.librina.flags_t;
import eu.irati.librina.flags_t.Flags;
import eu.irati.librina.obj_info_t;
import eu.irati.librina.res_info_t;
import eu.irati.librina.rina;
import eu.irati.librina.src_info_t;
import eu.irati.librina.vers_info_t;

/**
 * @author mcrotty@tssg.org
 *
 */
public class CDAPEchoClient extends CDAPCallbackInterface {
	
	// Our registration information
	private String dif_name;
	//private String app_name;
	//private String app_instance;
	private ApplicationProcessNamingInformation localAni = null;
	// Should we register this app
	private boolean app_register; // true means we register ourselves

	
	// Server registration info
	//private String server_name;
	//private String server_instance;
	private ApplicationProcessNamingInformation remoteAni = null;
	
	// Traffic generation types.
	private boolean quiet;
	private long times; // -1 is infinite
	private int wait;
	private int gap;
	private int dealloc_wait;
	
	// Flow to server
	// This is only valid after call to createFlow
	private Flow flow = null;
	
	// CDAPProvider
	// This is only valid after call to cacep
	private CDAPProviderInterface provider = null;
	private con_handle_t cdapconnection = null;
	
	/**
	 * Constructor, based on the parameters given.
	 * 
	 * @param dif_name
	 * @param apn
	 * @param api
	 * @param server_apn
	 * @param server_api
	 * @param q
	 * @param count
	 * @param registration
	 * @param w
	 * @param g
	 * @param dw
	 */
	public CDAPEchoClient(String dif_name, ApplicationProcessNamingInformation clientAni, 
			ApplicationProcessNamingInformation serverAni,
			boolean q, long count, boolean registration, int w, int g, int dw) {
	
		// Record application reg info.
		this.dif_name = dif_name;
		//this.app_name = apn;
		//this.app_instance = api;
		this.localAni = clientAni;
		this.app_register = registration;
		
		// Server info
		//this.server_name=server_apn;
		//this.server_instance=server_api;
		this.remoteAni = serverAni;
	
		// Traffic info
		this.quiet=q;
		this.times=count;
		this.wait = w;
		this.gap = g;
		this.dealloc_wait = dw;
		
		// Housekeeping
		//ipcEventConsumer = new IPCEventConsumer();
		//ipcEventConsumer.addApplicationRegistrationListener(this, clientApNamingInfo);
		//executorService = Executors.newCachedThreadPool();
		//executorService.execute(ipcEventConsumer);

	}

	/*
	 * Perform processing
	 */
	public void execute() {
		
		applicationRegister();
		
		createFlow();
		if (flow != null) {
			// Create CDAP connection
			cacep();
			
			// Main loop sending CDAP reads
			mainLoopSendReads();
			
			// Release the connection
			release();			
		}
	}
	
	/**
	 * Register the application with the RINA stack
	 */
	protected void applicationRegister() {
		
		// Check to see if an application registration should be carried out
		if (!app_register) {
			return; // bail out
		}
		
		ApplicationRegistrationInformation ari = new ApplicationRegistrationInformation();
		
		ari.setIpcProcessId(0); // This is an application not an IPC process
		ari.setAppName(localAni); // Use the local app name and instance
		
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
	
	/**
	 * Create a flow to the server
	 */
	protected void createFlow() {
		
		FlowSpecification qosspec = new FlowSpecification();
		long seqnum = 0;
		
		if (gap >= 0) {
			qosspec.setMaxAllowableGap(gap);
		}
		// Get the ipc manager instance
		IPCManagerSingleton ipcManager = rina.getIpcManager();
		
		//ApplicationProcessNamingInformation localAppName = new ApplicationProcessNamingInformation(app_name, app_instance);
		//ApplicationProcessNamingInformation remoteAppName = new ApplicationProcessNamingInformation(server_name, server_instance);
		// Have we been told to connect to a specific DIF
		if ((dif_name != null) && (!dif_name.isEmpty())) {
			ApplicationProcessNamingInformation localDifName = new ApplicationProcessNamingInformation(dif_name, "");
			seqnum=ipcManager.requestFlowAllocationInDIF(localAni, remoteAni, localDifName, qosspec);
		} else {
			seqnum=ipcManager.requestFlowAllocation(localAni, remoteAni, qosspec);
		}
		
		// TODO: This code needs to move to the event processing thread
		
		// Spin waiting for a result
		IPCEventProducerSingleton ipcEventProducer = rina.getIpcEventProducer();
		IPCEvent event;
		for (;;) {
			event = ipcEventProducer.eventWait();
			
			// Wait for the reply event
			if ((event.getEventType() == IPCEventType.ALLOCATE_FLOW_REQUEST_RESULT_EVENT)
				&& (event.getSequenceNumber() == seqnum) ) {
				break;	
			}
			System.out.println("Logging: Got an event of type " + event.getEventType());
		}
		
		// Second half handler, should only be called based on seqnum match.
		createFlowResponse(event);
	}
	
	/**
	 * 
	 */
	protected void createFlowResponse(IPCEvent event) {
		IPCManagerSingleton ipcManager = rina.getIpcManager();
		AllocateFlowRequestResultEvent afrrevent = (AllocateFlowRequestResultEvent) event;
		
		Flow f = ipcManager.commitPendingFlow(afrrevent.getSequenceNumber(), afrrevent.getPortId(), afrrevent.getDifName());
		
		// Set the global flow parameter
		if ((f != null) && (f.getPortId() != -1)) {
			System.out.println("Port id = " + f.getPortId());
			flow = f;
		} else {
			System.err.println("Failed to allocate a flow.");
			flow = null;
		}		
	}
	
	/**
	 * Destroy the flow
	 */
	protected void destroyFlow() {
		
		
		// Get access to the IPC Manager to request flow deallocation
		int port_id = flow.getPortId();

		// TODO: no equivalent method on CDAP
		//provider.destroy(port_id);
		long seqnum = rina.getIpcManager().requestFlowDeallocation(port_id);		
		
		// For the moment we follow the blocking style of the example,
		//
		// TODO: Fix this into the main event loop. See createFlow.
		IPCEventProducerSingleton producer =  rina.getIpcEventProducer();
		IPCEvent event = null;
		while (true) {
			event = producer.eventWait();
			if ((event != null ) 
			  && (event.getEventType() == IPCEventType.DEALLOCATE_FLOW_RESPONSE_EVENT) 
			  && (event.getSequenceNumber() == seqnum)) {
			  	// We got the event we are after
			  	break;
			}
		}
		// End bit to fix, the code below should be in an event handler.
		
		destroyFlowResponse(event);
	}
	
	
	/**
	 * Destroy flow response.
	 */
	protected void destroyFlowResponse(IPCEvent event) {
		DeallocateFlowResponseEvent dfrevent = (DeallocateFlowResponseEvent) event;
		rina.getIpcManager().flowDeallocationResult(dfrevent.getPortId(), dfrevent.getResult() == 0);
		
		// TODO: Equivalent ?
		//provider.finit()
	}

	/**
	 * Initiate the cacep phase
	 */
	protected void cacep() {
		
		vers_info_t ver = new vers_info_t();
		ver.setVersion_(1);
		
		src_info_t src = new src_info_t();
		src.setAp_name_(flow.getLocalApplicationName().getProcessName());
		src.setAp_inst_(flow.getLocalApplicationName().getProcessInstance());
		src.setAe_name_(flow.getLocalApplicationName().getEntityName());
		src.setAe_inst_(flow.getLocalApplicationName().getEntityInstance());

		// TODO:  Fix naming typo in getRemoteApplicationName 
		dest_info_t dest = new dest_info_t();
		dest.setAp_name_(flow.getRemoteApplcationName().getProcessName());
		dest.setAp_inst_(flow.getRemoteApplcationName().getProcessInstance());
		dest.setAe_name_(flow.getRemoteApplcationName().getEntityName());
		dest.setAe_inst_(flow.getRemoteApplcationName().getEntityInstance());
		
		auth_info_t auth = new auth_info_t();
		auth.setAuth_mech_(AuthTypes.AUTH_NONE);
		
		// Create new CDAP provider
		provider = rina.create(false, this);
		
		System.out.println("CDAP: Open connection request message sent");
		con_handle_t con = provider.open_connection(ver, src, dest, auth, flow.getPortId());
		
		byte[] sdu = new byte[2000]; 
		// TODO: This request blocks waiting on a response. Needs non-blocking call
		int bytesRead = flow.readSDU(sdu, 2000);
		
		// TODO: Blocking Problem.
		// There is no way to convert the byte array to a string type
		SerializedObject message = new SerializedObject(); // Use default ctor for now.

		// TODO: blocking problem
		// C++ code calls to cp.process_message() we have no such call
		// 
		//
		// MAJOR BLOCKER - How to change the above problems without modifying SWIG?
	}

	
	public void mainLoopSendReads()
	{
		IPCEventProducerSingleton producer = rina.getIpcEventProducer();
		// Send the number of CDAP Reads required
		for(int count = 0;count < times;) {
			IPCEvent event = producer.eventPoll();
			
			if (event != null) {
				switch (event.getEventType()) {
				case FLOW_DEALLOCATED_EVENT:
					System.out.println("Client recevied a flow deallocated event");
					destroyFlow();
					break;
				default:
					System.out.println("Client recevied an event " + event.getEventType() );
					break;
				}
			} else {
				// Send more CDAP reads
				obj_info_t obj = new obj_info_t();
				obj.setName_("test name");
				obj.setClass_("test class");
				obj.setInst_(1);
				
				flags_t flags = new flags_t();
				flags.setFlags_(Flags.NONE_FLAGS);
				filt_info_t filter = new filt_info_t();
				filter.setFilter_("");
				filter.setScope_(0);
				
				// Send the read
				provider.remote_read(cdapconnection, obj, flags, filter);
				System.out.println("CDAP Read request sent");
				byte[] sdu = new byte[2000]; 
				// TODO: This request blocks waiting on a response. Needs non-blocking call
				int bytesRead = flow.readSDU(sdu, 2000);
				
				// TODO: Blocking Problem.
				// There is no way to convert the byte array to a string type
				SerializedObject message = new SerializedObject(); // Use default ctor for now.

				// TODO: blocking problem
				// C++ code calls to cp.process_message() we have no such call
				// 
				//
				// MAJOR BLOCKER - How to change the above problems without modifying SWIG?
				
				// Move up the counter
				count++;
			}
		}
		
	}
	
	/**
	 * Release the CDAP connection
	 */
	public void release() {
		
		// See if we need to clean up
		if (cdapconnection != null) {
			System.out.println("Client sending close_connection");
			provider.close_connection(cdapconnection);
					
			System.out.println("Client expecting a close_connection response");
			byte[] sdu = new byte[2000]; 
			// TODO: This request blocks waiting on a response. Needs non-blocking call
			int bytesRead = flow.readSDU(sdu, 2000);
			
			// TODO: Blocking Problem.
			// There is no way to convert the byte array to a string type
			SerializedObject message = new SerializedObject(); // Use default ctor for now.

			// TODO: blocking problem
			// C++ code calls to cp.process_message() we have no such call
			// 
			//
			// MAJOR BLOCKER - How to change the above problems without modifying SWIG?
			
			cdapconnection = null;
			System.out.println("Client: close_connection completed.");				
		}
	}
	
	/**
	 * Handlers
	 */
	
	// Handle the open connection result.
	public void open_connection_result(con_handle_t con, res_info_t res) {
		System.out.println("CDAP: open_connection response received");
	}

	// Handle the close connection result.
	public void close_connection_result(con_handle_t con, res_info_t res) {
		System.out.println("CDAP: close_connection response received");
		//cons = null;
	}

	// Handle the READ responses
	public void remote_read_result(con_handle_t con, res_info_t res) {
		System.out.println("CDAP: read response received");
		// TODO: Further analysis on the res
	}
	
}
