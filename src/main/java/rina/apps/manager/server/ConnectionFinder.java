/**
 * (C) Copyright 2015, Waterford Institute of Technology, PRISTINE.
 */
package rina.apps.manager.server;

/**
 * Interface for finding the correct CDAP connection.
 * 
 * @author mcrotty@tssg.org
 */
public interface ConnectionFinder {
	/**
	 * Find a CDAP connection given the remote application name and instance
	 * @param name Application name
	 * @param instance Application instance
	 * @return CDAPConnection or null
	 */
	CDAPConnection find(String name, String instance);
	
	/**
	 * Find a CDAP connection given the local port id
	 * @param localPort The local port
	 * @return CDAPConnection or null
	 */
	CDAPConnection find(Integer localPort);

	/**
	 * Find the corresponding WS connection.
	 * @return CDAPConnection or null
	 */
	CDAPConnection findManager();
}
