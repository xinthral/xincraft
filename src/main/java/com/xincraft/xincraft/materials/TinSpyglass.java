package com.xincraft.xincraft.materials;

import com.xincraft.xincraft.XincraftMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * @brief A vanilla spyglass carrying a component, rather than an item of its own, which gives night
 *        vision.
 *
 * Vanilla's zoom and scope overlay check for the spyglass item specifically, and not all of those
 * checks can be hooked. Looking through one also tints the view green. The lonsdaleite helmet gives
 * the same night vision and is handled here too, as taking off one mustn't remove what the other
 * gives.
 *
 * @see materials.client.TinSpyglassTint
 */
@Mod.EventBusSubscriber(modid = XincraftMod.MODID)
public final class TinSpyglass {
    /**
     * @brief How long each dose of night vision lasts, in ticks.
     *
     * Night vision flickers in its last 10 seconds, so it's renewed early to stay above that, and
     * removed as soon as the player stops looking.
     */
    private static final int DURATION_TICKS = 15 * 20;
    /** @brief Time left, in ticks, at which the effect is renewed. */
    private static final int RENEW_TICKS = 11 * 20;

    /**
     * @brief Makes a tin spyglass, as it comes from crafting.
     *
     * This must match the tin spyglass recipe.
     *
     * @return The new stack.
     */
    public static ItemStack create() {
        ItemStack stack = new ItemStack(Items.SPYGLASS);
        stack.set(MaterialComponents.TIN_LENS.get(), Unit.INSTANCE);
        stack.set(DataComponents.ITEM_NAME, Component.translatable("item.xincraft.tin_spyglass"));
        return stack;
    }

    public static boolean isTinSpyglass(ItemStack stack) {
        return stack.is(Items.SPYGLASS) && stack.has(MaterialComponents.TIN_LENS.get());
    }

    public static boolean isLookingThrough(Player player) {
        return player.isScoping() && isTinSpyglass(player.getUseItem());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Pre event) {
        if (!(event.player() instanceof ServerPlayer player)) {
            return;
        }
        MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
        boolean ours = current != null && isFromSpyglass(current);
        if (isLookingThrough(player) || LonsdaleiteArmorItem.isWearingHelmet(player)) {
            // Leaves other night vision alone, e.g. from a potion
            if (current == null || ours && current.endsWithin(RENEW_TICKS)) {
                // No particles and no icon: it's the lens or helmet, not a status effect
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, DURATION_TICKS, 0, false, false, false));
            }
        } else if (ours) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    /**
     * @brief Whether an effect came from the spyglass.
     *
     * It's recognised by its hidden particles and icon, which potions and beacons don't use, so no
     * state needs saving and any left over (e.g. after a crash) is cleared on the next tick.
     *
     * @param effect The effect to check.
     * @return True if it came from the spyglass.
     */
    private static boolean isFromSpyglass(MobEffectInstance effect) {
        return !effect.isAmbient() && !effect.isVisible() && !effect.showIcon() && effect.getDuration() <= DURATION_TICKS;
    }

    private TinSpyglass() {}
}
