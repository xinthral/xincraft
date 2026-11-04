package com.xincraft.xincraft.magic;

import com.xincraft.xincraft.common.Registers;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.RegistryObject;

public final class MagicMenus {
    public static final RegistryObject<MenuType<ApothecaryAltarMenu>> APOTHECARY_ALTAR = Registers.MENU_TYPES.register("apothecary_altar",
        () -> new MenuType<>(ApothecaryAltarMenu::new, FeatureFlags.VANILLA_SET));

    public static final RegistryObject<MenuType<ApothecaryCrucibleMenu>> APOTHECARY_CRUCIBLE = Registers.MENU_TYPES.register("apothecary_crucible",
        () -> new MenuType<>(ApothecaryCrucibleMenu::new, FeatureFlags.VANILLA_SET));

    /** @brief Loading this class is what registers everything above. */
    static void init() {}

    private MagicMenus() {}
}
