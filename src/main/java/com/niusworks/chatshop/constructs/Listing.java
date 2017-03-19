package com.niusworks.chatshop.constructs;

import java.sql.Timestamp;

/**
 * Represents a listing that exists in the database.
 * @author ObsidianCraft Staff
 */
public class Listing
{
    /** Unique ID of this listing in the database. **/
    public final int ID;
    /** The official Minecraft name for this material. **/
    public final String MATERIAL;
    /** The damage value of the specified item. **/
    public final int DAMAGE;
    /** The UUID of the relevant player. **/
    public final String PLAYER_UUID;
    /** The alias of the relevant player, as displayed in the database. **/ 
    public final String PLAYER_ALIAS;
    /** The price per item. **/
    public final double PRICE;
    /** The quantity for sale. **/
    public final int QUANTITY;
    /** An optional MySQL Timestamp. **/
    public final Timestamp DATE;
    
    /**
     * Create a new Listing object with no date.
     * 
     * @param id        Unique ID of this listing in the database.
     * @param mat       The official Minecraft name for this material.
     * @param dmg       The damage value of the specified item.
     * @param uuid      The UUID of the involved player.
     * @param alias     The alias of the involved player.
     * @param price     The price per item.
     * @param qty       The quantity for sale.
     */
    public Listing(int id, String mat, int dmg, String uuid, String alias, double price, int qty)
    {
        ID = id; MATERIAL = mat; DAMAGE = dmg; PLAYER_UUID = uuid; PLAYER_ALIAS = alias; PRICE = price; QUANTITY = qty; DATE = null;
    }
    
    /**
     * Create a new Listing object with a specific date.
     * 
     * @param id        Unique ID of this listing in the database.
     * @param mat       The official Minecraft name for this material.
     * @param dmg       The damage value of the specified item.
     * @param uuid      The UUID of the involved player.
     * @param alias     The alias of the involved player.
     * @param price     The price per item.
     * @param qty       The quantity for sale.
     * @param date      A SQL Timestamp.
     */
    public Listing(int id, String mat, int dmg, String uuid, String alias, double price, int qty, Timestamp date)
    {
        ID = id; MATERIAL = mat; DAMAGE = dmg; PLAYER_UUID = uuid; PLAYER_ALIAS = alias; PRICE = price; QUANTITY = qty; DATE = date;
    }
}