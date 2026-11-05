package dev.xardas.nekoroworldsetup.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.neoforged.neoforge.common.world.BiomeGenerationSettingsBuilder;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

import dev.xardas.nekoroworldsetup.WsConfig;

/**
 * One biome modifier (data/nekoroworldsetup/neoforge/biome_modifier/config.json) that reads worldgen.json
 * and removes ores, geodes, lava lakes, lava springs and carvers from every biome, vanilla and modded.
 */
public final class WorldSetupBiomeModifier implements BiomeModifier {
    public static final WorldSetupBiomeModifier INSTANCE = new WorldSetupBiomeModifier();
    public static final MapCodec<WorldSetupBiomeModifier> CODEC = MapCodec.unit(INSTANCE);

    private WorldSetupBiomeModifier() { }

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.REMOVE) return;
        WsConfig.Worldgen cfg = WsConfig.get().worldgen;
        BiomeGenerationSettingsBuilder generation = builder.getGenerationSettings();
        if (cfg.removeOres || cfg.removeLavaLakes || cfg.removeLavaSprings || cfg.removeGeodes) {
            for (GenerationStep.Decoration step : GenerationStep.Decoration.values()) {
                generation.getFeatures(step).removeIf(feature -> BiomeCleanup.shouldRemove(cfg, step, feature));
            }
        }
        if (cfg.removeCarvers) {
            for (GenerationStep.Carving step : GenerationStep.Carving.values()) {
                generation.getCarvers(step).clear();
            }
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
