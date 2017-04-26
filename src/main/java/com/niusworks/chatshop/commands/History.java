package com.niusworks.chatshop.commands;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.constructs.Item;
import com.niusworks.chatshop.constructs.Listing;
import com.niusworks.chatshop.managers.ChatManager;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Executor for the "history" and "sales" commands for OC Network's ChatShop.
 * <br>
 * Players can query the ChatShop for a complete history of all transactions pertinent to
 * a given player. Transactions are reverse-ordered by date (most recent first). Each transaction is prefixed with a timestamp:
 * <ul>
 * <li><code>HH:mm:ss</code> if the transaction occurred within the last 24 hours
 * <li><code>MM/dd</code> if the transaction occurred earlier than 24 hours ago. This shortened stamp can be moused-over to
 *     reveal a full <code>YYYY-MM-dd HH:mm:ss</code> timestamp for the transaction.
 * </ul>
 * This command takes zero, one, or two arguments: player and page.
 * <br><br>
 * Player is resolved to a Minecraft UUID for comparison against the database. This will still work
 * with offline players, but long-time absentee players might turn up negative even if they have transactions
 * in the market history because of Spigot limitations. In this case the name that player had at the time of
 * the transaction will be read from the database - though this name is not reliable.
 * <br>
 * If no player argument is supplied then the command will be executed on the calling player. Calling this command
 * on a player other than oneself requires either <code>chatshop.history.other</code> or <code>chatshop.admin</code>
 * permissions.
 * <br><br>
 * Page, optional, is an integer indicating which page of output to display. Very often there are
 * many transactions to show for a given player, and to prevent flooding the player's chat these entries
 * are divided into "pages" by the {@link ChatManager}. Only the specified page is shown. If no
 * page number is given then the first page will be shown.
 * <br><br>
 * This command has the following limits (aside from basic perms):
 * <ul>
 * <li>Console access denied.
 * <li>World must be whitelisted in config OR config must allow querying from anyone (see below).
 * <li>Gamemode must be whitelisted in config OR config must allow querying from anyone (see below).
 * <li>Some (but not all) functions of this command require elevated permissions.
 * </ul>
 * This command is effectively a read-only command; the ChatShop is queried for information but
 * nothing is changed. By default ChatShop will allow this command even if the player is in the wrong
 * world or the wrong gamemode, but administrators can configuratively disable this liberty.
 * <br><br>
 * @author ObsidianCraft Staff
 */
public class History implements CommandExecutor
{
    /** Command usage. **/
    public static final String USAGE = "/history <player> [page]";
    
    /** The specific instance of the parent ChatShop plugin. **/
    private final ChatShop PLUGIN;
    
    /**
     * Instantiate the command executor for "history" and "sales" commands.
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
        //Permissions (more checks are carried out below)
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
            if(qPlayer == null || !qPlayer.hasPlayedBefore())
                return PLUGIN.CM.error(sender,
                        PLUGIN.CM.color("player") + qPlayer.getName() +
                        PLUGIN.CM.color("error") + " has never played on ObsidianCraft.");
            if(!qPlayer.getUniqueId().equals(usr.getUniqueId()))
                if(!usr.hasPermission("chatshop.history.other") && !usr.hasPermission("chatshop.admin"))
                    return PLUGIN.CM.error(sender,"You do not have permission to look up other players' histories.");
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
        
        //Getting colors is fairly expensive, so do it once on
        //execution rather than once per line of output.
        String textCol = PLUGIN.CM.color("text");
        String itemCol = PLUGIN.CM.color("item");
        String qtyCol = PLUGIN.CM.color("quantity");
        String priceCol = PLUGIN.CM.color("price");
        String playerCol = PLUGIN.CM.color("player");
        String dateColName = PLUGIN.getConfig().getString("chat.colors.date");
        String dateColCode = PLUGIN.CM.color("date");
        
        //Constructing date objects can be monstrously expensive, according
        //to the interwebs, so construct them once here rather than who-knows-
        //how-many times in the output loop.
        SimpleDateFormat shortDateFormatter = new SimpleDateFormat("MM/dd");
        SimpleDateFormat longDateFormatter = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
        
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
        page = Math.min(page,PLUGIN.CM.getPaginationSize(tenders));
        String msg =
                textCol + "History for " +
                playerCol + qPlayer.getName() +
                textCol + ", page " + page +
                " of " + PLUGIN.CM.getPaginationSize(tenders) + ":";
        PLUGIN.CM.reply(usr,msg);
        
        Timestamp today = Timestamp.from(Instant.now().truncatedTo(ChronoUnit.DAYS));
        
        //List all listings on this page.
        tenders = PLUGIN.CM.paginate(tenders,page);
        for(int i = 0; i < tenders.length; i ++)
        {
            // Attempt to resolve the other player's current username from
            // their UUID. This will usually be successful, but for
            // long-time absentee players this will fail. In that case
            // the posted name in the database will be used.
            String playerName = PLUGIN.getServer().getOfflinePlayer(
                    UUID.fromString(tenders[i].PLAYER_UUID))
                    .getName();
            if(playerName == null)
                playerName = tenders[i].PLAYER_ALIAS;
            
            // Attempt to resolve the name of the material.
            // This should always be successful because these are being read from
            // a database of theoretically valid listings, but just in case...
            String itemDisplay = "Unknown Item";
            Item thing = PLUGIN.IM.lookup(tenders[i].MATERIAL,tenders[i].DAMAGE);
            if(thing != null)
                itemDisplay = thing.DISPLAY;
            
            String quantity = qtyCol + ChatManager.format(Math.abs(tenders[i].QUANTITY));
            String item = itemCol + itemDisplay;
            String priceEach = priceCol + ChatManager.format(tenders[i].PRICE);
            String priceTotal = priceCol + ChatManager.format(tenders[i].PRICE * tenders[i].QUANTITY);
            String player = playerCol + playerName;
            
            if(tenders[i].QUANTITY < 1) //Queried player was the seller
            {
                msg = textCol + " Sold " + quantity + " " + item + textCol + " to " +
                    player + textCol + " @" + priceEach + textCol + "/ea. =" +
                    priceTotal.replaceAll("[()]","") + textCol + ".";
            }
            else //Queried player was the buyer
            {
                msg = textCol + " Bought " + quantity + " " + item + textCol + " from " +
                        player + textCol + " @" + priceEach + textCol + "/ea. =" +
                        priceTotal.replaceAll("[()]","") + textCol + ".";
            }
            
            if(tenders[i].DATE.before(today))
            {
                TextComponent output = new TextComponent();
                String shortDate = shortDateFormatter.format(tenders[i].DATE);
                String longDate = longDateFormatter.format(tenders[i].DATE);
                output.setText(shortDate);
                output.setColor(ChatColor.valueOf(dateColName));
                output.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(longDate).color(ChatColor.valueOf(dateColName)).create()));
                
                usr.spigot().sendMessage(output, new TextComponent(" " + msg));
            }
            else
            {
                String time = new SimpleDateFormat("HH:mm:ss").format(tenders[i].DATE);
                PLUGIN.CM.reply(usr,dateColCode + time + msg,false);
            }
        }
        
        return true;
    }
}
