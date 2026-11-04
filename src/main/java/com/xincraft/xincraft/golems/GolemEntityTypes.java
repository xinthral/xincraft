package com.xincraft.xincraft.golems;

import com.xincraft.xincraft.common.Registers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.RegistryObject;

public final class GolemEntityTypes {
    /**
     * @brief The size of the copper golem it's made from.
     *
     * @see BronzeGolem
     */
    public static final RegistryObject<EntityType<BronzeGolem>> BRONZE_GOLEM = Registers.ENTITY_TYPES.register("bronze_golem",
        () -> EntityType.Builder.<BronzeGolem>of(BronzeGolem::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.49F, 0.98F)
            .eyeHeight(0.8125F)
            .clientTrackingRange(10)
            .build(Registers.ENTITY_TYPES.key("bronze_golem")));

    /** @brief Loading this class is what registers everything above. */
    static void init() {}

    private GolemEntityTypes() {}
}
