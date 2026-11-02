package com.xincraft.xincraft.flowers;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class FlowerTags {
    // minecraft:small_flowers - the flowers that can be mixed on a mixing tray
    public static final TagKey<Item> SMALL_FLOWERS =
        TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("small_flowers"));

    private FlowerTags() {}
}
