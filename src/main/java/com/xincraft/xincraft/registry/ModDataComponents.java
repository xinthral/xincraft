package com.xincraft.xincraft.registry;

import com.mojang.serialization.Codec;
import com.xincraft.xincraft.XincraftMod;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
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

    private ModDataComponents() {}
}
