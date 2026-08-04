package com.batdan.valyrainsteel.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

public enum ModArmorMaterials implements ArmorMaterial {

    CASTLE_STEEL_PLATE(
            "castle_steel_plate",
            37,
            new int[]{3, 6, 8, 3},
            15,
            () -> Ingredient.of(ModItems.CASTLE_STEEL_INGOT.get())
    ),

    IRON_PLATE(
            "iron_plate",
            25,
            new int[]{2, 5, 6, 2},
            10,
            () -> Ingredient.of(Items.IRON_INGOT)
    );

    private static final int[] HEALTH_PER_SLOT = {13, 15, 16, 11};

    private final String name;
    private final int multiplier;
    private final int[] protection;
    private final int enchantment;
    private final Supplier<Ingredient> repair;

    ModArmorMaterials(String name,
                      int multiplier,
                      int[] protection,
                      int enchantment,
                      Supplier<Ingredient> repair) {

        this.name = name;
        this.multiplier = multiplier;
        this.protection = protection;
        this.enchantment = enchantment;
        this.repair = repair;
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return HEALTH_PER_SLOT[type.ordinal()] * multiplier;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return protection[type.ordinal()];
    }

    @Override
    public int getEnchantmentValue() {
        return enchantment;
    }

    @Override
    public net.minecraft.sounds.SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_IRON;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repair.get();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public float getToughness() {
        return 2.0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F;
    }
}