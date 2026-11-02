package com.xincraft.xincraft.golems;

import com.xincraft.xincraft.block.BronzeChestBlock;
import com.xincraft.xincraft.common.Registers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.RegistryObject;

public final class GolemMenus {
    // Two bronze chests joined: vanilla's chest menu, at a size it has no menu type for
    public static final RegistryObject<MenuType<ChestMenu>> BRONZE_CHEST_DOUBLE = Registers.MENU_TYPES.register("bronze_chest_double",
        () -> new MenuType<>((containerId, inventory) -> new ChestMenu(GolemMenus.BRONZE_CHEST_DOUBLE.get(), containerId, inventory,
            new SimpleContainer(BronzeChestBlock.DOUBLE_ROWS * 9), BronzeChestBlock.DOUBLE_ROWS), FeatureFlags.VANILLA_SET));

    // Loading this class is what registers everything above
    static void init() {}

    private GolemMenus() {}
}
