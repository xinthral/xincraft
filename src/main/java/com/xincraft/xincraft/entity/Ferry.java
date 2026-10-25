package com.xincraft.xincraft.entity;

import java.util.function.Supplier;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// The carriage's boat: a passenger raft, seating two, for hitching behind a motorboat with a linking
// bar (see LinkedTransport). Its passengers ride but don't paddle or steer, so it goes where it's
// towed and otherwise drifts. Like the carriage, it won't take hostile mobs aboard unless it has the
// cell upgrade, which carries them caged (see MobCell). Takes upgrades (see CartUpgrades).
public class Ferry extends AbstractBoat implements UpgradableCart, LinkableTransport {
    private static final EntityDataAccessor<Integer> DATA_UPGRADES = SynchedEntityData.defineId(Ferry.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TOWED = SynchedEntityData.defineId(Ferry.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_HITCHED = SynchedEntityData.defineId(Ferry.class, EntityDataSerializers.BOOLEAN);

    private final CartUpgrades upgrades = new CartUpgrades(this, DATA_UPGRADES);
    private final LinkedTransport transport = new LinkedTransport(this, DATA_TOWED, DATA_HITCHED);

    public Ferry(EntityType<? extends Ferry> type, Level level, Supplier<Item> dropItem) {
        super(type, level, dropItem);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_UPGRADES, 0);
        builder.define(DATA_TOWED, LinkedTransport.NONE);
        builder.define(DATA_HITCHED, false);
    }

    @Override
    public CartUpgrades upgrades() {
        return upgrades;
    }

    @Override
    public LinkedTransport transport() {
        return transport;
    }

    // As vanilla's raft
    @Override
    protected double rideHeight(EntityDimensions dimensions) {
        return dimensions.height() * 0.8888889F;
    }

    // Passengers only ride: nobody steers a ferry
    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return null;
    }

    // Hostile mobs only ride in a cell; this also keeps it from picking them up
    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return MobCell.canCarry(this, passenger) && super.canAddPassenger(passenger);
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel serverLevel) {
            transport.tick(serverLevel);
            upgrades.tickShimmer(serverLevel);
            if (upgrades.has(CartUpgrade.CELL)) {
                MobCell.tick(this);
            }
            if (upgrades.has(CartUpgrade.SHRIEKER)) {
                upgrades.clearTrackAhead(serverLevel, getDeltaMovement());
            }
        }
        super.tick();
    }

    // The linking bar and upgrades first; then, with a mob in the cell, right-click lets it out
    // (or with a lead, ties one ready for it; see MobCell); otherwise right-click to ride, as
    // any boat
    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        InteractionResult result = transport.interact(player, hand);
        if (result == InteractionResult.PASS) {
            result = upgrades.interact(this, player, hand);
        }
        if (result == InteractionResult.PASS && upgrades.has(CartUpgrade.CELL)) {
            result = MobCell.interact(this, player, hand);
        }
        return result != InteractionResult.PASS ? result : super.interact(player, hand, location);
    }

    @Override
    public void push(Entity entity) {
        if (!transport.isLinkedTo(entity) && !upgrades.pushAside(entity)) {
            super.push(entity);
        }
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return !transport.isLinkedTo(entity) && super.canCollideWith(entity);
    }

    @Override
    protected void destroy(ServerLevel level, DamageSource source) {
        super.destroy(level, source);
        upgrades.dropAll(level);
    }

    @Override
    public void remove(RemovalReason reason) {
        transport.onRemove(reason);
        super.remove(reason);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        upgrades.save(output);
        transport.save(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        upgrades.load(input);
        transport.load(input);
    }
}
