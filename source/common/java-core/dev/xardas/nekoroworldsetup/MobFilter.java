package dev.xardas.nekoroworldsetup;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
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

/** Blocks mob spawns by id, mod or tag, only for the kinds of spawning listed in mobs.json. */
public final class MobFilter {
    /** Mobs that went through a normal spawn (FinalizeSpawnEvent); the rest appeared some other way. */
    private static final Set<Mob> NORMAL_SPAWNS = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private MobFilter() { }

    /** A mob finishing a normal spawn. True = this spawn is blocked. */
    public static boolean onFinalizeSpawn(Mob mob, MobSpawnType type) {
        WsConfig.Mobs cfg = WsConfig.get().mobs;
        if (!cfg.enabled) return false;
        NORMAL_SPAWNS.add(mob);
        return cfg.spawnTypes.contains(type) && blocked(cfg, mob.getType());
    }

    /** A mob joining a server level, new or loaded from disk. True = keep it out. */
    public static boolean onJoin(Entity entity, boolean loadedFromDisk) {
        if (entity.level().isClientSide() || !(entity instanceof Mob mob)) return false;
        WsConfig.Mobs cfg = WsConfig.get().mobs;
        if (!cfg.enabled) return false;
        if (loadedFromDisk) {
            // mobs saved in chunks from before the filter; only with "remove_existing"
            if (!cfg.removeExisting || !blocked(cfg, mob.getType())) return false;
            if (mob.hasCustomName() || mob.isLeashed() || mob.isPersistenceRequired()) return false;
            if (mob instanceof OwnableEntity owned && owned.getOwnerUUID() != null) return false;
            return !(mob instanceof AbstractHorse horse && horse.isTamed());
        }
        // a new mob without a normal spawn: a bee leaving its hive, a baby from breeding, a conversion...
        return !NORMAL_SPAWNS.remove(mob) && cfg.blockOtherSpawns && blocked(cfg, mob.getType());
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
