package com.bill.billeventbus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import com.bill.billlogger.SimpleLogger;

/**
 * 
 * EventBus is a central publish/subscribe event system. Events are posted
 * ({@link #post(Object)}) to the bus, which delivers it to subscribers
 * immediately that have a matching handler method for the event type. To
 * receive events, subscribers must register themselves to the bus using
 * {@link #register(Object)}. Once registered, subscribers receive events until
 * {@link #unregister(Object)} is called. Event handling methods must be
 * annotated by {@link Subscribe}, must be public, return nothing (void), and
 * have exactly one parameter(the event).
 *
 * @author Bill
 *
 */
public class EventBus {

	/** logger */
	private static final SimpleLogger logger = new SimpleLogger();
	/** the number of thread pool */
	private static final int THREAD_NUM = 4;

	/** log no subscriber message if event has no subscriber */
	private boolean logNoSubscriberMessages = false;
	/** throw exception if subscriber exception occur */
	private boolean throwSubscriberException = false;
	/** log exception if subscriber exception occur */
	private boolean logSubscriberExceptions = false;
	/** thread pool that executor asynchronous task */
	private ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_NUM);

	/** Event - Subscription */
	private final Map<Class<?>, CopyOnWriteArrayList<Subscription>> subscriptionsByEventType = new ConcurrentHashMap<>();
	/** Subscriber - Event */
	private final Map<Class<?>, List<Class<?>>> eventTypesBySubscriber = new HashMap<>();
	/** synchronous post event */
	private final SyncPoster syncPoster = new SyncPoster(this);
	/** asynchronous post event */
	private final AsyncPoster asyncPoster = new AsyncPoster(this);

	/** default event bus */
	private static volatile EventBus INSTANCE;

	/**
	 * get the default event bus
	 * 
	 * @return
	 */
	public final static EventBus getEventBus() {
		if (INSTANCE == null) {
			synchronized (EventBus.class) {
				if (INSTANCE == null) {
					INSTANCE = new EventBus();
				}
			}
		}
		return INSTANCE;
	}

	/**
	 * can called in the same package,just called by @EventBusBuilder.Do not use it
	 * directly.
	 */
	EventBus() {

	}

	/**
	 * Registers the given subscriber to receive events. Subscribers must call
	 * {@link #unregister(Object)} once they are no longer interested in receiving
	 * events.
	 * <p/>
	 * Subscribers have event handling methods that must be annotated by
	 * {@link Subscribe}. The {@link Subscribe} annotation also allows configuration
	 * like {@link ThreadMode} and priority.
	 */
	public void register(Object subscriber) {
		Class<?> subscriberClass = subscriber.getClass();
		List<SubscriberMethod> subscriberMethods = findSubscriberMethods(subscriberClass);
		synchronized (this) {
			for (SubscriberMethod subscriberMethod : subscriberMethods) {
				subscribe(subscriber, subscriberMethod);
			}
		}
	}

	/**
	 * 
	 * find subscribe method in class
	 * 
	 * @param subscriberClass
	 * @return
	 */
	private List<SubscriberMethod> findSubscriberMethods(Class<?> subscriberClass) {
		ArrayList<SubscriberMethod> methods = new ArrayList<SubscriberMethod>();
		Method[] declaredMethods = subscriberClass.getDeclaredMethods();
		for (Method method : declaredMethods) {
			Subscribe[] annotationsByType = method.getAnnotationsByType(Subscribe.class);
			if (annotationsByType != null && annotationsByType.length > 0) {
				Subscribe subscribe = annotationsByType[0];
				Class<?>[] parameterTypes = method.getParameterTypes();
				if (parameterTypes == null || parameterTypes.length != 1) {
					throw new EventBusException("@Class " + subscriberClass.getName() + "   @Subscribe method "
							+ method.getName() + "must have exactly 1 parameter but has " + parameterTypes.length);
				}
				methods.add(
						new SubscriberMethod(method, parameterTypes[0], subscribe.threadMode(), subscribe.priority()));
			}
		}
		return methods;
	}

	/**
	 * 
	 * @param subscriber
	 * @param subscriberMethod
	 */
	private void subscribe(Object subscriber, SubscriberMethod subscriberMethod) {
		CopyOnWriteArrayList<Subscription> subscriptions = subscriptionsByEventType
				.get(subscriberMethod.getEventType());
		if (subscriptions == null) {
			subscriptions = new CopyOnWriteArrayList<Subscription>();
			CopyOnWriteArrayList<Subscription> putIfAbsent = subscriptionsByEventType
					.putIfAbsent(subscriberMethod.getEventType(), subscriptions);
			if (putIfAbsent != null) {
				subscriptions = putIfAbsent;
			}
		}
		subscriptions.add(new Subscription(subscriber, subscriberMethod));

	}

	/**
	 * Unregisters the given subscriber from all event classes.
	 * 
	 */
	public synchronized void unregister(Object subscriber) {
		List<Class<?>> eventTypes = eventTypesBySubscriber.get(subscriber);
		if (eventTypes != null) {
			for (Class<?> eventType : eventTypes) {
				unsubscribeByEventType(subscriber, eventType);
			}
			eventTypesBySubscriber.remove(subscriber);
		} else {
			logger.log(Level.WARNING, "Subscriber to unregister was not registered before: " + subscriber.getClass());
		}
	}

	/**
	 * Only updates subscriptionsByEventType, not eventTypesBySubscriber! Caller
	 * must update eventTypesBySubscriber.
	 * 
	 * @param subscriber
	 * @param eventType
	 */
	private void unsubscribeByEventType(Object subscriber, Class<?> eventType) {
		List<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
		if (subscriptions != null) {
			int size = subscriptions.size();
			for (int i = 0; i < size; i++) {
				Subscription subscription = subscriptions.get(i);
				if (subscription.getSubscriber() == subscriber) {
					subscription.setActive(false);
					subscriptions.remove(i);
					i--;
					size--;
				}
			}
		}
	}

	/**
	 * @param event
	 * 
	 *              Posts the given event to the event bus.
	 */
	public void post(Object event) throws Error {
		Class<?> eventClass = event.getClass();
		boolean subscriptionFound = postEvent(event, eventClass);
		if (!subscriptionFound) {
			if (logNoSubscriberMessages) {
				logger.log(Level.FINE, "No subscribers registered for event " + eventClass);
			}
		}
	}

	/**
	 * @param event
	 * @param eventClass
	 * @return
	 */
	private boolean postEvent(Object event, Class<?> eventClass) {
		CopyOnWriteArrayList<Subscription> subscriptions;
		synchronized (this) {
			subscriptions = subscriptionsByEventType.get(eventClass);
		}
		if (subscriptions != null && !subscriptions.isEmpty()) {
			for (Subscription subscription : subscriptions) {
				postToSubscription(subscription, event);
			}
			return true;
		}
		return false;
	}

	private void postToSubscription(Subscription subscription, Object event) {
		switch (subscription.getSubscriberMethod().getThreadMode()) {
		case SYNC:
			syncPoster.post(subscription, event);
			break;
		case ASYNC:
			asyncPoster.post(subscription, event);
			break;
		default:
			throw new IllegalStateException(
					"Unknown thread mode: " + subscription.getSubscriberMethod().getThreadMode());
		}
	}

	/**
	 * @param subscription
	 * @param event
	 */
	public void invokeSubscriber(Subscription subscription, Object event) {
		try {
			subscription.getSubscriberMethod().getMethod().invoke(subscription.getSubscriber(), event);
		} catch (InvocationTargetException e) {
			handleSubscriberException(subscription, event, e.getCause());
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Unexpected exception", e);
		}
	}

	private void handleSubscriberException(Subscription subscription, Object event, Throwable cause) {
		if (throwSubscriberException) {
			throw new EventBusException("Invoking subscriber failed", cause);
		}
		if (logSubscriberExceptions) {
			logger.log(Level.SEVERE, "Could not dispatch event: " + event.getClass() + " to subscribing class "
					+ subscription.getSubscriber().getClass(), cause);
		}
	}

	public void invokeAsyncSubscriber(Subscription subscription, Object event) {
		threadPool.submit(new Runnable() {
			public void run() {
				invokeSubscriber(subscription, event);
			}
		});
	}

	public boolean isLogNoSubscriberMessages() {
		return logNoSubscriberMessages;
	}

	public void setLogNoSubscriberMessages(boolean logNoSubscriberMessages) {
		this.logNoSubscriberMessages = logNoSubscriberMessages;
	}

	public boolean isThrowSubscriberException() {
		return throwSubscriberException;
	}

	public void setThrowSubscriberException(boolean throwSubscriberException) {
		this.throwSubscriberException = throwSubscriberException;
	}

	public boolean isLogSubscriberExceptions() {
		return logSubscriberExceptions;
	}

	public void setLogSubscriberExceptions(boolean logSubscriberExceptions) {
		this.logSubscriberExceptions = logSubscriberExceptions;
	}

	public ExecutorService getThreadPool() {
		return threadPool;
	}

	public void setThreadPool(ExecutorService threadPool) {
		this.threadPool = threadPool;
	}

	public Map<Class<?>, List<Class<?>>> getEventTypesBySubscriber() {
		return eventTypesBySubscriber;
	}

}
