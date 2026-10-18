package com.xincraft.xincraft.registry;

import com.xincraft.xincraft.XincraftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.trim.TrimMaterial;

public final class ModTrimMaterials {
    // Tin as an armor trim material, defined by data/xincraft/trim_material/tin.json. Tin ingots
    // provide it (ModItems.TIN_INGOT); its colors are the "tin" palettes added to the trim atlases
    // in assets/minecraft/atlases/.
    public static final ResourceKey<TrimMaterial> TIN =
        ResourceKey.create(Registries.TRIM_MATERIAL, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "tin"));

    private ModTrimMaterials() {}
}
