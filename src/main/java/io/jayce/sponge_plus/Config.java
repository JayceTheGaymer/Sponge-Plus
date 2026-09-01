package io.jayce.sponge_plus;

/*? if neoforge {*/
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
/*?}*/

/*? if fabric {*/
/*import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
*/
/*?}*/

/*? if neoforge {*/
@EventBusSubscriber(modid = SpongePlus.MODID)
/*?}*/
public class Config {

    /*? if neoforge {*/
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue DRYING_DELAY_TICKS = BUILDER
            .comment("How long a wet sponge must sit next to a drying block before it dries, in ticks. 20 ticks = 1 second.")
            .defineInRange("dryingDelayTicks", 60, 0, 24000);

    private static final ModConfigSpec.IntValue SUNLIGHT_DRYING_DELAY_TICKS = BUILDER
            .comment("How long a wet sponge must sit continuously under open sky during the day before it dries, in ticks. 20 ticks = 1 second.")
            .defineInRange("sunlightDryingDelayTicks", 6000, 0, 24000);

    private static final ModConfigSpec.IntValue RAIN_WETTING_DELAY_TICKS = BUILDER
            .comment("How long a dry sponge must sit continuously in the rain under open sky before it turns wet, in ticks. 20 ticks = 1 second.")
            .defineInRange("rainWettingDelayTicks", 2400, 0, 24000);

    static final ModConfigSpec SPEC = BUILDER.build();
    /*?}*/

    private static int dryingDelayTicks = 60;
    private static int sunlightDryingDelayTicks = 6000;
    private static int rainWettingDelayTicks = 2400;

    public static int dryingDelayTicks() {
        return dryingDelayTicks;
    }

    public static int sunlightDryingDelayTicks() {
        return sunlightDryingDelayTicks;
    }

    public static int rainWettingDelayTicks() {
        return rainWettingDelayTicks;
    }

    /*? if neoforge {*/
    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            dryingDelayTicks = DRYING_DELAY_TICKS.get();
            sunlightDryingDelayTicks = SUNLIGHT_DRYING_DELAY_TICKS.get();
            rainWettingDelayTicks = RAIN_WETTING_DELAY_TICKS.get();
        }
    }
    /*?}*/

    /*? if fabric {*/
    /*private static final int DEFAULT_DELAY_TICKS = 60;
    private static final int DEFAULT_SUNLIGHT_DELAY_TICKS = 6000;
    private static final int DEFAULT_RAIN_DELAY_TICKS = 2400;
    private static final int MAX_DELAY_TICKS = 24000;

    public static void init() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(SpongePlus.MODID + ".properties");
        Properties properties = new Properties();
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                properties.load(in);
            } catch (IOException ignored) {
            }
        }

        dryingDelayTicks = readDelayTicks(properties, "dryingDelayTicks", DEFAULT_DELAY_TICKS);
        sunlightDryingDelayTicks = readDelayTicks(properties, "sunlightDryingDelayTicks", DEFAULT_SUNLIGHT_DELAY_TICKS);
        rainWettingDelayTicks = readDelayTicks(properties, "rainWettingDelayTicks", DEFAULT_RAIN_DELAY_TICKS);

        properties.setProperty("dryingDelayTicks", String.valueOf(dryingDelayTicks));
        properties.setProperty("sunlightDryingDelayTicks", String.valueOf(sunlightDryingDelayTicks));
        properties.setProperty("rainWettingDelayTicks", String.valueOf(rainWettingDelayTicks));
        try (OutputStream out = Files.newOutputStream(path)) {
            properties.store(out, "dryingDelayTicks: how long a wet sponge must sit next to a drying block before it dries, in ticks. "
                    + "sunlightDryingDelayTicks: how long a wet sponge must sit continuously under open sky during the day before it dries, in ticks. "
                    + "rainWettingDelayTicks: how long a dry sponge must sit continuously in the rain under open sky before it turns wet, in ticks. "
                    + "20 ticks = 1 second.");
        } catch (IOException ignored) {
        }
    }

    // A hand-edited config must not take the game down, so fall back to the default and clamp to the same
    // range the NeoForge spec enforces.
    private static int readDelayTicks(Properties properties, String key, int defaultValue) {
        String raw = properties.getProperty(key);
        if (raw == null) return defaultValue;

        try {
            int parsed = Integer.parseInt(raw.trim());
            return Math.max(0, Math.min(MAX_DELAY_TICKS, parsed));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }*/
    /*?}*/
}
