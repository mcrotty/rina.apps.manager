/**
 * (C) Copyright 2015, Waterford Institute of Technology, PRISTINE.
 */
package rina.apps.manager.server;

import eu.ict_pristine.wp5.dms.es.taxonomy.Event_KeyTransformer;
import eu.ict_pristine.wp5.dms.esdsls.cdap.v100.CDAP_ValueKeys;

/**
 * Adds some extra semantics for converting CDAP keys into the events keys
 * 
 * @author mcrotty@tssg.org
 */
public class CDAPKeyTransformer extends Event_KeyTransformer {

	@Override
	public String secondaryTransform(Object key) {
		String ret = null;
		if(key instanceof CDAP_ValueKeys){
			ret = ((CDAP_ValueKeys) key).getName();
		}
		return ret;
	}
	
}
