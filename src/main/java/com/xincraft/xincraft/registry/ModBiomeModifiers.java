package com.xincraft.xincraft.registry;

import com.mojang.serialization.MapCodec;
import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.world.TinOreBiomeModifier;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// Biome modifier types, used as the "type" in data/xincraft/forge/biome_modifier/*.json
public final class ModBiomeModifiers {
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> SERIALIZERS =
        DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, XincraftMod.MODID);

    public static final RegistryObject<MapCodec<TinOreBiomeModifier>> TIN_ORE =
        SERIALIZERS.register("tin_ore", () -> TinOreBiomeModifier.CODEC);

    private ModBiomeModifiers() {}
}
