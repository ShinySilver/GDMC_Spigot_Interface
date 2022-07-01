package com.gdmc.endpoints;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class HandlerBase implements HttpHandler {

	@SuppressWarnings("serial")
	public static class HttpException extends RuntimeException {
		public String message;
		public int statusCode;

		public HttpException(String message, int statusCode) {
			this.message = message;
			this.statusCode = statusCode;
		}
	}

	@Override
	public void handle(HttpExchange httpExchange) throws IOException {
		try {
			internalHandle(httpExchange);
		} catch (HttpException e) {
			String responseString = e.message;
			byte[] responseBytes = responseString.getBytes(StandardCharsets.UTF_8);
			Headers headers = httpExchange.getResponseHeaders();
			headers.set("Content-Type", "text/plain; charset=UTF-8");

			httpExchange.sendResponseHeaders(e.statusCode, responseBytes.length);
			OutputStream outputStream = httpExchange.getResponseBody();
			outputStream.write(responseBytes);
			outputStream.close();

		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String stackTrace = sw.toString();

			String responseString = String.format("Internal server error: %s\n%s", e.toString(), stackTrace);
			byte[] responseBytes = responseString.getBytes(StandardCharsets.UTF_8);
			Headers headers = httpExchange.getResponseHeaders();
			headers.set("Content-Type", "text/plain; charset=UTF-8");

			httpExchange.sendResponseHeaders(500, responseBytes.length);
			OutputStream outputStream = httpExchange.getResponseBody();
			outputStream.write(responseBytes);
			outputStream.close();

			Bukkit.getLogger().severe(responseString);
			throw e;
		}
	}

	protected abstract void internalHandle(HttpExchange httpExchange) throws IOException;

	protected static void addDefaultHeaders(Headers headers) {
		headers.add("Access-Control-Allow-Origin", "*");
		headers.add("Content-Disposition", "inline");
	}

	protected static void resolveRequest(HttpExchange httpExchange, String responseString) throws IOException {
		byte[] responseBytes = responseString.getBytes(StandardCharsets.UTF_8);
		resolveRequest(httpExchange, responseBytes);
	}

	protected static void resolveRequest(HttpExchange httpExchange, byte[] responseBytes) throws IOException {
		httpExchange.sendResponseHeaders(200, responseBytes.length);
		OutputStream outputStream = httpExchange.getResponseBody();
		outputStream.write(responseBytes);
		outputStream.close();
	}

	protected static String getHeader(Headers headers, String key, String defaultValue) {
		List<String> list = headers.get(key);
		if (list == null || list.size() == 0)
			return defaultValue;
		else
			return list.get(0);
	}

	protected static Map<String, String> parseQueryString(String qs) {
		Map<String, String> result = new HashMap<>();
		if (qs == null)
			return result;

		int last = 0, next, l = qs.length();
		while (last < l) {
			next = qs.indexOf('&', last);
			if (next == -1)
				next = l;

			if (next > last) {
				int eqPos = qs.indexOf('=', last);
				try {
					if (eqPos < 0 || eqPos > next)
						result.put(URLDecoder.decode(qs.substring(last, next), "utf-8"), "");
					else
						result.put(URLDecoder.decode(qs.substring(last, eqPos), "utf-8"),
								URLDecoder.decode(qs.substring(eqPos + 1, next), "utf-8"));
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e); // will never happen, utf-8 support is mandatory for java
				}
			}
			last = next + 1;
		}
		return result;
	}
}
