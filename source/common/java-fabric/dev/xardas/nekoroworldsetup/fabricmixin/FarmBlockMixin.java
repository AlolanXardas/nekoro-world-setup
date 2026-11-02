package dev.xardas.nekoroworldsetup.fabricmixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import dev.xardas.nekoroworldsetup.EnchantExtras;

/** Feather Falling keeps farmland from being trampled. */
@Mixin(FarmBlock.class)
public abstract class FarmBlockMixin {
    @WrapOperation(method = "fallOn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/FarmBlock;turnToDirt(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private void nekoroWorldSetup$trample(Entity entity, BlockState state, Level level, BlockPos pos, Operation<Void> original) {
        if (!EnchantExtras.protectsFarmland(entity)) original.call(entity, state, level, pos);
    }
}
