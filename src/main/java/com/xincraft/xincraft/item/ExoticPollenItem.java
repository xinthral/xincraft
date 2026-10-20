package com.xincraft.xincraft.item;

import java.util.Map;
import java.util.function.Supplier;
import com.xincraft.xincraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

// Made on a mixing tray (block.MixingTrayBlockEntity). Used on certain vanilla flowers, it turns them
// into the mod's exotic flowers; used on leaves, it makes them glow faintly (block.GlowingLeavesBlock).
// On anything else it does nothing.
public class ExoticPollenItem extends Item {
    // Which flower turns into which. Add new exotic flowers here.
    private static final Map<Block, Supplier<Block>> CONVERSIONS = Map.of(
        Blocks.LILY_OF_THE_VALLEY, ModBlocks.MENDING_LILAC,
        Blocks.DANDELION, ModBlocks.FIREFLY_PETUNIA,
        Blocks.GOLDEN_DANDELION, ModBlocks.MOONLIT_DANDELION);

    public ExoticPollenItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Supplier<Block> exotic = CONVERSIONS.get(state.getBlock());
        if (exotic == null) {
            exotic = ModBlocks.GLOWING_LEAVES.get(state.getBlock());
        }
        if (exotic == null) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel) {
            // Keeps the leaves' decay distance, persistence and waterlogging
            serverLevel.setBlock(pos, exotic.get().withPropertiesOf(state), Block.UPDATE_ALL);
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                12, 0.3, 0.3, 0.3, 0.0);
            serverLevel.playSound(null, pos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            context.getItemInHand().consume(1, context.getPlayer());
        }
        return InteractionResult.SUCCESS;
    }
}
