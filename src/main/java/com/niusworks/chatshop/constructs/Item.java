package com.niusworks.chatshop.constructs;

/**
 * Represents an item that exists in the items dictionary items.csv.
 * @author ObsidianCraft Staff
 */
public class Item
{
    /** The primary ID of the item. **/
    public final int ID;
    /** The data value of the item. 0 if not needed. **/
    public final int DMG;
    /** The official Minecraft name for this item. **/
    public final String MNAME;
    /** The display name for this item; usually its first alias. **/
    public final String DISPLAY;
    /** The maximum price for this item. 0 signifies no maximum. **/
    public final double MAXPRICE;
    /** The maximum quantity for this item. 0 signifies no maximum. **/
    public final int MAXQUANTITY;
    /** Whether this item is banned from use on the ChatShop by players. **/
    public final boolean ISBANNED;
    
    /**
     * Instantiate a new Item.
     * 
     * @param id          The primary ID of the item.
     * @param damage      The data value of the item. 0 if not needed.
     * @param mname       The official Minecraft name for this item.
     * @param display     The display name for this item.
     * @param maxprice    The maximum price for this item. 0 signifies no maximum.
     * @param maxquantity The maximum quantity for this item. 0 signifies no maximum.
     * @param banned      Whether this item is banned from use on the ChatShop by players.
     */
    public Item(int id, int damage,String mname,String display,double maxprice,int maxquantity,boolean banned)
    {
        ID = id;
        DMG = damage;
        MNAME = mname;
        DISPLAY = display;
        MAXPRICE = maxprice;
        MAXQUANTITY = maxquantity;
        ISBANNED = banned;
    }
}