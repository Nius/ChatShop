package com.niusworks.chatshop.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.niusworks.chatshop.ChatShop;
import com.niusworks.chatshop.constructs.ESellOrder;
import com.niusworks.chatshop.constructs.Item;
import com.niusworks.chatshop.managers.ChatManager;
import com.niusworks.chatshop.managers.ItemManager;

import net.md_5.bungee.api.chat.TextComponent;

/**
 * Executor for the "esell" command for OC Network's ChatShop.
 * <br>
 * This command will also be called if a player uses "/sell &lt;price&gt;" AND the price is a valid
 * number AND the item in their main hand is enchanted. This is for convenience; players will likely
 * often forget the "e" in front of "sell".1
 * Sell and ESell are separated for clarity of structure/function/code/sanity.
 * <br>
 * Users can post enchanted items to the ChatShop marketplace. This command requires only one
 * argument specifying the price of the item to post.
 * <br><br>
 * Price is a double value indicating the price to assign the listing. The plugin config file
 * defines whether price limits set forth in items.csv are honored here; attempting to price an
 * enchanted item above the configured maximum price of a non-enchanted item of the same type
 * may be prevented.
 * <br><br>
 * Upon successful posting a broadcast is sent out notifying players of the newly created listing.
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
public class ESell implements CommandExecutor
{
    /** Command usage. **/
    public static final String USAGE = "/esell <price>";
    
    /** The specific instance of the parent ChatShop plugin. **/
    private final ChatShop PLUGIN;
    
    /**
     * Instantiate the command executor for "esell" commands.
     * 
     * @param master    The specific instance of the parent ChatShop plugin.
     */
    public ESell(ChatShop master)
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
            return PLUGIN.CM.reply(sender,"ChatShop.esell cannot be executed as console.");
        Player usr = (Player)sender;
        //Permissions
        if(!sender.hasPermission("chatshop.esell"))
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
        
        //Check that the given item is enchanted.
        ItemStack handItem = usr.getInventory().getItemInMainHand().clone();
        if(handItem == null | ItemManager.isAir(handItem))
            return PLUGIN.CM.error(usr,"You are not holding an item.");
        if(handItem.getEnchantments().size() == 0 &&
            !handItem.getType().equals(Material.ENCHANTED_BOOK))
            return PLUGIN.CM.error(usr,
                "This item is not enchanted. Please use " +
                PLUGIN.CM.color("helpUsage") + "/sell" +
                PLUGIN.CM.color("error") + ".");
        
        //At this point we are guaranteed that the item is enchanted.
        
        //Check whether the item is damaged, if it matters.
        if(!PLUGIN.getConfig().getBoolean("allow-damaged-enchanted",false))
            if(handItem.getDurability() != 0)
                return PLUGIN.CM.error(usr,"Damaged items cannot be sold on the ChatShop.");
        
        //Look up the config definition for this item sans-ENCHANTS.
        //ESell allows damaged items but ItemManager#lookup will return
        //null, so we look up using a zero-damage copy of the item.
        ItemStack unenchanted = handItem.clone();
        unenchanted.setDurability((short)0);
        Item cfg = PLUGIN.IM.lookup(unenchanted);
        
        //Verify that the item is not banned.
        //This function is normally performed by ItemManager#parse but
        //  ESell does not use that method, so must do this manually.
        if(cfg.ISBANNED)
            return PLUGIN.CM.error(usr,"That item cannot be sold on the ChatShop.");
        
        //Price check.
        //User entry of "-" is not valid for this command.
        double price = 0;
        try
        {
            price = Double.parseDouble(args[0]);
            if(price < .01)
                return PLUGIN.CM.error(sender,
                    "No item may be priced lower than " +
                    PLUGIN.CM.color("price") + "$0.01" +
                    PLUGIN.CM.color("error") + ".");
            double globalmax = PLUGIN.getConfig().getDouble("global-max-price");
            if(price > globalmax)
                return PLUGIN.CM.error(sender,
                    "No item may be priced higher than " +
                    PLUGIN.CM.color("price") + ChatManager.format(globalmax) +
                    PLUGIN.CM.color("error") + ".");
            if(PLUGIN.getConfig().getBoolean("allow-enchanted-overprice",false)
               && cfg.MAXPRICE > 0 && price > cfg.MAXPRICE)
                return PLUGIN.CM.error(sender,
                    "The maximum allowed price for " +
                    PLUGIN.CM.color("item") + cfg.DISPLAY +
                    PLUGIN.CM.color("error") + " is " + 
                    PLUGIN.CM.color("price") + ChatManager.format(cfg.MAXPRICE) +
                    PLUGIN.CM.color("error") + ".");
        } catch (NumberFormatException e)
        {
            if(args[0].equals("-"))
                return PLUGIN.CM.error(sender,"You must always specify a price for enchanted items.");
            else
                return PLUGIN.CM.error(sender,USAGE);
        }
        
        //
        //  EXECUTION
        //  Deferred to DatabaseManager for synchronization purposes.
        //
        
        if(PLUGIN.DB.getPlayerFlag(usr,2) != 'X')
        {
            //The player is using /confirm for esells.
            ESellOrder order = new ESellOrder(usr,handItem,cfg,price,System.currentTimeMillis());
            PLUGIN.PENDING.put(usr,order);
            String textCol = PLUGIN.CM.color("text");
            String msg =
                textCol + "Preparing to sell your " +
                PLUGIN.CM.color("item") + "enchanted " + cfg.DISPLAY + " " +
                textCol + "for " +
                PLUGIN.CM.color("price") + ChatManager.format(price) +
                textCol + ".\n" + PLUGIN.CM.PREFIX +
                textCol + "Use " +
                PLUGIN.CM.color("helpUsage") + "/confirm " +
                textCol + "to confirm this order.";
            return PLUGIN.CM.reply(usr,msg);
        }
        else{} //The player is not using /confirm for esells.
            
        //
        //  RESULT
        //
        
        return processResults(usr,handItem,cfg,price);
    }
    
    /**
     * Process the results of an esell action.
     * This is effectively contiguous from {@link ESell#onCommand}
     * but is separated to accomodate completion of execution by {@link Confirm}.
     * 
     * @param usr           The user who is executing the buy operaion.
     * @param merchandise   The merchandise (including amount) the user tried to buy.
     * @param cfg           The item, as configured from file.
     * @param price         The user-provided price for this item.
     * @return              Always returns true, so that calling methods can finalize
     *                      the buy order and terminate in one line.
     */
    public boolean processResults(Player usr,ItemStack merchandise,Item cfg, double price)
    {
        //Verify that the player still has the specified item (necessary for /confirm).
        if(!PLUGIN.IM.hasItem(usr,merchandise))
            return PLUGIN.CM.error(usr,"You no longer have the item you are trying to sell.");
        
        int res = PLUGIN.DB.esell(usr,merchandise,price);
        
        //On SQL fail...
        if(res == -2)
            return PLUGIN.CM.err500(usr);
        
        //Remove the specified items from the player's inventory.
        ItemStack[] inv = usr.getInventory().getContents();
        for(int i = 0; i < inv.length; i ++)
            if(PLUGIN.IM.areSameItem(inv[i],merchandise))
            {
                usr.getInventory().clear(i);
                break;
            }
        
        String textCol = PLUGIN.CM.color("text");
        String itemCol = PLUGIN.CM.color("item");
        String playerCol = PLUGIN.CM.color("player");
        String priceCol = PLUGIN.CM.color("price");
        
        // Inform the user that their item has posted successfully.
        // It's necessary to provide more than just the broadcast so that
        //  the user is explicitly told his lot number.
        PLUGIN.CM.reply(usr,
            textCol + "Your item has been posted as lot " +
            itemCol + "#" + res +
            textCol + ".");
        
        // Construct a broadcast message.
        
        if(!PLUGIN.getConfig().getBoolean("chat.broadcast-offers"))
            return true;
        
        //Build the chat message.
        TextComponent tc0 = new TextComponent();
        tc0.setText(PLUGIN.CM.PREFIX +
                    playerCol + usr.getName() +
                    textCol + " is selling ");
        TextComponent tc1 = PLUGIN.CM.MOTforEnchanted(
            itemCol +
            (merchandise.getType().equals(Material.ENCHANTED_BOOK) ? "" : "enchanted ") +
            cfg.DISPLAY,res,merchandise);
        TextComponent tc2 = new TextComponent();
        tc2.setText(textCol + " for " +
                    priceCol + ChatManager.format(price) +
                    textCol + ".");
        
        PLUGIN.getServer().spigot().broadcast(tc0,tc1,tc2);
        
        return true;
    }
}
