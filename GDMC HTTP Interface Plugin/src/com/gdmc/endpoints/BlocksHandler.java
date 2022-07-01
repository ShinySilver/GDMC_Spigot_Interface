package com.gdmc.endpoints;

import com.gdmc.HttpInterfacePlugin;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_16_R3.block.data.CraftBlockData;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

public class BlocksHandler extends HandlerBase {

	private static final int BLOCK_PLACEMENT_BATCH_SIZE = 20000;

	@Override
	public void internalHandle(HttpExchange httpExchange) throws IOException {

		// query parameters
		Map<String, String> queryParams = parseQueryString(httpExchange.getRequestURI().getRawQuery());
		int x;
		int y;
		int z;
		boolean includeState;
		boolean doBlockUpdates;
		boolean spawnDrops;
		int customFlags; // -1 == no custom flags

		try {
			x = Integer.parseInt(queryParams.getOrDefault("x", "0"));
			y = Integer.parseInt(queryParams.getOrDefault("y", "0"));
			z = Integer.parseInt(queryParams.getOrDefault("z", "0"));

			includeState = Boolean.parseBoolean(queryParams.getOrDefault("includeState", "false"));

			doBlockUpdates = Boolean.parseBoolean(queryParams.getOrDefault("doBlockUpdates", "true"));
			spawnDrops = Boolean.parseBoolean(queryParams.getOrDefault("spawnDrops", "false"));
			customFlags = Integer.parseInt(queryParams.getOrDefault("customFlags", "-1"), 2);
		} catch (NumberFormatException e) {
			String message = "Could not parse query parameter: " + e.getMessage();
			throw new HandlerBase.HttpException(message, 400);
		}

		// if content type is application/json use that otherwise return text
		Headers reqestHeaders = httpExchange.getRequestHeaders();
		String contentType = getHeader(reqestHeaders, "Accept", "*/*");
		boolean returnJson = contentType.equals("application/json") || contentType.equals("text/json");

		// construct response
		String method = httpExchange.getRequestMethod().toLowerCase();
		String responseString;

		// utility values
		@SuppressWarnings("unused")
		int blockFlags = customFlags >= 0 ? customFlags : getBlockFlags(doBlockUpdates, spawnDrops);
		World w = Bukkit.getWorlds().get(0);

		// processed values
		InputStreamReader reader = new InputStreamReader(httpExchange.getRequestBody());
		List<String> body = new BufferedReader(reader).lines().collect(Collectors.toList());
		CraftBlockData[] blockData = new CraftBlockData[body.size()];
		int[] blockCoordinates = new int[3 * body.size()];

		if (method.equals("put")) {
			// Doing preprocessing to limit the impact on the main thread - we wouldn't want
			// the tps to drop, right?
			for (int i = 0; i < body.size(); i++) {
				try {
					String[] entries = body.get(i).split(" ");
					if (entries.length == 4) {
						if (entries[0].startsWith("~")) {
							blockCoordinates[i * 3] = x + Integer.parseInt(entries[0].substring(1));
						} else {
							blockCoordinates[i * 3] = Integer.parseInt(entries[0]);
						}
						if (entries[1].startsWith("~")) {

							blockCoordinates[i * 3 + 1] = y + Integer.parseInt(entries[1].substring(1));
						} else {
							blockCoordinates[i * 3 + 1] = Integer.parseInt(entries[1]);
						}
						if (entries[2].startsWith("~")) {
							blockCoordinates[i * 3 + 2] = z + Integer.parseInt(entries[2].substring(1));
						} else {
							blockCoordinates[i * 3 + 2] = Integer.parseInt(entries[2]);
						}
					} else if (entries.length == 1) {
						blockCoordinates[i * 3] = x;
						blockCoordinates[i * 3 + 1] = y;
						blockCoordinates[i * 3 + 2] = z;
					} else {
						String message = "Invalid syntax - a single body line should contains either '<block>' or '<x> <y> <z> <block>'.";
						throw new HandlerBase.HttpException(message, 400);
					}
					if (entries[entries.length - 1].contains("[")) {
						String materialName = entries[entries.length - 1].split("[")[0];
						if (materialName.startsWith("minecraft:")) {
							materialName = materialName.substring(10);
						}
						Material m;
						try {
							m = Material.valueOf(materialName.toUpperCase());
						} catch (Exception e) {
							String message = "Unknown material \"minecraft:" + materialName.toLowerCase() + "\".";
							throw new HandlerBase.HttpException(message, 400);
						}
						String data = String.join("[",
								Arrays.asList(entries[entries.length - 1].split("\\[")).subList(1, entries.length));
						blockData[i] = CraftBlockData.newData(m, data);
					} else {
						String materialName = entries[entries.length - 1].split("\\[")[0];
						if (materialName.startsWith("minecraft:")) {
							materialName = materialName.substring(10);
						}
						Material m;
						try {
							m = Material.valueOf(materialName.toUpperCase());
						} catch (Exception e) {
							String message = "Unknown material \"minecraft:" + materialName.toLowerCase() + "\".";
							throw new HandlerBase.HttpException(message, 400);
						}
						blockData[i] = CraftBlockData.newData(m, null);
					}
				} catch (HttpException e) {
					throw e;
				} catch (Exception e) {
					e.printStackTrace();
					String message = "GDMC HTTP Interface Plugin encountered a bug. Look for it in the server log &"
							+ " eventually report it!";
					throw new HandlerBase.HttpException(message, 500);
				}
			}

			// Now asking the scheduler to actually place the blocks from the main thread,
			// one batch per tick.
			Semaphore s = new Semaphore(0);
			int tokenCount = 0;
			for (int i = 0; i < body.size(); i += BLOCK_PLACEMENT_BATCH_SIZE) {
				int iFinal = i;
				tokenCount += 1;
				Bukkit.getScheduler().runTaskLater(HttpInterfacePlugin.getInstance(), new Runnable() {
					@Override
					public void run() {
						try {
							for (int j = 0; j < BLOCK_PLACEMENT_BATCH_SIZE
									&& iFinal * BLOCK_PLACEMENT_BATCH_SIZE + j < body.size(); j++) {
								if (blockData[iFinal * BLOCK_PLACEMENT_BATCH_SIZE + j] != null)
									w.getBlockAt(blockCoordinates[(iFinal * BLOCK_PLACEMENT_BATCH_SIZE + j) * 3],
											blockCoordinates[(iFinal * BLOCK_PLACEMENT_BATCH_SIZE + j) * 3 + 1],
											blockCoordinates[(iFinal * BLOCK_PLACEMENT_BATCH_SIZE + j) * 3 + 2])
											.setBlockData(blockData[iFinal * BLOCK_PLACEMENT_BATCH_SIZE + j]);
							}
						} finally {
							s.release();
						}
					}
				}, i);
			}
			try {
				s.acquire(tokenCount);
			} catch (InterruptedException e) {
				e.printStackTrace();
				String message = "GDMC HTTP Interface Plugin encountered a bug. Look for it in the server log &"
						+ " eventually report it!";
				throw new HandlerBase.HttpException(message, 500);
			}

			// If we reach this line, it means the operation was a success. response string
			// to "Done"; response code to 200.
			responseString = "Done";

		} else if (method.equals("get")) {

			// TODO: Add batching to get

			Block b = w.getBlockAt(x, y, z);
			if (includeState) {
				responseString = getBlockWithState(b, returnJson);
			} else {
				responseString = getBlock(b, returnJson) + "";
			}
		} else {
			throw new HandlerBase.HttpException("Method not allowed. Only PUT and GET requests are supported.", 405);
		}

		// Sending back the response code 200 with the supplied responseString as a message.
		Headers headers = httpExchange.getResponseHeaders();
		addDefaultHeaders(headers);
		if (returnJson) {
			headers.add("Content-Type", "application/json; charset=UTF-8");
		} else {
			headers.add("Content-Type", "text/plain; charset=UTF-8");
		}
		resolveRequest(httpExchange, responseString);
	}

	public int getBlockFlags(boolean doBlockUpdates, boolean spawnDrops) {
		/*
		 * flags: 1 will cause a block update. 2 will send the change to clients. 4 will
		 * prevent the block from being re-rendered. 8 will force any re-renders to run
		 * on the main thread instead 16 will prevent neighbor reactions (e.g. fences
		 * connecting, observers pulsing). 32 will prevent neighbor reactions from
		 * spawning drops. 64 will signify the block is being moved.
		 */
		// construct flags
		return 2 | (doBlockUpdates ? 1 : (32 | 16)) | (spawnDrops ? 0 : 32);
	}

	private String getBlock(Block block, boolean returnJson) {

		String str;
		if (returnJson) {
			JsonObject json = new JsonObject();
			json.add("id", new JsonPrimitive("minecraft:" + block.getType().toString().toLowerCase()));
			str = new Gson().toJson(json);
		} else {
			str = "minecraft:" + block.getType().toString().toLowerCase();
		}
		return str;
	}

	private String getBlockWithState(Block block, boolean returnJson) {

		String str;
		if (returnJson) {
			JsonObject json = new JsonObject();
			json.add("id", new JsonPrimitive("minecraft:" + block.getType().toString().toLowerCase()));
			List<String> array = Arrays.asList(((CraftBlockData) block.getBlockData()).getAsString().split("["));
			if (array.size() > 1) {
				try {
					json.add("state", new JsonParser().parse(String.join("[", array.subList(1, array.size()))));
				} catch (JsonSyntaxException e) {
					Bukkit.getLogger()
							.severe("[GDMC HTTP Interface Plugin] Could not parse the following block state to json: \""
									+ String.join("[", array.subList(1, array.size())) + '"');
				}
			}
			str = new Gson().toJson(json);
		} else {
			str = "minecraft:" + ((CraftBlockData) block.getBlockData()).getAsString();
		}
		return str;
	}
}
