package com.herbmc.extra_enchants.enchantments;

import net.minecraft.enchantment.*;
import net.minecraft.entity.EquipmentSlot;

public class Berserk extends Enchantment {

    public Berserk(Rarity weight, EnchantmentTarget type, EquipmentSlot[] equipmentSlots) {
        super(weight, type, equipmentSlots);
    }

    @Override
    public int getMinPower(int level) {
        return 5 + (level - 1) * 10;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public int getMaxPower(int level) {
        return this.getMinPower(level) + 10;
    }

    @Override
    protected boolean canAccept(Enchantment other)
    {
        return super.canAccept(other) && !(other instanceof ProtectionEnchantment);
    }

}
