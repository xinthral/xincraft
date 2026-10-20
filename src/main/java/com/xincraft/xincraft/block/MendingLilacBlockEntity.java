package com.xincraft.xincraft.block;

import com.xincraft.xincraft.Config;
import com.xincraft.xincraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

// The mending lilac's effect: every couple of seconds it gives Regeneration I to every living thing
// within Config.mendingLilacRange blocks (a 16-block cube by default) that isn't a hostile mob, the
// way a beacon refreshes its effect. Several lilacs don't add up; they just keep it topped up.
public class MendingLilacBlockEntity extends BlockEntity {
    private static final int INTERVAL_TICKS = 40;
    // Outlasts the interval, so the effect never flickers off while in range
    private static final int EFFECT_TICKS = 100;

    public MendingLilacBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MENDING_LILAC.get(), pos, state);
    }

    void serverTick(ServerLevel level) {
        // Staggered by position, so a field of lilacs doesn't all search on the same tick
        if (Math.floorMod(level.getGameTime() + worldPosition.asLong(), INTERVAL_TICKS) != 0) {
            return;
        }
        AABB area = new AABB(worldPosition).inflate(Config.mendingLilacRange);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.isAlive() && !entity.isSpectator() && !(entity instanceof Enemy))) {
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, EFFECT_TICKS, 0, true, true));
        }
    }
}
