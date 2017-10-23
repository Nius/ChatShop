package com.niusworks.chatshop.constructs;

import org.bukkit.entity.Player;

import com.niusworks.chatshop.commands.Confirm;
import com.niusworks.chatshop.commands.EBuy;

/**
 * A simple vehicle for storing a pending ebuy order. These orders are created by
 * {@link EBuy} and read exclusively by {@link Confirm}.
 * The {@link #MERCH} and {@link #CONFIG} for an EBuyOrder are always null.
 * @author ObsidianCraft Staff
 */
public class EBuyOrder extends Order
{
    /** The lot number to purchase **/
    public final int LOT;
    
    /** The price for the lot. Used to ensure that the price
     *  doesn't change before /confirm. **/
    public final double PRICE;
    
    /**
     * @param usr     The player who created this order.
     * @param lot     The lot number to purchase.
     * @param price   The price of the lot.
     * @param time    The time at which this order was created.
     */
    public EBuyOrder(Player usr,int lot,double price,long time)
    {
        super(usr,null,null,time);
        LOT = lot; PRICE = price;
    }
}
