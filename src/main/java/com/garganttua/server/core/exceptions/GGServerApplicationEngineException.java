package com.garganttua.server.core.exceptions;

public class GGServerApplicationEngineException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1165775789486081660L;

	public GGServerApplicationEngineException(String string) {
		super(string);
	}

	public GGServerApplicationEngineException(Exception e) {
		super(e);
	}

}
