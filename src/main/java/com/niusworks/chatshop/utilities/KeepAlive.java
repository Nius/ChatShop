package com.niusworks.chatshop.utilities;

import java.util.TimerTask;

import com.niusworks.chatshop.ChatShop;

/**
 * Periodical code definition for ChatShop's keep-alive feature.
 * 
 * @author ObsidianCraft Staff
 */
public class KeepAlive extends TimerTask
{
    /** The master plugin for this manager. **/
    protected final ChatShop PLUGIN;
    
    /**
     * Create a KeepAlive object with a reference to the master plugin.
     * 
     * @param plugin    The master ChatShop plugin reference.
     */
    public KeepAlive(ChatShop plugin)
    {
        PLUGIN = plugin;
    }
    
    @Override
    public void run()
    {
        PLUGIN.CM.log("Sending keep-alive query...");
        PLUGIN.DB.keepAlive();
    }
}
