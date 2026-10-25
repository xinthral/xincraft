package com.xincraft.xincraft.entity;

import com.xincraft.xincraft.registry.ModItems;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

// A rideable passenger minecart, lacquered and trimmed in bronze, for hitching into a locomotive's train
// with a linking bar (see LinkedTransport). Otherwise an ordinary minecart. Takes upgrades (see
// CartUpgrades). It won't take hostile mobs aboard unless it has the cell upgrade, which carries
// them caged (see MobCell).
public class Carriage extends Minecart implements UpgradableCart, LinkableTransport {
    private static final EntityDataAccessor<Integer> DATA_UPGRADES = SynchedEntityData.defineId(Carriage.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TOWED = SynchedEntityData.defineId(Carriage.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_HITCHED = SynchedEntityData.defineId(Carriage.class, EntityDataSerializers.BOOLEAN);

    private final CartUpgrades upgrades = new CartUpgrades(this, DATA_UPGRADES);
    private final LinkedTransport transport = new LinkedTransport(this, DATA_TOWED, DATA_HITCHED);

    public Carriage(EntityType<?> type, Level level) {
        super(type, level);
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

    // The linking bar and upgrades first; then, with a mob in the cell, right-click lets it out
    // (or with a lead, ties one ready for it; see MobCell); otherwise right-click to ride, as
    // any minecart
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
    public void tick() {
        if (level() instanceof ServerLevel serverLevel) {
            transport.tick(serverLevel);
            upgrades.tickShimmer(serverLevel);
            if (upgrades.has(CartUpgrade.CELL)) {
                MobCell.tick(this);
            }
        }
        super.tick();
    }

    // Hostile mobs only ride in a cell; this also keeps a cart without one from picking them up
    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return MobCell.canCarry(this, passenger) && super.canAddPassenger(passenger);
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
    protected Item getDropItem() {
        return ModItems.CARRIAGE.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.CARRIAGE.get());
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
