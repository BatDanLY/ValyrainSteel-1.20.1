package com.batdan.valyrainsteel.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import team.lodestar.lodestone.registry.common.particle.LodestoneParticleRegistry;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Blackfyre - a Valyrian steel longsword that runs on a stored "flame" resource.
 *
 * Passive: every melee hit that lands charges the flame store a little.
 * Active: hold right-click to unleash a continuous jet of dragonfire from the blade - a real
 * flamethrower. Particles spawn at the nozzle (the blade tip) with a random angle and speed, then
 * just fly outward - the cone shape is a straight result of those trajectories diverging, not
 * something faked by placing particles along a path.
 *
 * IMPORTANT SPLIT: Lodestone's WorldParticleBuilder is a client-side renderer and does not network
 * itself to nearby players the way vanilla's ServerLevel#sendParticles does. So this tick is split:
 *   - server: drains the flame store and does the (invisible) damage/ignite cone check
 *   - client: purely cosmetic, spawns the actual particles, using its own local aim smoothing
 * If you were previously only seeing nothing at all, this split is almost certainly why - a
 * server-only call to WorldParticleBuilder never reaches any client to render.
 *
 * The nozzle position is built off the player's BODY rotation and swinging hand (not head look),
 * so the fire visibly comes from the sword rather than snapping to wherever the camera points.
 * The stream direction also eases toward the player's actual facing every tick rather than
 * snapping instantly, so whipping the camera drags the jet behind it.
 */
public class BlackfyreItem extends LegendarySwordItem {

    private static final String TAG_FLAME = "BlackfyreFlame";
    private static final String TAG_AIM_YAW = "BlackfyreAimYaw";
    private static final String TAG_AIM_PITCH = "BlackfyreAimPitch";

    public static final float MAX_FLAME = 100.0f;
    public static final float MIN_FLAME_TO_IGNITE = 8.0f;   // needed to start the active ability
    public static final float FLAME_GAIN_PER_HIT = 10.0f;   // passive charge per melee hit
    public static final float FLAME_DRAIN_PER_TICK = 1.2f;  // active drain while firing (~4-5s from a full tank)
    public static final float FLAME_REGEN_PER_TICK = 0.3f;  // passive regen while not firing

    // How far the jet reaches and how wide the emission cone is (half-angle, in degrees).
    private static final float STREAM_RANGE = 13.0f;
    private static final float STREAM_HALF_ANGLE_DEG = 9.0f;

    private static final float STREAM_DAMAGE = 2.5f;
    private static final int DAMAGE_INTERVAL_TICKS = 4;
    private static final int FIRE_SECONDS = 5;

    // How quickly the effective stream direction catches up to where the player is actually
    // facing. Lower = laggier, heavier-feeling stream. 1.0 would mean no smoothing at all.
    private static final float AIM_SMOOTHING = 0.15f;

    // Particle tuning. Speed is in blocks/tick; lifetime is in ticks, so speed * lifetime is
    // roughly how far a given particle travels before it dies - keep that near STREAM_RANGE.
    private static final int STREAM_PARTICLES_PER_TICK = 18;
    private static final double STREAM_PARTICLE_MIN_SPEED = 0.85;
    private static final double STREAM_PARTICLE_MAX_SPEED = 1.2;
    private static final int STREAM_PARTICLE_MIN_LIFE = 10;
    private static final int STREAM_PARTICLE_MAX_LIFE = 18;
    private static final double STREAM_GRAVITY = 0.015; // downward tug applied to every stream particle

    private static final int IMPACT_PARTICLES_PER_TICK = 4;

    private static final Color FLAME_COLOR_START = new Color(255, 205, 90);
    private static final Color FLAME_COLOR_END = new Color(110, 35, 10);
    private static final Color SMOKE_COLOR_START = new Color(90, 90, 90);
    private static final Color SMOKE_COLOR_END = new Color(20, 20, 20);

    // Client-only local aim smoothing cache, keyed by entity id. Deliberately NOT stored in the
    // ItemStack's NBT, because that's written by the server and won't be synced back down every
    // single tick - reading it client-side would be laggy/stale. Only ever touched when
    // level.isClientSide is true.
    private static final Map<Integer, float[]> CLIENT_AIM = new HashMap<>();

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

    /** Passive regen, called once per tick while the weapon isn't actively firing. */
    public static void regenFlame(ItemStack stack) {
        float current = getFlame(stack);
        if (current < MAX_FLAME) {
            setFlame(stack, current + FLAME_REGEN_PER_TICK);
        }
    }

    /**
     * Drains the active-use cost for one tick of firing.
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
        tooltip.add(Component.literal("Hold to unleash a jet of dragonfire").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Landing hits recharges the flame store").withStyle(ChatFormatting.DARK_GRAY));
    }

    // ---------------------------------------------------------------
    // Active: flamethrower stream
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

        // Snap whichever side's aim tracker to the player's current facing, so the stream
        // doesn't have to "catch up" from zero the instant the trigger is held.
        if (level.isClientSide) {
            CLIENT_AIM.put(player.getId(), new float[] { player.yBodyRot, player.getXRot() });
        } else {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putFloat(TAG_AIM_YAW, player.yBodyRot);
            tag.putFloat(TAG_AIM_PITCH, player.getXRot());
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

        if (level.isClientSide) {
            tickClientVisuals(level, player);
        } else {
            tickServerLogic(level, player, stack, remainingUseDuration);
        }
    }

    /** Server-side: costs flame, checks the cone for entities to damage/ignite. No visuals here. */
    private void tickServerLogic(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        if (!drainForTick(stack)) {
            // ran dry mid-stream: cut off immediately and snuff it out
            player.stopUsingItem();
            playExtinguish(level, player);
            return;
        }

        float aimYaw = updateSmoothedAngle(stack, TAG_AIM_YAW, player.yBodyRot, AIM_SMOOTHING);
        float aimPitch = updateSmoothedAngle(stack, TAG_AIM_PITCH, player.getXRot(), AIM_SMOOTHING);
        Vec3 dir = calculateViewVector(aimPitch, aimYaw);
        Vec3 origin = computeOrigin(player, aimYaw, dir);

        if (remainingUseDuration % DAMAGE_INTERVAL_TICKS == 0) {
            applyStreamDamage(level, player, origin, dir);
        }

        if (remainingUseDuration % 10 == 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_BURN, SoundSource.PLAYERS, 0.6f, 1.0f);
        }
    }

    /** Client-side: purely cosmetic. Spawns the actual flame + smoke particles. */
    private static void tickClientVisuals(Level level, Player player) {
        float[] aim = CLIENT_AIM.computeIfAbsent(player.getId(), id -> new float[] { player.yBodyRot, player.getXRot() });
        aim[0] += Mth.wrapDegrees(player.yBodyRot - aim[0]) * AIM_SMOOTHING;
        aim[1] += Mth.wrapDegrees(player.getXRot() - aim[1]) * AIM_SMOOTHING;
        float aimYaw = aim[0];
        float aimPitch = aim[1];

        Vec3 dir = calculateViewVector(aimPitch, aimYaw);
        Vec3 origin = computeOrigin(player, aimYaw, dir);
        RandomSource random = level.getRandom();

        // --- fire the stream: spawn AT the nozzle with an initial velocity and let it fly ---
        for (int i = 0; i < STREAM_PARTICLES_PER_TICK; i++) {
            float particleYaw = aimYaw + (float) ((random.nextDouble() - 0.5) * 2.0 * STREAM_HALF_ANGLE_DEG);
            float particlePitch = aimPitch + (float) ((random.nextDouble() - 0.5) * 2.0 * STREAM_HALF_ANGLE_DEG);
            Vec3 particleDir = calculateViewVector(particlePitch, particleYaw);

            double speed = STREAM_PARTICLE_MIN_SPEED + random.nextDouble() * (STREAM_PARTICLE_MAX_SPEED - STREAM_PARTICLE_MIN_SPEED);
            Vec3 motion = particleDir.scale(speed).add(0, -STREAM_GRAVITY, 0);

            double px = origin.x + (random.nextDouble() - 0.5) * 0.15;
            double py = origin.y + (random.nextDouble() - 0.5) * 0.15;
            double pz = origin.z + (random.nextDouble() - 0.5) * 0.15;

            int lifetime = STREAM_PARTICLE_MIN_LIFE + random.nextInt(STREAM_PARTICLE_MAX_LIFE - STREAM_PARTICLE_MIN_LIFE);

            WorldParticleBuilder.create(LodestoneParticleRegistry.SMOKE_PARTICLE)
                    .setLifetime(lifetime)
                    .setScaleData(GenericParticleData.create(0.45f, 0.05f).build())
                    .setTransparencyData(GenericParticleData.create(0.9f, 0.0f).build())
                    .setColorData(ColorParticleData.create(FLAME_COLOR_START, FLAME_COLOR_END).build())
                    .setSpinData(SpinParticleData.create(0f, 0.15f).build())
                    .setMotion(motion.x, motion.y, motion.z)
                    .enableNoClip()
                    .spawn(level, px, py, pz);
        }

        // --- extra smoke billowing where the jet reaches the end of its range ---
        Vec3 impactCenter = origin.add(dir.scale(STREAM_RANGE));
        Vec3 right = dir.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0E-4) {
            right = dir.cross(new Vec3(1, 0, 0));
        }
        right = right.normalize();
        Vec3 up = right.cross(dir).normalize();
        float impactRadius = STREAM_RANGE * (float) Math.tan(Math.toRadians(STREAM_HALF_ANGLE_DEG));

        for (int i = 0; i < IMPACT_PARTICLES_PER_TICK; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = Math.sqrt(random.nextDouble()) * impactRadius;
            Vec3 offset = right.scale(Math.cos(angle) * radius).add(up.scale(Math.sin(angle) * radius));

            double px = impactCenter.x + offset.x;
            double py = impactCenter.y + offset.y;
            double pz = impactCenter.z + offset.z;

            Vec3 billow = new Vec3(random.nextDouble() - 0.5, random.nextDouble() * 0.6, random.nextDouble() - 0.5);
            if (billow.lengthSqr() > 1.0E-6) {
                billow = billow.normalize().scale(0.05 + random.nextDouble() * 0.05);
            }

            WorldParticleBuilder.create(LodestoneParticleRegistry.SMOKE_PARTICLE)
                    .setLifetime(14 + random.nextInt(16))
                    .setScaleData(GenericParticleData.create(0.55f, 0.05f).build())
                    .setTransparencyData(GenericParticleData.create(0.7f, 0f).build())
                    .setColorData(ColorParticleData.create(SMOKE_COLOR_START, SMOKE_COLOR_END).build())
                    .setSpinData(SpinParticleData.create(0f, 0.1f).build())
                    .setMotion(billow.x, billow.y, billow.z)
                    .enableNoClip()
                    .spawn(level, px, py, pz);
        }
    }

    private static void applyStreamDamage(Level level, Player player, Vec3 origin, Vec3 dir) {
        double halfAngleCos = Math.cos(Math.toRadians(STREAM_HALF_ANGLE_DEG));
        AABB searchBox = new AABB(origin, origin).inflate(STREAM_RANGE);

        List<Entity> nearby = level.getEntities(player, searchBox,
                e -> e instanceof LivingEntity && e != player && e.isAlive());

        for (Entity e : nearby) {
            Vec3 toEntity = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(origin);
            double distance = toEntity.length();
            if (distance < 0.001 || distance > STREAM_RANGE) continue;

            double cos = toEntity.scale(1.0 / distance).dot(dir);
            if (cos < halfAngleCos) continue;

            LivingEntity living = (LivingEntity) e;
            living.hurt(player.damageSources().playerAttack(player), STREAM_DAMAGE);
            living.setSecondsOnFire(FIRE_SECONDS);
        }
    }

    /** Blade-tip nozzle position: body rotation + swinging hand, not head look. */
    private static Vec3 computeOrigin(Player player, float aimYaw, Vec3 dir) {
        InteractionHand usedHand = player.getUsedItemHand();
        boolean isMainHandRight = player.getMainArm() == HumanoidArm.RIGHT;
        boolean usingMainHand = usedHand == InteractionHand.MAIN_HAND;
        double handSide = (usingMainHand == isMainHandRight) ? 1.0 : -1.0;

        double yawRad = Math.toRadians(aimYaw);
        Vec3 bodyForward = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3 bodyRight = new Vec3(bodyForward.z, 0, -bodyForward.x);

        return new Vec3(player.getX(), player.getY() + player.getEyeHeight() * 0.82, player.getZ())
                .add(bodyForward.scale(0.9))
                .add(bodyRight.scale(0.4 * handSide))
                .add(dir.scale(0.6));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide) {
            CLIENT_AIM.remove(entity.getId());
            return;
        }
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

    // ---------------------------------------------------------------
    // Aim smoothing / math helpers
    // ---------------------------------------------------------------

    /**
     * Moves the stored angle at {@code tagKey} a fraction of the way toward {@code target},
     * wrapping correctly across the 180/-180 boundary, and returns the new value. Server-side only.
     */
    private static float updateSmoothedAngle(ItemStack stack, String tagKey, float target, float smoothing) {
        CompoundTag tag = stack.getOrCreateTag();
        float current = tag.getFloat(tagKey);
        float delta = Mth.wrapDegrees(target - current);
        float next = current + delta * smoothing;
        tag.putFloat(tagKey, next);
        return next;
    }

    /** Same math as vanilla's Entity#calculateViewVector, exposed so we can feed it smoothed angles. */
    private static Vec3 calculateViewVector(float pitch, float yaw) {
        float f = pitch * ((float) Math.PI / 180F);
        float f1 = -yaw * ((float) Math.PI / 180F);
        float f2 = Mth.cos(f1);
        float f3 = Mth.sin(f1);
        float f4 = Mth.cos(f);
        float f5 = Mth.sin(f);
        return new Vec3(f3 * f4, -f5, f2 * f4);
    }
}