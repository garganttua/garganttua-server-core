package com.garganttua.server.core.services;

public class GGServerServiceException extends Exception {

	public GGServerServiceException(GGServerServiceExceptionLabels label) {
		super( label.toString() );
	}

	public GGServerServiceException(Exception e) {
		super(e);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8802792679307062653L;

}
