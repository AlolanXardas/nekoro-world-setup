package dev.xardas.nekoroworldsetup.fabricmixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.xardas.nekoroworldsetup.MobFilter;
import dev.xardas.nekoroworldsetup.fabric.FabricMobs;

/** New entities: blocked spawns and blocked mobs that appear another way (hives, breeding...) are not added. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelAddMixin {
    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void nekoroWorldSetup$add(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (FabricMobs.blocked(entity) || MobFilter.onJoin(entity, false)) {
            cir.setReturnValue(false);
            return;
        }
        FabricMobs.fresh(entity);
    }
}
