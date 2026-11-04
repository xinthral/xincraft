package com.xincraft.xincraft.magic;

import java.util.EnumMap;
import java.util.Map;
import com.xincraft.xincraft.XincraftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class MagicTags {
    /**
     * @brief xincraft:element_powders - every elemental powder.
     *
     * @see ElementPowderItem
     */
    public static final TagKey<Item> ELEMENT_POWDERS =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "element_powders"));

    /**
     * @brief The plants the apothecary altar burns into each element's essence.
     *
     * A plant can be in several of these tags, giving each element.
     *
     * @see ApothecaryAltarBlockEntity
     */
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
