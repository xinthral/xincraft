package com.xincraft.xincraft.transport;

import java.util.Set;
import com.xincraft.xincraft.block.BuoyBlockEntity;
import com.xincraft.xincraft.block.ChargingRailBlockEntity;
import com.xincraft.xincraft.block.EnderRailBlockEntity;
import com.xincraft.xincraft.common.Registers;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

public final class TransportBlockEntities {
    public static final RegistryObject<BlockEntityType<EnderRailBlockEntity>> ENDER_RAIL = Registers.BLOCK_ENTITIES.register("ender_rail",
        () -> new BlockEntityType<>(EnderRailBlockEntity::new, Set.of(TransportBlocks.ENDER_RAIL.get())));

    public static final RegistryObject<BlockEntityType<ChargingRailBlockEntity>> CHARGING_RAIL = Registers.BLOCK_ENTITIES.register("charging_rail",
        () -> new BlockEntityType<>(ChargingRailBlockEntity::new, Set.of(TransportBlocks.CHARGING_RAIL.get())));

    public static final RegistryObject<BlockEntityType<BuoyBlockEntity>> BUOY = Registers.BLOCK_ENTITIES.register("buoy",
        () -> new BlockEntityType<>(BuoyBlockEntity::new, Set.of(TransportBlocks.BUOY.get(), TransportBlocks.ADVANCED_BUOY.get())));

    // Loading this class is what registers everything above
    static void init() {}

    private TransportBlockEntities() {}
}
