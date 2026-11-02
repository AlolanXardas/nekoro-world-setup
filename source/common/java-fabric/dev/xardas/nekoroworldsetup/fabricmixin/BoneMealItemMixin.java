package dev.xardas.nekoroworldsetup.fabricmixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.xardas.nekoroworldsetup.BoneMeal;

/** Bone meal on plants vanilla ignores (players and dispensers both go through growCrop). */
@Mixin(BoneMealItem.class)
public abstract class BoneMealItemMixin {
    @Inject(method = "growCrop", at = @At("HEAD"), cancellable = true)
    private static void nekoroWorldSetup$moreBoneMeal(ItemStack stack, Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (BoneMeal.tryUse(level, pos, level.getBlockState(pos), stack)) cir.setReturnValue(true);
    }
}
