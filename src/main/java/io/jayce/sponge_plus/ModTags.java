package io.jayce.sponge_plus;

import net.minecraft.core.registries.Registries;
/*? if <26.2 {*/
import net.minecraft.resources.ResourceLocation;
/*?}*/
/*? if >=26.2 {*/
/*import net.minecraft.resources.Identifier;
*/
/*?}*/
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    public static final TagKey<Block> DRYING_BLOCKS = TagKey.create(
            Registries.BLOCK,
            /*? if <26.2 {*/
            ResourceLocation.fromNamespaceAndPath(SpongePlus.MODID, "drying_blocks")
            /*?}*/
            /*? if >=26.2 {*/
            /*Identifier.fromNamespaceAndPath(SpongePlus.MODID, "drying_blocks")*/
            /*?}*/
    );

    private ModTags() {}
}
