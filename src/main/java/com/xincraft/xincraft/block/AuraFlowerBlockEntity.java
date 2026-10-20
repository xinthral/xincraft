package com.xincraft.xincraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

// An aura flower's effect (see AuraFlowerBlock): every couple of seconds it gives its effect to
// players within its range (a 16-block cube by default), and to mobs that aren't hostile if the
// flower includes them, the way a beacon refreshes its effect. Several flowers don't add up; they
// just keep it topped up.
public class AuraFlowerBlockEntity extends BlockEntity {
    private static final int INTERVAL_TICKS = 40;

    public AuraFlowerBlockEntity(BlockPos pos, BlockState state) {
        super(((AuraFlowerBlock) state.getBlock()).getBlockEntityType(), pos, state);
    }

    void serverTick(ServerLevel level) {
        // Staggered by position, so a field of flowers doesn't all search on the same tick
        if (Math.floorMod(level.getGameTime() + worldPosition.asLong(), INTERVAL_TICKS) != 0
                || !(getBlockState().getBlock() instanceof AuraFlowerBlock flower)) {
            return;
        }
        AABB area = new AABB(worldPosition).inflate(flower.getRange());
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, entity -> entity.isAlive() && !entity.isSpectator()
                && (entity instanceof Player || flower.includesMobs() && !(entity instanceof Enemy)))) {
            entity.addEffect(new MobEffectInstance(flower.getAura(), flower.getEffectTicks(), 0, true, true));
        }
    }
}
