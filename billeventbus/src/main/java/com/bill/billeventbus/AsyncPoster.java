package com.bill.billeventbus;

/**
 * asynchronous post
 * 
 * @author Bill
 *
 */
public class AsyncPoster implements Poster {

	private EventBus eventBus;

	public AsyncPoster(EventBus eventBus) {
		this.eventBus = eventBus;
	}

	@Override
	public void post(Subscription subscription, Object event) {
		eventBus.invokeAsyncSubscriber(subscription, event);
	}

}
