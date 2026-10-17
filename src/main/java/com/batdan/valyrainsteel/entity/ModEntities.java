package com.batdan.valyrainsteel.entity;

import com.batdan.valyrainsteel.Valyrainsteel;
import com.batdan.valyrainsteel.entity.projectile.CalamityProjectile;
import com.batdan.valyrainsteel.entity.projectile.CatspawDaggerProjectile;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Valyrainsteel.MODID);

    public static final RegistryObject<EntityType<CatspawDaggerProjectile>> CATSPAW_DAGGER =
            ENTITY_TYPES.register("catspaw_dagger_projectile",
                    () -> EntityType.Builder
                            .<CatspawDaggerProjectile>of(
                                    CatspawDaggerProjectile::new,
                                    MobCategory.MISC
                            )
                            .sized(0.1F, 0.1F)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build("catspaw_dagger_projectile"));

    public static final RegistryObject<EntityType<CalamityProjectile>> CALAMITY =
            ENTITY_TYPES.register("calamity_projectile",
                    () -> EntityType.Builder
                            .<CalamityProjectile>of(
                                    CalamityProjectile::new,
                                    MobCategory.MISC
                            )
                            .sized(0.75F, 1.5F)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build("calamity_projectile"));


    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}