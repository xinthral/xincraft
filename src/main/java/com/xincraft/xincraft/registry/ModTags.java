package com.xincraft.xincraft.registry;

import com.xincraft.xincraft.XincraftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    // c:ingots/tin - common cross-mod tag identifying tin ingots
    public static final TagKey<Item> TIN_INGOTS =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/tin"));

    // xincraft:repairs_tin_armor - repair ingredient tag for tin armor, mirroring vanilla's
    // per-material repair tags (e.g. minecraft:repairs_copper_armor)
    public static final TagKey<Item> REPAIRS_TIN_ARMOR =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "repairs_tin_armor"));

    // xincraft:padded_rails - every colour of padded rail and padded powered rail; minecart
    // sounds are muffled on these (see client.PaddedRailSounds)
    public static final TagKey<Block> PADDED_RAILS =
        TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "padded_rails"));

    // xincraft:tin_rails - rails on which minecarts can go faster (see entity.TinRailMinecartBehavior)
    public static final TagKey<Block> TIN_RAILS =
        TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "tin_rails"));

    private ModTags() {}
}
