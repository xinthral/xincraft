package com.xincraft.xincraft.registry;

import com.xincraft.xincraft.XincraftMod;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public final class ModEquipmentAssets {
    public static final ResourceKey<EquipmentAsset> TIN =
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "tin"));

    private ModEquipmentAssets() {}
}
