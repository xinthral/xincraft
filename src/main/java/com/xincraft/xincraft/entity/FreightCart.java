package com.xincraft.xincraft.entity;

import com.xincraft.xincraft.registry.ModItems;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

// A minecart carrying a shulker box, and working like one (see ShulkerHold): opened by
// right-clicking, and breaks into a single item with everything still inside. Hoppers load and
// unload it. Can be hitched into a locomotive's train with a linking
// bar (see LinkedTransport). Takes upgrades (see CartUpgrades).
public class FreightCart extends AbstractMinecartContainer implements UpgradableCart, LinkableTransport {
    private static final EntityDataAccessor<Integer> DATA_UPGRADES = SynchedEntityData.defineId(FreightCart.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TOWED = SynchedEntityData.defineId(FreightCart.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_HITCHED = SynchedEntityData.defineId(FreightCart.class, EntityDataSerializers.BOOLEAN);

    private final CartUpgrades upgrades = new CartUpgrades(this, DATA_UPGRADES);
    private final LinkedTransport transport = new LinkedTransport(this, DATA_TOWED, DATA_HITCHED);

    public FreightCart(EntityType<?> type, Level level) {
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

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return Blocks.SHULKER_BOX.defaultBlockState();
    }

    @Override
    public int getDefaultDisplayOffset() {
        return 8;
    }

    // The linking bar and upgrades first; then right-click opens the box
    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        InteractionResult result = transport.interact(player, hand);
        if (result == InteractionResult.PASS) {
            result = upgrades.interact(this, player, hand);
        }
        if (result != InteractionResult.PASS) {
            return result;
        }
        result = interactWithContainerVehicle(player);
        if (result.consumesAction() && player.level() instanceof ServerLevel) {
            gameEvent(GameEvent.CONTAINER_OPEN, player);
        }
        return result;
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel serverLevel) {
            transport.tick(serverLevel);
            upgrades.tickShimmer(serverLevel);
        }
        super.tick();
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
    public void destroy(ServerLevel level, DamageSource source) {
        ShulkerHold.destroy(this, level, getDropItem());
        upgrades.dropAll(level);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        ShulkerHold.unpack(components, getItemStacks());
    }

    @Override
    public void remove(RemovalReason reason) {
        transport.onRemove(reason);
        super.remove(reason);
    }

    @Override
    public void startOpen(ContainerUser user) {
        ShulkerHold.opened(this, user);
    }

    @Override
    public void stopOpen(ContainerUser user) {
        ShulkerHold.closed(this, user);
    }

    @Override
    public int getContainerSize() {
        return ShulkerHold.SLOT_COUNT;
    }

    // No shulker boxes, freight carts or anything else that can't go inside a container item
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return ShulkerHold.canHold(stack);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new ShulkerBoxMenu(containerId, inventory, this);
    }

    @Override
    protected Item getDropItem() {
        return ModItems.FREIGHT_CART.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.FREIGHT_CART.get());
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
