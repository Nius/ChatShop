package com.niusworks.chatshop.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.managers.ChatManager;
import com.niusworks.chatshop.managers.DatabaseManager.Listing;
import com.niusworks.chatshop.managers.ItemManager;

/**
 * Executor for the "history" command for
 * OC Network's ChatShop.
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
        //  ...nothing to do.
        //
        
        //
        //  EXECUTION
        //
        
        int totalSold = 0;
        double valuePosted = 0;
        for(ItemStack merchandise : usr.getInventory().getContents())
        {
            Object res = PLUGIN.DB.sell(usr,merchandise,-1);
        
            // On fail...
            if(res instanceof Integer && ((Integer)res).intValue() == -2)
                return PLUGIN.CM.err500(sender);
            
            // On "-" price but no listing found...
            if(res instanceof Integer && ((Integer)res).intValue() == -1)
                continue;
            
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