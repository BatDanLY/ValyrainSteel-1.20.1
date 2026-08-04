package com.batdan.valyrainsteel.event;

import com.batdan.valyrainsteel.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber
public class TreecapEvent {

    private static final int MAX_LOGS = 256;

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {

        Level level = (Level) event.getLevel();

        if (level.isClientSide())
            return;

        Player player = event.getPlayer();
        ItemStack stack = player.getMainHandItem();

        if (stack.getItem() != ModItems.CALAMITY.get())
            return;

        BlockPos start = event.getPos();

        if (!level.getBlockState(start).is(BlockTags.LOGS))
            return;

        // Only trigger on the bottom log
        if (level.getBlockState(start.below()).is(BlockTags.LOGS))
            return;

        Set<BlockPos> visited = new HashSet<>();

        collectLogs(level, start, visited);

        visited.remove(start); // vanilla already broke this one

        for (BlockPos pos : visited) {

            Block.dropResources(level.getBlockState(pos), level, pos);

            level.destroyBlock(pos, false);

            stack.hurtAndBreak(
                    1,
                    player,
                    p -> p.broadcastBreakEvent(player.getUsedItemHand())
            );
        }
    }

    private static void collectLogs(Level level, BlockPos pos, Set<BlockPos> visited) {

        if (visited.size() >= MAX_LOGS)
            return;

        if (visited.contains(pos))
            return;

        if (!level.getBlockState(pos).is(BlockTags.LOGS))
            return;

        visited.add(pos);

        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {

                    if (x == 0 && y == 0 && z == 0)
                        continue;

                    collectLogs(
                            level,
                            pos.offset(x, y, z),
                            visited
                    );
                }
            }
        }
    }
}