package io.jayce.sponge_plus;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(SpongePlus.MODID)
public class SpongePlus {
    public static final String MODID = "sponge_plus";

    public SpongePlus(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
