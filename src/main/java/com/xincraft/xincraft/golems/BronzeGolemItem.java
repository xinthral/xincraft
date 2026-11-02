package com.xincraft.xincraft.golems;

import java.util.function.Consumer;
import com.xincraft.xincraft.common.CommonComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.ItemEnchantments;

// A packed-up bronze golem, or combat golem with its sword in the xincraft:golem_sword component;
// a golem shift + right-clicked with an empty hand (golems.BronzeGolem) becomes one, and using it on
// a block puts the golem back. The altar arms one (magic.AltarRecipe).
public class BronzeGolemItem extends Item {
    public BronzeGolemItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        BronzeGolem golem = GolemEntityTypes.BRONZE_GOLEM.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (golem == null) {
            return InteractionResult.FAIL;
        }
        golem.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        ItemStack sword = stack.get(CommonComponents.GOLEM_SWORD.get());
        if (sword != null) {
            golem.arm(sword.copy());
        }
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            golem.setCustomName(stack.get(DataComponents.CUSTOM_NAME));
        }
        level.addFreshEntity(golem);
        stack.consume(1, context.getPlayer());
        return InteractionResult.SUCCESS_SERVER;
    }

    // What it does, and for a combat golem, the state of its sword
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        ItemStack sword = stack.get(CommonComponents.GOLEM_SWORD.get());
        if (sword != null) {
            tooltip.accept(Component.translatable("item.xincraft.combat_golem.sword", sword.getMaxDamage() - sword.getDamageValue(), sword.getMaxDamage())
                .withStyle(ChatFormatting.GRAY));
            sword.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).addToTooltip(context, tooltip, flag, sword);
        }
    }
}
