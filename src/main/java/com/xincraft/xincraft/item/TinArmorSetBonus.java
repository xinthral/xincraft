package com.xincraft.xincraft.item;

import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Wearing the full set of tin armor gives Fire Resistance for 5 seconds, renewed every 5
// seconds for as long as all four pieces stay on
@Mod.EventBusSubscriber(modid = XincraftMod.MODID)
public final class TinArmorSetBonus {
    private static final int DURATION_TICKS = 5 * 20;

    // Runs at the start of the player's tick so a renewal lands before the old effect would
    // expire, leaving no tick without fire resistance
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Pre event) {
        if (!(event.player() instanceof ServerPlayer player) || !isWearingFullSet(player)) {
            return;
        }
        // Leaves longer fire resistance alone, e.g. from a potion
        MobEffectInstance current = player.getEffect(MobEffects.FIRE_RESISTANCE);
        if (current == null || current.endsWithin(1)) {
            // Ambient like a beacon effect: icon shown, no swirling particles
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, DURATION_TICKS, 0, true, false, true));
        }
    }

    private static boolean isWearingFullSet(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.TIN_HELMET.get())
            && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.TIN_CHESTPLATE.get())
            && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.TIN_LEGGINGS.get())
            && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.TIN_BOOTS.get());
    }

    private TinArmorSetBonus() {}
}
