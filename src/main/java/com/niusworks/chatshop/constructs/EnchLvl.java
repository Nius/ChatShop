package com.niusworks.chatshop.constructs;

import org.bukkit.enchantments.Enchantment;

/**
 * A simple vehicle for representing an Enchantment and its level.
 * 
 * @author ObsidianCraft Staff
 */
public class EnchLvl
{
    /** A Minecraft Enchantment type **/
    public final Enchantment ENCHANT;
    
    /** The level associated with this enchantment type **/
    public final int LVL;
    
    /**
     * @param enchant   A Minecraft Enchantment type.
     * @param lvl       The level associated with this enchantment type.
     */
    public EnchLvl(Enchantment enchant, int lvl)
    {
        ENCHANT = enchant; LVL = lvl;
    }
}
