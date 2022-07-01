package com.gdmc;

import java.io.IOException;

import javax.annotation.Nullable;

import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import com.gdmc.commands.BuildAreaCommand;

public class HttpInterfacePlugin extends JavaPlugin implements Listener {
	private static HttpInterfacePlugin instance;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onEnable() {
		instance = this;

		PluginCommand command = this.getCommand("set-build-area");
		BuildAreaCommand e = new BuildAreaCommand();
		command.setExecutor(e);
		command.setTabCompleter(e);
		
		try {
            GdmcHttpServer.startServer();
            this.getLogger().info("GDMC HTTP Server started successfully.");
        } catch (IOException ex) {
            this.getLogger().severe("GDMC HTTP Server failed to start! Is an instance of the server already running?");
        }
	}

	@Override
	public void onDisable() {
		GdmcHttpServer.stopServer();
        this.getLogger().info("GDMC HTTP Server stopped.");
		instance = null;
	}
	
	@Nullable
	public static HttpInterfacePlugin getInstance() {
		return instance;
	}
}
