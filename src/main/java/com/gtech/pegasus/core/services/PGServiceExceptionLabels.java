package com.gtech.pegasus.core.services;

public enum PGServiceExceptionLabels {
	
	UNAUTHORIZED("unauthorized command"), SERVICE_NOT_RUNNING("service not running"), SERVICE_NOT_INITIALIZED("service not initialized"), SERVICE_NOT_FLUSHED("service not flushed"), SERVICE_NOT_STOPPED("service not stopped");

	private String label;

	PGServiceExceptionLabels(String string) {
		this.label = string;
	}
	
	@Override
	public String toString() {
		return this.label;
	}

}
