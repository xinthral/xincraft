package com.xincraft.xincraft.materials;

import com.xincraft.xincraft.XincraftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.trim.TrimMaterial;

public final class ModTrimMaterials {
    /**
     * @brief Tin as an armor trim material, which tin ingots provide.
     *
     * @see MaterialItems#TIN_INGOT
     */
    public static final ResourceKey<TrimMaterial> TIN =
        ResourceKey.create(Registries.TRIM_MATERIAL, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "tin"));

    private ModTrimMaterials() {}
}
