package com.niusworks.chatshop.managers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.niusworks.chatshop.ChatShop;

/**
 * Manages all Minecraft item handling functionality for
 * OC Network's ChatShop.
 * @author ObsidianCraft Staff
 */
public class ItemManager
{
    /**
     * A map of aliases, including the official Bukkit material name, to items.
     * This is the primary means of item lookup.
     */
    protected HashMap<String,Item> aliases = new HashMap<String,Item>();
    
    /**
     * A two-dimensional hash map of items; dimension 1 being item ID and dimension 2 being damage value.
     * {@link #aliases} maintains a list of all aliases for each item, including the Bukkit material name,
     * and is the primary means of item lookups considering that most users will search by string rather
     * than by ID.
     * This list serves to make lookups by ID:DMG much more efficient by removing the need to search through
     * all named items and compare their attributes.
     */
    protected HashMap<Integer,HashMap<Integer,Item>> items = new HashMap<Integer,HashMap<Integer,Item>>();
    
    /** The master plugin for this manager. **/
    protected final ChatShop PLUGIN;
    
    /**
     * Create an ItemManager with a reference to the master
     * plugin.
     * 
     * @param master    The master ChatShop plugin reference.
     */
    public ItemManager(ChatShop master)
    {
        PLUGIN = master;
    }
    
    /**
     * Load and parse the items CSV.
     * 
     * @param dataFolder    The directory in which to look for "items.csv".
     * @return              -1 on success, -2 on I/O fail, -3 on failure to
     *                      spawn a new file if it was missing, else the line
     *                      number where a parse fail occurred.
     */
    public int initialize(File dataFolder)
    {
        //In case of reload, clear existing items.
        items.clear();
        aliases.clear();
        
        int totalLoaded = 0;
        int totalCommented = 0;
        int totalBanned = 0;
        
        try
        {
            //Create the items.csv if it does not exist.
            File itemFile = new File(dataFolder,"items.csv");
            if(!itemFile.exists())
            {
                PLUGIN.CM.log("Items file is missing. Spawning a new one now.");
                URL inputUrl = getClass().getResource("/items.csv");
                FileUtils.copyURLToFile(inputUrl, itemFile);
            }
            else
                PLUGIN.CM.log("Items file found. Loading now...");
                
            //Parse items.csv into a malleable datastructure of Items.
            BufferedReader reader = new BufferedReader(new FileReader(itemFile));
            int i = 0;
            for(i = 0; reader.ready(); i ++)
            {
                try
                {                    
                    String line = reader.readLine().trim();
                    
                    //Skip commented or empty lines.
                    if(line.startsWith("#") || line.length() < 1)
                    {
                        totalCommented ++;
                        continue;
                    }
                    
                    //Skip banned entries
                    boolean isban = false;
                    String[] flags = line.split("!");
                    for(String flag : flags)
                        if(flag.trim().equalsIgnoreCase("BAN") || flag.trim().equalsIgnoreCase("TECHNICAL"))
                        {
                            isban = true;
                            break;
                        }
                    if(isban)
                    {
                        totalBanned ++;
                        continue;
                    }
                    
                    String[] tokens = flags[0].split(",");
                    
                    //Read ID:DMG
                    int dam = 0, id = 0;
                    try
                    {
                        boolean hasDam = tokens[0].contains(":");
                        dam = (hasDam ? Integer.parseInt(tokens[0].split(":")[1]) : 0);
                        id = Integer.parseInt(hasDam ? tokens[0].split(":")[0] : tokens[0]);
                    }
                    catch(NumberFormatException e)
                    {
                        reader.close();
                        return i;
                    }
                    
                    //Read minecraft official name
                    String mname = tokens[1];
                    
                    //Read all aliases
                    //Aliases which contain spaces will be registered twice:
                    //  once with spaces removed, and
                    //  once with spaces replaced with underscores.
                    String[] alii = new String[tokens.length - 2];
                    for(int j = 2; j < tokens.length; j ++)
                    {
                        alii[j-2] = tokens[j].trim();
                    }
                    
                    //Read remaining flags
                    double maxPrice = 0;
                    int maxQuantity = 0;
                    for(String flag : flags)
                        if(flag.trim().toUpperCase().startsWith("MAXPRICE="))
                        {
                            try { maxPrice = Double.parseDouble(flag.substring(9)); }
                            catch(NumberFormatException e){ return i; }
                        }
                        else if(flag.trim().toUpperCase().startsWith("MAXQTY="))
                        {
                            try { maxQuantity = Integer.parseInt(flag.substring(7)); }
                            catch(NumberFormatException e){ return i; }
                        }
                    
                    //Read the display name from the first alias but maintain capitalization.
                    String display = "";
                    if(display.length() == 0)
                        display = tokens[2].trim().replaceAll("\\s","");
                    
                    //Store the new item in the items 2D-hash
                    //In case of duplicates, the latest entry will prevail.
                    Item itm = new Item(id, dam, mname.trim().toUpperCase(),(display.length() > 0 ? display.trim() : alii[0].trim()),maxPrice,maxQuantity);
                    if(!items.containsKey(id))                      //If this ID is undefined...
                        items.put(id,new HashMap<Integer,Item>());  //...create a new map for it.
                    if(!items.get(id).containsKey(dam))             //If this exact item is undefined...
                        items.get(id).put(dam,itm);                 //...store it.
                    
                    //Store all aliases.
                    //Aliases with spaces will be stored twice:
                    //  once with spaces removed,
                    //  once with spaces replaced with underscores.
                    for(String alias : alii)
                    {
                        aliases.put(alias.toUpperCase().replaceAll("\\s",""),itm);
                        aliases.put(alias.toUpperCase().replaceAll("\\s","_"),itm);
                    }
                    //Store the official Minecraft name as an alias only for items with damage 0.
                    if(dam == 0)
                        aliases.put(mname.trim().toUpperCase(),itm);
                    
                    totalLoaded ++;
                }
                catch(ArrayIndexOutOfBoundsException|StringIndexOutOfBoundsException e)
                {
                    return i;
                }
            }
            reader.close();
        }
        catch (IOException e)
        {
            return -2;
        }
        
        PLUGIN.CM.log("Loaded " + totalLoaded + " items. (" + totalBanned
            + " banned items and " + totalCommented + " commentes, totaling "
            + (totalLoaded + totalBanned + totalCommented) + " lines.)");
        return -1;
    }
    
    /**
     * Gives the specified ItemStack to the user, attempting to put items
     * in the main hand first, then disubursing remaining items to the
     * player's inventory.
     * Items which do not fit in the inventory are dropped on the ground.
     * 
     * @param usr   The user to whom to give the items.
     * @param gift  The items to give to the player.
     * @return      True if items were dropped on the ground, otherwise false.
     */
    public boolean giveItem(Player usr, ItemStack gift)
    {
        PlayerInventory inv = usr.getInventory();
        int given = 0;
        //Query the maximum stack size of this item type, but protect against
        // a -1 response from the API, whose documentation did not engender
        // confidence in its own robustness.
        int stackSize = Math.max(gift.getMaxStackSize(),1);
        
        //Try to put the items in the player's (main) hand first.
        ItemStack itm = inv.getItemInMainHand();
        if(itm == null || isAir(itm) || areSameItem(itm,gift))
        {
            //Put no more than was specified or the maximum stack size of
            // this item.
            int toPut = Math.min(gift.getAmount(),stackSize);
            
            //Put no more than can fit on top the amount that was already
            // in hand.
            if(!isAir(itm))
                toPut = Math.min(toPut,stackSize - itm.getAmount());
            
            //If there is still room in hand (after checking stack size limits)
            // then put some stuff.
            if(toPut != 0)
            {
                given = toPut;
                ItemStack newItm = new ItemStack(gift);
                newItm.setAmount(isAir(itm) ? toPut : itm.getAmount() + toPut);
                inv.setItemInMainHand(newItm);
            }
        }
        
        //Disburse the remaining items throughout the player's inventory.
        gift.setAmount(gift.getAmount() - given);
        HashMap<Integer,ItemStack> notAdded = inv.addItem(gift);
        
        //Drop on the ground items which could not fit in the player's inventory.
        boolean didDrop = false;
        for(ItemStack toDrop : notAdded.values())
        {
            didDrop = true;
            usr.getWorld().dropItem(usr.getLocation(),toDrop);
            String displayName = getDisplayName(toDrop);
            String msg =
                    PLUGIN.CM.color("text") + "Insufficient space in your inventory: there are now " +
                    PLUGIN.CM.color("quantity") + toDrop.getAmount() + " " +
                    PLUGIN.CM.color("item") + displayName + " " +
                    PLUGIN.CM.color("error") + "on the ground " +
                    PLUGIN.CM.color("text") + "below you.";
            
            PLUGIN.CM.reply(usr,msg);
        }
        
        return didDrop;
    }
          
    /**
     * Look up a valid Minecraft item by ItemStack.
     * This is essentially a reverse-lookup from validated
     * item to dictionary definition, useful for getting
     * configuration information such as maximum price.
     * 
     * @param item  The item to look up.
     * @return      The item definition.
     */
    public Item lookup(ItemStack item)
    {        
        Item sibling = aliases.get(item.getType().toString());
        if(sibling == null)
        {
            PLUGIN.CM.severe("Error resolving item: type not found: \"" + item.getType().toString() + "\".");
            return null;
        }
        if(item.getDurability() == sibling.DMG)
            return sibling;
        
        Item self = lookup(sibling.ID,item.getDurability());
        if(self == null)
        {
            PLUGIN.CM.severe("Error resolving item: item not found: " + sibling.ID + ":" + item.getDurability() + ".");
            return null;
        }
        return self;
    }
    
    /**
     * Resolve a (validated) item to its configured display name.
     * 
     * @param item  The item to resolve to displayname.
     * @return      The display name of the specified item.
     */
    public String getDisplayName(ItemStack item)
    {
        Item itm = lookup(item);
        if(itm == null)
            return "~INVALID";
        return itm.DISPLAY;
    }

    /**
     * Look up a valid Minecraft item using the material name
     * and the damage value.
     * 
     * @param material  The (validated) material name.
     * @param dmg       The damage value.
     * @return          An Item, or null on fail.
     */
    public Item lookup(String material, int dmg)
    {
        Item sibling = aliases.get(material);
        if(sibling == null)
            return null;
        Item self = lookup(sibling.ID,dmg);
        return self;
    }
    
    /**
     * Look up a valid Minecraft item using ID:DMG
     * or an alias or the official Minecraft name.
     * Using the official minecraft name will default
     * to DMG=0.
     * 
     * @param query     The string by which to look up an item.
     * @return          A valid Minecraft item reference or null.
     */
    public Item lookup(String query)
    {        
        int id = 0, dmg = 0;
        
        //ID:DMG
        if(query.split(":").length == 2)
            try
            {
                id = Integer.parseInt(query.split(":")[0]);
                dmg = Integer.parseInt(query.split(":")[1]);
                return lookup(id,dmg);
            }
            catch(NumberFormatException e)
            {
                return null;
            }
        
        //ID
        try
        {
            id = Integer.parseInt(query);
            return lookup(id,0);
        }
        catch(NumberFormatException e){}
        
        //ALIAS
        if(aliases.containsKey(query.toUpperCase()))
            return aliases.get(query.toUpperCase());
        query = (query.toUpperCase().endsWith("S") ? query.substring(0,query.length() - 1) : query + "s");
        if(aliases.containsKey(query.toUpperCase()))
            return aliases.get(query.toUpperCase());
        //NO RESULT
        return null;
    }
    
    /**
     * Check whether the specified ItemStacks are the same
     * item, regardless of amount.
     * 
     * @param a     An ItemStack
     * @param b     Another ItemStack
     * @return      Whether the two stacks are the same item
     *              regardless of amount. If either argument
     *              is null, returns false.
     */
    public static boolean areSameItem(ItemStack a, ItemStack b)
    {
        if(a == null || b == null)
            return false;
        return
                a.getType().toString().equals(b.getType().toString()) &&
                a.getDurability() == b.getDurability();
    }
     
    /**
     * Parse a (player-provided) String which seeks to represent
     * an item.
     * 
     * @param caller    The user who supplied the String.
     * @param arg       A user-provided String; can be the name of the
     *                  item (such as "stone") or an ID or an ID:DMG
     *                  value. Could also be "hand".
     * @return          A (validated) ItemStack if one was found,
     *                  -1 if the String was "hand" but no item is held,
     *                  -2 if item lookup failed,
     *                  -3 if there was an error validating to ItemStack.
     */
    public Object parse(Player caller, String arg)
    {
        ItemStack result;
        
        //If item = "hand"
        if(arg.equalsIgnoreCase("hand"))
        {
            ItemStack handItem = caller.getInventory().getItemInMainHand();
            if(handItem == null || isAir(handItem))
                return -1;
            result = new ItemStack(handItem.getType(),1,handItem.getDurability());
        }
        //If not "hand"
        else
        {
            Item thing = lookup(arg);
            if(thing == null)
                return -2;
            result = PLUGIN.IM.validate(thing);
            if(result == null)
            {
                //Something has gone wrong converting from Item to ItemStack,
                //  which indicates that the items.csv file is misconfigured.
                //Because this is a configuration problem ChatShop needs to be
                //  as verbose as possible.
                
                String debug = "Error converting Item (loaded from items.csv) " +
                    "to ItemStack (validated Minecraft item). " +
                    "Item(ID=" + thing.ID + ",DMG=" + thing.DMG +
                    ",MNAME=\"" + thing.MNAME + "\") does not represent a valid " +
                    "Minecraft item.";
                
                PLUGIN.CM.severe(debug);
                
                return -3;
            }
        }
        
        return result;
    }
    
    /**
     * Determines whether the item in question is air. 
     * 
     * @param itm   The item to query.
     * @return      Whether the item is air.
     */
    public static boolean isAir(ItemStack itm)
    {
        return itm.getType().toString().equalsIgnoreCase("air");
    }
    
    /**
     * Look up an item by its ID and DMG value.
     * 
     * @param id    Item ID
     * @param dmg   Damage value
     * @return      A valid Minecraft item reference or null.
     */
    public Item lookup(int id, int dmg)
    {
        if(items.containsKey(id) && items.get(id).containsKey(dmg))
            return items.get(id).get(dmg);
        return null;
    }
    
    /**
     * Convert an Item into a valid ItemStack with quantity 1.
     * 
     * @param itm   The item to convert.
     * @return      An ItemStack with quantity 1, or null on fail.
     */
    public ItemStack validate(Item itm)
    {
        String mname = itm.MNAME;
        Material mtl = Material.matchMaterial(mname);
        return (mtl == null ? null : new ItemStack(mtl,1,(short)itm.DMG));
    }
        
    
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
        
        /**
         * Instantiate a new Item.
         * 
         * @param id          The primary ID of the item.
         * @param damage      The data value of the item. 0 if not needed.
         * @param mname       The official Minecraft name for this item.
         * @param display     The display name for this item.
         * @param maxprice    The maximum price for this item. 0 signifies no maximum.
         * @param maxquantity The maximum quantity for this item. 0 signifies no maximum.
         */
        public Item(int id, int damage,String mname,String display,double maxprice,int maxquantity)
        {
            ID = id;
            DMG = damage;
            MNAME = mname;
            DISPLAY = display;
            MAXPRICE = maxprice;
            MAXQUANTITY = maxquantity;
        }
    }
}
