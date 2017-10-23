package com.niusworks.chatshop.commands;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.constructs.EListing;
import com.niusworks.chatshop.constructs.EnchLvl;
import com.niusworks.chatshop.managers.ChatManager;
import com.niusworks.chatshop.managers.ItemManager;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Executor for the "efind" and "ef" commands for OC Network's ChatShop.
 * <br>
 * Players can search the market for an item, showing all listings of its prices and sellers.
 * Listings are ordered by price ascending. This command has a dynamic number of elements.
 * <br><br>
 * Item can be any ItemManager-recognized string representation of a Minecraft item as understood
 * by {@link ItemManager#parse}. Invalid items are caught and appropriate messages are sent to
 * the player.
 * Attempting to efind a non-enchantable item will result in an empty set: "no listings found".
 * <br>
 * Alternatively, if the item is an integer and there are no other args, then the lot number
 * specified will be shown, if it exists.
 * <br><br>
 * Users can specify any number of enchantments that they desire of an object.
 * To specify an enchant, the user must use this format: "STRING-X", where X is a number
 * indicating the level of the enchantment and STRING is a string of their choosing which
 * matches exactly one enchantment name, as defined by ItemManager.
 * X may be an integer or a roman numeral.
 * <br><br>
 * Page, optional, is an integer indicating which page of output to display. Very often there are
 * many listings available for a given item, and to prevent flooding the player's chat these listings
 * are divided into "pages" by the {@link ChatManager}. Only the specified page is shown. If no
 * page number is given then the first page will be shown.
 * Page must be the last argument.
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
public class EFind implements CommandExecutor
{
    /** Command usage. **/
    public static final String USAGE = "/efind <item> [enchant [enchant [...]]] [page]";
    
    /** The specific instance of the parent ChatShop plugin. **/
    private final ChatShop PLUGIN;
    
    /**
     * Instantiate the command executor for "efind" and "ef" commands.
     * 
     * @param master    The specific instance of the parent ChatShop plugin.
     */
    public EFind(ChatShop master)
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
            return PLUGIN.CM.reply(sender,"ChatShop.efind cannot be executed as console.");
        Player usr = (Player)sender;
        //Permissions
        if(!sender.hasPermission("chatshop.efind"))
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
        if(args.length < 1)
            return PLUGIN.CM.error(sender,USAGE);
        
        //Check to see if there is only one arg and it's an integer.
        //  If so, we're just looking up that specific lot.
        EListing lotlisting = null;
        if(args.length == 1)
        {
            try
            {
                int lot = Integer.parseInt(args[0]);
                lotlisting = PLUGIN.DB.getEListing(lot);
            }
            catch(NumberFormatException e){/* do nothing */}
        }
        
        //Item check
        Object parse = PLUGIN.IM.parse(usr,
            (lotlisting == null ? args[0] : lotlisting.MATERIAL));
        if(parse instanceof Integer)
            switch((Integer)parse)
            {
                case -1: return PLUGIN.CM.error(usr,"You are not holding an item.");
                case -2:
                case -3: return PLUGIN.CM.error(usr,"Invalid item: " + PLUGIN.CM.color("item") + args[0] + PLUGIN.CM.color("error") + ".");
                case -6: return PLUGIN.CM.error(usr,"That item cannot be sold on the ChatShop.");
                default: return PLUGIN.CM.err500(usr);
            }
        ItemStack merchandise = (ItemStack)parse;
        String displayName = PLUGIN.IM.getDisplayName(merchandise);
        
        //Page check
        //The page must be the last argument, which will be an integer only
        //  if the user is specifying a page.
        int page = 1;
        boolean didSpecifyPage = false;
        if(args.length > 1)
            try
            {
                page = Integer.parseInt(args[args.length - 1]);
                didSpecifyPage = true;
            }
            catch(NumberFormatException e)
            { /* Do nothing; the last arg is probably an enchant. */}
        
        //Resolve remaining args to enchantments.
        EnchLvl[] enchs =
            new EnchLvl[args.length - (didSpecifyPage ? 2 : 1)];
        for(int i = 0; i < enchs.length; i ++)
        {
            Object res = PLUGIN.IM.resolveEnchantment(args[i + 1]);
            if(res instanceof Integer)
                switch(((Integer) res).intValue())
                {
                    case -1: return PLUGIN.CM.error(usr,args[i+1] + " doesn't make sense. Please use NAME-LVL, such as Eff-2.");
                    case -2: return PLUGIN.CM.error(usr,args[i+1] + " doesn't match any known enchantment.");
                    case -3: return PLUGIN.CM.error(usr,args[i+1] + " matches more than one enchantment. Please be more specific.");
                    case -4: return PLUGIN.CM.error(usr,args[i+1] + " is too high a level for that enchantment type.");
                }
            enchs[i] = (EnchLvl)res;
        }
        
        //
        //  EXECUTION
        //
        
        EListing[] listings =
            (lotlisting == null ?
                PLUGIN.DB.getListings(merchandise,enchs) :
                new EListing[]{lotlisting});
        
        //
        //  RESULT
        //
        
        //Getting colors is fairly expensive, so do it once on
        //execution rather than once per line of output.
        String textCol = PLUGIN.CM.color("text");
        String itemCol = PLUGIN.CM.color("item");
        String priceCol = PLUGIN.CM.color("price");
        String playerCol = PLUGIN.CM.color("player");
        
        //On SQL fail...
        if(listings == null)
            return PLUGIN.CM.err500(usr);
        //On no listings...
        if(listings.length == 0)
            return PLUGIN.CM.error(usr,"No listings found.");
        
        //Head the sales list.
        //Checking the page number is already taken care of by
        //ChatManager, but for purposes of displaying an accurate
        //number it needs to happen here.
        //If the user has queried a single lot, no such header
        //is necessary.
        if(lotlisting == null)
        {
            page = Math.max(page,1);
            int pages = PLUGIN.CM.getPaginationSize(listings);
            String msg =
                    textCol + "Listings for " +
                    itemCol +
                    (merchandise.getType().equals(Material.ENCHANTED_BOOK) ? "" : "enchanted ") +
                    displayName +
                    textCol + ", page " + Math.min(page,pages) +
                    " of " + pages + ":";
            PLUGIN.CM.reply(usr,msg);
        }
        
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
            
            //Build the item in question
            ItemStack itm = new ItemStack(Material.getMaterial(listings[i].MATERIAL));
            itm.setDurability((short)listings[i].DAMAGE);
            ItemManager.addEnchantments(itm,listings[i].ENCHANTS);
            
            //Build the chat message.
            TextComponent tc0 = new TextComponent();
            tc0.setText(//If the user is querying a single lot, add the
                        //  prefix so it doesn't look weird.
                        (lotlisting != null ? PLUGIN.CM.PREFIX : "") +
                        priceCol + ChatManager.format(listings[i].PRICE) +
                        textCol + ", lot ");
            TextComponent tc1 = PLUGIN.CM.MOTforEnchanted(
                itemCol + 
                (lotlisting != null ?
                    (merchandise.getType().equals(Material.ENCHANTED_BOOK) ? "" : "enchanted ") + displayName :
                    "lot #" + listings[i].ID),
                listings[i].ID,itm);
            TextComponent tc2 = new TextComponent();
            tc2.setText(textCol + " from " +
                        playerCol + playerName +
                        textCol + ".");
            usr.spigot().sendMessage(tc0,tc1,tc2);
        }
        return true;
    }
}
