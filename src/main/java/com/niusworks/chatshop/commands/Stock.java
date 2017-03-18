package com.niusworks.chatshop.commands;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.managers.ChatManager;
import com.niusworks.chatshop.managers.DatabaseManager.Listing;
import com.niusworks.chatshop.managers.ItemManager.Item;

/**
 * Executor for the "stock" command for
 * OC Network's ChatShop.
 * @author ObsidianCraft Staff
 */
public class Stock implements CommandExecutor
{
    /** Command usage. **/
    public static final String USAGE = "/stock [player] [page]";
    
    /** The specific instance of the parent ChatShop plugin. **/
    private final ChatShop PLUGIN;
    
    /**
     * Instantiate the command executor for "stock" commands.
     * 
     * @param master    The specific instance of the parent ChatShop plugin.
     */
    public Stock(ChatShop master)
    {
        PLUGIN = master;
    }
    
    @SuppressWarnings("deprecation") //For getOfflinePlayer() (see below)
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String lbl, String[] args)
    {
        //
        // Denial of service conditions
        //
        
        //No console
        if(!(sender instanceof Player))
            return PLUGIN.CM.reply(sender,"ChatShop.find cannot be executed as console.");
        Player usr = (Player)sender;
        //Permissions
        if(!sender.hasPermission("chatshop.find"))
            return PLUGIN.CM.denyPermission(sender);
        //Gamemode
        if(!PLUGIN.getConfig().getBoolean("query-anyone"))
        {
            Object[] modes = PLUGIN.getConfig().getList("allowed-modes").toArray();
            boolean allowed = false;
            for(int i = 0; i < modes.length; i ++)
                if(modes[i] instanceof String)
                    if(((String)modes[i]).equalsIgnoreCase(usr.getGameMode().toString()))
                        allowed = true;
            if(!allowed)
                return PLUGIN.CM.denyGameMode(sender);
            
            allowed = false;
            Object[] worlds = PLUGIN.getConfig().getList("allowed-worlds").toArray();
            for(int i = 0; i < worlds.length; i ++)
                if(worlds[i] instanceof String)
                    if(((String)worlds[i]).equalsIgnoreCase(usr.getWorld().getName()))
                        allowed = true;
            if(!allowed)
                return PLUGIN.CM.denyWorld(sender);
        }
        
        //
        //  VALIDATION
        //
        
        //Number of args
        if(args.length > 2)
            return PLUGIN.CM.error(sender,USAGE);
        
        //Page check
        int page = 1;
        boolean didSpecifyPage = false;
        if(args.length > 0)
            try
            {
                //Parse the last argument for a page number.
                page = Integer.parseInt(args[args.length - 1]);
                didSpecifyPage = true;
            }
            catch(NumberFormatException e)
            {
                //If there are two arguments and the last one isn't a number,
                // then the command is malformed.
                if(args.length == 2)
                    return PLUGIN.CM.error(sender,USAGE);
                
                //The only other possibility is that there is only one argument,
                // and that argument is a playername, so the page number was omitted.
                else
                    page = 1;
            }
        
        //Player check
        //There is no way around this deprecated method, which can be
        // extremely painful if you think about it too much.
        //Keep an eye out for an officially implemented replacement
        // method in future.
        OfflinePlayer qPlayer;
        if(args.length > (didSpecifyPage ? 1 : 0))
        {
            qPlayer = PLUGIN.getServer().getOfflinePlayer(args[0]);
            if(!qPlayer.hasPlayedBefore())
                return PLUGIN.CM.error(sender,
                        PLUGIN.CM.color("player") + qPlayer.getName() +
                        PLUGIN.CM.color("error") + " has never played on ObsidianCraft.");
        }
        else
            qPlayer = (Player)sender;
        
        //
        //  EXECUTION
        //
        
        Listing[] listings = PLUGIN.DB.getListings(qPlayer);
        
        //
        //  RESULT
        //
        
        //On SQL fail...
        if(listings == null)
            return PLUGIN.CM.err500(usr);
        //On no listings...
        if(listings.length == 0)
            return PLUGIN.CM.error(usr,
                "No listings found for " +
                PLUGIN.CM.color("player") + qPlayer.getName() +
                PLUGIN.CM.color("error") + ".");
        
        //Head the sales list.
        //Checking the page number is already taken care of by
        //ChatManager, but for purposes of displaying an accurate
        //number it needs to happen here.
        page = Math.max(page,1);
        page = Math.min(page,PLUGIN.CM.paginate(listings));
        String msg =
                PLUGIN.CM.color("text") + "Listings for " +
                PLUGIN.CM.color("item") + qPlayer.getName() +
                PLUGIN.CM.color("text") + ", page " + page +
                " of " + PLUGIN.CM.paginate(listings) + ":";
        PLUGIN.CM.reply(usr,msg);
        
        //List all listings on this page.
        listings = PLUGIN.CM.paginate(listings,page);
        for(int i = 0; i < listings.length; i ++)
        {
            // Attempt to resolve the name of the material.
            // This should always be successful because these are being read from
            // a database of theoretically valid listings, but just in case...
            String itemDisplay = "Unknown Item";
            Item thing = PLUGIN.IM.lookup(listings[i].MATERIAL,listings[i].DAMAGE);
            if(thing != null)
                itemDisplay = thing.DISPLAY;
            
            msg =
                PLUGIN.CM.color("quantity") + ChatManager.format(listings[i].QUANTITY) + " " +
                PLUGIN.CM.color("item") + itemDisplay +
                PLUGIN.CM.color("text") + " at " +
                PLUGIN.CM.color("price") + ChatManager.format(listings[i].PRICE) +
                PLUGIN.CM.color("text") + " each.";
            PLUGIN.CM.reply(usr,msg,false);
        }
        
        return true;
    }
}
