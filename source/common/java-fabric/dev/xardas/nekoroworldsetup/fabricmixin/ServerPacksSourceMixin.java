package dev.xardas.nekoroworldsetup.fabricmixin;

import java.util.Arrays;

import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.repository.ServerPacksSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import dev.xardas.nekoroworldsetup.GlobalDatapacks;

/** Global datapacks: one more datapack source for every world (NeoForge has an event for this). */
@Mixin(ServerPacksSource.class)
public abstract class ServerPacksSourceMixin {
    @ModifyArg(method = "createPackRepository(Ljava/nio/file/Path;Lnet/minecraft/world/level/validation/DirectoryValidator;)Lnet/minecraft/server/packs/repository/PackRepository;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/repository/PackRepository;<init>([Lnet/minecraft/server/packs/repository/RepositorySource;)V"))
    private static RepositorySource[] nekoroWorldSetup$globalDatapacks(RepositorySource[] sources) {
        RepositorySource extra = GlobalDatapacks.source();
        if (extra == null) return sources;
        RepositorySource[] out = Arrays.copyOf(sources, sources.length + 1);
        out[sources.length] = extra;
        return out;
    }
}
