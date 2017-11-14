package com.niusworks.chatshop.managers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.commands.EFind;
import com.niusworks.chatshop.constructs.EnchLvl;
import com.niusworks.chatshop.constructs.Item;

/**
 * Manages all Minecraft item handling functionality for OC Network's ChatShop.
 * <br>
 * This class, and its {@link Item} children, are the bridge between old ID:DMG
 * and MATERIAL:DMG rules. This class also serves as an adapter for Potions and
 * Tipped Arrows, which use a totally different system for differentiating
 * themselves.
 * <br>
 * Upon initialization ItemManager automatically reads items.csv and parses its contents
 * into configured {@link Item}s ready to be used by the rest of the plugin.
 * <br>
 * ItemManger's job is to make design of all other classes easier by providing these services:
 * <ul>
 * <li>A string representation (usually a user argument) of an item can be converted into
 *     a usable Item or ItemStack as needed in one line, or else responded to with meaningful
 *     feedback.
 * <li>Any item can be checked in one line to see if it's damaged, enchanted or invalid.
 * <li>Any other class can ask ItemManager to "wash" an item, checking it against special
 *     rules for potions (or other special items as added in the future), and subsequently
 *     totally ignore the fact that these items work completely differently. This is mainly
 *     because {@link #areSameItem} and {@link #giveItem} are sensitive to these special rules
 *     and automatically make necessary adjustments.
 * </ul>
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
     * A map of potion types (as defined by {@link org.bukkit.potion.PotionType}) to
     * (arbitrary) integer values, for the purpose of superimposing a MATERIAL:DMG system
     * atop the potion architecture.
     */
    protected final HashMap<String,Integer> POTIONS = new HashMap<String,Integer>();
    
    /**
     * A two-dimensional hash map of items; dimension 1 being item ID and dimension 2 being damage value.
     * {@link #aliases} maintains a list of all aliases for each item, including the Bukkit material name,
     * and is the primary means of item lookups considering that most users will search by string rather
     * than by ID.
     * This list serves to make lookups by ID:DMG much more efficient by removing the need to search through
     * all named items and compare their attributes.
     */
    protected HashMap<Integer,HashMap<Integer,Item>> items = new HashMap<Integer,HashMap<Integer,Item>>();
    
    /** A map of presentable names for enchantments to their Bukkit names. **/
    protected final HashMap<Enchantment,String> ENCHANTS = new HashMap<Enchantment,String>();
    
    /** The master plugin for this manager. **/
    protected final ChatShop PLUGIN;
    
    /** The folder in which to look for items.csv **/
    protected final File DATA_FOLDER;
    
    /**
     * Create an ItemManager with a reference to the master
     * plugin, and define the potions map.
     * 
     * @param master     The master ChatShop plugin reference.
     * @param dataFolder The folder in which to look for items.csv
     */
    public ItemManager(ChatShop master,File dataFolder)
    {
        PLUGIN = master;
        DATA_FOLDER = dataFolder;
        
        /* This is an arbitrary assignment of numbers to potion types, but it must
         * be matched by items.csv.
         * This is to facilitate superimposition of a MATERIAL:DMG system atop the
         * potion architecture, which is NBT-driven and currently incompatible with
         * spigot.
         */
        POTIONS.put("AWKWARD",0);
        POTIONS.put("FIRE_RESISTANCE",1);
        POTIONS.put("INSTANT_DAMAGE",2);
        POTIONS.put("INSTANT_HEAL",3);
        POTIONS.put("INVISIBILITY",4);
        POTIONS.put("JUMP",5);
        POTIONS.put("LUCK",6);
        POTIONS.put("MUNDANE",7);
        POTIONS.put("NIGHT_VISION",8); 
        POTIONS.put("POISON",9);
        POTIONS.put("REGEN",10);
        POTIONS.put("SLOWNESS",11);
        POTIONS.put("SPEED",12);
        POTIONS.put("STRENGTH",13); 
        POTIONS.put("THICK",14);
        POTIONS.put("UNCRAFTABLE",15); 
        POTIONS.put("WATER",16);
        POTIONS.put("WATER_BREATHING",17); 
        POTIONS.put("WEAKNESS",18);
        
        /* A map of all enchantment types and their usable, presentable names.
         * Note that this is different and independent from the map maintained by
         * DatabaseManager, and that this is not referenced by any other part of
         * the plugin.
         */
        ENCHANTS.put(Enchantment.ARROW_DAMAGE,"Power");
        ENCHANTS.put(Enchantment.ARROW_FIRE,"Flame");
        ENCHANTS.put(Enchantment.ARROW_INFINITE,"Infinity");
        ENCHANTS.put(Enchantment.ARROW_KNOCKBACK,"Punch");
        ENCHANTS.put(Enchantment.BINDING_CURSE,"Curse of Binding");
        ENCHANTS.put(Enchantment.DAMAGE_ALL,"Sharpness");
        ENCHANTS.put(Enchantment.DAMAGE_ARTHROPODS,"Bane of Arthropods");
        ENCHANTS.put(Enchantment.DAMAGE_UNDEAD,"Smite");
        ENCHANTS.put(Enchantment.DEPTH_STRIDER,"Depth Strider");
        ENCHANTS.put(Enchantment.DIG_SPEED,"Efficiency");
        ENCHANTS.put(Enchantment.DURABILITY,"Unbreaking");
        ENCHANTS.put(Enchantment.FIRE_ASPECT,"Fire Aspect");
        ENCHANTS.put(Enchantment.FROST_WALKER,"Frost Walker");
        ENCHANTS.put(Enchantment.KNOCKBACK,"Knockback");
        ENCHANTS.put(Enchantment.LOOT_BONUS_BLOCKS,"Fortune");
        ENCHANTS.put(Enchantment.LOOT_BONUS_MOBS,"Looting");
        ENCHANTS.put(Enchantment.LUCK,"Luck");
        ENCHANTS.put(Enchantment.LURE,"Lure");
        ENCHANTS.put(Enchantment.MENDING,"Mending");
        ENCHANTS.put(Enchantment.OXYGEN,"Respiration");
        ENCHANTS.put(Enchantment.PROTECTION_ENVIRONMENTAL,"Protection");
        ENCHANTS.put(Enchantment.PROTECTION_EXPLOSIONS,"Blast Protection");
        ENCHANTS.put(Enchantment.PROTECTION_FALL,"Feather Falling");
        ENCHANTS.put(Enchantment.PROTECTION_FIRE,"Fire Protection");
        ENCHANTS.put(Enchantment.PROTECTION_PROJECTILE,"Projectile Protection");
        ENCHANTS.put(Enchantment.SILK_TOUCH,"Silk Touch");
        ENCHANTS.put(Enchantment.SWEEPING_EDGE,"Sweeping Edge");
        ENCHANTS.put(Enchantment.THORNS,"Thorns");
        ENCHANTS.put(Enchantment.VANISHING_CURSE,"Curse of Vanishing");
        ENCHANTS.put(Enchantment.WATER_WORKER,"Aqua Affinity");
    }
    
    /**
     * Load and parse the items CSV.
     * 
     * @return              -1 on success, -2 on I/O fail, -3 on failure to
     *                      spawn a new file if it was missing, else the line
     *                      number where a parse fail occurred.
     */
    public synchronized int loadItems()
    {
        HashMap<String,Item> newAliases = new HashMap<String,Item>();
        HashMap<Integer,HashMap<Integer,Item>> newItems = new HashMap<Integer,HashMap<Integer,Item>>();
        
        int totalLoaded = 0;
        int totalCommented = 0;
        int totalBanned = 0;
        
        try
        {
            //Create the items.csv if it does not exist.
            File itemFile = new File(DATA_FOLDER,"items.csv");
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
                    
                    //Determine whether this item is banned or technical.
                    //Banned items are loaded so that ChatShop can gracefully refuse them.
                    //Technical items are not loaded at all.
                    boolean isban = false;
                    boolean isTechnical = false;
                    String[] flags = line.split("!");
                    for(String flag : flags)
                        if(flag.trim().equalsIgnoreCase("BAN"))
                        {
                            isban = true;
                            break;
                        }
                        else if(flag.trim().equalsIgnoreCase("TECHNICAL"))
                        {
                            isTechnical = true;
                            break;
                        }
                    if(isTechnical)
                        continue;
                    if(isban)
                        totalBanned ++;
                    
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
                    
                    //Read bukkit official name
                    //This is the name compliant with org.bukkit.Material
                    String mname = tokens[1];
                    
                    //Read all aliases and store them for later registration.
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
                    String display = tokens[2].trim();//.replaceAll("\\s","");
                    
                    //Store the new item in the items 2D-hash
                    //In case of duplicate item ID:DMGs, the first entry will prevail.
                    Item itm = new Item(id, dam, mname.trim().toUpperCase(),(display.length() > 0 ? display.trim() : alii[0].trim()),maxPrice,maxQuantity,isban);
                    if(!newItems.containsKey(id))                      //If this ID is undefined...
                        newItems.put(id,new HashMap<Integer,Item>());  //...create a new map for it.
                    if(!newItems.get(id).containsKey(dam))             //If this exact item is undefined...
                        newItems.get(id).put(dam,itm);                 //...store it.
                    
                    //Store all aliases.
                    //Aliases with spaces will be stored twice:
                    //  once with spaces removed,
                    //  once with spaces replaced with underscores.
                    //In case of duplicates, the latest entry will prevail.
                    for(String alias : alii)
                    {
                        newAliases.put(alias.trim().toUpperCase().replaceAll("\\s",""),itm);
                        newAliases.put(alias.trim().toUpperCase().replaceAll("\\s","_"),itm);
                    }
                    //Store the official Bukkit name as an alias only for items with damage 0.
                    //(except for potions, which will store :111 (basic Fire Resistance potion).
                    //Official names with underscores (most of them) will be stored twice:
                    //  once as-is,
                    //  once with underscores removed.
                    //In case of duplicates, the latest entry will prevail.
                    if( dam == 0 ||
                        (
                            (itm.MNAME.equals("POTION") ||
                             itm.MNAME.equals("LINGERING_POTION") ||
                             itm.MNAME.equals("SPLASH_POTION") ||
                             itm.MNAME.equals("TIPPED_ARROW"))
                            &&
                            dam == 111
                         )
                       )
                    {
                        newAliases.put(mname.trim().toUpperCase(),itm);
                        newAliases.put(mname.trim().toUpperCase().replaceAll("_",""),itm);
                    }
                    
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
        
        //Overwrite existing item information with the newly loaded information.
        aliases = newAliases;
        items = newItems;
        
        //This output is for helping administrators debug changes to their items.csv,
        //in case it's necessary.
        PLUGIN.CM.log("Loaded " + totalLoaded + " items. (" + totalBanned
            + " banned items and " + totalCommented + " comments, totaling "
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
        int targetAmount = gift.getAmount();
        
        gift = getPotionFromSuperimposed(gift);
        
        PlayerInventory inv = usr.getInventory();
        int given = 0;
        //Query the maximum stack size of this item type, but protect against
        // a -1 response from the API, whose documentation did not engender
        // confidence in its own robustness.
        int stackSize = Math.max(gift.getMaxStackSize(),1);
        
        //Try to put the items in the player's (main) hand first.
        ItemStack handItem = inv.getItemInMainHand();
        if(handItem == null || isAir(handItem) || areSameType(handItem,gift))
        {
            //Put no more than was specified or the maximum stack size of
            // this item.
            int toPut = Math.min(targetAmount,stackSize);
            
            //Put no more than can fit on top the amount that was already
            // in hand.
            if(!isAir(handItem))
                toPut = Math.min(toPut,stackSize - handItem.getAmount());
            
            //If there is still room in hand (after checking stack size limits)
            // then put some stuff.
            if(toPut != 0)
            {
                given = toPut;
                ItemStack newItm = new ItemStack(gift);
                newItm.setAmount(isAir(handItem) ? toPut : handItem.getAmount() + toPut);
                inv.setItemInMainHand(newItm);
            }
        }
        
        int space = 0;
        //Determine how much space is in the player's inventory for this item.
        //Bukkit does not provide a way to determine how much was deposited after
        //a call to Inventory.addItem(), and the HashMap returned by that method
        //is proven unreliable.
        for(int i = 0; i <= 35; i ++)   //Inventory slots 0-35 are main inventory
        {
            ItemStack slot = inv.getItem(i);
            if(slot == null || isAir(slot))
                space += stackSize;
            else if(PLUGIN.IM.areSameType(gift,slot))
                space += stackSize - slot.getAmount();
        }
        
        //Give the player as much as there is space for in inventory
        ItemStack toGive = new ItemStack(gift);
        int toGiveQty = Math.min(space,targetAmount - given);
        toGive.setAmount(toGiveQty);
        inv.addItem(toGive);
        given += toGiveQty;
        
        //Drop remaining items on the ground.
        boolean didDrop = false;
        int toDropQty = targetAmount - given;
        if(toDropQty > 0)
        {
            didDrop = true;

            //Bukkit does not play nice with ItemStacks whose amounts are
            //greater than their maximum stack size. ChatShop must manually
            //drop stacks of size stackSize repeatedly until the appropriate
            //amount is dropped.
            ItemStack toDrop = new ItemStack(gift);
            toDrop.setAmount(stackSize);
            
            for(int i = 0; i + stackSize <= toDropQty; i += stackSize)
                usr.getWorld().dropItem(usr.getLocation(),toDrop);
            
            int remainingQty = toDropQty % stackSize;
            toDrop.setAmount(remainingQty);
            usr.getWorld().dropItem(usr.getLocation(),toDrop);
            
            String msg =
                PLUGIN.CM.color("text") + "Insufficient space in your inventory: there are now " +
                PLUGIN.CM.color("quantity") + ChatManager.format(toDropQty) + " " +
                PLUGIN.CM.color("item") + getDisplayName(toDrop) + " " +
                PLUGIN.CM.color("error") + "on the ground " +
                PLUGIN.CM.color("text") + "below you.";
          
            PLUGIN.CM.reply(usr,msg);
        }
        
        return didDrop;
    }
          
    /**
     * Look up a valid Minecraft item configuration by ItemStack.
     * This is essentially a reverse-lookup from validated
     * item to dictionary definition, useful for getting
     * configuration information such as maximum price.
     * 
     * @param item  The item to look up.
     * @return      The item definition, or null if no definition was found.
     */
    public Item lookup(ItemStack item)
    {        
        Item sibling = aliases.get(item.getType().toString());
        if(sibling == null)
        {
//            PLUGIN.CM.severe("Error resolving item: type not found: \"" + item.getType().toString() + "\".");
            return null;
        }
        if(item.getDurability() == sibling.DMG)
            return sibling;
        
        Item self = lookup(sibling.ID,item.getDurability());
        if(self == null)
        {
//            PLUGIN.CM.severe("Error resolving item: item not found: " + sibling.ID + ":" + item.getDurability() + ".");
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
        Object res = makeCompliant(item,true);
        if(!(res instanceof ItemStack))
            return "~INVALID";
        Item itm = lookup((ItemStack)res);
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
     * type of item, regardless of amount.
     * If either item is enchanted, returns false.
     * 
     * @param a     An ItemStack
     * @param b     Another ItemStack
     * @return      Whether the two stacks are the same item
     *              regardless of amount. If either argument
     *              is null, returns false.
     */
    public boolean areSameType(ItemStack a, ItemStack b)
    {
        if(a == null || b == null)
            return false;
        if(a.getEnchantments().size() > 0 || b.getEnchantments().size() > 0)
            return false;
        return
                a.getType().toString().equals(b.getType().toString()) &&
                superimposePotionDamage(a).getDurability() == superimposePotionDamage(b).getDurability();
    }
    
    /**
     * Check whether the specified ItemStacks are exactly the same
     * item, including enchantments.
     * 
     * @param a     An ItemStack
     * @param b     Another ItemStack
     * @return      Whether the two stacks are the same item
     *              regardless of amount. If either argument
     *              is null, returns false.
     */
    public boolean areSameItem(ItemStack a, ItemStack b)
    {
        if(a == null || b == null)
            return false;
        if(!(
            a.getType().toString().equals(b.getType().toString()) &&
            superimposePotionDamage(a).getDurability() == superimposePotionDamage(b).getDurability()
            ))
            return false;
        if(
            a.getEnchantments().size() > 0 ||
            b.getEnchantments().size() > 0 ||
            a.getType().equals(Material.ENCHANTED_BOOK))
            //If a is enchanted book then b must be too, via the second conditional of this method.
            //Enchanted books have getEnchantments().size() = 0.
        {
            Set<Map.Entry<Enchantment,Integer>> entrySetA =
                    (a.getType().equals(Material.ENCHANTED_BOOK) ?
                        ((EnchantmentStorageMeta)a.getItemMeta()).getStoredEnchants().entrySet() :
                        a.getEnchantments().entrySet());
            Map<Enchantment,Integer> entrySetB =
                    (b.getType().equals(Material.ENCHANTED_BOOK) ?
                        ((EnchantmentStorageMeta)b.getItemMeta()).getStoredEnchants() :
                        b.getEnchantments());
            for(Map.Entry<Enchantment,Integer> entry : entrySetA)
                if(!(
                    entrySetB.containsKey(entry.getKey()) &&
                    entrySetB.get(entry.getKey()).equals(entry.getValue())))
                    return false;
        }
        return true;
    }
    
    /**
     * Determine whether the specified user has any of the specified item,
     * regardless of quantity, sensitive to enchantments.
     * 
     * @param usr   The player whose inventory to check.
     * @param itm   The item to be sought.
     * @return      Whether the item exists in the player's inventory.
     */
    public boolean hasItem(Player usr, ItemStack itm)
    {
        for(ItemStack has : usr.getInventory())
            if(areSameItem(has,itm))
                return true;
        return false;
    }
     
    /**
     * Parse a (player-provided) String which seeks to represent
     * an item.
     * 
     * @param caller    The user who supplied the String.
     * @param arg       A user-provided String; can be the name of the
     *                  item (such as "stone") or an ID or an ID:DMG
     *                  value. Could also be "hand".
     * @return          A (validated) ItemStack if one was found (see {@link #makeCompliant(ItemStack, boolean)}),
     *                  <br>-1 if the String was "hand" but no item is held,
     *                  <br>-2 if item lookup failed,
     *                  <br>-3 if there was an error validating to ItemStack,
     *                  <br>-4 if the item is "hand" and is enchanted,
     *                  <br>-5 if the item is "hand" and is recognized but damaged.
     *                  <br>-6 if the item is recognized but banned.
     */
    public Object parse(Player caller, String arg)
    {
        return parse(caller,arg,false);
    }
    
    /**
     * Parse a (player-provided) String which seeks to represent
     * an item.
     * 
     * @param caller    The user who supplied the String.
     * @param arg       A user-provided String; can be the name of the
     *                  item (such as "stone") or an ID or an ID:DMG
     *                  value. Could also be "hand".
     * @param ignoreEnchDmg Whether to ignore enchantments and damage.
     *                      This should be used only by {@link EFind}.
     * @return          A (validated) ItemStack if one was found (see {@link #makeCompliant(ItemStack, boolean)}),
     *                  <br>-1 if the String was "hand" but no item is held,
     *                  <br>-2 if item lookup failed,
     *                  <br>-3 if there was an error validating to ItemStack,
     *                  <br>-4 if the item is enchanted,
     *                  <br>-5 if the item is recognized but damaged.
     *                  <br>-6 if the item is recognized but banned.
     */
    public Object parse(Player caller, String arg, boolean ignoreEnchDmg)
    {
        ItemStack result;
        
        //If item = "hand"
        if(arg.equalsIgnoreCase("hand"))
        {
            ItemStack handItem = caller.getInventory().getItemInMainHand().clone();
            if(handItem == null || isAir(handItem))
                return -1;
            
            //Check whether this item is damaged (is currently invalid but is valid at dmg 0)
            if(lookup(handItem) == null)
            {
                ItemStack copy = handItem.clone();
                copy.setDurability((short)0);
                if(lookup(copy) != null && !ignoreEnchDmg)
                    return -5;
                else
                    result = copy;
            }
            else
                result = handItem;
        }
        //If not "hand"
        else
        {
            Item thing = lookup(arg);
            if(thing == null)
                return -2;
            result = validate(thing);
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
        return makeCompliant(result,ignoreEnchDmg);
    }
    
    /**
     * Ensures that the provided ItemStack is compliant to ChatShop rules:
     * <ul>
     *  <li>If the item is a potion, rebuilds the item as a ChatShop-recognized potion.
     *  <li>If the item is banned, refuses it.
     *  <li>If the item is enchanted, refuses it unless instructed not to.
     * </ul>
     * The ban check occurs before the enchantment check.
     * 
     * @param itm   The ItemStack to verify.
     * @param ignoreEnchant Whether to ignore enchantments.
     * @return      A (validated) ItemStack which is ready to use. If the item was
     *              not a potion then the returned ItemStack is literally <code>itm</code>.
     *              <br>-4 if the item is enchanted.
     *              <br>-6 if the item is banned.
     */
    public Object makeCompliant(ItemStack itm,boolean ignoreEnchant)
    {
        ItemStack ret = superimposePotionDamage(itm);
        Item cfg = lookup(ret);
        if(cfg != null && cfg.ISBANNED)
            return -6;
        else if(itm.getEnchantments().size() > 0 && !ignoreEnchant)
            return -4;
        else
            return ret;
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
     * Convert an ItemStack which represents a Minecraft potion or tipped arrow to a
     * pseudo-ItemStack whose damage value fits the superimposed potion damage system.
     * 
     * Because potions do not use damage values, in order to fit them to the ChatShop
     * Material:DMG system damage values are superimposed upon them with the following
     * pattern:
     *
     * Digit 1 = is upgraded (II); yes=2 no=1
     * Digit 2 = is extended (long); yes=2 no=1
     * Remaining digits: an integer defined (arbitrarily) by ItemManager representing the
     * basic potion type (for example, LUCK or REGEN). These values are mapped in
     * {@link #POTIONS}.
     * 
     * @param itm   The ItemStack to convert to a potion.
     * @return      If the provided ItemStack is a potion, returns the same ItemStack but
     *              with the Durability value set to the superimposed damage value.
     *              If the provided ItemStack is not a potion, returns the same ItemStack
     *              unmodified.
     */
    protected ItemStack superimposePotionDamage(ItemStack itm)
    {
        if(itm.getDurability() != 0)
            return itm;
        if( itm.getType() == Material.POTION ||
            itm.getType() == Material.SPLASH_POTION ||
            itm.getType() == Material.LINGERING_POTION ||
            itm.getType() == Material.TIPPED_ARROW)
             {
                 PotionData pd = ((PotionMeta) itm.getItemMeta()).getBasePotionData();
                 int upgraded = (pd.isUpgraded() ? 2 : 1);
                 int extended = (pd.isExtended() ? 2 : 1);
                 int id = POTIONS.get(pd.getType().toString());
                 
                 short dmg = (short) Integer.parseInt("" + upgraded + extended + id);
                 
                 ItemStack ret = new ItemStack(itm.getType(),itm.getAmount(),dmg);
                 return ret;
             }
        return itm;
    }
    
    /**
     * Convert an ItemStack which has a superimposed Potion damage value to a valid Minecraft
     * potion or tipped arrow ItemStack complete with potion metadata.
     * 
     * Because potions do not use damage values, in order to fit them to the ChatShop
     * Material:DMG system damage values are superimposed upon them with the following
     * pattern:
     *
     * Digit 1 = is upgraded (II); yes=2 no=1
     * Digit 2 = is extended (long); yes=2 no=1
     * Remaining digits: an integer defined (arbitrarily) by ItemManager representing the
     * basic potion type (for example, LUCK or REGEN). These values are mapped in
     * {@link #POTIONS}.
     * 
     * @param itm   The ItemStack to convert to a potion.
     * @return      If the provided ItemStack is a potion, returns a new ItemStack with
     *              damage 0 but representing a valid Minecraft potion.
     *              If the provided ItemStack is not a potion, returns the same ItemStack
     *              unmodified.
     */
    protected ItemStack getPotionFromSuperimposed(ItemStack itm)
    {
        if( itm.getType() == Material.POTION ||
            itm.getType() == Material.SPLASH_POTION ||
            itm.getType() == Material.LINGERING_POTION ||
            itm.getType() == Material.TIPPED_ARROW)
             {
                 String dmg = "" + itm.getDurability();
            
                 int upgraded = Integer.parseInt(dmg.substring(0,1));
                 int extended = Integer.parseInt(dmg.substring(1,2));
                 int id = Integer.parseInt(dmg.substring(2));
                 
                 ItemStack ret = new ItemStack(itm.getType(),itm.getAmount(),(short)0);
                 PotionMeta pm = ((PotionMeta)ret.getItemMeta());
                 pm.setBasePotionData(new PotionData(
                         PotionType.valueOf(getPotionType(id)),
                         extended == 2,
                         upgraded == 2));
                 ret.setItemMeta(pm);
                 
                 return ret;
             }
        return itm;
    }
    
    /**
     * Return the key (type string) for the given integer value
     * in {@link #POTIONS}.
     * 
     * @param value The numerical.
     * @return      The string paired with this numerical.
     */
    protected String getPotionType(int value)
    {
        for(String key : POTIONS.keySet())
            if(POTIONS.get(key) == value)
                return key;
        return null;
    }
    
    /**
     * Resolve an enchantment to a usable name.
     * 
     * @param e The enchantment to resolve.
     * @return  A presentable name by which to refer to this enchantment type.
     */
    public String getUsableName(Enchantment e)
    {
        return ENCHANTS.get(e);
    }
    
    /**
     * Resolve a user-provided string to a valid {@link Enchantment}.
     * An enchantment whose level is not specified will be assigned level -1.
     * 
     * @param str   The string to resolve.
     * @return      An Enchantment or:
     *              -1 if the enchantment is incorrectly formatted
     *              -2 if the specified name does not match any enchantment
     *              -3 if the specified name matches more than one enchantment
     *              -4 if the specified level is impossibly high
     */
    public Object resolveEnchantment(String str)
    {
        String name; int lvl = -1;
        
        //Check format of enchantment name. Should be NAME or NAME-LVL.
        //LVL can be an int or a Roman numeral.
        
        String[] splat = str.split("-");
        if(splat.length > 2)
            return -1;
        else if(splat.length == 2)  //Formated for STRING-X
        {
            name = splat[0];
            try
            {
                lvl = Integer.parseInt(splat[1]);
            }
            catch(NumberFormatException e)
            {
                lvl = ChatManager.deRomanNumeralize(splat[1]);
                if(lvl == -1)
                    return -1;
            }
        }
        else
            name = str;
        
        //Check for enchantment name matches.
        //An individual check is made against "PROTECTION", because
        //  the entirety of the name of the enchantment "PROTECTION" is
        //  matched by several other enchants.
        if("PROTECTION".contains(name.toUpperCase()))
            return new EnchLvl(Enchantment.PROTECTION_ENVIRONMENTAL,lvl);
        
        Enchantment matched = null;
        for(Map.Entry<Enchantment,String> entry : ENCHANTS.entrySet())
        {
            if(entry.getValue().replaceAll(" ","").toUpperCase().contains(name.toUpperCase()))
            {
                if(matched == null)
                    matched = entry.getKey();
                else
                    return -3;  //Second match
            }
        }
        if(matched == null)     //No matches
            return -2;
        
        if(lvl > matched.getMaxLevel())
            return -4;
        
        return new EnchLvl(matched,lvl);
    }
}
