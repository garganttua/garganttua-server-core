package com.gtech.pegasus.core.exceptions;

public class PGApplicationEngineException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1165775789486081660L;

	public PGApplicationEngineException(String string) {
		super(string);
	}

	public PGApplicationEngineException(Exception e) {
		super(e);
	}

}
