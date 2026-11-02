package dev.xardas.nekoroworldsetup.mixin;

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
 * Vanilla's big cheese caves are written straight into the terrain: 0.27 + noise("cave_cheese"), air where it
 * is below zero. Reading that noise as a constant 1 keeps the sum positive everywhere, so no cheese caves form.
 */
@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$Noise")
public abstract class NoiseMixin {
    @Unique
    private static final ResourceLocation NEKORO_WORLD_SETUP$CHEESE = ResourceLocation.withDefaultNamespace("cave_cheese");

    @Shadow @Final private DensityFunction.NoiseHolder noise;

    @Inject(method = "mapAll", at = @At("HEAD"), cancellable = true)
    private void nekoroWorldSetup$noCheese(DensityFunction.Visitor visitor, CallbackInfoReturnable<DensityFunction> cir) {
        if (!WsConfig.get().worldgen.removeCheeseCaves) return;
        noise.noiseData().unwrapKey().ifPresent(key -> {
            if (key.location().equals(NEKORO_WORLD_SETUP$CHEESE)) cir.setReturnValue(visitor.apply(DensityFunctions.constant(1.0)));
        });
    }
}
