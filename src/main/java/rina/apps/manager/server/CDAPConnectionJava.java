/**
 * Micheal Crotty, Pristine.
 * Copyright (c) 2015, Waterford Institute of Technology.
 */
package rina.apps.manager.server;

import java.util.ArrayList;

import rina.messages.ApplicationProcessNamingInfoMessage.applicationProcessNamingInfo_t;
import rina.messages.CDAP.CDAPMessage;
import rina.messages.CDAP.CDAPMessage.Builder;
import rina.messages.CDAP.flagValues_t;
import rina.messages.CDAP.objVal_t;
import rina.messages.CDAP.opCode_t;
import rina.messages.CommonMessages.property_t;
import rina.messages.ConnectionPoliciesMessage.dtcpConfig_t;
import rina.messages.ConnectionPoliciesMessage.dtcpFlowControlConfig_t;
import rina.messages.ConnectionPoliciesMessage.dtcpRtxControlConfig_t;
import rina.messages.ConnectionPoliciesMessage.dtcpWindowBasedFlowControlConfig_t;
import rina.messages.ConnectionPoliciesMessage.dtpConfig_t;
import rina.messages.DataTransferConstantsMessage.dataTransferConstants_t;
import rina.messages.MAIPCP.addr_config_t;
import rina.messages.MAIPCP.addre_pref_config_t;
import rina.messages.MAIPCP.dif_config_t;
import rina.messages.MAIPCP.dif_info_t;
import rina.messages.MAIPCP.efcp_config_t;
import rina.messages.MAIPCP.fa_config_t;
import rina.messages.MAIPCP.ipcp_config_t;
import rina.messages.MAIPCP.nsm_config_t;
import rina.messages.MAIPCP.rmt_config_t;
import rina.messages.MAIPCP.security_config_t;
import rina.messages.MAIPCP.static_addr_config_t;
import rina.messages.PolicyDescriptorMessage.policyDescriptor_t;
import rina.messages.QoSCubeMessage.qosCube_t;

import com.google.protobuf.ByteString;

import eu.irati.librina.ApplicationProcessNamingInformation;
import eu.irati.librina.FlowInformation;
import eu.irati.librina.IPCManagerSingleton;
import eu.irati.librina.rina;

/**
 * This class captures the responses from the CDAP connection.
 * 
 * @author mcrotty@tssg.org
 *
 */
public class CDAPConnectionJava implements Runnable, CDAPConnection {

	// Connection finder
	private ConnectionFinder finder = null;
	
	// Flow information
	private FlowInformation info = null;

	// Corresponding flow local port id
	private int port_id = -1;
	
	// Negotiated syntax version
	private int abstract_syntax = 0;
	
	
	// Max buffer size 1K
	private int max_sdu_size_in_bytes = 1024;

	// Message identifiers
	private int message_id = 9000;
	
	// Allow the loop to be terminated.
	private boolean keep_running = true;

	private long version = 0;
	
	//private con_handle_t con_handle = null;
	
	/**
	 * Create a CDAP connection from an accepted flow
	 * @param info Flow information
	 * @param f    The connection finder
	 */
	public CDAPConnectionJava(FlowInformation info, ConnectionFinder f) {
		this.info = info;
		port_id = info.getPortId();
		finder = f;
	}


	/**
	 * Get a locally unique identifier
	 */
	@Override
	public String getId() {
		ApplicationProcessNamingInformation remote = info.getRemoteAppName();
		return (remote.getProcessName() + "-" + remote.getProcessInstance());
	}
	
	/**
	 * Cancel the task, and cleanup
	 */
	public void cancel() {
		keep_running = false;
	}


	/**
	 * Given a CDAP message, fill in the connection related information
	 * Encode it in a buffer and send to the MA
	 */
	@Override
	public void wrapped_send(Builder message) {
		// Encode the missing information.
		this.complete_message_for_ma(message);

		// Encode the message
		byte[] encMessage = CDAPSerialiser
				.encodeCDAPMessage(message.build());

		// Send message to MA
		//info("CDAP " + message.getOpCode() + " of " + message.getObjClass() +": Sending to MA[" + getId() + "] ");
		logShortMessage(message);
		send(encMessage);
	}

		
	/**
	 * This function registers this class as a callback for the CDAP provider.
	 * If also processes all messages received on the port through the CDAP provider.
	 */
	@Override
	public void run() {
		
		//rina.init(2000);
		//provider = null ; //rina.create(false, this);
		
		if ((info == null)||(port_id == -1)) {
			err("CDAP: No Port Id supplied.");
			return; // Bail out
		}
		
		byte[] sdu = new byte[max_sdu_size_in_bytes];
		//ByteBuffer sdu = ByteBuffer.allocate(max_sdu_size_in_bytes);
		
		IPCManagerSingleton ipc = rina.getIpcManager();
		
		while (keep_running) {
		    int bytesRead = ipc.readSDU(port_id, sdu, max_sdu_size_in_bytes);
		    info("Read " + bytesRead + " bytes into the sdu buffer");
		    if (bytesRead > 0) {
				// Debug, dump the buffer
//				int width = 16;
//				for (int index = 0; index < bytesRead; index += width) {
//					printHex(sdu, index, width);
//					printAscii(sdu, index, width);
//				}				
								    		    		
				//info("Calling process message");
				process_message(sdu, bytesRead, port_id);		    		    	
		    }
		    //if ()
		}
		
		// TODO: Verify if there is a cleaner way
		// For example, we dont call close_connection?
		rina.destroy(port_id);
		port_id = -1;
		//provider = null;
	}
	
	
	/**
	 * Process an incoming message from CDAP
	 * @param sdu The SDU
	 * @param sduSize The size
	 * @param port_id The relevant port
	 */
	public void process_message(byte[] sdu, int sduSize, int port_id) {
		
		// Decode the message
		CDAPMessage message = CDAPSerialiser.decodeCDAPMessage(sdu, sduSize);
		
		if (message == null) {
			info("Invalid CDAP message received");
			return;
		}
				
		// We process connects and releases in-line to avoid CDAP provider calls.
		if (message.getOpCode() == opCode_t.M_CONNECT) {
			// Invoke callback
			open_connection(message, port_id);		
		} else if (message.getOpCode() == opCode_t.M_RELEASE) {
			// Invoke callback
			close_connection(message, port_id);
		} else {
			//
			// Standard processing.
			//			
			// Bug fix: workaround
			// Add missing source and destination fields.
			CDAPMessage.Builder builder = CDAPMessage.newBuilder(message);
			this.complete_message_for_manager(builder);
			// End fix

			// Pass on to manager 
			CDAPConnection ws = finder.findManager();
			if (ws != null) {
				ws.wrapped_send(builder);
			} else {
				warn("No manager connection, message discarded.");
			}
		}
		
	}

	
	/**
	 * Send a CDAP message
	 * @param encMessage   The encoded form of the CDAP message
	 */
	public void send(byte[] encMessage) {
		
		IPCManagerSingleton ipc = rina.getIpcManager();
		
		try {
			ipc.writeSDU(port_id, encMessage, encMessage.length);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	/**
	 * Add missing fields to the message originated at the manager.
	 * It adds connection specific detail (src, dst, version, abstract_syntax, and an invoke id)
	 * 
	 * @param builder  The partial CDAP message
	 * @return A Builder that can be manipulated later.
	 */
	public CDAPMessage.Builder complete_message_for_ma(CDAPMessage.Builder builder) {
		// Connection negotiated bits
		builder.setAbsSyntax(abstract_syntax);
		builder.setInvokeID(message_id ++);
		builder.setVersion(version);
		
		// Source bits
		ApplicationProcessNamingInformation local = info.getLocalAppName();
		builder.setSrcApName(local.getProcessName());
		builder.setSrcApInst(local.getProcessInstance());
		builder.setSrcAEName(local.getEntityName());
		builder.setSrcAEInst(local.getEntityInstance());
		// Destination bits
		ApplicationProcessNamingInformation remote = info.getRemoteAppName();
		builder.setDestApName(remote.getProcessName());
		builder.setDestApInst(remote.getProcessInstance());
		/// This needs to change due to silly behaviour on the Management Agent.
		// Ae Name must be set to the version string.
		if (!builder.hasDestAEName()) {
			builder.setDestAEName(remote.getEntityName());
		    //builder.setDestAEName(new String("v") + version);
		    builder.setDestAEInst(remote.getEntityInstance());
		}
		// Type conversion
		if (builder.hasObjValue() && builder.getObjValue().hasJsonval()) {
			objVal_t value = builder.getObjValue();
			objVal_t.Builder newvalue = CDAPSerialiser.decodeJSONValue(value.getJsonval(), value.getTypeval());
			builder.setObjValue(newvalue);
		} else {
			info("Not a JSON value");
		}
		
		info("Message to MA [" + remote.getProcessName()+ ":" + remote.getProcessInstance() + ":" + remote.getEntityName() + ":" + remote.getEntityInstance() + "]");
		return builder;
	}

	/**
	 * Add missing fields to the message originated at the agent.
	 * It adds connection specific detail (src, dst)
	 * 
	 * @param builder  The partial CDAP message
	 * @return A Builder that can be manipulated later.
	 */
	public CDAPMessage.Builder complete_message_for_manager(CDAPMessage.Builder builder) {
		// version info is not added as the converter encodes this in the JSON "Language" header
		
		// Source bits
		ApplicationProcessNamingInformation remote = info.getRemoteAppName();
		builder.setSrcApName(remote.getProcessName());
		builder.setSrcApInst(remote.getProcessInstance());
		builder.setSrcAEName(remote.getEntityName());
		builder.setSrcAEInst(remote.getEntityInstance());
		// Destination bits
		ApplicationProcessNamingInformation local = info.getLocalAppName();
		builder.setDestApName(local.getProcessName());
		builder.setDestApInst(local.getProcessInstance());
		builder.setDestAEName(local.getEntityName());
		builder.setDestAEInst(local.getEntityInstance());
		
		return builder;
	}


	/*
	 * The following functions are used to generate CDAP Messages to test the MA connection
	 */

	/**
	 * Construct a message from scratch with a given opCode and message id
	 * @param opCode	  CDAP operation this message represents
	 * @param messageId   The invoke id for the message
	 * @return  A CDAPMessage.Builder that allows additional info to be set.
	 */
	private CDAPMessage.Builder construct_message(opCode_t opCode, int messageId) {
		CDAPMessage.Builder builder = CDAPMessage.newBuilder();
		builder.setAbsSyntax(abstract_syntax);
		builder.setInvokeID(messageId);
		// Set opcode
		builder.setOpCode(opCode);
		builder.setVersion(version);
		
		// Source bits
		ApplicationProcessNamingInformation local = info.getLocalAppName();
		builder.setSrcApName(local.getProcessName());
		builder.setSrcApInst(local.getProcessInstance());
		builder.setSrcAEName(local.getEntityName());
		builder.setSrcAEInst(local.getEntityInstance());
		// Destination bits
		ApplicationProcessNamingInformation remote = info.getRemoteAppName();
		builder.setDestApName(remote.getProcessName());
		builder.setDestApInst(remote.getProcessInstance());
		builder.setDestAEName(remote.getEntityName());
		builder.setDestAEInst(remote.getEntityInstance());
		
		return builder;
	}
	
	/**
	 * Constructs a reply message
	 * @param request The CDAP request message
	 * @return A CDAP message builder
	 */
	private CDAPMessage.Builder construct_reply(CDAPMessage request) {
		CDAPMessage.Builder builder = null;

		// OPCODE
		opCode_t opcode = calculate_reply_opcode(request.getOpCode());
		if (opcode != opCode_t.M_STOP) { // Trick to skip messages that dont need a reply
			builder = construct_message(opcode, request.getInvokeID());
		}
	
		return builder;
	}
	
	

	/**
	 * Calculate the response opcode based on the CDAP state machine
	 * @param opCode The incoming opCode
	 * @return The reply opcode
	 */
	private opCode_t calculate_reply_opcode(opCode_t opCode) {
		switch(opCode) {
		case M_CONNECT:
			return opCode_t.M_CONNECT_R;
		case M_RELEASE:
			return opCode_t.M_RELEASE_R;
		case M_CREATE:
			return opCode_t.M_CREATE_R;
		case M_DELETE:
			return opCode_t.M_DELETE_R;
		case M_READ:
			return opCode_t.M_READ_R;
		case M_CANCELREAD:
			return opCode_t.M_CANCELREAD_R;
		case M_WRITE:
			return opCode_t.M_WRITE_R;
		case M_START:
			return opCode_t.M_START_R;
		case M_STOP:
			return opCode_t.M_STOP_R;
		default:
			// Everything else should not yield a response
			return opCode_t.M_STOP; // Used to indicate a problem				
		}
	}


	/*
	 * What to do on receiving a response
	 */
	private void open_connection(CDAPMessage message, int port_id) {
		info("Open connection requested [port-id="+port_id+ "].");
		abstract_syntax = message.getAbsSyntax();
		version = message.getVersion();

		info("Message from MA [" + message.getSrcApName() + ":" + message.getSrcApInst() + ":" + message.getSrcAEName() + ":" + message.getSrcAEInst() + "]");
		info("             to [" + message.getDestApName() + ":" + message.getDestApInst() + ":" + message.getDestAEName() + ":" + message.getDestAEInst() + "]");
		
		CDAPMessage.Builder builder = construct_reply(message);
		CDAPMessage reply = builder.setFlags(flagValues_t.F_NO_FLAGS)
				.setResult(0)
				.setResultReason("OK")
				.setVersion(message.getVersion())
				.build();

		info("CDAP OPEN: Replying to [" + reply.getDestApName() + ":" + reply.getDestApInst() + ":" + reply.getDestAEName() + ":" + reply.getDestAEInst()+ "].");
		
		byte[] encMessage = CDAPSerialiser.encodeCDAPMessage(reply);
		send(encMessage);
		info("CDAP OPEN: Accepting response issued (" + abstract_syntax + ",v" + version + ")");
	}


	private void close_connection(CDAPMessage message, int port_id) {
		info("Close connection requested. [port-id="+port_id+ "].");
		CDAPMessage.Builder builder = construct_reply(message);
		CDAPMessage reply = builder.setFlags(flagValues_t.F_NO_FLAGS)
				.setResult(1)
				.setResultReason("OK")
				.build();
		
		byte[] encMessage = CDAPSerialiser.encodeCDAPMessage(reply);
		send(encMessage);
		info("CDAP CLOSE: Accepting response issued");
	}

/*	
    public void remote_create_result(CDAPMessage message, int port_id) {
		// Log it
		info("CDAP CREATE result:[ port=" + port_id + ", result=" + message.getResult() + "]");
	}

	
	public void remote_read_result(CDAPMessage message, int port_id) {
		// Log it
		info("CDAP READ result:[ port=" + port_id + ", result=" + message.getResult() + "]");
		
	}
*/
	
	/*
	 * Some handy testing functions
	 * 
	 */
	
	// TODO FIXME
	public void create_read(int messageId) {
		info("Close connection requested. [port-id="+port_id+ "].");
		CDAPMessage.Builder builder = construct_message(opCode_t.M_READ, messageId);
		byte[] filter = new byte[1];
		filter[0]=0x00;
		
		CDAPMessage reply = builder.setFlags(flagValues_t.F_NO_FLAGS)
				.setObjName("root, computingSystemID = 1, processingSystemID=1, kernelApplicationProcess, osApplicationProcess, ipcProcesses, ipcProcessID=2, RIBDaemon")
				.setObjInst(0)
				.setObjClass("RIBDaemon")
				.setScope(0)
				.setFilter(ByteString.copyFrom(filter))
				.build();
		
		byte[] encMessage = CDAPSerialiser.encodeCDAPMessage(reply);
		send(encMessage);
		info("CDAP READ: issued");
	}
	
	
	public dif_info_t.Builder createDIFInfo() {
				
		// Data transfer constants
		dataTransferConstants_t dt_const = dataTransferConstants_t.newBuilder()
				.setAddressLength(2)
				.setCepIdLength(2)
				.setLengthLength(2)
				.setPortIdLength(2)
				.setQosidLength(2)
				.setSequenceNumberLength(4)
				.setMaxPDUSize(10000)
				.setMaxPDULifetime(60000)
				.setCtrlSequenceNumberLength(4)
				.setRateLength(4)
				.setFrameLength(4)
				.build();	
		
		// QoS cube unreliable with flow control
		qosCube_t.Builder unreliable = qosCube_t.newBuilder()
				.setName("unreliablewithflowcontrol")
				.setQosId(1)
				.setPartialDelivery(false)
				.setOrder(true)
				; //.build();
		
		policyDescriptor_t dtp_policy = policyDescriptor_t.newBuilder()
				.setPolicyName("default")
				.setVersion("0")
				.build();
		dtpConfig_t.Builder dtp_config = dtpConfig_t.newBuilder()
				.setInitialATimer(300)
				.setDtcpPresent(true)
				;		
		
		dtcpConfig_t.Builder dtcp_config = dtcpConfig_t.newBuilder()
				.setRtxControl(false)
				.setFlowControl(true);
		policyDescriptor_t dtcp_policy = policyDescriptor_t.newBuilder()
				.setPolicyName("default")
				.setVersion("0")
				.build();
		
		dtcpFlowControlConfig_t.Builder flow_control_config = dtcpFlowControlConfig_t.newBuilder()
				.setRateBased(false)
				.setWindowBased(true)
				;
		
		dtcpWindowBasedFlowControlConfig_t window_based_config = dtcpWindowBasedFlowControlConfig_t.newBuilder()
				.setMaxclosedwindowqueuelength(50)
				.setInitialcredit(50)
				.build();

		// Qos Cube reliable with flow control
		qosCube_t.Builder reliable = qosCube_t.newBuilder()
				.setName("reliablewithflowcontrol")
				.setQosId(2)
				.setPartialDelivery(false)
				.setOrder(true)
				.setMaxAllowableGapSdu(0)
				; //.build();
		dtpConfig_t.Builder dtp_config2 = dtpConfig_t.newBuilder()
				.setInitialATimer(300)
				.setDtcpPresent(true);
		policyDescriptor_t dtp_policy2 = policyDescriptor_t.newBuilder()
				.setPolicyName("default")
				.setVersion("0")
				.build();
		dtcpConfig_t.Builder dtcp_config2 = dtcpConfig_t.newBuilder()
				.setRtxControl(false)
				.setFlowControl(true)
				; //.build();		
		policyDescriptor_t dtcp_policy2 = policyDescriptor_t.newBuilder()
				.setPolicyName("default")
				.setVersion("0")
				.build();
		dtcpRtxControlConfig_t rtx_control_config2 = dtcpRtxControlConfig_t.newBuilder()
				.setDatarxmsnmax(5)
				.setInitialRtxTime(1000)
				.build();	
		dtcpFlowControlConfig_t.Builder flow_control_config2 = dtcpFlowControlConfig_t.newBuilder()
				.setRateBased(false)
				.setWindowBased(true);
		dtcpWindowBasedFlowControlConfig_t window_based_config2 = dtcpWindowBasedFlowControlConfig_t.newBuilder()
				.setMaxclosedwindowqueuelength(50)
				.setInitialcredit(50)
				.build();
		

		// Namespace configuration
		static_addr_config_t addr1 = static_addr_config_t.newBuilder()
				.setApName("test1.IRATI")
				.setApInstance("1")
				.setAddress(16)
				.build();		
		static_addr_config_t addr2 = static_addr_config_t.newBuilder()
				.setApName("test2.IRATI")
				.setApInstance("1")
				.setAddress(17)
				.build();				
		addre_pref_config_t pref1 = addre_pref_config_t.newBuilder()
				.setAddressPrefix(0)
				.setOrganization("N.Bourbaki")
				.build();
		addre_pref_config_t pref2 = addre_pref_config_t.newBuilder()
				.setAddressPrefix(16)
				.setOrganization("IRATI")
				.build();
		policyDescriptor_t nsm_policy = policyDescriptor_t.newBuilder()
				.setPolicyName("default")
				.setVersion("1")
				.build();
		
		
		// RMT configuration
		policyDescriptor_t pft_policy = policyDescriptor_t.newBuilder()
				.setPolicyName("default")
				.setVersion("0")
				.build();
		policyDescriptor_t rmt_policy = policyDescriptor_t.newBuilder()
				.setPolicyName("default")
				.setVersion("1")
				.build();

		// Enrollment Task configuration
		ArrayList<property_t> params = new ArrayList<property_t>(5);
		property_t.Builder par = property_t.newBuilder()
				.setName("enrollTimeoutInMs")
				.setValue("10000");
		params.add(par.build());
		par = property_t.newBuilder()
				.setName("watchdogPeriodInMs")
				.setValue("30000");
		params.add(par.build());
		par = property_t.newBuilder()
				.setName("declaredDeadIntervalInMs")
				.setValue("120000");
		params.add(par.build());
		par = property_t.newBuilder()
				.setName("neighborsEnrollerPeriodInMs")
				.setValue("30000");
		params.add(par.build());
		par = property_t.newBuilder()
				.setName("maxEnrollmentRetries")
				.setValue("3");
		params.add(par.build());
		
		policyDescriptor_t et_policy = policyDescriptor_t.newBuilder()
				.setPolicyName("default")
				.setVersion("1")
				.addAllPolicyParameters(params)
				.build();
		
		// Flow allocation configuration
		policyDescriptor_t fa_policy = policyDescriptor_t.newBuilder()
				.setPolicyName("default")
				.setVersion("1")
				.build();
		
		// Security manager configuration
		policyDescriptor_t sm_policy = policyDescriptor_t.newBuilder()
				.setPolicyName("default")
				.setVersion("1")
				.build();
		
		// Resource allocation configuration
		policyDescriptor_t ra_policy = policyDescriptor_t.newBuilder()
				.setPolicyName("default")
				.setVersion("0")
				.build();
		
		// Routing configuration
		params.clear();
		par = property_t.newBuilder()
				.setName("objectMaximumAge")
				.setValue("10000");
		params.add(par.build());
		par = property_t.newBuilder()
				.setName("waitUntilReadCDAP")
				.setValue("5001");
		params.add(par.build());
		par = property_t.newBuilder()
				.setName("waitUntilError")
				.setValue("5001");
		params.add(par.build());
		par = property_t.newBuilder()
				.setName("waitUntilPFUFTComputation")
				.setValue("103");
		params.add(par.build());
		par = property_t.newBuilder()
				.setName("waitUntilFSODBPropagation")
				.setValue("101");
		params.add(par.build());
		par = property_t.newBuilder()
				.setName("waitUntilAgeIncement")
				.setValue("997");
		params.add(par.build());
		
		policyDescriptor_t ro_policy = policyDescriptor_t.newBuilder()
				.setPolicyName("link-state")
				.setVersion("1")
				.addAllPolicyParameters(params)
				.build();
		
		
		applicationProcessNamingInfo_t.Builder dif_name = applicationProcessNamingInfo_t.newBuilder();
		dif_name.setApplicationProcessName("normal.DIF");
		
		// Assignments after initializations
		efcp_config_t.Builder efcp_conf = efcp_config_t.newBuilder();
		dif_info_t.Builder dif_info=dif_info_t.newBuilder()
				.setDifType("normal-ipc")
				;
		
		dif_info.setDifName(dif_name);
		efcp_conf.setDataTransferConstants(dt_const);	
		
		
		// unreliable QoS Cube
		dtp_config.setDtppolicyset(dtp_policy);
		flow_control_config.setWindowBasedConfig(window_based_config);
		dtcp_config.setFlowControlConfig(flow_control_config);
		dtcp_config.setDtcppolicyset(dtcp_policy);
		unreliable.setDtcpConfiguration(dtcp_config);
		unreliable.setDtpConfiguration(dtp_config);
		
		// add unreliable QoS cube
		efcp_conf.addQosCubes(unreliable);
		
		
		// reliable QoS Cube
		dtp_config2.setDtppolicyset(dtp_policy2);
		flow_control_config2.setWindowBasedConfig(window_based_config2);
		dtcp_config2.setFlowControlConfig(flow_control_config2);	
		dtcp_config2.setDtcppolicyset(dtcp_policy2);
		dtcp_config2.setRtxControlConfig(rtx_control_config2);
		reliable.setDtcpConfiguration(dtcp_config2);
		reliable.setDtpConfiguration(dtp_config2);
		efcp_conf.addQosCubes(reliable);
		
		// addressing
		addr_config_t.Builder addressing_config = addr_config_t.newBuilder()
				.addAddress(addr1)
				.addAddress(addr2)
				.addPrefixes(pref1)
				.addPrefixes(pref2);
		nsm_config_t.Builder nsm_config = nsm_config_t.newBuilder()
			.setPolicySet(nsm_policy)
			.setAddressingConfig(addressing_config);		
		dif_config_t.Builder dif_conf = dif_config_t.newBuilder()
				.setAddress(16)
				.setNsmConfig(nsm_config);
		
		//pft_conf
		rmt_config_t.Builder rmt_conf = rmt_config_t.newBuilder()
			.setPolicySet(rmt_policy)
			.setPftConf(pft_policy);
		dif_conf.setRmtConfig(rmt_conf);

		dif_conf.setEtConfig(et_policy);
		
		fa_config_t.Builder fa_config = fa_config_t.newBuilder()
				.setPolicySet(fa_policy);
		dif_conf.setFaConfig(fa_config);
		
		security_config_t.Builder sm_config = security_config_t.newBuilder()
				.setPolicySet(sm_policy);
		dif_conf.setSmConfig(sm_config);
		
		dif_conf.setRaConfig(ra_policy);		
		dif_conf.setRoutingConfig(ro_policy);
		dif_conf.setEfcpConfig(efcp_conf);
		
		dif_info.setDifConfig(dif_conf);
		
		
		return dif_info;
	}		

	// Test IPCP 1
	public void createIPCP1(int messageId) {

		// Create message
		CDAPMessage.Builder builder = construct_message(opCode_t.M_CREATE, messageId);

		// JSON version
		String ipcpJSON = "{\"process_name\": {\"applicationProcessName\": \"normal-1.IPCP)\",\"applicationProcessInstance\": \"1\"},\"dif_to_assign\": {\"dif_type\": \"normal-ipc\",\"dif_name\": {\"applicationProcessName\": \"normal.DIF\"},\"dif_config\": {\"address\": 16,\"efcp_config\": {\"data_transfer_constants\": {\"maxPDUSize\": 10000,\"addressLength\": 2,\"portIdLength\": 2,\"cepIdLength\": 2,\"qosidLength\": 2,\"sequenceNumberLength\": 4,\"lengthLength\": 2,\"maxPDULifetime\": 60000,\"rateLength\": 4,\"frameLength\": 4,\"ctrlSequenceNumberLength\": 4},\"qos_cubes\": [{\"qosId\": 1,\"name\": \"unreliablewithflowcontrol\",\"partialDelivery\": false,\"order\": true,\"dtpConfiguration\": {\"dtcpPresent\": true,\"initialATimer\": 300,\"dtppolicyset\": {\"policyName\": \"default\",\"version\": \"0\"}},\"dtcpConfiguration\": {\"flowControl\": true,\"flowControlConfig\": {\"windowBased\": true,\"windowBasedConfig\": {\"maxclosedwindowqueuelength\": 50,\"initialcredit\": 50},\"rateBased\": false},\"rtxControl\": false,\"dtcppolicyset\": {\"policyName\": \"default\",\"version\": \"0\"}}},{\"qosId\": 2,\"name\": \"reliablewithflowcontrol\",\"partialDelivery\": false,\"order\": true,\"maxAllowableGapSdu\": 0,\"dtpConfiguration\": {\"dtcpPresent\": true,\"initialATimer\": 300,\"dtppolicyset\": {\"policyName\": \"default\",\"version\": \"0\"}},\"dtcpConfiguration\": {\"flowControl\": true,\"flowControlConfig\": {\"windowBased\": true,\"windowBasedConfig\": {\"maxclosedwindowqueuelength\": 50,\"initialcredit\": 50},\"rateBased\": false},\"rtxControl\": false,\"rtxControlConfig\": {\"datarxmsnmax\": 5,\"initialRtxTime\": 1000},\"dtcppolicyset\": {\"policyName\": \"default\",\"version\": \"0\"}}}]},\"rmt_config\": {\"policy_set\": {\"policyName\": \"default\",\"version\": \"1\"},\"pft_conf\": {\"policyName\": \"default\",\"version\": \"0\"}},\"fa_config\": {\"policy_set\": {\"policyName\": \"default\",\"version\": \"1\"}},\"et_config\": {\"policyName\": \"default\",\"version\": \"1\",\"policyParameters\": [{\"name\": \"enrollTimeoutInMs\",\"value\": \"10000\"},{\"name\": \"watchdogPeriodInMs\",\"value\": \"30000\"},{\"name\": \"declaredDeadIntervalInMs\",\"value\": \"120000\"},{\"name\": \"neighborsEnrollerPeriodInMs\",\"value\": \"30000\"},{\"name\": \"maxEnrollmentRetries\",\"value\": \"3\"}]},\"nsm_config\": {\"addressing_config\": {\"address\": [{\"ap_name\": \"test1.IRATI\",\"ap_instance\": \"1\",\"address\": 16},{\"ap_name\": \"test2.IRATI\",\"ap_instance\": \"1\",\"address\": 17}],\"prefixes\": [{\"address_prefix\": 0,\"organization\": \"N.Bourbaki\"},{\"address_prefix\": 16,\"organization\": \"IRATI\"}]},\"policy_set\": {\"policyName\": \"default\",\"version\": \"1\"}},\"routing_config\": {\"policyName\": \"link-state\",\"version\": \"1\",\"policyParameters\": [{\"name\": \"objectMaximumAge\",\"value\": \"10000\"},{\"name\": \"waitUntilReadCDAP\",\"value\": \"5001\"},{\"name\": \"waitUntilError\",\"value\": \"5001\"},{\"name\": \"waitUntilPFUFTComputation\",\"value\": \"103\"},{\"name\": \"waitUntilFSODBPropagation\",\"value\": \"101\"},{\"name\": \"waitUntilAgeIncement\",\"value\": \"997\"}]},\"ra_config\": {\"policyName\": \"default\",\"version\": \"0\"},\"sm_config\": {\"policy_set\": {\"policyName\": \"default\",\"version\": \"1\"}}}}}";	
		objVal_t.Builder value = CDAPSerialiser.decodeJSONValue(ipcpJSON, "rina.messages.MAIPCP.ipcp_config_t");
		
		
		// Object info
		builder.setObjName("/computingSystemID=1/processingSystemID=1/kernelApplicationProcess/osApplicationProcess/ipcProcesses/ipcProcessID=5")
			.setObjInst(0)
			.setObjClass("IPCProcess")
			.setObjValue(value);
		
		// Scope & Filter
		byte[] filter = new byte[1];
		filter[0]=0x00;
		CDAPMessage request = builder.setFlags(flagValues_t.F_NO_FLAGS)
				.setScope(0)
				.setFilter(ByteString.copyFrom(filter))
				.build();
		
		byte[] encMessage = CDAPSerialiser.encodeCDAPMessage(request);
		send(encMessage);
		info("CDAP CREATE: IPCP issued");
	}
	
	// Test IPCP 2
	public void createIPCP2(int messageId) {

		// Create IPCP config
		applicationProcessNamingInfo_t proc_name = applicationProcessNamingInfo_t.newBuilder()
				.setApplicationProcessInstance("1")
				.setApplicationProcessName("test1.IRATI)")
				.build();	
		dif_info_t.Builder dif_info = createDIFInfo();
		
		ipcp_config_t.Builder ipc_config = ipcp_config_t.newBuilder()
				.setProcessName(proc_name)
				.setDifToAssign(dif_info);
		
//		ipcp_config_t ipc_config = ipcp_config_t.newBuilder()
//				.setProcessInstance("1").setProcessName("normal-1.IPCP")
//				.setProcessName(name)
//				.setProcessType("normal-ipc").setDifToAssign("normal.DIF")
//				//.setDifToRegister("400")
//				.build();
	
		objVal_t.Builder value = objVal_t.newBuilder();
		value.setByteval(ipc_config.build().toByteString());

		// Create message
		CDAPMessage.Builder builder = construct_message(opCode_t.M_CREATE, messageId);
		
		// Object info
		builder.setObjName("/computingSystemID=1/processingSystemID=1/kernelApplicationProcess/osApplicationProcess/ipcProcesses/ipcProcessID=5")
			.setObjInst(0)
			.setObjClass("IPCProcess")
			.setObjValue(value);
		
		// Scope & Filter
		byte[] filter = new byte[1];
		filter[0]=0x00;
		CDAPMessage request = builder.setFlags(flagValues_t.F_NO_FLAGS)
				.setScope(0)
				.setFilter(ByteString.copyFrom(filter))
				.build();
		
		info("From Sven:" + CDAPSerialiser.encodeJSONMessage(request));
		
		byte[] encMessage = CDAPSerialiser.encodeCDAPMessage(request);
		send(encMessage);
		info("CDAP CREATE: IPCP issued");
	}

	
	/*
	 * Logging functions
	 */	
	private void info(String string) {
		System.out.println("INFO:" + string);
	}

	private void warn(String string) {
		System.out.println("WARN:" + string);
	}

	private void err(String string) {
		System.err.println("ERROR:" + string);
	}


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
			b.append(". ");
			break;
		case M_CONNECT:
		case M_RELEASE:
			b.append("(v");
			b.append(message.getVersion());
			b.append(",");
			b.append(message.getAbsSyntax());
			b.append("). ");
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
				b.append(" FAILED");
				if (message.hasResultReason()) {
					b.append("(");
					b.append(message.getResultReason());
					b.append(") ");
				}
				b.append(". ");
			} else {
				b.append(" SUCCESSFUL. ");
			}
			break;
		}
		b.append("Sending to MA[");
		b.append(getId());
		b.append("]");
		//info("CDAP " + message.getOpCode() + " of " + message.getObjClass() +"Sending to MA[" + getId() + "] ");
		info(b.toString());
	}
	


	/**
	 * 
	 * Extra debugging functions
	 * 
	 *
	private static void printHex(byte[] bytes, int offset, int width) {
		for (int index = 0; index < width; index++) {
			if (index + offset < bytes.length) {
				System.out.printf("%02x ", bytes[index + offset]);
			} else {
				System.out.print("	");
			}
		}
	}

	private static void printAscii(byte[] bytes, int index, int width) {
		if (index < bytes.length) {
			width = Math.min(width, bytes.length - index);
			try {
				System.out.println(
					":"
						+ new String(bytes, index, width, "UTF-8").replaceAll("\r\n", " ").replaceAll(
							"\n",
							" "));
			} catch (UnsupportedEncodingException e) {
				System.out.println(": <illegal chars>");
			}
		} else {
			System.out.println();
		}
	}*/


}
