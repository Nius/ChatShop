package com.niusworks.chatshop.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.commands.Confirm.SellOrder;
import com.niusworks.chatshop.managers.DatabaseManager.Listing;
import com.niusworks.chatshop.managers.ChatManager;
import com.niusworks.chatshop.managers.DatabaseManager;
import com.niusworks.chatshop.managers.ItemManager;
import com.niusworks.chatshop.managers.ItemManager.Item;

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
        
        allowed = false;
        Object[] worlds = PLUGIN.getConfig().getList("allowed-worlds").toArray();
        for(int i = 0; i < worlds.length; i ++)
            if(worlds[i] instanceof String)
                if(((String)worlds[i]).equalsIgnoreCase(usr.getWorld().getName()))
                    allowed = true;
        if(!allowed)
            return PLUGIN.CM.denyWorld(sender);
        
        //
        //  VALIDATION
        //
        
        //Number of args
        if(args.length != 3)
            return PLUGIN.CM.error(sender,USAGE);
               
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
        Item cfg = PLUGIN.IM.lookup(merchandise);
        String displayName = cfg.DISPLAY;
        
        //Check whether the user has any of the specified item.
        int has = 0;
        for(ItemStack item : usr.getInventory().getContents())
            if(item == null)
                continue;
            else if(ItemManager.areSameItem(item,merchandise))
                has += item.getAmount();
        if(has == 0)
            return PLUGIN.CM.error(sender,"You do not have any " + displayName + ".");
        
        //Price check
        //User entry of "-" results in a price of -1, which resolves
        //  within DatabaseManager#sell to the current listed price.
        double price = 0;
        try
        {
            price = Double.parseDouble(args[2]);
            if(price < .01)
                return PLUGIN.CM.error(sender,
                    "No item may be priced lower than " +
                    PLUGIN.CM.color("price") + "$0.01" +
                    PLUGIN.CM.color("error") + ".");
            double globalmax = PLUGIN.getConfig().getDouble("global-max-price");
            if(price > globalmax)
                return PLUGIN.CM.error(sender,
                    "No item may be priced higher than " +
                    PLUGIN.CM.color("price") + ChatManager.format(globalmax) +
                    PLUGIN.CM.color("error") + ".");
            if(cfg.MAXPRICE > 0 && price > cfg.MAXPRICE)
                return PLUGIN.CM.error(sender,
                    "The maximum allowed price for " +
                    PLUGIN.CM.color("item") + cfg.DISPLAY +
                    PLUGIN.CM.color("error") + " is " + 
                    PLUGIN.CM.color("price") + ChatManager.format(cfg.MAXPRICE) +
                    PLUGIN.CM.color("error") + ".");
        } catch (NumberFormatException e)
        {
            if(args[2].equals("-"))
                price = -1;
            else
                return PLUGIN.CM.error(sender,USAGE);
        }
        
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
        //Check the quantity against the defined maximum quantity.
        //This check is performed again by the DatabaseManager when attempting
        // to carry out the sell operation, in the provided number is less than
        // the limit but the provided number plus the already-existing stock is
        // in excess of the limit.
        //In such a situation the DatabaseManager reports to ProcessResults, and
        // a denial identical to this one is returned.
        if(cfg.MAXQUANTITY > 0 && merchandise.getAmount() > cfg.MAXQUANTITY)
            return PLUGIN.CM.error(sender,
                "You may not offer more than " +
                PLUGIN.CM.color("quantity") + ChatManager.format(cfg.MAXQUANTITY) + " " +
                PLUGIN.CM.color("item") + cfg.DISPLAY +
                PLUGIN.CM.color("error") + " at a time.");
        
        //
        //  EXECUTION
        //  Deferred to DatabaseManager for synchronization purposes.
        //
        
        if(PLUGIN.DB.getPlayerFlag(usr,1) != 'X')
        {
           //The player is using /confirm for sells.
           SellOrder order = new SellOrder(usr,merchandise,cfg,price,System.currentTimeMillis());
           PLUGIN.PENDING.put(usr,order);
           String textCol = PLUGIN.CM.color("text");
           String msg =
               textCol + "Preparing to sell " +
               PLUGIN.CM.color("quantity") + merchandise.getAmount() + " " +
               PLUGIN.CM.color("item") + displayName + " " +
               textCol + "for " +
               PLUGIN.CM.color("price") + (price == -1 ?
                   "your currently listed price" :
                   PLUGIN.CM.color("price") + ChatManager.format(price)) +
               textCol + ".\n" + PLUGIN.CM.PREFIX +
               textCol + "Use " +
               PLUGIN.CM.color("helpUsage") + "/confirm " +
               textCol + "to confirm this order.";
           return PLUGIN.CM.reply(usr,msg);
        }
        else{} //The player is not using /confirm for buys.
           
        Object res = PLUGIN.DB.sell(usr,merchandise,price);
        
        //
        //  RESULT
        //
        
        return processResults(usr,merchandise,cfg,price,res);
    }
    
    /**
     * Process the results of a sell action.
     * This was originally contiguous from onCommand, but was
     * separated so that {@link Confirm} can also finalize
     * buy operations.
     * 
     * @param usr           The user who is executing the buy operaion.
     * @param merchandise   The merchandise (including amount) the user tried to buy.
     * @param cfg           The item, as configured from file.
     * @param price         The user-provided price for these items.
     * @param res           The results of {@link DatabaseManager#sell}.
     * @return              Always returns true, so that calling methods can finalize
     *                      the buy order and terminate in one line.
     */
    public boolean processResults(Player usr,ItemStack merchandise,Item cfg,double price,Object res)
    {
        // On SQL fail...
        if(res instanceof Integer && ((Integer)res).intValue() == -2)
            return PLUGIN.CM.err500(usr);
        
        // On updated listing exceeds quantity limit...
        if(res instanceof Integer && ((Integer)res).intValue() == -3)
            return PLUGIN.CM.error(usr,
                "You may not offer more than " +
                        PLUGIN.CM.color("quantity") + ChatManager.format(cfg.MAXQUANTITY) + " " +
                        PLUGIN.CM.color("item") + cfg.DISPLAY +
                        PLUGIN.CM.color("error") + " at a time.");
        
        // On "-" price but no listing found...
        if(res instanceof Integer && ((Integer)res).intValue() == -1)
            return PLUGIN.CM.error(usr,"You do not have any " + cfg.DISPLAY + " for sale and must specify a price.");
        
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
        broadcast += PLUGIN.CM.color("item") + cfg.DISPLAY;
        
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
