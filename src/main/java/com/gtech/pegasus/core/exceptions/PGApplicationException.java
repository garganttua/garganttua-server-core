package com.gtech.pegasus.core.exceptions;

public class PGApplicationException extends Exception {

	public PGApplicationException(String string) {
		super(string);
	}

	public PGApplicationException(String string, Exception e) {
		super(string, e);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -1752613108762077709L;

}
