package com.batdan.valyrainsteel.effects;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.awt.*;

public class ShockedEffect extends MobEffect {

    public ShockedEffect() {
        super(MobEffectCategory.HARMFUL, 0x9EEBFF);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {

        // Completely stop movement
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hurtMarked = true;

        if (!entity.level().isClientSide) {

            // Half heart every 10 ticks
            if (entity.tickCount % 10 == 0) {

                entity.hurt(
                        entity.damageSources().magic(),
                        1.0F
                );

                entity.level().playSound(
                        null,
                        entity.blockPosition(),
                        SoundEvents.LIGHTNING_BOLT_IMPACT,
                        SoundSource.PLAYERS,
                        0.4F,
                        1.6F
                );
            }

        }
    }
}