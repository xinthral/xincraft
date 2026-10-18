package com.xincraft.xincraft.world;

import com.mojang.serialization.MapCodec;
import com.xincraft.xincraft.Config;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ForgeBiomeModifiers.AddFeaturesBiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo.BiomeInfo;

// Forge's add_features biome modifier (same JSON fields), except that it does nothing when tin
// ore generation is turned off in the config, e.g. in a modpack where another mod already
// generates tin. Biome modifiers run as a world loads, so a change applies from the next load,
// and only to chunks generated after that.
public record TinOreBiomeModifier(AddFeaturesBiomeModifier features) implements BiomeModifier {
    public static final MapCodec<TinOreBiomeModifier> CODEC =
        AddFeaturesBiomeModifier.CODEC.xmap(TinOreBiomeModifier::new, TinOreBiomeModifier::features);

    @Override
    public void modify(Holder<Biome> biome, Phase phase, BiomeInfo.Builder builder) {
        if (Config.generateTinOre) {
            features.modify(biome, phase, builder);
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
