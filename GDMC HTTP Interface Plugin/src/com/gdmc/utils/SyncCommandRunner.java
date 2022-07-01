package com.gdmc.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

public class SyncCommandRunner implements RemoteConsoleCommandSender {
	private List<String> value = new ArrayList<>();
	private Semaphore s = new Semaphore(0);

	public void run(String commandLine) {
		try {
			Bukkit.dispatchCommand(this, commandLine);
		} finally {
			s.release();
		}
	}

	public List<String> getOutput() {
		try {
			s.acquire();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return value;
	}

	@Override
	public boolean isPermissionSet(String name) {
		return Bukkit.getConsoleSender().isPermissionSet(name);
	}

	@Override
	public boolean isPermissionSet(Permission perm) {
		return Bukkit.getConsoleSender().isPermissionSet(perm);
	}

	@Override
	public boolean hasPermission(String name) {
		return Bukkit.getConsoleSender().hasPermission(name);
	}

	@Override
	public boolean hasPermission(Permission perm) {
		return Bukkit.getConsoleSender().hasPermission(perm);
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
		return Bukkit.getConsoleSender().addAttachment(plugin, name, value);
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin) {
		return Bukkit.getConsoleSender().addAttachment(plugin);
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
		return Bukkit.getConsoleSender().addAttachment(plugin, name, value, ticks);
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
		return Bukkit.getConsoleSender().addAttachment(plugin, ticks);
	}

	@Override
	public void removeAttachment(PermissionAttachment attachment) {
		Bukkit.getConsoleSender().removeAttachment(attachment);
	}

	@Override
	public void recalculatePermissions() {
		Bukkit.getConsoleSender().recalculatePermissions();
	}

	@Override
	public Set<PermissionAttachmentInfo> getEffectivePermissions() {
		return Bukkit.getConsoleSender().getEffectivePermissions();
	}

	@Override
	public boolean isOp() {
		return Bukkit.getConsoleSender().isOp();
	}

	@Override
	public void setOp(boolean value) {
		Bukkit.getConsoleSender().setOp(value);
	}

	@Override
	public void sendMessage(String message) {
		value.add(message);
	}

	@Override
	public void sendMessage(String[] messages) {
		for (String message : messages) {
			value.add(message);
		}
	}

	@Override
	public void sendMessage(UUID sender, String message) {
		value.add(message);
	}

	@Override
	public void sendMessage(UUID sender, String[] messages) {
		for (String message : messages) {
			value.add(message);
		}
	}

	@Override
	public Server getServer() {
		return Bukkit.getConsoleSender().getServer();
	}

	@Override
	public String getName() {
		return "GDMC_CommandExecutor";
	}

	@Override
	public Spigot spigot() {
		return Bukkit.getConsoleSender().spigot();
	}

}
