package com.xincraft.xincraft.turbines;

import com.xincraft.xincraft.common.Registers;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.RegistryObject;

public final class TurbineMenus {
    /** @brief Shared by the heat and steam turbines. */
    public static final RegistryObject<MenuType<TurbineMenu>> TURBINE = Registers.MENU_TYPES.register("turbine",
        () -> new MenuType<>(TurbineMenu::new, FeatureFlags.VANILLA_SET));

    /** @brief Loading this class is what registers everything above. */
    static void init() {}

    private TurbineMenus() {}
}
