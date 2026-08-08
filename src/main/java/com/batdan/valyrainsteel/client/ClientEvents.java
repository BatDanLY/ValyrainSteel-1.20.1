package com.batdan.valyrainsteel.client;

import com.batdan.valyrainsteel.Valyrainsteel;
import com.batdan.valyrainsteel.client.key.KeyBindings;
import com.batdan.valyrainsteel.client.renderer.BlackfyreRenderer;
import com.batdan.valyrainsteel.client.renderer.ThrownItemProjectileRenderer;
import com.batdan.valyrainsteel.entity.ModEntities;
import com.batdan.valyrainsteel.item.ModItems;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.renderer.GeoItemRenderer;

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
                ModEntities.CALAMITY.get(),
                ThrownItemProjectileRenderer::new
        );

        event.registerEntityRenderer(
                ModEntities.CATSPAW_DAGGER.get(),
                ThrownItemProjectileRenderer::new
        );

        GeoItemRenderer.registerItemRenderer(
                ModItems.BlACKFYRE.get(),
                new BlackfyreRenderer()
        );
    }
}