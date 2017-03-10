package com.niusworks.chatshop.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.managers.ChatManager;
import com.niusworks.chatshop.managers.DatabaseManager.Tender;

/**
 * Executor for the "buy" command for
 * OC Network's ChatShop.
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
        
        Tender res = PLUGIN.DB.buy(usr,merchandise,maxp);
        
        //
        //  RESULT
        //
        
        //On fail...
        if(res == null)
            return PLUGIN.CM.err500(sender);
        
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
            PLUGIN.CM.error(usr,"Ran out of cash.");
        if(res.QUANTITY < merchandise.getAmount())
            PLUGIN.CM.error(usr,"Exhausted shop stock.");
        
        //Charge the player for the purchase.
        PLUGIN.ECON.withdrawPlayer(usr,res.COST);
        
        //Credit the appropriate items to the user's inventory.
        merchandise.setAmount(res.QUANTITY);
        PLUGIN.IM.giveItem(usr,merchandise);
        
        return true;
    }
}
