package com.xincraft.xincraft.registry;

import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.inventory.LocomotiveCartMenu;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, XincraftMod.MODID);

    public static final RegistryObject<MenuType<LocomotiveCartMenu>> LOCOMOTIVE_CART = MENU_TYPES.register("locomotive_cart",
        () -> new MenuType<>(LocomotiveCartMenu::new, FeatureFlags.VANILLA_SET));

    private ModMenuTypes() {}
}
