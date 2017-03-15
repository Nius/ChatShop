package com.niusworks.chatshop.constructs;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.niusworks.chatshop.managers.ItemManager.Item;

/**
 * A simple vehicle for storing a pending buy order.
 * @author ObsidianCraft Staff
 */
public class BuyOrder extends Order
{
    /** The maximum price for the buy order. **/
    public final double MAXP;
    /** The calculated total price for the order at the time of its creation. **/
    public final double TOTAL;
    
    /**
     * @param usr     The player who created this order.
     * @param merch   The merchandise (including quantity) to purchse.
     * @param cfg     The already-looked-up configuration for this item.
     * @param maxp    The maximum price for the buy order.
     * @param total   The calculated total price for the buy order at the time of its creation.
     * @param time    The time at which this order was created.
     */
    public BuyOrder(Player usr,ItemStack merch,Item cfg,double maxp,double total,long time)
    {
        super(usr,merch,cfg,time);
        MAXP = maxp; TOTAL = total;
    }
}