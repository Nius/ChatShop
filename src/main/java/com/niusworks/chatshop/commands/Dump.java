package com.niusworks.chatshop.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.constructs.Listing;
import com.niusworks.chatshop.managers.ChatManager;
import com.niusworks.chatshop.managers.DatabaseManager;

/**
 * Executor for the "dump" command for OC Network's ChatShop.
 * <br>
 * Players can, in one command, have large quantities of their inventory posted to the
 * ChatShop. This command takes no arguments.
 * <br><br>
 * For each slot in a player's inventory, the {@link DatabaseManager} is instructed to
 * sell all of the given item for the currently listed price. In effect, this amounts to:
 * <pre>
 * for(item : inventory)
 *     /sell all item -</pre>
 * Error messages, such as pertain to invalid or damaged items, are handled but not omitted
 * to the player - it is not expected that this command will be able to sell all items.
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
public class Dump implements CommandExecutor
{
    /** Command usage. **/
    public static final String USAGE = "/dump";
    
    /** The specific instance of the parent ChatShop plugin. **/
    private final ChatShop PLUGIN;
    
    /**
     * Instantiate the command executor for "dump" commands.
     * 
     * @param master    The specific instance of the parent ChatShop plugin.
     */
    public Dump(ChatShop master)
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
        //  ...nothing to do.
        //
        
        //
        //  EXECUTION
        //
        
        int totalSold = 0;
        double valuePosted = 0;
        for(ItemStack merchandise : usr.getInventory().getContents())
        {
            if(merchandise == null)
                continue;
            
            // Wash each item through the ItemManager verifier.
            // This takes care of special cases such as potions, tipped arrows,
            //  and enchantments.
            Object res = PLUGIN.IM.verify(merchandise,false);
            if(!(res instanceof ItemStack))
                continue;
            merchandise = (ItemStack)res;
            
            res = PLUGIN.DB.sell(usr,merchandise,-1);
        
            // On fail...
            if(res instanceof Integer && ((Integer)res).intValue() == -2)
                return PLUGIN.CM.err500(sender);
            
            // On "-" price but no listing found...
            if(res instanceof Integer && ((Integer)res).intValue() == -1)
                continue;
            
            // On updated listing exceeds quantity limit...
            if(res instanceof Integer && ((Integer)res).intValue() == -3)
                continue;
            
            //Remove the specified items from the player's inventory.
            int removed = 0;
            ItemStack[] inv = usr.getInventory().getContents();
            for(int i = 0; i < inv.length; i ++)
                if(PLUGIN.IM.areSameType(inv[i],merchandise))
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
            
            totalSold += merchandise.getAmount();
            valuePosted += ((Listing)res).PRICE * merchandise.getAmount();
        }
        
        String msg =
            PLUGIN.CM.color("text") + "Posted " +
            PLUGIN.CM.color("quantity") + totalSold +
            PLUGIN.CM.color("text") + " items for a total of " +
            PLUGIN.CM.color("price") + ChatManager.format(valuePosted) +
            PLUGIN.CM.color("text") + ".";
        
        PLUGIN.CM.reply(usr,msg);
        
        return true;
    }
}