package com.batdan.valyrainsteel.entity.projectile;

import com.batdan.valyrainsteel.entity.ModEntities;
import com.batdan.valyrainsteel.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.PlayMessages;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class CalamityProjectile extends ThrowableItemProjectile {

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

        float damage = 8.0F;

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

            // Same rotation as the renderer
            Quaternionf rotation = new Quaternionf()
                    .rotateY((float)Math.toRadians(getYRot()))
                    .rotateX((float)Math.toRadians(getXRot()))
                    .rotateX((float)Math.toRadians((tickCount) * 30F));

            // Local positions on the model
            Vector3f top = new Vector3f(0F, 0.75F, 0F);
            Vector3f bottom = new Vector3f(0F, -0.75F, 0F);

            // Rotate them into world space
            top.rotate(rotation);
            bottom.rotate(rotation);

            // Small backwards velocity so they trail
            Vec3 motion = getDeltaMovement().scale(-0.2);

            for (int i = 0; i < 3; i++) {

                float ox = (random.nextFloat() - 0.5F) * 0.03F;
                float oy = (random.nextFloat() - 0.5F) * 0.03F;
                float oz = (random.nextFloat() - 0.5F) * 0.03F;

                level().addParticle(
                        ParticleTypes.ELECTRIC_SPARK,
                        getX() + top.x + ox,
                        getY() + top.y + oy,
                        getZ() + top.z + oz,
                        motion.x,
                        motion.y,
                        motion.z
                );

                level().addParticle(
                        ParticleTypes.ELECTRIC_SPARK,
                        getX() + bottom.x + ox,
                        getY() + bottom.y + oy,
                        getZ() + bottom.z + oz,
                        motion.x,
                        motion.y,
                        motion.z
                );
            }
        }
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

        if(level().isClientSide)
            return;

        double radius = 3.5D;

        List<LivingEntity> entities = level().getEntitiesOfClass(
                LivingEntity.class,
                getBoundingBox().inflate(radius));

        for(LivingEntity entity : entities) {

            if(entity == getOwner())
                continue;

            double distance = entity.distanceTo(this);

            float damage = (float)Math.max(
                    0,
                    14 - distance * 2
            );

            entity.hurt(
                    damageSources().explosion(this, getOwner()),
                    damage
            );

            Vec3 knockback = entity.position()
                    .subtract(position())
                    .normalize()
                    .scale(1.2);

            entity.push(
                    knockback.x,
                    0.5,
                    knockback.z
            );
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
                SoundEvents.LIGHTNING_BOLT_IMPACT,
                SoundSource.PLAYERS,
                5F,
                1F
        );

        level().playSound(
                null,
                blockPosition(),
                SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.PLAYERS,
                10F,
                1F
        );

        ((ServerLevel)level()).sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                getX(),
                getY()+1,
                getZ(),
                100,
                1,
                2,
                1,
                0.25
        );

        discard();
    }
}