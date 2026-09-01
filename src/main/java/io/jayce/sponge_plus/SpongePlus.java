package io.jayce.sponge_plus;

/*? if fabric {*/
/*import net.fabricmc.api.ModInitializer;
*/
/*?}*/

/*? if neoforge {*/
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
/*?}*/

/*? if neoforge {*/
@Mod(SpongePlus.MODID)
/*?}*/
/*? if fabric {*/
/*public class SpongePlus implements ModInitializer {*/
/*?}*/
/*? if neoforge {*/
public class SpongePlus {
/*?}*/

    public static final String MODID = "sponge_plus";

    /*? if neoforge {*/
    public SpongePlus(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
    /*?}*/

    /*? if fabric {*/
    /*@Override
    public void onInitialize() {
        Config.init();
        SpongeDryingHandler.registerFabric();
        SpongeWeatherHandler.registerFabric();
        SpongeLootHandler.registerFabric();
    }*/
    /*?}*/
}
