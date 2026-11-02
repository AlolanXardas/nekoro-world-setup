package dev.xardas.nekoroworldsetup.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import dev.xardas.nekoroworldsetup.WsConfig;

/**
 * Deep aquifers (below Y -10) turn into lava where the lava noise is strong. computeFluidType reads
 * Blocks.LAVA.defaultBlockState() twice: first to compare (ordinal 0), then as the new fluid (ordinal 1),
 * which becomes water instead.
 */
@Mixin(targets = "net.minecraft.world.level.levelgen.Aquifer$NoiseBasedAquifer")
public abstract class NoiseBasedAquiferMixin {
    @ModifyExpressionValue(method = "computeFluidType",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/Block;defaultBlockState()Lnet/minecraft/world/level/block/state/BlockState;",
                    ordinal = 1))
    private BlockState nekoroWorldSetup$noLavaAquifers(BlockState fluid) {
        return WsConfig.get().worldgen.removeDeepLava ? Blocks.WATER.defaultBlockState() : fluid;
    }
}
