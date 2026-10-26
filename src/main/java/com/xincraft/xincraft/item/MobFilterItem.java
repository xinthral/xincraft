package com.xincraft.xincraft.item;

import java.util.function.Consumer;
import com.xincraft.xincraft.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

// A mob filter, fitted to a carriage or ferry so it only takes aboard the mobs inscribed on it (see
// entity.CartUpgrades and entity.MobCell). Mobs are inscribed in the apothecary crucible
// (block.ApothecaryCrucibleBlock), up to the filter's capacity; washing it in a water cauldron wipes
// it clean. A strict filter tells babies from adults and a mob from its kin; a fuzzy one doesn't.
public class MobFilterItem extends Item {
    private final int capacity;
    private final boolean fuzzy;

    public MobFilterItem(int capacity, boolean fuzzy, Properties properties) {
        super(properties);
        this.capacity = capacity;
        this.fuzzy = fuzzy;
    }

    public int capacity() {
        return capacity;
    }

    public boolean isFuzzy() {
        return fuzzy;
    }

    public static MobFilterContents contents(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.MOB_FILTER.get(), MobFilterContents.EMPTY);
    }

    public static boolean isFull(ItemStack stack) {
        return stack.getItem() instanceof MobFilterItem filter && contents(stack).size() >= filter.capacity;
    }

    // The entry inscribing this mob would add: a fuzzy filter pays no heed to age, so it always
    // records the adult
    public static MobFilterContents.Entry entryFor(ItemStack stack, EntityType<?> type, boolean baby) {
        return new MobFilterContents.Entry(type, baby && !(stack.getItem() instanceof MobFilterItem filter && filter.fuzzy));
    }

    // Whether a vehicle with this filter fitted takes the mob aboard
    public static boolean allows(ItemStack stack, Mob mob) {
        return stack.getItem() instanceof MobFilterItem filter && contents(stack).matches(mob, filter.fuzzy);
    }

    // Washed in a water cauldron, an inscribed filter is wiped clean, using up a level of water
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        ItemStack stack = context.getItemInHand();
        if (!state.is(Blocks.WATER_CAULDRON) || contents(stack).size() == 0) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            stack.remove(ModDataComponents.MOB_FILTER.get());
            LayeredCauldronBlock.lowerFillLevel(state, level, pos);
            level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.6F, 1.2F);
        }
        return InteractionResult.SUCCESS;
    }

    // What it does, then the mobs inscribed on it and how many more it takes
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        MobFilterContents contents = contents(stack);
        tooltip.accept(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        for (MobFilterContents.Entry entry : contents.entries()) {
            Component name = fuzzy
                ? Component.translatable("item.xincraft.mob_filter.kin", entry.type().getDescription())
                : entry.name();
            tooltip.accept(Component.literal(" ").append(name).withStyle(ChatFormatting.DARK_AQUA));
        }
        tooltip.accept(Component.translatable("item.xincraft.mob_filter.count", contents.size(), capacity)
            .withStyle(ChatFormatting.DARK_GRAY));
    }
}
