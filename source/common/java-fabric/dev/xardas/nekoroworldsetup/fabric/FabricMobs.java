package dev.xardas.nekoroworldsetup.fabric;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * Bookkeeping for the mob filter on Fabric, where spawns are checked by mixins: mobs whose normal spawn was
 * blocked, mobs just added (so the chunk load event does not treat them as old ones), and old mobs to remove.
 */
public final class FabricMobs {
    private static final Set<Mob> BLOCKED = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private static final Set<Entity> FRESH = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private static final Set<Entity> TO_REMOVE = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private FabricMobs() { }

    public static void block(Mob mob) {
        BLOCKED.add(mob);
    }

    public static boolean blocked(Entity entity) {
        return entity instanceof Mob mob && BLOCKED.remove(mob);
    }

    public static void fresh(Entity entity) {
        FRESH.add(entity);
    }

    static boolean wasFresh(Entity entity) {
        return FRESH.remove(entity);
    }

    /** Removing an entity while the level is still loading it is not safe, so it waits until the end of the tick. */
    static void queueRemoval(Entity entity) {
        TO_REMOVE.add(entity);
    }

    static void removeQueued(ServerLevel level) {
        if (TO_REMOVE.isEmpty()) return;
        synchronized (TO_REMOVE) {
            TO_REMOVE.removeIf(entity -> {
                if (entity.level() != level) return false;
                if (!entity.isRemoved()) entity.discard();
                return true;
            });
        }
    }
}
