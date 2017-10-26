package com.niusworks.chatshop.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.constructs.EListing;
import com.niusworks.chatshop.constructs.EnchLvl;
import com.niusworks.chatshop.managers.ItemManager;

import net.md_5.bungee.api.chat.TextComponent;

/**
 * Executor for the "ecancel" command for OC Network's ChatShop.
 * <br>
 * Players can remove their own enchanted items from the chatshop. This command has one element:
 * the lot number for the item they wish to cancel.
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
public class ECancel implements CommandExecutor
{
    /** Command usage. **/
    public static final String USAGE = "/ecancel <quantity> <item>";
    
    /** The specific instance of the parent ChatShop plugin. **/
    private final ChatShop PLUGIN;
    
    /**
     * Instantiate the command executor for "ecancel" commands.
     * 
     * @param master    The specific instance of the parent ChatShop plugin.
     */
    public ECancel(ChatShop master)
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
            return PLUGIN.CM.reply(sender,"ChatShop.ecancel cannot be executed as console.");
        Player usr = (Player)sender;
        //Permissions
        if(!sender.hasPermission("chatshop.ecancel"))
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
        if(args.length != 1)
            return PLUGIN.CM.error(sender,USAGE);
        
        //Arg is a valid lot number
        String itemCol = PLUGIN.CM.color("item");
        EListing listing = null;
        int lot;
        try
        {
            lot = Integer.parseInt(args[0]);
        }
        catch(NumberFormatException e)
        {
            return PLUGIN.CM.error(usr,USAGE);
        }
        listing = PLUGIN.DB.getEListing(lot);
        if(listing == null)
            return PLUGIN.CM.error(usr,"Invalid lot number " + itemCol + lot);
        
        //Lot number belongs to user
        if(!listing.PLAYER_UUID.equals(usr.getUniqueId().toString()))
            return PLUGIN.CM.error(usr,"That item does not belong to you.");
        
        Object res = PLUGIN.DB.ecancel(lot);
        
        //Detect failures
        if(res instanceof Integer)
            switch(((Integer)res).intValue())
            {
                case -1:    return PLUGIN.CM.error(usr,"This listing no longer exists.");
                case -2:    return PLUGIN.CM.err500(usr);
            }
        
        ItemStack merchandise = listing.toItemStack();

        //Credit the item to the user's inventory.
        PLUGIN.IM.giveItem(usr,merchandise);
        
        String textCol = PLUGIN.CM.color("text");
        
        //Notify the player that the item was cancelled.
        TextComponent tc0 = new TextComponent();
        tc0.setText(PLUGIN.CM.PREFIX +
                    textCol + "Cancelled ");
        TextComponent tc1 = PLUGIN.CM.MOTforEnchanted(itemCol + "lot #" + listing.ID,listing.ID,merchandise);
        TextComponent tc2 = new TextComponent();
        tc2.setText(textCol + ".");
        usr.spigot().sendMessage(tc0,tc1,tc2);
        
        return true;
    }
}