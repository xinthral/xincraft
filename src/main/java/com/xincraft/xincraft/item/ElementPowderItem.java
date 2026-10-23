package com.xincraft.xincraft.item;

import java.util.function.Consumer;
import com.xincraft.xincraft.Config;
import com.xincraft.xincraft.block.AuraFlowerBlock;
import com.xincraft.xincraft.block.FireflyPetuniaBlock;
import com.xincraft.xincraft.block.FloraBlock;
import com.xincraft.xincraft.block.FloraBlockEntity;
import com.xincraft.xincraft.magic.Element;
import com.xincraft.xincraft.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

// An element's powder, ground from flowers and vines with the mortar and pestle. Used on a small
// flower, it turns it into that element's flora (block.FloraBlock), holding Config.floraInitialEssence;
// used on a flora of the same element, it adds Config.floraEssencePerPowder. The mod's exotic flowers
// are left alone.
public class ElementPowderItem extends Item {
    private final Element element;

    public ElementPowderItem(Element element, Item.Properties properties) {
        super(properties);
        this.element = element;
    }

    public Element element() {
        return element;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof FloraBlock flora) {
            return flora.getElement() == element ? refill(context, level, pos) : InteractionResult.PASS;
        }
        if (!isConvertible(state)) {
            return InteractionResult.PASS;
        }
        BlockState floraState = ModBlocks.FLORA.get(element).get().defaultBlockState();
        if (!floraState.canSurvive(level, pos)) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.setBlock(pos, floraState, Block.UPDATE_ALL);
            if (serverLevel.getBlockEntity(pos) instanceof FloraBlockEntity flora) {
                flora.insert(Config.floraInitialEssence, false);
            }
            effects(serverLevel, pos, 16);
            serverLevel.playSound(null, pos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            serverLevel.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.0F);
            context.getItemInHand().consume(1, context.getPlayer());
        }
        return InteractionResult.SUCCESS;
    }

    private InteractionResult refill(UseOnContext context, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof FloraBlockEntity flora)
                || flora.insert(Config.floraEssencePerPowder, true) <= 0) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel serverLevel) {
            flora.insert(Config.floraEssencePerPowder, false);
            effects(serverLevel, pos, 8);
            serverLevel.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 1.3F);
            context.getItemInHand().consume(1, context.getPlayer());
        }
        return InteractionResult.SUCCESS;
    }

    // Any small flower, vanilla or from other mods, except this mod's own special flowers
    private static boolean isConvertible(BlockState state) {
        Block block = state.getBlock();
        return state.is(BlockTags.SMALL_FLOWERS)
            && !(block instanceof FloraBlock || block instanceof AuraFlowerBlock || block instanceof FireflyPetuniaBlock);
    }

    private void effects(ServerLevel level, BlockPos pos, int count) {
        level.sendParticles(element.particle(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            count, 0.3, 0.3, 0.3, 0.0);
    }

    // What it does, from item.xincraft.element_powder.desc
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.xincraft.element_powder.desc",
            Component.translatable("block.xincraft." + element.floraId())).withStyle(ChatFormatting.GRAY));
    }
}
