package com.garganttua.server.core.exceptions;

public class GGServerApplicationException extends Exception {

	public GGServerApplicationException(String string) {
		super(string);
	}

	public GGServerApplicationException(String string, Exception e) {
		super(string, e);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -1752613108762077709L;

}
