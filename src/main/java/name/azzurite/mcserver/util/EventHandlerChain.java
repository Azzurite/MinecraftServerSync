package name.azzurite.mcserver.util;

import java.util.ArrayList;
import java.util.List;

import javafx.event.Event;
import javafx.event.EventHandler;

public class EventHandlerChain<T extends Event> implements EventHandler<T> {

	private final List<EventHandler<T>> eventHandlers = new ArrayList<>();

	@Override
	public void handle(T event) {
		eventHandlers.forEach(handler -> handler.handle(event));
	}

	public void addEventHandler(EventHandler<T> eventHandler) {
		eventHandlers.add(eventHandler);
	}
}
