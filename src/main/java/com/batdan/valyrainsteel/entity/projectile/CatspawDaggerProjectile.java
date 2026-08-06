package com.batdan.valyrainsteel.entity.projectile;

import com.batdan.valyrainsteel.entity.ModEntities;
import com.batdan.valyrainsteel.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class CatspawDaggerProjectile extends SpinProjectile {

    public boolean inGround = false;
    private int groundTicks = 0;

    // Used by Forge networking
    public CatspawDaggerProjectile(EntityType<? extends CatspawDaggerProjectile> type, Level level) {
        super(type, level);
    }

    // Used when the server spawns it
    public CatspawDaggerProjectile(Level level, LivingEntity thrower) {
        super(ModEntities.CATSPAW_DAGGER.get(), thrower, level);
    }

    // Used by client spawn packet
    public CatspawDaggerProjectile(PlayMessages.SpawnEntity packet, Level level) {
        this(ModEntities.CATSPAW_DAGGER.get(), level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.CATSPAW_DAGGER.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);

        Entity target = result.getEntity();

        float damage = 8.0F;

        target.hurt(
                damageSources().thrown(this, this.getOwner()),
                damage
        );

        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);

        if (!inGround) {
            inGround = true;
            setDeltaMovement(0, 0, 0);
        }
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


        // Rotate while flying
        if (!inGround) {
            //setYRot(getYRot() + 35F);
            //updateRotationFromVelocity();
        }

        // Tiny smoke trail
        if (level().isClientSide) {
            level().addParticle(
                    ParticleTypes.SMOKE,
                    getX(),
                    getY(),
                    getZ(),
                    0,
                    0,
                    0
            );
        }

        // Despawn after 2 seconds in wall
        if (inGround) {
            groundTicks++;

            if (groundTicks >= 20 * 3) {
                discard();
            }
        }
    }

    @Override
    protected float getGravity() {
        return inGround ? 0 : 0.03F;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public boolean isInGround() {
        return inGround;
    }
}