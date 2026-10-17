package com.batdan.valyrainsteel.item.lady_forlorn;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class WindBurstHandler {

    private static final double WALL_CHECK_RANGE = 4.5;

    private static final double CONE_RANGE_MIN = 6.0;
    private static final double CONE_RANGE_MAX = 10.0;
    private static final double CONE_HALF_ANGLE_DEGREES = 40.0;
    private static final double CONE_BASE_POWER = 12.0;
    private static final double CONE_UPWARD_BOOST = 3.5;

    private static final double WALL_BASE_POWER = 12.0;
    private static final double WALL_UPWARD_BOOST = 4.0;

    private static final double RECOIL_STRENGTH = 0.12;

    private WindBurstHandler() {
    }

    public static BlockHitResult traceLookHit(Level level, Player player) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 traceEnd = eyePos.add(player.getLookAngle().scale(WALL_CHECK_RANGE));
        return level.clip(new ClipContext(eyePos, traceEnd,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
    }

    public static boolean isWallHit(BlockHitResult hit) {
        return hit.getType() == HitResult.Type.BLOCK;
    }

    public static void applyPhysics(ServerLevel level, ServerPlayer player, BlockHitResult hit, float chargeFraction) {
        chargeFraction = Mth.clamp(chargeFraction, 0F, 1F);

        if (isWallHit(hit)) {
            wallBounce(player, hit, chargeFraction);
        } else {
            coneBlast(level, player, chargeFraction);
        }

        Vec3 look = player.getLookAngle();
        Vec3 current = player.getDeltaMovement();
        player.setDeltaMovement(
                current.x - look.x * RECOIL_STRENGTH * chargeFraction,
                current.y,
                current.z - look.z * RECOIL_STRENGTH * chargeFraction
        );
        player.hurtMarked = true;
    }

    private static void wallBounce(ServerPlayer player, BlockHitResult hit, float chargeFraction) {
        Vec3 hitPos = hit.getLocation();
        Vec3 eyePos = player.getEyePosition();
        double distance = eyePos.distanceTo(hitPos);
        double proximity = 1.0 - Mth.clamp(distance / WALL_CHECK_RANGE, 0.0, 1.0);

        Vec3 away = eyePos.subtract(hitPos);
        if (away.lengthSqr() < 1.0E-6) {
            away = player.getLookAngle().scale(-1);
        }
        away = away.normalize();

        double strength = WALL_BASE_POWER * chargeFraction * (0.4 + 0.6 * proximity);

        Vec3 current = player.getDeltaMovement();
        Vec3 newVelocity = new Vec3(
                current.x * 0.2 + away.x * strength,
                Math.max(current.y, 0) + WALL_UPWARD_BOOST * chargeFraction * (0.5 + proximity),
                current.z * 0.2 + away.z * strength
        );

        player.setDeltaMovement(newVelocity);
        player.hurtMarked = true;
        player.fallDistance = 0;
    }

    private static void coneBlast(ServerLevel level, ServerPlayer player, float chargeFraction) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        double range = Mth.lerp(chargeFraction, CONE_RANGE_MIN, CONE_RANGE_MAX);
        double cosHalfAngle = Math.cos(Math.toRadians(CONE_HALF_ANGLE_DEGREES));

        AABB searchBox = player.getBoundingBox().inflate(range);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive());

        for (LivingEntity target : targets) {
            Vec3 toTarget = target.getBoundingBox().getCenter().subtract(eyePos);
            double distance = toTarget.length();
            if (distance > range || distance < 1.0E-4) {
                continue;
            }

            Vec3 direction = toTarget.scale(1.0 / distance);
            if (direction.dot(look) < cosHalfAngle) {
                continue;
            }

            double falloff = 1.0 - (distance / range);
            double strength = CONE_BASE_POWER * chargeFraction * (0.35 + 0.65 * falloff);

            Vec3 push = direction.scale(strength);
            Vec3 currentVel = target.getDeltaMovement();
            target.setDeltaMovement(
                    currentVel.x + push.x,
                    Math.max(currentVel.y, 0) + CONE_UPWARD_BOOST * chargeFraction * (0.5 + falloff * 0.5),
                    currentVel.z + push.z
            );
            target.hurtMarked = true;
            target.fallDistance = 0;
        }
    }
}