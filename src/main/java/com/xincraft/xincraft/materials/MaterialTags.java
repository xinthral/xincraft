package com.xincraft.xincraft.materials;

import com.xincraft.xincraft.XincraftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class MaterialTags {
    // c:ingots/tin - common cross-mod tag identifying tin ingots
    public static final TagKey<Item> TIN_INGOTS =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/tin"));

    // xincraft:repairs_tin_armor - repair ingredient tag for tin armor, mirroring vanilla's
    // per-material repair tags (e.g. minecraft:repairs_copper_armor)
    public static final TagKey<Item> REPAIRS_TIN_ARMOR =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "repairs_tin_armor"));

    // xincraft:repairs_lonsdaleite_armor - repair ingredient tag for lonsdaleite armor
    public static final TagKey<Item> REPAIRS_LONSDALEITE_ARMOR =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "repairs_lonsdaleite_armor"));
    // xincraft:lonsdaleite_armor - the pieces client.LonsdaleiteGlintLayer draws its shimmer on
    public static final TagKey<Item> LONSDALEITE_ARMOR =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "lonsdaleite_armor"));
    // xincraft:repairs_bronze_armor - repair ingredient tag for bronze armor
    public static final TagKey<Item> REPAIRS_BRONZE_ARMOR =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "repairs_bronze_armor"));

    private MaterialTags() {}
}
