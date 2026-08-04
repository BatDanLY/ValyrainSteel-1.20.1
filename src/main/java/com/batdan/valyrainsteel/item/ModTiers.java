package com.batdan.valyrainsteel.item;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;


public enum ModTiers implements Tier {

    VALYRIAN_STEEL(
            4,
            2031,
            10.0f,
            4.0f,
            20,
            () -> Ingredient.of(ModItems.VALYRIAN_STEEL_INGOT.get())
    ),

    CASTLE_STEEL(
            3,
            1800,
            7.5f,
            2.5f,
            12,
            () -> Ingredient.of(ModItems.CASTLE_STEEL_INGOT.get())
    ),

    DRAGON_GLASS(
            3,
            1200,
            9.0f,
            4.0f,
            22,
            () -> Ingredient.of(ModItems.DRAGON_GLASS.get())
    );

    private final int level;
    private final int uses;
    private final float speed;
    private final float damage;
    private final int enchantmentValue;
    private final Supplier<Ingredient> repairIngredient;

    ModTiers(
            int level,
            int uses,
            float speed,
            float damage,
            int enchantmentValue,
            Supplier<Ingredient> repairIngredient
    ) {
        this.level = level;
        this.uses = uses;
        this.speed = speed;
        this.damage = damage;
        this.enchantmentValue = enchantmentValue;
        this.repairIngredient = repairIngredient;
    }

    @Override
    public int getUses() {
        return uses;
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return damage;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repairIngredient.get();
    }
}