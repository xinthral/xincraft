package com.xincraft.xincraft.transport;

import com.xincraft.xincraft.transport.cart.LocomotiveCartMenu;
import com.xincraft.xincraft.common.Registers;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.RegistryObject;

public final class TransportMenus {
    public static final RegistryObject<MenuType<LocomotiveCartMenu>> LOCOMOTIVE_CART = Registers.MENU_TYPES.register("locomotive_cart",
        () -> new MenuType<>(LocomotiveCartMenu::new, FeatureFlags.VANILLA_SET));

    /** @brief Loading this class is what registers everything above. */
    static void init() {}

    private TransportMenus() {}
}
