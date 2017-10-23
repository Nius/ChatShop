package com.niusworks.chatshop;

import java.io.File;
import java.util.HashMap;
import java.util.Timer;

import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.niusworks.chatshop.commands.*;
import com.niusworks.chatshop.constructs.Order;
import com.niusworks.chatshop.managers.ChatManager;
import com.niusworks.chatshop.managers.DatabaseManager;
import com.niusworks.chatshop.managers.ItemManager;
import com.niusworks.chatshop.utilities.KeepAlive;

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
    
    /** The timer which manages the keep-alive feature. **/
    protected final Timer KEEP_ALIVE = new Timer(true);
    
    @Override
    public void onEnable()
    {        
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
        
        // Register all commands.
        this.getCommand("buy").setExecutor(new Buy(this));
        this.getCommand("cancel").setExecutor(new Cancel(this));
        this.getCommand("chatshop").setExecutor(new Help(this));
        this.getCommand("confirm").setExecutor(new Confirm(this));
        this.getCommand("csadmin").setExecutor(new CSAdmin(this));
        this.getCommand("dump").setExecutor(new Dump(this));
        this.getCommand("ebuy").setExecutor(new EBuy(this));
        this.getCommand("ecancel").setExecutor(new ECancel(this));
        this.getCommand("efind").setExecutor(new EFind(this));
        this.getCommand("ereprice").setExecutor(new EReprice(this));
        this.getCommand("esell").setExecutor(new ESell(this));
        this.getCommand("find").setExecutor(new Find(this));
        this.getCommand("history").setExecutor(new History(this));
        this.getCommand("quote").setExecutor(new Quote(this));
        this.getCommand("reprice").setExecutor(new Reprice(this));
        this.getCommand("sell").setExecutor(new Sell(this));
        this.getCommand("stock").setExecutor(new Stock(this));
        
        // Schedule keep-alive queries (if enabled).
        int ivl = getConfig().getInt("MySQL.keep-alive",-1);
        if(ivl >= 60000) // A value less than 60,000 signifies the feature is disabled.
        {
            KEEP_ALIVE.schedule(new KeepAlive(this),0,ivl);
        }
    }
    
    @Override
    public void onDisable()
    {
        KEEP_ALIVE.cancel();
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
