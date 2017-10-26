package com.niusworks.chatshop.managers;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.constructs.EListing;
import com.niusworks.chatshop.constructs.Listing;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Manages all chat output functionality for OC Network's ChatShop.
 * <br>
 * This class facilitates formatting of numbers (decimal and integer),
 * quick reading of chat color codes from configuration for application in
 * output, and message output itself.
 * <br><br>
 * ALL ChatShop output should be passed through this class in order to ensure
 * that output is properly prefixed and/or logged.
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
        COLORS.put("prefix",ChatColor.valueOf(cfg.getString("chat.prefix.color")).toString());
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
     * Quickly format an integer to roman numerals.
     * A negative number returns an empty string.
     * 
     * @param x     The number to format.
     * @return      A roman numeral expression of the number.
     */
    public static String romanNumeralize(int x)
    {
        String out = "";
        while(x >= 1000){out += "M"; x -= 1000;}    //M = 1000
        if(x >= 900){out += "CM"; x -= 900;}        //CM = 900
        while(x >= 500){out += "D"; x -= 500;}      //D = 500
        if(x >= 400){out += "CD"; x -= 400;}        //CD = 400
        while(x >= 100){out += "C"; x -= 100;}      //C = 100
        if(x >= 90){out += "XC"; x -= 90;}          //XC = 90
        while(x >= 50){out += "L"; x -= 50;}        //L = 50
        if(x >= 40){out += "XL"; x -= 40;}          //XL = 40
        while(x >= 10){out += "X"; x -= 10;}        //X = 10
        if(x >= 9){out += "IX"; x -= 9;}            //IX = 9
        while(x >= 5){out +="V"; x -= 5;}           //V = 5
        if(x >= 4){out += "IV"; x -= 4;}            //IV = 4
        while(x >= 1){out += "I"; x -= 1;}          //I = 1
        
        return out;
    }
    
    /**
     * Returns an integer derived from a roman numeral string.
     * 
     * @param x The string to decipher.
     * @return  An integer; -1 if the string was invalid.
     */
    public static int deRomanNumeralize(String x)
    {
        int out = 0;
        for(int i = 0; i < x.length(); i ++)
        {
            boolean isEnd = (i == x.length() - 1);
            char next = '~';
            if(!isEnd)
                next = x.charAt(i + 1);
            switch(x.charAt(i))
            {
                case 'M':   out += 1000;    break;
                case 'D':   out += 500;     break;
                case 'C':   switch(next)
                            {
                                case 'M':   out += 900; i++; continue;
                                case 'D':   out += 400; i++; continue;
                            }
                            out += 100;     break;
                case 'L':   out += 50;      break;
                case 'X':   switch(next)
                            {
                                case 'C':   out += 90; i++; continue;
                                case 'L':   out += 40; i++; continue;
                            }
                            out += 10;      break;
                case 'V':   out += 5;       break;
                case 'I':   switch(next)
                            {
                                case 'X':   out += 9; i++; continue;
                                case 'V':   out += 4; i++; continue;
                            }
                            out += 1;       break;
                default:    return -1;
            }
        }
        return out;
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
        int pagesAvailable = getPaginationSize(available);
        
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
     * Given a list of EListings, return one page's worth
     * assuming that EListings and lines of chat have a
     * one-to-one relationship.
     * 
     * @param available All available EListings.
     * @param pageNum   The index of the desired page, where
     *                  the first page is index 1.
     * @return          One page of Listings, pursuant to
     *                  the config file.
     */
    public EListing[] paginate(EListing[] available, int pageNum)
    {        
        //Convert from natural page number to index
        pageNum --;
        
        //Get configured number of lines per page
        int listingsPerPage = PLUGIN.getConfig().getInt("chat.page-length");
        
        //Determine total number of possible pages
        int pagesAvailable = getPaginationSize(available);
        
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
        
        EListing[] res = new EListing[qty];
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
        //Get configured number of lines per page
        int listingsPerPage = PLUGIN.getConfig().getInt("chat.page-length");
        return paginate(available,listingsPerPage,pageNum);
    }
    
    /**
     * Given a list of Strings, return one page's worth
     * assuming that Objects and lines of chat have a
     * one-to-one relationship, and only the specified
     * number of lines are allowed per page.
     * 
     * This effectively overrides the config file's
     * specification of lines per page.
     * 
     * @param available    All available Strings.
     * @param linesPerPage How many lines are allowed on a page.
     * @param pageNum      The index of the desired page, where
     *                     the first page is index 1.
     * @return             One page of Strings, pursuant to
     *                     the config file.
     */
    public String[] paginate(String[] available, int linesPerPage, int pageNum)
    {        
        //Convert from natural page number to index
        pageNum --;
        
        //Determine total number of possible pages
        int pagesAvailable = getPaginationSize(available,linesPerPage);
        
        //Prevent asking for a nonexistent page
        if(pageNum >= pagesAvailable)
            pageNum = pagesAvailable - 1;
        if(pageNum < 0)
            pageNum = 0;
        
        int startIndex = pageNum * linesPerPage;
        
        //Determine how many listings are on this page
        //(in case of last page)
        int qty = Math.min(
            available.length - startIndex,
            linesPerPage);
        
        String[] res = new String[qty];
        for(int i = startIndex; i < startIndex + linesPerPage && i < available.length; i++)
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
    public int getPaginationSize(Object[] available)
    {
        int listingsPerPage = PLUGIN.getConfig().getInt("chat.page-length");
        return getPaginationSize(available,listingsPerPage);
    }
    
    /**
     * Determine the total number of pages it would take
     * to express the given array of Objects,
     * assuming that Objects and lines of chat have a
     * one-to-one relationship, and only the specified
     * number of lines are allowed per page.
     * 
     * This effectively overrides the config file's
     * specification of lines per page.
     * 
     * @param available    All available Objects.
     * @param linesPerPage How many lines are allowed on a page.
     * @return             The number of pages required to
     *                     express these Objects.
     */
    public int getPaginationSize(Object[] available, int linesPerPage)
    {
        double fpa = ((double)available.length) / linesPerPage;
        int pagesAvailable = ((int)fpa) +
            (fpa % 1.00 == 0 ? 0 : 1);
        return pagesAvailable;
    }
    
    /**
     * Construct mouse-over text for the provided enchanted item.
     * 
     * @param mot           The display text that will be moused over.
     * @param lot           The lot number, for the first line of the tooltip.
     * @param merchandise   The item for which to generate a tooltip.
     * @return              A working chunk of text with a mouseover attached.
     */
    public TextComponent MOTforEnchanted(String mot,int lot,ItemStack merchandise)
    {
        return MOTforEnchanted(mot,lot,merchandise,false);
    }
    
    /**
     * Construct mouse-over text for the provided enchanted item.
     * 
     * @param mot           The display text that will be moused over.
     * @param lot           The lot number, for the first line of the tooltip.
     * @param merchandise   The item for which to generate a tooltip.
     * @param omitLotNumber Whether to omit the lot number for this tooltip.
     * @return              A working chunk of text with a mouseover attached.
     */
    public TextComponent MOTforEnchanted(String mot,int lot,ItemStack merchandise,boolean omitLotNumber)
    {
        String attrColName = PLUGIN.getConfig().getString("chat.colors.attribute");
        String attCol = PLUGIN.CM.color("attribute");
        String itemColName = PLUGIN.getConfig().getString("chat.colors.item");
        
        //Calculate damage percentage.
        //If the item is an enchanted book, use -1 to signify
        //  that we are ignoring damage.
        int dampercent = -1;
        if(!merchandise.getType().equals(Material.ENCHANTED_BOOK))
        {
            double dur = merchandise.getDurability();
            double maxdur = merchandise.getType().getMaxDurability();
            double dp = (dur / maxdur) * 100.0;
            //If the percentage is less than 1 but not exactly 0, show 1%.
            dampercent = (dp == 0.0 ? 0 :
                Math.max((int)dp,1));
        }
        
        TextComponent motext = new TextComponent();
        if(!omitLotNumber)
        {
            motext.setText("Lot #" + lot + "\n");
            motext.setColor(ChatColor.valueOf(itemColName));
        }
        if(merchandise.getItemMeta().hasDisplayName())
        {
            TextComponent nmtext = new TextComponent();
            nmtext.setText("\"" + merchandise.getItemMeta().getDisplayName() + "\"\n");
            nmtext.setColor(ChatColor.valueOf(itemColName));
            nmtext.setItalic(true);
            motext.addExtra(nmtext);
        }
        TextComponent use = new TextComponent();
        use.setText((dampercent < 0 ? "" : attCol + dampercent + "% used"));
        use.setColor(ChatColor.valueOf(attrColName));
        motext.addExtra(use);
        Set<Map.Entry<Enchantment,Integer>> entrySet =
            (merchandise.getType().equals(Material.ENCHANTED_BOOK) ?
                ((EnchantmentStorageMeta)merchandise.getItemMeta()).getStoredEnchants().entrySet() :
                merchandise.getEnchantments().entrySet());
        for(Map.Entry<Enchantment,Integer> enchant : entrySet)
        {
            TextComponent attr = new TextComponent();
            attr.setColor(ChatColor.valueOf(attrColName));
            attr.setText(
                (dampercent < 0 ? "" : "\n")    //Skip the first newline if the damage percentage isn't being shown.
                + PLUGIN.IM.getUsableName(enchant.getKey()) +
                (enchant.getKey().getMaxLevel() == 1 ? "" :
                    " " + ChatManager.romanNumeralize(enchant.getValue())));
            motext.addExtra(attr);
            if(dampercent < 0)  //This number doesn't matter anymore, but if it's less than 0 set it to 1
                dampercent = 1; // so that future newlines are not skipped.
        }
        
        TextComponent tc1 = new TextComponent();
        tc1.setText(mot);
        tc1.setHoverEvent(
            new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new BaseComponent[]{motext}));
        
        return tc1;
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
     * Notify the user that there is a general freeze.
     * 
     * @param sender    The source of the action to which we are to reply.
     * @return          Always returns true, to allow command executors to
     *                  send a message and terminate in one line.
     */
    public boolean denyGeneralFreeze(CommandSender sender)
    {
        return error(sender, "All Chat Shop assets are currently frozen. Please consult a staff member.");
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
