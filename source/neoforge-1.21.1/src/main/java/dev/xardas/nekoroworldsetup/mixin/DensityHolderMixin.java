package dev.xardas.nekoroworldsetup.mixin;

import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.xardas.nekoroworldsetup.WsConfig;

/**
 * Caves are named density functions in the overworld terrain, mixed in with min(...): a negative value carves air.
 * Replacing them with a big positive constant while the terrain is being wired up removes them.
 * Tunnels: spaghetti, noodle and cave entrances (vanilla names, also used by Terralith).
 * Big caves: Terralith's cave_cheese (with its mega caves); vanilla's own cheese is inline, see NoiseMixin.
 */
@Mixin(DensityFunctions.HolderHolder.class)
public abstract class DensityHolderMixin {
    @Unique
    private static final Set<ResourceLocation> NEKORO_WORLD_SETUP$TUNNELS = Set.of(
            ResourceLocation.withDefaultNamespace("overworld/caves/spaghetti_2d"),
            ResourceLocation.withDefaultNamespace("overworld/caves/noodle"),
            ResourceLocation.withDefaultNamespace("overworld/caves/entrances"));
    @Unique
    private static final Set<ResourceLocation> NEKORO_WORLD_SETUP$BIG_CAVES = Set.of(
            ResourceLocation.fromNamespaceAndPath("terralith", "overworld/reference/cave_cheese"));

    @Shadow @Final private Holder<DensityFunction> function;

    @Inject(method = "mapAll", at = @At("HEAD"), cancellable = true)
    private void nekoroWorldSetup$noCaves(DensityFunction.Visitor visitor, CallbackInfoReturnable<DensityFunction> cir) {
        WsConfig.Worldgen cfg = WsConfig.get().worldgen;
        if (!cfg.removeSpaghettiCaves && !cfg.removeCheeseCaves) return;
        function.unwrapKey().ifPresent(key -> {
            ResourceLocation id = key.location();
            if ((cfg.removeSpaghettiCaves && NEKORO_WORLD_SETUP$TUNNELS.contains(id))
                    || (cfg.removeCheeseCaves && NEKORO_WORLD_SETUP$BIG_CAVES.contains(id))) {
                cir.setReturnValue(visitor.apply(DensityFunctions.constant(64.0)));
            }
        });
    }
}
