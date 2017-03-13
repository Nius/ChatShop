package com.niusworks.chatshop.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.managers.ChatManager;

/**
 * Executor for the "quote" command for
 * OC Network's ChatShop.
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
        }
        
        //
        //  VALIDATION
        //
        
        //Number of args
        if(args.length != 2 && args.length != 3)
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
        
        //On fail...
        if(total == -1)
            return PLUGIN.CM.err500(usr);
        else if(total < 0)
        {
            merchandise.setAmount((int)total);
            double realtotal = PLUGIN.DB.price(usr,merchandise,-1);
            return PLUGIN.CM.reply(usr,
                PLUGIN.CM.color("text") + "There is only " +
                PLUGIN.CM.color("quantity") + ChatManager.format((int)(total * -1)) + " " +
                PLUGIN.CM.color("item") + displayName + " " +
                PLUGIN.CM.color("text") + "currently for sale, totaling " +
                PLUGIN.CM.color("price") + ChatManager.format(realtotal).replaceAll("[()]","") +
                PLUGIN.CM.color("text") + ".");
        }
        
        String msg =
            PLUGIN.CM.color("quantity") + merchandise.getAmount() + " " +
            PLUGIN.CM.color("item") + displayName + " " +
            PLUGIN.CM.color("text") + "would cost you a total of " +
            PLUGIN.CM.color("price") + ChatManager.format(total) +
            PLUGIN.CM.color("text") + ".";
        PLUGIN.CM.reply(usr,msg);
        
        return true;
    }
}
