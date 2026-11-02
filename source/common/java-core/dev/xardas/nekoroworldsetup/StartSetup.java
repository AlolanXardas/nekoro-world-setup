package dev.xardas.nekoroworldsetup;

import java.util.Map;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Game rules and commands when a world starts or a player joins. */
public final class StartSetup {
    private StartSetup() { }

    public static void onServerStarted(MinecraftServer server) {
        dev.xardas.nekoroworldsetup.worldgen.BiomeCleanup.logSummary();
        WsConfig.Start cfg = WsConfig.get().start;
        SetupData data = SetupData.get(server);
        boolean firstStart = !data.worldSetupDone;
        CommandSourceStack source = server.createCommandSourceStack();

        if (cfg.gamerules && (firstStart || cfg.gamerulesEveryStart)) {
            for (Map.Entry<String, String> rule : cfg.rules.entrySet()) {
                run(server, source, "gamerule " + rule.getKey() + " " + rule.getValue());
            }
            NekoroWorldSetup.LOGGER.info("Set {} game rules", cfg.rules.size());
        }
        if (cfg.commands) {
            for (WsConfig.StartCommand cmd : cfg.commandList) {
                if (cmd.when() == WsConfig.When.SERVER_START || (firstStart && cmd.when() == WsConfig.When.WORLD_CREATED)) {
                    run(server, source, cmd.command());
                }
            }
        }
        if (firstStart) {
            data.worldSetupDone = true;
            data.setDirty();
        }
    }

    public static void onPlayerJoin(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        SetupData data = SetupData.get(server);
        // players are remembered even while commands are off, so turning them on later does not
        // treat everyone as new
        boolean firstJoin = data.joined.add(player.getUUID());
        if (firstJoin) data.setDirty();

        WsConfig.Start cfg = WsConfig.get().start;
        if (!cfg.commands) return;
        CommandSourceStack source = player.createCommandSourceStack().withPermission(4);
        for (WsConfig.StartCommand cmd : cfg.commandList) {
            if (cmd.when() == WsConfig.When.PLAYER_JOIN || (firstJoin && cmd.when() == WsConfig.When.PLAYER_FIRST_JOIN)) {
                run(server, source, cmd.command());
            }
        }
    }

    private static void run(MinecraftServer server, CommandSourceStack source, String command) {
        if (command.isBlank()) return;
        server.getCommands().performPrefixedCommand(source, command);
    }
}
