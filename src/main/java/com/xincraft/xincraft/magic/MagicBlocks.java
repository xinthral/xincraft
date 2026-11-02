package com.xincraft.xincraft.magic;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import com.xincraft.xincraft.block.ApothecaryAltarBlock;
import com.xincraft.xincraft.block.ApothecaryCrucibleBlock;
import com.xincraft.xincraft.block.FloraBlock;
import com.xincraft.xincraft.common.Registers;
import com.xincraft.xincraft.flowers.FlowerBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.RegistryObject;

public final class MagicBlocks {
    // Elemental flora, one per element (block.FloraBlock), made by using an element's powder on a
    // small flower. No items, and they drop nothing when broken.
    public static final Map<Element, RegistryObject<Block>> FLORA = registerFlora();

    private static Map<Element, RegistryObject<Block>> registerFlora() {
        Map<Element, RegistryObject<Block>> blocks = new EnumMap<>(Element.class);
        for (Element element : Element.values()) {
            blocks.put(element, Registers.registerBlock(element.floraId(), properties -> new FloraBlock(element, properties),
                FlowerBlocks.flowerProperties().noLootTable().lightLevel(FloraBlock::lightLevel), false));
        }
        return Collections.unmodifiableMap(blocks);
    }

    // Burns plants into elemental essence for the flora around it (block.ApothecaryAltarBlockEntity)
    public static final RegistryObject<Block> APOTHECARY_ALTAR = Registers.registerBlock(
        "apothecary_altar",
        ApothecaryAltarBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .strength(2.0F, 6.0F)
            .requiresCorrectToolForDrops()
            .lightLevel(state -> state.getValue(ApothecaryAltarBlock.LIT) ? 10 : 0),
        true
    );

    // Sits on the altar; inscribes mobs onto mob filters (block.ApothecaryCrucibleBlock)
    public static final RegistryObject<Block> APOTHECARY_CRUCIBLE = Registers.registerBlock(
        "apothecary_crucible",
        ApothecaryCrucibleBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(2.0F)
            .requiresCorrectToolForDrops()
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY),
        true
    );

    // Loading this class is what registers everything above
    static void init() {}

    private MagicBlocks() {}
}
