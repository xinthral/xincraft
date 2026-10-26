package com.xincraft.xincraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.client.compendium.CompendiumData;
import com.xincraft.xincraft.client.compendium.CompendiumScreen;
import com.xincraft.xincraft.registry.ModItems;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

// The compendium look-up key (G by default). With a compendium anywhere in the inventory, it opens the
// book at the entry for the item under the mouse in any inventory screen, or for the block or entity
// under the crosshair.
@Mod.EventBusSubscriber(modid = XincraftMod.MODID, value = Dist.CLIENT)
public final class CompendiumKeys {
    public static final KeyMapping LOOK_UP = new KeyMapping("key.xincraft.compendium_look_up",
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, KeyMapping.Category.MISC);

    private CompendiumKeys() {}

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(LOOK_UP);
    }

    // In the world: the block or entity under the crosshair
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (LOOK_UP.consumeClick()) {
            LocalPlayer player = minecraft.player;
            if (player == null || minecraft.level == null || minecraft.gui.screen() != null || !hasCompendium(player)) {
                continue;
            }
            CompendiumData.Entry entry = null;
            if (minecraft.hitResult instanceof BlockHitResult hit && minecraft.hitResult.getType() == BlockHitResult.Type.BLOCK) {
                BlockState state = minecraft.level.getBlockState(hit.getBlockPos());
                entry = CompendiumData.entryFor(state.getBlock());
            } else if (minecraft.hitResult instanceof EntityHitResult hit) {
                ItemStack pick = hit.getEntity().getPickResult();
                entry = pick == null ? null : CompendiumData.entryFor(pick.getItem());
            }
            open(player, entry);
        }
    }

    // In an inventory: the item under the mouse
    @SubscribeEvent
    public static boolean onScreenKey(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || !LOOK_UP.matches(event.getInfo())
                || screen.getFocused() instanceof EditBox) {
            return false;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        Slot slot = screen.getSlotUnderMouse();
        if (player == null || slot == null || !slot.hasItem() || !hasCompendium(player)) {
            return false;
        }
        open(player, CompendiumData.entryFor(slot.getItem().getItem()));
        return true;
    }

    private static boolean hasCompendium(LocalPlayer player) {
        return player.getInventory().contains(stack -> stack.is(ModItems.COMPENDIUM.get()));
    }

    private static void open(LocalPlayer player, CompendiumData.@Nullable Entry entry) {
        if (entry == null) {
            player.sendOverlayMessage(Component.translatable("compendium.xincraft.no_entry"));
        } else {
            CompendiumScreen.openEntry(entry.id());
        }
    }
}
