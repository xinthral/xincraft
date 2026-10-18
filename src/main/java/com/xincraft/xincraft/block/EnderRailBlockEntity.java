package com.xincraft.xincraft.block;

import com.xincraft.xincraft.registry.ModBlockEntities;
import com.xincraft.xincraft.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

// Remembers which ender rail (in the same dimension) this one is linked to
public class EnderRailBlockEntity extends BlockEntity {
    private @Nullable BlockPos link;

    public EnderRailBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENDER_RAIL.get(), pos, state);
    }

    public @Nullable BlockPos getLink() {
        return link;
    }

    void setLink(@Nullable BlockPos link) {
        this.link = link;
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        link = input.read("link", BlockPos.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable("link", BlockPos.CODEC, link);
    }

    // The placing item's link and glint are handled by EnderRailBlock#setPlacedBy; reading them
    // here marks them as consumed so they aren't copied into this block entity's saved components
    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        components.get(ModDataComponents.ENDER_RAIL_LINK.get());
        components.get(ModDataComponents.GLINT_UNTIL.get());
        components.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
    }

    // Breaking one end of a link leaves the other end unlinked
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (link != null && level instanceof ServerLevel serverLevel
                && serverLevel.getBlockEntity(link) instanceof EnderRailBlockEntity other && pos.equals(other.getLink())) {
            EnderRailBlock.unlink(serverLevel, link);
        }
    }
}
