package com.garganttua.server.core.services;

import java.util.List;

public interface IGGServerServicesManager {

	public List<IGGServerService> getServices();
	
	void stopService(IGGServerService service, GGServerServiceCommandRight right) throws GGServerServiceException;
	
	void startService(IGGServerService service, GGServerServiceCommandRight right) throws GGServerServiceException;
	
	void flushService(IGGServerService service, GGServerServiceCommandRight right) throws GGServerServiceException;

	void restartService(IGGServerService service, GGServerServiceCommandRight right, String[] arguments) throws GGServerServiceException;

	void initService(IGGServerService service, GGServerServiceCommandRight right, String[] arguments) throws GGServerServiceException;

}
