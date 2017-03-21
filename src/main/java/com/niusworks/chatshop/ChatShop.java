package com.niusworks.chatshop;

import java.io.File;
import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.niusworks.chatshop.commands.*;
import com.niusworks.chatshop.constructs.Order;
import com.niusworks.chatshop.managers.ChatManager;
import com.niusworks.chatshop.managers.DatabaseManager;
import com.niusworks.chatshop.managers.ItemManager;

import net.milkbowl.vault.economy.Economy;

/**
 * Chat-based shop plugin designed for ObsidianCraft Network.
 * This plugin and all of its future versions are freely licensed
 * for use on ObsidianCraft Network systems, and for modification and
 * continued development by ObsidianCraft staff.
 * Commissioned 2017-02-26
 * 
 * @author ObsidianCraft Staff
 */
public class ChatShop extends JavaPlugin
{
    /** The Economy functionality provider for this plugin. **/
    public Economy ECON;
    /** The output manager for this entire plugin. **/
    public final ChatManager CM = new ChatManager(this);
    /** The item manager for this plugin. **/
    public final ItemManager IM = new ItemManager(this,getDataFolder());
    /** The database manager for this plugin. **/
    public final DatabaseManager DB = new DatabaseManager(this);
    
    /** A map of pending orders, used by /buy, /confirm, and /sell. **/
    public final HashMap<Player,Order> PENDING = new HashMap<Player,Order>();
    
    @Override
    public void onEnable()
    {        
        // Say hello!
        CM.log("Initializing ChatShop plugin version 0.1 by Nicholas Harrell");
        
        // Load config
        if(!(new File(getDataFolder(),"config.yml")).exists())
        {
            CM.log("Configuration file is missing. Spawning a new one now.");
            saveDefaultConfig();
        }
        getConfig().options().copyDefaults(true);
        reloadConfig();
        
        // Load the databse.
        int dbstatus = DB.initialize();
        if(dbstatus != 1)
        {
            fail("Cannot proceed without database.");
            return;
        }
        
        // Execute item library setup and validation.
        int imstatus = IM.loadItems();
        switch(imstatus)
        {
            case -1:
                //Success, don't do anything.
                break;
            case -2:
                fail("Could not read file " + this.getDataFolder() + "/items.csv");
                return;
            case -3:
                fail("Could not create file " + this.getDataFolder() + "/items.csv");
                return;
            default:
                fail("Error in items.csv on line " + (imstatus + 1) + ".");
                return;
        }
        
        // Link to the economy provider.
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        ECON = rsp.getProvider();
        
        //Register all commands.
        this.getCommand("buy").setExecutor(new Buy(this));
        this.getCommand("cancel").setExecutor(new Cancel(this));
        this.getCommand("chatshop").setExecutor(new Help(this));
        this.getCommand("confirm").setExecutor(new Confirm(this));
        this.getCommand("csadmin").setExecutor(new CSAdmin(this));
        this.getCommand("dump").setExecutor(new Dump(this));
        this.getCommand("find").setExecutor(new Find(this));
        this.getCommand("history").setExecutor(new History(this));
        this.getCommand("quote").setExecutor(new Quote(this));
        this.getCommand("reprice").setExecutor(new Reprice(this));
        this.getCommand("sell").setExecutor(new Sell(this));
        this.getCommand("stock").setExecutor(new Stock(this));
    }
    
    @Override
    public void onDisable()
    {
        DB.close();
    }
        
    /**
     * Send a severe message to the console and terminate this plugin.
     * 
     * @param message   Suicide note.
     */
    public void fail(String message)
    {
        this.getLogger().severe(message);
        this.getServer().getPluginManager().disablePlugin(this);
    }
}
