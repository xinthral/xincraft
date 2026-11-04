package com.xincraft.xincraft.transport;

import java.util.UUID;
import com.xincraft.xincraft.common.Registers;
import com.mojang.serialization.Codec;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraftforge.registries.RegistryObject;

public final class TransportComponents {
    /**
     * @brief The placed ender rail an ender rail item was attuned to; placing the item links the
     *        two.
     */
    public static final RegistryObject<DataComponentType<GlobalPos>> ENDER_RAIL_LINK = Registers.COMPONENTS.register("ender_rail_link",
        () -> DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());

    /**
     * @brief Game time at which a temporary enchantment glint on an ender rail item should be
     *        removed.
     */
    public static final RegistryObject<DataComponentType<Long>> GLINT_UNTIL = Registers.COMPONENTS.register("glint_until",
        () -> DataComponentType.<Long>builder().persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG).build());

    /**
     * @brief The cart a linking bar was used on first; using it on a second cart hitches the two
     *        together.
     */
    public static final RegistryObject<DataComponentType<UUID>> LINKING_BAR_START = Registers.COMPONENTS.register("linking_bar_start",
        () -> DataComponentType.<UUID>builder().persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC).build());

    /**
     * @brief The block a linking bar was used on first; using it on a second links the two.
     *
     * Named for buoys, the first blocks to link, and kept so that bars already in worlds stay
     * valid.
     */
    public static final RegistryObject<DataComponentType<GlobalPos>> LINKING_BAR_BLOCK = Registers.COMPONENTS.register("linking_bar_buoy",
        () -> DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());

    /** @brief Loading this class is what registers everything above. */
    static void init() {}

    private TransportComponents() {}
}
