package dev.xardas.nekoroworldsetup.fabricmixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import dev.xardas.nekoroworldsetup.EnchantExtras;

/** Fortune raising the XP from ores. */
@Mixin(Block.class)
public abstract class BlockXpMixin {
    @ModifyExpressionValue(method = "tryDropExperience", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;processBlockExperience(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I"))
    private int nekoroWorldSetup$fortuneXp(int xp, @Local(argsOnly = true) ServerLevel level, @Local(argsOnly = true) ItemStack tool) {
        return EnchantExtras.blockXp(level, tool, xp);
    }
}
