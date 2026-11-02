package dev.xardas.nekoroworldsetup.mixin;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import dev.xardas.nekoroworldsetup.WsConfig;

/** The lava that fills every cave below Y -54 becomes air. */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {
    @ModifyArg(method = "createFluidPicker",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/Aquifer$FluidStatus;<init>(ILnet/minecraft/world/level/block/state/BlockState;)V",
                    ordinal = 0),
            index = 1)
    private static BlockState nekoroWorldSetup$noDeepLava(BlockState fluid) {
        return WsConfig.get().worldgen.removeDeepLava ? Blocks.AIR.defaultBlockState() : fluid;
    }
}
