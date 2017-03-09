package com.niusworks.chatshop.commands;

import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.managers.ChatManager;
import com.niusworks.chatshop.managers.DatabaseManager.Listing;

/**
 * A child of the ChatShop plugin for OC Network.
 * Executor for the "find" command.
 * @author Nicholas Harrell (SirNius / Nius Atreides)
 */
public class Find implements CommandExecutor
{
    /** Command usage. **/
    public static final String USAGE = "/find <item> [page]";
    
    /** The specific instance of the parent ChatShop plugin. **/
    private final ChatShop PLUGIN;
    
    /**
     * Instantiate the command executor for "buy" commands.
     * 
     * @param master    The specific instance of the parent ChatShop plugin.
     */
    public Find(ChatShop master)
    {
        PLUGIN = master;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String lbl, String[] args)
    {
        Player usr = (Player)sender;
        
        // Denial of service conditions
        // This command could conceivably be used from the console.
        // It'd make sense either way, to leave it available or block it.
        //if(!(sender instanceof Player))
        //    return PLUGIN.CM.reply(sender,"ChatShop.find cannot be executed as console.");
        if(!sender.hasPermission("chatshop.find"))
            return PLUGIN.CM.denyPermission(sender);
        
        //
        //  VALIDATION
        //
        
        //Number of args
        if(args.length != 1 && args.length != 2)
            return PLUGIN.CM.error(sender,USAGE);
        
        //Item check
        Object parse = PLUGIN.IM.parse(usr,args[0]);
        if(parse instanceof Integer)
            switch((Integer)parse)
            {
                case -1: return PLUGIN.CM.error(usr,"You are not holding an item.");
                case -2:
                case -3: return PLUGIN.CM.error(usr,"Invalid item: " + PLUGIN.CM.color("item") + args[0] + PLUGIN.CM.color("error") + ".");
                default: return PLUGIN.CM.err500(usr);
            }
        ItemStack merchandise = (ItemStack)parse;
        String displayName = PLUGIN.IM.getDisplayName(merchandise);
        
        //Page check
        int page = 1;
        if(args.length == 2)
            try
            {
                page = Integer.parseInt(args[1]);
            }
            catch(NumberFormatException e)
            {
                return PLUGIN.CM.error(sender,USAGE);
            }
        
        //
        //  EXECUTION
        //
        
        Listing[] listings = PLUGIN.DB.getListings(merchandise);
        
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
                PLUGIN.CM.color("item") + displayName +
                PLUGIN.CM.color("error") + ".");
        
        //Figure out which page of listings to display.
        int listingsPerPage = PLUGIN.getConfig().getInt("chat.page-length");
        int pagesAvailable = (int)(listings.length / listingsPerPage);
        if(page > pagesAvailable)
            page = pagesAvailable;
        
        //Head the sales list.
        String msg =
                PLUGIN.CM.color("text") + "Listings for " +
                PLUGIN.CM.color("item") + displayName +
                PLUGIN.CM.color("text") + ", page " + (page + 1) +
                " of " + (pagesAvailable + 1) + ":";
        PLUGIN.CM.reply(usr,msg);
        
        //List all listings on this page.
        int startIndex = page * listingsPerPage;
        for(int i = startIndex; i < listings.length && i < startIndex + 10; i ++)
        {
            msg =
                PLUGIN.CM.color("price") + ChatManager.format(listings[i].PRICE) +
                PLUGIN.CM.color("text") + ", " +
                PLUGIN.CM.color("quantity") + ChatManager.format(listings[i].QUANTITY) +
                PLUGIN.CM.color("text") + " from " +
                PLUGIN.CM.color("player") +
                    PLUGIN.getServer().getPlayer(
                            UUID.fromString(listings[i].SELLER))
                                .getName();
            PLUGIN.CM.reply(usr,msg);
        }
        
        return true;
    }
}
