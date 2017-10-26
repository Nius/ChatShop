package com.niusworks.chatshop.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.managers.ChatManager;
import com.niusworks.chatshop.managers.ItemManager;

/**
 * Executor for the "quote" command for OC Network's ChatShop.
 * <br>
 * Players can query the ChatShop for a price quote on a given quantity of a specified item.
 * This command has two elements: quantity and item.
 * <br><br>
 * Quantity must be an integer. If this number is more than is available on the market then
 * this command's output will indicate how much is available, and how much it would cost.
 * <br><br>
 * Item can be any ItemManager-recognized string representation of a Minecraft item as understood
 * by {@link ItemManager#parse}. Invalid items are caught and appropriate messages are sent to
 * the player.
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
public class Quote implements CommandExecutor
{
    /** Command usage. **/
    public static final String USAGE = "/quote <quantity> <item>";
    
    /** The specific instance of the parent ChatShop plugin. **/
    private final ChatShop PLUGIN;
    
    /**
     * Instantiate the command executor for "quote" commands.
     * 
     * @param master    The specific instance of the parent ChatShop plugin.
     */
    public Quote(ChatShop master)
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
            return PLUGIN.CM.reply(sender,"ChatShop.quote cannot be executed as console.");
        Player usr = (Player)sender;
        //Permissions
        if(!sender.hasPermission("chatshop.quote"))
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
        if(args.length != 2 && args.length != 3)
            return PLUGIN.CM.error(sender,USAGE);
        
        //Item check

        //If the specified item is non-specifically "potion" or some related query, show potions help instead.
        if( args[1].equalsIgnoreCase("potion")          || args[1].equalsIgnoreCase("potions")          ||
            args[1].equalsIgnoreCase("splashpotion")    || args[1].equalsIgnoreCase("splashpotions")    ||
            args[1].equalsIgnoreCase("lingeringpotion") || args[1].equalsIgnoreCase("lingeringpotions")    )
            return PLUGIN.getCommand("chatshop").getExecutor().onCommand(usr,cmd,"potions",new String[] {"0"});
        
        //Consult ItemManager to turn the user argument into a valid,
        //special-rules compliant item.
        Object parse = PLUGIN.IM.parse(usr,args[1]);
        if(parse instanceof Integer)
            switch((Integer)parse)
            {
                case -1: return PLUGIN.CM.error(usr,"You are not holding an item.");
                case -2:
                case -3: return PLUGIN.CM.error(usr,"Invalid item: " + PLUGIN.CM.color("item") + args[1] + PLUGIN.CM.color("error") + ".");
                case -4: return PLUGIN.CM.error(usr,
                        "Enchanted items must be bought by lot number. See "
                        + PLUGIN.CM.color("helpUsage") + "/ebuy"
                        + PLUGIN.CM.color("error") + ".");
                case -5: return PLUGIN.CM.error(usr,"Damaged items cannot be sold on the ChatShop.");
                case -6: return PLUGIN.CM.error(usr,"That item cannot be sold on the ChatShop.");
                default: return PLUGIN.CM.err500(usr);
            }
        ItemStack merchandise = (ItemStack)parse;
        String displayName = PLUGIN.IM.getDisplayName(merchandise);
        
        //Quantity check
        int qty = 0;
        try
        {
            qty = Integer.parseInt(args[0]);
            if(qty < 1)
                return PLUGIN.CM.error(sender,"You can't buy less than 1 of an item.");
        }
        catch(NumberFormatException e)
        {
            return PLUGIN.CM.error(sender,USAGE);
        }
        merchandise.setAmount(qty);
        
        //
        //  EXECUTION
        //  Deferred to DatabaseManager for synchronization purposes.
        //
        
        double total = PLUGIN.DB.price(usr,merchandise,-1);
        
        //
        //  RESULT
        //
        
        String textCol = PLUGIN.CM.color("text");
        
        //On fail...
        if(total == -1)
            return PLUGIN.CM.err500(usr);
        else if(total == 0)
        {
            return PLUGIN.CM.error(usr,
                    "No listings found for " +
                    PLUGIN.CM.color("item") + displayName +
                    PLUGIN.CM.color("error") + ".");
        }
        else if(total < 0)
        {
            merchandise.setAmount((int)(total * -1));
            double realtotal = PLUGIN.DB.price(usr,merchandise,-1);
            return PLUGIN.CM.reply(usr,
                textCol + "Currently only " +
                PLUGIN.CM.color("quantity") + ChatManager.format((int)(total * -1)) + " " +
                PLUGIN.CM.color("item") + displayName + " " +
                textCol + "for sale, totaling " +
                PLUGIN.CM.color("price") + ChatManager.format(realtotal).replaceAll("[()]","") +
                textCol + ".");
        }
        
        String msg =
            PLUGIN.CM.color("quantity") + ChatManager.format(merchandise.getAmount()) + " " +
            PLUGIN.CM.color("item") + displayName + " " +
            textCol + "would cost you a total of " +
            PLUGIN.CM.color("price") + ChatManager.format(total) +
            textCol + ".";
        PLUGIN.CM.reply(usr,msg);
        
        return true;
    }
}
