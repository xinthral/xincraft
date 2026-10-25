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
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// A cart's links to the carts ahead of and behind it in a train, made with a linking bar
// (item.LinkingBarItem): use the bar on one cart and then the other. Each cart takes one link at
// each end, so carts hitch together in a line. Every train is led by a locomotive, which tows at
// most LinkableCart.maxTowed() carts behind it, counting every cart down the line. When two carts
// are linked, the one used first goes in front if that makes a train; otherwise the other one does.
// Shift-using a bar on a cart unhitches it from the cart ahead (or, at the front of a train, from the
// cart behind) and hands the bar back; breaking a cart, or a link snapping (pulled too far apart, as
// by an ender rail), drops the bar.
//
// Each link holds its carts' centres SPACING apart, like a stiff spring with damping: each tick a
// cart pulls (or pushes) the cart behind it, and is held back by it in turn, along the line between
// them. On rails, vanilla turns that into motion along the track, so each cart follows the one ahead
// around curves and over slopes, pivoting about the bar. Linked carts don't collide with or bump each
// other, so they can't jam on a tight corner. Each side keeps the other's UUID, which is saved; each
// cart also syncs the entity id of the cart behind it to clients to draw the bar
// (client.RetexturedMinecartRenderer).
public final class CartCoupling {
    // Centre to centre: the cart bodies are 1.25 blocks long, leaving about 0.7 blocks for the bar
    public static final double SPACING = 1.95;
    // How far each end of the bar, where it pivots, is from its cart's centre
    public static final double PIVOT = 0.6875;
    public static final int NONE = -1;
    // Furthest apart (centre to centre) two carts can be hitched from, and how far a link
    // stretches before it snaps
    private static final double LINK_REACH = 3.5;
    private static final double SNAP_DISTANCE = 5.0;
    // Share of the stretch taken up, and of the difference in speed along the bar evened out, each
    // tick; and the most the spring changes either cart's speed in a tick
    private static final double STIFFNESS = 0.4;
    private static final double DAMPING = 0.4;
    private static final double MAX_PULL = 0.5;
    // A partner that's gone missing (moved to another dimension, or removed without notice) is
    // forgotten after this long; a partner that's just unloaded for a moment comes back sooner
    private static final int MISSING_TICKS = 200;
    // Most carts followed along a train, as a guard against a broken save looping it round
    private static final int MAX_TRAIN = 64;

    private final AbstractMinecart cart;
    // The entity id of the cart behind, for drawing the bar
    private final EntityDataAccessor<Integer> towedData;
    private final Link ahead = new Link();
    private final Link behind = new Link();

    // One end of the cart: the partner hitched there, and how long it's been missing
    private static final class Link {
        private @Nullable UUID partner;
        private int missingTicks;

        private void clear() {
            partner = null;
            missingTicks = 0;
        }
    }

    // Why a pair of carts can't be linked one way round, from the least to the most telling
    private enum Refusal {
        NEEDS_LOCOMOTIVE("needs_locomotive"),
        ALREADY_LINKED("already_linked"),
        SAME_TRAIN("same_train"),
        TOO_LONG("too_long");

        private final String message;

        Refusal(String message) {
            this.message = message;
        }
    }

    public CartCoupling(AbstractMinecart cart, EntityDataAccessor<Integer> towedData) {
        this.cart = cart;
        this.towedData = towedData;
    }

    public boolean isLinked() {
        return ahead.partner != null || behind.partner != null;
    }

    public boolean isLinkedTo(Entity entity) {
        UUID id = entity.getUUID();
        return id.equals(ahead.partner) || id.equals(behind.partner);
    }

    // Client side as well: the entity id of the cart this one tows, or NONE
    public int towedId() {
        return cart.getEntityData().get(towedData);
    }

    private void setTowedId(int id) {
        cart.getEntityData().set(towedData, id);
    }

    private static CartCoupling of(AbstractMinecart cart) {
        return ((LinkableCart) cart).coupling();
    }

    // Run by the cart at the start of each server tick, before it moves
    public void tick(ServerLevel level) {
        find(level, ahead, false);
        AbstractMinecart towed = find(level, behind, true);
        if (towed == null) {
            setTowedId(NONE);
            return;
        }
        setTowedId(towed.getId());
        if (cart.position().distanceTo(towed.position()) > SNAP_DISTANCE) {
            unlink(behind, towed);
            dropBar(level, cart);
            return;
        }
        pull(towed);
    }

    // The partner at one end, if it's loaded and still linked back. Forgets one that's been missing
    // too long, or that's no longer linked back (e.g. unhitched while this cart was unloaded).
    private @Nullable AbstractMinecart find(ServerLevel level, Link link, boolean isBehind) {
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
        if (!(found instanceof AbstractMinecart other) || !(found instanceof LinkableCart linkable) || !found.isAlive()
                || !cart.getUUID().equals((isBehind ? linkable.coupling().ahead : linkable.coupling().behind).partner)) {
            link.clear();
            return null;
        }
        return other;
    }

    // The spring and damper between this cart and the one it tows, along the line between them (on
    // the level, so a slope doesn't lift or press down on either cart)
    private void pull(AbstractMinecart towed) {
        Vec3 offset = towed.position().subtract(cart.position()).horizontal();
        double distance = offset.length();
        if (distance < 1.0E-4) {
            return;
        }
        Vec3 unit = offset.scale(1.0 / distance);
        double spring = Mth.clamp(STIFFNESS * (distance - SPACING), -MAX_PULL, MAX_PULL);
        double closing = towed.getDeltaMovement().subtract(cart.getDeltaMovement()).dot(unit);
        Vec3 change = unit.scale(spring + DAMPING * closing);
        cart.setDeltaMovement(cart.getDeltaMovement().add(change));
        towed.setDeltaMovement(towed.getDeltaMovement().subtract(change));
    }

    // A linking bar: shift-use unhitches; otherwise the first cart used is remembered on the bar,
    // and the second is hitched to it. PASS for anything else, so the cart carries on with its own
    // interaction.
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(ModItems.LINKING_BAR.get())) {
            return InteractionResult.PASS;
        }
        if (!(cart.level() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive()) {
            // Off the cart ahead, so it leaves the train with any carts behind it; at the front of a
            // train, off the cart behind
            Link link = ahead.partner != null ? ahead : behind;
            if (link.partner == null) {
                player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.not_linked"));
                return InteractionResult.FAIL;
            }
            if (level.getEntity(link.partner) instanceof AbstractMinecart other) {
                unlink(link, other);
            } else {
                link.clear();
            }
            // In creative, linking didn't use up a bar, so there's none to hand back
            if (!player.hasInfiniteMaterials()) {
                ItemStack bar = new ItemStack(ModItems.LINKING_BAR.get());
                if (!player.getInventory().add(bar)) {
                    cart.spawnAtLocation(level, bar);
                }
            }
            cart.playSound(SoundEvents.CHAIN_HIT, 1.0F, 1.0F);
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
            stack.set(ModDataComponents.LINKING_BAR_START.get(), cart.getUUID());
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            cart.playSound(SoundEvents.CHAIN_HIT, 1.0F, 1.2F);
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.started"));
            return InteractionResult.SUCCESS;
        }
        LinkingBarItem.clearPending(stack);
        if (first.equals(cart.getUUID())) {
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.cancelled"));
            return InteractionResult.SUCCESS;
        }
        if (!(level.getEntity(first) instanceof AbstractMinecart other) || !(other instanceof LinkableCart) || !other.isAlive()) {
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.missing"));
            return InteractionResult.FAIL;
        }
        if (other.position().distanceTo(cart.position()) > LINK_REACH) {
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.too_far"));
            return InteractionResult.FAIL;
        }
        // The cart used first goes in front if it can; otherwise this one does
        Refusal otherLeads = refusal(level, other, cart);
        Refusal thisLeads = otherLeads == null ? null : refusal(level, cart, other);
        if (otherLeads != null && thisLeads != null) {
            Refusal shown = otherLeads.compareTo(thisLeads) >= 0 ? otherLeads : thisLeads;
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar." + shown.message, maxTowed(level, shown == otherLeads ? other : cart)));
            return InteractionResult.FAIL;
        }
        if (otherLeads == null) {
            link(other, cart);
        } else {
            link(cart, other);
        }
        stack.consume(1, player);
        cart.playSound(SoundEvents.CHAIN_PLACE, 1.0F, 1.0F);
        player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.linked"));
        return InteractionResult.SUCCESS;
    }

    // Why the follower can't be hitched behind the leader, or null if it can: the leader's train must
    // be led by a locomotive with room for the follower and every cart behind it
    private static @Nullable Refusal refusal(ServerLevel level, AbstractMinecart leader, AbstractMinecart follower) {
        if (of(leader).behind.partner != null || of(follower).ahead.partner != null) {
            return Refusal.ALREADY_LINKED;
        }
        AbstractMinecart head = leader;
        int ahead = 0;
        AbstractMinecart next;
        while (ahead < MAX_TRAIN && (next = next(level, head, false)) != null) {
            head = next;
            ahead++;
        }
        if (head == follower) {
            return Refusal.SAME_TRAIN;
        }
        int maxTowed = ((LinkableCart) head).maxTowed();
        if (maxTowed <= 0 || of(head).ahead.partner != null) {
            return Refusal.NEEDS_LOCOMOTIVE;
        }
        int added = 1;
        for (AbstractMinecart tail = next(level, follower, true); tail != null && added < MAX_TRAIN; tail = next(level, tail, true)) {
            added++;
        }
        return ahead + added > maxTowed ? Refusal.TOO_LONG : null;
    }

    // The most carts the locomotive at the head of this cart's train can tow, for messages
    private static int maxTowed(ServerLevel level, AbstractMinecart cart) {
        AbstractMinecart head = cart;
        AbstractMinecart next;
        for (int i = 0; i < MAX_TRAIN && (next = next(level, head, false)) != null; i++) {
            head = next;
        }
        return ((LinkableCart) head).maxTowed();
    }

    // The loaded cart linked ahead of or behind this one, if any
    private static @Nullable AbstractMinecart next(ServerLevel level, AbstractMinecart cart, boolean isBehind) {
        UUID id = (isBehind ? of(cart).behind : of(cart).ahead).partner;
        return id != null && level.getEntity(id) instanceof AbstractMinecart next && next instanceof LinkableCart ? next : null;
    }

    private static void link(AbstractMinecart leader, AbstractMinecart follower) {
        CartCoupling front = of(leader);
        CartCoupling back = of(follower);
        front.behind.partner = follower.getUUID();
        front.behind.missingTicks = 0;
        front.setTowedId(follower.getId());
        back.ahead.partner = leader.getUUID();
        back.ahead.missingTicks = 0;
    }

    // Unhitches this cart from the partner at one end, and the partner from it
    private void unlink(Link link, AbstractMinecart other) {
        if (other instanceof LinkableCart linkable) {
            Link back = link == ahead ? linkable.coupling().behind : linkable.coupling().ahead;
            if (cart.getUUID().equals(back.partner)) {
                back.clear();
                if (link == ahead) {
                    linkable.coupling().setTowedId(NONE);
                }
            }
        }
        link.clear();
        if (link == behind) {
            setTowedId(NONE);
        }
    }

    private static void dropBar(ServerLevel level, Entity at) {
        if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
            at.spawnAtLocation(level, new ItemStack(ModItems.LINKING_BAR.get()));
        }
    }

    // Run when the cart leaves the level. Broken or killed, it unhitches the carts either side and
    // drops a bar for each link; just unloaded with its chunk, it keeps its links for when it comes back.
    public void onRemove(Entity.RemovalReason reason) {
        if (!isLinked() || !(cart.level() instanceof ServerLevel level)
                || reason == Entity.RemovalReason.UNLOADED_TO_CHUNK || reason == Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
            return;
        }
        for (Link link : new Link[] {ahead, behind}) {
            if (link.partner == null) {
                continue;
            }
            if (level.getEntity(link.partner) instanceof AbstractMinecart other) {
                unlink(link, other);
            } else {
                link.clear();
            }
            if (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.CHANGED_DIMENSION) {
                dropBar(level, cart);
            }
        }
        setTowedId(NONE);
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
    }
}
