package dev.xardas.nekoroworldsetup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.resources.ResourceLocation;

/** A config list of ids ("minecraft:zombie"), whole mods ("minecraft:*") and tags ("#minecraft:raiders"). */
public final class IdList {
    public static final IdList EMPTY = new IdList(Set.of(), Set.of(), List.of());

    private final Set<ResourceLocation> ids;
    private final Set<String> namespaces;
    private final List<ResourceLocation> tags;

    private IdList(Set<ResourceLocation> ids, Set<String> namespaces, List<ResourceLocation> tags) {
        this.ids = ids;
        this.namespaces = namespaces;
        this.tags = tags;
    }

    public static IdList of(List<String> entries) {
        Set<ResourceLocation> ids = new HashSet<>();
        Set<String> namespaces = new HashSet<>();
        List<ResourceLocation> tags = new ArrayList<>();
        for (String entry : entries) {
            String e = entry.trim();
            try {
                if (e.startsWith("#")) tags.add(ResourceLocation.parse(e.substring(1)));
                else if (e.endsWith(":*")) namespaces.add(e.substring(0, e.length() - 2));
                else if (!e.isEmpty()) ids.add(ResourceLocation.parse(e));
            } catch (RuntimeException ex) {
                NekoroWorldSetup.LOGGER.warn("Skipping invalid entry \"{}\": {}", entry, ex.getMessage());
            }
        }
        return new IdList(ids, namespaces, tags);
    }

    public boolean isEmpty() {
        return ids.isEmpty() && namespaces.isEmpty() && tags.isEmpty();
    }

    /** @param hasTag tells whether the thing with this id is in the given tag */
    public boolean matches(ResourceLocation id, Predicate<ResourceLocation> hasTag) {
        if (ids.contains(id) || namespaces.contains(id.getNamespace())) return true;
        for (ResourceLocation tag : tags) {
            if (hasTag.test(tag)) return true;
        }
        return false;
    }
}
