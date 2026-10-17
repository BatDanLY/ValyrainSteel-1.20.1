package com.batdan.valyrainsteel.registry;

import com.batdan.valyrainsteel.Valyrainsteel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Valyrainsteel.MODID);

    public static final RegistryObject<SoundEvent> LADY_FORLORN_CHARGE = registerSound("lady_forlorn.charge");
    public static final RegistryObject<SoundEvent> LADY_FORLORN_RELEASE = registerSound("lady_forlorn.release");
    public static final RegistryObject<SoundEvent> LADY_FORLORN_LAND = registerSound("lady_forlorn.land");

    public static final RegistryObject<SoundEvent> CALAMITY_EXPLODE = registerSound("calamity.explode");

    private ModSounds() {
    }

    private static RegistryObject<SoundEvent> registerSound(String name) {
        ResourceLocation id = new ResourceLocation(Valyrainsteel.MODID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
}