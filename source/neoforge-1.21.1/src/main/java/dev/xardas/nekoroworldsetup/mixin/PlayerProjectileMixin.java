package dev.xardas.nekoroworldsetup.mixin;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.xardas.nekoroworldsetup.WsConfig;

/**
 * Infinity without arrows. A bow only draws when getProjectile finds ammo; with no arrow at all, a bow with
 * Infinity gets a plain arrow here. Infinity already keeps that arrow from being used up or picked up.
 */
@Mixin(Player.class)
public abstract class PlayerProjectileMixin {
    @Inject(method = "getProjectile", at = @At("RETURN"), cancellable = true)
    private void nekoroWorldSetup$infinity(ItemStack weapon, CallbackInfoReturnable<ItemStack> cir) {
        if (!cir.getReturnValue().isEmpty() || !WsConfig.get().world.infinityWithoutArrows || !(weapon.getItem() instanceof BowItem)) return;
        Player player = (Player) (Object) this;
        boolean infinity = player.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(Enchantments.INFINITY)
                .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, weapon) > 0).orElse(false);
        if (infinity) cir.setReturnValue(new ItemStack(Items.ARROW));
    }
}
