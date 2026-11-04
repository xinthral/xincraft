package com.xincraft.xincraft.materials;

import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.common.Registers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraftforge.registries.RegistryObject;

public final class MaterialItems {
    /**
     * @brief Properties for a lonsdaleite armor piece.
     *
     * Fireproof like netherite. Vanilla's purple glint is off, even when enchanted, as the piece
     * has its own red shimmer.
     *
     * @param type The kind of piece.
     * @param attributes The piece's attribute modifiers.
     * @return The properties.
     */
    private static Item.Properties lonsdaleiteArmor(ArmorType type, ItemAttributeModifiers attributes) {
        return new Item.Properties().humanoidArmor(ModArmorMaterials.LONSDALEITE, type).attributes(attributes).fireResistant()
            .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
    }

    public static final RegistryObject<Item> RAW_TIN =
        Registers.registerItem("raw_tin", Item::new, Item.Properties::new);

    public static final RegistryObject<Item> TIN_INGOT =
        Registers.registerItem("tin_ingot", Item::new, () -> new Item.Properties().trimMaterial(ModTrimMaterials.TIN));

    public static final RegistryObject<Item> RAW_SILVER =
        Registers.registerItem("raw_silver", Item::new, Item.Properties::new);

    public static final RegistryObject<Item> SILVER_INGOT =
        Registers.registerItem("silver_ingot", Item::new, Item.Properties::new);

    /**
     * @brief Bronze: 3 raw copper and 1 raw tin craft into 4 raw bronze, which smelts into bronze
     *        ingots.
     */
    public static final RegistryObject<Item> RAW_BRONZE =
        Registers.registerItem("raw_bronze", Item::new, Item.Properties::new);

    public static final RegistryObject<Item> BRONZE_INGOT =
        Registers.registerItem("bronze_ingot", Item::new, Item.Properties::new);

    /**
     * @brief Electrum: raw gold and raw silver craft into 2 raw electrum, which smelts into
     *        electrum ingots.
     */
    public static final RegistryObject<Item> RAW_ELECTRUM =
        Registers.registerItem("raw_electrum", Item::new, Item.Properties::new);

    public static final RegistryObject<Item> ELECTRUM_INGOT =
        Registers.registerItem("electrum_ingot", Item::new, Item.Properties::new);

    public static final RegistryObject<Item> BRONZE_HELMET = Registers.registerItem("bronze_helmet", Item::new,
        () -> new Item.Properties().humanoidArmor(ModArmorMaterials.BRONZE, ArmorType.HELMET));
    public static final RegistryObject<Item> BRONZE_CHESTPLATE = Registers.registerItem("bronze_chestplate", Item::new,
        () -> new Item.Properties().humanoidArmor(ModArmorMaterials.BRONZE, ArmorType.CHESTPLATE));
    public static final RegistryObject<Item> BRONZE_LEGGINGS = Registers.registerItem("bronze_leggings", Item::new,
        () -> new Item.Properties().humanoidArmor(ModArmorMaterials.BRONZE, ArmorType.LEGGINGS));
    public static final RegistryObject<Item> BRONZE_BOOTS = Registers.registerItem("bronze_boots", Item::new,
        () -> new Item.Properties().humanoidArmor(ModArmorMaterials.BRONZE, ArmorType.BOOTS));

    /**
     * @brief Transmuted from a diamond at the apothecary altar; upgrades bronze armor there, and
     *        repairs the result.
     */
    public static final RegistryObject<Item> LONSDALEITE_SHARD =
        Registers.registerItem("lonsdaleite_shard", Item::new, Item.Properties::new);

    /**
     * @brief Bronze helmet upgraded at the apothecary altar.
     *
     * @see LonsdaleiteArmorItem
     */
    public static final RegistryObject<Item> LONSDALEITE_HELMET = Registers.registerItem("lonsdaleite_helmet", LonsdaleiteArmorItem::new,
        () -> lonsdaleiteArmor(ArmorType.HELMET, ModArmorMaterials.LONSDALEITE.createAttributes(ArmorType.HELMET)));
    /** @brief Bronze chestplate upgraded at the apothecary altar. */
    public static final RegistryObject<Item> LONSDALEITE_CHESTPLATE = Registers.registerItem("lonsdaleite_chestplate", LonsdaleiteArmorItem::new,
        () -> lonsdaleiteArmor(ArmorType.CHESTPLATE, ModArmorMaterials.LONSDALEITE.createAttributes(ArmorType.CHESTPLATE)));
    /** @brief Walks straight up full-block steps, from a step height of 0.6 to 1.0. */
    public static final RegistryObject<Item> LONSDALEITE_LEGGINGS = Registers.registerItem("lonsdaleite_leggings", LonsdaleiteArmorItem::new,
        () -> lonsdaleiteArmor(ArmorType.LEGGINGS, ModArmorMaterials.LONSDALEITE.createAttributes(ArmorType.LEGGINGS).withModifierAdded(
            Attributes.STEP_HEIGHT, new AttributeModifier(Identifier.fromNamespaceAndPath(XincraftMod.MODID, "lonsdaleite_leggings_step_height"),
                0.4, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.LEGS)));
    /** @brief The same 20% as Speed I. */
    public static final RegistryObject<Item> LONSDALEITE_BOOTS = Registers.registerItem("lonsdaleite_boots", LonsdaleiteArmorItem::new,
        () -> lonsdaleiteArmor(ArmorType.BOOTS, ModArmorMaterials.LONSDALEITE.createAttributes(ArmorType.BOOTS).withModifierAdded(
            Attributes.MOVEMENT_SPEED, new AttributeModifier(Identifier.fromNamespaceAndPath(XincraftMod.MODID, "lonsdaleite_boots_speed"),
                0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.FEET)));

    public static final RegistryObject<Item> TIN_HELMET = Registers.registerItem("tin_helmet", Item::new,
        () -> new Item.Properties().humanoidArmor(ModArmorMaterials.TIN, ArmorType.HELMET));
    public static final RegistryObject<Item> TIN_CHESTPLATE = Registers.registerItem("tin_chestplate", Item::new,
        () -> new Item.Properties().humanoidArmor(ModArmorMaterials.TIN, ArmorType.CHESTPLATE));
    public static final RegistryObject<Item> TIN_LEGGINGS = Registers.registerItem("tin_leggings", Item::new,
        () -> new Item.Properties().humanoidArmor(ModArmorMaterials.TIN, ArmorType.LEGGINGS));
    public static final RegistryObject<Item> TIN_BOOTS = Registers.registerItem("tin_boots", Item::new,
        () -> new Item.Properties().humanoidArmor(ModArmorMaterials.TIN, ArmorType.BOOTS));

    /**
     * @brief Applies the reinforced armor trim, and is found in mineshaft chests.
     *
     * @see ReinforcedTrimLoot
     */
    public static final RegistryObject<Item> REINFORCED_ARMOR_TRIM_SMITHING_TEMPLATE = Registers.registerItem(
        "reinforced_armor_trim_smithing_template", SmithingTemplateItem::createArmorTrimTemplate,
        () -> new Item.Properties().rarity(Rarity.UNCOMMON));

    /** @brief Loading this class is what registers everything above. */
    static void init() {}

    private MaterialItems() {}
}
