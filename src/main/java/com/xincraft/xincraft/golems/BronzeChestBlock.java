package com.xincraft.xincraft.golems;

import java.util.Optional;
import com.mojang.serialization.MapCodec;
import com.xincraft.xincraft.XincraftMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jspecify.annotations.Nullable;

// A copper chest with a fourth row: 36 slots, or 72 when two join into a double chest. It never
// weathers. It's in the block tag minecraft:copper_chests, so copper golems take items from it as
// from any copper chest, and it's the home a bronze golem (golems.BronzeGolem) stays around.
@Mod.EventBusSubscriber(modid = XincraftMod.MODID)
public class BronzeChestBlock extends ChestBlock {
    public static final MapCodec<BronzeChestBlock> CODEC = simpleCodec(BronzeChestBlock::new);
    public static final int DOUBLE_ROWS = BronzeChestBlockEntity.ROWS * 2;

    public BronzeChestBlock(BlockBehaviour.Properties properties) {
        super(GolemBlockEntities.BRONZE_CHEST::get, SoundEvents.COPPER_CHEST_OPEN, SoundEvents.COPPER_CHEST_CLOSE, properties);
    }

    @Override
    public MapCodec<? extends ChestBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BronzeChestBlockEntity(pos, state);
    }

    // Vanilla's double chest menu is six rows; ours is eight
    @Override
    protected @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return combine(state, level, pos, false).apply(new DoubleBlockCombiner.Combiner<ChestBlockEntity, Optional<MenuProvider>>() {
            @Override
            public Optional<MenuProvider> acceptDouble(ChestBlockEntity first, ChestBlockEntity second) {
                Container container = new CompoundContainer(first, second);
                return Optional.of(new MenuProvider() {
                    @Override
                    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                        if (!first.canOpen(player) || !second.canOpen(player)) {
                            return null;
                        }
                        first.unpackLootTable(player);
                        second.unpackLootTable(player);
                        return new ChestMenu(GolemMenus.BRONZE_CHEST_DOUBLE.get(), containerId, inventory, container, DOUBLE_ROWS);
                    }

                    @Override
                    public Component getDisplayName() {
                        if (first.hasCustomName()) {
                            return first.getDisplayName();
                        }
                        return second.hasCustomName() ? second.getDisplayName() : Component.translatable("container.xincraft.bronze_chest_double");
                    }
                });
            }

            @Override
            public Optional<MenuProvider> acceptSingle(ChestBlockEntity single) {
                return Optional.of(single);
            }

            @Override
            public Optional<MenuProvider> acceptNone() {
                return Optional.empty();
            }
        }).orElse(null);
    }

    // Copper chests join anything in the copper chest tag, which this has to be in for copper golems
    // to use it, so a copper chest placed against one would half-join it; this puts it back to single
    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        BlockState placed = event.getState();
        LevelAccessor level = event.getLevel();
        if (level.isClientSide() || !placed.is(BlockTags.COPPER_CHESTS) || placed.is(GolemBlocks.BRONZE_CHEST.get())
                || placed.getValue(TYPE) == ChestType.SINGLE) {
            return;
        }
        if (level.getBlockState(getConnectedBlockPos(event.getPos(), placed)).is(GolemBlocks.BRONZE_CHEST.get())) {
            level.setBlock(event.getPos(), placed.setValue(TYPE, ChestType.SINGLE), 3);
        }
    }
}
