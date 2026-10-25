package com.xincraft.xincraft.registry;

import java.util.Set;
import java.util.stream.Collectors;
import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.block.ApothecaryAltarBlockEntity;
import com.xincraft.xincraft.block.BuoyBlockEntity;
import com.xincraft.xincraft.block.ChargingRailBlockEntity;
import com.xincraft.xincraft.block.EnderRailBlockEntity;
import com.xincraft.xincraft.block.FloraBlockEntity;
import com.xincraft.xincraft.block.HeatTurbineBlockEntity;
import com.xincraft.xincraft.block.AuraFlowerBlockEntity;
import com.xincraft.xincraft.block.MixingTrayBlockEntity;
import com.xincraft.xincraft.block.SteamTurbineBlockEntity;
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

    public static final RegistryObject<BlockEntityType<HeatTurbineBlockEntity>> HEAT_TURBINE = BLOCK_ENTITIES.register("heat_turbine",
        () -> new BlockEntityType<>(HeatTurbineBlockEntity::new, Set.of(ModBlocks.HEAT_TURBINE.get())));

    public static final RegistryObject<BlockEntityType<SteamTurbineBlockEntity>> STEAM_TURBINE = BLOCK_ENTITIES.register("steam_turbine",
        () -> new BlockEntityType<>(SteamTurbineBlockEntity::new, Set.of(ModBlocks.STEAM_TURBINE.get())));

    public static final RegistryObject<BlockEntityType<MixingTrayBlockEntity>> MIXING_TRAY = BLOCK_ENTITIES.register("mixing_tray",
        () -> new BlockEntityType<>(MixingTrayBlockEntity::new, Set.of(ModBlocks.MIXING_TRAY.get())));

    // The aura flowers share one block entity class, with a type each (block.AuraFlowerBlock)
    public static final RegistryObject<BlockEntityType<AuraFlowerBlockEntity>> MENDING_LILAC = BLOCK_ENTITIES.register("mending_lilac",
        () -> new BlockEntityType<>(AuraFlowerBlockEntity::new, Set.of(ModBlocks.MENDING_LILAC.get())));

    public static final RegistryObject<BlockEntityType<AuraFlowerBlockEntity>> MOONLIT_DANDELION = BLOCK_ENTITIES.register("moonlit_dandelion",
        () -> new BlockEntityType<>(AuraFlowerBlockEntity::new, Set.of(ModBlocks.MOONLIT_DANDELION.get())));

    public static final RegistryObject<BlockEntityType<AuraFlowerBlockEntity>> SHIMMER_LILY = BLOCK_ENTITIES.register("shimmer_lily",
        () -> new BlockEntityType<>(AuraFlowerBlockEntity::new, Set.of(ModBlocks.SHIMMER_LILY.get())));

    // All the elemental flora share one type (block.FloraBlock)
    public static final RegistryObject<BlockEntityType<FloraBlockEntity>> FLORA = BLOCK_ENTITIES.register("flora",
        () -> new BlockEntityType<>(FloraBlockEntity::new,
            ModBlocks.FLORA.values().stream().map(RegistryObject::get).collect(Collectors.toUnmodifiableSet())));

    public static final RegistryObject<BlockEntityType<ApothecaryAltarBlockEntity>> APOTHECARY_ALTAR = BLOCK_ENTITIES.register("apothecary_altar",
        () -> new BlockEntityType<>(ApothecaryAltarBlockEntity::new, Set.of(ModBlocks.APOTHECARY_ALTAR.get())));

    public static final RegistryObject<BlockEntityType<BuoyBlockEntity>> BUOY = BLOCK_ENTITIES.register("buoy",
        () -> new BlockEntityType<>(BuoyBlockEntity::new, Set.of(ModBlocks.BUOY.get(), ModBlocks.ADVANCED_BUOY.get())));

    private ModBlockEntities() {}
}
