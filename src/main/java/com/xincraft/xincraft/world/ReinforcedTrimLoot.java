package com.xincraft.xincraft.world;

import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.registry.ModItems;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Puts the reinforced armor trim template in abandoned mineshaft chests, the way vanilla's templates
// are found in structure chests. Added as an extra pool when the table loads, rather than by
// replacing vanilla's table, so other mods' and datapacks' changes to it are kept.
@Mod.EventBusSubscriber(modid = XincraftMod.MODID)
public final class ReinforcedTrimLoot {
    private static final Identifier MINESHAFT_CHEST = Identifier.withDefaultNamespace("chests/abandoned_mineshaft");
    private static final float CHANCE = 0.1F;

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        LootTable table = event.getTable();
        if (table == null || !event.getName().equals(MINESHAFT_CHEST)) {
            return;
        }
        table.addPool(LootPool.lootPool()
            .name(XincraftMod.MODID + ":reinforced_armor_trim_smithing_template")
            .add(LootItem.lootTableItem(ModItems.REINFORCED_ARMOR_TRIM_SMITHING_TEMPLATE.get()))
            .when(LootItemRandomChanceCondition.randomChance(CHANCE))
            .build());
    }

    private ReinforcedTrimLoot() {}
}
