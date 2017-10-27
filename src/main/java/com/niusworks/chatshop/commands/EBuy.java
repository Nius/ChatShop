package com.niusworks.chatshop.commands;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.constructs.EBuyOrder;
import com.niusworks.chatshop.constructs.EListing;
import com.niusworks.chatshop.constructs.EnchLvl;
import com.niusworks.chatshop.constructs.Item;
import com.niusworks.chatshop.managers.ChatManager;
import com.niusworks.chatshop.managers.ItemManager;

import net.md_5.bungee.api.chat.TextComponent;

/**
 * Executor for the "ebuy" command for OC Network's ChatShop.
 * <br>
 * Players can buy enchanted items from the chatshop. Such purchases are always
 * made explicitly by lot number, so it is assumed that players already know the price.
 * The quantity is always one.
 * <br><br>
 * On execution of this command the database will be checked to see whether the user is employing buy
 * confirmations. If so, a {@link EBuyOrder} will be created and posted, which is then under the
 * jurisdiction of {@link Confirm}. If not, the buy operation will be executed immediately.
 * <br><br>
 * This command has the following limits (aside from basic perms):
 * <ul>
 * <li>Console access denied.
 * <li>World must be whitelisted in config.
 * <li>Gamemode must be whitelisted in config.
 * <li>General freeze prevents command.
 * </ul>
 * @author ObsidianCraft Staff
 */
public class EBuy implements CommandExecutor
{
    /** Command usage. **/
    public static final String USAGE = "/ebuy <lot number>";
    
    /** The specific instance of the parent ChatShop plugin. **/
    private final ChatShop PLUGIN;
    
    /**
     * Instantiate the command executor for "ebuy" commands.
     * 
     * @param master    The specific instance of the parent ChatShop plugin.
     */
    public EBuy(ChatShop master)
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
            return PLUGIN.CM.reply(sender,"ChatShop.ebuy cannot be executed as console.");
        Player usr = (Player)sender;
        //Permissions
        if(!sender.hasPermission("chatshop.ebuy"))
            return PLUGIN.CM.denyPermission(sender);
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
        //General freeze
        if(PLUGIN.DB.isGeneralFreeze())
            return PLUGIN.CM.denyGeneralFreeze(usr);
        
        //
        //  VALIDATION
        //
        
        //Number of args
        if(args.length != 1)
            return PLUGIN.CM.error(sender,USAGE);
        
        //Arg is a valid lot number
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
        
        //Price check
        double avbal = PLUGIN.ECON.getBalance(usr);
        if(avbal < listing.PRICE)
            return PLUGIN.CM.error(usr,"Insufficient funds for that item.");
        
        //
        //  EXECUTION
        //  Deferred to DatabaseManager for synchronization purposes.
        //  The database manager is responsible for notifying players
        //    who seed this purchase, so that (1) a list of listings
        //    does not need to be passed to this Buy instance and
        //    (2) such a list does not need to be traversed more than
        //    once.
        //
        
        String textCol = PLUGIN.CM.color("text");
        String cmdCol = PLUGIN.CM.color("helpUsage");
        String priceCol = PLUGIN.CM.color("price");
        
        if(PLUGIN.DB.getPlayerFlag(usr,3) != 'X')
        {
            String msg;
            //The player is using /confirm for ebuys, so instead of executing an ebuy
            //create an ebuy order and store it. The ebuy order is then under the jurisdiction
            //of /confirm.
            EBuyOrder order = new EBuyOrder(usr,lot,listing.PRICE,System.currentTimeMillis());
            PLUGIN.PENDING.put(usr,order);
            
            Item cfg = PLUGIN.IM.lookup(listing.MATERIAL);
            
            TextComponent tc0 = new TextComponent();
            tc0.setText(PLUGIN.CM.PREFIX +
                        textCol + "Preparing to buy ");
            TextComponent tc1 = PLUGIN.CM.MOTforEnchanted(
                    itemCol +
                    (Material.getMaterial(listing.MATERIAL).equals(Material.ENCHANTED_BOOK) ? "" : "enchanted ") +
                    itemCol + cfg.DISPLAY,listing.ID,listing.toItemStack());
            TextComponent tc2 = new TextComponent();
            tc2.setText(textCol + " for " +
                        priceCol + ChatManager.format(listing.PRICE) +
                        textCol + ".");
            usr.spigot().sendMessage(tc0,tc1,tc2);
            
            msg =
                textCol + "Use " +
                cmdCol + "/confirm " +
                textCol + "to confirm this order.";
            return PLUGIN.CM.reply(usr,msg);
        }
        else{} //The player is not using /confirm for buys; execute immediately.
        
        //
        //  RESULT
        //
        
        return processResults(usr,lot,listing.PRICE);
    }
    
    /**
     * Process the results of a buy action.
     * This was originally contiguous from onCommand, but was
     * separated so that {@link Confirm} can also finalize
     * buy operations.
     * 
     * @param usr           The user who is executing the buy operaion.
     * @param lot           The lot number to buy.
     * @param price         The expected price for this purchase. This is used to ensure
     *                      that the price has not changed between placing an EBuyOrder
     *                      and using /confirm.
     * @return              Always returns true, so that calling methods can finalize
     *                      the buy order and terminate in one line.
     */
    public boolean processResults(Player usr,int lot,double price)
    {
        //Final price check (to ensure that it hasn't changed before
        //  using /confirm) and player balance check is performed by
        //  DBManager.
        
        Object res = PLUGIN.DB.ebuy(usr,lot,price);
        
        //Detect failures
        if(res instanceof Integer)
            switch(((Integer)res).intValue())
            {
                case -1:    return PLUGIN.CM.error(usr,"Insufficient funds for that item.");
                case -2:    return PLUGIN.CM.error(usr,"The price for this item has changed. Purchase aborted.");
                case -3:    return PLUGIN.CM.error(usr,"This listing no longer exists.");
                case -4:    return PLUGIN.CM.err500(usr);
            }
        
        EListing listing = (EListing)res;
        
        UUID seller = UUID.fromString(listing.PLAYER_UUID);
        OfflinePlayer slr = PLUGIN.getServer().getOfflinePlayer(seller);
        
        ItemStack merchandise = listing.toItemStack();
        
        //Charge the buyer for the purchase.
        PLUGIN.ECON.withdrawPlayer(usr,listing.PRICE);
        //Credit the seller for the sale.
        PLUGIN.ECON.depositPlayer(slr,listing.PRICE);
        
        //Credit the item to the user's inventory.
        PLUGIN.IM.giveItem(usr,merchandise);
        
        //
        // CHAT MESSAGES
        //
        
        String textCol = PLUGIN.CM.color("text");
        String itemCol = PLUGIN.CM.color("item");
        String playerCol = PLUGIN.CM.color("player");
        String priceCol = PLUGIN.CM.color("price");
        
        Item cfg = PLUGIN.IM.lookup(listing.MATERIAL);
        
        //Notify the BUYER that the sale was accomplished.
        TextComponent tc0 = new TextComponent();
        tc0.setText(PLUGIN.CM.PREFIX +
                    textCol + "Bought ");
        TextComponent tc1 = PLUGIN.CM.MOTforEnchanted(
                itemCol +
                (Material.getMaterial(listing.MATERIAL).equals(Material.ENCHANTED_BOOK) ? "" : "enchanted ") +
                itemCol + cfg.DISPLAY,listing.ID,merchandise);
        TextComponent tc2 = new TextComponent();
        tc2.setText(textCol + " for " +
                    priceCol + ChatManager.format(price) +
                    textCol + ".");
        usr.spigot().sendMessage(tc0,tc1,tc2);
        
        //Notify the SELLER that the sale was accomplished.
        if(slr != null && slr.isOnline() &&
                !slr.getUniqueId().equals(usr.getUniqueId()))
        {
            tc0.setText(PLUGIN.CM.PREFIX +
                        playerCol + usr.getName() +
                        textCol + " just bought ");
            TextComponent tc3 = new TextComponent();
            tc3.setText(textCol + " from you");
            PLUGIN.getServer().getPlayer(seller).spigot().sendMessage(tc0,tc1,tc3,tc2);
        }
        
        return true;
    }
}