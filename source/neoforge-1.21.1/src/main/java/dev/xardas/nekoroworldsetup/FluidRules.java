package dev.xardas.nekoroworldsetup;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Stops lava and water from making cobblestone, stone, basalt or obsidian; the fluids stay as they were. */
final class FluidRules {
    private FluidRules() { }

    static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        WsConfig.World cfg = WsConfig.get().world;
        if (!cfg.fluidBlocks) return;
        BlockState made = event.getNewState();
        boolean blocked = (cfg.noCobblestone && made.is(Blocks.COBBLESTONE))
                || (cfg.noStone && made.is(Blocks.STONE))
                || (cfg.noBasalt && made.is(Blocks.BASALT))
                || (cfg.noObsidian && made.is(Blocks.OBSIDIAN));
        if (blocked) event.setNewState(event.getOriginalState());
    }
}
