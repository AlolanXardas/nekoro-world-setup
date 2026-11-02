package dev.xardas.nekoroworldsetup.neoforge;

import java.nio.file.Path;

import net.neoforged.fml.loading.FMLPaths;

import dev.xardas.nekoroworldsetup.Platform;

public final class NeoForgePlatform implements Platform {
    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }
}
