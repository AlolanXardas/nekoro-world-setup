package dev.xardas.nekoroworldsetup;

import net.minecraft.server.packs.repository.RepositorySource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.validation.DirectoryValidator;

/** Datapacks from config/Nekoro/WorldSetup/datapacks, added to every world (replaces mods like Paxi). */
public final class GlobalDatapacks {
    // true = packs from this folder are turned on automatically in every world
    private static final PackSource SOURCE = PackSource.create(
            name -> Component.translatable("pack.nameAndSource", name, Component.literal("Nekoro World Setup"))
                    .withStyle(ChatFormatting.GRAY),
            true);

    private GlobalDatapacks() { }

    /** The extra datapack source for the server, or null when global datapacks are off. */
    public static RepositorySource source() {
        if (!WsConfig.get().start.globalDatapacks) return null;
        Path folder = WsConfig.dir().resolve("datapacks");
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            NekoroWorldSetup.LOGGER.error("Could not create {}", folder, e);
            return null;
        }
        NekoroWorldSetup.LOGGER.info("Loading global datapacks from {}", folder);
        return new FolderRepositorySource(folder, PackType.SERVER_DATA, SOURCE, new DirectoryValidator(path -> true));
    }
}
