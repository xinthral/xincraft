package com.xincraft.xincraft.materials;

import java.util.Map;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

public final class ModArmorMaterials {
    /** @brief Positioned between Copper (durability 11) and Iron (durability 15). */
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
        MaterialTags.REPAIRS_TIN_ARMOR,
        ModEquipmentAssets.TIN
    );

    /**
     * @brief Iron's protection with more durability (iron is 15) and better enchantability (iron is
     *        9).
     */
    public static final ArmorMaterial BRONZE = new ArmorMaterial(
        17,
        Map.of(
            ArmorType.BOOTS, 2,
            ArmorType.LEGGINGS, 5,
            ArmorType.CHESTPLATE, 6,
            ArmorType.HELMET, 2,
            ArmorType.BODY, 6
        ),
        12,
        SoundEvents.ARMOR_EQUIP_COPPER,
        0.0F,
        0.0F,
        MaterialTags.REPAIRS_BRONZE_ARMOR,
        ModEquipmentAssets.BRONZE
    );

    /** @brief Netherite's values throughout. */
    public static final ArmorMaterial LONSDALEITE = new ArmorMaterial(
        37,
        Map.of(
            ArmorType.BOOTS, 3,
            ArmorType.LEGGINGS, 6,
            ArmorType.CHESTPLATE, 8,
            ArmorType.HELMET, 3,
            ArmorType.BODY, 19
        ),
        15,
        SoundEvents.ARMOR_EQUIP_NETHERITE,
        3.0F,
        0.1F,
        MaterialTags.REPAIRS_LONSDALEITE_ARMOR,
        ModEquipmentAssets.LONSDALEITE
    );

    private ModArmorMaterials() {}
}
