package dev.xardas.nekoroworldsetup.fabricmixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.LavaFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import dev.xardas.nekoroworldsetup.FluidRules;

/** Lava flowing down into water: no stone when fluid_blocks says so; the water stays. */
@Mixin(LavaFluid.class)
public abstract class LavaFluidMixin {
    @WrapOperation(method = "spreadTo", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/LevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean nekoroWorldSetup$stone(LevelAccessor level, BlockPos pos, BlockState made, int flags, Operation<Boolean> original) {
        return FluidRules.blocked(made) ? false : original.call(level, pos, made, flags);
    }
}
