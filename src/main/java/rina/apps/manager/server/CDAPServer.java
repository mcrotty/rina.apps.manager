/**
 * 
 */
package rina.apps.manager.server;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rinad.mad_manager.messages.MAIPCP.ipcp_config;

import com.google.protobuf.InvalidProtocolBufferException;

import eu.irati.librina.ApplicationProcessNamingInformation;
import eu.irati.librina.FlowDeallocatedEvent;
import eu.irati.librina.FlowInformation;
import eu.irati.librina.FlowRequestEvent;
import eu.irati.librina.IPCManagerSingleton;
import eu.irati.librina.filt_info_t;
import eu.irati.librina.flags_t;
import eu.irati.librina.flags_t.Flags;
import eu.irati.librina.obj_info_t;
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
	
	// Length to wait before initiating CDAP messages
	private int wait = 0;
		
	/**
	 * @param difName
	 * @param serverAni
	 */
	public CDAPServer(String difName,
			ApplicationProcessNamingInformation serverAni,
			int waitTime) {
		super(difName, serverAni);
		wait=waitTime;
	}

	@Override
	protected void newFlowRequest(FlowRequestEvent fre) {
		
		// First accept the flow request
		IPCManagerSingleton ipc = rina.getIpcManager();
		FlowInformation info = ipc.allocateFlowResponse(fre, 0, true);
		
		// Create a task to process incoming messages
		CDAPConnection connection = new CDAPConnection(info);
		
		ApplicationProcessNamingInformation remote = info.getRemoteAppName();
		
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

	@Override
	protected void tick() {
		
		// Have we a connection
		if (!connections.isEmpty()) {
			wait = wait-(TICK_TIME*1000);
			if (wait < 1) {
				// Start doing it.
				Iterator<CDAPConnection> i = connections.values().iterator();
				CDAPConnection c = i.next();
				
				// It is safe to sequence these as CDAPconnection has a thread pool
				
				cacep(c);
				
				createIPCP(c);
				
			}
		} // otherwise ignore		
	}
	
	
	private void cacep(CDAPConnection c) {
		// Nothing to do here, as the CDAPconnection loop processes incoming messages
	}
	
	private void createIPCP(CDAPConnection c) {
        byte[] sdu = new byte[2000];
        
       
//      mad_manager::structures::ipcp_config_t ipc_config;
//      ipc_config.process_instance = "1";
//      ipc_config.process_name = "normal-1.IPCP";
//      ipc_config.process_type = "normal-ipc";
//      ipc_config.dif_to_assign = "normal.DIF";
        ipcp_config ipc_config = ipcp_config.newBuilder()
        		.setProcessInstance("1")
        		.setProcessName("normal-1.IPCP")
        		.setProcessType("normal-ipc")
        		.setDifToAssign("normal.DIF")
        		.setDifToRegister("400")
        		.build();
        
        //
//        cdap_rib::obj_info_t obj;
//        obj.name_ =
//                        "root, computingSystemID = 1, processingSystemID=1, kernelApplicationProcess, osApplicationProcess, ipcProcesses, ipcProcessID=2";
//        obj.class_ = "IPCProcess";
//        obj.inst_ = 0;
        obj_info_t obj = new obj_info_t();
        obj.setName_("root, computingSystemID = 1, processingSystemID=1, kernelApplicationProcess, osApplicationProcess, ipcProcesses, ipcProcessID=2");
        obj.setInst_(0);
        
//        mad_manager::encoders::IPCPConfigEncoder().encode(ipc_config,
//                                                          obj.value_);
        byte[] sdu1 = ipc_config.toByteArray();
        
//      mad_manager::structures::ipcp_config_t object;
//      mad_manager::encoders::IPCPConfigEncoder().decode(obj.value_, object);
        try {
			ipcp_config ipc_conf2 = ipcp_config.parseFrom(sdu1);

			//
//	        cdap_rib::flags_t flags;
//	        flags.flags_ = cdap_rib::flags_t::NONE_FLAGS;
//
//	        cdap_rib::filt_info_t filt;
//	        filt.filter_ = 0;
//	        filt.scope_ = 0;
//
			flags_t flags = new flags_t();
			flags.setFlags_(Flags.NONE_FLAGS);
			
			filt_info_t filt = new filt_info_t();
			filt.setFilter_("");  // TODO: Check this now.
			filt.setScope_(0);
			
			
//	        cdap_prov_->remote_create(flow.portId, obj, flags, filt);
			
			//c.remote_create_request(c, obj, filt, message_id);
	        info("create IPC request CDAP message sent");
			
		} catch (InvalidProtocolBufferException e) {
			err("Decoding own message");
			e.printStackTrace();
		}
        
        
	}
	
	// 
	private void createIPCP2(CDAPConnection c) {
	       ipcp_config ipc_config = ipcp_config.newBuilder()
	    		   .setProcessInstance("1")
	    		   .setProcessName("")
	    		   .setProcessType("normal-ipc")
	    		   .setDifToAssign("normal.DIF")
	    		   .setDifToRegister("410") // TODO: Fix me
	    		   .build();
	       // Missing enr calls
		   //.setDifToAssign(EnrConf)
	       
	    	obj_info_t obj = new obj_info_t();
	    	obj.setName_("");
	    	obj.setClass_("IPCProcess");
	    	obj.setInst_(0);
	    	
	    	// Encode
	        byte[] sdu = ipc_config.toByteArray();
	    		   
	        try {
	        	// Test decode
				ipcp_config ipc_conf2 = ipcp_config.parseFrom(sdu);

				// Flags and filter
				flags_t flags = new flags_t();
				flags.setFlags_(Flags.NONE_FLAGS);
				
				filt_info_t filt = new filt_info_t();
				filt.setFilter_("");  // TODO: Check this now.
				filt.setScope_(0);
				
				// TODO:
//		        cdap_prov_->remote_create(flow.portId, obj, flags, filt);		
				//c.remote_create_request(c, obj, filt, message_id);
		        info("create IPC request CDAP message sent");
				
			} catch (InvalidProtocolBufferException e) {
				err("Decoding own message");
				e.printStackTrace();
			}
	    		   
		

	}
}
