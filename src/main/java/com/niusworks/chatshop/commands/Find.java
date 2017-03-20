package com.niusworks.chatshop.commands;

import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.constructs.Listing;
import com.niusworks.chatshop.managers.ChatManager;
import com.niusworks.chatshop.managers.ItemManager;

/**
 * Executor for the "find" and "f" commands for OC Network's ChatShop.
 * <br>
 * Players can search the market for an item, showing all listings of its prices and sellers.
 * Listings are ordered by price ascending. This command has two elements, item and page.
 * <br><br>
 * Item can be any ItemManager-recognized string representation of a Minecraft item as understood
 * by {@link ItemManager#parse}. Invalid items are caught and appropriate messages are sent to
 * the player.
 * <br><br>
 * Page, optional, is an integer indicating which page of output to display. Very often there are
 * many listings available for a given item, and to prevent flooding the player's chat these listings
 * are divided into "pages" by the {@link ChatManager}. Only the specified page is shown. If no
 * page number is given then the first page will be shown.
 * <br><br>
 * This command has the following limits (aside from basic perms):
 * <ul>
 * <li>Console access denied.
 * <li>World must be whitelisted in config OR config must allow querying from anyone (see below).
 * <li>Gamemode must be whitelisted in config OR config must allow querying from anyone (see below).
 * </ul>
 * This command is effectively a read-only command; the database is queried for information but
 * nothing is changed. By default ChatShop will allow this command even if the player is in the wrong
 * world or the wrong gamemode, but administrators can configuratively disable this liberty.
 * <br><br>
 * @author ObsidianCraft Staff
 */
public class Find implements CommandExecutor
{
    /** Command usage. **/
    public static final String USAGE = "/find <item> [page]";
    
    /** The specific instance of the parent ChatShop plugin. **/
    private final ChatShop PLUGIN;
    
    /**
     * Instantiate the command executor for "find" and "f" commands.
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
        if(args.length != 1 && args.length != 2)
            return PLUGIN.CM.error(sender,USAGE);
        
        //Item check
        
        //If the specified item is non-specifically "potion" or some related query, show potions help instead.
        if( args[0].equalsIgnoreCase("potion")          || args[0].equalsIgnoreCase("potions")          ||
            args[0].equalsIgnoreCase("splashpotion")    || args[0].equalsIgnoreCase("splashpotions")    ||
            args[0].equalsIgnoreCase("lingeringpotion") || args[0].equalsIgnoreCase("lingeringpotions")    )
            return PLUGIN.getCommand("chatshop").getExecutor().onCommand(usr,cmd,"potions",new String[] {"0"});
        
        //Consult ItemManager to turn the user argument into a valid,
        //special-rules compliant item.
        Object parse = PLUGIN.IM.parse(usr,args[0]);
        if(parse instanceof Integer)
            switch((Integer)parse)
            {
                case -1: return PLUGIN.CM.error(usr,"You are not holding an item.");
                case -2:
                case -3: return PLUGIN.CM.error(usr,"Invalid item: " + PLUGIN.CM.color("item") + args[0] + PLUGIN.CM.color("error") + ".");
                case -4: return PLUGIN.CM.error(usr,"Enchanted items cannot be sold on the ChatShop.");
                case -5: return PLUGIN.CM.error(usr,"Damaged items cannot be sold on the ChatShop.");
                case -6: return PLUGIN.CM.error(usr,"That item cannot be sold on the ChatShop.");
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
        
        //Getting colors is fairly expensive, so do it once on
        //execution rather than once per line of output.
        String textCol = PLUGIN.CM.color("text");
        String itemCol = PLUGIN.CM.color("item");
        String qtyCol = PLUGIN.CM.color("quantity");
        String priceCol = PLUGIN.CM.color("price");
        String playerCol = PLUGIN.CM.color("player");
        
        //On SQL fail...
        if(listings == null)
            return PLUGIN.CM.err500(usr);
        //On no listings...
        if(listings.length == 0)
            return PLUGIN.CM.error(usr,
                "No listings found for " +
                PLUGIN.CM.color("item") + displayName +
                PLUGIN.CM.color("error") + ".");
        
        //Head the sales list.
        //Checking the page number is already taken care of by
        //ChatManager, but for purposes of displaying an accurate
        //number it needs to happen here.
        page = Math.max(page,1);
        String msg =
                textCol + "Listings for " +
                itemCol + displayName +
                textCol + ", page " + page +
                " of " + PLUGIN.CM.paginate(listings) + ":";
        PLUGIN.CM.reply(usr,msg);
        
        //List all listings on this page.
        listings = PLUGIN.CM.paginate(listings,page);
        for(int i = 0; i < listings.length; i ++)
        {
            // Attempt to resolve the seller's current username from
            // their UUID. This will usually be successful, but for
            // long-time absentee players this will fail. In that case
            // the posted name in the database will be used.
            String playerName = PLUGIN.getServer().getOfflinePlayer(
                    UUID.fromString(listings[i].PLAYER_UUID))
                    .getName();
            if(playerName == null)
                playerName = listings[i].PLAYER_ALIAS;
            
            msg =
                priceCol + ChatManager.format(listings[i].PRICE) +
                textCol + ", " +
                qtyCol + ChatManager.format(listings[i].QUANTITY) +
                textCol + " from " +
                playerCol + playerName;
                    
            PLUGIN.CM.reply(usr,msg,false);
        }
        
        return true;
    }
}
