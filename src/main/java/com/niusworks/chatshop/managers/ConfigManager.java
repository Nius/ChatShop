package com.niusworks.chatshop.managers;

import java.io.File;

import com.niusworks.chatshop.ChatShop;

/**
 * A child of the ChatShop plugin for OC Network.
 * Handles interaction with the config file.
 * @author Nicholas Harrell (SirNius / Nius Atreides)
 */
public class ConfigManager
{
    //
    //      MANAGER NOT USED
    //
    
    
    /** The master plugin for this manager. **/
    protected final ChatShop PLUGIN;
    
    /**
     * Create an ItemManager with a reference to the master
     * plugin.
     * 
     * @param master    The master ChatShop plugin reference.
     */
    public ConfigManager(ChatShop master)
    {
        PLUGIN = master;
        load();
    }
    
    /**
     * Load the config. If the file is missing, place it.
     */
    public void load()
    {
        if(!(new File(PLUGIN.getDataFolder(),"config.yml")).exists())
        {
            PLUGIN.CM.log("Configuration file is missing. Spawning a new one now.");
            PLUGIN.saveDefaultConfig();
        }
        PLUGIN.reloadConfig();
    }
}
