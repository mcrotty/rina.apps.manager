package rina.utils.apps.echo;

import rina.utils.apps.echo.client.CDAPEchoClient;
import rina.utils.apps.echo.client.EchoClient;
import rina.utils.apps.echo.server.EchoServer;
import eu.irati.librina.ApplicationProcessNamingInformation;

public class Echo{
	
	public static final String DATA = "data";
	public static final String CONTROL = "control";
	
	private EchoServer echoServer = null;
	//private EchoClient echoClient = null;
	private CDAPEchoClient echoClient = null;
	
	public Echo(boolean server, ApplicationProcessNamingInformation serverNamingInfo, 
			ApplicationProcessNamingInformation clientNamingInfo, int numberOfSDUs, int sduSize, 
                    int timeout, int rate, int gap){
		if (server){
			echoServer = new EchoServer(serverNamingInfo);
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
