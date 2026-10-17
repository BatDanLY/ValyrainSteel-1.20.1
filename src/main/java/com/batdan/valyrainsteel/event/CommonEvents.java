package com.batdan.valyrainsteel.event;

import com.batdan.valyrainsteel.Valyrainsteel;
import com.batdan.valyrainsteel.registry.ModEffects;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import team.lodestar.lodestone.registry.common.particle.LodestoneParticleRegistry;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.particle.render_types.LodestoneWorldParticleRenderType;

import java.awt.*;

@Mod.EventBusSubscriber(modid = Valyrainsteel.MODID)
public class CommonEvents {

    @SubscribeEvent
    public static void livingTick(LivingEvent.LivingTickEvent event) {

        LivingEntity entity = event.getEntity();

        if (!entity.hasEffect(ModEffects.SHOCKED.get()))
            return;

        entity.setDeltaMovement(Vec3.ZERO);

        entity.zza = 0;
        entity.xxa = 0;

        entity.setSprinting(false);

        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
        }

        if (entity.level().isClientSide) {
            spawnLightning(entity);
        }
    }

    private static void spawnLightning(LivingEntity entity) {

        RandomSource random = entity.getRandom();

        Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.55, 0);

        for (int branch = 0; branch < 10; branch++) {

            Vec3 dir = randomDirection(random);
            Vec3 pos = center;

            for (int j = 0; j < 15; j++) {

                dir = dir.add(
                        random.nextGaussian() * 0.3,
                        random.nextGaussian() * 0.3,
                        random.nextGaussian() * 0.3
                ).normalize();

                pos = pos.add(dir.scale(0.15));

                WorldParticleBuilder.create(LodestoneParticleRegistry.SPARK_PARTICLE)
                        .setLifetime(12)
                        .setScaleData(GenericParticleData.create(0.08F, 0.02F).build())
                        .setTransparencyData(GenericParticleData.create(1F, 0F).build())
                        .setRenderType(LodestoneWorldParticleRenderType.ADDITIVE)
                        .setColorData(ColorParticleData.create(
                                Color.WHITE,
                                new Color(120, 200, 255)
                        ).build())
                        .enableNoClip()
                        .spawn(entity.level(), pos.x, pos.y, pos.z);
            }
        }
    }

    private static Vec3 randomDirection(RandomSource random) {

        return new Vec3(
                random.nextDouble() - 0.5,
                random.nextDouble() - 0.5,
                random.nextDouble() - 0.5
        ).normalize();
    }
}