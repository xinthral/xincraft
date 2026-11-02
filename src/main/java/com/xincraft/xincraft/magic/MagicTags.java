package com.xincraft.xincraft.magic;

import java.util.EnumMap;
import java.util.Map;
import com.xincraft.xincraft.XincraftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class MagicTags {
    // xincraft:element_powders - every elemental powder (magic.ElementPowderItem)
    public static final TagKey<Item> ELEMENT_POWDERS =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "element_powders"));

    // xincraft:essence/terra to /umbra - the plants the apothecary altar burns into each element's
    // essence (see magic.ApothecaryAltarBlockEntity). A plant can be in several, giving each.
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

    private MagicTags() {}
}
