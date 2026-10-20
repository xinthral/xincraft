package com.xincraft.xincraft.registry;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.block.HoneyRailBlock;
import com.xincraft.xincraft.block.BouncyRailBlock;
import com.xincraft.xincraft.block.ChargingRailBlock;
import com.xincraft.xincraft.block.InspectorRailBlock;
import com.xincraft.xincraft.block.EnderRailBlock;
import com.xincraft.xincraft.block.ModPoweredRailBlock;
import com.xincraft.xincraft.block.TurbineBlock;
import com.xincraft.xincraft.block.GlowingLeavesBlock;
import com.xincraft.xincraft.block.MixingTrayBlock;
import com.xincraft.xincraft.block.MendingLilacBlock;
import com.xincraft.xincraft.block.FireflyPetuniaBlock;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
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

    // Generators that burn fuel into Forge Energy for charging rails (block.TurbineBlockEntity): the
    // tin heat turbine, and the bronze steam turbine, which also uses water
    public static final RegistryObject<Block> HEAT_TURBINE = registerBlock(
        "heat_turbine",
        TurbineBlock::heat,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(3.5F)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .lightLevel(state -> state.getValue(TurbineBlock.LIT) ? 13 : 0),
        true
    );

    public static final RegistryObject<Block> STEAM_TURBINE = registerBlock(
        "steam_turbine",
        TurbineBlock::steam,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE)
            .strength(3.5F)
            .sound(SoundType.COPPER)
            .requiresCorrectToolForDrops()
            .lightLevel(state -> state.getValue(TurbineBlock.LIT) ? 13 : 0),
        true
    );

    // Exotic flowers, grown from vanilla flowers with exotic pollen (item.ExoticPollenItem), made on
    // the mixing tray. Same properties as vanilla's small flowers.
    public static final RegistryObject<Block> MIXING_TRAY = registerBlock(
        "mixing_tray",
        MixingTrayBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(0.5F)
            .sound(SoundType.WOOD),
        true
    );

    public static final RegistryObject<Block> MENDING_LILAC = registerBlock(
        "mending_lilac",
        MendingLilacBlock::new,
        flowerProperties(),
        true
    );

    public static final RegistryObject<Block> FIREFLY_PETUNIA = registerBlock(
        "firefly_petunia",
        FireflyPetuniaBlock::new,
        flowerProperties().lightLevel(state -> 10),
        true
    );

    // Glowing copies of each kind of vanilla leaves, made by using exotic pollen on them
    // (block.GlowingLeavesBlock), keyed by the leaves they came from. No items: they drop and
    // pick-block as the original leaves.
    public static final Map<Block, RegistryObject<Block>> GLOWING_LEAVES = registerGlowingLeaves(
            Blocks.OAK_LEAVES,
            Blocks.SPRUCE_LEAVES,
            Blocks.BIRCH_LEAVES,
            Blocks.JUNGLE_LEAVES,
            Blocks.ACACIA_LEAVES,
            Blocks.CHERRY_LEAVES,
            Blocks.DARK_OAK_LEAVES,
            Blocks.PALE_OAK_LEAVES,
            Blocks.MANGROVE_LEAVES,
            Blocks.AZALEA_LEAVES,
            Blocks.FLOWERING_AZALEA_LEAVES);

    private static Map<Block, RegistryObject<Block>> registerGlowingLeaves(Block... sources) {
        Map<Block, RegistryObject<Block>> blocks = new LinkedHashMap<>();
        for (Block source : sources) {
            String name = "glowing_" + BuiltInRegistries.BLOCK.getKey(source).getPath();
            blocks.put(source, registerBlock(name, properties -> new GlowingLeavesBlock(source, properties),
                GlowingLeavesBlock.propertiesOf(source), false));
        }
        return Collections.unmodifiableMap(blocks);
    }

    private static BlockBehaviour.Properties flowerProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .pushReaction(PushReaction.DESTROY);
    }

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
