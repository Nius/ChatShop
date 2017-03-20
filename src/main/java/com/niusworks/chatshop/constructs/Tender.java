package com.niusworks.chatshop.constructs;

import com.niusworks.chatshop.commands.Buy;
import com.niusworks.chatshop.managers.DatabaseManager;

/**
 * A simple vehicle for expressing the results of a buy order. This is used only by
 * {@link DatabaseManager} to return information to {@link Buy}, because when a user
 * entrusts the ChatShop to execute a purchase all of this information needs to be made
 * subsequently available to the user.
 * @author ObsidianCraft Staff
 */
public class Tender
{
    /** The quantity of items tendered. **/
    public final int QUANTITY;
    /** The TOTAL cost of ALL tendered items. **/
    public final double COST;
    /** Metadata about this transaction. **/
    public final boolean BROKE;
    /** How much the player bought from himself. **/
    public final int SELF;
    
    /**
     * @param q The quantity of items tendered. 
     * @param c The TOTAL cost of ALL tendered items.
     * @param b Whether the user went broke on this transaction.
     * @param self The amount this player bought from himself.
     */
    public Tender(int q, double c, boolean b, int self)
    {
        QUANTITY = q; COST = c; BROKE = b; SELF = self;
    }
}