package com.onarandombox.utils;

import com.onarandombox.MultiverseCore.MultiverseCore;

public class Destination {
    private String name;
    private DestinationType type;
    
    public Destination(String name, DestinationType type) {
        this.name = name;
        this.type = type;
    }
    
    public String getName() {
        return this.name;
    }
    
    public DestinationType getType() {
        return this.type;
    }
    
    private static Destination getBadDestination() {
        return new Destination("", DestinationType.Invalid);
    }
    /**
     * Takes the given string and returns a Destination. This will NEVER be NULL. It will return a destination of type Invalid if the destination is bad.
     * @param dest The parceable string, ex: w:My World
     * @param plugin The MultiverseCore plugin used to find valid worlds/portals
     * @return A new Destination from the parsed string.
     */
    public static Destination parseDestination(String dest, MultiverseCore plugin) {
        if (dest == null) {
            return getBadDestination();
        }
        
        String[] items = dest.split(":");
        if (items.length > 2) {
            return getBadDestination();
        }
        
        // If we only found one param, assume world, but still check for validity
        if (items.length == 1 && plugin.isMVWorld(items[0])) {
            return new Destination(items[0], DestinationType.World);
        }
        
        if (items[0].equalsIgnoreCase("w") && plugin.isMVWorld(items[0])) {
            return new Destination(items[1], DestinationType.World);
        } else if (items[0].equalsIgnoreCase("p")) {
            // TODO: Check for a valid portal, we can't right now, as portals aren't really in yet.
            return new Destination(items[1], DestinationType.Portal);
        }
        return getBadDestination();
    }
}
