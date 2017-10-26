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
 * Executor for the "cancel" command for OC Network's ChatShop.
 * <br>
 * Players can remove their own items from the chatshop. This command has two elements: quantity
 * and item.
 * <br><br>
 * Quantity can be an integer, which will be compared to the total amount of the specified
 * item currently in the user's inventory. It can also be the string "all" (case-insensitive)
 * in which case the command is executed as if the player entered the exact amount they have
 * in their inventory.
 * <br><br>
 * Item can be any ItemManager-recognized string representation of a Minecraft item as understood
 * by {@link ItemManager#parse}. Invalid items are caught and appropriate messages are sent to
 * the player. Items not currently listed on the market by this player are likewise refused.
 * <br><br>
 * This command has the following limits (aside from basic perms):
 * <ul>
 * <li>Console access denied.
 * <li>World must be whitelisted in config.
 * <li>Gamemode must be whitelisted in config.
 * <li>General freeze prevents command.
 * </ul>
 * 
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
        //If the user enters only an integer value preceded by a hash then they
        //  probably meant to use ecancel, so redirect to ECancel.
        if(args.length == 1 && args[0].charAt(0) == '#')
            try
            {
                int lot = Integer.parseInt(args[0].substring(1));
                return PLUGIN.getCommand("ecancel").getExecutor().onCommand(usr,cmd,lbl,new String[]{lot + ""});
            }
            catch(NumberFormatException e){/* do nothing, the next conditional will terminate this command. */}
        if(args.length != 2)
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
                        "Enchanted items must be cancelled by lot number. See "
                        + PLUGIN.CM.color("helpUsage") + "/ecancel"
                        + PLUGIN.CM.color("error") + ".");
                case -5: return PLUGIN.CM.error(usr,"Damaged items cannot be sold on the ChatShop.");
                case -6: return PLUGIN.CM.error(usr,"That item cannot be sold on the ChatShop.");
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
        
        //On no stock...
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
