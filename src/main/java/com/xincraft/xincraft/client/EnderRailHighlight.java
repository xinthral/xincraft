package com.xincraft.xincraft.client;

import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.registry.ModBlocks;
import com.xincraft.xincraft.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// While the player holds an ender rail and looks at a placed one, that rail lights up with
// purple portal sparkles to show it can be right-clicked to attune the held rail to it
@Mod.EventBusSubscriber(modid = XincraftMod.MODID, value = Dist.CLIENT)
public final class EnderRailHighlight {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null || minecraft.isPaused()
                || !(minecraft.hitResult instanceof BlockHitResult hit)
                || !(player.getMainHandItem().is(ModItems.ENDER_RAIL.get()) || player.getOffhandItem().is(ModItems.ENDER_RAIL.get()))) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        if (!level.getBlockState(pos).is(ModBlocks.ENDER_RAIL.get())) {
            return;
        }
        RandomSource random = level.getRandom();
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.REVERSE_PORTAL,
                pos.getX() + random.nextDouble(), pos.getY() + 0.1, pos.getZ() + random.nextDouble(),
                0.0, 0.03, 0.0);
        }
    }

    private EnderRailHighlight() {}
}
