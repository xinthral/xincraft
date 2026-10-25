package com.xincraft.xincraft.entity;

import java.util.UUID;
import com.xincraft.xincraft.item.LinkingBarItem;
import com.xincraft.xincraft.registry.ModDataComponents;
import com.xincraft.xincraft.registry.ModItems;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// A vehicle's links to the vehicles ahead of and behind it in a train, made with a linking bar
// (item.LinkingBarItem): use the bar on one and then the other. Minecarts make trains on rails and
// boats make trains on water, by the same rules, but a cart can't be linked to a boat. Each vehicle
// takes one link at each end, so they hitch together in a line. Every train is led by a vehicle that
// can tow (a locomotive, or a motorboat), which tows at most LinkableTransport.maxTowed() vehicles
// behind it, counting every one down the line. When two are linked, the one used first goes in front
// if that makes a train; otherwise the other one does. Shift-using a bar on a vehicle unhitches it
// from the one ahead (or, at the front of a train, from the one behind) and hands the bar back;
// breaking a vehicle, or a link snapping (pulled too far apart, as by an ender rail), drops the bar.
//
// Each link holds its vehicles a bar's length apart, end to end, like a stiff spring with damping:
// each tick a vehicle pulls (or pushes) the one behind it, and is held back by it in turn, along the
// line between them. On rails, vanilla turns that into motion along the track, so each cart follows
// the one ahead around curves and over slopes, pivoting about the bar; a towed boat is also turned
// to face the boat ahead. A hitched boat can't be steered, so nobody aboard fights the tow. Linked
// vehicles don't collide with or bump each other, so they can't jam on a tight corner. Each side
// keeps the other's UUID, which is saved; each vehicle also syncs the entity id of the one behind it
// to clients to draw the bar (client.LinkingBar), and whether it's hitched behind another.
public final class LinkedTransport {
    // Half a cart's and a boat's length, and the gap the bar spans between two of them, end to end
    public static final double CART_HALF_LENGTH = 0.625;
    public static final double BOAT_HALF_LENGTH = 0.875;
    public static final double BAR_GAP = 0.7;
    // How far each end of the bar, where it pivots, is beyond its vehicle's end
    public static final double PIVOT_OVERHANG = 0.0625;
    public static final int NONE = -1;
    // How much further apart than their spacing two vehicles can be hitched from, and how far a
    // link stretches beyond its spacing before it snaps
    private static final double LINK_REACH = 1.55;
    private static final double SNAP_STRETCH = 3.05;
    // Share of the stretch taken up, and of the difference in speed along the bar evened out, each
    // tick; and the most the spring changes either vehicle's speed in a tick
    private static final double STIFFNESS = 0.4;
    private static final double DAMPING = 0.4;
    private static final double MAX_PULL = 0.5;
    // Most degrees a towed boat turns each tick to face the boat ahead
    private static final float BOAT_TURN = 10.0F;
    // A partner that's gone missing (moved to another dimension, or removed without notice) is
    // forgotten after this long; a partner that's just unloaded for a moment comes back sooner
    private static final int MISSING_TICKS = 200;
    // Most vehicles followed along a train, as a guard against a broken save looping it round
    private static final int MAX_TRAIN = 64;

    private final VehicleEntity vehicle;
    // The entity id of the vehicle behind, for drawing the bar, and whether one's ahead
    private final EntityDataAccessor<Integer> towedData;
    private final EntityDataAccessor<Boolean> hitchedData;
    private final Link ahead = new Link();
    private final Link behind = new Link();

    // One end of the vehicle: the partner hitched there, and how long it's been missing
    private static final class Link {
        private @Nullable UUID partner;
        private int missingTicks;

        private void clear() {
            partner = null;
            missingTicks = 0;
        }
    }

    // Why a pair can't be linked one way round, from the least to the most telling
    private enum Refusal {
        NEEDS_LEADER("needs_locomotive", "needs_motorboat"),
        ALREADY_LINKED("already_linked", "already_linked"),
        SAME_TRAIN("same_train", "same_train"),
        TOO_LONG("too_long", "too_long_boats");

        private final String cartMessage;
        private final String boatMessage;

        Refusal(String cartMessage, String boatMessage) {
            this.cartMessage = cartMessage;
            this.boatMessage = boatMessage;
        }
    }

    public LinkedTransport(VehicleEntity vehicle, EntityDataAccessor<Integer> towedData, EntityDataAccessor<Boolean> hitchedData) {
        this.vehicle = vehicle;
        this.towedData = towedData;
        this.hitchedData = hitchedData;
    }

    public boolean isLinked() {
        return ahead.partner != null || behind.partner != null;
    }

    public boolean isLinkedTo(Entity entity) {
        UUID id = entity.getUUID();
        return id.equals(ahead.partner) || id.equals(behind.partner);
    }

    // Client side as well: the entity id of the vehicle this one tows, or NONE
    public int towedId() {
        return vehicle.getEntityData().get(towedData);
    }

    // Client side as well: whether it's hitched behind another vehicle
    public boolean isHitched() {
        return vehicle.getEntityData().get(hitchedData);
    }

    private void setTowedId(int id) {
        vehicle.getEntityData().set(towedData, id);
    }

    private void setHitched(boolean hitched) {
        vehicle.getEntityData().set(hitchedData, hitched);
    }

    // Half a vehicle's length, from its centre to the end the bar hitches to
    public static double halfLength(Entity vehicle) {
        return vehicle instanceof AbstractBoat ? BOAT_HALF_LENGTH : CART_HALF_LENGTH;
    }

    // How far apart, centre to centre, a link holds two vehicles
    public static double spacing(Entity a, Entity b) {
        return halfLength(a) + halfLength(b) + BAR_GAP;
    }

    private static LinkedTransport of(VehicleEntity vehicle) {
        return ((LinkableTransport) vehicle).transport();
    }

    private static boolean sameKind(Entity a, Entity b) {
        return (a instanceof AbstractMinecart) == (b instanceof AbstractMinecart);
    }

    // Run by the vehicle at the start of each server tick, before it moves
    public void tick(ServerLevel level) {
        find(level, ahead, false);
        setHitched(ahead.partner != null);
        VehicleEntity towed = find(level, behind, true);
        if (towed == null) {
            setTowedId(NONE);
            return;
        }
        setTowedId(towed.getId());
        if (vehicle.position().distanceTo(towed.position()) > spacing(vehicle, towed) + SNAP_STRETCH) {
            unlink(behind, towed);
            dropBar(level, vehicle);
            return;
        }
        pull(towed);
        if (towed instanceof AbstractBoat boat) {
            turnToward(boat, vehicle.position());
        }
    }

    // The partner at one end, if it's loaded and still linked back. Forgets one that's been missing
    // too long, or that's no longer linked back (e.g. unhitched while this vehicle was unloaded).
    private @Nullable VehicleEntity find(ServerLevel level, Link link, boolean isBehind) {
        if (link.partner == null) {
            return null;
        }
        Entity found = level.getEntity(link.partner);
        if (found == null) {
            if (++link.missingTicks >= MISSING_TICKS) {
                link.clear();
            }
            return null;
        }
        link.missingTicks = 0;
        if (!(found instanceof VehicleEntity other) || !(found instanceof LinkableTransport linkable) || !found.isAlive()
                || !vehicle.getUUID().equals((isBehind ? linkable.transport().ahead : linkable.transport().behind).partner)) {
            link.clear();
            return null;
        }
        return other;
    }

    // The spring and damper between this vehicle and the one it tows, along the line between them (on
    // the level, so a slope doesn't lift or press down on either)
    private void pull(VehicleEntity towed) {
        Vec3 offset = towed.position().subtract(vehicle.position()).horizontal();
        double distance = offset.length();
        if (distance < 1.0E-4) {
            return;
        }
        Vec3 unit = offset.scale(1.0 / distance);
        double spring = Mth.clamp(STIFFNESS * (distance - spacing(vehicle, towed)), -MAX_PULL, MAX_PULL);
        double closing = motion(towed).subtract(motion(vehicle)).dot(unit);
        Vec3 change = unit.scale(spring + DAMPING * closing);
        vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(change));
        towed.setDeltaMovement(towed.getDeltaMovement().subtract(change));
    }

    // How fast a vehicle's going. A boat a player steers is moved on their client, so the server
    // only knows how far it's come since last tick.
    private static Vec3 motion(VehicleEntity vehicle) {
        if (vehicle instanceof AbstractBoat && !vehicle.isLocalInstanceAuthoritative()) {
            return vehicle.position().subtract(vehicle.xo, vehicle.yo, vehicle.zo);
        }
        return vehicle.getDeltaMovement();
    }

    // Swings a towed boat's bow round toward the boat ahead, a little each tick
    private static void turnToward(AbstractBoat boat, Vec3 leader) {
        Vec3 toLeader = leader.subtract(boat.position()).horizontal();
        if (toLeader.lengthSqr() < 1.0E-4) {
            return;
        }
        // A boat's forward is (-sin yaw, cos yaw)
        float target = (float) (Mth.atan2(-toLeader.x, toLeader.z) * Mth.RAD_TO_DEG);
        float turn = Mth.clamp(Mth.wrapDegrees(target - boat.getYRot()), -BOAT_TURN, BOAT_TURN);
        boat.setYRot(boat.getYRot() + turn);
    }

    // A linking bar: shift-use unhitches; otherwise the first vehicle used is remembered on the bar,
    // and the second is hitched to it. PASS for anything else, so the vehicle carries on with its own
    // interaction.
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(ModItems.LINKING_BAR.get())) {
            return InteractionResult.PASS;
        }
        if (!(vehicle.level() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive()) {
            // Off the vehicle ahead, so it leaves the train with any behind it; at the front of a
            // train, off the one behind
            Link link = ahead.partner != null ? ahead : behind;
            if (link.partner == null) {
                player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.not_linked"));
                return InteractionResult.FAIL;
            }
            if (level.getEntity(link.partner) instanceof VehicleEntity other) {
                unlink(link, other);
            } else {
                link.clear();
                if (link == ahead) {
                    setHitched(false);
                }
            }
            // In creative, linking didn't use up a bar, so there's none to hand back
            if (!player.hasInfiniteMaterials()) {
                ItemStack bar = new ItemStack(ModItems.LINKING_BAR.get());
                if (!player.getInventory().add(bar)) {
                    vehicle.spawnAtLocation(level, bar);
                }
            }
            vehicle.playSound(SoundEvents.CHAIN_HIT, 1.0F, 1.0F);
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.unlinked"));
            return InteractionResult.SUCCESS;
        }
        if (LinkingBarItem.isLinkingBuoys(stack)) {
            LinkingBarItem.clearPending(stack);
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.mixed"));
            return InteractionResult.FAIL;
        }
        UUID first = stack.get(ModDataComponents.LINKING_BAR_START.get());
        if (first == null) {
            if (ahead.partner != null && behind.partner != null) {
                player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.already_linked"));
                return InteractionResult.FAIL;
            }
            stack.set(ModDataComponents.LINKING_BAR_START.get(), vehicle.getUUID());
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            vehicle.playSound(SoundEvents.CHAIN_HIT, 1.0F, 1.2F);
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.started"));
            return InteractionResult.SUCCESS;
        }
        LinkingBarItem.clearPending(stack);
        if (first.equals(vehicle.getUUID())) {
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.cancelled"));
            return InteractionResult.SUCCESS;
        }
        if (!(level.getEntity(first) instanceof VehicleEntity other) || !(other instanceof LinkableTransport) || !other.isAlive()) {
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.missing"));
            return InteractionResult.FAIL;
        }
        if (!sameKind(vehicle, other)) {
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.cart_and_boat"));
            return InteractionResult.FAIL;
        }
        if (other.position().distanceTo(vehicle.position()) > spacing(vehicle, other) + LINK_REACH) {
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.too_far"));
            return InteractionResult.FAIL;
        }
        // The vehicle used first goes in front if it can; otherwise this one does
        Refusal otherLeads = refusal(level, other, vehicle);
        Refusal thisLeads = otherLeads == null ? null : refusal(level, vehicle, other);
        if (otherLeads != null && thisLeads != null) {
            Refusal shown = otherLeads.compareTo(thisLeads) >= 0 ? otherLeads : thisLeads;
            String message = vehicle instanceof AbstractBoat ? shown.boatMessage : shown.cartMessage;
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar." + message,
                maxTowed(level, shown == otherLeads ? other : vehicle)));
            return InteractionResult.FAIL;
        }
        if (otherLeads == null) {
            link(other, vehicle);
        } else {
            link(vehicle, other);
        }
        stack.consume(1, player);
        vehicle.playSound(SoundEvents.CHAIN_PLACE, 1.0F, 1.0F);
        player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.linked"));
        return InteractionResult.SUCCESS;
    }

    // Why the follower can't be hitched behind the leader, or null if it can: the leader's train must
    // be led by a vehicle that tows, with room for the follower and every vehicle behind it
    private static @Nullable Refusal refusal(ServerLevel level, VehicleEntity leader, VehicleEntity follower) {
        if (of(leader).behind.partner != null || of(follower).ahead.partner != null) {
            return Refusal.ALREADY_LINKED;
        }
        VehicleEntity head = leader;
        int ahead = 0;
        VehicleEntity next;
        while (ahead < MAX_TRAIN && (next = next(level, head, false)) != null) {
            head = next;
            ahead++;
        }
        if (head == follower) {
            return Refusal.SAME_TRAIN;
        }
        int maxTowed = ((LinkableTransport) head).maxTowed();
        if (maxTowed <= 0 || of(head).ahead.partner != null) {
            return Refusal.NEEDS_LEADER;
        }
        int added = 1;
        for (VehicleEntity tail = next(level, follower, true); tail != null && added < MAX_TRAIN; tail = next(level, tail, true)) {
            added++;
        }
        return ahead + added > maxTowed ? Refusal.TOO_LONG : null;
    }

    // The most vehicles the one at the head of this one's train can tow, for messages
    private static int maxTowed(ServerLevel level, VehicleEntity vehicle) {
        VehicleEntity head = vehicle;
        VehicleEntity next;
        for (int i = 0; i < MAX_TRAIN && (next = next(level, head, false)) != null; i++) {
            head = next;
        }
        return ((LinkableTransport) head).maxTowed();
    }

    // The loaded vehicle linked ahead of or behind this one, if any
    private static @Nullable VehicleEntity next(ServerLevel level, VehicleEntity vehicle, boolean isBehind) {
        UUID id = (isBehind ? of(vehicle).behind : of(vehicle).ahead).partner;
        return id != null && level.getEntity(id) instanceof VehicleEntity next && next instanceof LinkableTransport ? next : null;
    }

    private static void link(VehicleEntity leader, VehicleEntity follower) {
        LinkedTransport front = of(leader);
        LinkedTransport back = of(follower);
        front.behind.partner = follower.getUUID();
        front.behind.missingTicks = 0;
        front.setTowedId(follower.getId());
        back.ahead.partner = leader.getUUID();
        back.ahead.missingTicks = 0;
        back.setHitched(true);
    }

    // Unhitches this vehicle from the partner at one end, and the partner from it
    private void unlink(Link link, VehicleEntity other) {
        if (other instanceof LinkableTransport linkable) {
            LinkedTransport partner = linkable.transport();
            Link back = link == ahead ? partner.behind : partner.ahead;
            if (vehicle.getUUID().equals(back.partner)) {
                back.clear();
                if (link == ahead) {
                    partner.setTowedId(NONE);
                } else {
                    partner.setHitched(false);
                }
            }
        }
        link.clear();
        if (link == behind) {
            setTowedId(NONE);
        } else {
            setHitched(false);
        }
    }

    private static void dropBar(ServerLevel level, Entity at) {
        if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
            at.spawnAtLocation(level, new ItemStack(ModItems.LINKING_BAR.get()));
        }
    }

    // Run when the vehicle leaves the level. Broken or killed, it unhitches the vehicles either side
    // and drops a bar for each link; just unloaded with its chunk, it keeps its links for when it
    // comes back.
    public void onRemove(Entity.RemovalReason reason) {
        if (!isLinked() || !(vehicle.level() instanceof ServerLevel level)
                || reason == Entity.RemovalReason.UNLOADED_TO_CHUNK || reason == Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
            return;
        }
        for (Link link : new Link[] {ahead, behind}) {
            if (link.partner == null) {
                continue;
            }
            if (level.getEntity(link.partner) instanceof VehicleEntity other) {
                unlink(link, other);
            } else {
                link.clear();
            }
            if (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.CHANGED_DIMENSION) {
                dropBar(level, vehicle);
            }
        }
        setTowedId(NONE);
        setHitched(false);
    }

    public void save(ValueOutput output) {
        if (ahead.partner != null) {
            output.store("CoupledAhead", UUIDUtil.CODEC, ahead.partner);
        }
        if (behind.partner != null) {
            output.store("CoupledBehind", UUIDUtil.CODEC, behind.partner);
        }
    }

    public void load(ValueInput input) {
        ahead.partner = input.read("CoupledAhead", UUIDUtil.CODEC).orElse(null);
        behind.partner = input.read("CoupledBehind", UUIDUtil.CODEC).orElse(null);
        // Saved before carts linked at both ends: one partner, and whether this cart towed it
        input.read("Coupled", UUIDUtil.CODEC).ifPresent(partner -> {
            if (input.getBooleanOr("Towing", false)) {
                behind.partner = partner;
            } else {
                ahead.partner = partner;
            }
        });
        setHitched(ahead.partner != null);
    }
}
