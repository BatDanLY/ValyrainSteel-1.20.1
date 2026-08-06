package com.batdan.valyrainsteel.registry;

import com.batdan.valyrainsteel.Valyrainsteel;
import com.batdan.valyrainsteel.effects.ShockedEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Valyrainsteel.MODID);

    public static final RegistryObject<MobEffect> SHOCKED =
            EFFECTS.register("shocked", ShockedEffect::new);
}