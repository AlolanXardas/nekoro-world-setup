package dev.xardas.nekoroworldsetup;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * Digging a natural block next to water fills its place with a water source, so no flowing water
 * spreads over beaches and lake shores. The block is only gone after the break event, so the fill
 * happens at the end of the same tick.
 */
public final class WaterFill {
    private static final Direction[] TOUCHING = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP};
    private static final Direction[] SPREAD = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN};
    private static final int FLOOD_RANGE = 2;
    private static final int FLOOD_LIMIT = 24;

    private static final Map<ResourceKey<Level>, Set<BlockPos>> PENDING = new HashMap<>();

    private WaterFill() { }

    /** A player broke this block (and it really goes away). */
    public static void onBreak(Level world, BlockPos pos, BlockState state) {
        if (!(world instanceof ServerLevel level)) return;
        WsConfig.World cfg = WsConfig.get().world;
        if (!cfg.waterFill || level.dimensionType().ultraWarm()) return;
        boolean listed = cfg.waterFillBlocks.matches(BuiltInRegistries.BLOCK.getKey(state.getBlock()),
                tag -> state.is(TagKey.create(Registries.BLOCK, tag)));
        if (!listed || sources(level, pos) < cfg.sourcesNeeded) return;
        PENDING.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(pos.immutable());
    }

    public static void onLevelTick(Level world) {
        if (PENDING.isEmpty() || !(world instanceof ServerLevel level)) return;
        Set<BlockPos> positions = PENDING.remove(level.dimension());
        if (positions == null) return;
        WsConfig.World cfg = WsConfig.get().world;
        for (BlockPos pos : positions) {
            if (fill(level, pos) && cfg.floodHoles) flood(level, pos, cfg.sourcesNeeded);
        }
    }

    private static boolean fill(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir()) return false;
        level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
        return true;
    }

    /** Fills empty spaces right next to the new water, never higher than it and only a few blocks away. */
    private static void flood(ServerLevel level, BlockPos origin, int sourcesNeeded) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(origin);
        seen.add(origin);
        int filled = 0;
        while (!queue.isEmpty() && filled < FLOOD_LIMIT) {
            BlockPos current = queue.poll();
            for (Direction dir : SPREAD) {
                BlockPos next = current.relative(dir);
                if (!seen.add(next) || next.getY() > origin.getY() || origin.getY() - next.getY() > FLOOD_RANGE) continue;
                if (Math.abs(next.getX() - origin.getX()) > FLOOD_RANGE || Math.abs(next.getZ() - origin.getZ()) > FLOOD_RANGE) continue;
                if (!level.isLoaded(next) || sources(level, next) < sourcesNeeded) continue;
                if (fill(level, next)) {
                    filled++;
                    queue.add(next);
                }
            }
        }
    }

    private static int sources(ServerLevel level, BlockPos pos) {
        int count = 0;
        for (Direction dir : TOUCHING) {
            FluidState fluid = level.getFluidState(pos.relative(dir));
            if (fluid.is(FluidTags.WATER) && fluid.isSource()) count++;
        }
        return count;
    }
}
