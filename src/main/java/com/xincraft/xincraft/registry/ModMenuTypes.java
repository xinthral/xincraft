package com.xincraft.xincraft.registry;

import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.inventory.ApothecaryAltarMenu;
import com.xincraft.xincraft.inventory.ApothecaryCrucibleMenu;
import com.xincraft.xincraft.inventory.LocomotiveCartMenu;
import com.xincraft.xincraft.inventory.TurbineMenu;
import com.xincraft.xincraft.block.BronzeChestBlock;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, XincraftMod.MODID);

    public static final RegistryObject<MenuType<LocomotiveCartMenu>> LOCOMOTIVE_CART = MENU_TYPES.register("locomotive_cart",
        () -> new MenuType<>(LocomotiveCartMenu::new, FeatureFlags.VANILLA_SET));

    // Shared by the heat and steam turbines
    public static final RegistryObject<MenuType<TurbineMenu>> TURBINE = MENU_TYPES.register("turbine",
        () -> new MenuType<>(TurbineMenu::new, FeatureFlags.VANILLA_SET));

    public static final RegistryObject<MenuType<ApothecaryAltarMenu>> APOTHECARY_ALTAR = MENU_TYPES.register("apothecary_altar",
        () -> new MenuType<>(ApothecaryAltarMenu::new, FeatureFlags.VANILLA_SET));

    public static final RegistryObject<MenuType<ApothecaryCrucibleMenu>> APOTHECARY_CRUCIBLE = MENU_TYPES.register("apothecary_crucible",
        () -> new MenuType<>(ApothecaryCrucibleMenu::new, FeatureFlags.VANILLA_SET));

    // Two bronze chests joined: vanilla's chest menu, at a size it has no menu type for
    public static final RegistryObject<MenuType<ChestMenu>> BRONZE_CHEST_DOUBLE = MENU_TYPES.register("bronze_chest_double",
        () -> new MenuType<>((containerId, inventory) -> new ChestMenu(ModMenuTypes.BRONZE_CHEST_DOUBLE.get(), containerId, inventory,
            new SimpleContainer(BronzeChestBlock.DOUBLE_ROWS * 9), BronzeChestBlock.DOUBLE_ROWS), FeatureFlags.VANILLA_SET));

    private ModMenuTypes() {}
}
