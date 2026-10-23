package com.xincraft.xincraft.registry;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.magic.Element;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    // minecraft:small_flowers - the flowers that can be mixed on a mixing tray
    public static final TagKey<Item> SMALL_FLOWERS =
        TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("small_flowers"));

    // c:ingots/tin - common cross-mod tag identifying tin ingots
    public static final TagKey<Item> TIN_INGOTS =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/tin"));

    // xincraft:repairs_tin_armor - repair ingredient tag for tin armor, mirroring vanilla's
    // per-material repair tags (e.g. minecraft:repairs_copper_armor)
    public static final TagKey<Item> REPAIRS_TIN_ARMOR =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "repairs_tin_armor"));

    // xincraft:repairs_bronze_armor - repair ingredient tag for bronze armor
    public static final TagKey<Item> REPAIRS_BRONZE_ARMOR =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "repairs_bronze_armor"));

    // xincraft:padded_rails - every colour of padded rail and padded powered rail; minecart
    // sounds are muffled on these (see client.PaddedRailSounds)
    public static final TagKey<Block> PADDED_RAILS =
        TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "padded_rails"));

    // xincraft:tin_rails - rails on which minecarts can go faster (see entity.XincraftMinecartBehavior)
    public static final TagKey<Block> TIN_RAILS =
        TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "tin_rails"));

    // xincraft:honey_rails - rails that hold minecarts at a reduced, steady speed
    // (see entity.XincraftMinecartBehavior)
    public static final TagKey<Block> HONEY_RAILS =
        TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "honey_rails"));

    // xincraft:bouncy_rails - rails that launch minecarts upward when powered
    // (see entity.XincraftMinecartBehavior)
    public static final TagKey<Block> BOUNCY_RAILS =
        TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "bouncy_rails"));

    // xincraft:inspector_rail_signal/1 to /15 - minecart entity types and the redstone signal the
    // inspector rail gives for them (see block.InspectorRailBlock). Add a new cart type by listing
    // it in the tag for its signal strength.
    private static final List<TagKey<EntityType<?>>> INSPECTOR_RAIL_SIGNALS = IntStream.rangeClosed(0, 15)
        .mapToObj(signal -> TagKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(XincraftMod.MODID, "inspector_rail_signal/" + signal)))
        .toList();

    public static TagKey<EntityType<?>> inspectorRailSignal(int signal) {
        return INSPECTOR_RAIL_SIGNALS.get(signal);
    }

    // xincraft:element_powders - every elemental powder (item.ElementPowderItem)
    public static final TagKey<Item> ELEMENT_POWDERS =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "element_powders"));

    // xincraft:essence/terra to /umbra - the plants the apothecary altar burns into each element's
    // essence (see block.ApothecaryAltarBlockEntity). A plant can be in several, giving each.
    private static final Map<Element, TagKey<Item>> ESSENCE = new EnumMap<>(Element.class);

    static {
        for (Element element : Element.values()) {
            ESSENCE.put(element, TagKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(XincraftMod.MODID, "essence/" + element.getSerializedName())));
        }
    }

    public static TagKey<Item> essence(Element element) {
        return ESSENCE.get(element);
    }

    private ModTags() {}
}
