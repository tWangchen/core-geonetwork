package org.fao.geonet.exceptions;

public class BatchEditException extends Exception{

	 private static final long serialVersionUID = 1L;

	    public BatchEditException(String message) {
	        super(message);
	    }

	    public BatchEditException(String message, Throwable cause) {
	        super(message, cause);
	    }
}
