package com.bill.billeventbus;

/**
 * @author Bill
 *
 */
public class EventBusException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public EventBusException(String message) {
		super(message);
	}

	public EventBusException(String message, Throwable cause) {
		super(message, cause);
	}

	public EventBusException(Throwable cause) {
		super(cause);
	}

}
