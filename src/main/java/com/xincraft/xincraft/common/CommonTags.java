package com.xincraft.xincraft.common;

import java.util.List;
import com.xincraft.xincraft.XincraftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class CommonTags {
    // xincraft:mob_family/<name> - mobs a fuzzy mob filter treats as kin (see common.MobFilterContents):
    // an entry for any one of a family lets the rest through too
    public static final List<TagKey<EntityType<?>>> MOB_FAMILIES = List.of(
            "zombies", "skeletons", "horses", "llamas", "camels", "cows", "piglins", "hoglins", "nautiluses",
            "spiders", "slimes", "squids", "guardians", "ghasts", "fish", "illagers", "golems", "frogs", "cats")
        .stream()
        .map(name -> TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(XincraftMod.MODID, "mob_family/" + name)))
        .toList();

    private CommonTags() {}
}
