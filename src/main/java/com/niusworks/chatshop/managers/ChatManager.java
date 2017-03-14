package com.niusworks.chatshop.managers;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.managers.DatabaseManager.Listing;

import net.md_5.bungee.api.ChatColor;

/**
 * Manages all chat output functionality for
 * OC Network's ChatShop.
 * @author ObsidianCraft Staff
 */
public class ChatManager
{
    /** Prefix for all outgoing messages. **/
    public final String PREFIX;
    
    /** The master plugin for this manager. **/
    protected final ChatShop PLUGIN;
    
    /** A list of colors used in chat messages, stored as name => formatString. **/
    protected final HashMap<String,String> COLORS = new HashMap<String,String>();
    
    /** A decimal formatter, used for {@link #format(double)}. **/
    protected static final NumberFormat DFORMAT = NumberFormat.getCurrencyInstance();
    
    /** An integer formatter, used for {@link #format(int)}. **/
    protected static final NumberFormat IFORMAT = NumberFormat.getIntegerInstance();
    
    /**
     * Create an ItemManager with a reference to the master
     * plugin.
     * 
     * @param master    The master ChatShop plugin reference.
     */
    public ChatManager(ChatShop master)
    {
        PLUGIN = master;
        Configuration cfg = PLUGIN.getConfig();
        PREFIX = ChatColor.valueOf(cfg.getString("chat.prefix.color")).toString() +
                 cfg.getString("chat.prefix.tag","") + " ";
        
        // Resolve all valid colors listed in config to chat color codes.
        for(Map.Entry<String,Object> param : cfg.getConfigurationSection("chat.colors").getValues(true).entrySet())
            if(param.getValue() instanceof String)
                try
                {
                    ChatColor col = ChatColor.valueOf((String) param.getValue());
                    String key = param.getKey().split("\\.")[
                        param.getKey().split("\\.").length - 1];
                    COLORS.put(key,col.toString());
                }
                catch(IllegalArgumentException e){/* do nothing */}
    }
    
    /**
     * Get the color format string for the specified color name.
     * 
     * @param colName   The name of the color to return.
     * @return          A formatString for the specified color.
     */
    public String color(String colName)
    {
        if(COLORS.containsKey(colName))
            return COLORS.get(colName);
        else
            return "";
    }
    
    /**
     * Quickly format a price to show two decimal places, unless
     * it is an even int, in which case no decimal is shown.
     * Includes a dollar sign.
     * 
     * @param price The price to format.
     * @return      The price, formatted and ready to print.
     */
    public static String format(double price)
    {
        return format(price,true);
    }
    
    /**
     * Quickly format a price to show two decimal places, unless
     * it is an even int, in which case no decimal is shown.
     * 
     * @param price The price to format.
     * @param includeDollar Whether to include a dollar sign.
     * @return      The price, formatted and ready to print.
     */
    public static String format(double price,boolean includeDollar)
    {
        String ret = DFORMAT.format(price).replaceAll("\\.00","");
        if(!includeDollar)
            return ret.replaceAll("\\$","");
        return ret;
    }
    
    /**
     * Quickly format a quantity to include commas as appropriate.
     * 
     * @param qty   The number to format.
     * @return      The number, formatted and ready to print.
     */
    public static String format(int qty)
    {
        return IFORMAT.format(qty);
    }
    
    
    /**
     * Send the specified message to all players.
     * 
     * @param message   The message to disburse.
     */
    public void broadcast(String message)
    {
        PLUGIN.getServer().broadcastMessage(PREFIX + message);
    }
    
    /**
     * Given a list of Listings, return one page's worth
     * assuming that Listings and lines of chat have a
     * one-to-one relationship.
     * 
     * @param available All available Listings.
     * @param pageNum   The index of the desired page, where
     *                  the first page is index 1.
     * @return          One page of Listings, pursuant to
     *                  the config file.
     */
    public Listing[] paginate(Listing[] available, int pageNum)
    {        
        //Convert from natural page number to index
        pageNum --;
        
        //Get configured number of lines per page
        int listingsPerPage = PLUGIN.getConfig().getInt("chat.page-length");
        
        //Determine total number of possible pages
        int pagesAvailable = paginate(available);
        
        //Prevent asking for a nonexistent page
        if(pageNum >= pagesAvailable)
            pageNum = pagesAvailable - 1;
        if(pageNum < 0)
            pageNum = 0;
        
        int startIndex = pageNum * listingsPerPage;
        
        //Determine how many listings are on this page
        //(in case of last page)
        int qty = Math.min(
            available.length - startIndex,
            listingsPerPage);
        
        Listing[] res = new Listing[qty];
        for(int i = startIndex; i < startIndex + listingsPerPage && i < available.length; i++)
            res[i - startIndex] = available[i];
        
        return res;
    }
    
    /**
     * Given a list of Strings, return one page's worth
     * assuming that Strings and lines of chat have a
     * one-to-one relationship.
     * 
     * @param available All available Strings.
     * @param pageNum   The index of the desired page, where
     *                  the first page is index 1.
     * @return          One page of Strings, pursuant to
     *                  the config file.
     */
    public String[] paginate(String[] available, int pageNum)
    {        
        //Convert from natural page number to index
        pageNum --;
        
        //Get configured number of lines per page
        int listingsPerPage = PLUGIN.getConfig().getInt("chat.page-length");
        
        //Determine total number of possible pages
        int pagesAvailable = paginate(available);
        
        //Prevent asking for a nonexistent page
        if(pageNum >= pagesAvailable)
            pageNum = pagesAvailable - 1;
        if(pageNum < 0)
            pageNum = 0;
        
        int startIndex = pageNum * listingsPerPage;
        
        //Determine how many listings are on this page
        //(in case of last page)
        int qty = Math.min(
            available.length - startIndex,
            listingsPerPage);
        
        String[] res = new String[qty];
        for(int i = startIndex; i < startIndex + listingsPerPage && i < available.length; i++)
            res[i - startIndex] = available[i];
        
        return res;
    }
    
    /**
     * Determine the total number of pages it would take
     * to express the given array of Objects,
     * assuming that Objects and lines of chat have a
     * one-to-one relationship.
     * 
     * @param available All available Objects.
     * @return          The number of pages required to
     *                  express these Objects.
     */
    public int paginate(Object[] available)
    {
        int listingsPerPage = PLUGIN.getConfig().getInt("chat.page-length");
        double fpa = ((double)available.length) / listingsPerPage;
        int pagesAvailable = ((int)fpa) +
            (fpa % 1.00 == 0 ? 0 : 1);
        return pagesAvailable;
    }
    
    /**
     * Log an INFO message.
     * 
     * @param message   The message to log.
     */
    public void log(String message)
    {
        PLUGIN.getLogger().info(message);
    }
    
    /**
     * Log a SEVERE message.
     * 
     * @param message   The message to log.
     */
    public void severe(String message)
    {
        PLUGIN.getLogger().severe(message);
    }
    
    /**
     * Send a message to the specified recipient.
     * 
     * @param sender    The source of the action to which we are to reply.
     * @param message   The message to send to the recipient.
     * @return          Always returns true, to allow command executors to
     *                  send a message and terminate in one line.
     */
    public boolean reply(CommandSender sender, String message)
    {        
        return reply(sender,message,true);
    }
    
    /**
     * Send a message to the specified recipient.
     * 
     * @param sender    The source of the action to which we are to reply.
     * @param message   The message to send to the recipient.
     * @param prefix    Whether to prepend the standard prefix to the specified
     *                  message.
     * @return          Always returns true, to allow command executors to
     *                  send a message and terminate in one line.
     */
    public boolean reply(CommandSender sender, String message,boolean prefix)
    {        
        sender.sendMessage((prefix ? PREFIX : "") + message);
        return true;
    }
    
    /**
     * {@link #reply(CommandSender, String)} with errorColor text.
     * 
     * @param sender    The source of the action to which we are to reply.
     * @param message   The message to send to the recipient.
     * @return          Always returns true, to allow command executors to
     *                  send a message and terminate in one line.
     */
    public boolean error(CommandSender sender, String message)
    {
        return reply(sender,color("error") + message);
    }
    
    /**
     * {@link #error(CommandSender, String)} with the predefined
     * message, "An unexpected error occurred. Please notify an
     * administrator."
     * 
     * @param sender    The source of the action to which we are to reply.
     * @return          Always returns true, to allow command executors to
     *                  send a message and terminate in one line.
     */
    public boolean err500(CommandSender sender)
    {
        return error(sender, "An unexpected error occurred. Please notify an administrator.");
    }
    
    /**
     * Notify the user that they do not have permission to proceed.
     * 
     * @param sender    The source of the action to which we are to reply.
     * @return          Always returns true, to allow command executors to
     *                  send a message and terminate in one line.
     */
    public boolean denyPermission(CommandSender sender)
    {
        return error(sender, "You do not have permission to do this.");
    }
    
    /**
     * Notify the user that they are in the wrong gamemode.
     * 
     * @param sender    The source of the action to which we are to reply.
     * @return          Always returns true, to allow command executors to
     *                  send a message and terminate in one line.
     */
    public boolean denyGameMode(CommandSender sender)
    {
        return error(sender, "You cannot use that command in your current game mode.");
    }
    
    /**
     * Notify the user that they are in the wrong world.
     * 
     * @param sender    The source of the action to which we are to reply.
     * @return          Always returns true, to allow command executors to
     *                  send a message and terminate in one line.
     */
    public boolean denyWorld(CommandSender sender)
    {
        return error(sender, "You cannot use that command in your current world.");
    }
}
