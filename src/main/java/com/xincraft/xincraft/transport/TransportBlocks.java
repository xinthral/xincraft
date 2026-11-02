package com.xincraft.xincraft.transport;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import com.xincraft.xincraft.block.AdvancedBuoyBlock;
import com.xincraft.xincraft.block.BuoyBlock;
import com.xincraft.xincraft.block.HoneyRailBlock;
import com.xincraft.xincraft.block.BouncyRailBlock;
import com.xincraft.xincraft.block.ChargingRailBlock;
import com.xincraft.xincraft.block.InspectorRailBlock;
import com.xincraft.xincraft.block.EnderRailBlock;
import com.xincraft.xincraft.block.ModPoweredRailBlock;
import com.xincraft.xincraft.common.Registers;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.RegistryObject;

public final class TransportBlocks {
    // One padded rail per wool colour (e.g. red_padded_rail): plain rails whose minecart sounds
    // are muffled client-side (see client.PaddedRailSounds). Every one is in TransportTags.PADDED_RAILS.
    public static final Map<DyeColor, RegistryObject<Block>> PADDED_RAILS =
        registerColored("padded_rail", RailBlock::new);

    // Powered rails with the same muffling, one per wool colour (e.g. red_padded_powered_rail)
    public static final Map<DyeColor, RegistryObject<Block>> PADDED_POWERED_RAILS =
        registerColored("padded_powered_rail", ModPoweredRailBlock::new);

    // Tin rails let minecarts travel faster than on other rails on straight track (slopes included),
    // slowing to the normal cap for corners (see entity.XincraftMinecartBehavior). Both are in
    // TransportTags.TIN_RAILS.
    public static final RegistryObject<Block> TIN_RAIL = Registers.registerBlock(
        "tin_rail",
        RailBlock::new,
        BlockBehaviour.Properties.of()
            .noCollision()
            .strength(0.7F)
            .sound(SoundType.METAL),
        true
    );

    public static final RegistryObject<Block> TIN_POWERED_RAIL = Registers.registerBlock(
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
    public static final RegistryObject<Block> CHARGING_RAIL = Registers.registerBlock(
        "charging_rail",
        ChargingRailBlock::new,
        BlockBehaviour.Properties.of()
            .noCollision()
            .strength(0.7F)
            .sound(SoundType.COPPER),
        true
    );

    // Holds minecarts at a reduced speed (half by default, Config.honeyRailSpeedMultiplier), slowing
    // fast carts and pushing slow ones (see entity.XincraftMinecartBehavior). In TransportTags.HONEY_RAILS.
    public static final RegistryObject<Block> HONEY_RAIL = Registers.registerBlock(
        "honey_rail",
        HoneyRailBlock::new,
        BlockBehaviour.Properties.of()
            .noCollision()
            .strength(0.7F)
            .sound(SoundType.HONEY_BLOCK),
        true
    );

    // Launches minecarts upward, higher the stronger its redstone signal, up to
    // Config.bouncyRailMaxHeight (see entity.XincraftMinecartBehavior). In TransportTags.BOUNCY_RAILS.
    public static final RegistryObject<Block> BOUNCY_RAIL = Registers.registerBlock(
        "bouncy_rail",
        BouncyRailBlock::new,
        BlockBehaviour.Properties.of()
            .noCollision()
            .strength(0.7F)
            .sound(SoundType.SLIME_BLOCK),
        true
    );

    // Inspector rail: gives a redstone signal whose strength depends on the type of minecart on it
    public static final RegistryObject<Block> INSPECTOR_RAIL = Registers.registerBlock(
        "inspector_rail",
        InspectorRailBlock::new,
        BlockBehaviour.Properties.of()
            .noCollision()
            .strength(0.7F)
            .sound(SoundType.METAL),
        true
    );

    // Teleports minecarts to the ender rail it's linked to (item registered in TransportItems)
    public static final RegistryObject<Block> ENDER_RAIL = Registers.registerBlock(
        "ender_rail",
        EnderRailBlock::new,
        BlockBehaviour.Properties.of()
            .noCollision()
            .strength(0.7F)
            .sound(SoundType.METAL)
            .lightLevel(state -> state.getValue(EnderRailBlock.GLOWING) ? 12 : state.getValue(EnderRailBlock.LINKED) ? 3 : 0),
        false
    );

    // Two-block marker buoy in still water, linked into paths for motorboats (block.BuoyBlock; item
    // in TransportItems). The lamp on top glows while it's switched on.
    public static final RegistryObject<Block> BUOY = Registers.registerBlock(
        "buoy",
        BuoyBlock::new,
        BlockBehaviour.Properties.of()
            .noCollision()
            .noOcclusion()
            .strength(0.5F)
            .sound(SoundType.LANTERN)
            .lightLevel(state -> state.getValue(BuoyBlock.HALF) == DoubleBlockHalf.UPPER && state.getValue(BuoyBlock.LIT) ? 7 : 0)
            .pushReaction(PushReaction.DESTROY),
        false
    );

    // A buoy with a longer reach and a brighter lamp, linked into long legs (block.AdvancedBuoyBlock)
    public static final RegistryObject<Block> ADVANCED_BUOY = Registers.registerBlock(
        "advanced_buoy",
        AdvancedBuoyBlock::new,
        BlockBehaviour.Properties.of()
            .noCollision()
            .noOcclusion()
            .strength(0.5F)
            .sound(SoundType.LANTERN)
            .lightLevel(state -> state.getValue(BuoyBlock.HALF) == DoubleBlockHalf.UPPER && state.getValue(BuoyBlock.LIT) ? 10 : 0)
            .pushReaction(PushReaction.DESTROY),
        false
    );

    private static Map<DyeColor, RegistryObject<Block>> registerColored(
            String name, Function<BlockBehaviour.Properties, Block> factory) {
        Map<DyeColor, RegistryObject<Block>> blocks = new EnumMap<>(DyeColor.class);
        for (DyeColor color : DyeColor.values()) {
            blocks.put(color, Registers.registerBlock(
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

    // Loading this class is what registers everything above
    static void init() {}

    private TransportBlocks() {}
}
