package com.niusworks.chatshop.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.managers.DatabaseManager.Tender;

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
            return PLUGIN.CM.reply(sender,"ChatShop.dump cannot be executed as console.");
        Player usr = (Player)sender;
        //Permissions
        if(!sender.hasPermission("chatshop.dump"))
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
                PLUGIN.CM.color("text") + args[1].substring(0,1).toUpperCase() + args[1].substring(1) +
                " confirmations are now " +
                (wasOn ? ChatColor.RED : ChatColor.GREEN) +
                (wasOn ? "off" : "on") +
                PLUGIN.CM.color("text") + ".";
            PLUGIN.CM.reply(usr,msg);
            return true;
        }
        
        //
        //  EXECUTION: CONFIRM
        //
        
        //Stop if no orders are pending.
        if(!PLUGIN.PENDING.containsKey(usr))
            return PLUGIN.CM.error(usr,"You have no pending buy or sell orders.");
        
        //Stop if the pending order is older than 5 seconds.
        Order pending = PLUGIN.PENDING.get(usr);
        if(System.currentTimeMillis() - pending.TIME > 5000)
            return PLUGIN.CM.error(usr,"Your pending order has expired.");
        
        //Execute the pending order appropriately, then defer to the normal CommandExecutor of that order
        //  to finalize the action.
        
        if(pending instanceof BuyOrder)
        {
            Tender res = PLUGIN.DB.buy(usr,pending.MERCH,((BuyOrder) pending).MAXP);
            return ((Buy)PLUGIN.getCommand("buy").getExecutor()).processResults(usr,pending.MERCH,pending.DISPLAY,res);
        }
        if(pending instanceof SellOrder)
        {
            Object res = PLUGIN.DB.sell(usr,pending.MERCH,((SellOrder) pending).PRICE);
            return ((Sell)PLUGIN.getCommand("sell").getExecutor()).processResults(usr,pending.MERCH,pending.DISPLAY,((SellOrder) pending).PRICE,res);
        }
        
        return true;
    }
    
    /**
     * A simple vehicle for storing a pending buy or sell order.
     * @author ObsidianCraft Staff
     */
    public static abstract class Order
    {
        /** The player who created this order. **/
        public final Player PLAYER;
        /** The merchandise (including quantity) to purchse. **/
        public final ItemStack MERCH;
        /** The already-looked-up display name of this item. **/
        public final String DISPLAY;
        /** The time at which this order was created. **/
        public final long TIME;
        
        /**
         * @param usr       The player who created this order.
         * @param merch     The merchandise (including quantity) to purchse.
         * @param display   The already-looked-up display name of this item.
         * @param time      The time at which this order was created.
         */
        public Order(Player usr,ItemStack merch,String display,long time)
        {
            PLAYER = usr; MERCH = merch; DISPLAY = display; TIME = time;
        }
    }
    
    /**
     * A simple vehicle for storing a pending sell order.
     * @author ObsidianCraft Staff
     */
    public static class SellOrder extends Order
    {
        /** The price per item for this sale. **/
        public final double PRICE;
        
        /**
         * @param usr       The player who created this order.
         * @param merch     The merchandise (including quantity) to purchse.
         * @param display   The already-looked-up display name of this item.
         * @param price     The price per item for this sale.
         * @param time      The time at which this order was created.
         */
        public SellOrder(Player usr,ItemStack merch,String display,double price,long time)
        {
            super(usr,merch,display,time);
            PRICE = price;
        }
    }
    
    /**
     * A simple vehicle for storing a pending buy order.
     * @author ObsidianCraft Staff
     */
    public static class BuyOrder extends Order
    {
        /** The maximum price for the buy order. **/
        public final double MAXP;
        /** The calculated total price for the order at the time of its creation. **/
        public final double TOTAL;
        
        /**
         * @param usr    The player who created this order.
         * @param merch   The merchandise (including quantity) to purchse.
         * @param display The already-looked-up display name of this item.
         * @param maxp    The maximum price for the buy order.
         * @param total   The calculated total price for the buy order at the time of its creation.
         * @param time    The time at which this order was created.
         */
        public BuyOrder(Player usr,ItemStack merch,String display,double maxp,double total,long time)
        {
            super(usr,merch,display,time);
            MAXP = maxp; TOTAL = total;
        }
    }
}