package com.bill.billeventbus;

/**
 * @author Bill
 *
 */
public interface Poster {

	 /**
     * an event to be posted for a particular subscription.
     *
     * @param subscription subscription which will receive the event.
     * @param event        Event that will be posted to subscribers.
     */
	void post(Subscription subscription, Object event);
}
