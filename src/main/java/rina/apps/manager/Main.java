/**
 * Micheal Crotty, Pristine.
 * Copyright (c) 2015, Waterford Institute of Technology.
 */
package rina.apps.manager;

import java.io.IOException;
import java.util.Arrays;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import rina.apps.manager.server.CDAPServer;
import eu.irati.librina.ApplicationProcessNamingInformation;

/**
 * This is the main class that parses the command line arguments.
 * And instantiates the CDAPSever with those arguments
 * 
 * @author mcrotty@tssg.org
 * 
 */
public class Main {

	static {
		System.loadLibrary("rina_java");
		System.loadLibrary("rina");
	}
	
	// Default argument values
	public static final String  DEFAULT_DIF = "NMS.DIF";
	public static final String  DEFAULT_AP_NAME = "rina.apps.manager";
	public static final String  DEFAULT_AP_INST = "1";
	public static final String  DEFAULT_LEVEL = "DEBUG";
	public static final Integer DEFAULT_WAIT = 10;
	public static final String  DEFAULT_WS = "ws://localhost:8887";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println(Arrays.toString(args));

		OptionParser parser = new OptionParser();
        parser.accepts( "manager-apn" ).withRequiredArg().ofType( String.class );
        parser.accepts( "manager-api" ).withRequiredArg().ofType( String.class );	
        parser.accepts( "dif" ).withRequiredArg().ofType( String.class );
        parser.accepts( "wait", "Time to wait before attempting CDAP operaitons" ).withRequiredArg().ofType( Integer.class );
        parser.accepts( "l", "Logging level" ).withOptionalArg().ofType( String.class );	
        parser.accepts( "ws", "WS connection URI" ).withOptionalArg().ofType( String.class );
        parser.accepts( "help" );	
        
        // Parse the command line.
        OptionSet options = parser.parse(args);
        
        // Use the options if set
		String difName = options.has("dif")?(String)options.valueOf("dif") : DEFAULT_DIF;
		String serverApName = options.has("manager-apn")?(String)options.valueOf("manager-apn") : DEFAULT_AP_NAME;
		String serverApInstance = options.has("manager-api")?(String)options.valueOf("manager-api") : DEFAULT_AP_INST;
        Integer wait = options.has("wait")?(Integer)options.valueOf("wait") : DEFAULT_WAIT;
        String level = options.has("l")?(String)options.valueOf("l") : DEFAULT_LEVEL;
        String ws = options.has("ws")?DEFAULT_WS : null;
        
        if (!options.has("help")) {
	        // Set application name
			ApplicationProcessNamingInformation serverAPNamingInfo = 
					new ApplicationProcessNamingInformation(serverApName, serverApInstance);
	
			System.out.println("Starting server ..");
			CDAPServer echoServer = new CDAPServer(difName, serverAPNamingInfo, wait, ws, level);
			echoServer.execute();
        } else {    
			System.out.println("Additional help:");
            try {
				parser.printHelpOn(System.out);
			} catch (IOException e) {
				e.printStackTrace();
			}        	
        }
    
	}

}
