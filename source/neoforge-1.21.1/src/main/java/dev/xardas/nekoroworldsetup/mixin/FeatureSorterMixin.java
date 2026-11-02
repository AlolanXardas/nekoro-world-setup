package dev.xardas.nekoroworldsetup.mixin;

import java.util.List;
import java.util.function.Function;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Mixin;

import dev.xardas.nekoroworldsetup.NekoroWorldSetup;
import dev.xardas.nekoroworldsetup.WsConfig;
import dev.xardas.nekoroworldsetup.worldgen.FeatureCycleReport;
import dev.xardas.nekoroworldsetup.worldgen.FeatureOrderFix;

/**
 * "Feature order cycle found": two biome mods put the same features in opposite orders and world generation
 * crashes. The features in the loop are always logged; with fix_feature_order the world generates anyway.
 */
@Mixin(FeatureSorter.class)
public abstract class FeatureSorterMixin {
    @WrapMethod(method = "buildFeaturesPerStep")
    private static <T> List<FeatureSorter.StepFeatureData> nekoroWorldSetup$featureOrder(List<T> sources,
            Function<T, List<HolderSet<PlacedFeature>>> toFeatures, boolean findSources,
            Operation<List<FeatureSorter.StepFeatureData>> original) {
        try {
            return original.call(sources, toFeatures, findSources);
        } catch (IllegalStateException e) {
            // vanilla calls itself again with findSources = false while narrowing down the biomes; act only once
            if (!findSources) throw e;
            FeatureCycleReport.log(sources, toFeatures);
            if (!WsConfig.get().worldgen.fixFeatureOrder) throw e;
            NekoroWorldSetup.LOGGER.warn("Feature order cycle fixed by Nekoro World Setup (fix_feature_order); generation continues");
            return FeatureOrderFix.sort(sources, toFeatures);
        }
    }
}
