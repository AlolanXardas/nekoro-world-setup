package dev.xardas.nekoroworldsetup;

import net.minecraft.core.RegistryAccess;
import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * New maximum levels and extra items, looked up by EnchantmentMixin. Enchantments are data-driven objects that
 * do not know their own id, so everything is worked out once per registry load and kept by object identity.
 * The server and the client have separate registry objects, so each side has its own maps.
 */
public final class EnchantLevels {
    private record Side(Map<Enchantment, Integer> levels, Map<Enchantment, IdList> extraItems) { }

    private static final Side EMPTY = new Side(Map.of(), Map.of());
    private static volatile Side server = EMPTY;
    private static volatile Side client = EMPTY;

    private EnchantLevels() { }

    /** After tags load: on the server (fromServer false) or from the server's data on a client (true). */
    public static void rebuild(RegistryAccess registries, boolean fromServer) {
        WsConfig.World cfg = WsConfig.get().world;
        Map<Enchantment, Integer> levels = new IdentityHashMap<>();
        Map<Enchantment, IdList> extra = new IdentityHashMap<>();
        Registry<Enchantment> registry = registries.registryOrThrow(Registries.ENCHANTMENT);
        for (Map.Entry<ResourceKey<Enchantment>, Enchantment> entry : registry.entrySet()) {
            Enchantment enchantment = entry.getValue();
            ResourceLocation id = entry.getKey().location();
            IdList items = cfg.extraItems.get(id);
            if (items != null) extra.put(enchantment, items);
            if (!cfg.enchantments) continue;
            int vanilla = enchantment.definition().maxLevel();
            Integer override = cfg.levelOverrides.get(id);
            int level;
            if (override != null) level = override;
            else if (cfg.skipSingleLevel && vanilla <= 1) level = vanilla;
            else level = Math.max(vanilla, cfg.maxLevel);
            if (level != vanilla) levels.put(enchantment, level);
        }
        Side side = new Side(levels, extra);
        if (fromServer) {
            client = side;
        } else {
            server = side;
            if (!levels.isEmpty()) NekoroWorldSetup.LOGGER.info("Changed the maximum level of {} enchantments", levels.size());
        }
    }

    /** @return the new maximum level, or -1 to keep the enchantment's own */
    public static int maxLevel(Enchantment enchantment) {
        Integer level = server.levels().get(enchantment);
        if (level == null) level = client.levels().get(enchantment);
        return level == null ? -1 : level;
    }

    /** True when "extra_items" lets this enchantment go on this item. */
    public static boolean extraItem(Enchantment enchantment, ItemStack stack) {
        IdList items = server.extraItems().get(enchantment);
        if (items == null) items = client.extraItems().get(enchantment);
        return items != null && items.matches(BuiltInRegistries.ITEM.getKey(stack.getItem()),
                tag -> stack.is(TagKey.create(Registries.ITEM, tag)));
    }

    /** True when "compatible" allows these two (normally exclusive) enchantments together. */
    public static boolean unlocked(Holder<Enchantment> a, Holder<Enchantment> b) {
        WsConfig.World cfg = WsConfig.get().world;
        if (cfg.allCompatible) return true;
        for (ResourceLocation tag : cfg.compatibleTags) {
            TagKey<Enchantment> key = TagKey.create(Registries.ENCHANTMENT, tag);
            if (a.is(key) && b.is(key)) return true;
        }
        return false;
    }

    /** The anvil's "Too Expensive!" limit (vanilla 40). */
    public static int anvilLimit(int vanilla) {
        WsConfig.World cfg = WsConfig.get().world;
        if (!cfg.enchantments || cfg.anvilCostLimit == vanilla) return vanilla;
        return cfg.anvilCostLimit <= 0 ? Integer.MAX_VALUE : cfg.anvilCostLimit;
    }
}
