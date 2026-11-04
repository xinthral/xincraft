package com.xincraft.xincraft.materials;

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
 * @brief Lonsdaleite armor, made from bronze armor at the apothecary altar, with netherite's
 *        protection and one power per piece.
 *
 * The helmet gives night vision, the chestplate keeps piglins neutral, and the leggings and boots
 * raise step height and speed. A red shimmer takes the place of vanilla's glint.
 *
 * @see magic.AltarRecipe
 * @see TinSpyglass
 * @see MaterialItems
 * @see materials.client.LonsdaleiteGlintLayer
 */
public class LonsdaleiteArmorItem extends Item {
    public LonsdaleiteArmorItem(Item.Properties properties) {
        super(properties);
    }

    public static boolean isWearingHelmet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(MaterialItems.LONSDALEITE_HELMET.get());
    }

    /** @brief Adds what the piece does to the tooltip. */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
