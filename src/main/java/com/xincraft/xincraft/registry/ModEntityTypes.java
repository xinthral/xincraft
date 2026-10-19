package com.xincraft.xincraft.registry;

import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.entity.EnderMinecart;
import com.xincraft.xincraft.entity.JingleCart;
import com.xincraft.xincraft.entity.LocomotiveCart;
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

    private ModEntityTypes() {}
}
