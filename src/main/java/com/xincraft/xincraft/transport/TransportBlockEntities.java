package com.xincraft.xincraft.transport;

import java.util.Set;
import com.xincraft.xincraft.transport.boat.BuoyBlockEntity;
import com.xincraft.xincraft.transport.rail.ChargingRailBlockEntity;
import com.xincraft.xincraft.transport.rail.ElectrumChargingRailBlockEntity;
import com.xincraft.xincraft.transport.rail.EnderRailBlockEntity;
import com.xincraft.xincraft.common.Registers;
import com.xincraft.xincraft.transport.power.ChargingStationBlockEntity;
import com.xincraft.xincraft.transport.power.SubstationBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

public final class TransportBlockEntities {
    public static final RegistryObject<BlockEntityType<EnderRailBlockEntity>> ENDER_RAIL = Registers.BLOCK_ENTITIES.register("ender_rail",
        () -> new BlockEntityType<>(EnderRailBlockEntity::new, Set.of(TransportBlocks.ENDER_RAIL.get())));

    public static final RegistryObject<BlockEntityType<ChargingRailBlockEntity>> CHARGING_RAIL = Registers.BLOCK_ENTITIES.register("charging_rail",
        () -> new BlockEntityType<>(ChargingRailBlockEntity::new, Set.of(TransportBlocks.CHARGING_RAIL.get())));

    public static final RegistryObject<BlockEntityType<ElectrumChargingRailBlockEntity>> ELECTRUM_CHARGING_RAIL = Registers.BLOCK_ENTITIES.register("electrum_charging_rail",
        () -> new BlockEntityType<>(ElectrumChargingRailBlockEntity::new, Set.of(TransportBlocks.ELECTRUM_CHARGING_RAIL.get())));

    public static final RegistryObject<BlockEntityType<BuoyBlockEntity>> BUOY = Registers.BLOCK_ENTITIES.register("buoy",
        () -> new BlockEntityType<>(BuoyBlockEntity::new, Set.of(TransportBlocks.BUOY.get(), TransportBlocks.ADVANCED_BUOY.get())));

    // Loading this class is what registers everything above
    static void init() {}

    public static final RegistryObject<BlockEntityType<ChargingStationBlockEntity>> CHARGING_STATION = Registers.BLOCK_ENTITIES.register("charging_station",
        () -> new BlockEntityType<>(ChargingStationBlockEntity::new, Set.of(TransportBlocks.CHARGING_STATION.get())));

    public static final RegistryObject<BlockEntityType<SubstationBlockEntity>> SUBSTATION = Registers.BLOCK_ENTITIES.register("substation",
        () -> new BlockEntityType<>(SubstationBlockEntity::new, Set.of(TransportBlocks.SUBSTATION.get())));

    private TransportBlockEntities() {}
}
