package com.batdan.valyrainsteel.item;

import com.batdan.valyrainsteel.Valyrainsteel;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Valyrainsteel.MODID);


    // Weapons, tools and armor tab
    public static final RegistryObject<CreativeModeTab> VALYRIAN_STEEL_EQUIPMENT_TAB =
            CREATIVE_MODE_TABS.register("valyrian_steel_equipment_tab",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("creativetab.valyrainsteel.valyrian_steel_equipment_tab"))
                            .withTabsBefore(CreativeModeTabs.COMBAT)
                            .icon(() -> ModItems.DARK_SISTER.get().getDefaultInstance())
                            .displayItems((parameters, output) -> {

                                // valyrian steel
                                output.accept(ModItems.DARK_SISTER.get());
                                output.accept(ModItems.BlACKFYRE.get());
                                output.accept(ModItems.CATSPAW_DAGGER.get());
                                output.accept(ModItems.LONGCLAW.get());
                                output.accept(ModItems.ICE.get());
                                output.accept(ModItems.CALAMITY.get());
                                output.accept(ModItems.LADY_FORLORN.get());
                                output.accept(ModItems.NIGHTFALL.get());
                                output.accept(ModItems.TRIBUTE.get());

                                // castle steel plate
                                output.accept(ModItems.CASTLE_STEEL_PLATE_HELMET.get());
                                output.accept(ModItems.CASTLE_STEEL_PLATE_CHESTPLATE.get());
                                output.accept(ModItems.CASTLE_STEEL_PLATE_LEGGINGS.get());
                                output.accept(ModItems.CASTLE_STEEL_PLATE_BOOTS.get());

                                // castle steel
                                output.accept(ModItems.CASTLE_STEEL_SWORD.get());
                                output.accept(ModItems.CASTLE_STEEL_PICKAXE.get());
                                output.accept(ModItems.CASTLE_STEEL_AXE.get());
                                output.accept(ModItems.CASTLE_STEEL_SHOVEL.get());
                                output.accept(ModItems.CASTLE_STEEL_HOE.get());

                                // iron plate
                                output.accept(ModItems.IRON_PLATE_HELMET.get());
                                output.accept(ModItems.IRON_PLATE_CHESTPLATE.get());
                                output.accept(ModItems.IRON_PLATE_LEGGINGS.get());
                                output.accept(ModItems.IRON_PLATE_BOOTS.get());

                                // mail


                                // bronze plate


                                // bronze




                                // Tools
                                // output.accept(ModItems.VALYRIAN_STEEL_PICKAXE.get());
                                // output.accept(ModItems.VALYRIAN_STEEL_AXE.get());
                                // output.accept(ModItems.VALYRIAN_STEEL_SHOVEL.get());
                                // output.accept(ModItems.VALYRIAN_STEEL_HOE.get());

                                // Armor
                                // output.accept(ModItems.VALYRIAN_STEEL_HELMET.get());
                                // output.accept(ModItems.VALYRIAN_STEEL_CHESTPLATE.get());
                                // output.accept(ModItems.VALYRIAN_STEEL_LEGGINGS.get());
                                // output.accept(ModItems.VALYRIAN_STEEL_BOOTS.get());

                                // Dragon Glass
                                output.accept(ModItems.DRAGON_GLASS_SWORD.get());
                                output.accept(ModItems.DRAGON_GLASS_PICKAXE.get());
                                output.accept(ModItems.DRAGON_GLASS_AXE.get());
                                output.accept(ModItems.DRAGON_GLASS_SHOVEL.get());
                                output.accept(ModItems.DRAGON_GLASS_HOE.get());

                            })
                            .build());


    // Materials and crafting items tab
    public static final RegistryObject<CreativeModeTab> VALYRIAN_STEEL_MATERIALS_TAB =
            CREATIVE_MODE_TABS.register("valyrian_steel_materials_tab",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("creativetab.valyrainsteel.valyrian_steel_materials_tab"))
                            .withTabsBefore(VALYRIAN_STEEL_EQUIPMENT_TAB.getId())
                            .icon(() -> ModItems.VALYRIAN_STEEL_INGOT.get().getDefaultInstance())
                            .displayItems((parameters, output) -> {

                                // Ingots/materials
                                output.accept(ModItems.VALYRIAN_STEEL_INGOT.get());
                                output.accept(ModItems.CASTLE_STEEL_INGOT.get());
                                output.accept(ModItems.DRAGON_GLASS.get());

                            })
                            .build());


    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}