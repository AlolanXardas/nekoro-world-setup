package dev.xardas.nekoroworldsetup.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import dev.xardas.nekoroworldsetup.WsConfig;

/**
 * Every Mending level above I repairs mending_bonus_per_level more per XP point (0.5: II 1.5x, X 5.5x).
 * The orb then keeps the XP it did not need, so a higher level also uses less XP for the same repair.
 */
@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMendingMixin {
    @WrapOperation(method = "repairPlayerItems", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;modifyDurabilityToRepairFromXp(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I"))
    private int nekoroWorldSetup$mendingLevel(ServerLevel level, ItemStack stack, int durability, Operation<Integer> original) {
        int repair = original.call(level, stack, durability);
        double bonus = WsConfig.get().world.mendingBonusPerLevel;
        if (bonus <= 0) return repair;
        int mending = EnchantmentHelper.getItemEnchantmentLevel(
                level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.MENDING), stack);
        return mending > 1 ? (int) Math.round(repair * (1 + bonus * (mending - 1))) : repair;
    }
}
