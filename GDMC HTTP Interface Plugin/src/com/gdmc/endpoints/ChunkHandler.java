package com.gdmc.endpoints;

import com.gdmc.HttpInterfacePlugin;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.NBTTagList;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R3.CraftChunk;

public class ChunkHandler extends HandlerBase {
	public ChunkHandler() {
	}

	@Override
	public void internalHandle(HttpExchange httpExchange) throws IOException {

		// query parameters
		Map<String, String> queryParams = parseQueryString(httpExchange.getRequestURI().getRawQuery());

		int chunkX;
		int chunkZ;
		int chunkDX;
		int chunkDZ;
		try {
			chunkX = Integer.parseInt(queryParams.getOrDefault("x", "0"));
			chunkZ = Integer.parseInt(queryParams.getOrDefault("z", "0"));
			chunkDX = Integer.parseInt(queryParams.getOrDefault("dx", "1"));
			chunkDZ = Integer.parseInt(queryParams.getOrDefault("dz", "1"));
		} catch (NumberFormatException e) {
			String message = "Could not parse query parameter: " + e.getMessage();
			throw new HandlerBase.HttpException(message, 400);
		}
		World w = Bukkit.getWorlds().get(0);

		String method = httpExchange.getRequestMethod().toLowerCase();
		if (!method.equals("get")) {
			throw new HandlerBase.HttpException("Method not allowed. Only GET requests are supported.", 405);
		}

		// with this header we return pure NBT binary
		// if content type is application/json use that otherwise return text
		Headers requestHeaders = httpExchange.getRequestHeaders();
		String contentType = getHeader(requestHeaders, "Accept", "*/*");
		boolean RETURN_TEXT = !contentType.equals("application/octet-stream");

		Semaphore l = new Semaphore(0);
		NBTTagList chunkList = new NBTTagList();
		Bukkit.getScheduler().runTask(HttpInterfacePlugin.getInstance(), new Runnable() {
			@Override
			public void run() {
				try {
					for (int z = chunkZ; z < chunkZ + chunkDZ; z++)
						for (int x = chunkX; x < chunkX + chunkDX; x++) {
							NBTTagCompound chunkNBT = ((CraftChunk) w.getChunkAt(x, z)).getHandle().p().b();
							chunkList.add(chunkNBT);
						}
				} finally {
					l.release();
				}
			}
		});

		// block this thread until the above code has run on the main thread
		try {
			l.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		NBTTagCompound bodyNBT = new NBTTagCompound();
		bodyNBT.set("Chunks", chunkList);
		bodyNBT.setInt("ChunkX", chunkX);
		bodyNBT.setInt("ChunkZ", chunkZ);
		bodyNBT.setInt("ChunkDX", chunkDX);
		bodyNBT.setInt("ChunkDZ", chunkDZ);

		// headers and body
		Headers headers = httpExchange.getResponseHeaders();

		if (RETURN_TEXT) {
			headers.add("Content-Type", "text/plain; charset=UTF-8");
			String responseString = bodyNBT.toString();

			resolveRequest(httpExchange, responseString);
		} else {
			headers.add("Content-Type", "application/octet-stream");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);

			NBTTagCompound containterNBT = new NBTTagCompound();
			containterNBT.set("file", bodyNBT);
			containterNBT.write(dos);
			dos.flush();
			byte[] responseBytes = baos.toByteArray();

			resolveRequest(httpExchange, responseBytes);
		}
	}
}