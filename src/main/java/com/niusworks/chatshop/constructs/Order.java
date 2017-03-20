package com.niusworks.chatshop.constructs;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.niusworks.chatshop.commands.Buy;
import com.niusworks.chatshop.commands.Confirm;
import com.niusworks.chatshop.commands.Sell;

/**
 * A simple vehicle for storing a pending buy or sell order. These orders are created by
 * {@link Buy} or {@link Sell} and are read exclusively by {@link Confirm}.
 * @author ObsidianCraft Staff
 */
public abstract class Order
{
    /** The player who created this order. **/
    public final Player PLAYER;
    /** The merchandise (including quantity) to purchse. **/
    public final ItemStack MERCH;
    /** The already-looked-up configuration for this item. **/
    public final Item CONFIG;
    /** The time at which this order was created. **/
    public final long TIME;
    
    /**
     * @param usr       The player who created this order.
     * @param merch     The merchandise (including quantity) to purchse.
     * @param cfg       The already-looked-up configuration for this item.
     * @param time      The time at which this order was created.
     */
    public Order(Player usr,ItemStack merch,Item cfg,long time)
    {
        PLAYER = usr; MERCH = merch; CONFIG = cfg; TIME = time;
    }
}