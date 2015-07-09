package rina.apps.manager;

import java.util.Arrays;

import rina.apps.manager.server.CDAPServer;
import eu.irati.librina.ApplicationProcessNamingInformation;

/**
 * Here the various options you can specify when starting Echo as either a client or a server.
 
-server-apn AP -server-api AI

The AP and AI that Echo app will register as.


-count N

Number of attempts to be made

-wait T

Timeout between CDAP messages

 * @author eduardgrasa
 *
 */
public class Main {

	static {
		System.loadLibrary("rina_java");
	}
	
	public static final String ARGUMENT_SEPARATOR = "-";
	public static final String ROLE = "l";
	public static final String DIF = "d";
	public static final String SDUSIZE = "sdusize";
	public static final String COUNT = "count";
	public static final String SERVER = "server";
	public static final String CLIENT = "client";
	public static final String SAPINSTANCE = "server-api";
	public static final String SAPNAME = "server-apn";
	public static final String CAPINSTANCE = "client-api";
	public static final String CAPNAME = "client-apn";
	public static final String WAIT = "w";
    //public static final String RATE = "rate";
    //public static final String GAP = "gap";
	
	//public static final int DEFAULT_SDU_SIZE_IN_BYTES = 1500;
	public static final int DEFAULT_SDU_COUNT = 5000;
	public static final String DEFAULT_ROLE = SERVER;
	public static final String DEFAULT_DIF = "normal.DIF";
	public static final String DEFAULT_SERVER_AP_NAME = "rina.apps.cdapecho.server";
//	public static final String DEFAULT_CLIENT_AP_NAME = "rina.apps.cdapecho.client";
	public static final String DEFAULT_AP_INSTANCE = "1";
	public static final int DEFAULT_WAIT_IN_MS = 2000;
//    public static final int DEFAULT_RATE_IN_MBPS = 1000;
//    public static final int DEFAULT_MAX_ALLOWABLE_GAP_IN_SDUS = -1;
	
    
//    rina-echo-time  [--dealloc-wait <integer>] [--perf-interval <integer>]
//            [-g <integer>] [-t <string>] [-d <string>] [--client-api
//            <string>] [--client-apn <string>] [--server-api
//            <string>] [--server-apn <string>] [-w <unsigned
//            integer>] [-s <unsigned integer>] [-q] [-r] [-c
//            <unsigned integer>] [-l] [--] [--version] [-h]

    
	public static final String USAGE = "java -jar rina.utils.apps.echo " +
			"[-" + DIF +"] difName [-"+ SAPNAME +"] serverApName [-" + SAPINSTANCE +"] serverApInstance " +
//			"[-client-apn] clientApName " + "[-client-api] clientApInstance " +
//			"[-rate] rate_in_mbps [-gap] max_allowable_gap_in_sdus" +
			"[-"+ COUNT +"] num_cdap " + "[-"+ WAIT +"] wait_in_milliseconds" +
			"[-h]";
	
	public static final String DEFAULTS = "The defaults are: " +DIF+"="+ DEFAULT_DIF + "; "+SAPNAME+"="+ DEFAULT_SERVER_AP_NAME + "; " + 
			SAPINSTANCE+"=" + DEFAULT_AP_INSTANCE +"; "+COUNT+"="+DEFAULT_SDU_COUNT +"; "+WAIT+"="+DEFAULT_WAIT_IN_MS;
	
	
	public static void main(String[] args){
		System.out.println(Arrays.toString(args));
		
		//int sduSizeInBytes = DEFAULT_SDU_SIZE_IN_BYTES;
		//boolean server = false;
		int sduCount = DEFAULT_SDU_COUNT;
		String difName = DEFAULT_DIF;
		String serverApName = DEFAULT_SERVER_AP_NAME;
		String serverApInstance = DEFAULT_AP_INSTANCE;
		//String clientApName = DEFAULT_CLIENT_AP_NAME;
		//String clientApInstance = DEFAULT_AP_INSTANCE;
		int wait = DEFAULT_WAIT_IN_MS;
//        int rate = DEFAULT_RATE_IN_MBPS;
//        int gap = DEFAULT_MAX_ALLOWABLE_GAP_IN_SDUS;
		
		int i=0;
		while(i<args.length){
			if (args[i].equals(ARGUMENT_SEPARATOR + DIF)){
				difName = args[i+1];
			}else if (args[i].equals(ARGUMENT_SEPARATOR + SAPNAME)){
				serverApName = args[i+1];
			}else if (args[i].equals(ARGUMENT_SEPARATOR + SAPINSTANCE)){
				serverApInstance = args[i+1];
//			}else if (args[i].equals(ARGUMENT_SEPARATOR + CAPNAME)){
//				clientApName = args[i+1];
//			}else if (args[i].equals(ARGUMENT_SEPARATOR + CAPINSTANCE)){
//				clientApInstance = args[i+1];
//			}else if (args[i].equals(ARGUMENT_SEPARATOR + SDUSIZE)){
//				try{
//					sduSizeInBytes = Integer.parseInt(args[i+1]);
//					if (sduSizeInBytes <1){
//						showErrorAndExit(SDUSIZE);
//					}
//				}catch(Exception ex){
//					showErrorAndExit(SDUSIZE);
//				}
			}else if (args[i].equals(ARGUMENT_SEPARATOR + COUNT)){
				try{
					sduCount = Integer.parseInt(args[i+1]);
					if (sduCount <1){
						showErrorAndExit(COUNT);
					}
				}catch(Exception ex){
					showErrorAndExit(COUNT);
				}
			}else if (args[i].equals(ARGUMENT_SEPARATOR + WAIT)){
				try{
					wait = Integer.parseInt(args[i+1]);
					if (wait < 100){
						showErrorAndExit(WAIT);
					}
				}catch(Exception ex){
					showErrorAndExit(WAIT);
				}
//			}else if (args[i].equals(ARGUMENT_SEPARATOR + RATE)){
//				try{
//					rate = Integer.parseInt(args[i+1]);
//					if (rate <= 0){
//						showErrorAndExit(RATE);
//					}
//				}catch(Exception ex){
//					showErrorAndExit(RATE);
//				}
//			}else if (args[i].equals(ARGUMENT_SEPARATOR + GAP)){
//				try{
//					gap = Integer.parseInt(args[i+1]);
//					if (rate <= -1){
//						showErrorAndExit(GAP);
//					}
//				}catch(Exception ex){
//					showErrorAndExit(GAP);
//				}
			}else{
				System.out.println("Wrong argument.\nUsage: "
						+USAGE+"\n"+DEFAULTS);
				System.exit(-1);
			}

			i = i+2;
		}
		
		ApplicationProcessNamingInformation serverAPNamingInfo = 
				new ApplicationProcessNamingInformation(serverApName, serverApInstance);
//		ApplicationProcessNamingInformation clientAPNamingInfo = 
//				new ApplicationProcessNamingInformation(clientApName, clientApInstance);
		
		
		// Create server or client
//		if (server){
			System.err.println("Starting server ..");
			CDAPServer echoServer = new CDAPServer(difName, serverAPNamingInfo, wait);
			echoServer.execute();
//		}else{
			//echoClient = new EchoClient(numberOfSDUs, sduSize, serverNamingInfo, clientNamingInfo, timeout, rate, gap);
			//boolean q, long count, boolean registration, int w, int g, int dw)

//			CDAPEchoClient echoClient = new CDAPEchoClient(difName, serverAPNamingInfo, clientAPNamingInfo, false, sduCount, true, timeout, gap, 1);
//			echoClient.execute();
//		}
	
	}
	
	public static void showErrorAndExit(String parameterName){
		System.out.println("Wrong value for argument "+parameterName+".\nUsage: "
				+USAGE+"\n"+DEFAULTS);
		System.exit(-1);
	}

}
