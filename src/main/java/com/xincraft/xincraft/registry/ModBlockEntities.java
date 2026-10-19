package com.xincraft.xincraft.registry;

import java.util.Set;
import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.block.ChargingRailBlockEntity;
import com.xincraft.xincraft.block.EnderRailBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, XincraftMod.MODID);

    public static final RegistryObject<BlockEntityType<EnderRailBlockEntity>> ENDER_RAIL = BLOCK_ENTITIES.register("ender_rail",
        () -> new BlockEntityType<>(EnderRailBlockEntity::new, Set.of(ModBlocks.ENDER_RAIL.get())));

    public static final RegistryObject<BlockEntityType<ChargingRailBlockEntity>> CHARGING_RAIL = BLOCK_ENTITIES.register("charging_rail",
        () -> new BlockEntityType<>(ChargingRailBlockEntity::new, Set.of(ModBlocks.CHARGING_RAIL.get())));

    private ModBlockEntities() {}
}
