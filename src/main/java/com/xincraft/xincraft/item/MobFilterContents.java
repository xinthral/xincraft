package com.xincraft.xincraft.item;

import java.util.ArrayList;
import java.util.List;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xincraft.xincraft.registry.ModTags;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

// The mobs inscribed on a mob filter (MobFilterItem), in the order they were inscribed. Each entry is
// a mob and whether it's the baby: a strict filter lets through only exactly that, while a fuzzy one
// lets through the mob at any age and any of its kin (the xincraft:mob_family/* tags).
public record MobFilterContents(List<Entry> entries) {
    public static final MobFilterContents EMPTY = new MobFilterContents(List.of());

    public static final Codec<MobFilterContents> CODEC = Entry.CODEC.listOf()
        .xmap(MobFilterContents::new, MobFilterContents::entries);
    public static final StreamCodec<RegistryFriendlyByteBuf, MobFilterContents> STREAM_CODEC = Entry.STREAM_CODEC
        .apply(ByteBufCodecs.list())
        .map(MobFilterContents::new, MobFilterContents::entries);

    public MobFilterContents {
        entries = List.copyOf(entries);
    }

    public int size() {
        return entries.size();
    }

    public boolean contains(Entry entry) {
        return entries.contains(entry);
    }

    public MobFilterContents with(Entry entry) {
        List<Entry> added = new ArrayList<>(entries);
        added.add(entry);
        return new MobFilterContents(added);
    }

    public boolean matches(Mob mob, boolean fuzzy) {
        return entries.stream().anyMatch(entry -> entry.matches(mob, fuzzy));
    }

    public record Entry(EntityType<?> type, boolean baby) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                EntityType.CODEC.fieldOf("mob").forGetter(Entry::type),
                Codec.BOOL.optionalFieldOf("baby", false).forGetter(Entry::baby))
            .apply(i, Entry::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
            EntityType.STREAM_CODEC, Entry::type,
            ByteBufCodecs.BOOL, Entry::baby,
            Entry::new);

        public boolean matches(Mob mob, boolean fuzzy) {
            if (!fuzzy) {
                return mob.getType() == type && mob.isBaby() == baby;
            }
            return mob.getType() == type || sameFamily(mob.getType(), type);
        }

        // "Chicken" or "Baby Chicken"
        public Component name() {
            return baby ? Component.translatable("item.xincraft.mob_filter.baby", type.getDescription()) : type.getDescription();
        }

        private static boolean sameFamily(EntityType<?> a, EntityType<?> b) {
            for (TagKey<EntityType<?>> family : ModTags.MOB_FAMILIES) {
                if (a.builtInRegistryHolder().is(family) && b.builtInRegistryHolder().is(family)) {
                    return true;
                }
            }
            return false;
        }
    }
}
