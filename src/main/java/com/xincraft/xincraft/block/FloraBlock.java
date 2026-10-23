package com.xincraft.xincraft.block;

import com.mojang.serialization.MapCodec;
import com.xincraft.xincraft.magic.Element;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

// An elemental flora: a flower changed by an element's powder (item.ElementPowderItem) into a living
// store of that element's essence (FloraBlockEntity), for rituals to draw from. More of the same
// powder tops it up. It has no item and drops nothing: broken, it's gone, along with its essence.
// An empty hand shows how much it holds.
public class FloraBlock extends FlowerBlock implements EntityBlock {
    // How full it is, in quarters (0 to 4), which sets how much it glows and sparkles
    public static final int MAX_FILL = 4;
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, MAX_FILL);

    private final Element element;
    private final MapCodec<FloraBlock> codec;

    public FloraBlock(Element element, BlockBehaviour.Properties properties) {
        super(MobEffects.SATURATION, 0.35F, properties);
        this.element = element;
        this.codec = simpleCodec(props -> new FloraBlock(element, props));
        registerDefaultState(stateDefinition.any().setValue(FILL, 0));
    }

    public Element getElement() {
        return element;
    }

    // Light level for a fill level, used in the block properties (registry.ModBlocks)
    public static int lightLevel(BlockState state) {
        return state.getValue(FILL) * 2;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILL);
    }

    @Override
    public MapCodec<FloraBlock> codec() {
        return codec;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FloraBlockEntity(pos, state);
    }

    // An empty hand shows how much essence it holds, e.g. "Flora Ignis: 32/64 essence"
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof FloraBlockEntity flora)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            player.sendOverlayMessage(Component.translatable("message.xincraft.flora_essence",
                getName(), flora.getEssence(), flora.getCapacity()));
        }
        return InteractionResult.SUCCESS;
    }

    // Sparkles in its element's colour, more the fuller it is
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int fill = state.getValue(FILL);
        if (fill > 0 && random.nextInt(MAX_FILL + 2) < fill + 1) {
            level.addParticle(element.particle(),
                pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.4 + random.nextDouble() * 0.5,
                pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.0, 0.0);
        }
    }
}
