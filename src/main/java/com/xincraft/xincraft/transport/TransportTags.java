package com.xincraft.xincraft.transport;

import java.util.List;
import java.util.stream.IntStream;
import com.xincraft.xincraft.XincraftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

public final class TransportTags {
    // xincraft:padded_rails - every colour of padded rail and padded powered rail; minecart
    // sounds are muffled on these (see client.PaddedRailSounds)
    public static final TagKey<Block> PADDED_RAILS =
        TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "padded_rails"));

    // xincraft:tin_rails - rails on which minecarts can go faster (see entity.XincraftMinecartBehavior)
    public static final TagKey<Block> TIN_RAILS =
        TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "tin_rails"));

    // xincraft:honey_rails - rails that hold minecarts at a reduced, steady speed
    // (see entity.XincraftMinecartBehavior)
    public static final TagKey<Block> HONEY_RAILS =
        TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "honey_rails"));

    // xincraft:bouncy_rails - rails that launch minecarts upward when powered
    // (see entity.XincraftMinecartBehavior)
    public static final TagKey<Block> BOUNCY_RAILS =
        TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "bouncy_rails"));

    // xincraft:inspector_rail_signal/1 to /15 - minecart entity types and the redstone signal the
    // inspector rail gives for them (see block.InspectorRailBlock). Add a new cart type by listing
    // it in the tag for its signal strength.
    private static final List<TagKey<EntityType<?>>> INSPECTOR_RAIL_SIGNALS = IntStream.rangeClosed(0, 15)
        .mapToObj(signal -> TagKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(XincraftMod.MODID, "inspector_rail_signal/" + signal)))
        .toList();

    public static TagKey<EntityType<?>> inspectorRailSignal(int signal) {
        return INSPECTOR_RAIL_SIGNALS.get(signal);
    }

    private TransportTags() {}
}
