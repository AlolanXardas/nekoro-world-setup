package dev.xardas.nekoroworldsetup;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Stops lava and water from making cobblestone, stone, basalt or obsidian; the fluids stay as they were. */
public final class FluidRules {
    private FluidRules() { }

    /** True when lava and water may not make this block (the fluids then stay as they were). */
    public static boolean blocked(BlockState made) {
        WsConfig.World cfg = WsConfig.get().world;
        if (!cfg.fluidBlocks) return false;
        return (cfg.noCobblestone && made.is(Blocks.COBBLESTONE))
                || (cfg.noStone && made.is(Blocks.STONE))
                || (cfg.noBasalt && made.is(Blocks.BASALT))
                || (cfg.noObsidian && made.is(Blocks.OBSIDIAN));
    }
}
