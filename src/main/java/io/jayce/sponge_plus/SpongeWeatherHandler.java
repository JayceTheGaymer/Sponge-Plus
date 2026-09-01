package io.jayce.sponge_plus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

/*? if neoforge {*/
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
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

// Turns sponges left out in the weather: wet ones dry in the sun, dry ones soak up rain. Neither
// sunrise nor a passing cloud is a block update, so sponges are held in a registry and rechecked
// on a fixed cadence.
/*? if neoforge {*/
@EventBusSubscriber(modid = SpongePlus.MODID)
/*?}*/
public final class SpongeWeatherHandler {

    private static final int CHECK_INTERVAL_TICKS = 20;

    // Game time is never negative, so this doubles as "tracked, but not currently exposed".
    private static final long NOT_EXPOSED = -1L;

    private static final Predicate<BlockState> SPONGE = state -> state.is(Blocks.SPONGE) || state.is(Blocks.WET_SPONGE);

    // Bucketed by chunk so an unload drops a whole chunk's worth in one step.
    private static final Map<ChunkKey, Map<BlockPos, Long>> TRACKED = new HashMap<>();

    private static int lastCheckTick = 0;

    private SpongeWeatherHandler() {}

    private record ChunkKey(ResourceKey<Level> dimension, ChunkPos chunk) {}

    private record Due(ServerLevel level, BlockPos pos) {}

    /*? if fabric {*/
    /*public static void registerFabric() {
        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> dropChunk(level, chunk.getPos()));
        ServerTickEvents.END_SERVER_TICK.register(SpongeWeatherHandler::onServerTick);
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

    // Called from SpongeDryingHandler.handleNeighborChange, the one block-change signal both loaders share.
    public static void trackNearby(ServerLevel level, BlockPos pos, BlockState state) {
        updateTracking(level, pos, state);

        // Fabric's neighborChanged fires on the blocks around a change rather than the changed block,
        // so a sponge that was just placed or broken only turns up as a neighbor of pos.
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            if (!level.hasChunkAt(neighborPos)) continue;

            updateTracking(level, neighborPos, level.getBlockState(neighborPos));
        }
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
        if (tickCount >= lastCheckTick && tickCount - lastCheckTick < CHECK_INTERVAL_TICKS) return;
        lastCheckTick = tickCount;

        List<ChunkKey> staleChunks = new ArrayList<>();
        List<Due> due = new ArrayList<>();

        for (Map.Entry<ChunkKey, Map<BlockPos, Long>> chunkEntry : TRACKED.entrySet()) {
            ChunkKey key = chunkEntry.getKey();
            ServerLevel level = server.getLevel(key.dimension());
            Map<BlockPos, Long> positions = chunkEntry.getValue();

            // A chunk that went away without an unload event, or a dimension that no longer exists.
            // Dropping it is safe: loading the chunk again scans it back in.
            if (level == null || positions.isEmpty() || !level.hasChunkAt(positions.keySet().iterator().next())) {
                staleChunks.add(key);
                continue;
            }

            positions.entrySet().removeIf(position -> {
                BlockPos pos = position.getKey();
                BlockState state = level.getBlockState(pos);
                if (!SPONGE.test(state)) return true;

                if (!isExposed(level, pos, state)) {
                    position.setValue(NOT_EXPOSED);
                    return false;
                }

                long since = position.getValue();
                if (since == NOT_EXPOSED) {
                    position.setValue(level.getGameTime());
                } else if (level.getGameTime() - since >= delayTicks(state)) {
                    due.add(new Due(level, pos));
                }
                return false;
            });
        }

        for (ChunkKey key : staleChunks) {
            TRACKED.remove(key);
        }

        // Converting sends a block update that re-enters trackNearby, so it waits until the sweep
        // above has finished walking the registry.
        for (Due entry : due) {
            convert(entry.level(), entry.pos());
        }
    }

    public static void onServerStopping() {
        TRACKED.clear();
        lastCheckTick = 0;
    }

    private static void convert(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return;

        // An earlier conversion in the same batch can have taken this block out from under us.
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.WET_SPONGE)) {
            SpongeDryingHandler.convert(level, pos);
        } else if (state.is(Blocks.SPONGE)) {
            wet(level, pos);
        } else {
            untrack(level, pos);
            return;
        }

        // The sponge is still tracked under its new form, so its timer starts over.
        TRACKED.computeIfAbsent(keyOf(level, pos), key -> new HashMap<>()).put(pos.immutable(), NOT_EXPOSED);
    }

    private static void wet(ServerLevel level, BlockPos pos) {
        BlockState wetSponge = Blocks.WET_SPONGE.defaultBlockState();
        level.setBlock(pos, wetSponge, Block.UPDATE_ALL);

        // Matches the volume and pitch vanilla uses when a block is placed by hand.
        SoundType soundType = wetSponge.getSoundType();
        level.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS,
                (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);

        level.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                15, 0.35, 0.1, 0.35, 0.02);
    }

    private static boolean isExposed(ServerLevel level, BlockPos pos, BlockState state) {
        // Checked against the air above the sponge: an opaque block stores no sky light of its own,
        // so asking about its own position always reports darkness and fair weather.
        BlockPos above = pos.above();
        if (state.is(Blocks.WET_SPONGE)) {
            return isDay(level) && level.canSeeSky(above) && !isPrecipitatingAt(level, above);
        }
        return level.isRainingAt(above);
    }

    private static int delayTicks(BlockState state) {
        return state.is(Blocks.WET_SPONGE) ? Config.sunlightDryingDelayTicks() : Config.rainWettingDelayTicks();
    }

    // Level#isDay() is gone in 26.2, so this repeats what it did.
    private static boolean isDay(ServerLevel level) {
        return !level.dimensionType().hasFixedTime() && level.getSkyDarken() < 4;
    }

    // Snow hides the sun as well as rain does, and isRainingAt only reports the latter.
    private static boolean isPrecipitatingAt(ServerLevel level, BlockPos pos) {
        if (!level.isRaining()) return false;

        /*? if <26.2 {*/
        return level.getBiome(pos).value().getPrecipitationAt(pos) != Biome.Precipitation.NONE;
        /*?}*/
        /*? if >=26.2 {*/
        /*return level.precipitationAt(pos) != Biome.Precipitation.NONE;*/
        /*?}*/
    }

    private static void updateTracking(ServerLevel level, BlockPos pos, BlockState state) {
        if (SPONGE.test(state)) {
            track(level, pos);
        } else {
            untrack(level, pos);
        }
    }

    private static void track(ServerLevel level, BlockPos pos) {
        TRACKED.computeIfAbsent(keyOf(level, pos), key -> new HashMap<>()).putIfAbsent(pos.immutable(), NOT_EXPOSED);
    }

    private static void untrack(ServerLevel level, BlockPos pos) {
        if (TRACKED.isEmpty()) return;

        ChunkKey key = keyOf(level, pos);
        Map<BlockPos, Long> positions = TRACKED.get(key);
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
