package dev.xardas.nekoroworldsetup;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.BaseCoralPlantTypeBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;

import dev.xardas.nekoroworldsetup.mixin.StemBlockAccessor;

/**
 * Bone meal on plants and blocks vanilla ignores: sugar cane, cactus, vines, nether wart, full melon and
 * pumpkin stems, lily pads, small flowers, dead bushes, chorus, coral, spore blossoms, podzol, mycelium and
 * dirt. Each one is a separate option. Blocks vanilla already grows are left to vanilla.
 */
final class BoneMeal {
    private static final int SPREAD = 3;

    private BoneMeal() { }

    /** What bone meal does to one block; returns false when there is nothing it can do right now. */
    private interface Use {
        boolean canUse(Level level, BlockPos pos, BlockState state);

        void use(ServerLevel level, BlockPos pos, BlockState state, RandomSource random);
    }

    static void onBonemeal(BonemealEvent event) {
        WsConfig.World cfg = WsConfig.get().world;
        if (!cfg.boneMeal || event.isValidBonemealTarget()) return;
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        Use use = find(cfg, state);
        if (use == null || !use.canUse(level, pos, state)) return;
        // canceled + successful: the item counts as used (arm swing, particles), and vanilla does nothing more
        event.setCanceled(true);
        event.setSuccessful(true);
        if (level instanceof ServerLevel server) {
            use.use(server, pos, state, server.random);
            ItemStack stack = event.getStack();
            stack.shrink(1);
        }
    }

    private static Use find(WsConfig.World cfg, BlockState state) {
        Block block = state.getBlock();
        if (cfg.boneMealSugarCane && block == Blocks.SUGAR_CANE) return COLUMN;
        if (cfg.boneMealCactus && block == Blocks.CACTUS) return COLUMN;
        if (cfg.boneMealVines && block instanceof VineBlock) return VINE;
        if (cfg.boneMealNetherWart && block instanceof NetherWartBlock) return NETHER_WART;
        if (cfg.boneMealFruitStems && block instanceof StemBlock) return STEM;
        if (cfg.boneMealLilyPad && block == Blocks.LILY_PAD) return LILY_PAD;
        if (cfg.boneMealSmallFlowers && state.is(BlockTags.SMALL_FLOWERS) && block != Blocks.WITHER_ROSE) return SPREAD_COPY;
        if (cfg.boneMealDeadBush && block == Blocks.DEAD_BUSH) return SPREAD_COPY;
        if (cfg.boneMealChorus && (block == Blocks.CHORUS_FLOWER || block == Blocks.CHORUS_PLANT)) return CHORUS;
        if (cfg.boneMealCorals && block instanceof BaseCoralPlantTypeBlock && !state.is(BlockTags.WALL_CORALS)) return CORAL;
        if (cfg.boneMealSporeBlossom && block == Blocks.SPORE_BLOSSOM) return SPORE_BLOSSOM;
        if (cfg.boneMealPodzol && block == Blocks.PODZOL) return PODZOL;
        if (cfg.boneMealMycelium && block == Blocks.MYCELIUM) return MYCELIUM;
        if (cfg.boneMealDirt && (block == Blocks.DIRT || block == Blocks.COARSE_DIRT || block == Blocks.ROOTED_DIRT)) return DIRT;
        return null;
    }

    /** Sugar cane and cactus: one block more on top, up to the height they grow to on their own (3). */
    private static final Use COLUMN = new Use() {
        private BlockPos top(Level level, BlockPos pos, Block block) {
            BlockPos top = pos;
            while (level.getBlockState(top.above()).is(block)) top = top.above();
            return top;
        }

        private int height(Level level, BlockPos top, Block block) {
            int height = 1;
            while (level.getBlockState(top.below(height)).is(block)) height++;
            return height;
        }

        @Override
        public boolean canUse(Level level, BlockPos pos, BlockState state) {
            BlockPos top = top(level, pos, state.getBlock());
            return height(level, top, state.getBlock()) < 3 && level.isEmptyBlock(top.above())
                    && state.getBlock().defaultBlockState().canSurvive(level, top.above());
        }

        @Override
        public void use(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
            level.setBlockAndUpdate(top(level, pos, state.getBlock()).above(), state.getBlock().defaultBlockState());
        }
    };

    /** Vines grow one block further down. */
    private static final Use VINE = new Use() {
        private BlockPos bottom(Level level, BlockPos pos, Block block) {
            BlockPos bottom = pos;
            while (level.getBlockState(bottom.below()).is(block)) bottom = bottom.below();
            return bottom;
        }

        @Override
        public boolean canUse(Level level, BlockPos pos, BlockState state) {
            BlockPos below = bottom(level, pos, state.getBlock()).below();
            return level.isEmptyBlock(below) && below.getY() >= level.getMinBuildHeight();
        }

        @Override
        public void use(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
            BlockPos bottom = bottom(level, pos, state.getBlock());
            level.setBlockAndUpdate(bottom.below(), level.getBlockState(bottom).setValue(VineBlock.UP, false));
        }
    };

    private static final Use NETHER_WART = new Use() {
        @Override
        public boolean canUse(Level level, BlockPos pos, BlockState state) {
            return state.getValue(NetherWartBlock.AGE) < NetherWartBlock.MAX_AGE;
        }

        @Override
        public void use(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
            level.setBlock(pos, state.setValue(NetherWartBlock.AGE, state.getValue(NetherWartBlock.AGE) + 1), Block.UPDATE_CLIENTS);
        }
    };

    /** A fully grown melon or pumpkin stem makes its fruit right away, the same way it does on its own. */
    private static final Use STEM = new Use() {
        private List<Direction> free(Level level, BlockPos pos) {
            List<Direction> out = new ArrayList<>();
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos side = pos.relative(direction);
                BlockState ground = level.getBlockState(side.below());
                if (level.isEmptyBlock(side) && (ground.getBlock() instanceof FarmBlock || ground.is(BlockTags.DIRT))) out.add(direction);
            }
            return out;
        }

        @Override
        public boolean canUse(Level level, BlockPos pos, BlockState state) {
            return state.getValue(StemBlock.AGE) >= StemBlock.MAX_AGE && !free(level, pos).isEmpty();
        }

        @Override
        public void use(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
            List<Direction> free = free(level, pos);
            Direction direction = free.get(random.nextInt(free.size()));
            StemBlockAccessor stem = (StemBlockAccessor) state.getBlock();
            Registry<Block> blocks = level.registryAccess().registryOrThrow(Registries.BLOCK);
            Block fruit = blocks.get(stem.nekoroWorldSetup$fruit());
            Block attached = blocks.get(stem.nekoroWorldSetup$attachedStem());
            if (fruit == null || attached == null) return;
            level.setBlockAndUpdate(pos.relative(direction), fruit.defaultBlockState());
            level.setBlockAndUpdate(pos, attached.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, direction));
        }
    };

    /** Lily pads spread over still water around them. */
    private static final Use LILY_PAD = new Use() {
        @Override
        public boolean canUse(Level level, BlockPos pos, BlockState state) {
            return true;
        }

        @Override
        public void use(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
            spread(level, pos, random, 4, at -> level.isEmptyBlock(at) && level.getFluidState(at.below()).isSourceOfType(
                    net.minecraft.world.level.material.Fluids.WATER) ? Blocks.LILY_PAD.defaultBlockState() : null);
        }
    };

    /** Small flowers and dead bushes: copies of the same plant around it, where that plant can grow. */
    private static final Use SPREAD_COPY = new Use() {
        @Override
        public boolean canUse(Level level, BlockPos pos, BlockState state) {
            return true;
        }

        @Override
        public void use(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
            BlockState plant = state.getBlock().defaultBlockState();
            spread(level, pos, random, 3, at -> level.isEmptyBlock(at) && plant.canSurvive(level, at) ? plant : null);
        }
    };

    /** Chorus: the nearest flower of the plant (up to 48 blocks of stem above) grows like on its own. */
    private static final Use CHORUS = new Use() {
        private BlockPos flower(Level level, BlockPos pos) {
            BlockPos at = pos;
            for (int i = 0; i < 48; i++) {
                BlockState there = level.getBlockState(at);
                if (there.is(Blocks.CHORUS_FLOWER)) return there.getValue(ChorusFlowerBlock.AGE) < ChorusFlowerBlock.DEAD_AGE ? at : null;
                if (!there.is(Blocks.CHORUS_PLANT)) return null;
                BlockPos next = null;
                for (Direction direction : new Direction[] {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                    BlockState side = level.getBlockState(at.relative(direction));
                    if (side.is(Blocks.CHORUS_FLOWER) && side.getValue(ChorusFlowerBlock.AGE) < ChorusFlowerBlock.DEAD_AGE) return at.relative(direction);
                    if (direction == Direction.UP && side.is(Blocks.CHORUS_PLANT)) next = at.above();
                }
                if (next == null) return null;
                at = next;
            }
            return null;
        }

        @Override
        public boolean canUse(Level level, BlockPos pos, BlockState state) {
            return flower(level, pos) != null;
        }

        @Override
        public void use(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
            BlockPos flower = flower(level, pos);
            if (flower != null) level.getBlockState(flower).randomTick(level, flower, random);
        }
    };

    /** Coral in a warm ocean grows into a coral tree, like the ones in reefs. */
    private static final Use CORAL = new Use() {
        @Override
        public boolean canUse(Level level, BlockPos pos, BlockState state) {
            return level.getBiome(pos).is(Biomes.WARM_OCEAN) && level.getFluidState(pos).is(FluidTags.WATER)
                    && level.getFluidState(pos.above()).is(FluidTags.WATER);
        }

        @Override
        public void use(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
            boolean grown = Feature.CORAL_TREE.place(FeatureConfiguration.NONE, level,
                    level.getChunkSource().getGenerator(), random, pos);
            if (!grown) level.setBlock(pos, state, Block.UPDATE_ALL);
        }
    };

    private static final Use SPORE_BLOSSOM = new Use() {
        @Override
        public boolean canUse(Level level, BlockPos pos, BlockState state) {
            return true;
        }

        @Override
        public void use(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
            Block.popResource(level, pos, new ItemStack(Items.SPORE_BLOSSOM));
        }
    };

    /** Podzol grows ferns, sometimes large ferns and sweet berry bushes, on podzol around it. */
    private static final Use PODZOL = new Use() {
        @Override
        public boolean canUse(Level level, BlockPos pos, BlockState state) {
            return level.isEmptyBlock(pos.above());
        }

        @Override
        public void use(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
            spread(level, pos.above(), random, 5, at -> {
                if (!level.isEmptyBlock(at) || !level.getBlockState(at.below()).is(Blocks.PODZOL)) return null;
                int roll = random.nextInt(10);
                if (roll == 0) return Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 0);
                if (roll == 1 && level.isEmptyBlock(at.above())) {
                    net.minecraft.world.level.block.DoublePlantBlock.placeAt(level, Blocks.LARGE_FERN.defaultBlockState(), at, Block.UPDATE_ALL);
                    return null;
                }
                return Blocks.FERN.defaultBlockState();
            });
        }
    };

    /** Mycelium grows brown and red mushrooms on mycelium around it. */
    private static final Use MYCELIUM = new Use() {
        @Override
        public boolean canUse(Level level, BlockPos pos, BlockState state) {
            return level.isEmptyBlock(pos.above());
        }

        @Override
        public void use(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
            spread(level, pos.above(), random, 3, at -> level.isEmptyBlock(at) && level.getBlockState(at.below()).is(Blocks.MYCELIUM)
                    ? (random.nextBoolean() ? Blocks.BROWN_MUSHROOM : Blocks.RED_MUSHROOM).defaultBlockState() : null);
        }
    };

    /** Dirt next to grass or mycelium turns into it. */
    private static final Use DIRT = new Use() {
        private List<Block> neighbours(Level level, BlockPos pos) {
            List<Block> out = new ArrayList<>();
            for (BlockPos at : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                BlockState there = level.getBlockState(at);
                if (there.is(Blocks.GRASS_BLOCK) || there.is(Blocks.MYCELIUM)) out.add(there.getBlock());
            }
            return out;
        }

        @Override
        public boolean canUse(Level level, BlockPos pos, BlockState state) {
            return !level.getBlockState(pos.above()).isSolidRender(level, pos.above()) && !neighbours(level, pos).isEmpty();
        }

        @Override
        public void use(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
            List<Block> next = neighbours(level, pos);
            level.setBlockAndUpdate(pos, next.get(random.nextInt(next.size())).defaultBlockState());
        }
    };

    /** Tries a few random spots around the center (same height, one above or one below) and places what fits. */
    private static void spread(ServerLevel level, BlockPos center, RandomSource random, int count,
                               java.util.function.Function<BlockPos, BlockState> what) {
        int placed = 0;
        for (int attempt = 0; attempt < count * 8 && placed < count; attempt++) {
            BlockPos at = center.offset(random.nextIntBetweenInclusive(-SPREAD, SPREAD), random.nextIntBetweenInclusive(-1, 1),
                    random.nextIntBetweenInclusive(-SPREAD, SPREAD));
            if (at.equals(center)) continue;
            BlockState state = what.apply(at);
            if (state == null) continue;
            level.setBlockAndUpdate(at, state);
            placed++;
        }
    }
}
