package com.xincraft.xincraft.entity;

import com.xincraft.xincraft.registry.ModItems;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

// A minecart carrying a shulker box, and working like one: 27 slots, opened by right-clicking, and
// broken it drops as a single item with everything still inside, which places back down full. Like
// a shulker box, it can't hold shulker boxes or other storage carts (anything that can't go inside
// a container item), from players or hoppers. Can be hitched into a locomotive's train with a linking
// bar (see CartCoupling). Takes upgrades (see CartUpgrades).
public class StorageCart extends AbstractMinecartContainer implements UpgradableCart, LinkableCart {
    public static final int SLOT_COUNT = 27;
    public static final int ROW_SIZE = 9;

    private static final EntityDataAccessor<Integer> DATA_UPGRADES = SynchedEntityData.defineId(StorageCart.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TOWED = SynchedEntityData.defineId(StorageCart.class, EntityDataSerializers.INT);

    private final CartUpgrades upgrades = new CartUpgrades(this, DATA_UPGRADES);
    private final CartCoupling coupling = new CartCoupling(this, DATA_TOWED);

    public StorageCart(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_UPGRADES, 0);
        builder.define(DATA_TOWED, CartCoupling.NONE);
    }

    @Override
    public CartUpgrades upgrades() {
        return upgrades;
    }

    @Override
    public CartCoupling coupling() {
        return coupling;
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
        InteractionResult result = coupling.interact(player, hand);
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
            coupling.tick(serverLevel);
            upgrades.tickShimmer(serverLevel);
        }
        super.tick();
    }

    @Override
    public void push(Entity entity) {
        if (!coupling.isLinkedTo(entity) && !upgrades.pushAside(entity)) {
            super.push(entity);
        }
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return !coupling.isLinkedTo(entity) && super.canCollideWith(entity);
    }

    // Drops as one item holding the contents, as a shulker box does, rather than spilling them
    @Override
    public void destroy(ServerLevel level, DamageSource source) {
        ItemStack drop = new ItemStack(getDropItem());
        drop.set(DataComponents.CUSTOM_NAME, getCustomName());
        drop.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(getItemStacks().subList(0, SLOT_COUNT)));
        // Emptied first, so removing the cart doesn't spill them as well
        clearContent();
        kill(level);
        if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
            spawnAtLocation(level, drop);
        }
        upgrades.dropAll(level);
    }

    // Placed from an item that's holding contents, it starts with them
    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        ItemContainerContents contents = components.get(DataComponents.CONTAINER);
        if (contents != null) {
            contents.copyInto(getItemStacks());
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        coupling.onRemove(reason);
        super.remove(reason);
    }

    @Override
    public void startOpen(ContainerUser user) {
        playBoxSound(user, SoundEvents.SHULKER_BOX_OPEN);
    }

    @Override
    public void stopOpen(ContainerUser user) {
        level().gameEvent(GameEvent.CONTAINER_CLOSE, position(), GameEvent.Context.of(user.getLivingEntity()));
        playBoxSound(user, SoundEvents.SHULKER_BOX_CLOSE);
    }

    private void playBoxSound(ContainerUser user, SoundEvent sound) {
        if (!user.getLivingEntity().isSpectator()) {
            level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.BLOCKS, 0.5F, random.nextFloat() * 0.1F + 0.9F);
        }
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    // No shulker boxes, storage carts or anything else that can't go inside a container item
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem().canFitInsideContainerItems();
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new ShulkerBoxMenu(containerId, inventory, this);
    }

    @Override
    protected Item getDropItem() {
        return ModItems.STORAGE_CART.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.STORAGE_CART.get());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        upgrades.save(output);
        coupling.save(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        upgrades.load(input);
        coupling.load(input);
    }
}
