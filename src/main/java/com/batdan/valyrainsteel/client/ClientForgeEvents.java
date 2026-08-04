package com.batdan.valyrainsteel.client;

import com.batdan.valyrainsteel.Valyrainsteel;
import com.batdan.valyrainsteel.client.gui.ReputationScreen;
import com.batdan.valyrainsteel.client.key.KeyBindings;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Valyrainsteel.MODID,
        value = Dist.CLIENT
)
public class ClientForgeEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        while (KeyBindings.OPEN_REPUTATION.consumeClick()) {
            Minecraft.getInstance().setScreen(new ReputationScreen());
        }
    }
}