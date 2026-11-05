package dev.xardas.nekoroworldsetup;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;

/**
 * The time of day follows the clock of the computer the server runs on. Game time 0 is 6:00, one hour is 1000 ticks,
 * and every real day is one Minecraft day, so the moon phases follow real days too. The clock is read only every few
 * seconds, not every tick.
 */
public final class RealTime {
    private RealTime() { }

    private static int ticks;

    /** At the end of every server tick. */
    public static void onServerTick(MinecraftServer server) {
        WsConfig.World cfg = WsConfig.get().world;
        if (!cfg.realTime) return;
        if (ticks++ % (cfg.realTimeCheckSeconds * 20) != 0) return;
        GameRules.BooleanValue cycle = server.getGameRules().getRule(GameRules.RULE_DAYLIGHT);
        // the game would move the sun on its own between two checks
        if (cycle.get()) cycle.set(false, server);
        long dayTime = dayTime(LocalDateTime.now().plusMinutes(Math.round(cfg.realTimeOffsetHours * 60)));
        for (ServerLevel level : server.getAllLevels()) {
            level.setDayTime(dayTime);
        }
    }

    /** Seconds since 1970 by the local clock, shifted so that 6:00 is the start of a Minecraft day. */
    static long dayTime(LocalDateTime now) {
        long seconds = now.toEpochSecond(ZoneOffset.UTC) - 6 * 3600;
        return Math.floorDiv(seconds * 10, 36);
    }
}
