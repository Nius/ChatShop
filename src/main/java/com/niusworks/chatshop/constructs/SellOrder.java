package com.niusworks.chatshop.constructs;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.niusworks.chatshop.commands.Confirm;
import com.niusworks.chatshop.commands.Sell;

/**
 * A simple vehicle for storing a pending sell order. These orders are created by
 * {@link Sell} and read exclusively by {@link Confirm}.
 * @author ObsidianCraft Staff
 */
public class SellOrder extends Order
{
    /** The price per item for this sale. **/
    public final double PRICE;
    
    /**
     * @param usr       The player who created this order.
     * @param merch     The merchandise (including quantity) to sell.
     * @param cfg       The already-looked-up configuration for this item.
     * @param price     The price per item for this sale.
     * @param time      The time at which this order was created.
     */
    public SellOrder(Player usr,ItemStack merch,Item cfg,double price,long time)
    {
        super(usr,merch,cfg,time);
        PRICE = price;
    }
}