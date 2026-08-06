package com.batdan.valyrainsteel.client;

import com.batdan.valyrainsteel.Valyrainsteel;
import com.batdan.valyrainsteel.client.gui.ReputationScreen;
import com.batdan.valyrainsteel.client.key.KeyBindings;
import com.batdan.valyrainsteel.registry.ModEffects;
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

    private static float yaw;
    private static float pitch;


    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        while (KeyBindings.OPEN_REPUTATION.consumeClick()) {
            Minecraft.getInstance().setScreen(new ReputationScreen());
        }


        Minecraft mc = Minecraft.getInstance();

        if(mc.player == null)
            return;

        if(mc.player.hasEffect(ModEffects.SHOCKED.get())){

            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);

            // Lodestone screenshake here

        }else{

            yaw = mc.player.getYRot();
            pitch = mc.player.getXRot();

        }
    }
}