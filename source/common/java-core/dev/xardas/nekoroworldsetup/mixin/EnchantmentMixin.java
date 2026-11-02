package dev.xardas.nekoroworldsetup.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.xardas.nekoroworldsetup.EnchantLevels;

/**
 * Enchantment rules from world.json: higher maximum levels, enchantments that may be combined, and extra
 * items an enchantment may go on. The anvil, the enchanting table and /enchant all ask these methods.
 */
@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {
    @Inject(method = "getMaxLevel", at = @At("HEAD"), cancellable = true)
    private void nekoroWorldSetup$maxLevel(CallbackInfoReturnable<Integer> cir) {
        int level = EnchantLevels.maxLevel((Enchantment) (Object) this);
        if (level > 0) cir.setReturnValue(level);
    }

    @Inject(method = "areCompatible", at = @At("HEAD"), cancellable = true)
    private static void nekoroWorldSetup$compatible(Holder<Enchantment> a, Holder<Enchantment> b, CallbackInfoReturnable<Boolean> cir) {
        if (!a.equals(b) && EnchantLevels.unlocked(a, b)) cir.setReturnValue(true);
    }

    @Inject(method = {"canEnchant", "isSupportedItem", "isPrimaryItem"}, at = @At("RETURN"), cancellable = true)
    private void nekoroWorldSetup$extraItems(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && EnchantLevels.extraItem((Enchantment) (Object) this, stack)) cir.setReturnValue(true);
    }
}
