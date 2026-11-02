package dev.xardas.nekoroworldsetup.mixin;

import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import dev.xardas.nekoroworldsetup.EnchantLevels;

/** The same limit for the red "Too Expensive!" text on the anvil screen. */
@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin {
    @ModifyConstant(method = "renderLabels", constant = @Constant(intValue = 40))
    private int nekoroWorldSetup$tooExpensive(int vanilla) {
        return EnchantLevels.anvilLimit(vanilla);
    }
}
