package com.xincraft.xincraft.golems;

import com.xincraft.xincraft.common.Registers;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

public final class GolemItems {
    /**
     * @brief Turns a copper golem into a bronze golem.
     *
     * @see BronzeGolemHelmItem
     */
    public static final RegistryObject<Item> BRONZE_GOLEM_HELM =
        Registers.registerItem("bronze_golem_helm", BronzeGolemHelmItem::new, () -> new Item.Properties().stacksTo(16));
    /**
     * @brief A bronze golem packed up.
     *
     * @see BronzeGolemItem
     */
    public static final RegistryObject<Item> BRONZE_GOLEM =
        Registers.registerItem("bronze_golem", BronzeGolemItem::new, () -> new Item.Properties().stacksTo(1));
    /** @brief A packed-up bronze golem armed at the apothecary altar. */
    public static final RegistryObject<Item> COMBAT_GOLEM =
        Registers.registerItem("combat_golem", BronzeGolemItem::new, () -> new Item.Properties().stacksTo(1));

    /** @brief Loading this class is what registers everything above. */
    static void init() {}

    private GolemItems() {}
}
