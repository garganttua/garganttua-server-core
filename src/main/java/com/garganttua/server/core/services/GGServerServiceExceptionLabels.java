package com.garganttua.server.core.services;

public enum GGServerServiceExceptionLabels {
	
	UNAUTHORIZED("unauthorized command"), SERVICE_NOT_RUNNING("service not running"), SERVICE_NOT_INITIALIZED("service not initialized"), SERVICE_NOT_FLUSHED("service not flushed"), SERVICE_NOT_STOPPED("service not stopped");

	private String label;

	GGServerServiceExceptionLabels(String string) {
		this.label = string;
	}
	
	@Override
	public String toString() {
		return this.label;
	}

}
