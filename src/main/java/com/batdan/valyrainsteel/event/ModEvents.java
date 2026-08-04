package com.batdan.valyrainsteel.event;

import com.batdan.valyrainsteel.Valyrainsteel;
import com.batdan.valyrainsteel.item.BlackfyreItem;
import com.batdan.valyrainsteel.item.DarkSisterItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = Valyrainsteel.MODID)
public class ModEvents {

    private static final long DASH_HIT_COOLDOWN_TICKS = 10;

    /** Passive ability: rolls the stacking crit chance whenever Dark Sister lands a melee hit. */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        Entity direct = source.getEntity();

        if (!(direct instanceof Player player)) return;

        ItemStack weapon = player.getMainHandItem();

        if (weapon.getItem() instanceof BlackfyreItem) {
            BlackfyreItem.addFlame(weapon, BlackfyreItem.FLAME_GAIN_PER_HIT);
        }

        if (!(weapon.getItem() instanceof DarkSisterItem)) return;

        Level level = player.level();
        boolean isCrit = DarkSisterItem.rollAndAdvanceCrit(weapon, level);

        if (isCrit) {
            event.setAmount(event.getAmount() * 2.0f);

            LivingEntity target = event.getEntity();
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        12, 0.3, 0.3, 0.3, 0.1);
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        1, 0.0, 0.0, 0.0, 0.0);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 0.9f);
            level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.5f, 1.6f);
        }
    }

    /** Roughly where the blade tip sits for a player, used for the heat/extinguish effects. */
    private static net.minecraft.world.phys.Vec3 bladeTipPos(Player player) {
        net.minecraft.world.phys.Vec3 look = player.getLookAngle().normalize();
        return player.getEyePosition().add(look.x * 1.1, -0.3 + look.y * 1.1, look.z * 1.1);
    }

    /**
     * Ambient effects for a held Dark Sister: the hotter the streak, the more flame/smoke drips
     * off the blade, and letting the streak go cold plays an extinguish hiss.
     */
    @SubscribeEvent
    public static void onPlayerHeldItemTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        Level level = player.level();
        if (level.isClientSide) return;

        ItemStack weapon = player.getMainHandItem();
        if (!(weapon.getItem() instanceof DarkSisterItem)) return;

        // Extinguish check: if the streak has gone cold, snuff it out and play the effect
        if (DarkSisterItem.tickAndCheckExtinguish(weapon, level)) {
            net.minecraft.world.phys.Vec3 pos = bladeTipPos(player);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 10, 0.15, 0.15, 0.15, 0.02);
                serverLevel.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 14, 0.2, 0.2, 0.2, 0.01);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 0.8f, 1.2f);
        }

        // Ambient heat particles, scaled by how many stacks are built up
        int stacks = DarkSisterItem.getCritStacks(weapon);
        if (stacks <= 0) return;
        if (level.getGameTime() % 4 != 0) return; // throttle: every 4 ticks

        net.minecraft.world.phys.Vec3 pos = bladeTipPos(player);
        int flameCount = stacks / 2;       // 0-5
        int smokeCount = stacks;           // 0-10

        if (level instanceof ServerLevel serverLevel) {
            if (flameCount > 0) {
                serverLevel.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, flameCount, 0.08, 0.08, 0.08, 0.01);
            }
            if (stacks >= 8) {
                serverLevel.sendParticles(ParticleTypes.LAVA, pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.0);
            }
            serverLevel.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, Math.max(1, smokeCount / 3), 0.1, 0.1, 0.1, 0.01);
        }
    }

    /** Passive regen: Blackfyre's flame store slowly refills whenever it isn't actively firing. */
    @SubscribeEvent
    public static void onBlackfyreRegenTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return;

        ItemStack weapon = player.getMainHandItem();
        if (!(weapon.getItem() instanceof BlackfyreItem)) return;

        boolean activelyFiring = player.isUsingItem()
                && player.getUsedItemHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                && player.getUseItem() == weapon;

        if (!activelyFiring) {
            BlackfyreItem.regenFlame(weapon);
        }
    }

    /** Active ability: while a dash is in progress, sweeps for entities the player passes through. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return;

        CompoundTag data = player.getPersistentData();
        int dashTicks = data.getInt("valyrainsteel_dash_ticks");
        if (dashTicks <= 0) return;

        ItemStack weapon = player.getMainHandItem();
        boolean holdingDarkSister = weapon.getItem() instanceof DarkSisterItem;

        // Sweep a box covering this tick's movement so fast-moving dashes don't skip over entities
        AABB sweep = player.getBoundingBox()
                .expandTowards(player.getDeltaMovement().x, player.getDeltaMovement().y, player.getDeltaMovement().z)
                .inflate(0.6);

        List<Entity> nearby = player.level().getEntities(player, sweep,
                e -> e instanceof LivingEntity && e != player && e.isAlive());

        long now = player.level().getGameTime();

        for (Entity entity : nearby) {
            LivingEntity living = (LivingEntity) entity;

            long lastDashHit = living.getPersistentData().getLong("valyrainsteel_dash_hit_time");
            if (now - lastDashHit < DASH_HIT_COOLDOWN_TICKS) continue; // already struck this dash

            float damage = DarkSisterItem.DASH_HIT_DAMAGE;
            boolean isCrit = holdingDarkSister && DarkSisterItem.rollAndAdvanceCrit(weapon, player.level());
            if (isCrit) damage *= 2.0f;

            living.hurt(player.damageSources().playerAttack(player), damage);
            living.setSecondsOnFire(5);
            living.getPersistentData().putLong("valyrainsteel_dash_hit_time", now);

            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        living.getX(), living.getY() + living.getBbHeight() / 2, living.getZ(),
                        10, 0.2, 0.3, 0.2, 0.02);
            }
            player.level().playSound(null, living.blockPosition(), SoundEvents.GENERIC_BURN,
                    SoundSource.PLAYERS, 0.7f, 1.2f);
        }

        data.putInt("valyrainsteel_dash_ticks", dashTicks - 1);
    }
}