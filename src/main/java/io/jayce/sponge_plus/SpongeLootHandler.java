package io.jayce.sponge_plus;

/*? if fabric {*/
/*import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.Set;
*/
/*?}*/

// NeoForge gets this via the data-driven loot_modifiers in resources; Fabric has no vanilla-datapack
// equivalent for extending an existing loot table, so it's done here instead.
public final class SpongeLootHandler {

    private SpongeLootHandler() {}

    /*? if fabric {*/
    /*private static final Set<ResourceKey<LootTable>> TARGET_TABLES = Set.of(
            BuiltInLootTables.SHIPWRECK_SUPPLY,
            BuiltInLootTables.UNDERWATER_RUIN_BIG,
            BuiltInLootTables.UNDERWATER_RUIN_SMALL);

    public static void registerFabric() {
        LootTableEvents.MODIFY.register((key, builder, source, lookup) -> {
            if (!TARGET_TABLES.contains(key)) return;

            builder.withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0F))
                    .add(LootItem.lootTableItem(Items.WET_SPONGE).setWeight(1))
                    .add(EmptyLootItem.emptyItem().setWeight(3)));
        });
    }*/
    /*?}*/
}
