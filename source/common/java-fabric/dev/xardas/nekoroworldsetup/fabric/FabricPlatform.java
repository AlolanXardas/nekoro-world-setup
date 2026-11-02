package dev.xardas.nekoroworldsetup.fabric;

import java.nio.file.Path;

import net.fabricmc.loader.api.FabricLoader;

import dev.xardas.nekoroworldsetup.Platform;

public final class FabricPlatform implements Platform {
    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
