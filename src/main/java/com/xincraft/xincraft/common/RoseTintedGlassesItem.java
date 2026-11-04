package com.xincraft.xincraft.common;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * @brief Glasses that show hidden information when worn on the head or held.
 *
 * They trace the links between buoys, in yellow along a complete loop and red along anything else.
 * They also read the essence in flora and label caged mobs. Worn, they tint the view rose, and used
 * on a motorboat with the RC upgrade, they start or stop it on a buoy path.
 *
 * @see transport.boat.BuoyBlock
 * @see transport.boat.client.BuoyHighlight
 * @see magic.client.EssenceReadout
 * @see transport.train.client.CellCage
 * @see common.client.RoseTintedGlassesTint
 * @see transport.boat.Motorboat
 */
public class RoseTintedGlassesItem extends Item {
    public RoseTintedGlassesItem(Item.Properties properties) {
        super(properties);
    }

    /**
     * @brief Whether someone is wearing the glasses.
     *
     * @param entity The entity to check.
     * @return True if Someone is wearing the glasses.
     */
    public static boolean isWearing(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(CommonItems.ROSE_TINTED_GLASSES.get());
    }

    /**
     * @brief Whether someone sees what the glasses show: wearing them, or holding them in either
     *        hand.
     *
     * @param entity The entity to check.
     * @return True if Someone sees what the glasses show.
     */
    public static boolean isUsing(LivingEntity entity) {
        Item glasses = CommonItems.ROSE_TINTED_GLASSES.get();
        return isWearing(entity) || entity.getMainHandItem().is(glasses) || entity.getOffhandItem().is(glasses);
    }

    /** @brief Adds what they do to the tooltip. */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
