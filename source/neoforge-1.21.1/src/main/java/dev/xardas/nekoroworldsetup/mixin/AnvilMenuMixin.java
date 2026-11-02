package dev.xardas.nekoroworldsetup.mixin;

import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import dev.xardas.nekoroworldsetup.EnchantLevels;

/**
 * The "Too Expensive!" limit. createResult has three 40s: the first one (ordinal 0) is the cost
 * of a stacked item and stays; ordinal 1 caps renaming-only at 39, ordinal 2 refuses the result.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 40, ordinal = 1))
    private int nekoroWorldSetup$renameCheck(int vanilla) {
        return EnchantLevels.anvilLimit(vanilla);
    }

    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 39))
    private int nekoroWorldSetup$renameCap(int vanilla) {
        int limit = EnchantLevels.anvilLimit(vanilla + 1);
        return limit - 1;
    }

    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 40, ordinal = 2))
    private int nekoroWorldSetup$tooExpensive(int vanilla) {
        return EnchantLevels.anvilLimit(vanilla);
    }
}
