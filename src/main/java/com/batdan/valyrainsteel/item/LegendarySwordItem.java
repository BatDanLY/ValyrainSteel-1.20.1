package com.batdan.valyrainsteel.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public abstract class LegendarySwordItem extends SwordItem {

    public LegendarySwordItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack)
                .copy()
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD, ChatFormatting.ITALIC);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(this);

        String base = "item." + id.getNamespace() + "." + id.getPath();

        tooltip.add(Component.translatable(base + ".lore")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));

        tooltip.add(Component.empty());

        tooltip.add(Component.translatable("item.valyrainsteel.legendary_sword.active")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable(base + ".active")
                .withStyle(ChatFormatting.WHITE));

        tooltip.add(Component.translatable("item.valyrainsteel.legendary_sword.passive")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(base + ".passive")
                .withStyle(ChatFormatting.WHITE));



        super.appendHoverText(stack, level, tooltip, flag);
    }
}