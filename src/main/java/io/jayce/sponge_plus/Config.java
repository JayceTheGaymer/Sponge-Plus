package io.jayce.sponge_plus;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = SpongePlus.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue DRYING_DELAY_TICKS = BUILDER
            .comment("How long a wet sponge must sit next to a drying block before it dries, in ticks. 20 ticks = 1 second.")
            .defineInRange("dryingDelayTicks", 60, 0, 24000);

    static final ModConfigSpec SPEC = BUILDER.build();

    private static int dryingDelayTicks = 60;

    public static int dryingDelayTicks() {
        return dryingDelayTicks;
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            dryingDelayTicks = DRYING_DELAY_TICKS.get();
        }
    }
}
