package com.niusworks.chatshop.constructs;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.niusworks.chatshop.managers.ItemManager.Item;

/**
 * A simple vehicle for storing a pending sell order.
 * @author ObsidianCraft Staff
 */
public class SellOrder extends Order
{
    /** The price per item for this sale. **/
    public final double PRICE;
    
    /**
     * @param usr       The player who created this order.
     * @param merch     The merchandise (including quantity) to purchse.
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