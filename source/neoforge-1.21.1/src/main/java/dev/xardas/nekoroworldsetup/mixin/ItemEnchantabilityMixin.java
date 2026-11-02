package dev.xardas.nekoroworldsetup.mixin;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShieldItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.xardas.nekoroworldsetup.WsConfig;

/**
 * The enchanting table refuses items with an enchantability of 0, which is why shears and shields only take
 * enchantments in an anvil. Giving them a medium value (like iron armor) lets the table offer their enchantments.
 */
@Mixin(Item.class)
public abstract class ItemEnchantabilityMixin {
    @Inject(method = "getEnchantmentValue", at = @At("HEAD"), cancellable = true)
    private void nekoroWorldSetup$enchantability(CallbackInfoReturnable<Integer> cir) {
        WsConfig.World cfg = WsConfig.get().world;
        Object self = this;
        if ((cfg.enchantableShears && self instanceof ShearsItem) || (cfg.enchantableShields && self instanceof ShieldItem)) {
            cir.setReturnValue(9);
        }
    }
}
