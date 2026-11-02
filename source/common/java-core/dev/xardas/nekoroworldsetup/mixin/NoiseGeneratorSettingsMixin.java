package dev.xardas.nekoroworldsetup.mixin;

import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.xardas.nekoroworldsetup.WsConfig;

/** The big copper and iron veins from 1.18 come from the terrain generator itself, not from features. */
@Mixin(NoiseGeneratorSettings.class)
public abstract class NoiseGeneratorSettingsMixin {
    @Inject(method = "oreVeinsEnabled", at = @At("HEAD"), cancellable = true)
    private void nekoroWorldSetup$noOreVeins(CallbackInfoReturnable<Boolean> cir) {
        if (WsConfig.get().worldgen.removeOres) cir.setReturnValue(false);
    }
}
