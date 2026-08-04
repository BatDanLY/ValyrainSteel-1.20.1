package com.batdan.valyrainsteel.item;

import com.batdan.valyrainsteel.entity.projectile.CatspawDaggerProjectile;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.List;


public class CatspawDaggerItem extends LegendarySwordItem {

    public static final int COOLDOWN = 0;

    public CatspawDaggerItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
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
        return UseAnim.SPEAR; // or BOW if you prefer
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {

        if (!(entity instanceof Player player))
            return;

        int charge = getUseDuration(stack) - timeLeft;

        float velocity = Math.min(charge / 20F, 1F) * 1.5F;

        if (!level.isClientSide) {

            CatspawDaggerProjectile projectile =
                    new CatspawDaggerProjectile(level, player);

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
}