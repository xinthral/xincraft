package com.xincraft.xincraft.magic;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import com.xincraft.xincraft.common.Registers;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

public final class MagicItems {
    /**
     * @brief Grinds flowers and vines into elemental powders in the crafting grid, wearing down
     *        instead of being used up.
     */
    public static final RegistryObject<Item> MORTAR_AND_PESTLE = Registers.registerItem("mortar_and_pestle", MortarAndPestleItem::new,
        () -> new Item.Properties().durability(64));

    /**
     * @brief One powder per element (e.g. ignis_powder); turns small flowers into elemental flora.
     */
    public static final Map<Element, RegistryObject<Item>> ELEMENT_POWDERS = registerElementPowders();

    private static Map<Element, RegistryObject<Item>> registerElementPowders() {
        Map<Element, RegistryObject<Item>> items = new EnumMap<>(Element.class);
        for (Element element : Element.values()) {
            items.put(element, Registers.registerItem(element.powderId(),
                properties -> new ElementPowderItem(element, properties), Item.Properties::new));
        }
        return Collections.unmodifiableMap(items);
    }

    /** @brief Loading this class is what registers everything above. */
    static void init() {}

    private MagicItems() {}
}
