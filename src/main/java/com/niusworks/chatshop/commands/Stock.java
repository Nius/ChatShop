package com.niusworks.chatshop.commands;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.managers.ChatManager;
import com.niusworks.chatshop.managers.DatabaseManager.Listing;

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
     * Instantiate the command executor for "buy" commands.
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
        Player usr = (Player)sender;
        
        // Denial of service conditions
        // This command could conceivably be used from the console.
        // It'd make sense either way, to leave it available or block it.
        // Were the console to use it, the second argument would be required.
        //if(!(sender instanceof Player))
        //    return PLUGIN.CM.reply(sender,"ChatShop.sell cannot be executed as console.");
        if(args.length != 2 && !(sender instanceof Player))
            return PLUGIN.CM.reply(sender,"ChatShop.stock requires a second argument when used from the console.");
        if(!sender.hasPermission("chatshop.find"))
            return PLUGIN.CM.denyPermission(sender);
        
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
        
        //Figure out which page of listings to display.
        int listingsPerPage = PLUGIN.getConfig().getInt("chat.page-length");
        int pagesAvailable = (int)(listings.length / listingsPerPage);
        if(page > pagesAvailable)
            page = pagesAvailable;
        
        //Head the sales list.
        String msg =
                PLUGIN.CM.color("text") + "Listings for " +
                PLUGIN.CM.color("item") + qPlayer.getName() +
                PLUGIN.CM.color("text") + ", page " + (page + 1) +
                " of " + (pagesAvailable + 1) + ":";
        PLUGIN.CM.reply(usr,msg);
        
        //List all listings on this page.
        int startIndex = page * listingsPerPage;
        for(int i = startIndex; i < listings.length && i < startIndex + 10; i ++)
        {
            msg =
                PLUGIN.CM.color("quantity") + ChatManager.format(listings[i].QUANTITY) + " " +
                PLUGIN.CM.color("item") + PLUGIN.IM.lookup(listings[i].ID,listings[i].DAMAGE).DISPLAY +
                PLUGIN.CM.color("text") + " at " +
                PLUGIN.CM.color("price") + ChatManager.format(listings[i].PRICE) +
                PLUGIN.CM.color("text") + " each.";
            PLUGIN.CM.reply(usr,msg);
        }
        
        return true;
    }
}
