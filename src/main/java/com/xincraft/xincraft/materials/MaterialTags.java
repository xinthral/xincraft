package com.xincraft.xincraft.materials;

import com.xincraft.xincraft.XincraftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class MaterialTags {
    /** @brief Common cross-mod tag identifying tin ingots. */
    public static final TagKey<Item> TIN_INGOTS =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/tin"));

    /** @brief Repair ingredients for tin armor, mirroring vanilla's per-material repair tags. */
    public static final TagKey<Item> REPAIRS_TIN_ARMOR =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "repairs_tin_armor"));

    /** @brief Repair ingredients for lonsdaleite armor. */
    public static final TagKey<Item> REPAIRS_LONSDALEITE_ARMOR =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "repairs_lonsdaleite_armor"));
    /**
     * @brief The pieces LonsdaleiteGlintLayer draws its shimmer on.
     *
     * @see materials.client.LonsdaleiteGlintLayer
     */
    public static final TagKey<Item> LONSDALEITE_ARMOR =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "lonsdaleite_armor"));
    /** @brief Repair ingredients for bronze armor. */
    public static final TagKey<Item> REPAIRS_BRONZE_ARMOR =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "repairs_bronze_armor"));

    private MaterialTags() {}
}
