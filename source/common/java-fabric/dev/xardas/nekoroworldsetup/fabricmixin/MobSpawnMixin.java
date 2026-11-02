package dev.xardas.nekoroworldsetup.fabricmixin;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.xardas.nekoroworldsetup.MobFilter;
import dev.xardas.nekoroworldsetup.fabric.FabricMobs;

/** A mob finishing a normal spawn; a blocked one is then kept out when it is added to the level. */
@Mixin(Mob.class)
public abstract class MobSpawnMixin {
    @Inject(method = "finalizeSpawn", at = @At("HEAD"))
    private void nekoroWorldSetup$spawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type,
                                        SpawnGroupData data, CallbackInfoReturnable<SpawnGroupData> cir) {
        Mob mob = (Mob) (Object) this;
        if (MobFilter.onFinalizeSpawn(mob, type)) FabricMobs.block(mob);
    }
}
