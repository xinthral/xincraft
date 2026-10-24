package com.xincraft.xincraft.registry;

import java.util.UUID;
import com.mojang.serialization.Codec;
import com.xincraft.xincraft.XincraftMod;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.Unit;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, XincraftMod.MODID);

    // The placed ender rail an ender rail item was attuned to; placing the item links the two
    public static final RegistryObject<DataComponentType<GlobalPos>> ENDER_RAIL_LINK = COMPONENTS.register("ender_rail_link",
        () -> DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());

    // Game time at which a temporary enchantment glint on an ender rail item should be removed
    public static final RegistryObject<DataComponentType<Long>> GLINT_UNTIL = COMPONENTS.register("glint_until",
        () -> DataComponentType.<Long>builder().persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG).build());

    // Marks a vanilla spyglass as a tin spyglass (see item.TinSpyglass)
    public static final RegistryObject<DataComponentType<Unit>> TIN_LENS = COMPONENTS.register("tin_lens",
        () -> DataComponentType.<Unit>builder().persistent(Unit.CODEC).networkSynchronized(Unit.STREAM_CODEC).build());

    // The cart a linking bar was used on first; using it on a second cart hitches the two together
    public static final RegistryObject<DataComponentType<UUID>> LINKING_BAR_START = COMPONENTS.register("linking_bar_start",
        () -> DataComponentType.<UUID>builder().persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC).build());

    // The buoy a linking bar was used on first; using it on a second buoy links the two
    public static final RegistryObject<DataComponentType<GlobalPos>> LINKING_BAR_BUOY = COMPONENTS.register("linking_bar_buoy",
        () -> DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());

    private ModDataComponents() {}
}
