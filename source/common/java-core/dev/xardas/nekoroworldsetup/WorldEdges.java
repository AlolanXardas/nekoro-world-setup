package dev.xardas.nekoroworldsetup;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/** The wrapping edge of the world and the teleport out of the void, both for players only. */
public final class WorldEdges {
    private static final int CHECK_EVERY = 5;
    private static final int SEARCH = 48;
    /** Players teleported out of the void whose next landing does no damage. */
    private static final Set<UUID> SAFE_LANDING = new HashSet<>();

    private WorldEdges() { }

    public static void onPlayerTick(Player entity) {
        if (!(entity instanceof ServerPlayer player) || player.isSpectator()) return;
        if (!SAFE_LANDING.isEmpty() && (player.isInWater() || player.isFallFlying() || player.getAbilities().flying)) {
            SAFE_LANDING.remove(player.getUUID());
        }
        if (player.tickCount % CHECK_EVERY != 0) return;
        WsConfig.World cfg = WsConfig.get().world;
        ServerLevel level = player.serverLevel();
        if (cfg.voidTeleport && inDimension(cfg.voidDimensions, level, true)
                && player.getY() < level.getMinBuildHeight() - cfg.voidDepth) {
            fromVoid(player, level, cfg);
        } else if (cfg.borderWrap && inDimension(cfg.borderDimensions, level, false)) {
            wrap(player, level, cfg);
        }
    }

    /** True when this fall does no damage (the first landing after a void teleport). */
    public static boolean safeLanding(LivingEntity entity) {
        return entity instanceof ServerPlayer player && SAFE_LANDING.remove(player.getUUID());
    }

    private static void fromVoid(ServerPlayer player, ServerLevel level, WsConfig.World cfg) {
        double y;
        if (level.dimensionType().hasCeiling()) {
            // with a bedrock roof (the Nether), the top of the world would be above the roof
            int top = level.getMinBuildHeight() + level.dimensionType().logicalHeight() - 3;
            y = findStandingY(level, Mth.floor(player.getX()), Mth.floor(player.getZ()), top, top);
        } else {
            y = level.getMaxBuildHeight();
        }
        player.stopRiding();
        player.teleportTo(level, player.getX(), y, player.getZ(), player.getYRot(), player.getXRot());
        player.setDeltaMovement(0, 0, 0);
        player.resetFallDistance();
        if (cfg.noFallDamage) SAFE_LANDING.add(player.getUUID());
    }

    private static void wrap(ServerPlayer player, ServerLevel level, WsConfig.World cfg) {
        double x = player.getX();
        double z = player.getZ();
        double newX = x;
        double newZ = z;
        if (x > cfg.centerX + cfg.radius) newX = cfg.centerX - cfg.radius + cfg.moveInside;
        else if (x < cfg.centerX - cfg.radius) newX = cfg.centerX + cfg.radius - cfg.moveInside;
        if (z > cfg.centerZ + cfg.radius) newZ = cfg.centerZ - cfg.radius + cfg.moveInside;
        else if (z < cfg.centerZ - cfg.radius) newZ = cfg.centerZ + cfg.radius - cfg.moveInside;
        if (newX == x && newZ == z) return;

        int blockX = Mth.floor(newX);
        int blockZ = Mth.floor(newZ);
        double y;
        if (level.dimensionType().hasCeiling()) {
            int top = level.getMinBuildHeight() + level.dimensionType().logicalHeight() - 3;
            y = findStandingY(level, blockX, blockZ, Mth.floor(player.getY()), top);
        } else {
            // Level.getHeight answers the bottom of the world for chunks that are not loaded yet, so the chunk
            // is loaded fully first (generated if needed) and its own heightmap is read
            LevelChunk chunk = level.getChunk(blockX >> 4, blockZ >> 4);
            y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX & 15, blockZ & 15) + 1;
        }
        player.stopRiding();
        player.teleportTo(level, newX, y, newZ, player.getYRot(), player.getXRot());
        player.resetFallDistance();
    }

    /** The nearest spot (two free blocks above a solid one) to startY, searching up and down; startY if none. */
    private static double findStandingY(ServerLevel level, int x, int z, int startY, int maxY) {
        int minY = level.getMinBuildHeight() + 1;
        for (int offset = 0; offset <= SEARCH; offset++) {
            for (int y : new int[] {startY - offset, startY + offset}) {
                if (y < minY || y > maxY) continue;
                BlockPos feet = new BlockPos(x, y, z);
                if (level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()
                        && level.getBlockState(feet.below()).isSolidRender(level, feet.below())) {
                    return y;
                }
            }
        }
        return startY;
    }

    /** An empty list means every dimension when emptyMeansAll is true, otherwise none. */
    private static boolean inDimension(List<String> dimensions, ServerLevel level, boolean emptyMeansAll) {
        if (dimensions.isEmpty()) return emptyMeansAll;
        return dimensions.contains(level.dimension().location().toString());
    }
}
