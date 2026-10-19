package com.xincraft.xincraft.registry;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.block.HoneyRailBlock;
import com.xincraft.xincraft.block.BouncyRailBlock;
import com.xincraft.xincraft.block.ChargingRailBlock;
import com.xincraft.xincraft.block.InspectorRailBlock;
import com.xincraft.xincraft.block.EnderRailBlock;
import com.xincraft.xincraft.block.ModPoweredRailBlock;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, XincraftMod.MODID);

    // Generic block factory: builds a block with the given properties, registers it, and
    // (when withItem is true) registers a matching BlockItem under the same name.
    public static RegistryObject<Block> registerBlock(
            String name, Function<BlockBehaviour.Properties, Block> factory,
            BlockBehaviour.Properties properties, boolean withItem) {
        RegistryObject<Block> block = BLOCKS.register(name, () -> factory.apply(properties.setId(BLOCKS.key(name))));
        if (withItem) {
            ModItems.registerBlockItem(name, block);
        }
        return block;
    }

    public static final RegistryObject<Block> TIN_BLOCK = registerBlock(
        "tin_block",
        Block::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(5.0F, 6.0F)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops(),
        true
    );

    // As hard as tin and iron blocks, with copper's sound
    public static final RegistryObject<Block> BRONZE_BLOCK = registerBlock(
        "bronze_block",
        Block::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE)
            .strength(5.0F, 6.0F)
            .sound(SoundType.COPPER)
            .requiresCorrectToolForDrops(),
        true
    );

    // Tin ores generate in the Overworld (data/xincraft/worldgen, data/xincraft/forge/biome_modifier,
    // world.TinOreBiomeModifier) unless Config.generateTinOre is off, and drop raw tin. Properties
    // match vanilla's iron ores.
    public static final RegistryObject<Block> TIN_ORE = registerBlock(
        "tin_ore",
        Block::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(3.0F, 3.0F),
        true
    );

    public static final RegistryObject<Block> DEEPSLATE_TIN_ORE = registerBlock(
        "deepslate_tin_ore",
        Block::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.DEEPSLATE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(4.5F, 3.0F)
            .sound(SoundType.DEEPSLATE),
        true
    );

    // One padded rail per wool colour (e.g. red_padded_rail): plain rails whose minecart sounds
    // are muffled client-side (see client.PaddedRailSounds). Every one is in ModTags.PADDED_RAILS.
    public static final Map<DyeColor, RegistryObject<Block>> PADDED_RAILS =
        registerColored("padded_rail", RailBlock::new);

    // Powered rails with the same muffling, one per wool colour (e.g. red_padded_powered_rail)
    public static final Map<DyeColor, RegistryObject<Block>> PADDED_POWERED_RAILS =
        registerColored("padded_powered_rail", ModPoweredRailBlock::new);

    // Tin rails let minecarts travel faster than on other rails on straight track (slopes included),
    // slowing to the normal cap for corners (see entity.XincraftMinecartBehavior). Both are in
    // ModTags.TIN_RAILS.
    public static final RegistryObject<Block> TIN_RAIL = registerBlock(
        "tin_rail",
        RailBlock::new,
        BlockBehaviour.Properties.of()
            .noCollision()
            .strength(0.7F)
            .sound(SoundType.METAL),
        true
    );

    public static final RegistryObject<Block> TIN_POWERED_RAIL = registerBlock(
        "tin_powered_rail",
        ModPoweredRailBlock::new,
        BlockBehaviour.Properties.of()
            .noCollision()
            .strength(0.7F)
            .sound(SoundType.METAL),
        true
    );

    // A bronze powered rail holding Forge Energy for locomotive carts with the energy upgrade to
    // charge from (block.ChargingRailBlock)
    public static final RegistryObject<Block> CHARGING_RAIL = registerBlock(
        "charging_rail",
        ChargingRailBlock::new,
        BlockBehaviour.Properties.of()
            .noCollision()
            .strength(0.7F)
            .sound(SoundType.COPPER),
        true
    );

    // Holds minecarts at a reduced speed (half by default, Config.honeyRailSpeedMultiplier), slowing
    // fast carts and pushing slow ones (see entity.XincraftMinecartBehavior). In ModTags.HONEY_RAILS.
    public static final RegistryObject<Block> HONEY_RAIL = registerBlock(
        "honey_rail",
        HoneyRailBlock::new,
        BlockBehaviour.Properties.of()
            .noCollision()
            .strength(0.7F)
            .sound(SoundType.HONEY_BLOCK),
        true
    );

    // Launches minecarts upward, higher the stronger its redstone signal, up to
    // Config.bouncyRailMaxHeight (see entity.XincraftMinecartBehavior). In ModTags.BOUNCY_RAILS.
    public static final RegistryObject<Block> BOUNCY_RAIL = registerBlock(
        "bouncy_rail",
        BouncyRailBlock::new,
        BlockBehaviour.Properties.of()
            .noCollision()
            .strength(0.7F)
            .sound(SoundType.SLIME_BLOCK),
        true
    );

    // Inspector rail: gives a redstone signal whose strength depends on the type of minecart on it
    public static final RegistryObject<Block> INSPECTOR_RAIL = registerBlock(
        "inspector_rail",
        InspectorRailBlock::new,
        BlockBehaviour.Properties.of()
            .noCollision()
            .strength(0.7F)
            .sound(SoundType.METAL),
        true
    );

    // Teleports minecarts to the ender rail it's linked to (item registered in ModItems)
    public static final RegistryObject<Block> ENDER_RAIL = registerBlock(
        "ender_rail",
        EnderRailBlock::new,
        BlockBehaviour.Properties.of()
            .noCollision()
            .strength(0.7F)
            .sound(SoundType.METAL)
            .lightLevel(state -> state.getValue(EnderRailBlock.GLOWING) ? 12 : state.getValue(EnderRailBlock.LINKED) ? 3 : 0),
        false
    );

    private static Map<DyeColor, RegistryObject<Block>> registerColored(
            String name, Function<BlockBehaviour.Properties, Block> factory) {
        Map<DyeColor, RegistryObject<Block>> blocks = new EnumMap<>(DyeColor.class);
        for (DyeColor color : DyeColor.values()) {
            blocks.put(color, registerBlock(
                color.getName() + "_" + name,
                factory,
                BlockBehaviour.Properties.of()
                    .noCollision()
                    .strength(0.7F)
                    .sound(SoundType.WOOL),
                true
            ));
        }
        return Collections.unmodifiableMap(blocks);
    }

    private ModBlocks() {}
}
