package com.batdan.valyrainsteel.client;

import com.batdan.valyrainsteel.Valyrainsteel;
import com.batdan.valyrainsteel.client.key.KeyBindings;
import com.batdan.valyrainsteel.client.renderer.CalamityRenderer;
import com.batdan.valyrainsteel.client.renderer.CatspawDaggerRenderer;
import com.batdan.valyrainsteel.client.particle.ModParticleFactories;
import com.batdan.valyrainsteel.entity.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Valyrainsteel.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public class ClientEvents {

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        KeyBindings.register(event);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        // Register HUD overlay here
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {

        event.registerEntityRenderer(
                ModEntities.CATSPAW_DAGGER.get(),
                CatspawDaggerRenderer::new
        );

        event.registerEntityRenderer(
                ModEntities.CALAMITY.get(),
                CalamityRenderer::new
        );
    }
}