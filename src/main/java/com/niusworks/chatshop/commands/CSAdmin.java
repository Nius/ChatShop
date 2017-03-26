package com.niusworks.chatshop.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.managers.DatabaseManager;

import net.md_5.bungee.api.ChatColor;

/**
 * Executor for the "csadmin" command for OC Network's ChatShop.
 * <br>
 * Players can issue general administrative commands for ChatShop operations,
 * given the appropriate permissions. This command takes a single argument,
 * which determines its function.
 * <br><br>
 * If the single argument provided is an integer, a list of valid administrative
 * commands is provided.
 * <br><br>
 * If the single argument provided is the string "freeze" then the {@link DatabaseManager}
 * will be instructed to toggle the general freeze state of the ChatShop.
 * <br><br>
 * This command has the following limits (aside from basic perms):
 * <ul>
 * <li>Console access denied.
 * <li>Requires permission: <code>chatshop.admin</code>
 * </ul>
 * 
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
        
        /* There are no limitations on when or where admins can execute administrative commands,
           But there could be.
           
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
        
        */
        
        //
        //  VALIDATION
        //
        
        //Number of args
        if(args.length != 1)
            return PLUGIN.CM.error(sender,USAGE);
        
        //
        //  EXECUTION
        //  Some actions deferred to DatabaseManager for synchronization and flag control purposes,
        //  because some actions involve flags attached to a pseudo-player representing the ChatShop
        //  itself, in the database.
        //
        
        String textCol = PLUGIN.CM.color("text");
        String cmdCol = PLUGIN.CM.color("helpUsage");
        
        try
        {
            //If the argument was an integer, show a list of
            //available administrative commands.
            
            int page = Integer.parseInt(args[0]);
            
            String[] out = {
                    cmdCol + "freeze",
                    textCol + "Freeze ALL chatshop assets.",
                    cmdCol + "reload",
                    textCol + "Reload item definitions and configurations.",
                    cmdCol + "version",
                    textCol + "Get the current ChatShop version."};
                
            //Head the commands list.
            //Checking the page number is already taken care of by
            //ChatManager, but for purposes of displaying an accurate
            //number it needs to happen here.
            int pagesAvail = PLUGIN.CM.paginate(out);
            page = Math.max(page,1);
            page = Math.min(page,pagesAvail);
            
            String msg =
                    PLUGIN.CM.color("prefix") + "ChatShop Administrative Commands" +
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
        
        //General freeze command.
        if(args[0].equalsIgnoreCase("freeze"))
        {
            boolean isNowFrozen = PLUGIN.DB.toggleGeneralFreeze();
            
            String msg = (isNowFrozen ?
                textCol + "All chatshop assets are now " +
                ChatColor.RED + "frozen" + PLUGIN.CM.color("text") + "."
                :
                textCol + "The general freeze has been " +
                ChatColor.GREEN + "lifted" + PLUGIN.CM.color("text") + ".");
            
            return PLUGIN.CM.reply(usr,msg);
        }
        else if(args[0].equalsIgnoreCase("reload"))
        {
            String msg;
            int status = PLUGIN.IM.loadItems();
            switch(status)
            {
                case -1: msg = textCol + "Items information reloaded " + ChatColor.GREEN + "successfully" + textCol + "."; break;
                case -2: msg = PLUGIN.CM.color("error") + "Failed to read items file."; break;
                case -3: msg = PLUGIN.CM.color("error") + "Failed to spawn a new items file."; break;
                default: msg = PLUGIN.CM.color("error") + "Error in items file on line " + status + "."; break;
            }
            return PLUGIN.CM.reply(usr,msg);
        }
        else if(args[0].equalsIgnoreCase("version"))
        {
            String msg =
                textCol + "ChatShop, by Obsidian Network staff. Version " +
                cmdCol + PLUGIN.getDescription().getVersion() +
                textCol + ".";
            return PLUGIN.CM.reply(usr,msg);
        }
        else
            return PLUGIN.CM.error(usr,"Unknown administrative command.");
    }
}