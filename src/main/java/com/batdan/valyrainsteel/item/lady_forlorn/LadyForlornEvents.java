package com.batdan.valyrainsteel.item.lady_forlorn;

import com.batdan.valyrainsteel.item.LadyForlornItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = "valyrainsteel", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LadyForlornEvents {

    private static final float LANDING_EFFECT_MIN_DISTANCE = 3.0F;

    private LadyForlornEvents() {
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();

        if (!(entity.getMainHandItem().getItem() instanceof LadyForlornItem)) {
            return;
        }

        if (event.getDistance() >= LANDING_EFFECT_MIN_DISTANCE) {
            LadyForlornEffects.spawnLandingEffects(entity);
        }

        event.setDamageMultiplier(0.0F);
    }
}