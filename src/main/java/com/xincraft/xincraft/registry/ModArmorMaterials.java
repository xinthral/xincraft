package com.xincraft.xincraft.registry;

import java.util.Map;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

public final class ModArmorMaterials {
    // Positioned between Copper (durability 11) and Iron (durability 15)
    public static final ArmorMaterial TIN = new ArmorMaterial(
        13,
        Map.of(
            ArmorType.BOOTS, 1,
            ArmorType.LEGGINGS, 4,
            ArmorType.CHESTPLATE, 5,
            ArmorType.HELMET, 2,
            ArmorType.BODY, 5
        ),
        10,
        SoundEvents.ARMOR_EQUIP_IRON,
        0.0F,
        0.0F,
        ModTags.REPAIRS_TIN_ARMOR,
        ModEquipmentAssets.TIN
    );

    private ModArmorMaterials() {}
}
