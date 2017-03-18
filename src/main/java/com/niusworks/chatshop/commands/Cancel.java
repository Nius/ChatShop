package com.niusworks.chatshop.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.managers.ChatManager;

/**
 * Executor for the "cancel" command for
 * OC Network's ChatShop.
 * @author ObsidianCraft Staff
 */
public class Cancel implements CommandExecutor
{
    /** Command usage. **/
    public static final String USAGE = "/cancel <quantity> <item>";
    
    /** The specific instance of the parent ChatShop plugin. **/
    private final ChatShop PLUGIN;
    
    /**
     * Instantiate the command executor for "cancel" commands.
     * 
     * @param master    The specific instance of the parent ChatShop plugin.
     */
    public Cancel(ChatShop master)
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
            return PLUGIN.CM.reply(sender,"ChatShop.cancel cannot be executed as console.");
        Player usr = (Player)sender;
        //Permissions
        if(!sender.hasPermission("chatshop.cancel"))
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
        if(args.length != 2)
            return PLUGIN.CM.error(sender,USAGE);
        
        //Item check
        Object parse = PLUGIN.IM.parse(usr,args[1]);
        if(parse instanceof Integer)
            switch((Integer)parse)
            {
                case -1: return PLUGIN.CM.error(usr,"You are not holding an item.");
                case -2:
                case -3: return PLUGIN.CM.error(usr,"Invalid item: " + PLUGIN.CM.color("item") + args[0] + PLUGIN.CM.color("error") + ".");
                default: return PLUGIN.CM.err500(usr);
            }
        ItemStack merchandise = (ItemStack)parse;
        String displayName = PLUGIN.IM.getDisplayName(merchandise);
        
        //Quantity Check
        //A user must enter a valid number greater than zero, or "all" which is
        // indicated by an ItemStack amount of -1.
        if(args[0].equalsIgnoreCase("all"))
            merchandise.setAmount(-1);
        else
        {
            try
            {
                merchandise.setAmount(Integer.parseInt(args[0]));
            } catch (NumberFormatException e) {
                return PLUGIN.CM.error(sender,USAGE);
            }
            if(merchandise.getAmount() < 1)
                return PLUGIN.CM.error(sender,"You cannot cancel less than one item.");
        }
        
        //
        //  EXECUTION
        //  Deferred to DatabaseManager for synchronization purposes.
        //
        
        int res = PLUGIN.DB.cancel(usr,merchandise);
        
        //
        //  RESULT
        //
        
        //On SQL fail...
        if(res == -2)
            return PLUGIN.CM.err500(sender);
        
        //On no stock fail...
        if(res == -1)
            return PLUGIN.CM.error(sender,
                    "You do not have any " +
                    PLUGIN.CM.color("item") + displayName +
                    PLUGIN.CM.color("error") + " for sale.");
        
        //Notify the player that a cancellation was made.
        
        String msg =
                PLUGIN.CM.color("text") + "Cancelled " +
                PLUGIN.CM.color("quantity") + ChatManager.format(res) + " " +
                PLUGIN.CM.color("item") + displayName +
                PLUGIN.CM.color("text") + ".";
        PLUGIN.CM.reply(usr,msg);
        
        //Credit the appropriate items to the user's inventory.
        merchandise.setAmount(res);
        PLUGIN.IM.giveItem(usr,merchandise);
        
        
        return true;
    }
}
