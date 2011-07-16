package com.onarandombox.MultiverseCore.command.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.onarandombox.MultiverseCore.MVWorld;
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.command.BaseCommand;

public class ModifyCopyCommand extends BaseCommand {

    public ModifyCopyCommand(MultiverseCore plugin) {
        super(plugin);
        this.name = "Copy all properties";
        this.description = "Copy all the aspects of one world to another.";
        this.usage = "/mvmodify " + ChatColor.GREEN + " COPY " + ChatColor.GOLD + " [WORLD_FROM] " + ChatColor.GREEN + " {WORLD_TO}";
        this.minArgs = 1;
        this.maxArgs = 2;
        this.identifiers.add("mvmodify copy");
        this.commandKeys.add("mv modify copy");
        this.commandKeys.add("mvm copy");
        this.commandKeys.add("mvmcopy");
        this.permission = "multiverse.world.modify";
        this.requiresOp = true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // We NEED a world from the command line
        Player p = null;
        if (!(sender instanceof Player)) {
            p = (Player) sender;
        }

        if (args.length == 3 && p == null) {
            sender.sendMessage("From the command line, both WORLD_FROM and WORLD_TO are required.");
            sender.sendMessage(this.description);
            sender.sendMessage(this.usage);
            sender.sendMessage("Nothing changed.");
            return;
        }

        MVWorld worldTo;
        MVWorld worldFrom;

        if (args.size() == 1) {
            worldTo = ((MultiverseCore) this.plugin).getMVWorld(p.getWorld().getName());
        } else {
            worldTo = ((MultiverseCore) this.plugin).getMVWorld(args.get(1));
        }

        worldFrom = this.plugin.getMVWorld(args.get(0));

        if (worldFrom == null) {
            sender.sendMessage("The world " + args.get(0) + " does not exist!");
            return;
        }

        if (worldTo == null) {
            if(args.size() == 1) {
                showNotMVWorldMessage(sender, p.getWorld().getName());
            } else {
            sender.sendMessage("The world " + args.get(0) + " does not exist!");
            }
            return;
        }

        if (!ModifyCommand.validateAction(Action.Set, property)) {
            sender.sendMessage("Sorry, you can't COPY properties.");
            sender.sendMessage("Please visit our wiki for more information: URLGOESHERE FERNFERRET DON'T FORGET IT!");
            return;
        }

        for (enum eachProperity : AddProperties.values()) {
            worldTo.setVariable(eachProperity, worldFrom.get(eachProperity)
        }

        for (enum eachProperity : SetProperties.values()) {
            worldTo.setVariable(eachProperity, worldFrom.get(eachProperity)
        }

        if (world.setVariable(property, value)) {
            sender.sendMessage("Property " + property + " was set to " + value);
        } else {
            sender.sendMessage("There was an error setting " + property);
        }
    }

}
