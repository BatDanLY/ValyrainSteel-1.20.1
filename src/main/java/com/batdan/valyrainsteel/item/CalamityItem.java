package com.batdan.valyrainsteel.item;

import com.batdan.valyrainsteel.entity.projectile.CalamityProjectile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class CalamityItem extends AxeItem {

    public static final int COOLDOWN = 0;

    public CalamityItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        player.startUsingItem(hand);

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {

        if (!(entity instanceof Player player))
            return;

        int charge = getUseDuration(stack) - timeLeft;

        if (charge < 10)
            return;

        float velocity = Math.min(charge / 20F, 1F) * 2F;

        if (!level.isClientSide) {

            CalamityProjectile projectile =
                    new CalamityProjectile(level, player);

            projectile.setItem(stack.copy());

            projectile.shootFromRotation(
                    player,
                    player.getXRot(),
                    player.getYRot(),
                    0.0F,
                    velocity,
                    0.5F
            );

            level.addFreshEntity(projectile);

            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.TRIDENT_THROW,
                    SoundSource.PLAYERS,
                    1F,
                    1F
            );

            player.getCooldowns().addCooldown(this, COOLDOWN);

            stack.hurtAndBreak(1, player,
                    p -> p.broadcastBreakEvent(player.getUsedItemHand()));
        }
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
