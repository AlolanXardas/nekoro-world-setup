package dev.xardas.nekoroworldsetup;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/** Blocks mob spawns by id, mod or tag, only for the kinds of spawning listed in mobs.json. */
public final class MobFilter {
    /** Mobs that went through a normal spawn (FinalizeSpawnEvent); the rest appeared some other way. */
    private static final Set<Mob> NORMAL_SPAWNS = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private MobFilter() { }

    static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        WsConfig.Mobs cfg = WsConfig.get().mobs;
        if (!cfg.enabled) return;
        NORMAL_SPAWNS.add(event.getEntity());
        if (!cfg.spawnTypes.contains(event.getSpawnType())) return;
        if (blocked(cfg, event.getEntity().getType())) {
            event.setSpawnCancelled(true);
            event.setCanceled(true);
        }
    }

    static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) return;
        WsConfig.Mobs cfg = WsConfig.get().mobs;
        if (!cfg.enabled) return;
        if (event.loadedFromDisk()) {
            // mobs saved in chunks from before the filter; only with "remove_existing"
            if (!cfg.removeExisting || !blocked(cfg, mob.getType())) return;
            if (mob.hasCustomName() || mob.isLeashed() || mob.isPersistenceRequired()) return;
            if (mob instanceof OwnableEntity owned && owned.getOwnerUUID() != null) return;
            if (mob instanceof AbstractHorse horse && horse.isTamed()) return;
            event.setCanceled(true);
        } else if (!NORMAL_SPAWNS.remove(mob) && cfg.blockOtherSpawns && blocked(cfg, mob.getType())) {
            // a new mob without a normal spawn: a bee leaving its hive, a baby from breeding, a conversion...
            event.setCanceled(true);
        }
    }

    /** True when "block_other_spawns" keeps this mob from appearing outside a normal spawn (used by the beehive). */
    public static boolean blocksOtherSpawn(EntityType<?> type) {
        WsConfig.Mobs cfg = WsConfig.get().mobs;
        return cfg.enabled && cfg.blockOtherSpawns && blocked(cfg, type);
    }

    private static boolean blocked(WsConfig.Mobs cfg, EntityType<?> type) {
        boolean listed = cfg.mobs.matches(BuiltInRegistries.ENTITY_TYPE.getKey(type),
                tag -> type.is(TagKey.create(Registries.ENTITY_TYPE, tag)));
        return cfg.allowMode != listed;
    }
}
