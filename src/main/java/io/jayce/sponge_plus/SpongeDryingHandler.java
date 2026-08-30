package io.jayce.sponge_plus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@EventBusSubscriber(modid = Sponge_plus.MODID)
public class SpongeDryingHandler {

    private static final int DRYING_DELAY_TICKS = 60; // 3 second delay

    private static final Map<GlobalPos, Long> PENDING = new HashMap<>();

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        LevelAccessor level = event.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getState();

        if (isActiveDryingBlock(state)) {
            scheduleNeighboringSponges(serverLevel, pos, state);
        } else if (state.is(Blocks.WET_SPONGE) && hasDryingNeighbor(serverLevel, pos)) {
            schedule(serverLevel, pos);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) return;

        Iterator<Map.Entry<GlobalPos, Long>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<GlobalPos, Long> entry = iterator.next();
            GlobalPos globalPos = entry.getKey();
            ServerLevel level = event.getServer().getLevel(globalPos.dimension());

            if (level == null) {
                iterator.remove();
                continue;
            }
            if (level.getGameTime() < entry.getValue()) continue;

            iterator.remove();
            BlockPos pos = globalPos.pos();
            // Makes sure neither blocks were broken before the conversion completed.
            if (level.getBlockState(pos).is(Blocks.WET_SPONGE) && hasDryingNeighbor(level, pos)) {
                convert(level, pos);
            }
        }
    }

    private static void convert(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.SPONGE.defaultBlockState(), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
        spawnSteam(level, pos);
    }

    private static void spawnSteam(ServerLevel level, BlockPos pos) {
        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 0.5;
        double centerZ = pos.getZ() + 0.5;

        // Top face
        level.sendParticles(ParticleTypes.WHITE_SMOKE, centerX, pos.getY() + 1.0, centerZ,
                15, 0.35, 0.1, 0.35, 0.02);

        // The 4 side faces - no bottom, since the drying block is usually right underneath.
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            double faceX = centerX + 0.5 * direction.getStepX();
            double faceZ = centerZ + 0.5 * direction.getStepZ();
            double xOffset = direction.getAxis() == Direction.Axis.X ? 0.05 : 0.3;
            double zOffset = direction.getAxis() == Direction.Axis.Z ? 0.05 : 0.3;

            level.sendParticles(ParticleTypes.WHITE_SMOKE, faceX, centerY, faceZ,
                    8, xOffset, 0.4, zOffset, 0.02);
        }
    }

    private static void scheduleNeighboringSponges(ServerLevel level, BlockPos pos, BlockState dryingState) {
        for (Direction direction : Direction.values()) {
            // Campfires only radiate through their top and sides, not into whatever they're sitting on.
            if (direction == Direction.DOWN && dryingState.is(BlockTags.CAMPFIRES)) continue;

            BlockPos neighborPos = pos.relative(direction);
            if (level.getBlockState(neighborPos).is(Blocks.WET_SPONGE)) {
                schedule(level, neighborPos);
            }
        }
    }

    private static void schedule(ServerLevel level, BlockPos pos) {
        GlobalPos globalPos = GlobalPos.of(level.dimension(), pos.immutable());
        PENDING.putIfAbsent(globalPos, level.getGameTime() + DRYING_DELAY_TICKS);
    }

    private static boolean hasDryingNeighbor(LevelAccessor level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockState neighborState = level.getBlockState(pos.relative(direction));
            if (!isActiveDryingBlock(neighborState)) continue;

            // A campfire directly above the sponge is touching the campfire's bottom - doesn't count.
            if (direction == Direction.UP && neighborState.is(BlockTags.CAMPFIRES)) continue;

            return true;
        }
        return false;
    }

    // A block only counts while it's actually hot - an extinguished campfire shouldn't dry sponges.
    private static boolean isActiveDryingBlock(BlockState state) {
        if (!state.is(ModTags.DRYING_BLOCKS)) return false;
        return !state.hasProperty(BlockStateProperties.LIT) || state.getValue(BlockStateProperties.LIT);
    }
}
