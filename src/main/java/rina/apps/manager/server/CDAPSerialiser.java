/**
 * Micheal Crotty, Pristine.
 * Copyright (c) 2015, Waterford Institute of Technology.
 */
package rina.apps.manager.server;

import java.io.UnsupportedEncodingException;

import rina.messages.CDAP.CDAPMessage;
import rina.messages.CDAP.opCode_t;
import rina.messages.MAIPCP;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.googlecode.protobuf.format.JsonFormat;
import com.googlecode.protobuf.format.JsonFormat.ParseException;

import eu.ict_pristine.wp5.dms.es.taxonomy.ESEvent_Header;


/**
 * A bypass class for the CDAP native serialiser.
 * 
 * @author mcrotty@tssg.org
 *
 */
public class CDAPSerialiser {
	
	
	/**
	 * Decode from CDAP binary
	 * @param b A byte array containing the JSON
	 * @param bytesRead Number of bytes read.
	 */
	static public CDAPMessage decodeCDAPMessage(byte[] b, int bytesRead) {
		// Copy buffer
		ByteString sdu = ByteString.copyFrom(b, 0, bytesRead);
		
	    CDAPMessage message = null;
		try {
			message = CDAPMessage.parseFrom(sdu);
		    validate(message);
		    info("Message: abssyntax=" + message.getAbsSyntax() + ", version=" + message.getVersion());;
		    info("Message opcode is=" + message.getOpCode() + ", size=" + bytesRead);
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			// Dump the buffer
			int width = 16;
			for (int index = 0; index < bytesRead; index += width) {
				printHex(sdu, index, width);
				printAscii(sdu, index, width);
			}				
		}
		return message;
	}

	/**
	 * Encode to a CDAP binary
	 * @param message The message 
	 * @return A byte array containing the message
	 */
	public static byte[] encodeCDAPMessage(CDAPMessage message) {		
	    validate(message);
		return message.toByteArray();
	}
	
	/**
	 * Decode from JSON
	 * @param b A string containing the JSON
	 * @return A CDAPMessage
	 */
	public static CDAPMessage.Builder decodeJSONMessage(String b) {
	    CDAPMessage.Builder message = null;
		try {
			CDAPMessage.Builder builder = CDAPMessage.newBuilder();
			JsonFormat.merge(b, builder);
			//message = builder.build(); 
			message = builder; 
		    //validate(message);
		} catch (ParseException e) {
			e.printStackTrace();
			// Dump the buffer
			System.err.println(b);
		}
		return message;
	}
	
	/**
	 * Encode to JSON
	 * @param message The message 
	 * @return A string containing the JSON
	 */
	public static String encodeJSONMessage(CDAPMessage message) {
	    validate(message);
		return JsonFormat.printToString(message);
	}

	
	/**
	 * Encode to JSON
	 * @param message The message 
	 * @return A string containing the JSON
	 */
	public static String encodeJSONMessage(CDAPMessage message, ESEvent_Header header) {
	    validate(message);
		return JsonFormatterWithHeader.encodeJSONMessage(message, header);
	}
	
	/**
	 * Test the encoding of ipcp config to JSON
	 * @param config
	 * @return The JSON encoded string
	 */
	public static String encodeJSONMessage(MAIPCP.ipcp_config_t config) {
	    //validate(message);
		return JsonFormat.printToString(config);
	}

	/**
	 * Test the decoding of the ipcp config from JSON
	 * @param b The Json string
	 * @return
	 */
	public static MAIPCP.ipcp_config_t.Builder decodeJSONConfig(String b) {
		MAIPCP.ipcp_config_t.Builder message = null;
		try {
			MAIPCP.ipcp_config_t.Builder builder = MAIPCP.ipcp_config_t.newBuilder();
			JsonFormat.merge(b, builder);
			//message = builder.build(); 
			message = builder; 
		    //validate(message);
		} catch (ParseException e) {
			e.printStackTrace();
			// Dump the buffer
			System.err.println(b);
		}
		return message;
	}
	
	/**
	 * Validate the message, required parameters must be present
	 * @param message
	 */
	static private void validate(CDAPMessage message) {
	    assert(message.hasOpCode());
	    assert(message.hasInvokeID());
	    opCode_t opcode = message.getOpCode();
	    
	    if ((opcode == opCode_t.M_CONNECT) || (opcode == opCode_t.M_CONNECT_R)) {
	    	assert(message.hasAbsSyntax());
		    assert(message.hasVersion());
	    } else if ((opcode == opCode_t.M_CREATE) || (opcode == opCode_t.M_DELETE) || 
	    		(opcode == opCode_t.M_READ) || (opcode == opCode_t.M_WRITE) ||
	    		(opcode == opCode_t.M_START) || (opcode == opCode_t.M_STOP) ||
	    		(opcode == opCode_t.M_CANCELREAD) || (opcode == opCode_t.M_READ_R)) {
	    	assert(message.hasObjClass());
	    	assert(message.hasObjName());
	    	assert(message.hasObjInst());
	    }
	
	}
	
	private static void info(String s) {
		System.out.println("INFO:" + s);
	}

	/**
	 * 
	 * Extra debugging functions
	 * 
	 */
	
	private static void printHex(ByteString bytes, int offset, int width) {
		for (int index = 0; index < width; index++) {
			if (index + offset < bytes.size()) {
				System.out.printf("%02x ", bytes.byteAt(index + offset));
			} else {
				System.out.print("	");
			}
		}
	}

	private static void printAscii(ByteString bytes, int index, int width) {
		if (index < bytes.size()) {
			width = Math.min(width, bytes.size() - index);
			try {
				System.out.println(
					":"
						+ new String(bytes.toByteArray(), index, width, "UTF-8").replaceAll("\r\n", " ").replaceAll(
							"\n",
							" "));
			} catch (UnsupportedEncodingException e) {
				System.out.println(": <illegal chars>");
			}
		} else {
			System.out.println();
		}
	}

	
}
