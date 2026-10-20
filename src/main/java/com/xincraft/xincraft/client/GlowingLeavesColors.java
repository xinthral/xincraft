package com.xincraft.xincraft.client;

import java.util.List;
import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.block.GlowingLeavesBlock;
import com.xincraft.xincraft.registry.ModBlocks;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Tints glowing leaves the same way as the leaves they came from (biome foliage colour for oak, a
// fixed green for spruce and birch, none for cherry and azalea)
@Mod.EventBusSubscriber(modid = XincraftMod.MODID, value = Dist.CLIENT)
public final class GlowingLeavesColors {
    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        for (var glowing : ModBlocks.GLOWING_LEAVES.values()) {
            GlowingLeavesBlock block = (GlowingLeavesBlock) glowing.get();
            List<BlockTintSource> tints = event.getBlockColors().getTintSources(block.getSource().defaultBlockState());
            if (!tints.isEmpty()) {
                event.register(tints, block);
            }
        }
    }

    private GlowingLeavesColors() {}
}
