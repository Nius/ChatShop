package com.niusworks.chatshop.commands;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.constructs.Item;
import com.niusworks.chatshop.constructs.Listing;
import com.niusworks.chatshop.managers.ChatManager;
import com.niusworks.chatshop.managers.ItemManager;

import net.md_5.bungee.api.ChatColor;

/**
 * Executor for the "reprice" command for OC Network's ChatShop.
 * <br>
 * Players can change the prices of their existing listings on the chatshop. This command
 * has two elements: item and newprice.
 * <br><br>
 * Item can be any ItemManager-recognized string representation of a Minecraft item as understood
 * by {@link ItemManager#parse}. Invalid items are caught and appropriate messages are sent to
 * the player. Items not currently listed on the market by this player are likewise refused.
 * <br><br>
 * Newprice is a double value indicating the price PER ITEM (not total) to assign the listing.
 * Price limits set forth in items.csv are honored here; attempting to reprice an item above its
 * configured maximum price will result in a refusal message.
 * <br><br>
 * Upon successful sale a broadcast is sent out notifying players of the newly updated listing.
 * <br><br>
 * This command has the following limits (aside from basic perms):
 * <ul>
 * <li>Console access denied.
 * <li>World must be whitelisted in config OR config must allow querying from anyone (see below).
 * <li>Gamemode must be whitelisted in config OR config must allow querying from anyone (see below).
 * <li>General freeze prevents command.
 * </ul>
 * This command conducts modification of items already on the market. Because players cannot use this command
 * to place new things on or withrdaw things from the market, world and gamemode controls are lifted for this
 * command. Should this be deemed inappropriate for a server's needs, administrators can configuratively
 * disable this liberty.
 * <br><br>
 * @author ObsidianCraft Staff
 */
public class Reprice implements CommandExecutor
{
    /** Command usage. **/
    public static final String USAGE = "/reprice <item> <newPrice>";
    
    /** The specific instance of the parent ChatShop plugin. **/
    private final ChatShop PLUGIN;
    
    /**
     * Instantiate the command executor for "reprice" commands.
     * 
     * @param master    The specific instance of the parent ChatShop plugin.
     */
    public Reprice(ChatShop master)
    {
        PLUGIN = master;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String lbl, String[] args)
    {
        return execute(sender,null,cmd,lbl,args);
    }
    
    /**
     * Execute the reprice command.
     * This was originally contiguous with {@link #onCommand} but has been separated to support
     * forced execution by proxy via CSAdmin.
     * 
     * @param issuer    The player that issued the command - either for himself or by proxy.
     * @param target    The player upon whose stock this command is being executed. This will be
     *                  null if the player is calling this command on himself.
     * @param cmd       The literal of the executed command.
     * @param lbl       The alias used to execute this command.
     * @param args      The command arguments to be parsed and validated.
     * @return          Always returns true.
     */
    public boolean execute(CommandSender issuer, OfflinePlayer target, Command cmd, String lbl, String[] args)
    {
        //
        // Denial of service conditions
        //
        
        //No console
        if(!(issuer instanceof Player))
            return PLUGIN.CM.reply(issuer,"ChatShop.reprice cannot be executed as console.");
        Player usr = (Player)issuer;
        //Permissions
        if(!issuer.hasPermission("chatshop.reprice"))
            return PLUGIN.CM.denyPermission(issuer);
        if(!PLUGIN.getConfig().getBoolean("query-anyone"))
        {
            //Gamemode
            Object[] modes = PLUGIN.getConfig().getList("allowed-modes").toArray();
            boolean allowed = false;
            for(int i = 0; i < modes.length; i ++)
                if(modes[i] instanceof String)
                    if(((String)modes[i]).equalsIgnoreCase(usr.getGameMode().toString()))
                        allowed = true;
            if(!allowed)
                return PLUGIN.CM.denyGameMode(issuer);
            //World
            allowed = false;
            Object[] worlds = PLUGIN.getConfig().getList("allowed-worlds").toArray();
            for(int i = 0; i < worlds.length; i ++)
                if(worlds[i] instanceof String)
                    if(((String)worlds[i]).equalsIgnoreCase(usr.getWorld().getName()))
                        allowed = true;
            if(!allowed)
                return PLUGIN.CM.denyWorld(issuer);
        }
        //General freeze
        if(PLUGIN.DB.isGeneralFreeze())
            return PLUGIN.CM.denyGeneralFreeze(usr);
        
        //
        //  VALIDATION
        //
        
        //Number of args
        if(args.length != 2)
            return PLUGIN.CM.error(issuer,USAGE);
               
        //Item check

        //If the first arg is an integer value preceded by a hash then they
        //  probably meant to use ereprice, so redirect to EReprice.
        if(args[0].charAt(0) == '#')
            try
            {
                int lot = Integer.parseInt(args[0].substring(1));
                return PLUGIN.getCommand("ereprice").getExecutor().onCommand(usr,cmd,lbl,new String[]{lot + "",args[1]});
            }
            catch(NumberFormatException e){/* do nothing */}
        
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
                case -3: return PLUGIN.CM.error(usr,"Invalid item: " + PLUGIN.CM.color("item") + args[1] + PLUGIN.CM.color("error") + ".");
                case -4: return PLUGIN.CM.error(usr,
                        "Enchanted items must be repriced by lot number. See "
                        + PLUGIN.CM.color("helpUsage") + "/ereprice"
                        + PLUGIN.CM.color("error") + ".");
                case -5: return PLUGIN.CM.error(usr,"Damaged items cannot be sold on the ChatShop.");
                case -6: return PLUGIN.CM.error(usr,"That item cannot be sold on the ChatShop.");
                default: return PLUGIN.CM.err500(usr);
            }
        ItemStack merchandise = (ItemStack)parse;
        Item cfg = PLUGIN.IM.lookup(merchandise);
        String displayName = cfg.DISPLAY;
        
        //Price check
        double price = 0;
        try
        {
            price = Double.parseDouble(args[1]);
            if(price < .01)
                return PLUGIN.CM.error(issuer,"Minimum price is $0.01.");
            double globalmax = PLUGIN.getConfig().getDouble("global-max-price");
            if(price > globalmax)
                return PLUGIN.CM.error(issuer,
                    "No item may be priced higher than " +
                    PLUGIN.CM.color("price") + ChatManager.format(globalmax) +
                    PLUGIN.CM.color("error") + ".");
            if(cfg.MAXPRICE > 0 && price > cfg.MAXPRICE)
                return PLUGIN.CM.error(issuer,
                        "The maximum allowed price for " +
                        PLUGIN.CM.color("item") + cfg.DISPLAY +
                        PLUGIN.CM.color("error") + " is " + 
                        PLUGIN.CM.color("price") + ChatManager.format(cfg.MAXPRICE) +
                        PLUGIN.CM.color("error") + ".");
        } catch (NumberFormatException e)
        {
            return PLUGIN.CM.error(issuer,USAGE);
        }
        
        //
        //  EXECUTION
        //  Deferred to DatabaseManager for synchronization purposes.
        //
        
        //Ternary operation is to determine whether this command is being executed on oneself
        //  or by proxy (admin).
        Object res = PLUGIN.DB.reprice(
            (target == null ? usr : target),
            merchandise,price);
        
        //
        //  RESULT
        //
        
        // On fail...
        if(res instanceof Integer && ((Integer)res).intValue() == -1)
            return PLUGIN.CM.err500(issuer);
        
        // On "-" price but no listing found...
        if(res instanceof Integer && ((Integer)res).intValue() == 0)
            return PLUGIN.CM.error(issuer,
                (target == null ? "You do " : "This player does ") +
                    "not have any " + displayName + " for sale.");
        
        String textColor = PLUGIN.CM.color("text");
        String qColor = PLUGIN.CM.color("quantity");
        
        // Construct a broadcast message.
        
        if(!PLUGIN.getConfig().getBoolean("chat.broadcast-offers"))
            return true;
        
        String broadcast = PLUGIN.CM.color("player") +
                (target == null ? usr : target).getName() +
                " " +
                textColor + "is selling " + qColor;
        
        // Indicate the appropriate quantity.
        broadcast += ChatManager.format(((Listing)res).QUANTITY) + " ";
        
        // Indicate the appropriate item.
        broadcast += PLUGIN.CM.color("item") + displayName;
        
        broadcast += textColor + " for ";
        
        // Indicate the appropriate price.
        broadcast += PLUGIN.CM.color("price") + ChatManager.format(price) + " ";
        if(price > ((Listing)res).PRICE)
            broadcast += ChatColor.RED + "(\u25B2" + ChatManager.format(price - ((Listing)res).PRICE) + ") ";
        else if(price < ((Listing)res).PRICE)
            broadcast += ChatColor.GREEN + "(\u25BC" + ChatManager.format(((Listing)res).PRICE - price) + ") ";
        
        broadcast += textColor + "each.";
        
        PLUGIN.CM.broadcast(broadcast);
        return true;
    }
}