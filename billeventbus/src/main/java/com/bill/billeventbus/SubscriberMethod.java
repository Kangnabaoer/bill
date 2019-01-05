package com.bill.billeventbus;

import java.lang.reflect.Method;

/**
 * the encapsulation of the subscribe method
 * 
 * @author Bill
 *
 */
public class SubscriberMethod {

	private final Method method;
	private final ThreadMode threadMode;
	private final Class<?> eventType;
	private final int priority;
	/** Used for efficient comparison */
	String methodString;

	public SubscriberMethod(Method method, Class<?> eventType, ThreadMode threadMode, int priority) {
		this.method = method;
		this.threadMode = threadMode;
		this.eventType = eventType;
		this.priority = priority;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		} else if (other instanceof SubscriberMethod) {
			checkMethodString();
			SubscriberMethod otherSubscriberMethod = (SubscriberMethod) other;
			otherSubscriberMethod.checkMethodString();
			// Don't use method.equals
			return methodString.equals(otherSubscriberMethod.methodString);
		} else {
			return false;
		}
	}

	private synchronized void checkMethodString() {
		if (methodString == null) {
			// Method.toString has more overhead, just take relevant parts of the method
			StringBuilder builder = new StringBuilder(64);
			builder.append(method.getDeclaringClass().getName());
			builder.append('#').append(method.getName());
			builder.append('(').append(eventType.getName());
			methodString = builder.toString();
		}
	}

	@Override
	public int hashCode() {
		return method.hashCode();
	}

	public Method getMethod() {
		return method;
	}

	public ThreadMode getThreadMode() {
		return threadMode;
	}

	public Class<?> getEventType() {
		return eventType;
	}

	public int getPriority() {
		return priority;
	}

	public String getMethodString() {
		return methodString;
	}

}
