package com.xincraft.xincraft.item;

import java.util.function.Consumer;
import com.xincraft.xincraft.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

// Worn on the head (or just held), they show the links between buoys (block.BuoyBlock), traced in
// yellow along a path that makes a complete loop and red along one that doesn't
// (client.BuoyHighlight). They also read the essence in an elemental flora, or in all the flora
// around an apothecary altar (client.EssenceReadout). Worn, they tint the view rose
// (client.RoseTintedGlassesTint). Used on a
// motorboat with the RC upgrade on a buoy path with nobody steering, they start or stop it (entity.Motorboat).
public class RoseTintedGlassesItem extends Item {
    public RoseTintedGlassesItem(Item.Properties properties) {
        super(properties);
    }

    // Whether someone is wearing the glasses
    public static boolean isWearing(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ROSE_TINTED_GLASSES.get());
    }

    // Whether someone sees what the glasses show: wearing them, or holding them in either hand
    public static boolean isUsing(LivingEntity entity) {
        Item glasses = ModItems.ROSE_TINTED_GLASSES.get();
        return isWearing(entity) || entity.getMainHandItem().is(glasses) || entity.getOffhandItem().is(glasses);
    }

    // What they do, from item.xincraft.rose_tinted_glasses.desc
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
