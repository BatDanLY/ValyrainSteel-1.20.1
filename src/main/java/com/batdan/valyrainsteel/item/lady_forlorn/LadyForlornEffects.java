package com.batdan.valyrainsteel.item.lady_forlorn;

import com.batdan.valyrainsteel.registry.ModSounds;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import team.lodestar.lodestone.registry.common.particle.LodestoneParticleRegistry;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData;

import java.awt.Color;
import java.util.Random;

/*
 * Confirmed against Lodestone's own wiki (github.com/LodestarMC/Lodestone/wiki/Particles):
 *   WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
 *       .setScaleData(GenericParticleData.create(...).build())
 *       .setColorData(ColorParticleData.create(Color, Color).build())   <- java.awt.Color, not Vector3f
 *       .setTransparencyData(GenericParticleData.create(...).build())
 *       .setSpinData(SpinParticleData.create(...).build())
 *       .setLifetime(int)
 *       .addMotion(x, y, z) / .setMotion(x, y, z)
 *       .enableNoClip()
 *       .spawn(level, x, y, z);
 *
 * WISP_PARTICLE is a pre-built, generic soft-glow particle Lodestone ships out of the box - no
 * custom ParticleType registration, particle JSON, or texture needed. If you'd like a different
 * shape (a streak/spark look instead of a soft blob), check LodestoneParticleRegistry in your IDE
 * for the other constants it ships (e.g. SPARKLE_PARTICLE) and swap the reference below; I've stuck
 * to WISP_PARTICLE here since it's the one actually confirmed in Lodestone's docs.
 */
public final class LadyForlornEffects {

    private static final Random RANDOM = new Random();

    private static final Color WIND_COLOR_START = new Color(108, 8, 31);
    private static final Color WIND_COLOR_END = new Color(255, 88, 255);
    private static final Color IMPACT_COLOR_START = new Color(108, 8, 31);
    private static final Color IMPACT_COLOR_END = new Color(255, 88, 255);

    private LadyForlornEffects() {
    }

    // ================= ACTIVE ABILITY: CHARGE =================

    public static void playChargeStartSound(Level level, Player player) {
        level.playSound(player, player.blockPosition(), ModSounds.LADY_FORLORN_CHARGE.get(),
                SoundSource.PLAYERS, 0.6F, 0.9F);
    }

    /** Called every client tick while charging - swirls air particles in around the blade. */
    public static void spawnChargeParticles(Level level, Player player, float chargeFraction) {
        Vec3 handPos = getBladeTipPosition(player);

        int particlesThisTick = 1 + Math.round(chargeFraction * 2);
        for (int i = 0; i < particlesThisTick; i++) {
            // Spawn on a shrinking ring around the blade tip and give it inward motion, so it reads
            // as air being pulled INTO the sword rather than emitted from it.
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double radius = Mth.lerp(RANDOM.nextDouble(), 0.4, 1.0) * (1.0 - 0.5 * chargeFraction);

            double px = handPos.x + Math.cos(angle) * radius;
            double py = handPos.y + (RANDOM.nextDouble() - 0.5) * 0.6;
            double pz = handPos.z + (Math.sin(angle) * radius);


            Vec3 inward = handPos.subtract(px, py, pz).normalize().scale(0.08 + 0.1 * chargeFraction);

            WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                    .setLifetime(6 + RANDOM.nextInt(6))
                    .setScaleData(GenericParticleData.create(0.15F, 0.05F).build())
                    .setTransparencyData(GenericParticleData.create(0.8F, 0F).build())
                    .setColorData(ColorParticleData.create(WIND_COLOR_START, WIND_COLOR_END).build())
                    .setSpinData(SpinParticleData.create(0F, 0.15F).build())
                    .setMotion(inward.x, inward.y, inward.z)
                    .enableNoClip()
                    .spawn(level, px, py, pz);
        }
    }

    // ================= ACTIVE ABILITY: RELEASE =================

    public static void spawnReleaseVisuals(Level level, Player player, BlockHitResult hit, float chargeFraction) {
        if (WindBurstHandler.isWallHit(hit)) {
            spawnWallBurst(level, hit, chargeFraction);
        } else {
            spawnConeBurst(level, player, chargeFraction);
        }

        level.playSound(player, player.blockPosition(), ModSounds.LADY_FORLORN_RELEASE.get(),
                SoundSource.PLAYERS, 1.0F, 1.1F - chargeFraction * 0.2F);
    }

    private static void spawnConeBurst(Level level, Player player, float chargeFraction) {
        Vec3 origin = getBladeTipPosition(player);
        Vec3 look = player.getLookAngle();

        int count = 16 + Math.round(chargeFraction * 24);
        for (int i = 0; i < count; i++) {
            // Scatter the initial velocity in a cone around the look vector.
            Vec3 spread = randomInCone(look, 25.0);
            double speed = 0.5 + RANDOM.nextDouble() * 0.6 * (0.5 + chargeFraction);
            Vec3 motion = spread.scale(speed);

            WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                    .setLifetime(8 + RANDOM.nextInt(8))
                    .setScaleData(GenericParticleData.create(0.25F + chargeFraction * 0.3F, 0.1F).build())
                    .setTransparencyData(GenericParticleData.create(0.9F, 0F).build())
                    .setColorData(ColorParticleData.create(WIND_COLOR_START, WIND_COLOR_END).build())
                    .setRandomOffset(0.25F)
                    .setMotion(motion.x, motion.y, motion.z)
                    .enableNoClip()
                    .spawn(level, origin.x, origin.y, origin.z);
        }
    }

    private static void spawnWallBurst(Level level, BlockHitResult hit, float chargeFraction) {
        Vec3 pos = hit.getLocation();
        Direction face = hit.getDirection();
        Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());

        int count = 20 + Math.round(chargeFraction * 20);
        for (int i = 0; i < count; i++) {
            Vec3 spread = randomInCone(normal, 55.0);
            double speed = 0.4 + RANDOM.nextDouble() * 0.5 * (0.5 + chargeFraction);
            Vec3 motion = spread.scale(speed);

            WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                    .setLifetime(10 + RANDOM.nextInt(8))
                    .setScaleData(GenericParticleData.create(0.3F + chargeFraction * 0.3F, 0.1F).build())
                    .setTransparencyData(GenericParticleData.create(0.9F, 0F).build())
                    .setColorData(ColorParticleData.create(IMPACT_COLOR_START, IMPACT_COLOR_END).build())
                    .setRandomOffset(0.3F)
                    .setMotion(motion.x, motion.y, motion.z)
                    .enableNoClip()
                    .spawn(level, pos.x + normal.x * 0.1, pos.y + normal.y * 0.1, pos.z + normal.z * 0.1);
        }
    }

    // ================= PASSIVE: SOFT LANDING =================

    /**
     * Called from the server-side LivingFallEvent handler, so unlike everything above this needs an
     * actual network broadcast to be visible to everyone - plain vanilla particles keep that simple
     * (ServerLevel#sendParticles broadcasts on its own; a custom Lodestone-configured particle would
     * need its own synced ParticleOptions/packet to do the same, which is out of scope here).
     */
    public static void spawnLandingEffects(LivingEntity entity) {
        if (!(entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                entity.getX(), entity.getY() + 0.1, entity.getZ(),
                14, 0.4, 0.05, 0.4, 0.02);

        serverLevel.playSound(null, entity.blockPosition(), ModSounds.LADY_FORLORN_LAND.get(),
                SoundSource.PLAYERS, 0.5F, 1.3F);
    }

    // ================= helpers =================

    private static Vec3 getBladeTipPosition(Player player) {
        Vec3 eye = player.getEyePosition();

        // Forward direction
        Vec3 forward = player.getLookAngle().normalize();

        // Right vector (horizontal)
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();

        // Up vector
        Vec3 up = new Vec3(0, 1, 0);

        return eye
                .add(up.scale(0.15))      // slightly below eye level
                .add(right.scale(0.35))   // move to player's right shoulder
                .add(forward.scale(0.15)); // slightly forward
    }

    /** Random unit vector within maxAngleDegrees of the given axis - used to fan particles into a cone. */
    private static Vec3 randomInCone(Vec3 axis, double maxAngleDegrees) {
        double maxAngleRad = Math.toRadians(maxAngleDegrees);
        double cosAngle = 1.0 - RANDOM.nextDouble() * (1.0 - Math.cos(maxAngleRad));
        double sinAngle = Math.sqrt(1.0 - cosAngle * cosAngle);
        double phi = RANDOM.nextDouble() * Math.PI * 2;

        // Build an arbitrary basis perpendicular to axis.
        Vec3 up = Math.abs(axis.y) < 0.99 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 tangentA = axis.cross(up).normalize();
        Vec3 tangentB = axis.cross(tangentA).normalize();

        Vec3 offset = tangentA.scale(Math.cos(phi) * sinAngle).add(tangentB.scale(Math.sin(phi) * sinAngle));
        return axis.scale(cosAngle).add(offset).normalize();
    }
}