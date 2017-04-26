package com.niusworks.chatshop.commands;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.constructs.Item;
import com.niusworks.chatshop.constructs.Listing;
import com.niusworks.chatshop.managers.ChatManager;

/**
 * Executor for the "stock" command for OC Network's ChatShop.
 * <br>
 * Players can query the ChatShop for a complete listing of all items for sale from a
 * given player. Listings are ordered alphabetically by item name. This command takes
 * zero, one, or two arguments: player and page.
 * <br><br>
 * Player is resolved to a Minecraft UUID for comparison against the database. This will still work
 * with offline players, but long-time absentee players might turn up negative even if they have transactions
 * in the market history because of Spigot limitations. In this case the name that player had at the time the
 * listing was posted will be read from the database - though this name is not reliable.
 * <br>
 * If no player argument is supplied then the command will be executed on the calling player. Calling this command
 * on a player other than oneself does not require special permissions because, as is the whole purpose of this
 * plugin, a player's available stock is deliberately public information.
 * <br><br>
 * Page, optional, is an integer indicating which page of output to display. Very often there are
 * many listings available for a given player, and to prevent flooding the player's chat these listings
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
                playerCol + qPlayer.getName() +
                PLUGIN.CM.color("error") + ".");
        
        //Head the sales list.
        //Checking the page number is already taken care of by
        //ChatManager, but for purposes of displaying an accurate
        //number it needs to happen here.
        page = Math.max(page,1);
        page = Math.min(page,PLUGIN.CM.getPaginationSize(listings));
        String msg =
                textCol + "Listings for " +
                playerCol + qPlayer.getName() +
                textCol + ", page " + page +
                " of " + PLUGIN.CM.getPaginationSize(listings) + ":";
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
                qtyCol + ChatManager.format(listings[i].QUANTITY) + " " +
                itemCol + itemDisplay +
                textCol + " at " +
                priceCol + ChatManager.format(listings[i].PRICE) +
                textCol + " each.";
            PLUGIN.CM.reply(usr,msg,false);
        }
        
        return true;
    }
}
