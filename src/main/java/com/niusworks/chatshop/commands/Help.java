package com.niusworks.chatshop.commands;

import java.util.ArrayList;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.managers.ChatManager;

/**
 * OC Network's ChatShop's executor for the following commands:
 * <ul>
 * <li>chatshop<li>cs<li>os<li>oshop<li>potion<li>potions<li>shop<li>vm<li>vs
 * </ul>
 * Players can query the ChatShop for all of its available commands and their usage.
 * Commands and usage are read direclty from plugin.yml; adding a new command will
 * automatically reflect here.
 * <br>
 * This command takes one argument, page, or zero.
 * <br><br>
 * Page, optional, is an integer indicating which page of output to display. This command outputs
 * a lot of text. To avoid flooding a player's chat, this text is divided into "pages" by the
 * {@link ChatManager}. Only the specified page is shown. If no page number is given then the first
 * page will be shown.
 * <br><br>
 * If this command is called by its alias "potion" or "potions" then only the information pertinent
 * to potions will be displayed.
 * <br><br>
 * This command has the following limits (aside from basic perms):
 * <ul>
 * <li>Console access denied.
 * <li>World must be whitelisted in config OR config must allow querying from anyone (see below).
 * <li>Gamemode must be whitelisted in config OR config must allow querying from anyone (see below).
 * </ul>
 * This command is effectively a read-only command; the ChatShop is queried for information but
 * nothing is changed. By default ChatShop will allow this command even if the player is in the wrong
 * world or the wrong gamemode, but administrators can configuratively disable this liberty.
 * <br><br>
 * @author ObsidianCraft Staff
 */
public class Help implements CommandExecutor
{
    /** Command usage. **/
    public static final String USAGE = "/<chatshop|cs|os|oshop|potion|potions|shop|vm|vs> [page]";
    
    /** The specific instance of the parent ChatShop plugin. **/
    private final ChatShop PLUGIN;
    
    /**
     * Instantiate the command executor for help commands.
     * 
     * @param master    The specific instance of the parent ChatShop plugin.
     */
    public Help(ChatShop master)
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
            return PLUGIN.CM.reply(sender,"ChatShop.help cannot be executed as console.");
        Player usr = (Player)sender;
        //Permissions
        if(!sender.hasPermission("chatshop.help"))
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
            
            allowed = false;
            Object[] worlds = PLUGIN.getConfig().getList("allowed-worlds").toArray();
            for(int i = 0; i < worlds.length; i ++)
                if(worlds[i] instanceof String)
                    if(((String)worlds[i]).equalsIgnoreCase(usr.getWorld().getName()))
                        allowed = true;
            if(!allowed)
                return PLUGIN.CM.denyWorld(sender);
        }
        
        //
        //  VALIDATION
        //
        
        //Number of args
        if(args.length > 1)
            return PLUGIN.CM.error(sender,USAGE);
        
        //Page check
        int page = 1;
        if(args.length == 1)
            try
            {
                page = Integer.parseInt(args[0]);
            }
            catch(NumberFormatException e)
            {
                return PLUGIN.CM.error(sender,USAGE);
            }
        
        //
        //  EXECUTION
        //
        
        String textCol = PLUGIN.CM.color("text");
        String itemCol = PLUGIN.CM.color("item");
        String cmdCol = PLUGIN.CM.color("helpUsage");
        String priceCol = PLUGIN.CM.color("price");
        
        ArrayList<String> lines = new ArrayList<String>();
        
        PLUGIN.getDescription().getCommands().forEach((command,properties)->
        {
            boolean isOmitted = false;
            int index = lines.size();
            if(command.equalsIgnoreCase("buy"))                     //Ensure that /buy is the first listed command
                index = 0;
            else if(command.equalsIgnoreCase("sell"))               //Ensure that /sell is the second listed command
                index = 2;                                          //(each command occupies two lines)
            else if(command.equalsIgnoreCase("csadmin"))
            {
                if(!usr.hasPermission("chatshop.admin"))            //Don't show /csadmin if the user doesn't have perms for it.
                    isOmitted = true;
            }
            else if(lines.get(index - 2).contains("/csadmin"))      //Ensure that /csadmin is the last command.
                index = lines.size() - 2;                           //(each command occupies two lines)
                
            if(!isOmitted)
            {
                lines.add(index,
                    cmdCol + properties.get("usage"));
                lines.add(index + 1,
                    textCol + " " + properties.get("description"));
            }
        });
        
        String[] shortcuts = {
            "",
            textCol + "=== SHORTCUTS ===",
            textCol + "You can use the shortcut \"" + itemCol + "hand" + textCol + "\" instead of an item name",
            textCol + "to indicate the item you're currently holding in your main hand.",
            textCol + "When selling items, you can use \"" + priceCol + "-" + textCol + "\" instead of a price",
            textCol + "to use whatever price you already have posted."};
        for(String i : shortcuts)
            lines.add(i);
        
        String[] potions = {
            textCol + "=== CHATSHOP and POTIONS ===",
            textCol + "ChatShop supports all potions and tipped arrows.",
            textCol + "You can search for such items by their full name,",
            textCol + "such as " + itemCol + "LongPotionOfRegeneration" + textCol + ".",
            textCol + "All potions also follow a standardized shortcut system:",
            itemCol + "p-harm2" + textCol + ", " + itemCol + "sp-longnight" + textCol + ", and " + itemCol + "ta-water",
            textCol + "are all valid item references.",
            textCol + "Don't forget you can also use " + itemCol + "hand" + textCol + "."};
        
        String[] out;
        boolean isPotions = lbl.equalsIgnoreCase("potions") || lbl.equalsIgnoreCase("potion"); 
        if(isPotions)
            out = potions;
        else
        {
            lines.add("");
            for(String i : potions)
                lines.add(i);
            out = lines.toArray(new String[lines.size()]);
        }
        
        //Head the commands list.
        //Checking the page number is already taken care of by
        //ChatManager, but for purposes of displaying an accurate
        //number it needs to happen here.
        int pagesAvail = PLUGIN.CM.paginate(out);
        page = Math.max(page,1);
        page = Math.min(page,pagesAvail);
        
        String msg =
                PLUGIN.CM.color("prefix") + "ChatShop Commands" +
                (!isPotions ?
                    textCol + ", page " + page +
                    " of " + pagesAvail + ":":
                    "");
        PLUGIN.CM.reply(usr,msg,false);
        
        //List all commands on this page.
        out = PLUGIN.CM.paginate(out,page);
        for(int i = 0; i < out.length; i ++)
        {
            PLUGIN.CM.reply(usr,out[i],false);
        }
           
        return true;
    }
}