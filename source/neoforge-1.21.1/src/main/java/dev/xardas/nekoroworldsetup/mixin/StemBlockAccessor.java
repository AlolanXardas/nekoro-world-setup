package dev.xardas.nekoroworldsetup.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StemBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Which fruit a melon or pumpkin stem makes, for growing it with bone meal. */
@Mixin(StemBlock.class)
public interface StemBlockAccessor {
    @Accessor("fruit")
    ResourceKey<Block> nekoroWorldSetup$fruit();

    @Accessor("attachedStem")
    ResourceKey<Block> nekoroWorldSetup$attachedStem();
}
