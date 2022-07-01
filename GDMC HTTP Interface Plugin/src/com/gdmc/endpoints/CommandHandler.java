package com.gdmc.endpoints;

import com.gdmc.HttpInterfacePlugin;
import com.gdmc.utils.SyncCommandRunner;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.bukkit.Bukkit;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

public class CommandHandler extends HandlerBase {

	@Override
	public void internalHandle(HttpExchange httpExchange) throws IOException {

		// execute command(s)
		InputStream bodyStream = httpExchange.getRequestBody();
		List<String> commands = new BufferedReader(new InputStreamReader(bodyStream)).lines()
				.filter(a -> a.length() > 0).collect(Collectors.toList());

		SyncCommandRunner r = new SyncCommandRunner();
		for (String command : commands) {
			Bukkit.getScheduler().runTask(HttpInterfacePlugin.getInstance(), new Runnable() {
				@Override
				public void run() {
					r.run(command);
				}
			});
		}

		// headers
		Headers headers = httpExchange.getResponseHeaders();
		headers.add("Content-Type", "text/plain; charset=UTF-8");

		// body
		String responseString = String.join("\n", r.getOutput());
		resolveRequest(httpExchange, responseString);
	}
}