package dev.xardas.nekoroworldsetup.worldgen;

import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.GeodeConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SpringConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.Fluids;

import dev.xardas.nekoroworldsetup.IdList;
import dev.xardas.nekoroworldsetup.WsConfig;

/**
 * Which ores, geodes, lava lakes and springs worldgen.json takes out of biomes, vanilla and modded.
 * The loaders apply it: a NeoForge biome modifier, Fabric's biome modification API.
 */
public final class BiomeCleanup {
    private BiomeCleanup() { }

    /** The common c:ores block tag (the same name on NeoForge and Fabric). */
    private static final TagKey<Block> ORES_TAG = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores"));

    /** "ore" or "ores" as a whole word in an id: ore_iron, iron_ore, deepslate_ores, but not "core" or "forest". */
    private static final Pattern ORE_WORD = Pattern.compile("(^|[_/])ores?($|[_/])");

    /** How many features were taken out of biomes, for the summary in the log when the server has started. */
    private static final java.util.concurrent.atomic.AtomicInteger ORES = new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicInteger GEODES = new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicInteger LAVA = new java.util.concurrent.atomic.AtomicInteger();

    public static void logSummary() {
        if (ORES.get() + GEODES.get() + LAVA.get() == 0) return;
        dev.xardas.nekoroworldsetup.NekoroWorldSetup.LOGGER.info("Removed from biomes: {} ore features, {} geodes, {} lava lakes and springs (counted per biome)",
                ORES.getAndSet(0), GEODES.getAndSet(0), LAVA.getAndSet(0));
    }

    /** True when worldgen.json removes this feature from this generation step. */
    public static boolean shouldRemove(WsConfig.Worldgen cfg, GenerationStep.Decoration step, Holder<PlacedFeature> feature) {
        Optional<ResourceLocation> id = feature.unwrapKey().map(ResourceKey::location);
        FeatureConfiguration config = feature.value().feature().value().config();
        if (cfg.removeOres && isOre(id, config) && !kept(cfg.keepOres, id, config)) return count(ORES);
        if (cfg.removeLavaLakes && step == GenerationStep.Decoration.LAKES) {
            if (config instanceof LakeFeature.Configuration lake) {
                if (lake.fluid().getState(RandomSource.create(0), BlockPos.ZERO).is(Blocks.LAVA)) return count(LAVA);
            } else if (id.isPresent() && id.get().getPath().contains("lava")) {
                return count(LAVA);
            }
        }
        // features like Terralith's amethyst geode are a random choice between several geodes, so look inside too
        if (cfg.removeGeodes && nested(feature).anyMatch(c -> c instanceof GeodeConfiguration)) return count(GEODES);
        if (cfg.removeLavaSprings && config instanceof SpringConfiguration spring && spring.state.getType().isSame(Fluids.LAVA)) {
            return count(LAVA);
        }
        return false;
    }

    /**
     * Ore features (OreConfiguration, also used for ancient debris) count when one of their target
     * blocks is an ore; that keeps granite, dirt and gravel blobs. Other feature types count by name.
     */
    private static boolean isOre(Optional<ResourceLocation> id, FeatureConfiguration config) {
        if (config instanceof OreConfiguration ore) {
            for (OreConfiguration.TargetBlockState target : ore.targetStates) {
                if (isOreBlock(target.state)) return true;
            }
            return false;
        }
        if (config.getFeatures().anyMatch(inner -> inner.config() instanceof OreConfiguration ore
                && ore.targetStates.stream().anyMatch(t -> isOreBlock(t.state)))) return true;
        return id.isPresent() && ORE_WORD.matcher(id.get().getPath()).find();
    }

    private static boolean isOreBlock(BlockState state) {
        if (state.is(ORES_TAG)) return true;
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return ORE_WORD.matcher(path).find() || path.equals("ancient_debris");
    }

    private static boolean kept(IdList keep, Optional<ResourceLocation> id, FeatureConfiguration config) {
        if (keep.isEmpty()) return false;
        if (id.isPresent() && keep.matches(id.get(), tag -> false)) return true;
        if (config instanceof OreConfiguration ore) {
            for (OreConfiguration.TargetBlockState target : ore.targetStates) {
                BlockState state = target.state;
                if (keep.matches(BuiltInRegistries.BLOCK.getKey(state.getBlock()),
                        tag -> state.is(TagKey.create(Registries.BLOCK, tag)))) return true;
            }
        }
        return false;
    }

    private static boolean count(java.util.concurrent.atomic.AtomicInteger counter) {
        counter.incrementAndGet();
        return true;
    }

    /** The feature's own configuration and those of every feature nested inside it. */
    private static Stream<FeatureConfiguration> nested(Holder<PlacedFeature> feature) {
        return feature.value().feature().value().getFeatures().map(ConfiguredFeature::config);
    }
}
