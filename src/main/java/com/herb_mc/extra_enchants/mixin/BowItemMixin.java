package com.herb_mc.extra_enchants.mixin;

import com.herb_mc.extra_enchants.lib.EnchantmentMappings;
import com.herb_mc.extra_enchants.lib.PersistentProjectileEntityMixinAccess;
import com.herb_mc.extra_enchants.registry.ModEnchants;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(BowItem.class)
public class BowItemMixin {

    @Unique private static int strongDrawLevel;
    @Unique private static int nimbleLevel;

    @Inject(
            method = "onStoppedUsing",
            at = @At("HEAD")
    )
    private void getEnchantLevels(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfo ci) {
        strongDrawLevel = EnchantmentHelper.getLevel(ModEnchants.SNIPER, stack);
        nimbleLevel = EnchantmentHelper.getLevel(ModEnchants.NIMBLE, stack);
    }

    @Inject(
            method = "onStoppedUsing",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/projectile/PersistentProjectileEntity;setProperties(Lnet/minecraft/entity/Entity;FFFFF)V",
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void applyBowEnchants(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfo ci, PlayerEntity playerEntity, boolean bl, ItemStack itemStack, int i, float f, boolean bl2, ArrowItem arrowItem, PersistentProjectileEntity persistentProjectileEntity) {
        ((PersistentProjectileEntityMixinAccess) persistentProjectileEntity).setPlayerOwner(true);
        if (EnchantmentHelper.getEquipmentLevel(ModEnchants.CORE_OF_PURITY, user) > 0)
            persistentProjectileEntity.setDamage(EnchantmentMappings.corePurityBaseDamage.getFloat());
        else {
            if (f == 1.0F)
                ((PersistentProjectileEntityMixinAccess) persistentProjectileEntity).setCrit(true);
            if (EnchantmentHelper.getLevel(ModEnchants.ARROW_SPEED, stack) > 0)
                persistentProjectileEntity.setVelocity(persistentProjectileEntity.getVelocity().multiply((1.0F + EnchantmentMappings.arrowSpeedVelocityMult.getFloat() * EnchantmentHelper.getLevel(ModEnchants.ARROW_SPEED, stack))));
            if (EnchantmentHelper.getLevel(ModEnchants.NIMBLE, stack) > 0) {
                persistentProjectileEntity.setDamage(persistentProjectileEntity.getDamage() - 0.2F * EnchantmentHelper.getLevel(ModEnchants.NIMBLE, stack));
                if (persistentProjectileEntity.getDamage() < 0.5)
                    persistentProjectileEntity.setDamage(0.5);
            }
            if (EnchantmentHelper.getLevel(ModEnchants.SNIPER, stack) > 0) {
                persistentProjectileEntity.setVelocity(persistentProjectileEntity.getVelocity().multiply(1.0F + EnchantmentHelper.getLevel(ModEnchants.SNIPER, stack) * EnchantmentMappings.sniperVelocityMult.getFloat()));
                persistentProjectileEntity.setDamage(persistentProjectileEntity.getDamage() + EnchantmentMappings.sniperDamageBase.getFloat() * EnchantmentHelper.getLevel(ModEnchants.SNIPER, stack));
            }
            if (EnchantmentHelper.getEquipmentLevel(ModEnchants.ACE, user) > 0 && user.isFallFlying())
                persistentProjectileEntity.setDamage(persistentProjectileEntity.getDamage() + EnchantmentHelper.getEquipmentLevel(ModEnchants.ACE, user) * EnchantmentMappings.aceExtraArrowDamage.getFloat());
            if (EnchantmentHelper.getEquipmentLevel(ModEnchants.SHARPSHOOTER, user) > 0 && user.isSneaking()) {
                persistentProjectileEntity.setDamage(persistentProjectileEntity.getDamage() + EnchantmentMappings.sharpshooterArrowDamage.getFloat());
                ((PersistentProjectileEntityMixinAccess) persistentProjectileEntity).setSharpshooter(true);
            }
            if (EnchantmentHelper.getLevel(ModEnchants.EXPLOSIVE, stack) > 0)
                ((PersistentProjectileEntityMixinAccess) persistentProjectileEntity).setExplosive(EnchantmentHelper.getLevel(ModEnchants.EXPLOSIVE, stack));
            else if (EnchantmentHelper.getLevel(ModEnchants.ENDER, stack) > 0)
                ((PersistentProjectileEntityMixinAccess) persistentProjectileEntity).setEnder(true);
        }
    }

    @ModifyVariable(
            method = "getPullProgress",
            at = @At(
                    value = "STORE",
                    ordinal = 0
            ),
            index = 1
    )
    private static float FOV(float f) {
        f *= 20.0F;
        float div = 20.0F;
        if (strongDrawLevel > 0)
            div = div + div * EnchantmentMappings.sniperDrawMult.getFloat() * strongDrawLevel;
        else if (nimbleLevel > 0)
            div = div + div * nimbleLevel * EnchantmentMappings.nimbleDrawMult.getFloat();
        if (div <= 0)
            div = 1F;
        f /= div;
        return f;
    }

}
