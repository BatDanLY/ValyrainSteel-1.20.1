package com.batdan.valyrainsteel.item;

import com.batdan.valyrainsteel.item.lady_forlorn.LadyForlornEffects;
import com.batdan.valyrainsteel.item.lady_forlorn.WindBurstHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import team.lodestar.lodestone.registry.common.particle.LodestoneParticleRegistry;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData;

import team.lodestar.lodestone.systems.rendering.trail.*;

import java.awt.*;
import java.util.Random;

public class LadyForlornItem extends LegendarySwordItem {

    // ---- Active ability tuning ----
    // getUseDuration() is deliberately huge so the player can hold the charge as long as they like;
    // MAX_CHARGE_TICKS is our own soft cap that determines when the charge is "full".
    private static final int MAX_USE_DURATION = 72000;
    private static final int MAX_CHARGE_TICKS = 20 * 2; // 2.0s at full charge, tune to taste
    private static final float MIN_CHARGE_TO_FIRE = 0.05F; // tap-and-release does basically nothing

    // ---- Passive ability tuning ----
    private static final double AIR_ACCELERATION = 0.09; // added to horizontal speed per tick while steering in air
    private static final double AIR_MAX_SPEED = 5.0;    // horizontal speed cap while airborne (roughly 2x sprint)
    private static final int AGILITY_ACTIVATE_TICKS = 14;

    private static final Random RANDOM = new Random();

    private static final Color WIND_COLOR_START = new Color(108, 8, 31);
    private static final Color WIND_COLOR_END = new Color(255, 88, 255);

    public LadyForlornItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }

    // ============================================================
    //  ACTIVE ABILITY — charge-and-release wind burst
    // ============================================================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        LadyForlornEffects.playChargeStartSound(level, player);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return MAX_USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        // SPEAR keeps the sword held out in front (like a trident aim) instead of the bow's
        // sideways draw - reads better for "gathering wind in front of the blade".
        return UseAnim.SPEAR;
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        super.onUseTick(level, livingEntity, stack, remainingUseDuration);
        if (!(livingEntity instanceof Player player)) {
            return;
        }

        float chargeFraction = getChargeFraction(remainingUseDuration);

        // onUseTick is mirrored client-side for every nearby player watching this entity use the
        // item (that's how vanilla shows other players charging a bow too) - so gating on
        // isClientSide here means every observer's own client spawns the swirl locally, with no
        // networking code required on our part.
        if (level.isClientSide) {
            LadyForlornEffects.spawnChargeParticles(level, player, chargeFraction);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }

        float chargeFraction = getChargeFraction(timeLeft);
        if (chargeFraction < MIN_CHARGE_TO_FIRE) {
            return;
        }

        // Both sides independently raytrace from the same eye position / look angle, so they agree
        // on whether this was a wall-bounce or an open-air cone blast without needing to sync it.
        BlockHitResult hit = WindBurstHandler.traceLookHit(level, player);

        if (level.isClientSide) {
            LadyForlornEffects.spawnReleaseVisuals(level, player, hit, chargeFraction);
            return;
        }

        if (livingEntity instanceof ServerPlayer serverPlayer) {
            WindBurstHandler.applyPhysics((ServerLevel) level, serverPlayer, hit, chargeFraction);
        }
    }

    private static float getChargeFraction(int ticksRemainingOrLeft) {
        int chargeTicks = MAX_USE_DURATION - ticksRemainingOrLeft;
        return Mth.clamp(chargeTicks / (float) MAX_CHARGE_TICKS, 0F, 1F);
    }

    // ============================================================
    //  PASSIVE ABILITY — aerial agility
    //  (soft-landing/no fall damage lives in LadyForlonEvents, since that needs a Forge event)
    // ============================================================

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slot, isSelected);

        if (!(entity instanceof Player player) || player.getMainHandItem() != stack) {
            return;
        }

        //TrailPointBuilder.create(40).addTrailPoint(TrailPoint())



        applyAerialAgility(player, level);
    }

    /**
     * Deliberately runs on BOTH logical sides. Client and server compute the exact same boost from
     * the exact same inputs (xxa/zza/yaw/onGround), so they naturally stay in agreement without any
     * position-correction jank - this is the same trick used for the local player's normal movement
     * prediction. Do not gate this with !level.isClientSide.
     */
    private void applyAerialAgility(Player player, Level level) {
        // Don't apply while on the ground or in states where air control shouldn't work.
        if (player.onGround() || player.isFallFlying() || player.isInWater() || player.isInLava()) {
            player.getPersistentData().putInt("AirTicks", 0);
            return;
        }

        // Only activate after the player has actually been airborne for a few ticks.
        int airTicks = player.getPersistentData().getInt("AirTicks") + 1;
        player.getPersistentData().putInt("AirTicks", airTicks);

        if (airTicks < AGILITY_ACTIVATE_TICKS) {
            return;
        }

        Vec3 inputDirection = getWorldSpaceInput(player);

        // No movement input.
        if (inputDirection.lengthSqr() < 1.0E-7) {
            return;
        }

        Vec3 motion = player.getDeltaMovement();
        Vec3 horizontal = new Vec3(motion.x, 0.0D, motion.z);

        // Speed in the direction the player wants to move.
        double speedAlongInput = horizontal.dot(inputDirection);

        // Only add speed up to AIR_MAX_SPEED in that direction.
        if (speedAlongInput < AIR_MAX_SPEED) {
            double accel = Math.min(AIR_ACCELERATION, AIR_MAX_SPEED - speedAlongInput);
            horizontal = horizontal.add(inputDirection.scale(accel));
        }

        player.setDeltaMovement(horizontal.x, motion.y, horizontal.z);

        Vec3 loc = player.position();


        double angle = RANDOM.nextDouble() * Math.PI * 2;
        double radius = Mth.lerp(RANDOM.nextDouble(), 0.4, 1.0) * 0.5;

        double px = loc.x + Math.cos(angle) * radius;
        double py = loc.y;
        double pz = loc.z + (Math.sin(angle) * radius);

        Vec3 inward = motion.subtract(px, py, pz).normalize().scale(0.08 + 0.1);

        WorldParticleBuilder.create(LodestoneParticleRegistry.SMOKE_PARTICLE)
                .setLifetime(6 + RANDOM.nextInt(14))
                .setScaleData(GenericParticleData.create(0.3F, 0.00F).build())
                .setTransparencyData(GenericParticleData.create(0.8F, 0.4F).build())
                .setColorData(ColorParticleData.create(WIND_COLOR_START, WIND_COLOR_END).build())
                .setSpinData(SpinParticleData.create(0F, 0.15F).build())
                .setMotion(inward.x, inward.y, inward.z)
                .enableNoClip()
                .spawn(level, px, py, pz);
    }



    /**
     * Rebuilds the player's raw WASD input (xxa = strafe, zza = forward) as a world-space direction
     * relative to their yaw - the same math LivingEntity#getInputVector uses internally for travel().
     * We can't call that method directly (it's protected on Entity), so it's reproduced here.
     */
    private static Vec3 getWorldSpaceInput(Player player) {
        double strafe = player.xxa;
        double forward = player.zza;
        double lenSq = strafe * strafe + forward * forward;
        if (lenSq < 1.0E-7) {
            return Vec3.ZERO;
        }

        Vec3 relative = new Vec3(strafe, 0, forward);
        if (lenSq > 1.0) {
            relative = relative.normalize();
        }

        float yaw = player.getYRot();
        float sinYaw = Mth.sin(yaw * ((float) Math.PI / 180F));
        float cosYaw = Mth.cos(yaw * ((float) Math.PI / 180F));

        return new Vec3(
                relative.x * cosYaw - relative.z * sinYaw,
                0,
                relative.z * cosYaw + relative.x * sinYaw
        );
    }
}