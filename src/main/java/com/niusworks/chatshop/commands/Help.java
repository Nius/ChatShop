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
        
        ArrayList<String> lines = new ArrayList<String>();
        
        PLUGIN.getDescription().getCommands().forEach((command,properties)->
        {
            lines.add(
                PLUGIN.CM.color("helpUsage") + properties.get("usage") +
                PLUGIN.CM.color("text") + " " + properties.get("description"));
        });
        
        String[] out = lines.toArray(new String[lines.size()]);
        
        //Head the sales list.
        //Checking the page number is already taken care of by
        //ChatManager, but for purposes of displaying an accurate
        //number it needs to happen here.
        page = Math.max(page,1);
        page = Math.min(page,PLUGIN.CM.paginate(out));
        String msg =
                "ChatShop Commands" +
                PLUGIN.CM.color("text") + ", page " + page +
                " of " + PLUGIN.CM.paginate(out) + ":";
        PLUGIN.CM.reply(usr,msg);
        
        //List all listings on this page.
        out = PLUGIN.CM.paginate(out,page);
        for(int i = 0; i < out.length; i ++)
        {
            PLUGIN.CM.reply(usr,out[i],false);
        }
           
        return true;
    }
}