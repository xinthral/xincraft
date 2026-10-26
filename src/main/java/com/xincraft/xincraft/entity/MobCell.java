package com.xincraft.xincraft.entity;

import java.util.UUID;
import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.item.MobFilterItem;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jspecify.annotations.Nullable;

// The cell upgrade's job: a mob riding a carriage or ferry with a cell has its AI switched off, so it
// can't attack, shoot, swell or wander, and hostile mobs can be carried safely. The bars shade it,
// so undead don't burn in sunlight, and hold it, so an enderman can't teleport out. Right-clicking
// the vehicle lets it out, and it gets its AI back RELEASE_TICKS after it's out of the cell, however
// it got out (released, an activator rail, the vehicle broken or the cell snipped off), giving time
// to step away. Mobs that already had no AI (e.g. summoned that way) are left alone.
//
// With the reinforced cage fitted over the cell, it holds any mob at all but the ender dragon (a boss
// built of several parts, which caging would break): it hauls aboard anything it touches, even mobs
// that won't ride (withers, wardens, iron golems), so long as it's empty. Whatever's caged, any
// damage it would do is stopped, as slimes and magma cubes hurt by touch even without AI.
//
// A lead used on the vehicle, or on the mob itself, is tied ready for when it comes out: leashing
// would pull it out of its seat, so the lead is kept until then, and the mob comes out on it, held by
// whoever tied it. Only mobs that can be leashed at all (not hostile ones) take a lead.
@Mod.EventBusSubscriber(modid = XincraftMod.MODID)
public final class MobCell {
    public static final int RELEASE_TICKS = 30;
    // Marks a mob whose AI the cell switched off; scoreboard tags are cheap to check and saved with the mob
    private static final String CAGED_TAG = XincraftMod.MODID + ".caged";
    // Ticks left until a released mob wakes, and who tied a lead on it, kept in its Forge data so they
    // survive a reload
    private static final String RELEASE_KEY = XincraftMod.MODID + ":CellRelease";
    private static final String LEAD_KEY = XincraftMod.MODID + ":CellLead";

    private MobCell() {}

    public static boolean holds(@Nullable Entity vehicle) {
        return vehicle instanceof UpgradableCart cart && cart.upgrades().has(CartUpgrade.CELL);
    }

    public static boolean isReinforced(@Nullable Entity vehicle) {
        return holds(vehicle) && ((UpgradableCart) vehicle).upgrades().has(CartUpgrade.REINFORCED_CAGE);
    }

    // Whether a carriage or ferry takes a passenger: hostile mobs only into a cell, the ender dragon
    // never, and with a mob filter fitted, only the mobs on it (players and other riders aren't
    // filtered)
    public static boolean canCarry(VehicleEntity vehicle, Entity passenger) {
        if (passenger instanceof EnderDragon) {
            return false;
        }
        if (passenger instanceof Mob mob && vehicle instanceof UpgradableCart cart) {
            ItemStack filter = cart.upgrades().filter();
            if (filter != null && !MobFilterItem.allows(filter, mob)) {
                return false;
            }
        }
        return !(passenger instanceof Enemy) || holds(vehicle);
    }

    // Whether the mob is sitting in a cell, on either side
    public static boolean isCaged(Entity entity) {
        return entity instanceof Mob && holds(entity.getVehicle());
    }

    // Run by a carriage or ferry with a cell each server tick
    public static void tick(VehicleEntity carriage) {
        if (isReinforced(carriage) && !carriage.isVehicle()) {
            haulAboard(carriage);
        }
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

    // The reinforced cage takes in whatever mob it touches, forcing aboard any that won't ride. It
    // passes over mobs just let out (still waking), on a lead, or carrying riders of their own.
    private static void haulAboard(VehicleEntity carriage) {
        for (Mob mob : carriage.level().getEntitiesOfClass(Mob.class, carriage.getBoundingBox().inflate(0.2))) {
            if (mob.isAlive() && !mob.isPassenger() && !mob.isVehicle() && !mob.isLeashed()
                    && !mob.entityTags().contains(CAGED_TAG) && canCarry(carriage, mob)
                    && mob.startRiding(carriage, true, true)) {
                return;
            }
        }
    }

    // Right-clicking a vehicle with a cell: with a lead, ties it ready on the mob inside; otherwise
    // lets the mob out. PASS if there's no mob in the cell.
    public static InteractionResult interact(VehicleEntity carriage, Player player, InteractionHand hand) {
        if (!(carriage.getFirstPassenger() instanceof Mob mob)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.LEAD)) {
            return tieLead(player, mob, stack);
        }
        if (!carriage.level().isClientSide()) {
            carriage.ejectPassengers();
            carriage.playSound(SoundEvents.IRON_DOOR_OPEN, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    // Keeps a lead ready on a caged mob, for when it comes out
    private static InteractionResult tieLead(Player player, Mob mob, ItemStack stack) {
        if (mob.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        CompoundTag data = mob.getPersistentData();
        if (!mob.canBeLeashed() || mob.isLeashed()) {
            player.sendOverlayMessage(Component.translatable("message.xincraft.cell.cannot_lead"));
            return InteractionResult.FAIL;
        }
        if (data.contains(LEAD_KEY)) {
            player.sendOverlayMessage(Component.translatable("message.xincraft.cell.lead_ready"));
            return InteractionResult.FAIL;
        }
        data.store(LEAD_KEY, UUIDUtil.CODEC, player.getUUID());
        stack.consume(1, player);
        mob.playSound(SoundEvents.LEAD_TIED);
        player.sendOverlayMessage(Component.translatable("message.xincraft.cell.lead_tied"));
        return InteractionResult.SUCCESS;
    }

    // Puts a mob that's just come out of its cell on the lead tied ready for it, held by whoever tied
    // it; if they've gone, or are too far away to hold it, the lead drops instead
    private static void attachLead(ServerLevel level, Mob mob, CompoundTag data) {
        UUID holderId = data.read(LEAD_KEY, UUIDUtil.CODEC).orElse(null);
        data.remove(LEAD_KEY);
        if (holderId == null) {
            return;
        }
        Player holder = level.getPlayerByUUID(holderId);
        if (holder != null && mob.canHaveALeashAttachedTo(holder)) {
            mob.setLeashedTo(holder, true);
        } else {
            dropLead(level, mob);
        }
    }

    private static void dropLead(ServerLevel level, Mob mob) {
        if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
            mob.spawnAtLocation(level, new ItemStack(Items.LEAD));
        }
    }

    // In its cell, a caged mob is shaded from the sun. Out of it, it's put on any lead tied ready, then
    // counted down and given its AI back when time's up.
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)
                || !mob.entityTags().contains(CAGED_TAG)) {
            return;
        }
        CompoundTag data = mob.getPersistentData();
        if (holds(mob.getVehicle())) {
            data.remove(RELEASE_KEY);
            // Undead are set alight at the end of their tick, so putting it out at the start of the next
            // one means it never burns or shows; fire from lava still burns
            if (mob.is(EntityTypeTags.BURN_IN_DAYLIGHT) && mob.getRemainingFireTicks() > 0 && !mob.isInLava()) {
                mob.clearFire();
            }
            return;
        }
        if (data.contains(LEAD_KEY)) {
            attachLead(level, mob, data);
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

    // Nothing in a cell does any harm, whether it hits, shoots, or (like a slime) just touches
    @SubscribeEvent
    public static boolean onAttack(LivingAttackEvent event) {
        Entity attacker = event.getSource().getEntity();
        Entity direct = event.getSource().getDirectEntity();
        return attacker != null && isCaged(attacker) || direct != null && isCaged(direct);
    }

    // The bars hold an enderman in, however it's hurt
    @SubscribeEvent
    public static boolean onEnderTeleport(EntityTeleportEvent.EnderEntity event) {
        return isCaged(event.getEntity());
    }

    // A lead used on a caged mob itself is tied ready rather than pulling it out of its seat
    @SubscribeEvent
    public static boolean onInteractEntity(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!isCaged(event.getTarget()) || !event.getItemStack().is(Items.LEAD)) {
            return false;
        }
        event.setCancellationResult(tieLead(event.getEntity(), (Mob) event.getTarget(), event.getItemStack()));
        return true;
    }

    // A caged mob that dies with a lead tied ready drops it
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Mob mob && mob.level() instanceof ServerLevel level
                && mob.getPersistentData().contains(LEAD_KEY)) {
            mob.getPersistentData().remove(LEAD_KEY);
            dropLead(level, mob);
        }
    }
}
