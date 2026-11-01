package com.xincraft.xincraft.item;

import java.util.function.Consumer;
import com.xincraft.xincraft.registry.ModEntityTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

// Used on a copper golem, at any stage of oxidation, turns it into a bronze golem (entity.BronzeGolem).
// What it was carrying or had on its antenna drops.
public class BronzeGolemHelmItem extends Item {
    public BronzeGolemHelmItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof CopperGolem golem)) {
            return InteractionResult.PASS;
        }
        if (golem.level() instanceof ServerLevel level) {
            for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.MAINHAND, CopperGolem.EQUIPMENT_SLOT_ANTENNA}) {
                ItemStack held = golem.getItemBySlot(slot);
                if (!held.isEmpty()) {
                    golem.spawnAtLocation(level, held);
                    golem.setItemSlot(slot, ItemStack.EMPTY);
                }
            }
            golem.convertTo(ModEntityTypes.BRONZE_GOLEM.get(), ConversionParams.single(golem, false, false), converted ->
                converted.playSound(SoundEvents.COPPER_GOLEM_SPAWN));
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
