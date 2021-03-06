package com.onarandombox.MultiverseCore.command.commands;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.command.BaseCommand;

public class SetSpawnCommand extends BaseCommand {
    
    public SetSpawnCommand(MultiverseCore plugin) {
        super(plugin);
        this.name = "Set World Spawn";
        this.description = "Sets the spawn for the current world.";
        this.usage = "/mvsetspawn";
        this.minArgs = 0;
        this.maxArgs = 0;
        this.identifiers.add("mvsetspawn");
        this.permission = "multiverse.world.spawn.set";
        this.requiresOp = true;
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
            Location l = p.getLocation();
            World w = p.getWorld();
            w.setSpawnLocation(l.getBlockX(), l.getBlockY(), l.getBlockZ());
            p.sendMessage(w.getName() + " - Spawn set to X: " + l.getBlockX() + "  Y: " + l.getBlockY() + " Z: " + l.getBlockZ());
        } else {
            sender.sendMessage(this.IN_GAME_COMMAND_MSG);
        }
        return;
    }
    
}
