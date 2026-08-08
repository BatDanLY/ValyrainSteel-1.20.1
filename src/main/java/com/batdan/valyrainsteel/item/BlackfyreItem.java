package com.batdan.valyrainsteel.item;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import team.lodestar.lodestone.registry.common.particle.LodestoneParticleRegistry;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
public class BlackfyreItem extends LegendarySwordItem implements GeoItem {

    private static final String TAG_FLAME = "BlackfyreFlame";

    public static final float MAX_FLAME = 200.0f;
    public static final float MIN_FLAME_TO_IGNITE = 8.0f;   // needed to start the active ability
    public static final float FLAME_GAIN_PER_HIT = 10.0f;   // the ONLY source of flame: landed melee hits
    public static final float FLAME_DRAIN_PER_TICK = 1f;  // active drain while firing (~4-5s from a full tank)

    // --- the two shared knobs: both the particles and the hit-cone are built from these ---
    private static final float RANGE = 13.0f;
    private static final float SPREAD_DEG = 14.0f;

    // Small practical floor so the hit-area (and the impact billow) isn't literally zero-sized
    // right at the blade. Not a "spread" concept, just keeps the very start of the cone usable.
    private static final float MIN_RADIUS = 0.35f;

    private static final float STREAM_DAMAGE = 5;
    private static final int FIRE_SECONDS = 5;
    private static final double KNOCKBACK_STRENGTH = 0.2;

    // --- traveling hit-area ("pulse") tuning ---
    private static final int PULSE_SPAWN_INTERVAL_TICKS = 2; // how often a fresh area spawns
    private static final double PULSE_SPEED = 1.5;           // blocks/tick it travels outward

    // --- particle tuning (purely cosmetic, client-side) ---
    private static final int STREAM_PARTICLES_PER_TICK = 18;
    private static final double NOZZLE_JITTER = 0.05; // how tight the spawn point is
    private static final double STREAM_PARTICLE_MIN_SPEED = 0.85;
    private static final double STREAM_PARTICLE_MAX_SPEED = 1.6;
    private static final int STREAM_PARTICLE_MIN_LIFE = 10;
    private static final int STREAM_PARTICLE_MAX_LIFE = 22;
    private static final double STREAM_GRAVITY = 0.015; // downward tug applied to every stream particle
    private static final int IMPACT_PARTICLES_PER_TICK = 4;

    private static final Color FLAME_COLOR_START = new Color(255, 120, 20);
    private static final Color FLAME_COLOR_END = new Color(130, 40, 5);
    private static final Color SMOKE_COLOR_START = new Color(90, 90, 90);
    private static final Color SMOKE_COLOR_END = new Color(20, 20, 20);

    // Server-side only: each player's chain of currently-traveling hit-areas.
    private static final Map<Integer, List<Pulse>> ACTIVE_PULSES = new HashMap<>();

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private static final class Pulse {
        final Vec3 origin;
        final Vec3 dir;
        final Set<Integer> hitEntities = new HashSet<>();
        int age;

        Pulse(Vec3 origin, Vec3 dir) {
            this.origin = origin;
            this.dir = dir;
        }
    }

    public BlackfyreItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }

    public static float getFlame(ItemStack stack) {
        return Math.min(stack.getOrCreateTag().getFloat(TAG_FLAME), MAX_FLAME);
    }

    private static void setFlame(ItemStack stack, float value) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putFloat(TAG_FLAME, Math.max(0.0f, Math.min(MAX_FLAME, value)));
    }

    /** The only source of flame: called whenever Blackfyre lands a melee hit. */
    public static void addFlame(ItemStack stack, float amount) {
        setFlame(stack, getFlame(stack) + amount);
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

    // ---------------------------------------------------------------
    // Ambient: blade glow while simply holding the sword
    // ---------------------------------------------------------------

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!level.isClientSide) return;
        if (!isSelected) return;
        if (!(entity instanceof Player player)) return;
        if (player.isUsingItem()) return; // the firing stream already covers this

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != player) return; // only render this for the controlling client
        if (!mc.options.getCameraType().isFirstPerson()) return;

        tickBladeGlow(level, player, stack);
    }

    private static void tickBladeGlow(Level level, Player player, ItemStack stack) {
        float fraction = getFlame(stack) / MAX_FLAME;
        if (fraction <= 0f) return;

        RandomSource random = level.getRandom();
        // spawns more often, and more embers at once, the fuller the tank is
        if (random.nextFloat() > 0.12f + fraction * 0.45f) return;

        Vec3 lookDir = player.getLookAngle();
        Vec3 right = lookDir.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0E-4) right = lookDir.cross(new Vec3(1, 0, 0));
        right = right.normalize();
        Vec3 up = right.cross(lookDir).normalize();

        // rough position of the blade as it sits in a typical first-person viewmodel
        Vec3 bladePos = player.getEyePosition()
                .add(lookDir.scale(0.55))
                .add(right.scale(0.55))
                .add(up.scale(-0.09));

        int embers = 1 + Math.round(fraction * 4);
        for (int i = 0; i < embers; i++) {
            double px = bladePos.x + (random.nextDouble() - 0.5) * 0.1;
            double py = bladePos.y + (random.nextDouble() - 0.5) * 0.1;
            double pz = bladePos.z + (random.nextDouble() - 0.5) * 0.1;

            double dx = (random.nextDouble() - 0.5) * 0.01;
            double dy = 0.04 + random.nextDouble() * 0.015;
            double dz = (random.nextDouble() - 0.5) * 0.01;

            WorldParticleBuilder.create(LodestoneParticleRegistry.SMOKE_PARTICLE)
                    .setLifetime(10 + random.nextInt(6))
                    .setScaleData(GenericParticleData.create(0.08f + fraction * 0.06f, 0.02f).build())
                    .setTransparencyData(GenericParticleData.create(0.85f, 0f).build())
                    .setColorData(ColorParticleData.create(FLAME_COLOR_START, FLAME_COLOR_END).build())
                    .setSpinData(SpinParticleData.create(0f, 0.1f).build())
                    .setMotion(dx, dy, dz)
                    .enableNoClip()
                    .spawn(level, px, py, pz);
        }
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

    /** Server-side: costs flame, spawns/advances/resolves the traveling hit-areas. No visuals here. */
    private void tickServerLogic(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        if (!drainForTick(stack)) {
            // ran dry mid-stream: cut off immediately and snuff it out
            player.stopUsingItem();
            ACTIVE_PULSES.remove(player.getId());
            playExtinguish(level, player);
            return;
        }

        List<Pulse> pulses = ACTIVE_PULSES.computeIfAbsent(player.getId(), id -> new ArrayList<>());

        // spawn a fresh area every couple of ticks, at the blade, aimed wherever the player is
        // looking right now
        if (remainingUseDuration % PULSE_SPAWN_INTERVAL_TICKS == 0) {
            Vec3 pulseOrigin = computeOrigin(player, player.yBodyRot);
            pulses.add(new Pulse(pulseOrigin, player.getLookAngle()));
        }

        double spreadTan = Math.tan(Math.toRadians(SPREAD_DEG));

        Iterator<Pulse> it = pulses.iterator();
        while (it.hasNext()) {
            Pulse pulse = it.next();
            pulse.age++;

            double distance = pulse.age * PULSE_SPEED;
            if (distance > RANGE) {
                it.remove();
                continue;
            }

            // true cone geometry: radius grows in lockstep with SPREAD_DEG, same angle the
            // particles jitter within, so the visible cone and the hit-area match
            float radius = MIN_RADIUS + (float) (distance * spreadTan);
            Vec3 center = pulse.origin.add(pulse.dir.scale(distance));

            AABB searchBox = new AABB(center, center).inflate(radius);
            List<Entity> nearby = level.getEntities(player, searchBox,
                    e -> e instanceof LivingEntity && e != player && e.isAlive());

            for (Entity e : nearby) {
                if (pulse.hitEntities.contains(e.getId())) continue;

                double dist = e.position().add(0, e.getBbHeight() * 0.5, 0).distanceTo(center);
                if (dist > radius) continue;

                LivingEntity living = (LivingEntity) e;
                living.hurt(player.damageSources().playerAttack(player), STREAM_DAMAGE);
                living.setSecondsOnFire(FIRE_SECONDS);
                living.knockback(KNOCKBACK_STRENGTH, player.getX() - living.getX(), player.getZ() - living.getZ());
                pulse.hitEntities.add(e.getId());
            }
        }

        if (remainingUseDuration % 10 == 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_BURN, SoundSource.PLAYERS, 0.6f, 1.0f);
        }
    }

    /** Client-side: purely cosmetic. Spawns at the blade tip, flies toward where the player is looking. */
    private static void tickClientVisuals(Level level, Player player) {
        Vec3 origin = computeOrigin(player, player.yBodyRot);

        float lookYaw = player.getYRot();
        float lookPitch = player.getXRot();
        Vec3 lookDir = player.getLookAngle();

        RandomSource random = level.getRandom();

        // --- fire the stream: spawn AT the blade, but the velocity points where you're looking ---
        for (int i = 0; i < STREAM_PARTICLES_PER_TICK; i++) {
            float particleYaw = lookYaw + (float) ((random.nextDouble() - 0.5) * 2.0 * SPREAD_DEG);
            float particlePitch = lookPitch + (float) ((random.nextDouble() - 0.5) * 2.0 * SPREAD_DEG);
            Vec3 particleDir = calculateViewVector(particlePitch, particleYaw);

            double speed = STREAM_PARTICLE_MIN_SPEED + random.nextDouble() * (STREAM_PARTICLE_MAX_SPEED - STREAM_PARTICLE_MIN_SPEED);
            Vec3 motion = particleDir.scale(speed).add(0, -STREAM_GRAVITY, 0);

            double px = origin.x + (random.nextDouble() - 0.5) * NOZZLE_JITTER;
            double py = origin.y + (random.nextDouble() - 0.5) * NOZZLE_JITTER;
            double pz = origin.z + (random.nextDouble() - 0.5) * NOZZLE_JITTER;

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
        Vec3 impactCenter = origin.add(lookDir.scale(RANGE));
        Vec3 right = lookDir.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0E-4) {
            right = lookDir.cross(new Vec3(1, 0, 0));
        }
        right = right.normalize();
        Vec3 up = right.cross(lookDir).normalize();
        float impactRadius = MIN_RADIUS + (float) (RANGE * Math.tan(Math.toRadians(SPREAD_DEG)));

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

    /** Blade-tip nozzle position: body rotation + swinging hand, not head look. */
    private static Vec3 computeOrigin(Player player, float bodyYaw) {
        InteractionHand usedHand = player.getUsedItemHand();
        boolean isMainHandRight = player.getMainArm() == HumanoidArm.RIGHT;
        boolean usingMainHand = usedHand == InteractionHand.MAIN_HAND;
        double handSide = (usingMainHand == isMainHandRight) ? 1.0 : -1.0;

        double yawRad = Math.toRadians(bodyYaw);
        Vec3 bodyForward = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3 bodyRight = new Vec3(bodyForward.z, 0, -bodyForward.x);

        return new Vec3(player.getX(), player.getY() + player.getEyeHeight() * 0.82, player.getZ())
                .add(bodyForward.scale(0.9))
                .add(bodyRight.scale(-0.4 * handSide));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide) return;

        ACTIVE_PULSES.remove(entity.getId());
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

    /** Same math as vanilla's Entity#calculateViewVector, exposed so we can feed it jittered angles. */
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