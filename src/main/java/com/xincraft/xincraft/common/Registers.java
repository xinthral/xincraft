package com.xincraft.xincraft.common;

import java.util.function.Function;
import java.util.function.Supplier;
import com.xincraft.xincraft.XincraftMod;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * @brief The registers every feature adds its content to.
 *
 * Each feature keeps what it registers in its own holder classes (e.g. transport.TransportItems),
 * loaded by its *Content.init() from XincraftMod.
 */
public final class Registers {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, XincraftMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, XincraftMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, XincraftMod.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, XincraftMod.MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, XincraftMod.MODID);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, XincraftMod.MODID);

    /**
     * @brief Builds an item with the given properties and registers it.
     *
     * This handles the required Item.Properties#setId call so callers can't forget it. Properties
     * are supplied lazily because some builders (e.g. humanoidArmor -> repairable) look up
     * registries, which is only allowed once registration actually runs.
     *
     * @tparam T The item's class.
     * @param name The item's registry name.
     * @param factory Builds the item from its properties.
     * @param properties Supplies the item's properties.
     * @return The registered item.
     */
    public static <T extends Item> RegistryObject<T> registerItem(
            String name, Function<Item.Properties, T> factory, Supplier<Item.Properties> properties) {
        return ITEMS.register(name, () -> factory.apply(properties.get().setId(ITEMS.key(name))));
    }

    /**
     * @brief Builds a block with the given properties and registers it.
     *
     * With withItem set, it also registers a matching BlockItem under the same name.
     *
     * @param name The block's registry name.
     * @param factory Builds the block from its properties.
     * @param properties The block's properties.
     * @param withItem Whether to register a matching BlockItem.
     * @return The registered block.
     */
    public static RegistryObject<Block> registerBlock(
            String name, Function<BlockBehaviour.Properties, Block> factory,
            BlockBehaviour.Properties properties, boolean withItem) {
        RegistryObject<Block> block = BLOCKS.register(name, () -> factory.apply(properties.setId(BLOCKS.key(name))));
        if (withItem) {
            registerBlockItem(name, block);
        }
        return block;
    }

    /**
     * @brief Registers the BlockItem for a block, under the same name.
     *
     * Like vanilla's block items, it takes its name from the block rather than having its own item
     * name.
     *
     * @param name The registry name to use.
     * @param block The block to make an item for.
     * @return The registered item.
     */
    private static RegistryObject<Item> registerBlockItem(String name, RegistryObject<Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(),
            new Item.Properties().useBlockDescriptionPrefix().setId(ITEMS.key(name))));
    }

    private Registers() {}
}
