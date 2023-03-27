package com.gtech.pegasus.core.services;

public interface IPGService {

	public void restart(PGServiceCommandRight right, String[] arguments) throws PGServiceException;
	
	public void start(PGServiceCommandRight right) throws PGServiceException;
	
	public void stop(PGServiceCommandRight right) throws PGServiceException;
	
	public void init(PGServiceCommandRight right, String[] arguments) throws PGServiceException;
	
	public void flush(PGServiceCommandRight right) throws PGServiceException;

	public String getName();
	
	public PGServiceStatus getStatus();

	public PGServicePriority getPriority();

}
