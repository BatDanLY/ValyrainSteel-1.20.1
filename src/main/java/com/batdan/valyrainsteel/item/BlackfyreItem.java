package com.batdan.valyrainsteel.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

/**
 * Blackfyre - a Valyrian steel longsword that runs on a stored "flame" resource.
 *
 * Passive: every melee hit that lands charges the flame store a little.
 * Active: hold right-click to breathe fire from the blade in a cone in front of you, damaging
 * and igniting anything caught in it. This drains the flame store while held; once it hits 0 the
 * flame cuts out and you can't reignite it until it's built back up. The store slowly regenerates
 * on its own whenever you aren't actively breathing fire.
 */
public class BlackfyreItem extends LegendarySwordItem {

    private static final String TAG_FLAME = "BlackfyreFlame";

    public static final float MAX_FLAME = 100.0f;
    public static final float MIN_FLAME_TO_IGNITE = 5.0f;   // needed to start the active ability
    public static final float FLAME_GAIN_PER_HIT = 8.0f;    // passive charge per melee hit
    public static final float FLAME_DRAIN_PER_TICK = 2.0f;  // active drain while firing
    public static final float FLAME_REGEN_PER_TICK = 0.35f; // passive regen while not firing

    private static final float BREATH_RANGE = 6.0f;
    private static final float BREATH_DAMAGE = 1.5f;
    private static final int BREATH_FIRE_SECONDS = 3;
    private static final int DAMAGE_INTERVAL_TICKS = 4;

    public BlackfyreItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }

    // Valyrian steel never takes durability damage and cannot break
    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    // ---------------------------------------------------------------
    // Flame store
    // ---------------------------------------------------------------

    public static float getFlame(ItemStack stack) {
        return Math.min(stack.getOrCreateTag().getFloat(TAG_FLAME), MAX_FLAME);
    }

    private static void setFlame(ItemStack stack, float value) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putFloat(TAG_FLAME, Math.max(0.0f, Math.min(MAX_FLAME, value)));
    }

    /** Passive charge, called whenever Blackfyre lands a melee hit. */
    public static void addFlame(ItemStack stack, float amount) {
        setFlame(stack, getFlame(stack) + amount);
    }

    /** Passive regen, called once per tick while the weapon isn't actively breathing fire. */
    public static void regenFlame(ItemStack stack) {
        float current = getFlame(stack);
        if (current < MAX_FLAME) {
            setFlame(stack, current + FLAME_REGEN_PER_TICK);
        }
    }

    /**
     * Drains the active-use cost for one tick of breathing fire.
     * @return true if there was enough flame to pay for this tick, false if the store just ran dry
     */
    private static boolean drainForTick(ItemStack stack) {
        float current = getFlame(stack);
        if (current <= 0.0f) return false;
        setFlame(stack, current - FLAME_DRAIN_PER_TICK);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        int flame = Math.round(getFlame(stack));
        tooltip.add(Component.literal(String.format(Locale.ROOT, "Flame: %d / %d", flame, (int) MAX_FLAME))
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Hold to breathe fire from the blade").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Landing hits recharges the flame store").withStyle(ChatFormatting.DARK_GRAY));
    }

    // ---------------------------------------------------------------
    // Active: flame breath
    // ---------------------------------------------------------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (getFlame(stack) < MIN_FLAME_TO_IGNITE) {
            if (!level.isClientSide) {
                level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 0.4f, 1.8f);
            }
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000; // fires for as long as it's held and there's flame left
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(entity instanceof Player player)) return;
        if (level.isClientSide) return;

        if (!drainForTick(stack)) {
            // ran dry mid-breath: cut off immediately and snuff it out
            player.stopUsingItem();
            playExtinguish(level, player);
            return;
        }

        Vec3 origin = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        // Step out along the breath and both damage entities and lay down particles as we go,
        // so it reads as a jet of fire rather than a single hitbox check
        int steps = 6;
        for (int i = 1; i <= steps; i++) {
            double dist = BREATH_RANGE * (i / (double) steps);
            Vec3 point = origin.add(look.scale(dist));

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z, 3, 0.15, 0.15, 0.15, 0.01);
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, point.x, point.y, point.z, 1, 0.1, 0.1, 0.1, 0.01);
                // Hook: this is where a Veil Quasar "blackfyre_flame" emitter would be triggered
                // per point instead of/alongside the vanilla particles above - see the note in
                // spawnVeilFlameJet() below.
            }
        }
        spawnVeilFlameJet(level, player, origin, look);

        if (remainingUseDuration % DAMAGE_INTERVAL_TICKS == 0) {
            AABB cone = new AABB(origin, origin.add(look.scale(BREATH_RANGE)))
                    .inflate(1.4);
            List<net.minecraft.world.entity.Entity> hit = level.getEntities(player, cone,
                    e -> e instanceof LivingEntity && e != player && e.isAlive());

            for (net.minecraft.world.entity.Entity e : hit) {
                LivingEntity living = (LivingEntity) e;
                living.hurt(player.damageSources().playerAttack(player), BREATH_DAMAGE);
                living.setSecondsOnFire(BREATH_FIRE_SECONDS);
            }
        }

        if (remainingUseDuration % 10 == 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_BURN, SoundSource.PLAYERS, 0.6f, 1.0f);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide) return;
        if (entity instanceof Player player) {
            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 0.5f, 1.4f);
        }
    }

    private static void playExtinguish(Level level, Player player) {
        if (level instanceof ServerLevel serverLevel) {
            Vec3 pos = player.getEyePosition().add(player.getLookAngle().scale(1.2));
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 12, 0.2, 0.2, 0.2, 0.02);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 0.8f, 1.1f);
    }

    /**
     * Integration point for the Veil particle library. Veil's Quasar particles are JSON-defined
     * emitters (assets/valyrainsteel/quasar/...) rather than a simple spawn(x,y,z) call, and the
     * exact Java entry point can shift between Veil versions, so wire this up against whatever
     * Veil build you're on rather than trusting a guessed signature from here. Roughly, you'd:
     *   1. Author a particle emitter JSON at assets/valyrainsteel/quasar/blackfyre_flame.json
     *      (lifetime, emission rate, a Shape module for a cone/line, and modules for color,
     *      velocity, and size-over-life to get that jet-of-fire look).
     *   2. Look up Veil's particle manager (check the Quasar Particle System page on the Veil
     *      wiki/DeepWiki for your installed version) and call its "spawn emitter at position/
     *      direction" method here, once per tick this method runs.
     * Until that's wired in, the vanilla FLAME/LARGE_SMOKE particles above already give a working
     * (if less flashy) flamethrower effect, so nothing is broken by leaving this as a no-op.
     */
    private static void spawnVeilFlameJet(Level level, Player player, Vec3 origin, Vec3 direction) {
        // Intentionally left as an integration point - see the javadoc above.
    }
}