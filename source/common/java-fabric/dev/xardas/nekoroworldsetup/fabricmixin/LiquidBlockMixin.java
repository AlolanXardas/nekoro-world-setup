package dev.xardas.nekoroworldsetup.fabricmixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import dev.xardas.nekoroworldsetup.FluidRules;

/** Lava meeting water: no cobblestone, obsidian or basalt when fluid_blocks says so; the fluids stay. */
@Mixin(LiquidBlock.class)
public abstract class LiquidBlockMixin {
    @WrapOperation(method = "shouldSpreadLiquid", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean nekoroWorldSetup$fluidBlock(Level level, BlockPos pos, BlockState made, Operation<Boolean> original) {
        return FluidRules.blocked(made) ? false : original.call(level, pos, made);
    }
}
