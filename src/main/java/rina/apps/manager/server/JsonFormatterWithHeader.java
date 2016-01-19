/**
 * (C) Copyright 2015, Waterford Institute of Technology, PRISTINE.
 */
package rina.apps.manager.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import rina.messages.CDAP.CDAPMessage;

import com.googlecode.protobuf.format.JsonFormat;

import eu.ict_pristine.wp5.dms.es.dsl.esdsls.eslang.v100.ESD_ValueKeys;
import eu.ict_pristine.wp5.dms.es.taxonomy.ESEvent_Header;

/**
 * Output json formatting with ES_Event headers
 * 
 * @author mcrotty@tssg.org
 *
 */
public class JsonFormatterWithHeader extends JsonFormat {

	/**
	 * Output a json encoded event with header information included.
	 * @param message  The CDAP message to be jsonified
	 * @param header   The header 
	 * @return
	 */
	public static String encodeJSONMessage(CDAPMessage message, ESEvent_Header header) {

        try {
            StringBuilder text = new StringBuilder();
            JsonFormat.JsonGenerator generator = new JsonFormat.JsonGenerator(text);
            generator.print("{");
            print(header, generator);
            
            print(message, generator);
            
            generator.print("}");
            return text.toString();
        } catch (IOException e) {
            throw new RuntimeException("Writing to a StringBuilder threw an IOException (should never happen).",
                                       e);
        }
	}


	/**
	 * Output the event header fields as Json
	 * 
	 * @param header       The event header
	 * @param generator    A generator for json
	 * @throws IOException
	 */
	public static void print(ESEvent_Header header, JsonFormat.JsonGenerator generator) throws IOException {
		
        for (Iterator<Map.Entry<String, Object>> i=header.asMap().entrySet().iterator(); i.hasNext();) {
        	
        	Map.Entry<String, Object> entry = i.next();
			generator.print("\"");
			generator.print(entry.getKey());
			generator.print("\":");
			
			generator.print("\"");
			generator.print(entry.getValue().toString());
			generator.print("\"");
			
//           if (i.hasNext()) {
                generator.print(",");
//            }
		}

        // Extra stuff
        Map<String,String> extras = new HashMap<String,String>();
        extras.put(ESD_ValueKeys.name.getName(), "t" + header.getSource());
        extras.put(ESD_ValueKeys.reason.getName(), header.getDialect());
        extras.put(ESD_ValueKeys.info.getName(), "CDAP response");
               
        for (Entry<String, String> entry : extras.entrySet()) {
        	
        	generator.print("\"");
			generator.print(entry.getKey());
			generator.print("\":");
			
			generator.print("\"");
			generator.print(entry.getValue().toString());
			generator.print("\"");
			
            generator.print(",");
		}
 
	}

	// This is currently not working.
//    protected static void print(GeneratedMessageLite message, JsonGenerator generator) throws IOException {
//
//        for (Iterator<Map.Entry<FieldDescriptor, Object>> iter = message.getAllFields().entrySet().iterator(); iter.hasNext();) {
//            Map.Entry<FieldDescriptor, Object> field = iter.next();
//            printField(field.getKey(), field.getValue(), generator);
//            if (iter.hasNext()) {
//                generator.print(",");
//            }
//        }
//        if (message.getUnknownFields().asMap().size() > 0)
//            generator.print(", ");
//        printUnknownFields(message.getUnknownFields(), generator);
//    }

	
}
