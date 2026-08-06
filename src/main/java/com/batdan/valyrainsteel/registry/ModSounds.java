package com.batdan.valyrainsteel.registry;

import com.batdan.valyrainsteel.Valyrainsteel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * EXAMPLE registration - fold into your existing sound registry class if you have one.
 * Needs matching entries in src/main/resources/assets/valyrainsteel/sounds.json, e.g.:
 *
 * {
 *   "lady_forlon.charge": { "sounds": [ "valyrainsteel:item/lady_forlon/charge" ] },
 *   "lady_forlon.release": { "sounds": [ "valyrainsteel:item/lady_forlon/release" ] },
 *   "lady_forlon.land": { "sounds": [ "valyrainsteel:item/lady_forlon/land" ] }
 * }
 *
 * with matching .ogg files under assets/valyrainsteel/sounds/item/lady_forlon/.
 */
public final class ModSounds {
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