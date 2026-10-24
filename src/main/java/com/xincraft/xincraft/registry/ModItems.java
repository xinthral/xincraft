package com.xincraft.xincraft.registry;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.entity.CartUpgrade;
import com.xincraft.xincraft.entity.MotorboatType;
import com.xincraft.xincraft.item.BuoyItem;
import com.xincraft.xincraft.item.CartUpgradeItem;
import com.xincraft.xincraft.item.LinkingBarItem;
import com.xincraft.xincraft.item.ElementPowderItem;
import com.xincraft.xincraft.item.MortarAndPestleItem;
import com.xincraft.xincraft.item.RoseTintedGlassesItem;
import com.xincraft.xincraft.magic.Element;
import com.xincraft.xincraft.item.EnderRailItem;
import com.xincraft.xincraft.item.ExoticPollenItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, XincraftMod.MODID);

    // Generic item factory: builds an item with the given properties and registers it,
    // handling the required Item.Properties#setId call so callers can't forget it.
    // Properties are supplied lazily because some builders (e.g. humanoidArmor -> repairable)
    // look up registries, which is only allowed once registration actually runs.
    public static <T extends Item> RegistryObject<T> registerItem(
            String name, Function<Item.Properties, T> factory, Supplier<Item.Properties> properties) {
        return ITEMS.register(name, () -> factory.apply(properties.get().setId(ITEMS.key(name))));
    }

    // Registers the BlockItem for a block under the same name. Called by ModBlocks.registerBlock.
    // Like vanilla's block items, it takes its name from the block's translation key
    // (block.xincraft.<name>) rather than item.xincraft.<name>.
    static RegistryObject<Item> registerBlockItem(String name, RegistryObject<Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(),
            new Item.Properties().useBlockDescriptionPrefix().setId(ITEMS.key(name))));
    }

    public static final RegistryObject<Item> RAW_TIN =
        registerItem("raw_tin", Item::new, Item.Properties::new);

    public static final RegistryObject<Item> TIN_INGOT =
        registerItem("tin_ingot", Item::new, () -> new Item.Properties().trimMaterial(ModTrimMaterials.TIN));

    // Bronze: 3 raw copper and 1 raw tin craft into 4 raw bronze, which smelts into bronze ingots
    public static final RegistryObject<Item> RAW_BRONZE =
        registerItem("raw_bronze", Item::new, Item.Properties::new);

    public static final RegistryObject<Item> BRONZE_INGOT =
        registerItem("bronze_ingot", Item::new, Item.Properties::new);

    public static final RegistryObject<Item> BRONZE_HELMET = registerItem("bronze_helmet", Item::new,
        () -> new Item.Properties().humanoidArmor(ModArmorMaterials.BRONZE, ArmorType.HELMET));
    public static final RegistryObject<Item> BRONZE_CHESTPLATE = registerItem("bronze_chestplate", Item::new,
        () -> new Item.Properties().humanoidArmor(ModArmorMaterials.BRONZE, ArmorType.CHESTPLATE));
    public static final RegistryObject<Item> BRONZE_LEGGINGS = registerItem("bronze_leggings", Item::new,
        () -> new Item.Properties().humanoidArmor(ModArmorMaterials.BRONZE, ArmorType.LEGGINGS));
    public static final RegistryObject<Item> BRONZE_BOOTS = registerItem("bronze_boots", Item::new,
        () -> new Item.Properties().humanoidArmor(ModArmorMaterials.BRONZE, ArmorType.BOOTS));

    public static final RegistryObject<Item> TIN_HELMET = registerItem("tin_helmet", Item::new,
        () -> new Item.Properties().humanoidArmor(ModArmorMaterials.TIN, ArmorType.HELMET));
    public static final RegistryObject<Item> TIN_CHESTPLATE = registerItem("tin_chestplate", Item::new,
        () -> new Item.Properties().humanoidArmor(ModArmorMaterials.TIN, ArmorType.CHESTPLATE));
    public static final RegistryObject<Item> TIN_LEGGINGS = registerItem("tin_leggings", Item::new,
        () -> new Item.Properties().humanoidArmor(ModArmorMaterials.TIN, ArmorType.LEGGINGS));
    public static final RegistryObject<Item> TIN_BOOTS = registerItem("tin_boots", Item::new,
        () -> new Item.Properties().humanoidArmor(ModArmorMaterials.TIN, ArmorType.BOOTS));

    // Applies the reinforced armor trim (data/xincraft/trim_pattern/reinforced.json); found in
    // mineshaft chests (world.ReinforcedTrimLoot)
    public static final RegistryObject<Item> REINFORCED_ARMOR_TRIM_SMITHING_TEMPLATE = registerItem(
        "reinforced_armor_trim_smithing_template", SmithingTemplateItem::createArmorTrimTemplate,
        () -> new Item.Properties().rarity(Rarity.UNCOMMON));

    public static final RegistryObject<Item> ENDER_RAIL = registerItem("ender_rail",
        properties -> new EnderRailItem(ModBlocks.ENDER_RAIL.get(), properties),
        () -> new Item.Properties().useBlockDescriptionPrefix());

    // Crafting ingredient for the mod's special minecarts (ender minecart, locomotive cart)
    public static final RegistryObject<Item> MINECART_FRAME =
        registerItem("minecart_frame", Item::new, Item.Properties::new);

    public static final RegistryObject<Item> ENDER_MINECART = registerItem("ender_minecart",
        properties -> new MinecartItem(ModEntityTypes.ENDER_MINECART.get(), properties),
        () -> new Item.Properties().stacksTo(1));

    public static final RegistryObject<Item> LOCOMOTIVE_CART = registerItem("locomotive_cart",
        properties -> new MinecartItem(ModEntityTypes.LOCOMOTIVE_CART.get(), properties),
        () -> new Item.Properties().stacksTo(1));

    public static final RegistryObject<Item> JINGLE_CART = registerItem("jingle_cart",
        properties -> new MinecartItem(ModEntityTypes.JINGLE_CART.get(), properties),
        () -> new Item.Properties().stacksTo(1));

    public static final RegistryObject<Item> CARRIAGE = registerItem("carriage",
        properties -> new MinecartItem(ModEntityTypes.CARRIAGE.get(), properties),
        () -> new Item.Properties().stacksTo(1));

    // Hitches a cart behind a locomotive (see entity.CartCoupling)
    public static final RegistryObject<Item> LINKING_BAR = registerItem("linking_bar",
        LinkingBarItem::new, Item.Properties::new);

    // Minecart upgrades (see entity.CartUpgrades)
    public static final RegistryObject<Item> SHRIEKER_UPGRADE = registerItem("shrieker_upgrade",
        properties -> new CartUpgradeItem(CartUpgrade.SHRIEKER, properties), Item.Properties::new);
    public static final RegistryObject<Item> SCULK_SENSOR_UPGRADE = registerItem("sculk_sensor_upgrade",
        properties -> new CartUpgradeItem(CartUpgrade.SCULK_SENSOR, properties), Item.Properties::new);
    public static final RegistryObject<Item> ENERGY_UPGRADE = registerItem("energy_upgrade",
        properties -> new CartUpgradeItem(CartUpgrade.ENERGY, properties), Item.Properties::new);
    public static final RegistryObject<Item> ICE_BREAKER_UPGRADE = registerItem("ice_breaker_upgrade",
        properties -> new CartUpgradeItem(CartUpgrade.ICE_BREAKER, properties), Item.Properties::new);
    public static final RegistryObject<Item> SPEED_UPGRADE = registerItem("speed_upgrade",
        properties -> new CartUpgradeItem(CartUpgrade.SPEED, properties), Item.Properties::new);
    public static final RegistryObject<Item> SHIMMER_UPGRADE = registerItem("shimmer_upgrade",
        properties -> new CartUpgradeItem(CartUpgrade.SHIMMER, properties), Item.Properties::new);
    public static final RegistryObject<Item> RC_UPGRADE = registerItem("rc_upgrade",
        properties -> new CartUpgradeItem(CartUpgrade.RC, properties), Item.Properties::new);

    // Crafted with any boat to make a motorboat
    public static final RegistryObject<Item> PROPELLER =
        registerItem("propeller", Item::new, Item.Properties::new);

    // A motorboat item for each kind of boat (entity.MotorboatType), placed like a boat
    public static final Map<MotorboatType, RegistryObject<Item>> MOTORBOATS = registerMotorboats();

    private static Map<MotorboatType, RegistryObject<Item>> registerMotorboats() {
        Map<MotorboatType, RegistryObject<Item>> items = new EnumMap<>(MotorboatType.class);
        for (MotorboatType kind : MotorboatType.values()) {
            items.put(kind, registerItem(kind.id(),
                properties -> new BoatItem(ModEntityTypes.MOTORBOATS.get(kind).get(), properties),
                () -> new Item.Properties().stacksTo(1)));
        }
        return Collections.unmodifiableMap(items);
    }

    // Bronze ingots shaped to line a boat; with any motorboat and a jukebox, makes a party boat
    public static final RegistryObject<Item> BRONZE_HULL_LINING =
        registerItem("bronze_hull_lining", Item::new, Item.Properties::new);

    public static final RegistryObject<Item> PARTY_BOAT = registerItem("party_boat",
        properties -> new BoatItem(ModEntityTypes.PARTY_BOAT.get(), properties),
        () -> new Item.Properties().stacksTo(1));

    // Placed in still water; linked into paths for motorboats with the RC upgrade (block.BuoyBlock)
    public static final RegistryObject<Item> BUOY = registerItem("buoy",
        properties -> new BuoyItem(ModBlocks.BUOY.get(), properties),
        () -> new Item.Properties().useBlockDescriptionPrefix());

    // Worn (or held), shows buoy paths; used on an RC motorboat on a path, starts and stops it
    public static final RegistryObject<Item> ROSE_TINTED_GLASSES = registerItem("rose_tinted_glasses", RoseTintedGlassesItem::new,
        () -> new Item.Properties().stacksTo(1).equippable(EquipmentSlot.HEAD));

    // Mixed from three different flowers on the mixing tray; turns certain flowers into exotic ones
    public static final RegistryObject<Item> EXOTIC_POLLEN = registerItem("exotic_pollen", ExoticPollenItem::new,
        () -> new Item.Properties().rarity(Rarity.UNCOMMON));

    // Grinds flowers and vines into elemental powders in the crafting grid, wearing down instead of
    // being used up
    public static final RegistryObject<Item> MORTAR_AND_PESTLE = registerItem("mortar_and_pestle", MortarAndPestleItem::new,
        () -> new Item.Properties().durability(64));

    // One powder per element (e.g. ignis_powder); turns small flowers into elemental flora
    public static final Map<Element, RegistryObject<Item>> ELEMENT_POWDERS = registerElementPowders();

    private static Map<Element, RegistryObject<Item>> registerElementPowders() {
        Map<Element, RegistryObject<Item>> items = new EnumMap<>(Element.class);
        for (Element element : Element.values()) {
            items.put(element, registerItem(element.powderId(),
                properties -> new ElementPowderItem(element, properties), Item.Properties::new));
        }
        return Collections.unmodifiableMap(items);
    }

    private ModItems() {}
}
