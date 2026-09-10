package io.jayce.sponge_plus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

// Owns the campfire drying policy: which neighbor changes arm a timer, and whether one still
// applies when that timer comes due. SpongeTracker owns the timer itself.
public final class SpongeDryingHandler {

    private SpongeDryingHandler() {}

    static void onNeighborChange(ServerLevel level, BlockPos pos, BlockState state) {
        if (isActiveDryingBlock(state)) {
            scheduleNeighboringSponges(level, pos, state);
        } else if (state.is(Blocks.WET_SPONGE) && hasDryingNeighbor(level, pos)) {
            schedule(level, pos);
        }
    }

    // Package-visible so SpongeTracker can re-confirm the condition before it converts.
    static boolean hasDryingNeighbor(LevelAccessor level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            // A neighbor can sit in an adjacent chunk that is not loaded, and reading it would load that chunk.
            if (!level.hasChunkAt(neighborPos)) continue;

            BlockState neighborState = level.getBlockState(neighborPos);
            if (!isActiveDryingBlock(neighborState)) continue;
            if (direction == Direction.UP && neighborState.is(BlockTags.CAMPFIRES)) continue;

            return true;
        }
        return false;
    }

    private static void scheduleNeighboringSponges(ServerLevel level, BlockPos pos, BlockState dryingState) {
        for (Direction direction : Direction.values()) {
            // Campfires only radiate through their top and sides, not into whatever they're sitting on.
            if (direction == Direction.DOWN && dryingState.is(BlockTags.CAMPFIRES)) continue;

            BlockPos neighborPos = pos.relative(direction);
            if (!level.hasChunkAt(neighborPos)) continue;

            if (level.getBlockState(neighborPos).is(Blocks.WET_SPONGE)) {
                schedule(level, neighborPos);
            }
        }
    }

    private static void schedule(ServerLevel level, BlockPos pos) {
        SpongeTracker.scheduleDrying(level, pos, Config.dryingDelayTicks());
    }

    // Stops extinguished campfires from working to dry sponges.
    private static boolean isActiveDryingBlock(BlockState state) {
        if (!state.is(ModTags.DRYING_BLOCKS)) return false;
        return !state.hasProperty(BlockStateProperties.LIT) || state.getValue(BlockStateProperties.LIT);
    }
}
