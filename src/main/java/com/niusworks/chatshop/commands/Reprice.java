package com.niusworks.chatshop.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.managers.ChatManager;
import com.niusworks.chatshop.managers.DatabaseManager.Listing;

import net.md_5.bungee.api.ChatColor;

/**
 * Executor for the "reprice" command for
 * OC Network's ChatShop.
 * @author ObsidianCraft Staff
 */
public class Reprice implements CommandExecutor
{
    /** Command usage. **/
    public static final String USAGE = "/reprice <item> <newprice>";
    
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
        //
        // Denial of service conditions
        //
        
        //No console
        if(!(sender instanceof Player))
            return PLUGIN.CM.reply(sender,"ChatShop.reprice cannot be executed as console.");
        Player usr = (Player)sender;
        //Permissions
        if(!sender.hasPermission("chatshop.reprice"))
            return PLUGIN.CM.denyPermission(sender);
        //Gamemode
        Object[] modes = PLUGIN.getConfig().getList("allowed-modes").toArray();
        boolean allowed = false;
        for(int i = 0; i < modes.length; i ++)
            if(modes[i] instanceof String)
                if(((String)modes[i]).equalsIgnoreCase(usr.getGameMode().toString()))
                    allowed = true;
        if(!allowed)
            return PLUGIN.CM.denyGameMode(sender);
        
        //
        //  VALIDATION
        //
        
        //Number of args
        if(args.length != 2)
            return PLUGIN.CM.error(sender,USAGE);
               
        //Price check
        double price = 0;
        try
        {
            price = Double.parseDouble(args[1]);
            if(price < .01)
                return PLUGIN.CM.error(sender,"Minimum price is $0.01.");
            double globalmax = PLUGIN.getConfig().getDouble("global-max-price");
            if(price > globalmax)
                return PLUGIN.CM.error(sender,"Maximum price is " + ChatManager.format(globalmax) + ".");
        } catch (NumberFormatException e)
        {
            return PLUGIN.CM.error(sender,USAGE);
        }
        
        //Item check
        Object parse = PLUGIN.IM.parse(usr,args[0]);
        if(parse instanceof Integer)
            switch((Integer)parse)
            {
                case -1: return PLUGIN.CM.error(usr,"You are not holding an item.");
                case -2:
                case -3: return PLUGIN.CM.error(usr,"Invalid item: " + PLUGIN.CM.color("item") + args[1] + PLUGIN.CM.color("error") + ".");
                default: return PLUGIN.CM.err500(usr);
            }
        ItemStack merchandise = (ItemStack)parse;
        String displayName = PLUGIN.IM.getDisplayName(merchandise);
        
        //
        //  EXECUTION
        //  Deferred to DatabaseManager for synchronization purposes.
        //
        
        Object res = PLUGIN.DB.reprice(usr,merchandise,price);
        
        //
        //  RESULT
        //
        
        // On fail...
        if(res instanceof Integer && ((Integer)res).intValue() == -1)
            return PLUGIN.CM.err500(sender);
        
        // On "-" price but no listing found...
        if(res instanceof Integer && ((Integer)res).intValue() == 0)
            return PLUGIN.CM.error(sender,"You do not have any " + displayName + " for sale.");
        
        String textColor = PLUGIN.CM.color("text");
        String qColor = PLUGIN.CM.color("quantity");
        
        // Construct a broadcast message.
        
        if(!PLUGIN.getConfig().getBoolean("chat.broadcast-offers"))
            return true;
        
        String broadcast = PLUGIN.CM.color("player") + usr.getName() + " " +
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