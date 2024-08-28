package com.ericsson.eniq.scheduler.exception;


public class SchedulerException extends Exception{
	
	private static final long serialVersionUID = 1L;
	
	public SchedulerException(final String message, final Exception e) {
		super(message, e);
	}

	public SchedulerException(final Throwable cause) {
		super(cause);
	}

	public SchedulerException(final String message) {
		super(message);
	}
	
	

}
