package com.niusworks.chatshop.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.constructs.BuyOrder;
import com.niusworks.chatshop.constructs.Item;
import com.niusworks.chatshop.constructs.Tender;
import com.niusworks.chatshop.managers.ChatManager;
import com.niusworks.chatshop.managers.DatabaseManager;
import com.niusworks.chatshop.managers.ItemManager;

/**
 * Executor for the "buy" command for OC Network's ChatShop.
 * <br>
 * Players can buy items from the chatshop. Purchases are always made in ascending order by price;
 * the ChatShop always buys the cheapest available items. This command has three elements: quantity,
 * item, and maxPrice.
 * <br><br>
 * Quantity must be an integer greater than zero.
 * <br><br>
 * Item can be any ItemManager-recognized string representation of a Minecraft item as understood
 * by {@link ItemManager#parse}. Invalid items are caught and appropriate messages are sent to
 * the player.
 * <br><br>
 * MaxPrice, optional, is the maximum price the player is willing to pay for the merchandise. When
 * querying the database, listings for greater than the specified maxPrice are ignored as if they
 * don't exist. Thus, a player might be informed that the shop is out of stock for an item when
 * that item still is for sale, if the entirety of the stock is above the specified price threshhold.
 * <br><br>
 * On execution of this command the database will be checked to see whether the user is employing buy
 * confirmations. If so, a {@link BuyOrder} will be created and posted, which is then under the
 * jurisdiction of {@link Confirm}. If not, the buy operation will be executed immediately.
 * <br><br>
 * This command has the following limits (aside from basic perms):
 * <ul>
 * <li>Console access denied.
 * <li>World must be whitelisted in config.
 * <li>Gamemode must be whitelisted in config.
 * <li>General freeze prevents command.
 * </ul>
 * @author ObsidianCraft Staff
 */
public class Buy implements CommandExecutor
{
    /** Command usage. **/
    public static final String USAGE = "/buy <quantity> <item> [maxPrice]";
    
    /** The specific instance of the parent ChatShop plugin. **/
    private final ChatShop PLUGIN;
    
    /**
     * Instantiate the command executor for "buy" commands.
     * 
     * @param master    The specific instance of the parent ChatShop plugin.
     */
    public Buy(ChatShop master)
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
            return PLUGIN.CM.reply(sender,"ChatShop.buy cannot be executed as console.");
        Player usr = (Player)sender;
        //Permissions
        if(!sender.hasPermission("chatshop.buy"))
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
        if(args.length != 2 && args.length != 3)
            return PLUGIN.CM.error(sender,USAGE);
        
        //Maxprice check
        //If no max price is submitted, -1 is used to signify so.
        double maxp = -1;
        if(args.length == 3)
            try
            {
                maxp = Double.parseDouble(args[2]);
            }
            catch(NumberFormatException e)
            {
                return PLUGIN.CM.error(sender,USAGE);
            }
        
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
                case -4: return PLUGIN.CM.error(usr,"Enchanted items cannot be sold on the ChatShop.");
                case -5: return PLUGIN.CM.error(usr,"Damaged items cannot be sold on the ChatShop.");
                case -6: return PLUGIN.CM.error(usr,"That item cannot be sold on the ChatShop.");
                default: return PLUGIN.CM.err500(usr);
            }
        ItemStack merchandise = (ItemStack)parse;
        //Get the item configruation as defined in items.csv
        Item cfg = PLUGIN.IM.lookup(merchandise);
        String displayName = cfg.DISPLAY;
        
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
        //  The database manager is responsible for notifying players
        //    who seed this purchase, so that (1) a list of listings
        //    does not need to be passed to this Buy instance and
        //    (2) such a list does not need to be traversed more than
        //    once.
        //
        
        if(PLUGIN.DB.getPlayerFlag(usr,0) != 'X')
        {
           //The player is using /confirm for buys, so instead of executing a buy
           //create a buy order and store it. The buy order is then under the jurisdiction
           //of /confirm.
           double tprice = PLUGIN.DB.price(usr,merchandise,maxp);
           BuyOrder order = new BuyOrder(usr,merchandise,cfg,maxp,tprice,System.currentTimeMillis());
           PLUGIN.PENDING.put(usr,order);
           
           String textCol = PLUGIN.CM.color("text");
           String msg =
               textCol + "Preparing to buy " +
               PLUGIN.CM.color("quantity") + merchandise.getAmount() + " " +
               PLUGIN.CM.color("item") + displayName + " " +
               textCol + "for a total of " +
               PLUGIN.CM.color("price") + ChatManager.format(order.TOTAL) +
               textCol + ".\n" + PLUGIN.CM.PREFIX +
               textCol + "Use " +
               PLUGIN.CM.color("helpUsage") + "/confirm " +
               textCol + "to confirm this order.";
           return PLUGIN.CM.reply(usr,msg);
        }
        else{} //The player is not using /confirm for buys; execute immediately.
        
        Tender res = PLUGIN.DB.buy(usr,merchandise,maxp);
        
        //
        //  RESULT
        //
        
        return processResults(usr,merchandise,displayName,res);
    }
    
    /**
     * Process the results of a buy action.
     * This was originally contiguous from onCommand, but was
     * separated so that {@link Confirm} can also finalize
     * buy operations.
     * 
     * @param usr           The user who is executing the buy operaion.
     * @param merchandise   The merchandise (including amount) the user tried to buy.
     * @param displayName   The already-looked-up display name of the items.
     * @param res           The results of {@link DatabaseManager#buy}.
     * @return              Always returns true, so that calling methods can finalize
     *                      the buy order and terminate in one line.
     */
    public boolean processResults(Player usr,ItemStack merchandise,String displayName,Tender res)
    {
        //On fail...
        if(res == null)
            return PLUGIN.CM.err500(usr);
        
        //Notify the buyer that a purchase was made.
        String msg =
            PLUGIN.CM.color("text") + "Bought " +
            PLUGIN.CM.color("quantity") + ChatManager.format(res.QUANTITY) + " " +
            PLUGIN.CM.color("item") + displayName + " " +
            (res.SELF > 0 ?
                PLUGIN.CM.color("text") + "(" +
                /*PLUGIN.CM.color("quantity") + */ChatManager.format(res.SELF) +
                PLUGIN.CM.color("text") + " from yourself) " : "") +                
            PLUGIN.CM.color("text") + "for a total of " +
            PLUGIN.CM.color("price") + ChatManager.format(res.COST) +
            PLUGIN.CM.color("text") + ".";
        PLUGIN.CM.reply(usr,msg);
        
        //Notify the buyer of any metadata.
        if(res.BROKE)
            PLUGIN.CM.error(usr,"Ran out of money.");
        else if(res.QUANTITY < merchandise.getAmount())
            PLUGIN.CM.error(usr,"Exhausted shop stock.");
        
        //Charge the player for the purchase.
        PLUGIN.ECON.withdrawPlayer(usr,res.COST);
        
        //Credit the appropriate items to the user's inventory.
        merchandise.setAmount(res.QUANTITY);
        PLUGIN.IM.giveItem(usr,merchandise);
        
        return true;
    }
}
