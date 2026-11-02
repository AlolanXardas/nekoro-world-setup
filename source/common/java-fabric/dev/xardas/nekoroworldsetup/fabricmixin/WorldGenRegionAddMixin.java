package dev.xardas.nekoroworldsetup.fabricmixin;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.xardas.nekoroworldsetup.fabric.FabricMobs;

/** Mobs placed while chunks generate (structures): blocked spawns are not added. */
@Mixin(WorldGenRegion.class)
public abstract class WorldGenRegionAddMixin {
    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void nekoroWorldSetup$add(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (FabricMobs.blocked(entity)) cir.setReturnValue(false);
    }
}
