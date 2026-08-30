package io.jayce.sponge_plus;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class ModTags {
    public static final TagKey<Block> DRYING_BLOCKS = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Sponge_plus.MODID, "drying_blocks")
    );
}
