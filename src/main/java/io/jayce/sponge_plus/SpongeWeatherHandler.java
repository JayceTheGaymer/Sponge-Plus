package io.jayce.sponge_plus;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

// Owns the weather predicates only: wet sponges dry in the sun, dry sponges soak up rain. Neither
// sunrise nor a passing cloud is a block update, so SpongeTracker holds these in a registry and
// rechecks them on a fixed cadence.
public final class SpongeWeatherHandler {

    private SpongeWeatherHandler() {}

    static boolean driesInSun(ServerLevel level, BlockPos above) {
        return isDay(level) && level.canSeeSky(above) && !isPrecipitatingAt(level, above);
    }

    static boolean wetsInRain(ServerLevel level, BlockPos above) {
        return level.isRainingAt(above);
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
}
