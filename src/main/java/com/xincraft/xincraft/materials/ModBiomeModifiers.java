package com.xincraft.xincraft.materials;

import com.mojang.serialization.MapCodec;
import com.xincraft.xincraft.XincraftMod;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** @brief Biome modifier types, for the biome modifier data files to use. */
public final class ModBiomeModifiers {
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> SERIALIZERS =
        DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, XincraftMod.MODID);

    public static final RegistryObject<MapCodec<TinOreBiomeModifier>> TIN_ORE =
        SERIALIZERS.register("tin_ore", () -> TinOreBiomeModifier.CODEC);

    public static final RegistryObject<MapCodec<SilverOreBiomeModifier>> SILVER_ORE =
        SERIALIZERS.register("silver_ore", () -> SilverOreBiomeModifier.CODEC);

    private ModBiomeModifiers() {}
}
