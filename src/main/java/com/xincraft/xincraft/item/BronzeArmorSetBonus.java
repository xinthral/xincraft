package com.xincraft.xincraft.item;

import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.registry.ModItems;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Wearing the full set of bronze armor lets the player walk straight up full-block steps without
// jumping, as horses do: their step height goes from 0.6 blocks to 1.0.
@Mod.EventBusSubscriber(modid = XincraftMod.MODID)
public final class BronzeArmorSetBonus {
    private static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath(XincraftMod.MODID, "bronze_armor_step_height");
    private static final AttributeModifier STEP_UP =
        new AttributeModifier(MODIFIER_ID, 0.4, AttributeModifier.Operation.ADD_VALUE);

    // Runs on both sides: the player's own client moves them, so it needs the step height too, and
    // it sees the same equipment as the server. Transient, so nothing is saved and taking a piece off
    // (or a reload) can't leave it behind.
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Pre event) {
        Player player = event.player();
        AttributeInstance stepHeight = player.getAttribute(Attributes.STEP_HEIGHT);
        if (stepHeight == null) {
            return;
        }
        boolean fullSet = isWearingFullSet(player);
        if (fullSet && !stepHeight.hasModifier(MODIFIER_ID)) {
            stepHeight.addTransientModifier(STEP_UP);
        } else if (!fullSet && stepHeight.hasModifier(MODIFIER_ID)) {
            stepHeight.removeModifier(MODIFIER_ID);
        }
    }

    private static boolean isWearingFullSet(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.BRONZE_HELMET.get())
            && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.BRONZE_CHESTPLATE.get())
            && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.BRONZE_LEGGINGS.get())
            && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.BRONZE_BOOTS.get());
    }

    private BronzeArmorSetBonus() {}
}
