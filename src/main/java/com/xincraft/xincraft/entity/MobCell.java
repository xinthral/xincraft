package com.xincraft.xincraft.entity;

import com.xincraft.xincraft.XincraftMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// The cell upgrade's job: a mob riding a carriage with a cell has its AI switched off, so it can't
// attack, shoot, swell or wander, and hostile mobs can be carried safely. Right-clicking the
// carriage lets it out, and it gets its AI back RELEASE_TICKS after it's out of the cell, however it
// got out (released, an activator rail, the carriage broken or the cell snipped off), giving time
// to step away. Mobs that already had no AI (e.g. summoned that way) are left alone.
@Mod.EventBusSubscriber(modid = XincraftMod.MODID)
public final class MobCell {
    public static final int RELEASE_TICKS = 30;
    // Marks a mob whose AI the cell switched off; scoreboard tags are cheap to check and saved with the mob
    private static final String CAGED_TAG = XincraftMod.MODID + ".caged";
    // Ticks left until a released mob wakes, kept in its Forge data so it survives a reload
    private static final String RELEASE_KEY = XincraftMod.MODID + ":CellRelease";

    private MobCell() {}

    public static boolean holds(Entity vehicle) {
        return vehicle instanceof UpgradableCart cart && cart.upgrades().has(CartUpgrade.CELL);
    }

    // Run by a carriage with a cell each server tick
    public static void tick(AbstractMinecart carriage) {
        for (Entity passenger : carriage.getPassengers()) {
            if (passenger instanceof Mob mob && !mob.isNoAi()) {
                mob.setNoAi(true);
                mob.addTag(CAGED_TAG);
                mob.setTarget(null);
                if (mob instanceof Creeper creeper) {
                    creeper.setSwellDir(-1);
                }
                carriage.playSound(SoundEvents.IRON_DOOR_CLOSE, 1.0F, 1.0F);
            }
        }
    }

    // Lets out a mob riding in the cell; true if there was one
    public static boolean release(AbstractMinecart carriage) {
        if (!(carriage.getFirstPassenger() instanceof Mob)) {
            return false;
        }
        carriage.ejectPassengers();
        carriage.playSound(SoundEvents.IRON_DOOR_OPEN, 1.0F, 1.0F);
        return true;
    }

    // Counts down a caged mob that's out of its cell, and gives it its AI back when time's up
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide() || !mob.entityTags().contains(CAGED_TAG)) {
            return;
        }
        CompoundTag data = mob.getPersistentData();
        if (mob.getVehicle() != null && holds(mob.getVehicle())) {
            data.remove(RELEASE_KEY);
            return;
        }
        int left = data.getIntOr(RELEASE_KEY, RELEASE_TICKS) - 1;
        if (left > 0) {
            data.putInt(RELEASE_KEY, left);
            return;
        }
        data.remove(RELEASE_KEY);
        mob.removeTag(CAGED_TAG);
        mob.setNoAi(false);
    }
}
