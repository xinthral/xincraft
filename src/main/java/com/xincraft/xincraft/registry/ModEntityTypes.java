package com.xincraft.xincraft.registry;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.entity.Carriage;
import com.xincraft.xincraft.entity.EnderMinecart;
import com.xincraft.xincraft.entity.JingleCart;
import com.xincraft.xincraft.entity.LocomotiveCart;
import com.xincraft.xincraft.entity.Motorboat;
import com.xincraft.xincraft.entity.MotorboatType;
import com.xincraft.xincraft.entity.PartyBoat;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, XincraftMod.MODID);

    // Same dimensions and tracking as the vanilla minecart
    public static final RegistryObject<EntityType<EnderMinecart>> ENDER_MINECART = ENTITY_TYPES.register("ender_minecart",
        () -> EntityType.Builder.<EnderMinecart>of(EnderMinecart::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.98F, 0.7F)
            .passengerAttachments(0.1875F)
            .clientTrackingRange(8)
            .build(ENTITY_TYPES.key("ender_minecart")));

    public static final RegistryObject<EntityType<LocomotiveCart>> LOCOMOTIVE_CART = ENTITY_TYPES.register("locomotive_cart",
        () -> EntityType.Builder.<LocomotiveCart>of(LocomotiveCart::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.98F, 0.7F)
            .passengerAttachments(0.1875F)
            .clientTrackingRange(8)
            .build(ENTITY_TYPES.key("locomotive_cart")));

    public static final RegistryObject<EntityType<JingleCart>> JINGLE_CART = ENTITY_TYPES.register("jingle_cart",
        () -> EntityType.Builder.<JingleCart>of(JingleCart::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.98F, 0.7F)
            .passengerAttachments(0.1875F)
            .clientTrackingRange(8)
            .build(ENTITY_TYPES.key("jingle_cart")));

    public static final RegistryObject<EntityType<Carriage>> CARRIAGE = ENTITY_TYPES.register("carriage",
        () -> EntityType.Builder.<Carriage>of(Carriage::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.98F, 0.7F)
            .passengerAttachments(0.1875F)
            .clientTrackingRange(8)
            .build(ENTITY_TYPES.key("carriage")));

    // A motorboat for each kind of boat (entity.MotorboatType), sized and tracked like vanilla boats
    public static final Map<MotorboatType, RegistryObject<EntityType<Motorboat>>> MOTORBOATS = registerMotorboats();

    private static Map<MotorboatType, RegistryObject<EntityType<Motorboat>>> registerMotorboats() {
        Map<MotorboatType, RegistryObject<EntityType<Motorboat>>> types = new EnumMap<>(MotorboatType.class);
        for (MotorboatType kind : MotorboatType.values()) {
            types.put(kind, ENTITY_TYPES.register(kind.id(),
                () -> EntityType.Builder.<Motorboat>of(
                        (type, level) -> new Motorboat(type, level, () -> ModItems.MOTORBOATS.get(kind).get(), kind.raft()), MobCategory.MISC)
                    .noLootTable()
                    .sized(1.375F, 0.5625F)
                    .eyeHeight(0.5625F)
                    .clientTrackingRange(10)
                    .build(ENTITY_TYPES.key(kind.id()))));
        }
        return Collections.unmodifiableMap(types);
    }

    // A motorboat with a jukebox, built on the oak boat (entity.PartyBoat)
    public static final RegistryObject<EntityType<PartyBoat>> PARTY_BOAT = ENTITY_TYPES.register("party_boat",
        () -> EntityType.Builder.<PartyBoat>of(
                (type, level) -> new PartyBoat(type, level, () -> ModItems.PARTY_BOAT.get()), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
            .build(ENTITY_TYPES.key("party_boat")));

    private ModEntityTypes() {}
}
