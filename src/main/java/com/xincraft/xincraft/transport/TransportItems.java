package com.xincraft.xincraft.transport;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import com.xincraft.xincraft.transport.train.CartUpgrade;
import com.xincraft.xincraft.transport.boat.MotorboatType;
import com.xincraft.xincraft.transport.boat.BargeItem;
import com.xincraft.xincraft.transport.boat.BuoyItem;
import com.xincraft.xincraft.transport.train.CartUpgradeItem;
import com.xincraft.xincraft.transport.train.LinkingBarItem;
import com.xincraft.xincraft.transport.cart.FreightCartItem;
import com.xincraft.xincraft.transport.rail.EnderRailItem;
import com.xincraft.xincraft.common.Registers;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MinecartItem;
import net.minecraftforge.registries.RegistryObject;

public final class TransportItems {
    public static final RegistryObject<Item> ENDER_RAIL = Registers.registerItem("ender_rail",
        properties -> new EnderRailItem(TransportBlocks.ENDER_RAIL.get(), properties),
        () -> new Item.Properties().useBlockDescriptionPrefix());

    // Crafting ingredient for the mod's special minecarts (ender minecart, locomotive cart)
    public static final RegistryObject<Item> MINECART_FRAME =
        Registers.registerItem("minecart_frame", Item::new, Item.Properties::new);

    public static final RegistryObject<Item> ENDER_MINECART = Registers.registerItem("ender_minecart",
        properties -> new MinecartItem(TransportEntityTypes.ENDER_MINECART.get(), properties),
        () -> new Item.Properties().stacksTo(1));

    public static final RegistryObject<Item> LOCOMOTIVE_CART = Registers.registerItem("locomotive_cart",
        properties -> new MinecartItem(TransportEntityTypes.LOCOMOTIVE_CART.get(), properties),
        () -> new Item.Properties().stacksTo(1));

    public static final RegistryObject<Item> JINGLE_CART = Registers.registerItem("jingle_cart",
        properties -> new MinecartItem(TransportEntityTypes.JINGLE_CART.get(), properties),
        () -> new Item.Properties().stacksTo(1));

    public static final RegistryObject<Item> CARRIAGE = Registers.registerItem("carriage",
        properties -> new MinecartItem(TransportEntityTypes.CARRIAGE.get(), properties),
        () -> new Item.Properties().stacksTo(1));

    public static final RegistryObject<Item> FREIGHT_CART = Registers.registerItem("freight_cart",
        FreightCartItem::new, () -> new Item.Properties().stacksTo(1));

    // Hitches carts and boats into trains (see transport.train.LinkedTransport)
    public static final RegistryObject<Item> LINKING_BAR = Registers.registerItem("linking_bar",
        LinkingBarItem::new, Item.Properties::new);

    // Minecart upgrades (see transport.train.CartUpgrades)
    public static final RegistryObject<Item> SHRIEKER_UPGRADE = Registers.registerItem("shrieker_upgrade",
        properties -> new CartUpgradeItem(CartUpgrade.SHRIEKER, properties), Item.Properties::new);
    public static final RegistryObject<Item> SCULK_SENSOR_UPGRADE = Registers.registerItem("sculk_sensor_upgrade",
        properties -> new CartUpgradeItem(CartUpgrade.SCULK_SENSOR, properties), Item.Properties::new);
    public static final RegistryObject<Item> ENERGY_UPGRADE = Registers.registerItem("energy_upgrade",
        properties -> new CartUpgradeItem(CartUpgrade.ENERGY, properties), Item.Properties::new);
    public static final RegistryObject<Item> ICE_BREAKER_UPGRADE = Registers.registerItem("ice_breaker_upgrade",
        properties -> new CartUpgradeItem(CartUpgrade.ICE_BREAKER, properties), Item.Properties::new);
    public static final RegistryObject<Item> SPEED_UPGRADE = Registers.registerItem("speed_upgrade",
        properties -> new CartUpgradeItem(CartUpgrade.SPEED, properties), Item.Properties::new);
    public static final RegistryObject<Item> SHIMMER_UPGRADE = Registers.registerItem("shimmer_upgrade",
        properties -> new CartUpgradeItem(CartUpgrade.SHIMMER, properties), Item.Properties::new);
    public static final RegistryObject<Item> RC_UPGRADE = Registers.registerItem("rc_upgrade",
        properties -> new CartUpgradeItem(CartUpgrade.RC, properties), Item.Properties::new);
    public static final RegistryObject<Item> CELL_UPGRADE = Registers.registerItem("cell_upgrade",
        properties -> new CartUpgradeItem(CartUpgrade.CELL, properties), Item.Properties::new);
    public static final RegistryObject<Item> ADVANCED_ANTENNA_UPGRADE = Registers.registerItem("advanced_antenna_upgrade",
        properties -> new CartUpgradeItem(CartUpgrade.ANTENNA, properties), Item.Properties::new);
    public static final RegistryObject<Item> REINFORCED_CAGE_UPGRADE = Registers.registerItem("reinforced_cage_upgrade",
        properties -> new CartUpgradeItem(CartUpgrade.REINFORCED_CAGE, properties), Item.Properties::new);

    // Crafted with any boat to make a motorboat
    public static final RegistryObject<Item> PROPELLER =
        Registers.registerItem("propeller", Item::new, Item.Properties::new);

    // A motorboat item for each kind of boat (transport.boat.MotorboatType), placed like a boat
    public static final Map<MotorboatType, RegistryObject<Item>> MOTORBOATS = registerMotorboats();

    private static Map<MotorboatType, RegistryObject<Item>> registerMotorboats() {
        Map<MotorboatType, RegistryObject<Item>> items = new EnumMap<>(MotorboatType.class);
        for (MotorboatType kind : MotorboatType.values()) {
            items.put(kind, Registers.registerItem(kind.id(),
                properties -> new BoatItem(TransportEntityTypes.MOTORBOATS.get(kind).get(), properties),
                () -> new Item.Properties().stacksTo(1)));
        }
        return Collections.unmodifiableMap(items);
    }

    // Bronze ingots shaped to line a boat; with any motorboat and a jukebox, makes a party boat
    public static final RegistryObject<Item> BRONZE_HULL_LINING =
        Registers.registerItem("bronze_hull_lining", Item::new, Item.Properties::new);

    public static final RegistryObject<Item> PARTY_BOAT = Registers.registerItem("party_boat",
        properties -> new BoatItem(TransportEntityTypes.PARTY_BOAT.get(), properties),
        () -> new Item.Properties().stacksTo(1));

    public static final RegistryObject<Item> FERRY = Registers.registerItem("ferry",
        properties -> new BoatItem(TransportEntityTypes.FERRY.get(), properties),
        () -> new Item.Properties().stacksTo(1));

    public static final RegistryObject<Item> BARGE = Registers.registerItem("barge",
        BargeItem::new, () -> new Item.Properties().stacksTo(1));

    // Placed in still water; linked into paths for motorboats with the RC upgrade (transport.boat.BuoyBlock)
    public static final RegistryObject<Item> BUOY = Registers.registerItem("buoy",
        properties -> new BuoyItem(TransportBlocks.BUOY.get(), properties),
        () -> new Item.Properties().useBlockDescriptionPrefix());
    // A buoy whose links to other advanced buoys reach twice as far (transport.boat.AdvancedBuoyBlock)
    public static final RegistryObject<Item> ADVANCED_BUOY = Registers.registerItem("advanced_buoy",
        properties -> new BuoyItem(TransportBlocks.ADVANCED_BUOY.get(), properties),
        () -> new Item.Properties().useBlockDescriptionPrefix());

    // Loading this class is what registers everything above
    static void init() {}

    private TransportItems() {}
}
