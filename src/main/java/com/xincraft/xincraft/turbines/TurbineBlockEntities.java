package com.xincraft.xincraft.turbines;

import java.util.Set;
import com.xincraft.xincraft.block.HeatTurbineBlockEntity;
import com.xincraft.xincraft.block.SteamTurbineBlockEntity;
import com.xincraft.xincraft.common.Registers;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

public final class TurbineBlockEntities {
    public static final RegistryObject<BlockEntityType<HeatTurbineBlockEntity>> HEAT_TURBINE = Registers.BLOCK_ENTITIES.register("heat_turbine",
        () -> new BlockEntityType<>(HeatTurbineBlockEntity::new, Set.of(TurbineBlocks.HEAT_TURBINE.get())));

    public static final RegistryObject<BlockEntityType<SteamTurbineBlockEntity>> STEAM_TURBINE = Registers.BLOCK_ENTITIES.register("steam_turbine",
        () -> new BlockEntityType<>(SteamTurbineBlockEntity::new, Set.of(TurbineBlocks.STEAM_TURBINE.get())));

    // Loading this class is what registers everything above
    static void init() {}

    private TurbineBlockEntities() {}
}
