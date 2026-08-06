package com.batdan.valyrainsteel.item;

import com.batdan.valyrainsteel.Valyrainsteel;
import net.minecraft.world.item.*;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Valyrainsteel.MODID);

    //Materials and Crafting Items
    public static final RegistryObject<Item> VALYRIAN_STEEL_INGOT = ITEMS.register("valyrian_steel_ingot",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> CASTLE_STEEL_INGOT = ITEMS.register("castle_steel_ingot",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> DRAGON_GLASS = ITEMS.register("dragon_glass_shard",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));


    //Valyrian Steel Equipment
    public static final RegistryObject<Item> DARK_SISTER = ITEMS.register("dark_sister",
            () -> new DarkSisterItem(ModTiers.VALYRIAN_STEEL, 5, -2f,
                    new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)));

    public static final RegistryObject<Item> BlACKFYRE = ITEMS.register("blackfyre",
            () -> new BlackfyreItem(ModTiers.VALYRIAN_STEEL, 8, -2.6F,
                    new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)));

    public static final RegistryObject<Item> CATSPAW_DAGGER = ITEMS.register("catspaw_dagger",
            () -> new CatspawDaggerItem(ModTiers.VALYRIAN_STEEL, 3, -1F,
                    new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)));

    public static final RegistryObject<Item> LONGCLAW = ITEMS.register("longclaw",
            () -> new LongclawItem(ModTiers.VALYRIAN_STEEL, 7, -2.4F,
                    new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)));

    public static final RegistryObject<Item> ICE = ITEMS.register("ice",
            () -> new IceItem(ModTiers.VALYRIAN_STEEL, 14, -3.6F,
                    new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)));

    public static final RegistryObject<Item> CALAMITY = ITEMS.register("calamity",
            () -> new CalamityItem(ModTiers.VALYRIAN_STEEL, 10, -3F,
                    new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)));

    public static final RegistryObject<Item> NIGHTFALL = ITEMS.register("nightfall",
            () -> new NightfallItem(ModTiers.VALYRIAN_STEEL, 8, -2.6F,
                    new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)));

    public static final RegistryObject<Item> TRIBUTE = ITEMS.register("tribute",
            () -> new TributeItem(ModTiers.CASTLE_STEEL, 4, -1.6F,
                    new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)));

    public static final RegistryObject<Item> LADY_FORLORN = ITEMS.register("lady_forlorn",
            () -> new LadyForlornItem(ModTiers.VALYRIAN_STEEL, 7, -2.4F,
                    new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)));

    //Valyrian Steel Armor


    //Castle Steel Equipment
    public static final RegistryObject<Item> CASTLE_STEEL_SWORD =
            ITEMS.register("castle_steel_sword",
                    () -> new SwordItem(ModTiers.CASTLE_STEEL, 3, -2.4F,
                            new Item.Properties()));

    public static final RegistryObject<Item> CASTLE_STEEL_PICKAXE =
            ITEMS.register("castle_steel_pickaxe",
                    () -> new PickaxeItem(ModTiers.CASTLE_STEEL, 1, -2.8F,
                            new Item.Properties()));

    public static final RegistryObject<Item> CASTLE_STEEL_AXE =
            ITEMS.register("castle_steel_axe",
                    () -> new AxeItem(ModTiers.CASTLE_STEEL,5.0F, -3.0F,
                            new Item.Properties()));

    public static final RegistryObject<Item> CASTLE_STEEL_SHOVEL =
            ITEMS.register("castle_steel_shovel",
                    () -> new ShovelItem(ModTiers.CASTLE_STEEL, 1.5F, -3.0F,
                            new Item.Properties()));

    public static final RegistryObject<Item> CASTLE_STEEL_HOE =
            ITEMS.register("castle_steel_hoe",
                    () -> new HoeItem(ModTiers.CASTLE_STEEL, -2, 0.0F,
                            new Item.Properties()));

    //Castle Steel Plate Armor
    public static final RegistryObject<Item> CASTLE_STEEL_PLATE_HELMET =
            ITEMS.register("castle_steel_plate_helmet",
                    () -> new ArmorItem(ModArmorMaterials.CASTLE_STEEL_PLATE,
                            ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistryObject<Item> CASTLE_STEEL_PLATE_CHESTPLATE =
            ITEMS.register("castle_steel_plate_chestplate",
                    () -> new ArmorItem(ModArmorMaterials.CASTLE_STEEL_PLATE,
                            ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistryObject<Item> CASTLE_STEEL_PLATE_LEGGINGS =
            ITEMS.register("castle_steel_plate_leggings",
                    () -> new ArmorItem(ModArmorMaterials.CASTLE_STEEL_PLATE,
                            ArmorItem.Type.LEGGINGS, new Item.Properties()));

    public static final RegistryObject<Item> CASTLE_STEEL_PLATE_BOOTS =
            ITEMS.register("castle_steel_plate_boots",
                    () -> new ArmorItem(ModArmorMaterials.CASTLE_STEEL_PLATE,
                            ArmorItem.Type.BOOTS, new Item.Properties()));
    
    
    //Iron Plate
    public static final RegistryObject<Item> IRON_PLATE_HELMET =
            ITEMS.register("iron_plate_helmet",
                    () -> new ArmorItem(ModArmorMaterials.IRON_PLATE,
                            ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistryObject<Item> IRON_PLATE_CHESTPLATE =
            ITEMS.register("iron_plate_chestplate",
                    () -> new ArmorItem(ModArmorMaterials.IRON_PLATE,
                            ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistryObject<Item> IRON_PLATE_LEGGINGS =
            ITEMS.register("iron_plate_leggings",
                    () -> new ArmorItem(ModArmorMaterials.IRON_PLATE,
                            ArmorItem.Type.LEGGINGS, new Item.Properties()));

    public static final RegistryObject<Item> IRON_PLATE_BOOTS =
            ITEMS.register("iron_plate_boots",
                    () -> new ArmorItem(ModArmorMaterials.IRON_PLATE,
                            ArmorItem.Type.BOOTS, new Item.Properties()));


    //Dragon Glass Equipment
    public static final RegistryObject<Item> DRAGON_GLASS_SWORD =
            ITEMS.register("dragon_glass_sword",
                    () -> new SwordItem(ModTiers.DRAGON_GLASS, 3, -2.4F,
                            new Item.Properties()));

    public static final RegistryObject<Item> DRAGON_GLASS_PICKAXE =
            ITEMS.register("dragon_glass_pickaxe",
                    () -> new PickaxeItem(ModTiers.DRAGON_GLASS, 1, -2.8F,
                            new Item.Properties()));

    public static final RegistryObject<Item> DRAGON_GLASS_AXE =
            ITEMS.register("dragon_glass_axe",
                    () -> new AxeItem(ModTiers.DRAGON_GLASS, 5.0F, -3.0F,
                            new Item.Properties()));

    public static final RegistryObject<Item> DRAGON_GLASS_SHOVEL =
            ITEMS.register("dragon_glass_shovel",
                    () -> new ShovelItem(ModTiers.DRAGON_GLASS, 1.5F, -3.0F,
                            new Item.Properties()));

    public static final RegistryObject<Item> DRAGON_GLASS_HOE =
            ITEMS.register("dragon_glass_hoe",
                    () -> new HoeItem(ModTiers.DRAGON_GLASS, -2, 0.0F,
                            new Item.Properties()));


    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}