package com.herb_mc.extra_enchants.mixin;

import com.herb_mc.extra_enchants.commons.AttributeModCommons;
import com.herb_mc.extra_enchants.registry.ModEnchants;
import com.herb_mc.extra_enchants.commons.UUIDCommons;
import net.minecraft.block.Material;
import net.minecraft.block.OreBlock;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.EntityDamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Objects;
import java.util.Random;

import static net.minecraft.enchantment.EnchantmentHelper.getEquipmentLevel;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements EntityInterfaceMixin, HorseBaseEntityInterfaceMixin, AttributeModCommons, UUIDCommons {

    @Shadow public abstract int getArmor();

    @Unique private final LivingEntity thisEntity = (LivingEntity) (Object) this;
    @Unique private float STEP_HEIGHT = 0F;
    @Unique private int SPRINT_BOOST = 0;
    @Unique private static final Random rand = new Random();
    @Unique int level = 0;

    @Inject(method = "writeCustomDataToNbt", at = @At("RETURN"))
    protected void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo info) {
        nbt.putFloat("stepHeight", STEP_HEIGHT);
        nbt.putInt("sprintBoost", SPRINT_BOOST);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
    protected void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo info) {
        STEP_HEIGHT = nbt.getFloat("stepHeight");
        SPRINT_BOOST = nbt.getInt("sprintBoost");
    }

    @Inject(at = @At("HEAD"), method = "onDeath")
    protected void onDeath(DamageSource source, CallbackInfo info){
        if (source.getAttacker() instanceof LivingEntity) {
            ((LivingEntity) source.getAttacker()).heal((float) getEquipmentLevel(ModEnchants.LIFESTEAL, (LivingEntity) source.getAttacker()));
        }
    }



    @ModifyVariable(
            method = "applyArmorToDamage",
            at = @At(value = "HEAD", target = "Lnet/minecraft/entity/LivingEntity;applyArmorToDamage(Lnet/minecraft/entity/damage/DamageSource;F)F"),
            ordinal = 0)
    private float amount(float amount) {
        return (getEquipmentLevel(ModEnchants.TOUGH, thisEntity) > 0) ? amount * (1.0F - 0.03F * getEquipmentLevel(ModEnchants.TOUGH, thisEntity)) : amount;
    }

    @ModifyVariable(
            method = "applyArmorToDamage",
            at = @At(value = "HEAD", target = "Lnet/minecraft/entity/LivingEntity;applyArmorToDamage(Lnet/minecraft/entity/damage/DamageSource;F)F"),
            ordinal = 0)
    private DamageSource source(DamageSource source) {
        level = 0;
        if (source.getAttacker() != null)
            if (source.getAttacker() instanceof LivingEntity)
                level = getEquipmentLevel(ModEnchants.CLEAVING, (LivingEntity) source.getAttacker());
        return source;
    }

    @ModifyVariable(
            method = "computeFallDamage",
            at = @At(value = "HEAD", target = "Lnet/minecraft/entity/LivingEntity;computeFallDamage(FF)I"),
            ordinal = 0)
    private float fallDistance(float fallDistance) {
        int i = EnchantmentHelper.getEquipmentLevel(ModEnchants.FEATHERWEIGHT, thisEntity);
        if (!thisEntity.isSneaking() && i > 0)
            fallDistance /= 2 * i;
        i = EnchantmentHelper.getEquipmentLevel(ModEnchants.LEAPING, thisEntity);
        if (i > 0)
            fallDistance -= (float) i - 1.0F;
        return fallDistance;
    }

    @ModifyVariable(
            method = "damage",
            at = @At(value = "HEAD", target = "Lnet/minecraft/entity/LivingEntity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"),
            ordinal = 0)
    private float amount(float amount, DamageSource source) {
        int i = EnchantmentHelper.getEquipmentLevel(ModEnchants.CORE_OF_THE_BLOOD_GOD, thisEntity);
        if (source instanceof EntityDamageSource && i > 0 && rand.nextDouble() < 0.25)
            amount *= 1.8;
        i = EnchantmentHelper.getEquipmentLevel(ModEnchants.BLAZE_AFFINITY, thisEntity);
        if (i > 0 && thisEntity.isOnFire())
            amount *= 0.95;
        i = EnchantmentHelper.getEquipmentLevel(ModEnchants.CORE_OF_THE_VOID, thisEntity);
        if (i > 0)
            amount *= 0.6;
        if (source.getSource() != null && source.getSource() instanceof LivingEntity) {
            i = EnchantmentHelper.getEquipmentLevel(ModEnchants.CORE_OF_PURITY, (LivingEntity) source.getSource());
            if (i > 0)
                amount = 1;
        }
        i = EnchantmentHelper.getEquipmentLevel(ModEnchants.CORE_OF_PURITY, thisEntity);
        if (i > 0 && thisEntity.getHealth() / thisEntity.getMaxHealth() > 0.6)
            amount *= 0.3;
        return amount;
    }

    @ModifyVariable(
            method = "travel",
            at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/block/Block;getSlipperiness()F")
    )
    private float t(float t) {
        return (EnchantmentHelper.getEquipmentLevel(ModEnchants.SLIMEY, thisEntity) > 0) ? 1.0F : t;
    }

    @ModifyArg(
            method = "applyArmorToDamage",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/DamageUtil;getDamageLeft(FFF)F"),
            index = 1)
    private float armor(float armor) {
        double mult = 1.0D - (float) (1.8D * (level / (2.0D * level + 4.0D)));
        return (float) (armor * mult);
    }

    @ModifyArg(
            method = "jump",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;setVelocity(DDD)V"),
            index = 1)
    private double jumpVelocity(double d) {
        return (EnchantmentHelper.getEquipmentLevel(ModEnchants.LEAPING, thisEntity) > 0) ? d + 0.075F * (float) EnchantmentHelper.getEquipmentLevel(ModEnchants.LEAPING, thisEntity) : d;
    }

    @ModifyConstant(method = "travel", constant = @Constant(doubleValue = 0.08D))
    private double d(double d){
        return (EnchantmentHelper.getEquipmentLevel(ModEnchants.FEATHERWEIGHT, thisEntity) > 0 && thisEntity.getVelocity().y < 0 && !thisEntity.isSneaking()) ? d / (EnchantmentHelper.getEquipmentLevel(ModEnchants.FEATHERWEIGHT, thisEntity) + 1) : d;
    }

    @ModifyArg(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;setAir(I)V"))
    private int neptuneModAirUnderwater(int air) {
        return (EnchantmentHelper.getEquipmentLevel(ModEnchants.CORE_OF_NEPTUNE, thisEntity) > 0 && thisEntity.isSubmergedIn(FluidTags.WATER)) ? air - 3 : air;
    }

    @ModifyConstant(method = "getNextAirOnLand", constant = @Constant(intValue = 4))
    private int neptuneModAir(int air) {
        return (EnchantmentHelper.getEquipmentLevel(ModEnchants.CORE_OF_NEPTUNE, thisEntity) > 0) ? rand.nextInt(16) % 2 : air;
    }

    @ModifyVariable(
            method = "getNextAirUnderwater",
            at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/enchantment/EnchantmentHelper;getRespiration(Lnet/minecraft/entity/LivingEntity;)I"), ordinal = 1)
    protected int respLevel(int i) {
        return (EnchantmentHelper.getEquipmentLevel(ModEnchants.CORE_OF_NEPTUNE, thisEntity) > 0) ? 0 : i;
    }

    @ModifyArgs(
            method = "travel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V", ordinal = 0)
    )
    protected void neptuneSwimmingVelocity(Args args) {
        if (EnchantmentHelper.getEquipmentLevel(ModEnchants.CORE_OF_NEPTUNE, thisEntity) > 0) {
            Vec3d vec3d = thisEntity.getVelocity();
            if (thisEntity.horizontalCollision && thisEntity.isClimbing()) {
                vec3d = new Vec3d(vec3d.x, 0.2D, vec3d.z);
            }
            args.set(0, vec3d.multiply(0.97D, 0.800000011920929D, 0.97D));
        }
    }

    @Inject(
            method = "jump",
            at = @At("TAIL"))
    private void changeVelocity(CallbackInfo info) {
        int i = EnchantmentHelper.getEquipmentLevel(ModEnchants.LUNGING, thisEntity);
        float mult = (float) (1 + 0.1 * i);
        if (i > 0)
            thisEntity.setVelocity(thisEntity.getVelocity().x * mult, thisEntity.getVelocity().y, thisEntity.getVelocity().z * mult);
        i = EnchantmentHelper.getEquipmentLevel(ModEnchants.SHARPSHOOTER, thisEntity);
        if (i > 0 && thisEntity.isSneaking())
            thisEntity.setVelocity(0, thisEntity.getVelocity().y, 0);
    }

    @Inject(at = @At("HEAD"), method = "tick")
    public void tick(CallbackInfo info) {
        if (STEP_HEIGHT == 0F)
            STEP_HEIGHT = thisEntity.stepHeight;
        int i = getEquipmentLevel(ModEnchants.WINDSTEP, thisEntity);
        thisEntity.stepHeight = STEP_HEIGHT;
        if (i > 0)
            thisEntity.stepHeight += i * 0.4F;
        removeAttribute(thisEntity, EntityAttributes.GENERIC_ATTACK_DAMAGE, CORE_OF_NEPTUNE_ATTRIBUTE_ID);
        removeAttribute(thisEntity, EntityAttributes.GENERIC_ATTACK_SPEED, CORE_OF_NEPTUNE_ATTRIBUTE_ID);
        removeAttribute(thisEntity, EntityAttributes.GENERIC_MOVEMENT_SPEED, CORE_OF_NEPTUNE_ATTRIBUTE_ID);
        if (EnchantmentHelper.getEquipmentLevel(ModEnchants.CORE_OF_NEPTUNE, thisEntity) > 0) {
            int air = thisEntity.getAir();
            if (air > 120) {
                modAttributeBase(thisEntity, EntityAttributes.GENERIC_ATTACK_DAMAGE, 1, CORE_OF_NEPTUNE_ATTRIBUTE_ID, "nep_attack_damage", (120.0 - air) / 450.0, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
                modAttributeBase(thisEntity, EntityAttributes.GENERIC_ATTACK_SPEED, 1, CORE_OF_NEPTUNE_ATTRIBUTE_ID, "nep_attack_speed", (120.0 - air) / 450.0, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
                modAttributeBase(thisEntity, EntityAttributes.GENERIC_MOVEMENT_SPEED, 1, CORE_OF_NEPTUNE_ATTRIBUTE_ID, "nep_movement_speed", (120.0 - air) / 900.0, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
            }
            else if (air < 90) {
                modAttributeBase(thisEntity, EntityAttributes.GENERIC_ATTACK_DAMAGE, 1, CORE_OF_NEPTUNE_ATTRIBUTE_ID, "nep_attack_damage", (90 - air) / 270.0, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
                modAttributeBase(thisEntity, EntityAttributes.GENERIC_ATTACK_SPEED, 1, CORE_OF_NEPTUNE_ATTRIBUTE_ID, "nep_attack_speed", (90 - air) / 270.0, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
                modAttributeBase(thisEntity, EntityAttributes.GENERIC_MOVEMENT_SPEED, 1, CORE_OF_NEPTUNE_ATTRIBUTE_ID, "nep_movement_speed", (90 - air) / 540.0, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
            }
        }
        removeAttribute(thisEntity, EntityAttributes.GENERIC_ATTACK_DAMAGE, BLAZE_ATTRIBUTE_ID);
        i = EnchantmentHelper.getEquipmentLevel(ModEnchants.BLAZE_AFFINITY, thisEntity);
        if (i > 0 && thisEntity.isOnFire())
            modAttributeBase(thisEntity, EntityAttributes.GENERIC_ATTACK_DAMAGE, 1, BLAZE_ATTRIBUTE_ID, "blz_attack_daamge", 0.1, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
        removeAttribute(thisEntity, EntityAttributes.GENERIC_MOVEMENT_SPEED, BOOSTING_ATTRIBUTE_ID);
        i = getEquipmentLevel(ModEnchants.BOOSTING, thisEntity);
        if (thisEntity.isSprinting() && i > 0 && SPRINT_BOOST >= 0) {
            SPRINT_BOOST++;
            if (SPRINT_BOOST < 20 * i)
                modAttributeBase(thisEntity, EntityAttributes.GENERIC_MOVEMENT_SPEED, i, BOOSTING_ATTRIBUTE_ID, "boost_speed", 0.4, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
            else
                SPRINT_BOOST = -60;
        }
        if (SPRINT_BOOST > 0 && !thisEntity.isSprinting())
            SPRINT_BOOST = -60;
        if (SPRINT_BOOST < 0 && !thisEntity.isSprinting())
            SPRINT_BOOST++;
        i = getEquipmentLevel(ModEnchants.DWARVEN, thisEntity);
        if (i > 0) {
            Vec3d vec = getNearestOre();
            double sneak = 0;
            if (thisEntity.isInSwimmingPose())
                sneak = 0.8;
            if (thisEntity.isSneaking())
                sneak = 0.2;
            if (vec != null) {
                double dx = vec.getX() - thisEntity.getX();
                double dy = vec.getY() - thisEntity.getY() - 1.0 - sneak;
                double dz = vec.getZ() - thisEntity.getZ();
                thisEntity.world.addParticle(ParticleTypes.ELECTRIC_SPARK, true, thisEntity.getX() + dx / 10, thisEntity.getY() + 1.0 - sneak + dy / 10, thisEntity.getZ() + dz / 10, dx / 1.25, dy / 1.25, dz / 1.25);
            }
        }
        i = getEquipmentLevel(ModEnchants.CORE_OF_THE_BLOOD_GOD, thisEntity);
        removeAttribute(thisEntity, EntityAttributes.GENERIC_ARMOR, CORE_OF_THE_BLOOD_GOD_ATTRIBUTE_ID);
        removeAttribute(thisEntity, EntityAttributes.GENERIC_ATTACK_DAMAGE, CORE_OF_THE_BLOOD_GOD_ATTRIBUTE_ID);
        removeAttribute(thisEntity, EntityAttributes.GENERIC_MAX_HEALTH, CORE_OF_THE_BLOOD_GOD_ATTRIBUTE_ID);
        if (i > 0) {
            modAttributeBase(thisEntity, EntityAttributes.GENERIC_ARMOR, 1, CORE_OF_THE_BLOOD_GOD_ATTRIBUTE_ID, "bld_armor", -0.2, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
            modAttributeBase(thisEntity, EntityAttributes.GENERIC_ATTACK_DAMAGE, 1, CORE_OF_THE_BLOOD_GOD_ATTRIBUTE_ID, "bld_attack_damage", 0.15, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
            modAttributeBase(thisEntity, EntityAttributes.GENERIC_MAX_HEALTH, 1, CORE_OF_THE_BLOOD_GOD_ATTRIBUTE_ID, "bld_attack_damage", 1.0, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
        }
        i = getEquipmentLevel(ModEnchants.CORE_OF_THE_VOID, thisEntity);
        removeAttribute(thisEntity, EntityAttributes.GENERIC_MAX_HEALTH, CORE_OF_THE_VOID_ATTRIBUTE_ID);
        removeAttribute(thisEntity, EntityAttributes.GENERIC_MOVEMENT_SPEED, CORE_OF_THE_VOID_ATTRIBUTE_ID);
        if (i > 0) {
            modAttributeBase(thisEntity, EntityAttributes.GENERIC_MAX_HEALTH, 1, CORE_OF_THE_VOID_ATTRIBUTE_ID, "vd_health", -0.5, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
            modAttributeBase(thisEntity, EntityAttributes.GENERIC_MOVEMENT_SPEED, 1, CORE_OF_THE_VOID_ATTRIBUTE_ID, "vd_speed", 0.15, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);

        }
        i = getEquipmentLevel(ModEnchants.CORE_OF_PURITY, thisEntity);
        removeAttribute(thisEntity, EntityAttributes.GENERIC_MAX_HEALTH, CORE_OF_PURITY_ATTRIBUTE_ID);
        removeAttribute(thisEntity, EntityAttributes.GENERIC_MOVEMENT_SPEED, CORE_OF_PURITY_ATTRIBUTE_ID);
        if (i > 0) {
            modAttributeBase(thisEntity, EntityAttributes.GENERIC_MAX_HEALTH, 1, CORE_OF_PURITY_ATTRIBUTE_ID, "ev_health", 1.0, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
            modAttributeBase(thisEntity, EntityAttributes.GENERIC_MOVEMENT_SPEED, 1, CORE_OF_PURITY_ATTRIBUTE_ID, "ev_speed", -0.1, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
        }
        i = getEquipmentLevel(ModEnchants.NIGHT_VISION, thisEntity);
        if (i > 0 && thisEntity.isSneaking())
            thisEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 2000, 100, true, false, false));
        if ((i == 0 || i > 0 && !thisEntity.isSneaking()) && thisEntity.getStatusEffect(StatusEffects.NIGHT_VISION) != null)
            if (Objects.requireNonNull(thisEntity.getStatusEffect(StatusEffects.NIGHT_VISION)).getAmplifier() == 100)
                thisEntity.removeStatusEffect(StatusEffects.NIGHT_VISION);
        i = getEquipmentLevel(ModEnchants.PSYCHIC, thisEntity);
        if (i > 0 && thisEntity.isSneaking()) {
            EntityHitResult result = raycast();
            if (result != null)
                if (result.getEntity() instanceof LivingEntity)
                    ((LivingEntity) result.getEntity()).addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 2, 20, true, false, false));
        }
        i = getEquipmentLevel(ModEnchants.BARBARIC, thisEntity);
        removeAttribute(thisEntity, EntityAttributes.GENERIC_ATTACK_DAMAGE, BARBARIC_ATTRIBUTE_ID);
        if (i > 0)
            modAttributeBase(thisEntity, EntityAttributes.GENERIC_ATTACK_DAMAGE, 20 - this.getArmor(), BARBARIC_ATTRIBUTE_ID, "bar_attack_damage", 0.04, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
        i = getEquipmentLevel(ModEnchants.BERSERK, thisEntity);
        removeAttribute(thisEntity, EntityAttributes.GENERIC_ATTACK_DAMAGE, BERSERK_ATTRIBUTE_ID);
        if (i > 0)
            modAttributeExtended(thisEntity, EntityAttributes.GENERIC_ATTACK_DAMAGE, i, BERSERK_ATTRIBUTE_ID, "ber_attack_damage", (thisEntity.getMaxHealth() - thisEntity.getHealth()), 2.0, 2.0, 1.0, 2.0, 0.0, 4.0, 0.0, EntityAttributeModifier.Operation.ADDITION);
        if (thisEntity instanceof HorseEntity) {
            if (getEquipmentLevel(ModEnchants.SURFACE_SKIMMER, thisEntity) > 0)
                this.updateFloating();
            i = getEquipmentLevel(ModEnchants.SWIFTNESS, thisEntity);
            removeAttribute(thisEntity, EntityAttributes.GENERIC_MOVEMENT_SPEED, SWIFTNESS_ATTRIBUTE_ID);
            if (i > 0)
                modAttributeBase(thisEntity, EntityAttributes.GENERIC_MOVEMENT_SPEED, i, SWIFTNESS_ATTRIBUTE_ID, "swift_speed_boost", 0.1, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
            i = getEquipmentLevel(ModEnchants.BOUNDING, thisEntity);
            removeAttribute(thisEntity, EntityAttributes.HORSE_JUMP_STRENGTH, BOUNDING_JUMP_BOOST_ID);
            if (i > 0)
                modAttributeBase(thisEntity, EntityAttributes.HORSE_JUMP_STRENGTH, i, BOUNDING_JUMP_BOOST_ID, "bounding_jump_height_boost", 0.1, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
        }
        if (EnchantmentHelper.getEquipmentLevel(ModEnchants.CORE_OF_NEPTUNE, thisEntity) > 0 && thisEntity.getAir() < 0)
            thisEntity.setAir(0);
        if (thisEntity.getHealth() > thisEntity.getMaxHealth())
            thisEntity.setHealth(thisEntity.getMaxHealth());
    }

    private void updateFloating() {
        if (this.isInWater()) {
            thisEntity.setOnGround(true);
            double vecY = 0.019 + rand.nextGaussian() / 500;
            thisEntity.setVelocity(thisEntity.getVelocity().add(0.0D, vecY, 0.0D));
        }
        BlockPos entityPos = thisEntity.getBlockPos();
        Material mat = thisEntity.world.getBlockState((new BlockPos(entityPos.getX(), thisEntity.getBoundingBox().getMin(Direction.Axis.Y) + 0.1D, entityPos.getZ()))).getMaterial();
        if (mat == Material.WATER || mat == Material.UNDERWATER_PLANT) {
            if (thisEntity.getVelocity().y < 0.0)
                thisEntity.setVelocity(new Vec3d(thisEntity.getVelocity().x, 0.0, thisEntity.getVelocity().z));
            thisEntity.setOnGround(true);
            this.setAir(false);
        }
    }

    public boolean isInWater() {
        return !this.firstUpdate() && this.fluidHeight().getDouble(FluidTags.WATER) > 0.0D;
    }

    private Vec3d getNearestOre() {
        Vec3d vec3d = null;
        double lowestSquareDistance = 30.25;
        Vec3d vec1 = new Vec3d(thisEntity.getX(), thisEntity.getY() + 1.0, thisEntity.getZ());
        for (BlockPos pos : BlockPos.iterate(thisEntity.getBlockPos().add(-6, -6, -6), thisEntity.getBlockPos().add(6, 6, 6))) {
            Vec3d vec2 = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (thisEntity.world.getBlockState(pos).getBlock() instanceof OreBlock && getSquareDist(vec1, vec2) <= lowestSquareDistance) {
                vec3d = vec2;
                lowestSquareDistance = getSquareDist(vec1, vec2);
            }
        }
        return vec3d;
    }

    public double getSquareDist(Vec3d in1, Vec3d in2){
        in2 = in2.subtract(in1);
        return in2.x * in2.x + in2.y * in2.y + in2.z * in2.z;
    }

    public EntityHitResult raycast() {
        Vec3d veceye = thisEntity.getEyePos();
        Vec3d vecdir = thisEntity.getRotationVec(1.0F);
        Vec3d vecext = veceye.add(vecdir.x * 6, vecdir.y * 6, vecdir.z * 6);
        Box box = thisEntity.getBoundingBox().stretch(vecdir.multiply(6)).expand(0.0D, 0.0D, 0.0D);
        return ProjectileUtil.raycast(thisEntity, veceye, vecext, box, (entityx) -> !entityx.isSpectator() && entityx.collides(), 49);
    }

}