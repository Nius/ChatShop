package com.niusworks.chatshop.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.constructs.EListing;
import com.niusworks.chatshop.constructs.EnchLvl;
import com.niusworks.chatshop.constructs.Item;
import com.niusworks.chatshop.managers.ChatManager;
import com.niusworks.chatshop.managers.ItemManager;

import net.md_5.bungee.api.chat.TextComponent;

/**
 * Executor for the "ereprice" command for OC Network's ChatShop.
 * <br>
 * Players can change the prices of their existing enchanted items on the chatshop. This command
 * has two elements: lot number and newprice.
 * <br><br>
 * Newprice is a double value indicating the to assign the listing.
 * The plugin config file defines whether price limits set forth in items.csv are honored here;
 * attempting to price an enchanted item above the configured maximum price of a non-enchanted
 * item of the same type may be prevented.
 * <br><br>
 * Upon successful sale a broadcast is sent out notifying players of the newly updated listing.
 * <br><br>
 * This command has the following limits (aside from basic perms):
 * <ul>
 * <li>Console access denied.
 * <li>World must be whitelisted in config OR config must allow querying from anyone (see below).
 * <li>Gamemode must be whitelisted in config OR config must allow querying from anyone (see below).
 * <li>General freeze prevents command.
 * </ul>
 * This command conducts modification of items already on the market. Because players cannot use this command
 * to place new things on or withrdaw things from the market, world and gamemode controls are lifted for this
 * command. Should this be deemed inappropriate for a server's needs, administrators can configuratively
 * disable this liberty.
 * <br><br>
 * @author ObsidianCraft Staff
 */
public class EReprice implements CommandExecutor
{
    /** Command usage. **/
    public static final String USAGE = "/ereprice <lot number> <newPrice>";
    
    /** The specific instance of the parent ChatShop plugin. **/
    private final ChatShop PLUGIN;
    
    /**
     * Instantiate the command executor for "ereprice" commands.
     * 
     * @param master    The specific instance of the parent ChatShop plugin.
     */
    public EReprice(ChatShop master)
    {
        PLUGIN = master;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String lbl, String[] args)
    {
        //
        // Denial of service conditions
        //
        
        //No console
        if(!(sender instanceof Player))
            return PLUGIN.CM.reply(sender,"ChatShop.ereprice cannot be executed as console.");
        Player usr = (Player)sender;
        //Permissions
        if(!sender.hasPermission("chatshop.ereprice"))
            return PLUGIN.CM.denyPermission(sender);
        if(!PLUGIN.getConfig().getBoolean("query-anyone"))
        {
            //Gamemode
            Object[] modes = PLUGIN.getConfig().getList("allowed-modes").toArray();
            boolean allowed = false;
            for(int i = 0; i < modes.length; i ++)
                if(modes[i] instanceof String)
                    if(((String)modes[i]).equalsIgnoreCase(usr.getGameMode().toString()))
                        allowed = true;
            if(!allowed)
                return PLUGIN.CM.denyGameMode(sender);
            //World
            allowed = false;
            Object[] worlds = PLUGIN.getConfig().getList("allowed-worlds").toArray();
            for(int i = 0; i < worlds.length; i ++)
                if(worlds[i] instanceof String)
                    if(((String)worlds[i]).equalsIgnoreCase(usr.getWorld().getName()))
                        allowed = true;
            if(!allowed)
                return PLUGIN.CM.denyWorld(sender);
        }
        //General freeze
        if(PLUGIN.DB.isGeneralFreeze())
            return PLUGIN.CM.denyGeneralFreeze(usr);
        
        //
        //  VALIDATION
        //
        
        //Number of args
        if(args.length != 2)
            return PLUGIN.CM.error(sender,USAGE);
        
        //First arg is a valid lot number
        String itemCol = PLUGIN.CM.color("item");
        EListing listing = null;
        int lot;
        try
        {
            lot = Integer.parseInt(args[0]);
        }
        catch(NumberFormatException e)
        {
            return PLUGIN.CM.error(usr,USAGE);
        }
        listing = PLUGIN.DB.getEListing(lot);
        if(listing == null)
            return PLUGIN.CM.error(usr,"Invalid lot number " + itemCol + lot);
        
        //Lot number belongs to user
        if(!listing.PLAYER_UUID.equals(usr.getUniqueId().toString()))
            return PLUGIN.CM.error(usr,"That item does not belong to you.");
        
        //Look up the config definition for the item sans-ENCHANTS.
        //ESell allows damaged items but ItemManager#lookup will return
        //null, so we look up using a zero-damage copy of the item.
        ItemStack unenchanted = new ItemStack(Material.getMaterial(listing.MATERIAL));
        Item cfg = PLUGIN.IM.lookup(unenchanted);
        
        //Price check
        double price = 0;
        try
        {
            price = Double.parseDouble(args[1]);
            if(price < .01)
                return PLUGIN.CM.error(sender,"Minimum price is $0.01.");
            double globalmax = PLUGIN.getConfig().getDouble("global-max-price");
            if(price > globalmax)
                return PLUGIN.CM.error(sender,
                    "No item may be priced higher than " +
                    PLUGIN.CM.color("price") + ChatManager.format(globalmax) +
                    PLUGIN.CM.color("error") + ".");
            if(cfg.MAXPRICE > 0 && price > cfg.MAXPRICE)
                return PLUGIN.CM.error(sender,
                        "The maximum allowed price for " +
                        PLUGIN.CM.color("item") + cfg.DISPLAY +
                        PLUGIN.CM.color("error") + " is " + 
                        PLUGIN.CM.color("price") + ChatManager.format(cfg.MAXPRICE) +
                        PLUGIN.CM.color("error") + ".");
        } catch (NumberFormatException e)
        {
            return PLUGIN.CM.error(sender,USAGE);
        }
        
        //
        //  EXECUTION
        //  Deferred to DatabaseManager for synchronization purposes.
        //
        
        Object res = PLUGIN.DB.ereprice(lot,price);
        
        //
        //  RESULT
        //
        
        //Detect failures
        if(res instanceof Integer)
            switch(((Integer)res).intValue())
            {
                case -1:    return PLUGIN.CM.error(usr,"This listing no longer exists.");
                case -2:    return PLUGIN.CM.err500(usr);
            }
        
        ItemStack merchandise = listing.toItemStack();
        
        // Construct a broadcast message.
        
        if(!PLUGIN.getConfig().getBoolean("chat.broadcast-offers"))
            return true;
        
        String textCol = PLUGIN.CM.color("text");
        String playerCol = PLUGIN.CM.color("player");
        String priceCol = PLUGIN.CM.color("price");
        
        //Build the chat message.
        TextComponent tc0 = new TextComponent();
        tc0.setText(PLUGIN.CM.PREFIX +
                    playerCol + usr.getName() +
                    textCol + " is selling ");
        TextComponent tc1 = PLUGIN.CM.MOTforEnchanted(
            itemCol +
            (merchandise.getType().equals(Material.ENCHANTED_BOOK) ? "" : "enchanted ") +
            cfg.DISPLAY,lot,merchandise);
        TextComponent tc2 = new TextComponent();
        tc2.setText(textCol + " for " +
                    priceCol + ChatManager.format(price) +
                    
                    (price == listing.PRICE ? "" : " " +
                    (price > listing.PRICE ?
                            ChatColor.RED + "(\u25B2" + ChatManager.format(price - listing.PRICE) + ")" :
                            ChatColor.GREEN + "(\u25BC" + ChatManager.format(listing.PRICE - price) + ")")) +
                                
                    textCol + ".");
        
        PLUGIN.getServer().spigot().broadcast(tc0,tc1,tc2);

        return true;
    }
}