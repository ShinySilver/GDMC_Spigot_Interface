package com.gdmc.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class BuildAreaCommand implements CommandExecutor, TabCompleter {

	public BuildAreaCommand() {
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		ArrayList<String> list = new ArrayList<>();
		if (args.length == 1) {
			// TODO
		} else if (args.length == 2 && !args[0].equals("undo")) {
			// TODO
		} else if (args.length == 3 && !args[0].equals("undo")) {
			// TODO
		}
		return list;
	}
}
