package com.bill.billeventbus;

/**
 * @author Bill
 *
 */
public class Subscription {

	private final Object subscriber;
	private final SubscriberMethod subscriberMethod;
	/**
	 * Becomes false as soon as {@link EventBus#unregister(Object)} is called, which
	 * is checked by queued event delivery {@link EventBus#invokeSubscriber} to
	 * prevent race conditions.
	 */
	private volatile boolean active;

	Subscription(Object subscriber, SubscriberMethod subscriberMethod) {
		this.subscriber = subscriber;
		this.subscriberMethod = subscriberMethod;
		active = true;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Subscription) {
			Subscription otherSubscriber = (Subscription) other;
			return subscriber == otherSubscriber.subscriber
					&& subscriberMethod.equals(otherSubscriber.subscriberMethod);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return subscriber.hashCode() + subscriberMethod.methodString.hashCode();
	}

	public Object getSubscriber() {
		return subscriber;
	}

	public SubscriberMethod getSubscriberMethod() {
		return subscriberMethod;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
	
	

}
