package com.xincraft.xincraft.transport.train.client;

import java.util.HashSet;
import java.util.Set;
import com.xincraft.xincraft.XincraftMod;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * @brief Hides mobs sitting in a cell, which are shown by their spawn egg instead.
 *
 * Render states don't say which entity they're for, so a vehicle drawing its cage notes each caged
 * mob by its type and where it's drawn this frame, and the mob drawn at that spot is skipped. The
 * notes are cleared once the frame's drawn.
 *
 * @see transport.train.MobCell
 * @see CellCage
 */
@Mod.EventBusSubscriber(modid = XincraftMod.MODID, value = Dist.CLIENT)
public final class CagedMobs {
    private record Spot(EntityType<?> type, double x, double y, double z) {}

    private static final Set<Spot> HIDDEN = new HashSet<>();

    private CagedMobs() {}

    /**
     * @brief Hides the mob for this frame.
     *
     * @param mob The caged mob.
     * @param partialTicks How far through the current tick this frame is.
     */
    static void hide(Entity mob, float partialTicks) {
        HIDDEN.add(new Spot(mob.getType(), Mth.lerp(partialTicks, mob.xOld, mob.getX()),
            Mth.lerp(partialTicks, mob.yOld, mob.getY()), Mth.lerp(partialTicks, mob.zOld, mob.getZ())));
    }

    @SubscribeEvent
    public static void onFrameDrawn(TickEvent.RenderTickEvent.Post event) {
        HIDDEN.clear();
    }

    @SubscribeEvent
    public static boolean onRenderLiving(RenderLivingEvent.Pre<?, ?, ?> event) {
        var state = event.getState();
        return !HIDDEN.isEmpty() && HIDDEN.contains(new Spot(state.entityType, state.x, state.y, state.z));
    }
}
