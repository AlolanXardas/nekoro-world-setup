package dev.xardas.nekoroworldsetup.fabricmixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.xardas.nekoroworldsetup.EnchantExtras;
import dev.xardas.nekoroworldsetup.WorldEdges;

/** No fall damage after a void teleport, and Looting raising mob XP (NeoForge has events for both). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityFabricMixin {
    @Shadow protected Player lastHurtByPlayer;

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void nekoroWorldSetup$safeLanding(float distance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (WorldEdges.safeLanding((LivingEntity) (Object) this)) cir.setReturnValue(false);
    }

    @ModifyArg(method = "dropExperience", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"), index = 2)
    private int nekoroWorldSetup$lootingXp(int xp) {
        return EnchantExtras.mobXp((LivingEntity) (Object) this, lastHurtByPlayer, xp);
    }
}
