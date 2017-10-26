package com.niusworks.chatshop.constructs;

import java.sql.Timestamp;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import com.niusworks.chatshop.commands.History;
import com.niusworks.chatshop.managers.DatabaseManager;

/**
 * Represents a listing for an enchanted item that exists in the database.
 * <br>
 * Currently this class is used in two different contexts: firstly to represent a current listing
 * on the market - this is its most common usage, and does not utilize the {@link #DATE}.
 * <br>
 * {@link History} (by means of {@link DatabaseManager#getHistory}) uses this to represent a
 * transaction which has already occurred; in this case the {@link #DATE} is used and the
 * attached player ({@link #PLAYER_ALIAS},{@link #PLAYER_UUID}) is, rather than the queried
 * player (usually the caller), the third-party (unknown) player invoved in the transaction.
 * <br>
 * For example, if I query /history JimmerMcSpock then a series of Listings is returned but the
 * attached player for each listing is the other player involved in JimmerMcSpock's
 * transaction. In this way both parties related to the transaction are known. 
 * 
 * @author ObsidianCraft Staff
 */
public class EListing extends Listing
{
    /** The list of Enchantments attached to this listing. **/
    public final EnchLvl[] ENCHANTS;
    /** The Enchantment encoded String. **/
    public final String ENCHANTS_STRING;
    /** The name of the item (as named by the player). **/
    public final String ITEM_NAME;
    
    /**
     * Create a new EListing object with no date.
     * 
     * @param id        Unique ID of this listing in the database.
     * @param mat       The official Minecraft name for this material.
     * @param dmg       The damage value of the specified item.
     * @param uuid      The UUID of the involved player.
     * @param alias     The alias of the involved player.
     * @param price     The price per item.
     * @param itemName  The name of the item (as named by the player).
     * @param enchants  The enchantments attached to this listing.
     * @param estring   The encoded String representation of enchantments.
     */
    public EListing(int id, String mat, int dmg, String uuid, String alias, double price, String itemName, EnchLvl[] enchants, String estring)
    {
        super(id,mat,dmg,uuid,alias,price,1);
        ITEM_NAME = itemName;
        ENCHANTS = enchants;
        ENCHANTS_STRING = estring;
    }
    
    /**
     * Create a new EListing object with a specific date.
     * 
     * @param id        Unique ID of this listing in the database.
     * @param mat       The official Minecraft name for this material.
     * @param dmg       The damage value of the specified item.
     * @param uuid      The UUID of the involved player.
     * @param alias     The alias of the involved player.
     * @param price     The price per item.
     * @param itemName  The name of the item (as named by the player).
     * @param enchants  The enchantments attached to this listing.
     * @param estring   The encoded String representation of enchantments.
     * @param date      A SQL Timestamp.
     */
    public EListing(int id, String mat, int dmg, String uuid, String alias, double price, String itemName, EnchLvl[] enchants, String estring, Timestamp date)
    {
        super(id,mat,dmg,uuid,alias,price,1,date);
        ITEM_NAME = itemName;
        ENCHANTS = enchants;
        ENCHANTS_STRING = estring;
    }
    
    /**
     * <b>Discouraged.</b><br>
     * Create a new EListing object with a specific date and a specific quantity.
     * This is strictly to facilitate {@link DatabaseManager#getHistory(OfflinePlayer, boolean)}
     * and should not be used for any other purpose.
     * 
     * @param id        Unique ID of this listing in the database.
     * @param mat       The official Minecraft name for this material.
     * @param dmg       The damage value of the specified item.
     * @param uuid      The UUID of the involved player.
     * @param alias     The alias of the involved player.
     * @param price     The price per item.
     * @param itemName  The name of the item (as named by the player).
     * @param enchants  The enchantments attached to this listing.
     * @param estring   The encoded String representation of enchantments.
     * @param qty       The quantity for this listing.
     * @param date      A SQL Timestamp.
     */
    public EListing(int id, String mat, int dmg, String uuid, String alias, double price, String itemName, EnchLvl[] enchants, String estring, int qty, Timestamp date)
    {
        super(id,mat,dmg,uuid,alias,price,qty,date);
        ITEM_NAME = itemName;
        ENCHANTS = enchants;
        ENCHANTS_STRING = estring;
    }
    
    /**
     * @return  An ItemStack with the set {@link #MATERIAL}, {@link #QUANTITY} and {@link #DAMAGE}
     *          as well as all {@link #ENCHANTS} and the {@link #ITEM_NAME}.
     */
    @Override
    public ItemStack toItemStack()
    {
        ItemStack ret = super.toItemStack();
        
        for(EnchLvl enchant : ENCHANTS)
            if(ret.getType().equals(Material.ENCHANTED_BOOK))
            {
                EnchantmentStorageMeta esm = ((EnchantmentStorageMeta)ret.getItemMeta());
                esm.addStoredEnchant(enchant.ENCHANT,enchant.LVL,true);
                ret.setItemMeta(esm);
            }
            else
                ret.addEnchantment(enchant.ENCHANT,enchant.LVL);
        
        if(ITEM_NAME != null)
        {
            ItemMeta im = ret.getItemMeta();
            im.setDisplayName(ITEM_NAME);
            ret.setItemMeta(im);
        }
        
        return ret;
    }
}
