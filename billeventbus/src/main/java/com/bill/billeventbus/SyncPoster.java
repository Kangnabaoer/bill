package com.bill.billeventbus;

/**
 * synchronous to post the event
 * @author Bill
 *
 */
public class SyncPoster implements Poster {

	private EventBus eventBus;

	public SyncPoster(EventBus eventBus) {
		this.eventBus = eventBus;
	}

	@Override
	public void post(Subscription subscription, Object event) {
		eventBus.invokeSubscriber(subscription, event);
	}

}
