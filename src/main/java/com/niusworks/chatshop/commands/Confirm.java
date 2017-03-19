package com.niusworks.chatshop.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.constructs.BuyOrder;
import com.niusworks.chatshop.constructs.Order;
import com.niusworks.chatshop.constructs.SellOrder;
import com.niusworks.chatshop.constructs.Tender;

import net.md_5.bungee.api.ChatColor;

/**
 * Executor for the "confirm" command for
 * OC Network's ChatShop.
 * @author ObsidianCraft Staff
 */
public class Confirm implements CommandExecutor
{
    /** Command usage. **/
    public static final String USAGE = "/confirm [toggle <buy|sell>]";
    
    /** The specific instance of the parent ChatShop plugin. **/
    private final ChatShop PLUGIN;
    
    /**
     * Instantiate the command executor for "confirm" commands.
     * 
     * @param master    The specific instance of the parent ChatShop plugin.
     */
    public Confirm(ChatShop master)
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
            return PLUGIN.CM.reply(sender,"ChatShop.confirm cannot be executed as console.");
        Player usr = (Player)sender;
        //Permissions
        if(!sender.hasPermission("chatshop.confirm"))
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
        //World
        allowed = false;
        Object[] worlds = PLUGIN.getConfig().getList("allowed-worlds").toArray();
        for(int i = 0; i < worlds.length; i ++)
            if(worlds[i] instanceof String)
                if(((String)worlds[i]).equalsIgnoreCase(usr.getWorld().getName()))
                    allowed = true;
        if(!allowed)
            return PLUGIN.CM.denyWorld(sender);
        //General freeze
        if(PLUGIN.DB.isGeneralFreeze())
            return PLUGIN.CM.denyGeneralFreeze(usr);
        
        //
        //  VALIDATION
        //
        
        //Number of args
        boolean toggleMode;
        if(args.length == 0)
            toggleMode = false;
        else if(args.length == 2)
            toggleMode = true;
        else
            return PLUGIN.CM.error(usr,USAGE);
        
        //Args validation
        
        //This char is used to signify which command (buy/sell) is being toggled.
        //This is not the char that will be put in the database.
        char mode = 'x';
        if(toggleMode)
        {
            if(!args[0].equalsIgnoreCase("toggle"))
                return PLUGIN.CM.error(usr,USAGE);
            
            if(args[1].equalsIgnoreCase("buy"))
                mode = 'b';
            else if(args[1].equalsIgnoreCase("sell"))
                mode = 's';
            else
                return PLUGIN.CM.error(usr,USAGE);
        }
        
        //
        //  EXECUTION: TOGGLE
        //
        
        if(toggleMode)
        {
            int index = -1;
            switch(mode)
            {
                case 'b':
                    index = 0;
                    break;
                case 's':
                    index = 1;
                    break;
            }
            boolean wasOn = PLUGIN.DB.getPlayerFlag(usr,index) != 'X';
            PLUGIN.DB.writePlayerFlag(usr,index,(wasOn ? 'X' : ' '));
            
            String msg =
                PLUGIN.CM.color("helpUsage") + args[1].substring(0,1).toUpperCase() + args[1].substring(1) +
                PLUGIN.CM.color("text") + " confirmations are now " +
                (wasOn ? ChatColor.RED : ChatColor.GREEN) +
                (wasOn ? "off" : "on") +
                PLUGIN.CM.color("text") + ".";
            return PLUGIN.CM.reply(usr,msg);
        }
        
        //
        //  EXECUTION: CONFIRM
        //
        
        //Stop if no orders are pending.
        if(!PLUGIN.PENDING.containsKey(usr))
            return PLUGIN.CM.error(usr,"You have no pending buy or sell orders.");
        
        //Stop if the pending order is older than 5 seconds.
        Order pending = PLUGIN.PENDING.get(usr);
        if(System.currentTimeMillis() - pending.TIME > PLUGIN.getConfig().getInt("confirm-timeout",10000))
            return PLUGIN.CM.error(usr,"Your pending order has expired.");
        
        //Execute the pending order appropriately, then defer to the normal CommandExecutor of that order
        //  to finalize the action.
        
        //First, check for a general freeze.
        if(PLUGIN.DB.isGeneralFreeze())
            return PLUGIN.CM.denyGeneralFreeze(usr);
        
        if(pending instanceof BuyOrder)
        {
            Tender res = PLUGIN.DB.buy(usr,pending.MERCH,((BuyOrder) pending).MAXP);
            return ((Buy)PLUGIN.getCommand("buy").getExecutor()).processResults(usr,pending.MERCH,pending.CONFIG.DISPLAY,res);
        }
        if(pending instanceof SellOrder)
        {
            Object res = PLUGIN.DB.sell(usr,pending.MERCH,((SellOrder) pending).PRICE);
            return ((Sell)PLUGIN.getCommand("sell").getExecutor()).processResults(usr,pending.MERCH,pending.CONFIG,((SellOrder) pending).PRICE,res);
        }
        
        return true;
    }
}