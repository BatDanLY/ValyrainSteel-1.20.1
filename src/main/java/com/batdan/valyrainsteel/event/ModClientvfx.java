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

/**
 * Extra client-side flourish on top of the vanilla particles in ModEvents, using Lodestone's
 * particle builder for a nicer-looking flame jet / crit burst on the LOCAL player's own weapon.
 *
 * IMPORTANT: Lodestone particles are rendered purely client-side (they don't go through vanilla's
 * particle packet), so this only runs for whoever is actually holding the weapon - everyone else
 * still sees the networked vanilla particles from ModEvents, which is why those are kept as-is
 * rather than replaced. The exact WorldParticleBuilder/ParticleContent/*Data method names below
 * are written from the common usage pattern across Lodestone-based mods; double check them against
 * whatever Lodestone version you've pinned (right-click -> "Go to declaration" in your IDE) before
 * relying on this compiling as-is - the class names have moved around between Lodestone releases.
 */
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

    /** Continuous Lodestone flame jet while Blackfyre is actively breathing fire. */
    private static void spawnBlackfyreFlameJetVfx(ClientLevel level, LocalPlayer player) {
        Vec3 origin = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 tip = origin.add(look.scale(1.0));

        // See the class javadoc: verify these calls against your Lodestone version.
        //
        // WorldParticleBuilder.create(ParticleContent.SIMPLE_FLAME)
        //         .setScaleData(GenericParticleData.create(0.4f, 0.9f).build())
        //         .setLifetime(8)
        //         .setColorData(ColorParticleData.create(new Color(255, 200, 80), new Color(255, 60, 0)).build())
        //         .setSpinData(SpinParticleData.create(0, 0).build())
        //         .setTransparencyData(GenericParticleData.create(1f, 0f).build())
        //         .enableNoClip()
        //         .setRandomOffset(0.15f)
        //         .setRandomVelocity(0.05f)
        //         .repeat(level, tip.x, tip.y, tip.z, 3);
    }

    /** One-shot Lodestone spark/flare burst the instant Dark Sister lands a critical hit. */
    private static void spawnDarkSisterCritBurstVfx(ClientLevel level, LocalPlayer player) {
        Vec3 origin = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 point = origin.add(look.scale(2.0));

        // See the class javadoc: verify these calls against your Lodestone version.
        //
        // WorldParticleBuilder.create(ParticleContent.SIMPLE_FLAME)
        //         .setScaleData(GenericParticleData.create(0.7f, 0f).build())
        //         .setLifetime(12)
        //         .setColorData(ColorParticleData.create(new Color(255, 255, 220), new Color(255, 120, 0)).build())
        //         .setSpinData(SpinParticleData.create(0, 10).build())
        //         .enableNoClip()
        //         .setRandomOffset(0.3f)
        //         .setRandomVelocity(0.15f)
        //         .repeat(level, point.x, point.y, point.z, 10);
    }
}