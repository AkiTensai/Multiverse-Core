package com.onarandombox.MultiverseCore;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import com.fernferret.allpay.AllPay;
import com.fernferret.allpay.GenericBank;
import com.onarandombox.MultiverseCore.command.CommandManager;
import com.onarandombox.MultiverseCore.command.commands.*;
import com.onarandombox.MultiverseCore.configuration.DefaultConfiguration;
import com.onarandombox.utils.DebugLog;
import com.onarandombox.utils.PurgeWorlds;
import com.onarandombox.utils.UpdateChecker;

public class MultiverseCore extends JavaPlugin {

    // Useless stuff to keep us going.
    private static final Logger log = Logger.getLogger("Minecraft");
    private static DebugLog debugLog;

    // Debug Mode
    private boolean debug;

    // Setup our Map for our Commands using the CommandHandler.
    private CommandManager commandManager;

    private final String tag = "[Multiverse-Core]";

    // Multiverse Permissions Handler
    public MVPermissions ph;

    // Configurations
    public Configuration configMV = null;
    public Configuration configWorlds = null;

    // Setup the block/player/entity listener.
    private MVPlayerListener playerListener = new MVPlayerListener(this);;

    private MVBlockListener blockListener = new MVBlockListener(this);
    private MVEntityListener entityListener = new MVEntityListener(this);
    private MVPluginListener pluginListener = new MVPluginListener(this);

    public UpdateChecker updateCheck;

    // HashMap to contain all the Worlds which this Plugin will manage.
    private HashMap<String, MVWorld> worlds = new HashMap<String, MVWorld>();

    // HashMap to contain information relating to the Players.
    public HashMap<String, MVPlayerSession> playerSessions = new HashMap<String, MVPlayerSession>();
    private PurgeWorlds worldPurger;
    public GenericBank bank = null;
    public AllPay banker = new AllPay(this, "[Multiverse-Core] ");;

    @Override
    public void onLoad() {
        // Create our DataFolder
        getDataFolder().mkdirs();
        // Setup our Debug Log
        debugLog = new DebugLog("Multiverse-Core", getDataFolder() + File.separator + "debug.log");

        // Setup & Load our Configuration files.
        loadConfigs();
    }

    public void onEnable() {
        // Output a little snippet to show it's enabled.
        this.log(Level.INFO, "- Version " + this.getDescription().getVersion() + " Enabled - By " + getAuthors());
        
        // Setup all the Events the plugin needs to Monitor.
        this.registerEvents();
        // Setup Permissions, we'll do an initial check for the Permissions plugin then fall back on isOP().
        this.ph = new MVPermissions(this);
        
        this.bank = this.banker.loadEconPlugin();
        // Setup the command manager
        this.commandManager = new CommandManager(this);
        // Setup the world purger
        this.worldPurger = new PurgeWorlds(this);
        // Call the Function to assign all the Commands to their Class.
        this.registerCommands();

        // Start the Update Checker
        // updateCheck = new UpdateChecker(this.getDescription().getName(), this.getDescription().getVersion());

        // Call the Function to load all the Worlds and setup the HashMap
        // When called with null, it tries to load ALL
        // this function will be called every time a plugin registers a new envtype with MV
        this.loadWorlds(true);
    }

    /**
     * Function to Register all the Events needed.
     */
    private void registerEvents() {
        PluginManager pm = getServer().getPluginManager();
        // pm.registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Highest, this); // Low so it acts above any other.
        pm.registerEvent(Event.Type.PLAYER_TELEPORT, this.playerListener, Priority.Highest, this); // Cancel Teleports if needed.
        pm.registerEvent(Event.Type.PLAYER_JOIN, this.playerListener, Priority.Normal, this); // To create the Player Session
        pm.registerEvent(Event.Type.PLAYER_QUIT, this.playerListener, Priority.Normal, this); // To remove Player Sessions
        pm.registerEvent(Event.Type.PLAYER_KICK, this.playerListener, Priority.Highest, this);
        pm.registerEvent(Event.Type.PLAYER_RESPAWN, this.playerListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_CHAT, this.playerListener, Priority.Normal, this);
        
        pm.registerEvent(Event.Type.ENTITY_REGAIN_HEALTH, this.entityListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.ENTITY_DAMAGE, this.entityListener, Priority.Normal, this); // To Allow/Disallow PVP as well as EnableHealth.
        pm.registerEvent(Event.Type.CREATURE_SPAWN, this.entityListener, Priority.Normal, this); // To prevent all or certain animals/monsters from spawning.

        pm.registerEvent(Event.Type.PLUGIN_ENABLE, this.pluginListener, Priority.Monitor, this);
        pm.registerEvent(Event.Type.PLUGIN_DISABLE, this.pluginListener, Priority.Monitor, this);

        // pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Priority.Normal, this); // To prevent Blocks being destroyed.
        // pm.registerEvent(Event.Type.BLOCK_PLACED, blockListener, Priority.Normal, this); // To prevent Blocks being placed.
        // pm.registerEvent(Event.Type.ENTITY_EXPLODE, entityListener, Priority.Normal, this); // Try to prevent Ghasts from blowing up structures.
        // pm.registerEvent(Event.Type.EXPLOSION_PRIMED, entityListener, Priority.Normal, this); // Try to prevent Ghasts from blowing up structures.
    }

    /**
     * Load the Configuration files OR create the default config files.
     */
    public void loadConfigs() {
        // Call the defaultConfiguration class to create the config files if they don't already exist.
        new DefaultConfiguration(getDataFolder(), "config.yml");
        new DefaultConfiguration(getDataFolder(), "worlds.yml");

        // Now grab the Configuration Files.
        this.configMV = new Configuration(new File(getDataFolder(), "config.yml"));
        this.configWorlds = new Configuration(new File(getDataFolder(), "worlds.yml"));

        // Now attempt to Load the configurations.
        try {
            this.configMV.load();
            log(Level.INFO, "- Multiverse Config -- Loaded");
        } catch (Exception e) {
            log(Level.INFO, "- Failed to load config.yml");
        }

        try {
            this.configWorlds.load();
            log(Level.INFO, "- World Config -- Loaded");
        } catch (Exception e) {
            log(Level.INFO, "- Failed to load worlds.yml");
        }

        // Setup the Debug option, we'll default to false because this option will not be in the default config.
        this.debug = this.configMV.getBoolean("debug", false);
    }

    /**
     * Register Multiverse-Core commands to DThielke's Command Manager.
     */
    private void registerCommands() {
        // Page 1
        this.commandManager.addCommand(new HelpCommand(this));
        this.commandManager.addCommand(new CoordCommand(this));
        this.commandManager.addCommand(new TeleportCommand(this));
        this.commandManager.addCommand(new ListCommand(this));
        this.commandManager.addCommand(new WhoCommand(this));
        this.commandManager.addCommand(new SetSpawnCommand(this));
        this.commandManager.addCommand(new CreateCommand(this));
        this.commandManager.addCommand(new ImportCommand(this));
        this.commandManager.addCommand(new SpawnCommand(this));
        this.commandManager.addCommand(new RemoveCommand(this));
        this.commandManager.addCommand(new DeleteCommand(this));
        this.commandManager.addCommand(new UnloadCommand(this));
        this.commandManager.addCommand(new ConfirmCommand(this));
        this.commandManager.addCommand(new InfoCommand(this));
        this.commandManager.addCommand(new ReloadCommand(this));
        
        this.commandManager.addCommand(new ModifyAddCommand(this));
        this.commandManager.addCommand(new ModifySetCommand(this));
        this.commandManager.addCommand(new ModifyRemoveCommand(this));
        this.commandManager.addCommand(new ModifyClearCommand(this));
        // This modify MUST go last.
        this.commandManager.addCommand(new ModifyCommand(this));
        this.commandManager.addCommand(new EnvironmentCommand(this));
        this.commandManager.addCommand(new PurgeCommand(this));
    }

    /**
     * Load the Worlds & Settings from the configuration file.
     */
    public void loadWorlds(boolean forceLoad) {
        // Basic Counter to count how many Worlds we are loading.
        int count = 0;
        // Grab all the Worlds from the Config.
        List<String> worldKeys = this.configWorlds.getKeys("worlds");

        // Force the worlds to be loaded, ie don't just load new worlds.
        if (forceLoad) {
            this.worlds.clear();
        }

        // Check that the list is not null.
        if (worldKeys != null) {
            for (String worldKey : worldKeys) {
                // Check if the World is already loaded within the Plugin.
                if (this.worlds.containsKey(worldKey)) {
                    continue;
                }
                // Grab the initial values from the config file.
                String environment = this.configWorlds.getString("worlds." + worldKey + ".environment", "NORMAL"); // Grab the Environment as a String.
                String seedString = this.configWorlds.getString("worlds." + worldKey + ".seed", "");

                String generatorstring = this.configWorlds.getString("worlds." + worldKey + ".generator");

                addWorld(worldKey, getEnvFromString(environment), seedString, generatorstring);

                // Increment the world count
                count++;
            }
        }

        // Ensure that the worlds created by the default server were loaded into MV, useful for first time runs
        // count += loadDefaultWorlds();
        // TODO: This was taken out because some people don't want nether! Instead show a message to people who have MVImport
        // and tell them to do MVImports for their worlds!

        // Simple Output to the Console to show how many Worlds were loaded.
        log(Level.INFO, count + " - World(s) loaded.");
    }

    /**
     *
     * @return
     */
    private int loadDefaultWorlds() {
        int additonalWorldsLoaded = 0;
        // Load the default world:
        World world = this.getServer().getWorlds().get(0);
        if (!this.worlds.containsKey(world.getName())) {
            addWorld(world.getName(), Environment.NORMAL, null, null);
            additonalWorldsLoaded++;
        }

        // This next one could be null if they have it disabled in server.props
        World world_nether = this.getServer().getWorld(world.getName() + "_nether");
        if (world_nether != null && !this.worlds.containsKey(world_nether.getName())) {
            addWorld(world_nether.getName(), Environment.NETHER, null, null);
            additonalWorldsLoaded++;
        }

        return additonalWorldsLoaded;
    }

    /**
     * Add a new World to the Multiverse Setup.
     *
     * Isn't there a prettier way to do this??!!?!?!
     *
     * @param name World Name
     * @param environment Environment Type
     */
    public boolean addWorld(String name, Environment env, String seedString, String generator) {
        this.debugLog(Level.CONFIG, "Adding world with: " + name + ", " + env.toString() + ", " + seedString + ", " + generator);
        Long seed = null;
        if (seedString != null && seedString.length() > 0) {
            try {
                seed = Long.parseLong(seedString);
            } catch (NumberFormatException numberformatexception) {
                seed = (long) seedString.hashCode();
            }
        }

        String generatorID = null;
        String generatorName = null;
        if (generator != null) {
            String[] split = generator.split(":", 2);
            String id = (split.length > 1) ? split[1] : null;
            generatorName = split[0];
            generatorID = id;
        }

        ChunkGenerator customGenerator = getChunkGenerator(generatorName, generatorID, name);

        if (customGenerator == null && generator != null && (generator.length() > 0)) {
            if (!pluginExists(generatorName)) {
                log(Level.WARNING, "Could not find plugin: " + generatorName);
            } else {
                log(Level.WARNING, "Found plugin: " + generatorName + ", but did not find generatorID: " + generatorID);

            }

            return false;
        }

        World world = null;
        if (seed != null) {
            if (customGenerator != null) {
                world = getServer().createWorld(name, env, seed, customGenerator);
                log(Level.INFO, "Loading World & Settings - '" + name + "' - " + env + " with seed: " + seed + " & Custom Generator: " + generator);
            } else {
                world = getServer().createWorld(name, env, seed);
                log(Level.INFO, "Loading World & Settings - '" + name + "' - " + env + " with seed: " + seed);
            }
        } else {
            if (customGenerator != null) {
                world = getServer().createWorld(name, env, customGenerator);
                log(Level.INFO, "Loading World & Settings - '" + name + "' - " + env + " & Custom Generator: " + generator);
            } else {
                world = getServer().createWorld(name, env);
                log(Level.INFO, "Loading World & Settings - '" + name + "' - " + env);
            }
        }
        MVWorld mvworld = new MVWorld(world, this.configWorlds, this, seed, generator);
        this.worldPurger.purgeWorld(null, mvworld);
        this.worlds.put(name, mvworld);
        return true;

    }

    private boolean pluginExists(String generator) {
        Plugin plugin = getServer().getPluginManager().getPlugin(generator);
        return plugin != null;
    }

    private ChunkGenerator getChunkGenerator(String generator, String generatorID, String worldName) {
        if (generator == null) {
            return null;
        }

        Plugin plugin = getServer().getPluginManager().getPlugin(generator);
        if (plugin == null) {
            return null;
        } else {
            return plugin.getDefaultWorldGenerator(worldName, generatorID);

        }
    }

    /**
     * Remove the world from the Multiverse list
     *
     * @param name The name of the world to remove
     * @return True if success, false if failure.
     */
    public boolean unloadWorld(String name) {
        if (this.worlds.containsKey(name)) {
            this.worlds.remove(name);
            return true;
        }
        return false;
    }

    /**
     * Remove the world from the Multiverse list and from the config
     *
     * @param name The name of the world to remove
     * @return True if success, false if failure.
     */
    public boolean removeWorld(String name) {
        unloadWorld(name);
        this.configWorlds.removeProperty("worlds." + name);
        this.configWorlds.save();
        return false;
    }

    /**
     * Remove the world from the Multiverse list, from the config and deletes the folder
     *
     * @param name The name of the world to remove
     * @return True if success, false if failure.
     */
    public boolean deleteWorld(String name) {
        unloadWorld(name);
        removeWorld(name);
        if (getServer().unloadWorld(name, false)) {
            return deleteFolder(new File(name));
        }
        return false;
    }

    /**
     * Delete a folder Courtesy of: lithium3141
     *
     * @param file The folder to delete
     * @return true if success
     */
    private boolean deleteFolder(File file) {
        if (file.exists()) {
            // If the file exists, and it has more than one file in it.
            if (file.isDirectory()) {
                for (File f : file.listFiles()) {
                    if (!this.deleteFolder(f)) {
                        return false;
                    }
                }
            }
            file.delete();
            return !file.exists();
        } else {
            return false;
        }
    }

    /**
     * What happens when the plugin gets disabled...
     */
    public void onDisable() {
        debugLog.close();
        log(Level.INFO, "- Disabled");
    }

    /**
     * Grab the players session if one exists, otherwise create a session then return it.
     *
     * @param player
     * @return
     */
    public MVPlayerSession getPlayerSession(Player player) {
        if (this.playerSessions.containsKey(player.getName())) {
            return this.playerSessions.get(player.getName());
        } else {
            this.playerSessions.put(player.getName(), new MVPlayerSession(player, this.configMV, this));
            return this.playerSessions.get(player.getName());
        }
    }

    /**
     * Grab and return the Teleport class.
     *
     * @return
     */
    public MVTeleport getTeleporter() {
        return new MVTeleport(this);
    }

    /**
     * Grab the Permissions Handler for MultiVerse
     */
    public MVPermissions getPermissions() {
        return this.ph;
    }

    public PurgeWorlds getWorldPurger() {
        return this.worldPurger;
    }

    /**
     * onCommand
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        if (this.isEnabled() == false) {
            sender.sendMessage("This plugin is Disabled!");
            return true;
        }
        return this.commandManager.dispatch(sender, command, commandLabel, args);
    }

    /**
     * Print messages to the server Log as well as to our DebugLog. 'debugLog' is used to seperate Heroes information from the Servers Log Output.
     *
     * @param level
     * @param msg
     */
    public void log(Level level, String msg) {
        log.log(level, this.tag + " " + msg);
        debugLog.log(level, this.tag + " " + msg);
    }

    /**
     * Print messages to the Debug Log, if the servers in Debug Mode then we also wan't to print the messages to the standard Server Console.
     *
     * @param level
     * @param msg
     */
    public void debugLog(Level level, String msg) {
        if (this.debug) {
            log.log(level, "[Debug] " + msg);
        }
        debugLog.log(level, "[Debug] " + msg);
    }

    /**
     * Parse the Authors Array into a readable String with ',' and 'and'.
     *
     * @return
     */
    private String getAuthors() {
        String authors = "";
        ArrayList<String> auths = this.getDescription().getAuthors();

        if (auths.size() == 1) {
            return auths.get(0);
        }

        for (int i = 0; i < auths.size(); i++) {
            if (i == this.getDescription().getAuthors().size() - 1) {
                authors += " and " + this.getDescription().getAuthors().get(i);
            } else {
                authors += ", " + this.getDescription().getAuthors().get(i);
            }
        }
        return authors.substring(2);
    }

    public CommandManager getCommandManager() {
        return this.commandManager;
    }

    public String getTag() {
        return this.tag;
    }

    /**
     * This code should get moved somewhere more appropriate, but for now, it's here.
     *
     * @param env
     * @return
     */
    public Environment getEnvFromString(String env) {
        // Don't reference the enum directly as there aren't that many, and we can be more forgiving to users this way
        if (env.equalsIgnoreCase("HELL") || env.equalsIgnoreCase("NETHER"))
            env = "NETHER";

        if (env.equalsIgnoreCase("SKYLANDS") || env.equalsIgnoreCase("SKYLAND") || env.equalsIgnoreCase("STARWARS"))
            env = "SKYLANDS";

        if (env.equalsIgnoreCase("NORMAL") || env.equalsIgnoreCase("WORLD"))
            env = "NORMAL";

        try {
            return Environment.valueOf(env);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // TODO: Find out where to put these next 3 methods! I just stuck them here for now --FF

    public Collection<MVWorld> getMVWorlds() {
        return this.worlds.values();
    }

    public MVWorld getMVWorld(String name) {
        if (this.worlds.containsKey(name)) {
            return this.worlds.get(name);
        }
        return null;
    }

    public boolean isMVWorld(String name) {
        return this.worlds.containsKey(name);
    }
}
