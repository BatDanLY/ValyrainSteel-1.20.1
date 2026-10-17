package com.batdan.valyrainsteel.event;

import com.batdan.valyrainsteel.Valyrainsteel;
import com.batdan.valyrainsteel.item.BlackfyreItem;
import com.batdan.valyrainsteel.item.DarkSisterItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.awt.Color;

@Mod.EventBusSubscriber(modid = Valyrainsteel.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ModClientvfx {

    private static long lastHandledCritTime = -1L;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) return;

        ItemStack mainHand = player.getMainHandItem();

        if (mainHand.getItem() instanceof BlackfyreItem && player.isUsingItem() && player.getUseItem() == mainHand) {
            spawnBlackfyreFlameJetVfx(level, player);
        }

        if (mainHand.getItem() instanceof DarkSisterItem) {
            long critTime = DarkSisterItem.getLastCritTime(mainHand);
            if (critTime != 0 && critTime != lastHandledCritTime) {
                lastHandledCritTime = critTime;
                spawnDarkSisterCritBurstVfx(level, player);
            }
        }
    }

    private static void spawnBlackfyreFlameJetVfx(ClientLevel level, LocalPlayer player) {
        Vec3 origin = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 tip = origin.add(look.scale(1.0));
    }

    private static void spawnDarkSisterCritBurstVfx(ClientLevel level, LocalPlayer player) {
        Vec3 origin = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 point = origin.add(look.scale(2.0));
    }
}