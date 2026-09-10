package io.jayce.sponge_plus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.IntSupplier;

// One row per sponge moisture state, holding everything a conversion needs.
enum SpongeMoisture {
    WET(Blocks.WET_SPONGE, Blocks.SPONGE, Config::sunlightDryingDelayTicks,
            SpongeWeatherHandler::driesInSun, SpongeMoisture::playDryingEffect),
    DRY(Blocks.SPONGE, Blocks.WET_SPONGE, Config::rainWettingDelayTicks,
            SpongeWeatherHandler::wetsInRain, SpongeMoisture::playWettingEffect);

    @FunctionalInterface
    interface ExposureCheck {
        boolean test(ServerLevel level, BlockPos above);
    }

    @FunctionalInterface
    interface ConversionEffect {
        void play(ServerLevel level, BlockPos pos);
    }

    private final Block block;
    private final Block target;
    private final IntSupplier weatherDelayTicks;
    private final ExposureCheck exposure;
    private final ConversionEffect effect;

    SpongeMoisture(Block block, Block target, IntSupplier weatherDelayTicks,
                   ExposureCheck exposure, ConversionEffect effect) {
        this.block = block;
        this.target = target;
        this.weatherDelayTicks = weatherDelayTicks;
        this.exposure = exposure;
        this.effect = effect;
    }

    // Written out longhand instead of looping values(), which allocates a fresh array per call.
    static SpongeMoisture of(BlockState state) {
        if (state.is(WET.block)) return WET;
        if (state.is(DRY.block)) return DRY;
        return null;
    }

    Block target() {
        return target;
    }

    int weatherDelayTicks() {
        return weatherDelayTicks.getAsInt();
    }

    boolean isWeatherExposed(ServerLevel level, BlockPos pos) {
        // Checked against the air above the sponge, since an opaque block stores no sky light of its own.
        return exposure.test(level, pos.above());
    }

    void playConversionEffect(ServerLevel level, BlockPos pos) {
        effect.play(level, pos);
    }

    private static void playDryingEffect(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);

        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 0.5;
        double centerZ = pos.getZ() + 0.5;

        // Particles for top face
        level.sendParticles(ParticleTypes.WHITE_SMOKE, centerX, pos.getY() + 1.0, centerZ,
                15, 0.35, 0.1, 0.35, 0.02);

        // Particles for side faces.
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            double faceX = centerX + 0.5 * direction.getStepX();
            double faceZ = centerZ + 0.5 * direction.getStepZ();
            double xOffset = direction.getAxis() == Direction.Axis.X ? 0.05 : 0.3;
            double zOffset = direction.getAxis() == Direction.Axis.Z ? 0.05 : 0.3;

            level.sendParticles(ParticleTypes.WHITE_SMOKE, faceX, centerY, faceZ,
                    8, xOffset, 0.4, zOffset, 0.02);
        }
    }

    private static void playWettingEffect(ServerLevel level, BlockPos pos) {
        // Matches the volume and pitch vanilla uses when a block is placed by hand.
        SoundType soundType = Blocks.WET_SPONGE.defaultBlockState().getSoundType();
        level.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS,
                (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);

        level.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                15, 0.35, 0.1, 0.35, 0.02);
    }
}
