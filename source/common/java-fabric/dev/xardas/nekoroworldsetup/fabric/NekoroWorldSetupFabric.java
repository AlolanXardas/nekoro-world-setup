package dev.xardas.nekoroworldsetup.fabric;

import java.util.List;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import dev.xardas.nekoroworldsetup.EnchantExtras;
import dev.xardas.nekoroworldsetup.EnchantLevels;
import dev.xardas.nekoroworldsetup.MobFilter;
import dev.xardas.nekoroworldsetup.NekoroWorldSetup;
import dev.xardas.nekoroworldsetup.RecipeTweaks;
import dev.xardas.nekoroworldsetup.StartSetup;
import dev.xardas.nekoroworldsetup.WaterFill;
import dev.xardas.nekoroworldsetup.WorldEdges;
import dev.xardas.nekoroworldsetup.WsConfig;
import dev.xardas.nekoroworldsetup.worldgen.BiomeCleanup;

/**
 * Fabric entry point: connects Fabric API events to the shared code. Spawns, fluids, bone meal, falls,
 * farmland, XP and global datapacks come from the mixins in fabricmixin (Fabric API has no events for them).
 */
public final class NekoroWorldSetupFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        WsConfig.load();
        // config files are read again whenever datapacks (re)load, before recipes are built
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, resources) -> WsConfig.load());
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> EnchantLevels.rebuild(registries, client));
        ServerLifecycleEvents.SERVER_STARTING.register(RecipeTweaks::apply);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
            if (success) RecipeTweaks.apply(server);
        });
        ServerLifecycleEvents.SERVER_STARTED.register(StartSetup::onServerStarted);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> StartSetup.onPlayerJoin(handler.getPlayer()));

        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> WaterFill.onBreak(level, pos, state));
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            WaterFill.onLevelTick(level);
            FabricMobs.removeQueued(level);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(WorldEdges::onPlayerTick));
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> EnchantExtras.afterDamage(entity, source));
        // mobs saved in chunks; new ones were already checked when they were added (see FabricMobs)
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (!FabricMobs.wasFresh(entity) && MobFilter.onJoin(entity, true)) FabricMobs.queueRemoval(entity);
        });

        BiomeModifications.create(ResourceLocation.fromNamespaceAndPath(NekoroWorldSetup.MOD_ID, "config"))
                .add(ModificationPhase.REMOVALS, BiomeSelectors.all(), (selection, context) -> {
                    WsConfig.Worldgen cfg = WsConfig.get().worldgen;
                    BiomeGenerationSettings settings = selection.getBiome().getGenerationSettings();
                    var generation = context.getGenerationSettings();
                    if (cfg.removeOres || cfg.removeLavaLakes || cfg.removeLavaSprings || cfg.removeGeodes) {
                        List<HolderSet<PlacedFeature>> steps = settings.features();
                        GenerationStep.Decoration[] names = GenerationStep.Decoration.values();
                        for (int i = 0; i < steps.size() && i < names.length; i++) {
                            GenerationStep.Decoration step = names[i];
                            for (Holder<PlacedFeature> feature : steps.get(i)) {
                                if (BiomeCleanup.shouldRemove(cfg, step, feature)) {
                                    feature.unwrapKey().ifPresent(key -> generation.removeFeature(step, key));
                                }
                            }
                        }
                    }
                    if (cfg.removeCarvers) {
                        for (GenerationStep.Carving step : GenerationStep.Carving.values()) {
                            for (Holder<ConfiguredWorldCarver<?>> carver : settings.getCarvers(step)) {
                                carver.unwrapKey().ifPresent(key -> generation.removeCarver(step, key));
                            }
                        }
                    }
                });
    }
}
