package com.niusworks.chatshop.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.managers.DatabaseManager.Listing;
import com.niusworks.chatshop.managers.ChatManager;
import com.niusworks.chatshop.managers.ItemManager;

/**
 * Executor for the "sell" command for
 * OC Network's ChatShop.
 * @author ObsidianCraft Staff
 */
public class Sell implements CommandExecutor
{
    /** Command usage. **/
    public static final String USAGE = "/sell <quantity> <item> <priceEach>";
    
    /** The specific instance of the parent ChatShop plugin. **/
    private final ChatShop PLUGIN;
    
    /**
     * Instantiate the command executor for "sell" commands.
     * 
     * @param master    The specific instance of the parent ChatShop plugin.
     */
    public Sell(ChatShop master)
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
            return PLUGIN.CM.reply(sender,"ChatShop.sell cannot be executed as console.");
        Player usr = (Player)sender;
        //Permissions
        if(!sender.hasPermission("chatshop.sell"))
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
        if(args.length != 3)
            return PLUGIN.CM.error(sender,USAGE);
               
        //Price check
        //User entry of "-" results in a price of -1, which resolves
        //  later to the current listed price.
        double price = 0;
        try
        {
            price = Double.parseDouble(args[2]);
            if(price < .01)
                return PLUGIN.CM.error(sender,"Minimum price is $0.01.");
        } catch (NumberFormatException e)
        {
            if(args[2].equals("-"))
                price = -1;
            else
                return PLUGIN.CM.error(sender,USAGE);
        }
        
        //Item check
        Object parse = PLUGIN.IM.parse(usr,args[1]);
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
        
        //Check whether the user has any of the specified item.
        int has = 0;
        for(ItemStack item : usr.getInventory().getContents())
            if(item == null)
                continue;
            else if(ItemManager.areSameItem(item,merchandise))
                has += item.getAmount();
        if(has == 0)
            return PLUGIN.CM.error(sender,"You do not have any " + displayName + ".");
        
        //Quantity Check
        //A user must enter a valid number greater than zero, or "all".
        if(args[0].equalsIgnoreCase("all"))
            merchandise.setAmount(has);
        else
        {
            try
            {
                merchandise.setAmount(Integer.parseInt(args[0]));
            } catch (NumberFormatException e) {
                return PLUGIN.CM.error(sender,USAGE);
            }
            if(merchandise.getAmount() < 1)
                return PLUGIN.CM.error(sender,"You must sell at least one item.");
        }
        
        //
        //  EXECUTION
        //  Deferred to DatabaseManager for synchronization purposes.
        //
        
        Object res = PLUGIN.DB.sell(usr,merchandise,price);
        
        //
        //  RESULT
        //
        
        // On fail...
        if(res instanceof Integer && ((Integer)res).intValue() == -2)
            return PLUGIN.CM.err500(sender);
        
        // On "-" price but no listing found...
        if(res instanceof Integer && ((Integer)res).intValue() == -1)
            return PLUGIN.CM.error(sender,"You do not have any " + displayName + " for sale and must specify a price.");
        
        //Remove the specified items from the player's inventory.
        int removed = 0;
        ItemStack[] inv = usr.getInventory().getContents();
        for(int i = 0; i < inv.length; i ++)
            if(ItemManager.areSameItem(inv[i],merchandise))
            {
                //If this slot has more than needs to be removed, trim it
                // and quit.
                if(merchandise.getAmount() - removed < inv[i].getAmount())
                {
                    inv[i].setAmount(inv[i].getAmount() - (merchandise.getAmount() - removed));
                    usr.getInventory().setItem(i,inv[i]);
                    break;
                }
                
                //(Otherwise,) enough still needs to be removed that this
                // whole slot can be emptied, so empty it.
                removed += inv[i].getAmount();
                usr.getInventory().clear(i);
            }
        
        String textColor = PLUGIN.CM.color("text");
        String qColor = PLUGIN.CM.color("quantity");
        
        // Construct a broadcast message.
        
        if(!PLUGIN.getConfig().getBoolean("chat.broadcast-offers"))
            return true;
        
        String broadcast = PLUGIN.CM.color("player") + usr.getName() + " " +
                textColor + "is selling " + qColor;
        
        // Indicate the appropriate quantity.
        if(res instanceof Listing)  // A previous listing existed and was ammended.
        {
            broadcast += ChatManager.format(((Listing)res).QUANTITY + merchandise.getAmount()) + " "
                + ChatColor.GREEN + "(\u25B2" + merchandise.getAmount() + ") ";
            if(price == -1)
                price = ((Listing)res).PRICE;
        }
        else                        // No listing previously existed.
            broadcast += ChatManager.format(merchandise.getAmount()) + " ";
        
        // Indicate the appropriate item.
        broadcast += PLUGIN.CM.color("item") + displayName;
        
        broadcast += textColor + " for ";
        
        // Indicate the appropriate price.
        broadcast += PLUGIN.CM.color("price") + ChatManager.format(price) + " ";
        if(res instanceof Listing)  // A previous listing existed and was repriced.
        {
            if(price > ((Listing)res).PRICE)
                broadcast += ChatColor.RED + "(\u25B2" + ChatManager.format(price - ((Listing)res).PRICE) + ") ";
            else if(price < ((Listing)res).PRICE)
                broadcast += ChatColor.GREEN + "(\u25BC" + ChatManager.format(((Listing)res).PRICE - price) + ") ";
        }
        
        broadcast += textColor + "each.";
        
        PLUGIN.CM.broadcast(broadcast);
        return true;
    }
}
