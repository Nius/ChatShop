package com.niusworks.chatshop.commands;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.managers.ChatManager;
import com.niusworks.chatshop.managers.DatabaseManager.Listing;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Executor for the "history" command for
 * OC Network's ChatShop.
 * @author ObsidianCraft Staff
 */
public class History implements CommandExecutor
{
    /** Command usage. **/
    public static final String USAGE = "/history <player> [page]";
    
    /** The specific instance of the parent ChatShop plugin. **/
    private final ChatShop PLUGIN;
    
    /**
     * Instantiate the command executor for "history" commands.
     * 
     * @param master    The specific instance of the parent ChatShop plugin.
     */
    public History(ChatShop master)
    {
        PLUGIN = master;
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String lbl, String[] args)
    {   
        //
        // Denial of service conditions
        //
        
        //No console
        if(!(sender instanceof Player))
            return PLUGIN.CM.reply(sender,"ChatShop.history cannot be executed as console.");
        Player usr = (Player)sender;
        //Permissions
        if(!sender.hasPermission("chatshop.history"))
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
        if(args.length > 2)
            return PLUGIN.CM.error(sender,USAGE);
        
        //Page check
        int page = 1;
        boolean didSpecifyPage = false;
        if(args.length > 0)
            try
            {
                //Parse the last argument for a page number.
                page = Integer.parseInt(args[args.length - 1]);
                didSpecifyPage = true;
            }
            catch(NumberFormatException e)
            {
                //If there are two arguments and the last one isn't a number,
                // then the command is malformed.
                if(args.length == 2)
                    return PLUGIN.CM.error(sender,USAGE);
                
                //The only other possibility is that there is only one argument,
                // and that argument is a playername, so the page number was omitted.
                else
                    page = 1;
            }
        
        //Player check
        //There is no way around this deprecated method, which can be
        // extremely painful if you think about it too much.
        //Keep an eye out for an officially implemented replacement
        // method in future.
        OfflinePlayer qPlayer;
        if(args.length > (didSpecifyPage ? 1 : 0))
        {
            qPlayer = PLUGIN.getServer().getOfflinePlayer(args[0]);
            if(!qPlayer.hasPlayedBefore())
                return PLUGIN.CM.error(sender,
                        PLUGIN.CM.color("player") + qPlayer.getName() +
                        PLUGIN.CM.color("error") + " has never played on ObsidianCraft.");
        }
        else
            qPlayer = (Player)sender;
        
        //
        //  EXECUTION
        //
        
        Listing[] tenders = PLUGIN.DB.getHistory(qPlayer);
        
        //
        //  RESULT
        //
        
        //On SQL fail...
        if(tenders == null)
            return PLUGIN.CM.err500(usr);
        //On no listings...
        if(tenders.length == 0)
            return PLUGIN.CM.error(usr,
                "No history found for " +
                PLUGIN.CM.color("player") + qPlayer.getName() +
                PLUGIN.CM.color("error") + ".");
        
        //Head the tenders list.
        //Checking the page number is already taken care of by
        //ChatManager, but for purposes of displaying an accurate
        //number it needs to happen here.
        page = Math.max(page,1);
        page = Math.min(page,PLUGIN.CM.paginate(tenders));
        String msg =
                PLUGIN.CM.color("text") + "History for " +
                PLUGIN.CM.color("item") + qPlayer.getName() +
                PLUGIN.CM.color("text") + ", page " + page +
                " of " + PLUGIN.CM.paginate(tenders) + ":";
        PLUGIN.CM.reply(usr,msg);
        
        Timestamp today = Timestamp.from(Instant.now().truncatedTo(ChronoUnit.DAYS));
        
        //List all listings on this page.
        tenders = PLUGIN.CM.paginate(tenders,page);
        for(int i = 0; i < tenders.length; i ++)
        {
            String quantity = PLUGIN.CM.color("quantity") + ChatManager.format(Math.abs(tenders[i].QUANTITY));
            String item = PLUGIN.CM.color("item") + PLUGIN.IM.lookup(tenders[i].MATERIAL,tenders[i].DAMAGE).DISPLAY;
            String priceEach = PLUGIN.CM.color("price") + ChatManager.format(tenders[i].PRICE);
            String priceTotal = PLUGIN.CM.color("price") + ChatManager.format(tenders[i].PRICE * tenders[i].QUANTITY);
            String player = PLUGIN.CM.color("player") + Bukkit.getPlayer(UUID.fromString(tenders[i].PLAYER)).getName();
            String textcol = PLUGIN.CM.color("text");
            String datecol = PLUGIN.getConfig().getString("chat.colors.date");
            
            if(tenders[i].QUANTITY < 1) //Queried player was the seller
            {
                msg = textcol + " Sold " + quantity + " " + item + textcol + " to " +
                    player + textcol + " @" + priceEach + textcol + "/ea. =" +
                    priceTotal.replaceAll("[()]","") + textcol + ".";
            }
            else //Queried player was the buyer
            {
                msg = textcol + " Bought " + quantity + " " + item + textcol + " from " +
                        player + textcol + " @" + priceEach + textcol + "/ea. =" +
                        priceTotal.replaceAll("[()]","") + textcol + ".";
            }
            
            if(tenders[i].DATE.before(today))
            {
                TextComponent output = new TextComponent();
                String shortDate = new SimpleDateFormat("MM/dd").format(tenders[i].DATE);
                String longDate = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(tenders[i].DATE);
                output.setText(shortDate);
                output.setColor(ChatColor.valueOf(datecol));
                output.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(longDate).color(ChatColor.valueOf(datecol)).create()));
                
                usr.spigot().sendMessage(output, new TextComponent(" " + msg));
            }
            else
            {
                String time = new SimpleDateFormat("HH:mm:ss").format(tenders[i].DATE);
                PLUGIN.CM.reply(usr,PLUGIN.CM.color("date") + time + msg,false);
            }
        }
        
        return true;
    }
}
