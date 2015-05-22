package rina.apps.manager;

import rina.apps.manager.client.CDAPEchoClient;
import rina.apps.manager.server.CDAPServer;
import eu.irati.librina.ApplicationProcessNamingInformation;

public class Echo{
	
	public static final String DATA = "data";
	public static final String CONTROL = "control";
	
	private CDAPServer echoServer = null;
	//private EchoClient echoClient = null;
	private CDAPEchoClient echoClient = null;
	
	public Echo(boolean server, ApplicationProcessNamingInformation serverNamingInfo, 
			ApplicationProcessNamingInformation clientNamingInfo, int numberOfSDUs, int sduSize, 
                    int timeout, int rate, int gap){
		if (server){
			echoServer = new CDAPServer("", serverNamingInfo);
		}else{
			//echoClient = new EchoClient(numberOfSDUs, sduSize, serverNamingInfo, clientNamingInfo, timeout, rate, gap);
			//boolean q, long count, boolean registration, int w, int g, int dw)

			echoClient = new CDAPEchoClient("", serverNamingInfo, clientNamingInfo, false, numberOfSDUs, true, timeout, gap, 1);
		}
	}
	
	public void execute(){
		if (echoServer != null){
			echoServer.execute();
		}else{
			echoClient.execute();
		}
	}
}
