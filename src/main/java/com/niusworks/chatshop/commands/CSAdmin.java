package com.niusworks.chatshop.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.niusworks.chatshop.ChatShop;

import net.md_5.bungee.api.ChatColor;

/**
 * Executor for the "csadmin" command for
 * OC Network's ChatShop.
 * @author ObsidianCraft Staff
 */
public class CSAdmin implements CommandExecutor
{
    /** Command usage. **/
    public static final String USAGE = "/csadmin <command>";
    
    /** The specific instance of the parent ChatShop plugin. **/
    private final ChatShop PLUGIN;
    
    /**
     * Instantiate the command executor for "csadmin" commands.
     * 
     * @param master    The specific instance of the parent ChatShop plugin.
     */
    public CSAdmin(ChatShop master)
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
            return PLUGIN.CM.reply(sender,"ChatShop.csadmin cannot be executed as console.");
        Player usr = (Player)sender;
        //Permissions
        if(!sender.hasPermission("chatshop.admin"))
            return PLUGIN.CM.denyPermission(sender);
        //Gamemode
//        Object[] modes = PLUGIN.getConfig().getList("allowed-modes").toArray();
//        boolean allowed = false;
//        for(int i = 0; i < modes.length; i ++)
//            if(modes[i] instanceof String)
//                if(((String)modes[i]).equalsIgnoreCase(usr.getGameMode().toString()))
//                    allowed = true;
//        if(!allowed)
//            return PLUGIN.CM.denyGameMode(sender);
//        
//        allowed = false;
//        Object[] worlds = PLUGIN.getConfig().getList("allowed-worlds").toArray();
//        for(int i = 0; i < worlds.length; i ++)
//            if(worlds[i] instanceof String)
//                if(((String)worlds[i]).equalsIgnoreCase(usr.getWorld().getName()))
//                    allowed = true;
//        if(!allowed)
//            return PLUGIN.CM.denyWorld(sender);
        
        //
        //  VALIDATION
        //
        
        //Number of args
        if(args.length != 1)
            return PLUGIN.CM.error(sender,USAGE);
        
        //
        //  EXECUTION
        //  Deferred to DatabaseManager for synchronization and flag control purposes,
        //  because the freeze flag is attached to a pseudo-user representing the ChatShop
        //  itself.
        //
        
        try
        {
            int page = Integer.parseInt(args[0]);
            
            String[] out = {
                    PLUGIN.CM.color("helpUsage") + "freeze",
                    PLUGIN.CM.color("text") + "Freeze ALL chatshop assets."};
                
            //Head the commands list.
            //Checking the page number is already taken care of by
            //ChatManager, but for purposes of displaying an accurate
            //number it needs to happen here.
            int pagesAvail = PLUGIN.CM.paginate(out);
            page = Math.max(page,1);
            page = Math.min(page,pagesAvail);
            
            String msg =
                    PLUGIN.CM.color("prefix") + "ChatShop Commands" +
                        PLUGIN.CM.color("text") + ", page " + page +
                        " of " + pagesAvail + ":";
            PLUGIN.CM.reply(usr,msg,false);
            
            //List all commands on this page.
            out = PLUGIN.CM.paginate(out,page);
            for(int i = 0; i < out.length; i ++)
            {
                PLUGIN.CM.reply(usr,out[i],false);
            }
               
            return true;
        }
        catch(NumberFormatException e){}
        
        if(args[0].equalsIgnoreCase("freeze"))
        {
            boolean isNowFrozen = PLUGIN.DB.toggleGeneralFreeze();
            
            String msg = (isNowFrozen ?
                PLUGIN.CM.color("text") + "All chatshop assets are now " +
                ChatColor.RED + "frozen" + PLUGIN.CM.color("text") + "."
                :
                PLUGIN.CM.color("text") + "The general freeze has been " +
                ChatColor.GREEN + "lifted" + PLUGIN.CM.color("text") + ".");
            
            return PLUGIN.CM.reply(usr,msg);
        }
        else
            return PLUGIN.CM.error(usr,"Unknown administrative command.");
    }
}