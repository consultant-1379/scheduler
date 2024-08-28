package com.distocraft.dc5000.etl.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.distocraft.dc5000.common.ServicenamesHelper;
import com.distocraft.dc5000.common.ServicenamesHelper.ServiceHostDetails;


class EngineConnector {
	
	private static Map<String, String> engineUrls = new HashMap<>();
	private static List<String> engines = new ArrayList<>();
	private static String lastServedEngine;
	private static final String LOCAL_HOST = "localhost";
	
	
	
	private EngineConnector() {
		
	}
	
	static void init(Properties appProps, Logger log) {
		Map<String, ServiceHostDetails> enginesFrmServiceNames = null;
		try {
			enginesFrmServiceNames = ServicenamesHelper.getEngines(log);
		} catch (final Exception e) {
			log.log(Level.WARNING, "not able to get engines from Servicenames",e);
		}
		if (enginesFrmServiceNames != null) {
			enginesFrmServiceNames.forEach((key, value) -> {
				engineUrls.put(key, getEngineUrl(value, appProps, log, false));
				engines.add(key);
			});
		} else {
			engineUrls.put(LOCAL_HOST, getEngineUrl(null, appProps, log, true));
			engines.add(LOCAL_HOST);
		}
		

	}
	
	private static String getEngineUrl(ServiceHostDetails serviceHostDetails, Properties appProps,
			Logger log, boolean localHostFlag) {
		String engHostNameByServiceName;
		if (!localHostFlag) {
			engHostNameByServiceName = serviceHostDetails.getServiceHostname();
		} else {
			engHostNameByServiceName = LOCAL_HOST;
		}
		return getEngineUrl(appProps, engHostNameByServiceName, log);
	}
	
	private static String getEngineUrl(Properties appProps,
			String engHostNameByServiceName, Logger log) {
		
		String engineServerHostName = appProps.getProperty("ENGINE_HOSTNAME", engHostNameByServiceName);
		int engineServerPort = 1200;
		final String engineSporttmp = appProps.getProperty("ENGINE_PORT", "1200");
		try {
			engineServerPort = Integer.parseInt(engineSporttmp);
		} catch (NumberFormatException nfe) {
			log.config("Using default ENGINE_PORT 1200.");
		}
		String engineServerRefName = appProps.getProperty("ENGINE_REFNAME", null);
		if (engineServerRefName == null) {
			log.config("Using default ENGINE_REFNAME \"TransferEngine\"");
			engineServerRefName = "TransferEngine";
		}
		String engineURL = "rmi://" + engineServerHostName + ":" + engineServerPort + "/" + engineServerRefName;
		log.config("Engine RMI Reference for " + engineServerHostName + ": " + engineURL);
		return engineURL;
	}
	
	/**
	 * Method that returns the engine based on availability and utilization 
	 * @return
	 */
	static String getAvailableEngine() {
		return engineUrls.get(getByRoundRobin());
	}
	
	private static String getByRoundRobin() {
		String engine;
		if(lastServedEngine == null || engines.size() == 1 
				|| engines.indexOf(lastServedEngine)== engines.size()-1) {
			engine = engines.get(0);
		} else {
			engine = engines.get(engines.indexOf(lastServedEngine)+1);
		}
		lastServedEngine = engine;
		return engine;
	}
}
