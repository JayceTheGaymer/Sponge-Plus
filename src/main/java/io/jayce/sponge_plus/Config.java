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

    static final ModConfigSpec SPEC = BUILDER.build();
    /*?}*/

    private static int dryingDelayTicks = 60;

    public static int dryingDelayTicks() {
        return dryingDelayTicks;
    }

    /*? if neoforge {*/
    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            dryingDelayTicks = DRYING_DELAY_TICKS.get();
        }
    }
    /*?}*/

    /*? if fabric {*/
    /*private static final int DEFAULT_DELAY_TICKS = 60;
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

        dryingDelayTicks = readDelayTicks(properties);

        properties.setProperty("dryingDelayTicks", String.valueOf(dryingDelayTicks));
        try (OutputStream out = Files.newOutputStream(path)) {
            properties.store(out, "How long a wet sponge must sit next to a drying block before it dries, in ticks. 20 ticks = 1 second.");
        } catch (IOException ignored) {
        }
    }

    // A hand-edited config must not take the game down, so fall back to the default and clamp to the same
    // range the NeoForge spec enforces.
    private static int readDelayTicks(Properties properties) {
        String raw = properties.getProperty("dryingDelayTicks");
        if (raw == null) return DEFAULT_DELAY_TICKS;

        try {
            int parsed = Integer.parseInt(raw.trim());
            return Math.max(0, Math.min(MAX_DELAY_TICKS, parsed));
        } catch (NumberFormatException e) {
            return DEFAULT_DELAY_TICKS;
        }
    }*/
    /*?}*/
}
