package com.xincraft.xincraft.registry;

import java.util.function.Function;
import java.util.function.Supplier;
import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.item.EnderRailItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MinecartItem;
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
        registerItem("tin_ingot", Item::new, Item.Properties::new);

    public static final RegistryObject<Item> TIN_HELMET = registerItem("tin_helmet", Item::new,
        () -> new Item.Properties().humanoidArmor(ModArmorMaterials.TIN, ArmorType.HELMET));
    public static final RegistryObject<Item> TIN_CHESTPLATE = registerItem("tin_chestplate", Item::new,
        () -> new Item.Properties().humanoidArmor(ModArmorMaterials.TIN, ArmorType.CHESTPLATE));
    public static final RegistryObject<Item> TIN_LEGGINGS = registerItem("tin_leggings", Item::new,
        () -> new Item.Properties().humanoidArmor(ModArmorMaterials.TIN, ArmorType.LEGGINGS));
    public static final RegistryObject<Item> TIN_BOOTS = registerItem("tin_boots", Item::new,
        () -> new Item.Properties().humanoidArmor(ModArmorMaterials.TIN, ArmorType.BOOTS));

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

    private ModItems() {}
}
