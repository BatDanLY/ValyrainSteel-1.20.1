package com.batdan.valyrainsteel.entity.projectile;

import com.batdan.valyrainsteel.effects.ShockedEffect;
import com.batdan.valyrainsteel.entity.ModEntities;
import com.batdan.valyrainsteel.item.ModItems;
import com.batdan.valyrainsteel.registry.ModEffects;
import com.batdan.valyrainsteel.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.awt.*;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.PlayMessages;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import team.lodestar.lodestone.registry.common.particle.LodestoneParticleRegistry;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData;
import team.lodestar.lodestone.systems.particle.render_types.LodestoneWorldParticleRenderType;

public class CalamityProjectile extends SpinProjectile {


    private static final Color TRACER_COLOR_START = new Color(157, 139, 224);
    private static final Color TRACER_COLOR_END = new Color(130, 126, 150);

    private Vec3 lastPos = Vec3.ZERO;
    private float prevYaw;
    private float prevPitch;

    // Used by Forge networking
    public CalamityProjectile(EntityType<? extends CalamityProjectile> type, Level level) {
        super(type, level);
    }

    // Used when the server spawns it
    public CalamityProjectile(Level level, LivingEntity thrower) {
        super(ModEntities.CALAMITY.get(), thrower, level);
    }

    // Used by client spawn packet
    public CalamityProjectile(PlayMessages.SpawnEntity packet, Level level) {
        this(ModEntities.CALAMITY.get(), level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.CALAMITY.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);

        Entity target = result.getEntity();

        float damage = 16.0F;

        explode();

        target.hurt(
                damageSources().thrown(this, this.getOwner()),
                damage
        );
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);

        explode();
    }

    @Override
    public void tick() {

        super.tick();

        Vec3 currentPos = position();

        prevYaw = getYRot();
        prevPitch = getXRot();

        if (lastPos == Vec3.ZERO) {
            lastPos = currentPos;
        }

        Vec3 velocity = getDeltaMovement();

        if (velocity.lengthSqr() > 1.0E-7D) {

            float yaw = (float)(Math.atan2(velocity.x, velocity.z) * Mth.RAD_TO_DEG);
            float pitch = (float)(Math.atan2(
                    velocity.y,
                    Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z)
            ) * Mth.RAD_TO_DEG);

            setYRot(yaw);
            setXRot(-pitch);
        }


        if (level().isClientSide) {

            Vec3 delta = currentPos.subtract(lastPos);
            double distance = delta.length();

            double speed = getDeltaMovement().length();
            int samples = Mth.clamp((int)(speed * 80), 4, 40);

            for (int s = 0; s <= samples; s++) {

                float t = s / (float)samples;

                // Interpolated position
                double px = Mth.lerp(t, lastPos.x, currentPos.x);
                double py = Mth.lerp(t, lastPos.y, currentPos.y);
                double pz = Mth.lerp(t, lastPos.z, currentPos.z);

                // Interpolated rotation
                float yaw = Mth.rotLerp(t, prevYaw, getYRot());
                float pitch = Mth.lerp(t, prevPitch, getXRot());
                float roll = Mth.lerp(t, prevSpin, spin);

                Quaternionf rotation = new Quaternionf()
                        .rotateY((float) Math.toRadians(yaw))
                        .rotateX((float) Math.toRadians(pitch))
                        .rotateX((float) Math.toRadians(roll)); // Change to rotateZ if your model rolls around Z

                Vector3f top = new Vector3f(0F, 0.75F, 0F);
                Vector3f bottom = new Vector3f(0F, -0.75F, 0F);

                top.rotate(rotation);
                bottom.rotate(rotation);

                float fade = t * t * (3F - 2F * t); // smoothstep
                float alpha = Mth.lerp(fade, 0.15F, 0.95F);
                float scale = Mth.lerp(fade, 0.03F, 0.09F);


                for (int i = 0; i < 2; i++) {

                    float ox = (random.nextFloat() - 0.5F) * 0.015F;
                    float oy = (random.nextFloat() - 0.5F) * 0.015F;
                    float oz = (random.nextFloat() - 0.5F) * 0.015F;

                    TracerParticle(px, py, pz, top, alpha, scale, ox, oy, oz);
                    TracerParticle(px, py, pz, bottom, alpha, scale, ox, oy, oz);
                }
            }
        }

        lastPos = currentPos;
    }

    private void TracerParticle(
            double x,
            double y,
            double z,
            Vector3f offset,
            float alpha,
            float scale,
            float ox,
            float oy,
            float oz) {

        WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                .setLifetime(6)
                .setScaleData(GenericParticleData.create(
                        0.05F,
                        0.0F
                ).build())
                .setTransparencyData(
                        GenericParticleData.create(alpha, 0F).build()
                )
                .setColorData(ColorParticleData.create(
                        TRACER_COLOR_START,
                        TRACER_COLOR_END
                ).build())
                .setSpinData(SpinParticleData.create(0F, 0.08F).build())
                .addMotion(0, 0, 0)
                .enableNoClip()
                .spawn(
                        level(),
                        x + offset.x + ox,
                        y + offset.y + oy,
                        z + offset.z + oz
                );
    }

    @Override
    protected float getGravity() {
        return 0.03F;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void explode() {

        if (level().isClientSide)
            return;

        double radius = 4D;

        List<LivingEntity> entities = level().getEntitiesOfClass(
                LivingEntity.class,
                getBoundingBox().inflate(radius));

        for (LivingEntity entity : entities) {

            if (entity == getOwner())
                continue;

            double distance = entity.distanceTo(this);

            float damage = (float)Math.max(0, 20 - distance * 2);

            entity.hurt(
                    damageSources().explosion(this, getOwner()),
                    damage
            );

            entity.addEffect(new MobEffectInstance(
                    ModEffects.SHOCKED.get(),
                    20 * 2,
                    0,
                    false,
                    false,
                    true
            ));

            Vec3 knockback = entity.position()
                    .subtract(position())
                    .normalize()
                    .scale(1.2);

            entity.push(knockback.x, 0.5, knockback.z);
        }

        level().explode(
                this,
                getX(),
                getY(),
                getZ(),
                0F,
                Level.ExplosionInteraction.NONE
        );

        level().playSound(
                null,
                blockPosition(),
                ModSounds.CALAMITY_EXPLODE.get(),
                SoundSource.PLAYERS,
                5F,
                1F
        );

        // Tell the client to spawn the fancy particles.
        level().broadcastEntityEvent(this, (byte)15);

        discard();
    }

    @Override
    public void handleEntityEvent(byte id) {
        super.handleEntityEvent(id);

        if (id == 15) {
            spawnExplosionParticles();
        }
    }

    private void spawnExplosionParticles() {

        Vec3 center = position();

        // Charge sparks
        for (int i = 0; i < 20; i++) {

            Vec3 offset = randomDirection().scale(1.5);

            WorldParticleBuilder.create(LodestoneParticleRegistry.SPARK_PARTICLE)
                    .setLifetime(8)
                    .setScaleData(GenericParticleData.create(0.05F, 0F).build())
                    .setTransparencyData(GenericParticleData.create(0.2F, 0.9F).build())
                    .setColorData(ColorParticleData.create(
                            new Color(90,170,255),
                            Color.WHITE
                    ).build())
                    .addMotion(
                            -offset.x * 0.08,
                            -offset.y * 0.08,
                            -offset.z * 0.08
                    )
                    .enableNoClip()
                    .spawn(
                            level(),
                            center.x + offset.x,
                            center.y + offset.y,
                            center.z + offset.z
                    );
        }

        // Branching arcs
        for (int branch = 0; branch < 24; branch++) {

            Vec3 dir = randomDirection();
            Vec3 pos = center;

            for (int j = 0; j < 44; j++) {

                dir = dir.add(
                        random.nextGaussian() * 0.3,
                        random.nextGaussian() * 0.3,
                        random.nextGaussian() * 0.3
                ).normalize();

                pos = pos.add(dir.scale(0.2));

                WorldParticleBuilder.create(LodestoneParticleRegistry.SPARK_PARTICLE)
                        .setLifetime(15)
                        .setScaleData(GenericParticleData.create(0.08F, 0.02F).build())
                        .setTransparencyData(GenericParticleData.create(1F, 0F).build())
                        .setRenderType(LodestoneWorldParticleRenderType.ADDITIVE)
                        .setColorData(ColorParticleData.create(
                                Color.WHITE,
                                new Color(120,200,255)
                        ).build())
                        .enableNoClip()
                        .spawn(level(), pos.x, pos.y, pos.z);

            }
        }

        // Central flash
        WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                .setLifetime(6)
                .setScaleData(GenericParticleData.create(3.5F, 0F).build())
                .setTransparencyData(GenericParticleData.create(1F, 0F).build())
                .setRenderType(LodestoneWorldParticleRenderType.ADDITIVE)
                .setColorData(ColorParticleData.create(
                        Color.WHITE,
                        new Color(180,220,255)
                ).build())
                .enableNoClip()
                .spawn(level(), center.x, center.y, center.z);

        // Outward sparks
        for (int i = 0; i < 120; i++) {

            Vec3 dir = randomDirection();
            double speed = 0.35 + random.nextDouble()*0.75;

            WorldParticleBuilder.create(LodestoneParticleRegistry.SPARK_PARTICLE)
                    .setLifetime(30)
                    .setScaleData(GenericParticleData.create(0.08F, 0.02F).build())
                    .setTransparencyData(GenericParticleData.create(1F, 0F).build())
                    .setRenderType(LodestoneWorldParticleRenderType.ADDITIVE)
                    .setColorData(ColorParticleData.create(
                            Color.WHITE,
                            new Color(80,150,255)
                    ).build())
                    .addMotion(
                            dir.x * speed,
                            dir.y * speed,
                            dir.z * speed
                    )
                    .enableNoClip()
                    .spawn(level(), center.x, center.y, center.z);
        }

        WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                .setLifetime(25)
                .setScaleData(GenericParticleData.create(0.5F, 10F).build())
                .setTransparencyData(GenericParticleData.create(0.5F, 0F).build())
                .setRenderType(LodestoneWorldParticleRenderType.ADDITIVE)
                .setColorData(ColorParticleData.create(
                        new Color(180,220,255),
                        new Color(90,170,255)
                ).build())
                .spawn(level(), getX(), getY(), getZ());
    }

    private Vec3 randomDirection() {

        Vec3 dir = new Vec3(
                random.nextGaussian(),
                random.nextGaussian(),
                random.nextGaussian()
        );

        return dir.normalize();
    }
}