package io.jayce.sponge_plus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

/*? if neoforge {*/
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
/*?}*/

/*? if fabric {*/
/*import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
*/
/*?}*/

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

// Shared registry, tick loop and conversion path for every sponge-moisture feature. Each feature
// supplies only its own "is this position due right now" answer; this class owns when and where.
/*? if neoforge {*/
@EventBusSubscriber(modid = SpongePlus.MODID)
/*?}*/
public final class SpongeTracker {

    private static final int CHECK_INTERVAL_TICKS = 20;

    private static final Predicate<BlockState> SPONGE = state -> SpongeMoisture.of(state) != null;

    // Bucketed by chunk so an unload drops a whole chunk's worth in one step.
    private static final Map<ChunkKey, Map<BlockPos, TrackedSponge>> TRACKED = new HashMap<>();

    private static int lastCheckTick = 0;

    // Conservative hint: true whenever a drying timer may still be armed somewhere in the registry.
    private static boolean timersArmed = false;

    private SpongeTracker() {}

    private record ChunkKey(ResourceKey<Level> dimension, ChunkPos chunk) {}

    private enum Trigger { DRYING_NEIGHBOR, WEATHER }

    private record Conversion(ServerLevel level, BlockPos pos, SpongeMoisture moisture, Trigger trigger) {}

    // The registry value type, replacing a Long field sharing its space with a -1L sentinel. Both
    // clocks live on one mutable per-position record, each as an explicit boolean plus a long, so the
    // pair cannot drift and neither transition boxes a Long.
    private static final class TrackedSponge {

        private boolean exposed;
        private long exposedSince;
        private boolean dryingScheduled;
        private long dryingDueTick;

        boolean isExposed() {
            return exposed;
        }

        long exposedSince() {
            return exposedSince;
        }

        void beginExposure(long gameTime) {
            exposed = true;
            exposedSince = gameTime;
        }

        void clearExposure() {
            exposed = false;
            exposedSince = 0L;
        }

        boolean isDryingScheduled() {
            return dryingScheduled;
        }

        long dryingDueTick() {
            return dryingDueTick;
        }

        // Mirrors the old putIfAbsent so a repeat neighbor update never pushes the deadline back.
        void scheduleDrying(long dueTick) {
            if (dryingScheduled) return;
            dryingScheduled = true;
            dryingDueTick = dueTick;
        }

        void clearDrying() {
            dryingScheduled = false;
            dryingDueTick = 0L;
        }
    }

    /*? if fabric {*/
    /*public static void registerFabric() {
        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> dropChunk(level, chunk.getPos()));
        ServerTickEvents.END_SERVER_TICK.register(SpongeTracker::onServerTick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> onServerStopping());

        // Fabric API added a "new chunk" flag to the load callback in 26.2.
    */
    /*?}*/
    /*? if fabric && <26.2 {*/
    /*    ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> scanChunk(level, chunk));*/
    /*?}*/
    /*? if fabric && >=26.2 {*/
    /*    ServerChunkEvents.CHUNK_LOAD.register((level, chunk, newChunk) -> scanChunk(level, chunk));*/
    /*?}*/
    /*? if fabric {*/
    /*}*/
    /*?}*/

    /*? if neoforge {*/
    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            onNeighborChange(serverLevel, event.getPos(), event.getState());
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            scanChunk(serverLevel, event.getChunk());
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            dropChunk(serverLevel, event.getChunk().getPos());
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        onServerTick(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        onServerStopping();
    }
    /*?}*/

    // Entry point for both loaders: NeoForge's NeighborNotifyEvent and Fabric's BlockBehaviour mixin.
    public static void onNeighborChange(ServerLevel level, BlockPos pos, BlockState state) {
        updateTracking(level, pos, state);

        // Fabric's neighborChanged fires on the blocks around a change rather than the changed block,
        // so a sponge that was just placed or broken only turns up as a neighbor of pos.
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            if (!level.hasChunkAt(neighborPos)) continue;

            updateTracking(level, neighborPos, level.getBlockState(neighborPos));
        }

        SpongeDryingHandler.onNeighborChange(level, pos, state);
    }

    // API for SpongeDryingHandler. track returns the entry so the caller never has to look it up twice.
    static void scheduleDrying(ServerLevel level, BlockPos pos, int delayTicks) {
        track(level, pos).scheduleDrying(level.getGameTime() + delayTicks);
        timersArmed = true;
    }

    // Picks up sponges that were placed in an earlier session, which no block update would announce.
    private static void scanChunk(ServerLevel level, ChunkAccess chunk) {
        LevelChunkSection[] sections = chunk.getSections();
        ChunkPos chunkPos = chunk.getPos();

        for (int index = 0; index < sections.length; index++) {
            LevelChunkSection section = sections[index];
            // Consulting the palette first keeps whole sections off the per-block path below.
            if (section.hasOnlyAir() || !section.maybeHas(SPONGE)) continue;

            int bottomY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index));
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (!SPONGE.test(section.getBlockState(x, y, z))) continue;

                        track(level, new BlockPos(chunkPos.getMinBlockX() + x, bottomY + y, chunkPos.getMinBlockZ() + z));
                    }
                }
            }
        }
    }

    private static void dropChunk(ServerLevel level, ChunkPos chunkPos) {
        TRACKED.remove(new ChunkKey(level.dimension(), chunkPos));
    }

    public static void onServerTick(MinecraftServer server) {
        if (TRACKED.isEmpty()) return;

        // Rejoining a world restarts the server's tick count, so a jump backwards counts as due.
        int tickCount = server.getTickCount();
        boolean weatherDue = tickCount < lastCheckTick || tickCount - lastCheckTick >= CHECK_INTERVAL_TICKS;
        if (!weatherDue && !timersArmed) return;
        if (weatherDue) lastCheckTick = tickCount;

        // Recomputed below from whatever timers survive this walk.
        timersArmed = false;

        List<ChunkKey> staleChunks = new ArrayList<>();
        List<Conversion> due = new ArrayList<>();

        for (Map.Entry<ChunkKey, Map<BlockPos, TrackedSponge>> chunkEntry : TRACKED.entrySet()) {
            ChunkKey key = chunkEntry.getKey();
            ServerLevel level = server.getLevel(key.dimension());
            Map<BlockPos, TrackedSponge> positions = chunkEntry.getValue();

            // A chunk that went away without an unload event, or a dimension that no longer exists.
            // Dropping it is safe: loading the chunk again scans it back in.
            if (level == null || positions.isEmpty()
                    || !level.hasChunkAt(positions.keySet().iterator().next())) {
                staleChunks.add(key);
                continue;
            }

            long gameTime = level.getGameTime();

            positions.entrySet().removeIf(position -> {
                BlockPos pos = position.getKey();
                TrackedSponge tracked = position.getValue();

                boolean timerDue = tracked.isDryingScheduled() && gameTime >= tracked.dryingDueTick();
                // Nothing can happen to this sponge on this tick, so skip the world read entirely.
                if (!weatherDue && !timerDue) {
                    timersArmed |= tracked.isDryingScheduled();
                    return false;
                }

                SpongeMoisture moisture = SpongeMoisture.of(level.getBlockState(pos));
                if (moisture == null) return true;

                if (timerDue) {
                    tracked.clearDrying();
                    due.add(new Conversion(level, pos, moisture, Trigger.DRYING_NEIGHBOR));
                    return false;
                }
                timersArmed |= tracked.isDryingScheduled();

                if (weatherDue) {
                    if (!moisture.isWeatherExposed(level, pos)) {
                        tracked.clearExposure();
                    } else if (!tracked.isExposed()) {
                        tracked.beginExposure(gameTime);
                    } else if (gameTime - tracked.exposedSince() >= moisture.weatherDelayTicks()) {
                        due.add(new Conversion(level, pos, moisture, Trigger.WEATHER));
                    }
                }
                return false;
            });
        }

        for (ChunkKey key : staleChunks) {
            TRACKED.remove(key);
        }

        // Converting sends a block update that re-enters onNeighborChange, so it waits until the
        // sweep above has finished walking the registry.
        for (Conversion conversion : due) {
            ServerLevel level = conversion.level();
            BlockPos pos = conversion.pos();
            // Reading an unloaded position would force a synchronous chunk load, so skip it instead.
            if (!level.isLoaded(pos)) continue;

            SpongeMoisture current = SpongeMoisture.of(level.getBlockState(pos));
            if (current == null) {
                untrack(level, pos);
                continue;
            }
            // Makes sure the block was not broken or already converted before this ran.
            if (current != conversion.moisture()) continue;
            if (conversion.trigger() == Trigger.DRYING_NEIGHBOR
                    && !SpongeDryingHandler.hasDryingNeighbor(level, pos)) continue;

            convert(level, pos, current);
        }
    }

    public static void onServerStopping() {
        TRACKED.clear();
        lastCheckTick = 0;
        timersArmed = false;
    }

    private static void convert(ServerLevel level, BlockPos pos, SpongeMoisture moisture) {
        level.setBlock(pos, moisture.target().defaultBlockState(), Block.UPDATE_ALL);
        moisture.playConversionEffect(level, pos);

        // setBlock's neighbor updates re-enter onNeighborChange, so only the exposure clock is
        // reset here; a drying timer armed by those updates has to survive.
        track(level, pos).clearExposure();
    }

    private static void updateTracking(ServerLevel level, BlockPos pos, BlockState state) {
        if (SPONGE.test(state)) {
            track(level, pos);
        } else {
            untrack(level, pos);
        }
    }

    private static TrackedSponge track(ServerLevel level, BlockPos pos) {
        return TRACKED.computeIfAbsent(keyOf(level, pos), key -> new HashMap<>())
                .computeIfAbsent(pos.immutable(), key -> new TrackedSponge());
    }

    private static void untrack(ServerLevel level, BlockPos pos) {
        if (TRACKED.isEmpty()) return;

        ChunkKey key = keyOf(level, pos);
        Map<BlockPos, TrackedSponge> positions = TRACKED.get(key);
        if (positions == null) return;

        if (positions.remove(pos) != null && positions.isEmpty()) {
            TRACKED.remove(key);
        }
    }

    private static ChunkKey keyOf(ServerLevel level, BlockPos pos) {
        return new ChunkKey(level.dimension(),
                new ChunkPos(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())));
    }
}
