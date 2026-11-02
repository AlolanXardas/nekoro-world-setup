package dev.xardas.nekoroworldsetup.mixin;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.xardas.nekoroworldsetup.MobFilter;

/**
 * A blocked bee can never leave its hive, so the hive would try again every tick and play the exit sound each
 * time. Reporting it as released makes the hive drop it: no sound, no bee, and the hive stops trying.
 */
@Mixin(BeehiveBlockEntity.class)
public abstract class BeehiveMixin {
    @Inject(method = "releaseOccupant", at = @At("HEAD"), cancellable = true)
    private static void nekoroWorldSetup$dropBlockedBee(Level level, BlockPos pos, BlockState state, BeehiveBlockEntity.Occupant occupant,
            @Nullable List<Entity> released, BeehiveBlockEntity.BeeReleaseStatus status, @Nullable BlockPos flowerPos,
            CallbackInfoReturnable<Boolean> cir) {
        if (level.isClientSide()) return;
        EntityType<?> type = EntityType.by(occupant.entityData().copyTag()).orElse(EntityType.BEE);
        if (MobFilter.blocksOtherSpawn(type)) cir.setReturnValue(true);
    }
}
