package com.xincraft.xincraft.client;

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

// Hides mobs sitting in a cell (entity.MobCell), which are shown by their spawn egg instead
// (CellCage). Render states don't say which entity they're for, so a vehicle drawing its cage notes
// each caged mob by its type and where it's drawn this frame, worked out exactly as vanilla works it
// out; every entity is set up for drawing before any is drawn, so the note is in before the mob is
// drawn, and the mob drawn at that spot is skipped. The notes are cleared once the frame's drawn:
// entities are set up before the render tick starts (Minecraft#runTick), so clearing them at its
// start would throw this frame's notes away.
@Mod.EventBusSubscriber(modid = XincraftMod.MODID, value = Dist.CLIENT)
public final class CagedMobs {
    private record Spot(EntityType<?> type, double x, double y, double z) {}

    private static final Set<Spot> HIDDEN = new HashSet<>();

    private CagedMobs() {}

    // Hides the mob for this frame, as vanilla places it (EntityRenderer#createRenderState)
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
