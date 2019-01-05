package com.bill.billeventbus;

import java.util.concurrent.ExecutorService;

/**
 * 
 * @author Bill
 *
 */
public class EventBusBuilder {

	public EventBusBuilder() {

	}

	public EventBus getEventBus() {
		return EventBus.getEventBus();
	}

	public EventBus buildExecutor(ExecutorService pool) {
		EventBus eventBus = new EventBus();
		eventBus.setThreadPool(pool);
		return eventBus;
	}

}
