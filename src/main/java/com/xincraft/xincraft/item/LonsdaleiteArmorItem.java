package com.xincraft.xincraft.item;

import java.util.function.Consumer;
import com.xincraft.xincraft.materials.MaterialItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

// Lonsdaleite armor, made from bronze armor at the apothecary altar (magic.AltarRecipe), with
// netherite's protection and one power per piece. The helmet gives night vision (TinSpyglass runs
// it, as both use the same hidden effect); the chestplate keeps piglins neutral (the item tag
// minecraft:piglin_safe_armor); the leggings' step height and the boots' speed are attribute
// modifiers (materials.MaterialItems). Its red shimmer takes the place of vanilla's glint, which is turned
// off: a model layer on the item, and client.LonsdaleiteGlintLayer when worn.
public class LonsdaleiteArmorItem extends Item {
    public LonsdaleiteArmorItem(Item.Properties properties) {
        super(properties);
    }

    public static boolean isWearingHelmet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(MaterialItems.LONSDALEITE_HELMET.get());
    }

    // What the piece does, from item.xincraft.lonsdaleite_<piece>.desc
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
