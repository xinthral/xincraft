package com.xincraft.xincraft.golems;

import com.xincraft.xincraft.item.BronzeGolemHelmItem;
import com.xincraft.xincraft.item.BronzeGolemItem;
import com.xincraft.xincraft.common.Registers;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

public final class GolemItems {
    // Turns a copper golem into a bronze golem (item.BronzeGolemHelmItem)
    public static final RegistryObject<Item> BRONZE_GOLEM_HELM =
        Registers.registerItem("bronze_golem_helm", BronzeGolemHelmItem::new, () -> new Item.Properties().stacksTo(16));
    // A bronze golem packed up, and one armed at the apothecary altar (item.BronzeGolemItem)
    public static final RegistryObject<Item> BRONZE_GOLEM =
        Registers.registerItem("bronze_golem", BronzeGolemItem::new, () -> new Item.Properties().stacksTo(1));
    public static final RegistryObject<Item> COMBAT_GOLEM =
        Registers.registerItem("combat_golem", BronzeGolemItem::new, () -> new Item.Properties().stacksTo(1));

    // Loading this class is what registers everything above
    static void init() {}

    private GolemItems() {}
}
