package com.gdmc;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.fastasyncworldedit.core.Fawe;
import com.gdmc.api.EndpointBuilder;
import com.gdmc.commands.BuildAreaCommand;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;

import spark.Spark;
import spark.routematch.RouteMatch;

public class HttpInterfacePlugin extends JavaPlugin implements Listener {
	private static HttpInterfacePlugin instance;
	private static EditSession session;
	private static LinkedList<EditSession> history;
	private static LinkedList<String> operationNames;
	private static World selectedWorld;
	
	/**
	 * TODO: More end points
	 * TODO: Batched changes
	 * TODO: Better history + undo
	 * TODO: Better list commands
	 * TODO: Schematics
	 */

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onEnable() {
		// Initializing stuff
		instance = this;
		history = new LinkedList<>();
		operationNames = new LinkedList<>();
		selectedWorld = Bukkit.getWorlds().get(0); // this defaults to the overworld

		// Setting up plugin commands. Currently unused.
		PluginCommand command = this.getCommand("set-build-area");
		BuildAreaCommand c = new BuildAreaCommand();
		command.setExecutor(c);
		command.setTabCompleter(c);

		// Setting up Spark
		getLogger().info("Starting Spark...");
		Spark.initExceptionHandler((e) -> {
			getLogger().warning("Could not start Spark. Plugin shutting down.");
			Bukkit.getPluginManager().disablePlugin(this);
		});
		Spark.webSocketIdleTimeoutMillis(1000 * 60 * 10);
		Spark.init();
		Spark.awaitInitialization();
		getLogger().info("Spark started! Plugin enabled.");

		// Setting up all the endpoints
		getLogger().info("Creating the endpoints...");

		/**
		 * Select the world we are working with
		 */
		Spark.put("/world", (req, res) -> {
			res.type("text");
			synchronized (selectedWorld) {
				selectedWorld = Bukkit.getWorld(req.params("worldName"));
				if (selectedWorld != null) {
					session = WorldEdit.getInstance().newEditSession(new BukkitWorld(selectedWorld));
					return "World set to world \"" + req.params("worldName") + "\" of type \""
							+ selectedWorld.getEnvironment().name() + "\".";
				} else {
					res.status(400);
					return "No world with the name \"" + req.params("worldName") + "\" found. Available worlds: "
							+ Arrays.toString(Bukkit.getWorlds().toArray()) + ".";
				}
			}
		});

		/**
		 * Drain a water or lava lake
		 */
		new EndpointBuilder("/drain").addInt("x").addInt("y").addInt("z").addOptionalInt("radius", "20")
				.post((res, input) -> {
					int affectedBlocks;
					try {
						affectedBlocks = session.drainArea(
								BlockVector3.at((int) input[0], (int) input[1], (int) input[2]), (int) input[3], true,
								true);
					} catch (MaxChangedBlocksException e) {
						res.status(500);
						return "Reached the configured maximum amount of blocks modifiable in a single request. "
								+ "This request could thus not be processed. Are you trying to drain an ocean?";
					}
					res.status(200);
					return affectedBlocks + " blocks were affected.";
				});

		/**
		 * Get the biome at a given position
		 */
		new EndpointBuilder("/biome").addInt("x").addInt("y").addInt("z").get((res, input) -> {
			res.status(200);
			return session.getBiome(BlockVector3.at((int) input[0], (int) input[1], (int) input[2])).toString();
		});

		/**
		 * Get the block at a given position
		 */
		new EndpointBuilder("/block").addInt("x").addInt("y").addInt("z").get((res, input) -> {
			res.status(200);
			return session.getBlock(BlockVector3.at((int) input[0], (int) input[1], (int) input[2])).toString();
		});

		/**
		 * Set the block at a given position
		 */
		new EndpointBuilder("/block").addInt("x").addInt("y").addInt("z").addBlock("block").put((res, input) -> {
			session.setBlock((int) input[0], (int) input[1], (int) input[2], (BlockState) input[3]);
			res.status(200);
			return "Done.";
		});

		/**
		 * Apply all changes for them to be applied as soon as possible by FAWE. Also
		 * add them to the history.
		 */
		new EndpointBuilder("/commit").addOptionalString("commit_name", "Unnamed_Commit").post((res, input) -> {
			session.flushQueue();
			history.add(session);
			operationNames.add((String) input[0]);
			synchronized (selectedWorld) {
				session = WorldEdit.getInstance().newEditSession(new BukkitWorld(selectedWorld));
			}
			res.status(200);
			return "Done.";
		});

		/**
		 * Wait for FAWE to finish applying all changes
		 */
		new EndpointBuilder("/sync").addOptionalInt("timeout", "300").post((res, input) -> {
			Lock lock = new ReentrantLock();
			lock.lock();
			
			int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(instance, new Runnable() {
				@Override
				public void run() {
					for(BukkitTask t: Bukkit.getScheduler().getPendingTasks()) {
						if(t.getOwner()==Fawe.instance() || t.getOwner() == WorldEditPlugin.getInstance()) {
							return;
						}
					}
					lock.unlock();
				}
			}, 0, 1);
			
			try {
				lock.tryLock((int) input[0], TimeUnit.SECONDS);
			} catch (InterruptedException e1) {
				Bukkit.getScheduler().cancelTask(taskId);
				res.status(400);
				return "Sync timed out after "+(int) input[0]+" seconds.";
			}
			
			Bukkit.getScheduler().cancelTask(taskId);
			res.status(200);
			return "Done.";
		});

		/**
		 * Apply all changes for them to be applied as soon as possible by FAWE. Also
		 * add them to the history.
		 */
		new EndpointBuilder("/history").addOptionalString("commit_name", "Unnamed_Commit").get((res, input) -> {
			if (history.isEmpty()) {
				res.status(200);
				return "No operations recorded.";
			}
			String output = "";
			for (int i = 0; i < history.size(); i++) {
				if (i != 0) {
					output += "<br>";
				}
				output += "id=" + i + ", commit_name=" + operationNames.get(i) + ", block_changed="
						+ history.get(i).getBlockChangeCount();
			}
			res.status(200);
			return output;
		});

		/**
		 * List all available commands
		 */
		Spark.get("/list", (req, res) -> {
			res.status(200);
			return "Available routes: "
					+ Spark.routes().stream().map(RouteMatch::getMatchUri).collect(Collectors.joining(", "));
		});

		getLogger().info("Endpoints created!");
		getLogger().info("Available routes: "
				+ Spark.routes().stream().map(RouteMatch::getMatchUri).collect(Collectors.joining(", ")));

		/**
		 * Scheduling the session initialization next tick. TODO: add an initialization
		 * check to all the methods in the api OR add WE as a dependancy
		 */
		Bukkit.getScheduler().runTask(this, new Runnable() {
			@Override
			public void run() {
				session = WorldEdit.getInstance().newEditSession(new BukkitWorld(selectedWorld));
			}
		});
	}

	@Override
	public void onDisable() {
		Spark.stop();
		getLogger().info("Awaiting Spark stop...");
		Spark.awaitStop();
		getLogger().info("Spark stopped! Plugin disabled.");
		instance = null;
	}

	@Nullable
	public static HttpInterfacePlugin getInstance() {
		return instance;
	}
}
