package com.garganttua.server.core.services;

public interface IGGServerService {

	public void restart(GGServerServiceCommandRight right, String[] arguments) throws GGServerServiceException;
	
	public void start(GGServerServiceCommandRight right) throws GGServerServiceException;
	
	public void stop(GGServerServiceCommandRight right) throws GGServerServiceException;
	
	public void init(GGServerServiceCommandRight right, String[] arguments) throws GGServerServiceException;
	
	public void flush(GGServerServiceCommandRight right) throws GGServerServiceException;

	public String getName();
	
	public GGServerServiceStatus getStatus();

	public GGServerServicePriority getPriority();

}
