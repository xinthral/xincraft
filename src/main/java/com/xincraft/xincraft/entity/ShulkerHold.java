package com.xincraft.xincraft.entity;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;

// What the freight cart and the barge share with the shulker box they carry: 27 slots, the box's
// sounds, no shulker boxes (or anything else that can't go inside a container item) inside, and
// breaking as a single item holding everything, which places back down full.
public final class ShulkerHold {
    public static final int SLOT_COUNT = 27;
    public static final int ROW_SIZE = 9;

    private ShulkerHold() {}

    // Breaks the vehicle into its item, holding the contents rather than spilling them
    public static <T extends VehicleEntity & ContainerEntity> void destroy(T vehicle, ServerLevel level, Item dropItem) {
        ItemStack drop = new ItemStack(dropItem);
        drop.set(DataComponents.CUSTOM_NAME, vehicle.getCustomName());
        drop.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(vehicle.getItemStacks().subList(0, SLOT_COUNT)));
        // Emptied first, so removing the vehicle doesn't spill them as well
        vehicle.clearContent();
        vehicle.kill(level);
        if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
            vehicle.spawnAtLocation(level, drop);
        }
    }

    // Placed from an item that's holding contents, the vehicle starts with them
    public static void unpack(DataComponentGetter components, NonNullList<ItemStack> into) {
        ItemContainerContents contents = components.get(DataComponents.CONTAINER);
        if (contents != null) {
            contents.copyInto(into);
        }
    }

    public static boolean canHold(ItemStack stack) {
        return stack.getItem().canFitInsideContainerItems();
    }

    public static void opened(VehicleEntity vehicle, ContainerUser user) {
        playSound(vehicle, user, true);
    }

    public static void closed(VehicleEntity vehicle, ContainerUser user) {
        vehicle.level().gameEvent(GameEvent.CONTAINER_CLOSE, vehicle.position(), GameEvent.Context.of(user.getLivingEntity()));
        playSound(vehicle, user, false);
    }

    private static void playSound(VehicleEntity vehicle, ContainerUser user, boolean open) {
        if (!user.getLivingEntity().isSpectator()) {
            vehicle.level().playSound(null, vehicle.getX(), vehicle.getY(), vehicle.getZ(),
                open ? SoundEvents.SHULKER_BOX_OPEN : SoundEvents.SHULKER_BOX_CLOSE, SoundSource.BLOCKS,
                0.5F, vehicle.getRandom().nextFloat() * 0.1F + 0.9F);
        }
    }
}
