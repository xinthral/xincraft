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

// The link between a locomotive and the one cart it tows behind it, made with a linking bar
// (item.LinkingBarItem): use the bar on one cart and then the other. The locomotive leads; if both
// are locomotives, the one used first does. Each cart takes only one link, so a train is a
// locomotive and one cart. Shift-using a bar on either cart unhitches them and hands the bar back;
// breaking either cart, or the link snapping (pulled too far apart, as by an ender rail), drops it.
//
// The link holds the carts' centres SPACING apart, like a stiff spring with damping: each tick the
// locomotive pulls (or pushes) the towed cart, and is held back by it in turn, along the line
// between them. On rails, vanilla turns that into motion along the track, so the towed cart follows
// the locomotive around curves and over slopes, pivoting about the bar. Linked carts don't collide
// with or bump each other, so they can't jam on a tight corner. Each side keeps the other's UUID,
// which is saved; the locomotive also syncs the towed cart's entity id to clients to draw the bar
// (client.RetexturedMinecartRenderer).
public final class CartCoupling {
    // Centre to centre: the cart bodies are 1.25 blocks long, leaving about 0.7 blocks for the bar
    public static final double SPACING = 1.95;
    // How far each end of the bar, where it pivots, is from its cart's centre
    public static final double PIVOT = 0.6875;
    public static final int NONE = -1;
    // Furthest apart (centre to centre) two carts can be hitched from, and how far the link
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

    private final AbstractMinecart cart;
    // The towed cart's entity id, for drawing the bar; only locomotives have one
    private final @Nullable EntityDataAccessor<Integer> towedData;
    private @Nullable UUID partner;
    // Whether this cart is in front, towing the partner
    private boolean towing;
    private int missingTicks;

    public CartCoupling(AbstractMinecart cart, @Nullable EntityDataAccessor<Integer> towedData) {
        this.cart = cart;
        this.towedData = towedData;
    }

    public boolean isLinked() {
        return partner != null;
    }

    public boolean isLinkedTo(Entity entity) {
        return partner != null && partner.equals(entity.getUUID());
    }

    // Client side as well: the entity id of the cart this one tows, or NONE
    public int towedId() {
        return towedData != null ? cart.getEntityData().get(towedData) : NONE;
    }

    private void setTowedId(int id) {
        if (towedData != null) {
            cart.getEntityData().set(towedData, id);
        }
    }

    // Run by the cart at the start of each server tick, before it moves
    public void tick(ServerLevel level) {
        if (partner == null) {
            return;
        }
        Entity found = level.getEntity(partner);
        if (found == null) {
            setTowedId(NONE);
            if (++missingTicks >= MISSING_TICKS) {
                clear();
            }
            return;
        }
        missingTicks = 0;
        // Forget a partner that's no longer linked back, e.g. unhitched while this cart was unloaded
        if (!(found instanceof AbstractMinecart other) || !(found instanceof LinkableCart linkable)
                || !found.isAlive() || !linkable.coupling().isLinkedTo(cart)) {
            clear();
            return;
        }
        if (!towing) {
            return;
        }
        setTowedId(other.getId());
        if (cart.position().distanceTo(other.position()) > SNAP_DISTANCE) {
            unlink(other);
            dropBar(level, cart);
            return;
        }
        pull(other);
    }

    // The spring and damper between the two carts, along the line between them (on the level, so
    // a slope doesn't lift or press down on either cart)
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
            if (partner == null) {
                player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.not_linked"));
                return InteractionResult.FAIL;
            }
            if (level.getEntity(partner) instanceof AbstractMinecart other) {
                unlink(other);
            } else {
                clear();
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
            if (partner != null) {
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
        if (!(level.getEntity(first) instanceof AbstractMinecart other) || !(other instanceof LinkableCart otherCart) || !other.isAlive()) {
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.missing"));
            return InteractionResult.FAIL;
        }
        if (partner != null || otherCart.coupling().isLinked()) {
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.already_linked"));
            return InteractionResult.FAIL;
        }
        if (other.position().distanceTo(cart.position()) > LINK_REACH) {
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.too_far"));
            return InteractionResult.FAIL;
        }
        LinkableCart self = (LinkableCart) cart;
        // The cart used first leads, if it can tow; otherwise this one must
        AbstractMinecart leader = otherCart.canTow() ? other : self.canTow() ? cart : null;
        if (leader == null) {
            player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.needs_locomotive"));
            return InteractionResult.FAIL;
        }
        AbstractMinecart follower = leader == cart ? other : cart;
        link(leader, follower);
        stack.consume(1, player);
        cart.playSound(SoundEvents.CHAIN_PLACE, 1.0F, 1.0F);
        player.sendOverlayMessage(Component.translatable("message.xincraft.linking_bar.linked"));
        return InteractionResult.SUCCESS;
    }

    private static void link(AbstractMinecart leader, AbstractMinecart follower) {
        CartCoupling front = ((LinkableCart) leader).coupling();
        CartCoupling back = ((LinkableCart) follower).coupling();
        front.partner = follower.getUUID();
        front.towing = true;
        front.missingTicks = 0;
        front.setTowedId(follower.getId());
        back.partner = leader.getUUID();
        back.towing = false;
        back.missingTicks = 0;
    }

    // Unhitches this cart and its partner
    private void unlink(AbstractMinecart other) {
        if (other instanceof LinkableCart linkable && linkable.coupling().isLinkedTo(cart)) {
            linkable.coupling().clear();
        }
        clear();
    }

    private void clear() {
        partner = null;
        towing = false;
        missingTicks = 0;
        setTowedId(NONE);
    }

    private static void dropBar(ServerLevel level, Entity at) {
        if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
            at.spawnAtLocation(level, new ItemStack(ModItems.LINKING_BAR.get()));
        }
    }

    // Run when the cart leaves the level. Broken or killed, it unhitches its partner and drops the
    // bar; just unloaded with its chunk, it keeps the link for when it comes back.
    public void onRemove(Entity.RemovalReason reason) {
        if (partner == null || !(cart.level() instanceof ServerLevel level)
                || reason == Entity.RemovalReason.UNLOADED_TO_CHUNK || reason == Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
            return;
        }
        if (level.getEntity(partner) instanceof AbstractMinecart other) {
            unlink(other);
        } else {
            clear();
        }
        if (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.CHANGED_DIMENSION) {
            dropBar(level, cart);
        }
    }

    public void save(ValueOutput output) {
        if (partner != null) {
            output.store("Coupled", UUIDUtil.CODEC, partner);
            output.putBoolean("Towing", towing);
        }
    }

    public void load(ValueInput input) {
        partner = input.read("Coupled", UUIDUtil.CODEC).orElse(null);
        towing = partner != null && input.getBooleanOr("Towing", false);
    }
}
