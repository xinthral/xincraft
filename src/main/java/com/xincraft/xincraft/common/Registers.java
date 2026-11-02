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

// The registers every feature adds its content to. Each feature keeps what it registers in its own
// holder classes (e.g. transport.TransportItems), loaded by its *Content.init() from XincraftMod.
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

    // Generic item factory: builds an item with the given properties and registers it,
    // handling the required Item.Properties#setId call so callers can't forget it.
    // Properties are supplied lazily because some builders (e.g. humanoidArmor -> repairable)
    // look up registries, which is only allowed once registration actually runs.
    public static <T extends Item> RegistryObject<T> registerItem(
            String name, Function<Item.Properties, T> factory, Supplier<Item.Properties> properties) {
        return ITEMS.register(name, () -> factory.apply(properties.get().setId(ITEMS.key(name))));
    }

    // Generic block factory: builds a block with the given properties, registers it, and
    // (when withItem is true) registers a matching BlockItem under the same name.
    public static RegistryObject<Block> registerBlock(
            String name, Function<BlockBehaviour.Properties, Block> factory,
            BlockBehaviour.Properties properties, boolean withItem) {
        RegistryObject<Block> block = BLOCKS.register(name, () -> factory.apply(properties.setId(BLOCKS.key(name))));
        if (withItem) {
            registerBlockItem(name, block);
        }
        return block;
    }

    // Registers the BlockItem for a block under the same name. Like vanilla's block items, it takes
    // its name from the block's translation key (block.xincraft.<name>) rather than
    // item.xincraft.<name>.
    private static RegistryObject<Item> registerBlockItem(String name, RegistryObject<Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(),
            new Item.Properties().useBlockDescriptionPrefix().setId(ITEMS.key(name))));
    }

    private Registers() {}
}
