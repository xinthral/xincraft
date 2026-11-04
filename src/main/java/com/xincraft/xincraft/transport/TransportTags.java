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
    /**
     * @brief Every colour of padded rail and padded powered rail, which muffle minecart sounds.
     *
     * @see transport.rail.client.PaddedRailSounds
     */
    public static final TagKey<Block> PADDED_RAILS =
        TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "padded_rails"));

    /**
     * @brief Rails on which minecarts can go faster.
     *
     * @see transport.rail.XincraftMinecartBehavior
     */
    public static final TagKey<Block> TIN_RAILS =
        TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "tin_rails"));

    /**
     * @brief Rails that hold minecarts at a reduced, steady speed.
     *
     * @see transport.rail.XincraftMinecartBehavior
     */
    public static final TagKey<Block> HONEY_RAILS =
        TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "honey_rails"));

    /**
     * @brief Rails that launch minecarts upward when powered.
     *
     * @see transport.rail.XincraftMinecartBehavior
     */
    public static final TagKey<Block> BOUNCY_RAILS =
        TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "bouncy_rails"));

    /**
     * @brief Minecart entity types and the redstone signal the inspector rail gives for them.
     *
     * Add a new cart type by listing it in the tag for its signal strength.
     *
     * @see transport.rail.InspectorRailBlock
     */
    private static final List<TagKey<EntityType<?>>> INSPECTOR_RAIL_SIGNALS = IntStream.rangeClosed(0, 15)
        .mapToObj(signal -> TagKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(XincraftMod.MODID, "inspector_rail_signal/" + signal)))
        .toList();

    public static TagKey<EntityType<?>> inspectorRailSignal(int signal) {
        return INSPECTOR_RAIL_SIGNALS.get(signal);
    }

    private TransportTags() {}
}
