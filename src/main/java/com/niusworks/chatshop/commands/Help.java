package com.niusworks.chatshop.commands;

import java.util.ArrayList;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.niusworks.chatshop.ChatShop;

/**
 * OC Network's ChatShop's executor for the
 * following commands:
 * 
 * chatshop
 * vm
 * vs
 * cs
 * oshop
 * shop
 * potions
 * potion
 * 
 * @author ObsidianCraft Staff
 */
public class Help implements CommandExecutor
{
    /** Command usage. **/
    public static final String USAGE = "/<chatshop|vm|vs|oshop|shop>";
    
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
        
        ArrayList<String> lines = new ArrayList<String>();
        
        PLUGIN.getDescription().getCommands().forEach((command,properties)->
        {
            lines.add(
                cmdCol + properties.get("usage"));
            lines.add(
                textCol + " " + properties.get("description"));
        });
        
        String[] potions = {
            textCol + "=== CHATSHOP and POTIONS ===",
            textCol + "ChatShop supports all potions and tipped arrows.",
            textCol + "You can search for such items by their full name,",
            textCol + "such as " + itemCol + "LongPotionOfRegeneration" + textCol + ".",
            textCol + "All potions also follow a standardized shortcut system:",
            itemCol + "p-regen2" + textCol + ", " + itemCol + "sp-longharm" + textCol + ", and " + itemCol + "ta-water",
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
        
        //Head the sales list.
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