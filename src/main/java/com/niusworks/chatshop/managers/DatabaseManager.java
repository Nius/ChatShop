package com.niusworks.chatshop.managers;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.commands.Buy;
import com.niusworks.chatshop.commands.Cancel;
import com.niusworks.chatshop.commands.Reprice;
import com.niusworks.chatshop.commands.Sell;
import com.niusworks.chatshop.constructs.Listing;
import com.niusworks.chatshop.constructs.Tender;

import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Manages all database functionality for OC Network's ChatShop.
 * <br>
 * <b>No other class or plugin should interact with ChatShop's tables.</b>
 * <br>
 * This class manages the ChatShop's database connection, ensures the proper
 * database schema, and generally protects the integrity of information in
 * the database tables.
 * <br>
 * This class holds synchronization control over the database. Though this can be
 * costly in environments with constant high volumes of queries, this is massively
 * important because it prevents corruption of database information and ensures that
 * items and monies are not duplicated or destroyed.
 * @author ObsidianCraft Staff
 */
public class DatabaseManager
{
    /** The master plugin for this manager. **/
    protected final ChatShop PLUGIN;
    
    /** The database connection. **/
    protected Connection connect;
    
    /**
     * Create an ItemManager with a reference to the master
     * plugin.
     * 
     * @param master    The master ChatShop plugin reference.
     */
    public DatabaseManager(ChatShop master)
    {
        PLUGIN = master;
    }
    
    /**
     * Connect to the database and perform any necessary setup.
     * 
     * @return  1 on success, 0 on fail.
     */
    @SuppressWarnings("unused")
    public int initialize()
    {
        try
        {
            int port = PLUGIN.getConfig().getInt("MySQL.port",3306);
            String database = PLUGIN.getConfig().getString("MySQL.database","ChatShop");
            String user = PLUGIN.getConfig().getString("MySQL.username","ChatSHop");
            String password = PLUGIN.getConfig().getString("MySQL.password","password");
            String host = PLUGIN.getConfig().getString("MySQL.host","localhost");
            
            String path =
                    "jdbc:mysql://" +
                    host +
                    ":" + port +
                    "/" + database +
                    "?user=" + user;
            
            //Load the database driver, and connect.
            PLUGIN.CM.log("Connecting to database " + path);
            
            Class.forName("com.mysql.jdbc.Driver");
            connect = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database,user,password);
            
            //Verify database schema.
            String query; int result;
            query = "CREATE TABLE IF NOT EXISTS ChatShop_listings("
                    + "id INT PRIMARY KEY AUTO_INCREMENT,"
                    + "material VARCHAR(39) NOT NULL,"      //Current longest official item name
                    + "damage INT NOT NULL,"
                    + "seller VARCHAR(36) NOT NULL,"        //Minecraft UUID length
                    + "sellerAlias VARCHAR(16) NOT NULL,"   //-- See below.
                    + "price DECIMAL(10,2) NOT NULL,"
                    + "quantity INT NOT NULL) ENGINE=INNODB";
            result = connect.createStatement().executeUpdate(query);
            query = "CREATE TABLE IF NOT EXISTS ChatShop_transactions("
                    + "id INT PRIMARY KEY AUTO_INCREMENT,"
                    + "material VARCHAR(39) NOT NULL,"      //Current longest official item name
                    + "damage INT NOT NULL,"
                    + "seller VARCHAR(36) NOT NULL,"        //Minecraft UUID length
                    + "sellerAlias VARCHAR(16) NOT NULL,"   //-- See below.
                    + "buyer VARCHAR(36) NOT NULL,"         //Minecraft UUID length
                    + "buyerAlias VARCHAR(16) NOT NULL,"    //-- See below.
                    + "price DECIMAL(10,2) NOT NULL,"
                    + "quantity INT NOT NULL,"
                    + "date TIMESTAMP NOT NULL DEFAULT NOW()) ENGINE=INNODB";
            result = connect.createStatement().executeUpdate(query);
            query = "CREATE TABLE IF NOT EXISTS ChatShop_players("
                    + "entryIndex INT PRIMARY KEY AUTO_INCREMENT,"
                    + "uuid VARCHAR(36) NOT NULL,"          //Minecraft UUID length
                    + "alias VARCHAR(16) NOT NULL,"         //-- See below.
                    + "flags VARCHAR(5) NOT NULL) ENGINE=INNODB";
            result = connect.createStatement().executeUpdate(query);
            /*
             * sellerAlias, buyerAlias, and alias in these tables are Minecraft usernames.
             * They serve two purposes: firstly, when Bukkit looks up playername by UUID
             * for a player who has been offline for a very long time it will return null.
             * In such cases -- and only then -- the database username is used.
             * Secondly, these usernames make the database much more readable in either
             * console or dump form, for administrators.
             */
        }
        catch(ClassNotFoundException|SQLException e)
        {
            PLUGIN.getLogger().log(Level.SEVERE,"Failed to load database.",e);
            e.printStackTrace();
            return -1;
        }
        return 1;
    }
    
    /**
     * Retrieve the desired player flag from the database.
     * Flags are indexed from left to right, so that index usage
     * could be thought of as <code>flags.charAt(index)</code>.
     * 
     * @param user      The user to query.
     * @param index     The index of the desired flag.
     * @return          The char value of the flag, or ' ' if the
     *                  flag was not defined. Returns ' ' on SQL fail.
     */
    public synchronized char getPlayerFlag(Player user,int index)
    {
        return getPlayerFlag(user.getUniqueId().toString(),index);
    }
    
    /**
     * Retrieve the desired player flag from the database.
     * Flags are indexed from left to right, so that index usage
     * could be thought of as <code>flags.charAt(index)</code>.
     * 
     * @param uuid      The user to query.
     * @param index     The index of the desired flag.
     * @return          The char value of the flag, or ' ' if the
     *                  flag was not defined. Returns ' ' on SQL fail.
     */
    protected synchronized char getPlayerFlag(String uuid, int index)
    {
        String query = "";
        if(index < 0)
            return ' '; //Flag out of bounds
        try
        {
            query = "SELECT flags FROM ChatShop_players "
                + "WHERE uuid = '" + uuid + "'";
            ResultSet res = connect.createStatement().executeQuery(query);
            
            if(!res.next())
                return ' '; //User hasn't been given flags yet
            
            String flags = "" + res.getString("flags");
            if(index + 1 > flags.length())
                return ' '; //Flag not defined
            
            char flag = flags.charAt(index);
            
            return flag;
        }
        catch (SQLException e)
        {
            error(query);
            e.printStackTrace();
            return ' ';
        }
    }
    
    /**
     * Write the desired player flag to the database.
     * Flags are indexed from left to right, so that index usage
     * could be thought of as <code>flags.charAt(index)</code>.
     * 
     * @param user      The user to update.
     * @param index     The index of the desired flag.
     * @param newFlag   The new value of the flag.
     */
    public synchronized void writePlayerFlag(Player user, int index, char newFlag)
    {
        writePlayerFlag(user.getUniqueId().toString(),user.getName(),index,newFlag);
    }
    
    /**
     * Write the desired player flag to the database.
     * Flags are indexed from left to right, so that index usage
     * could be thought of as <code>flags.charAt(index)</code>.
     * 
     * @param uuid      The user to update.
     * @param alias     The alias of the user in question.
     * @param index     The index of the desired flag.
     * @param newFlag   The new value of the flag.
     */
    protected synchronized void writePlayerFlag(String uuid, String alias, int index, char newFlag)
    {
        String query = "";
        if(index < 0)
            return; //Flag out of bounds
        try
        {            
            query = "SELECT flags FROM ChatShop_players "
                + "WHERE uuid = '" + uuid + "'";
            ResultSet res = connect.createStatement().executeQuery(query);
            
            String oldFlags = "";
            boolean hadEntry = false;
            if(res.next())
            {
                oldFlags = res.getString("flags");
                hadEntry = true;
            }
            
            //Expand the flags integer to be wide enough to include
            // the desired index.
            while(oldFlags.length() < index + 1)
                oldFlags += ' ';
            
            //Write the desired flag
            String out =
                oldFlags.substring(0,index) +
                newFlag +
                (index == oldFlags.length() + 1 ? "" : oldFlags.substring(index + 1));
            
            if(hadEntry)
                query = "UPDATE ChatShop_players "
                    + "SET flags = '" + out + "' "
                    + ", alias = '" + alias + "' "
                    + "WHERE uuid = '" + uuid + "'";
            else
                query = "INSERT INTO ChatShop_players VALUES("
                    + "null,'" + uuid + "',"
                    + "'" + alias + "',"
                    + "'" + out + "')";
            @SuppressWarnings("unused")
            int unused = connect.createStatement().executeUpdate(query);
        }
        catch (SQLException e)
        {
            error(query);
            e.printStackTrace();
            return; //SQL problem
        }
    }
    
    /**
     * Determine whether the ChatShop is under a general freeze.
     * 
     * @return  Whether the ChatShop is under a general freeze.
     */
    public synchronized boolean isGeneralFreeze()
    {
        return (getPlayerFlag("-1",0) == 'F');
    }
    
    /**
     * Toggle the general freeze state of the ChatShop.
     * 
     * @return The NEW state of the general freeze.
     */
    public synchronized boolean toggleGeneralFreeze()
    {
        boolean wasFrozen = isGeneralFreeze();
        writePlayerFlag("-1","ChatShop",0,(wasFrozen ? ' ' : 'F'));
        return !wasFrozen;
    }
    
    /**
     * Get this player's current listing for the specified merch.
     * 
     * @param user          The UUID of the user in question.
     * @param merchandise   The merchandise being sought after.
     * @return              A single listing, or null if none exists.
     */
    public synchronized Listing getListing(Player user, ItemStack merchandise)
    {
        if(merchandise == null)
            return null;
        
        String query = "SELECT * FROM ChatShop_listings WHERE seller = '" + user.getUniqueId() + "'"
            + " AND material = '" + merchandise.getType() + "'"
            + " AND damage = '" + merchandise.getDurability() + "'";
        try
        {
            ResultSet res = connect.createStatement().executeQuery(query);
            if(!res.next())
                return null;
            return new Listing (
                    res.getInt("id"),
                    res.getString("material"),
                    res.getInt("damage"),
                    res.getString("seller"),
                    res.getString("sellerAlias"),
                    res.getDouble("price"),
                    res.getInt("quantity"));
        }
        catch(SQLException e)
        {
            error(query);
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Get all listings (regardless of player) for the specified item.
     * 
     * @param merchandise   The item for which to get listings.
     * @return              All matching listings, ordered by price ASC.
     *                      If no listings are found, a {@link Listing}[]
     *                      object of length 0 will be returned.
     *                      null will be returned on SQL failure.
     */
    public synchronized Listing[] getListings(ItemStack merchandise)
    {
        ArrayList<Listing> listings = new ArrayList<Listing>();
        String query = "SELECT * FROM ChatShop_listings" +
                " WHERE material = '" + merchandise.getType() + "'" +
                " AND damage = '" + merchandise.getDurability() + "'" +
                " ORDER BY price ASC";
        try
        {
            ResultSet res = connect.createStatement().executeQuery(query);
            while(res.next())
                listings.add(new Listing (
                        res.getInt("id"),
                        res.getString("material"),
                        res.getInt("damage"),
                        res.getString("seller"),
                        res.getString("sellerAlias"),
                        res.getDouble("price"),
                        res.getInt("quantity")));
            return listings.toArray(new Listing[listings.size()]);            
        }
        catch(SQLException e)
        {
            error(query);
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Get all listings (regardless of item type) for the specified player.
     * 
     * @param qPlayer       The item for which to get listings.
     * @return              All matching listings, ordered by material.
     *                      If no listings are found, a {@link Listing}[]
     *                      object of length 0 will be returned.
     *                      null will be returned on SQL failure.
     */
    public synchronized Listing[] getListings(OfflinePlayer qPlayer)
    {
        ArrayList<Listing> listings = new ArrayList<Listing>();
        String query = "SELECT * FROM ChatShop_listings" +
                " WHERE seller = '" + qPlayer.getUniqueId() + "'" +
                " ORDER BY material";
        try
        {
            ResultSet res = connect.createStatement().executeQuery(query);
            while(res.next())
                listings.add(new Listing (
                        res.getInt("id"),
                        res.getString("material"),
                        res.getInt("damage"),
                        res.getString("seller"),
                        res.getString("sellerAlias"),
                        res.getDouble("price"),
                        res.getInt("quantity")));
            return listings.toArray(new Listing[listings.size()]);            
        }
        catch(SQLException e)
        {
            error(query);
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Get the transaction history of the specified player.
     * 
     * @param qPlayer   The player whose history to compile.
     * @return          A list of listings in reverse order by
     *                  date, such that the QUANTITY is positive
     *                  when the queried player was the buyer and
     *                  negative when the queried player was the
     *                  seller.
     */
    public synchronized Listing[] getHistory(OfflinePlayer qPlayer)
    {
        ArrayList<Listing> sales = new ArrayList<Listing>();
        String query = "SELECT * FROM ChatShop_transactions " +
            "WHERE seller = '" + qPlayer.getUniqueId().toString() + "' " +
            "OR buyer = '" + qPlayer.getUniqueId().toString() + "' " +
            "ORDER BY date DESC";
        
        try
        {
            ResultSet res = connect.createStatement().executeQuery(query);
            while(res.next())
            {
                String selleruuid = res.getString("seller");
                boolean qWasSeller = selleruuid.equalsIgnoreCase(qPlayer.getUniqueId().toString());
                sales.add(new Listing (
                        res.getInt("id"),
                        res.getString("material"),
                        res.getInt("damage"),
                        (qWasSeller ? res.getString("buyer") : selleruuid),
                        (qWasSeller ? res.getString("buyerAlias") : res.getString("sellerAlias")),
                        res.getDouble("price"),
                        res.getInt("quantity") * (qWasSeller ? -1 : 1),
                        res.getTimestamp("date")));
            }
            return sales.toArray(new Listing[sales.size()]);            
        }
        catch(SQLException e)
        {
        	e.printStackTrace();
            error(query);
        }
        return null;
    }
    
    /**
     * Execute a cancel operation.
     * This method resides here and not with {@link Cancel} in order
     * to manage synchronization with the database.
     * 
     * @param usr       The player who executed the cancel command.
     * @param merch     The (validated) items to cancel.
     * @return          -2 on SQL fail, -1 on no stock to cancel,
     *                  otherwise a positive int representing the
     *                  total number of items cancelled.
     */
    @SuppressWarnings("unused")
    public synchronized int cancel(Player usr, ItemStack merch)
    {
        try
        {
            Listing stock = getListing(usr,merch);
            if(stock == null)
                return -1;
            
            if(merch.getAmount() == -1 || merch.getAmount() >= stock.QUANTITY)
            {
                //The specified amount was either "all" or
                // greater than the available amount, so the
                // listing will be removed rather than updated.
                
                String query = "DELETE FROM ChatShop_listings WHERE id = " + stock.ID;
                int unused = connect.createStatement().executeUpdate(query);
                
                return stock.QUANTITY;
            }
            
            int targetQty = (stock.QUANTITY - merch.getAmount());
            String query =
                "UPDATE ChatShop_listings SET quantity = " +
                + targetQty + ", sellerAlias = '" + usr.getName() + "' WHERE id = " + stock.ID;
            int unused = connect.createStatement().executeUpdate(query);
            
            return merch.getAmount();
        }
        catch(SQLException e)
        {
        	e.printStackTrace();
            return -2;
        }
    }
    
    /**
     * Price a buy operation.
     * This method resides here and not with {@link Buy} in order
     * to manage synchronization with the database.
     * 
     * @param usr       The player who executed the buy command.
     * @param merch     The (validated) items to potentially buy.
     * @param maxp      The maximum price of purchase. -1 signifies
     *                  omission of maxprice.
     * @return          The total price the player would pay if they
     *                  had, at this moment, executed a buy with the
     *                  same arguments.
     *                  Returns -1 on SQL fail.
     *                  If the requested amount is more than is available
     *                  then the total available amount will be returned.
     */
    public synchronized double price(Player usr, ItemStack merch, double maxp)
    {
        Tender res = buy(usr,merch,maxp,true);
        if(res == null)
            return -1;
        if(res.QUANTITY < merch.getAmount())
            return -1 * res.QUANTITY;
        return res.COST;
    }
    
    /**
     * Execute a buy operation.
     * This method resides here and not with {@link Buy} in order
     * to manage synchronization with the database.
     * 
     * @param usr       The player who executed the buy command.
     * @param merch     The (validated) items to buy.
     * @param maxp      The maximum price of purchase. -1 signifies
     *                  omission of maxprice.
     * @return          A Tender representing the total quantity and
     *                  TOTAL COST OF ALL ITEMS tendered, or null
     *                  on fail.
     *                  
     */
    public synchronized Tender buy(Player usr, ItemStack merch, double maxp)
    {
        return buy(usr,merch,maxp,false);
    }
    
    /**
     * Execute a buy operation.
     * This method resides here and not with {@link Buy} in order
     * to manage synchronization with the database.
     * 
     * @param usr       The player who executed the buy command.
     * @param merch     The (validated) items to buy.
     * @param maxp      The maximum price of purchase. -1 signifies
     *                  omission of maxprice.
     * @param pricingOnly   Whether this command is being executed only to
     *                      compile a price, or to actually carry out a buy
     *                      operation.
     * @return          A Tender representing the total quantity and
     *                  TOTAL COST OF ALL ITEMS tendered, or null
     *                  on fail.
     *                  
     */
    @SuppressWarnings("unused")
    protected synchronized Tender buy(Player usr, ItemStack merch, double maxp, boolean pricingOnly)
    {
        String query = "";
        try
        {
            ArrayList<Listing> listings = new ArrayList<Listing>();
            
            //Query all listings which match the search criteria.
            query = "SELECT * FROM ChatShop_listings "
                    + "WHERE material = '" + merch.getType() + "' "
                    + "AND damage = " + merch.getDurability() + " ";
            if(maxp != -1)
                query += "AND price <= " + maxp + " ";
            query += "ORDER BY price ASC";
            
            //Compile relevant listings into a malleable data structure.
            ResultSet res = connect.createStatement().executeQuery(query);
            while(res.next())
                listings.add(new Listing(
                        res.getInt("id"),
                        res.getString("material"),
                        res.getInt("damage"),
                        res.getString("seller"),
                        res.getString("sellerAlias"),
                        res.getDouble("price"),
                        res.getInt("quantity")));
        
            //Cycle through the listings, counting how many items from each seller
            // and the total accrued cost.
            //Execute a transaction for each listing.
            double totalCost = 0;   //Total accrued cost of processed listings.
            int totalMerch = 0;     //Total quantity from processed listings.
            double avbal = PLUGIN.ECON.getBalance(usr); //Player balance.
            boolean broke = false;
            int self = 0;
            for(int i = 0; i < listings.size(); i ++)
            {
                Listing listing = listings.get(i);
                boolean isLast = false;
                int thisQuantity = 0;
                
                double listingCost = listing.PRICE * listing.QUANTITY;
                if((!pricingOnly && totalCost + listingCost > avbal) ||
                        totalMerch + listing.QUANTITY > merch.getAmount())
                {
                    //This is the last listing, because it meets or exceeds the limit
                    // of either the player's balance or the requested quantity of
                    // goods.
                    
                    
                    
                    isLast = true;
                    if(pricingOnly)
                        thisQuantity = merch.getAmount() - totalMerch;
                    else
                        thisQuantity = Math.min(
                                //The amount the user can afford
                                (int)((avbal - totalCost) / listing.PRICE),
                                //The amount the user asked for
                                merch.getAmount() - totalMerch);
                    
                    broke = thisQuantity < merch.getAmount() - totalMerch;
                    
                    //If no more can be bought, stop processing this final listing.
                    if(thisQuantity == 0)
                        break;
                    
                    if(listing.PLAYER_UUID.equals(usr.getUniqueId().toString()))
                        self = thisQuantity;
                    
                    listingCost = thisQuantity * listing.PRICE;
                    
                    //Update this listing in the market.
                    if(!pricingOnly)
                    {
                        query = "UPDATE ChatShop_listings SET quantity = " +
                                (listing.QUANTITY - thisQuantity) +
                                " WHERE id = " + listing.ID;
                        int unused = connect.createStatement().executeUpdate(query);
                    }
                }
                else
                {
                    //This is NOT the last listing, because the entirety of this
                    // particular stock is affordable and demanded by the user.
                    
                    thisQuantity = listing.QUANTITY;
                    if(listing.PLAYER_UUID.equals(usr.getUniqueId().toString()))
                        self = thisQuantity;
                    
                    //Remove this listing from the market.
                    if(!pricingOnly)
                    {
                        query = "DELETE FROM ChatShop_listings WHERE id = "
                                + listing.ID;
                        int unused = connect.createStatement().executeUpdate(query);
                    }
                }
                
                //Pay the player who had the listing.
                UUID seller = UUID.fromString(listing.PLAYER_UUID);
                if(!pricingOnly)
                {
                    PLUGIN.ECON.depositPlayer(
                            PLUGIN.getServer().getOfflinePlayer(seller),
                            listingCost);
                }
                
                //Notify the seller that this transaction took place.
                //  This database manager is responsible for notifying players
                //    who seed this purchase, so that (1) this list of listings
                //    does not need to be passed to any Buy instance and
                //    (2) this list does not need to be traversed more than
                //    once.
                if(!pricingOnly)
                {
                    OfflinePlayer slr = PLUGIN.getServer().getOfflinePlayer(seller);
                    if(slr != null && slr.isOnline() &&
                        !slr.getUniqueId().equals(usr.getUniqueId()))
                    {
                        String displayName = PLUGIN.IM.getDisplayName(merch);
                        if(slr != null) // If is online
                        {
                            String msg =
                                PLUGIN.CM.color("player") + usr.getName() + " " +
                                PLUGIN.CM.color("text") + "just bought " +
                                PLUGIN.CM.color("quantity") + ChatManager.format(thisQuantity) + " " +
                                PLUGIN.CM.color("item") + displayName + " " +
                                PLUGIN.CM.color("text") + "from you for " +
                                PLUGIN.CM.color("price") + ChatManager.format(listingCost) +
                                PLUGIN.CM.color("text") + ".";
                            PLUGIN.CM.reply(PLUGIN.getServer().getPlayer(seller),msg);
                        }
                    }
                }
                
                //Add the cost of this listing to the total accrued charge.
                totalCost += listingCost;
                
                //Add the quantity purchased from this listing to the total purchased quantity.
                totalMerch += thisQuantity;
                
                //Log this transaction.
                if(!pricingOnly)
                {
                    query = "INSERT INTO ChatShop_transactions VALUES ("
                            + "null, '" + merch.getType() + "', "
                            + merch.getDurability() + ", "
                            + "'" + listing.PLAYER_UUID + "', "
                            + "'" + Bukkit.getOfflinePlayer(UUID.fromString(listing.PLAYER_UUID)).getName() + "', "
                            + "'" + usr.getUniqueId() + "', "
                            + "'" + usr.getName() + "', "
                            + listing.PRICE + ", "
                            + thisQuantity + ", "
                            + "null)";
                    int unused = connect.createStatement().executeUpdate(query);
                }
            }
            
            //Return the quantity and price ultimately accrued.
            return new Tender(totalMerch,totalCost,broke,self);
        }
        catch(SQLException e)
        {
            error(query);
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Execute a sell operation.
     * This method resides here and not with {@link Sell} in order
     * to manage synchronization with the database.
     * 
     * @param usr       The player who executed the sell command.
     * @param merch     The (validated) items to sell.
     * @param price     The price (each, not total) for the merchandise.
     *                  -1 indicates the previously existing price should be used.
     * @return          -3 on maximum quantity exceeded.
     *                  -2 on SQL fail.
     *                  -1 if the user used "-" as price and does not have a listing.
     *                  0 on new listing.
     *                  else the original pre-existing listing.
     */
    @SuppressWarnings("unused")
    public synchronized Object sell(Player usr, ItemStack merch, double price)
    {
        String query = "";
        try
        {
            Listing current = getListing(usr,merch);
            
            // The user already has a listing for this item...
            if(current != null)
            {
                //A price of -1 is passed if the user used "-" for the price
                // argument, denoting that the already-listed price is to be
                // used.
                if(price == -1)
                    price = current.PRICE;
                
                int maxq = PLUGIN.IM.lookup(merch).MAXQUANTITY;
                if(maxq > 0 && current.QUANTITY + merch.getAmount() > maxq)
                    return -3;
                
                query = "UPDATE ChatShop_listings SET quantity = " + (merch.getAmount() + current.QUANTITY)
                        + ", price = " + price
                        + ", sellerAlias = '" + usr.getName() + "'"
                        + " WHERE id = " + current.ID;
                int unused = connect.createStatement().executeUpdate(query);
                return current;
            }
            
            if(price == -1)
                return -1;
            
            // The user does not have a listing, so one must be created.
            query = "INSERT INTO ChatShop_listings VALUES("
                    + "null,"
                    + "'" + merch.getType() + "',"
                    + merch.getDurability() + ","
                    + "'" + usr.getUniqueId() + "',"
                    + "'" + usr.getName() + "',"
                    + price + ","
                    + merch.getAmount() + ")";
            int unused = connect.createStatement().executeUpdate(query);
            return 0;
        }
        catch(SQLException e)
        {
            error(query);
            e.printStackTrace();
        }
        return -2;
    }
    
    /**
     * Execute a reprice operation.
     * This method resides here and not with {@link Reprice} in order
     * to manage synchronization with the database.
     * 
     * @param usr       The player who executed the sell command.
     * @param merch     The (validated) items to sell.
     * @param price     The price (each, not total) for the merchandise.
     * @return          -1 on SQL fail, 0 on no listing, or else
     *                  the original pre-existing listing.
     */
    @SuppressWarnings("unused")
    public synchronized Object reprice(Player usr, ItemStack merch, double price)
    {
        String query = "";
        try
        {
            Listing current = getListing(usr,merch);
            
            // The user already has a listing for this item...
            if(current != null)
            {                
                query = "UPDATE ChatShop_listings SET price = " + price
                        + ", sellerAlias = '" + usr.getName() + "' WHERE id = " + current.ID;
                int unused = connect.createStatement().executeUpdate(query);
                return current;
            }
            
            // The user did not have a listing
            return 0;
        }
        catch(SQLException e)
        {
            error(query);
            e.printStackTrace();
        }
        return -1;
    }
    
    /**
     * Log an error to the console involving the specified query.
     * 
     * @param query This query will be logged verbatim to the console.
     */
    public void error(String query)
    {
        PLUGIN.CM.severe("Unexpected error occurred with query: \"" + query + "\"");
    }
    
    /**
     * Send a query to the database server to keep the connection from timing out.
     * The MySQL default timeout (for interactive connections) is 8 hours; if the
     * ChatShop were to be unused for 8 hours then subsequent queries would fail.
     * This feature allows ChatShop to be run on quieter servers without having to
     * reconfigure MySQL.
     * 
     * Note that despite this method's direct interaction with the database it
     * does not need to be synchronized, because it (A) does not actually read any
     * meaningful information from the database and (B) does not write to the database
     * at all.
     */
    public void keepAlive()
    {
        String query = "SELECT id FROM ChatShop_transactions WHERE FALSE";
        try
        {
            connect.createStatement().executeQuery(query);
        }
        catch(SQLException e)
        {
            error("Error executing keep-alive query.");
            error(query);
            e.printStackTrace();
        }
    }
    
    /** Close the database connection. **/
    public void close()
    {
       try
       {
           connect.close();
       }
       catch (Exception e)
       {
           PLUGIN.CM.severe("Unexpected error attempting to close the database connection.");
           e.printStackTrace();
       }
    }
}