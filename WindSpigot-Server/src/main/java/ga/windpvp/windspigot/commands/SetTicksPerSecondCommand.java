package ga.windpvp.windspigot.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import net.md_5.bungee.api.ChatColor;
import net.minecraft.server.MinecraftServer;

// Sets a custom TPS
public class SetTicksPerSecondCommand extends Command {

	public SetTicksPerSecondCommand(String name) {
		super(name);
		this.description = "Sets a custom TPS";
		this.usageMessage = "/settps";
		this.setPermission("windspigot.command.settps");
	}

	@Override
	public boolean execute(CommandSender sender, String currentAlias, String[] args) {
		if (!testPermission(sender)) {
			return true;
		}

		if (args.length != 1) {
			sender.sendMessage(ChatColor.RED + "Usage: /settps <tps>");
			return true;
		} 
		
		if (Integer.valueOf(args[0]) != null) {
			MinecraftServer.getServer().setTps(Integer.valueOf(args[0]));
		}

		return true;
	}

}
