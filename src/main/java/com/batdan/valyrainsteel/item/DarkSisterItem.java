package com.batdan.valyrainsteel.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Dark Sister - a Valyrian steel longsword.
 *
 * Passive: every consecutive hit (melee or via the dash) raises the weapon's critical chance by
 * 5%, up to 10 stacks (50%). A critical hit deals 2x damage. The streak resets if the weapon
 * hasn't landed a hit in {@link #HIT_RESET_TICKS} ticks.
 *
 * Active: right-click and hold briefly, then release, to dash forward (Riptide-style, no water
 * required). Any living entity the player passes through during the dash is struck (counts
 * towards the crit streak) and set on fire.
 */
public class DarkSisterItem extends LegendarySwordItem {

    private static final String TAG_CRIT_STACKS = "DarkSisterCritStacks";
    private static final String TAG_LAST_HIT_TIME = "DarkSisterLastHit";
    private static final String TAG_LAST_CRIT_TIME = "DarkSisterLastCrit";

    public static final int MAX_STACKS = 10;
    public static final float STACK_PERCENT = 0.05f; // 5% per stack, caps at 50%
    public static final long HIT_RESET_TICKS = 60;   // 3 seconds without landing a hit resets the streak

    private static final int DASH_COOLDOWN_TICKS = 60;   // 3 second cooldown on the dash
    private static final float DASH_STRENGTH = 2.6f;
    private static final int MIN_CHARGE_TICKS = 5;       // minimum hold time before release triggers the dash
    public static final float DASH_HIT_DAMAGE = 6.0f;

    public DarkSisterItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }

    // ---------------------------------------------------------------
    // Passive: stacking critical chance
    // ---------------------------------------------------------------

    /** Current crit chance (0.0 - 0.5) stored on the stack, for tooltip/UI use. */
    public static float getCurrentCritChance(ItemStack stack) {
        int stacks = Math.min(stack.getOrCreateTag().getInt(TAG_CRIT_STACKS), MAX_STACKS);
        return stacks * STACK_PERCENT;
    }

    /** Current stack count (0-10), used to drive the blade's visual "heat" and particle intensity. */
    public static int getCritStacks(ItemStack stack) {
        return Math.min(stack.getOrCreateTag().getInt(TAG_CRIT_STACKS), MAX_STACKS);
    }

    /** 0.0 - 1.0 heat value read by the client-side item property, used to pick the blade model/texture stage. */
    public static float getHeatProperty(ItemStack stack) {
        return getCritStacks(stack) / (float) MAX_STACKS;
    }

    /**
     * Call once per server tick for a held Dark Sister. If the streak has gone quiet for longer
     * than {@link #HIT_RESET_TICKS}, immediately clears the stacks (rather than waiting for the
     * next swing to notice) and reports that it just extinguished so callers can play an effect.
     */
    public static boolean tickAndCheckExtinguish(ItemStack stack, Level level) {
        CompoundTag tag = stack.getOrCreateTag();
        int stacks = tag.getInt(TAG_CRIT_STACKS);
        if (stacks <= 0) return false;

        long lastHit = tag.getLong(TAG_LAST_HIT_TIME);
        long now = level.getGameTime();
        if (lastHit != 0 && now - lastHit > HIT_RESET_TICKS) {
            tag.putInt(TAG_CRIT_STACKS, 0);
            return true;
        }
        return false;
    }

    /**
     * Rolls a crit for the hit currently landing, using the chance built up from previous
     * consecutive hits, then advances the streak for the next hit. Call this exactly once per
     * hit landed (melee hit or dash pass-through).
     *
     * @return true if this hit rolled as a critical (i.e. should deal 2x damage)
     */
    public static boolean rollAndAdvanceCrit(ItemStack stack, Level level) {
        CompoundTag tag = stack.getOrCreateTag();

        long now = level.getGameTime();
        long lastHit = tag.getLong(TAG_LAST_HIT_TIME);
        int stacks = tag.getInt(TAG_CRIT_STACKS);

        if (lastHit != 0 && now - lastHit > HIT_RESET_TICKS) {
            stacks = 0; // streak broken, this hit starts back at 0%
        }

        float chance = Math.min(stacks, MAX_STACKS) * STACK_PERCENT;
        boolean isCrit = level.getRandom().nextFloat() < chance;

        stacks = Math.min(stacks + 1, MAX_STACKS);

        tag.putInt(TAG_CRIT_STACKS, stacks);
        tag.putLong(TAG_LAST_HIT_TIME, now);
        if (isCrit) {
            tag.putLong(TAG_LAST_CRIT_TIME, now);
        }

        return isCrit;
    }

    /** Game-time tick of the most recent crit, read client-side to trigger a one-shot VFX burst. */
    public static long getLastCritTime(ItemStack stack) {
        return stack.getOrCreateTag().getLong(TAG_LAST_CRIT_TIME);
    }

    // ---------------------------------------------------------------
    // Active: dash
    // ---------------------------------------------------------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000; // held indefinitely until released, same pattern as the trident
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return;

        int chargeDuration = this.getUseDuration(stack) - timeLeft;
        if (chargeDuration < MIN_CHARGE_TICKS) return; // released too early, no dash

        if (level.isClientSide) return;

        Vec3 look = player.getLookAngle().normalize();
        player.setDeltaMovement(player.getDeltaMovement().add(
                look.x * DASH_STRENGTH,
                look.y * DASH_STRENGTH * 0.6 + 0.15,
                look.z * DASH_STRENGTH
        ));
        player.hurtMarked = true; // forces the velocity change to sync to the client, like riptide does

        player.getCooldowns().addCooldown(this, DASH_COOLDOWN_TICKS);

        // Flag the player as "dashing" so ModEvents' tick handler sweeps for entities in the path
        player.getPersistentData().putInt("valyrainsteel_dash_ticks", 8);
        player.getPersistentData().putUUID("valyrainsteel_dash_id", UUID.randomUUID());

        level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 1.3f, 0.85f);
        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.6f, 1.6f);

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    player.getX(), player.getY() + 1, player.getZ(), 20, 0.3, 0.3, 0.3, 0.05);
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    player.getX(), player.getY() + 1, player.getZ(), 15, 0.3, 0.5, 0.3, 0.2);
        }
    }

    // ---------------------------------------------------------------
    // Ambient blade visuals: heats up / smokes with the crit streak, extinguishes when it drops
    // ---------------------------------------------------------------

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slot, isSelected);

        if (!(entity instanceof LivingEntity living)) return;
        if (!isSelected || living.getMainHandItem() != stack) return; // only while actively wielded

        CompoundTag tag = stack.getOrCreateTag();
        int stacks = Math.min(tag.getInt(TAG_CRIT_STACKS), MAX_STACKS);
        long lastHit = tag.getLong(TAG_LAST_HIT_TIME);
        long now = level.getGameTime();

        // The streak has gone cold - snuff the blade out
        if (stacks > 0 && lastHit != 0 && now - lastHit > HIT_RESET_TICKS) {
            tag.putInt(TAG_CRIT_STACKS, 0);
            tag.putLong(TAG_LAST_HIT_TIME, 0);
            if (!level.isClientSide) {
                playExtinguishEffects(level, living);
            }
            return;
        }

        if (stacks <= 0 || level.isClientSide) return;

        // Ambient heat particles get more frequent and heavier the higher the streak climbs
        int interval = Math.max(4, 14 - stacks);
        if (living.tickCount % interval != 0) return;

        spawnBladeHeatParticles((ServerLevel) level, living, stacks);
    }

    private static void spawnBladeHeatParticles(ServerLevel level, LivingEntity living, int stacks) {
        Vec3 look = living.getLookAngle();
        Vec3 origin = living.position().add(0, living.getEyeHeight() * 0.55, 0).add(look.scale(0.9));

        int flameCount = 1 + stacks / 2;
        level.sendParticles(ParticleTypes.FLAME, origin.x, origin.y, origin.z, flameCount, 0.08, 0.08, 0.08, 0.01);

        if (stacks >= 4) {
            int smokeCount = 1 + stacks / 3;
            level.sendParticles(stacks >= 8 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE,
                    origin.x, origin.y, origin.z, smokeCount, 0.1, 0.12, 0.1, 0.01);
        }

        if (stacks >= MAX_STACKS) {
            level.sendParticles(ParticleTypes.LAVA, origin.x, origin.y, origin.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    private static void playExtinguishEffects(Level level, LivingEntity living) {
        level.playSound(null, living.blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 0.8f, 1.1f);

        if (level instanceof ServerLevel serverLevel) {
            Vec3 look = living.getLookAngle();
            Vec3 origin = living.position().add(0, living.getEyeHeight() * 0.55, 0).add(look.scale(0.9));
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, origin.x, origin.y, origin.z, 8, 0.15, 0.2, 0.15, 0.02);
            serverLevel.sendParticles(ParticleTypes.CLOUD, origin.x, origin.y, origin.z, 6, 0.15, 0.1, 0.15, 0.01);
        }
    }

    // ---------------------------------------------------------------
    // Shared critical-hit sound/particle burst (used by both melee hits and dash pass-throughs)
    // ---------------------------------------------------------------

    public static void playCritEffects(Level level, LivingEntity attacker, LivingEntity target) {
        level.playSound(null, attacker.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 0.9f);
        level.playSound(null, attacker.blockPosition(), SoundEvents.LAVA_POP, SoundSource.PLAYERS, 0.5f, 1.3f);

        if (level instanceof ServerLevel serverLevel) {
            double y = target.getY() + target.getBbHeight() / 2;
            serverLevel.sendParticles(ParticleTypes.CRIT, target.getX(), y, target.getZ(), 14, 0.3, 0.3, 0.3, 0.15);
            serverLevel.sendParticles(ParticleTypes.LAVA, target.getX(), y, target.getZ(), 3, 0.25, 0.25, 0.25, 0.0);
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), y, target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    // ---------------------------------------------------------------
    // Blade tint - drives the layer1 "heat overlay" texture from cool steel to white-hot
    // ---------------------------------------------------------------

    /** Registered client-side against RegisterColorHandlersEvent.Item; see Valyrainsteel.ClientModEvents. */
    public static int getColor(ItemStack stack, int tintIndex) {
        if (tintIndex != 1) return 0xFFFFFF; // layer0 (base steel) is never tinted

        float heat = Mth.clamp(getCurrentCritChance(stack) / 0.5f, 0f, 1f); // 0 = cold, 1 = max streak
        return heatColor(heat);
    }

    private static int heatColor(float heat) {
        int r, g, b;
        if (heat < 0.5f) {
            float t = heat / 0.5f;
            r = Math.round(Mth.lerp(t, 50f, 255f));
            g = Math.round(Mth.lerp(t, 50f, 90f));
            b = Math.round(Mth.lerp(t, 55f, 20f));
        } else {
            float t = (heat - 0.5f) / 0.5f;
            r = Math.round(Mth.lerp(t, 255f, 255f));
            g = Math.round(Mth.lerp(t, 90f, 235f));
            b = Math.round(Mth.lerp(t, 20f, 190f));
        }
        return (r << 16) | (g << 8) | b;
    }
}