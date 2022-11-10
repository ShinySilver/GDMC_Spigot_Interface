package com.gdmc.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import spark.Route;

@WebSocket
public class WebsocketHandler {

	private static final HashMap<String, Route> mapper = new HashMap<>();
	private static final Queue<Session> sessions = new ConcurrentLinkedQueue<>();

	static void register(String name, Route route) {
		mapper.put(name, route);
	}

	@OnWebSocketConnect
	public void connected(Session session) {
		sessions.add(session);
	}

	@OnWebSocketClose
	public void closed(Session session, int statusCode, String reason) {
		sessions.remove(session);
	}

	@OnWebSocketMessage
	public void message(Session session, String message) throws IOException {
		session.getRemote().sendString("Pong!");
		// TODO: when receiving a message, go through the register & match it to a route.
		// TODO: Integrate the registering process to EndpointBuilder
	}
}
