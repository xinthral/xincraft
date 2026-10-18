package com.xincraft.xincraft.item;

import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.registry.ModDataComponents;
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

// The tin spyglass is a vanilla spyglass carrying the xincraft:tin_lens component rather than an
// item of its own: vanilla's zoom, scope overlay, hand hiding and slowed mouse all check for
// Items.SPYGLASS specifically (Player#isScoping), and not all of them can be hooked. Looking
// through one gives night vision (below) and tints the view green (client.TinSpyglassTint); its
// look comes from the component select in assets/minecraft/items/spyglass.json.
@Mod.EventBusSubscriber(modid = XincraftMod.MODID)
public final class TinSpyglass {
    // Night vision flickers in its last 10 seconds (GameRenderer#nightVisionScale), so the effect
    // is kept above that by renewing it early, and removed as soon as the player stops looking
    private static final int DURATION_TICKS = 15 * 20;
    private static final int RENEW_TICKS = 11 * 20;

    // A tin spyglass as crafted (see data/xincraft/recipe/tin_spyglass.json, which must match)
    public static ItemStack create() {
        ItemStack stack = new ItemStack(Items.SPYGLASS);
        stack.set(ModDataComponents.TIN_LENS.get(), Unit.INSTANCE);
        stack.set(DataComponents.ITEM_NAME, Component.translatable("item.xincraft.tin_spyglass"));
        return stack;
    }

    public static boolean isTinSpyglass(ItemStack stack) {
        return stack.is(Items.SPYGLASS) && stack.has(ModDataComponents.TIN_LENS.get());
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
        if (isLookingThrough(player)) {
            // Leaves other night vision alone, e.g. from a potion
            if (current == null || ours && current.endsWithin(RENEW_TICKS)) {
                // No particles and no icon: it's the lens, not a status effect
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, DURATION_TICKS, 0, false, false, false));
            }
        } else if (ours) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    // Recognised by its hidden particles and icon, which potions and beacons don't use, so no
    // state needs saving and any left over (e.g. after a crash) is cleared on the next tick
    private static boolean isFromSpyglass(MobEffectInstance effect) {
        return !effect.isAmbient() && !effect.isVisible() && !effect.showIcon() && effect.getDuration() <= DURATION_TICKS;
    }

    private TinSpyglass() {}
}
