package com.xincraft.xincraft.entity;

import com.xincraft.xincraft.common.MobHolder;
import java.util.EnumSet;
import java.util.List;
import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.block.BronzeChestBlockEntity;
import com.xincraft.xincraft.common.CommonComponents;
import com.xincraft.xincraft.golems.GolemEntityTypes;
import com.xincraft.xincraft.golems.GolemItems;
import com.xincraft.xincraft.materials.MaterialTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.animal.golem.CopperGolemOxidationLevel;
import net.minecraft.world.entity.animal.golem.CopperGolemOxidationLevels;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jspecify.annotations.Nullable;

// A copper golem refitted with a bronze golem helm (item.BronzeGolemHelmItem). It stays within the
// range a copper golem searches (32 blocks across, 8 up and down) of the nearest bronze chest, and
// moves no items. Near an iron golem it now and then holds out a copper nugget, as an iron golem
// offers a villager a poppy.
//
// Made at the apothecary altar with a diamond sword, it's a combat golem: the same mob with the sword
// in its main hand. The sword is what it fights with, so its damage and enchantments apply as they do
// on any mob, and looting comes from it too. It hunts hostile mobs in its area and carries what they
// drop, one stack at a time in its off hand, to the chest. Killing comes first, but it empties its
// hands before it fights. Each hit wears the sword. When the sword breaks, or the golem takes a
// killing blow (which destroys the sword), it's a bronze golem again, at full health.
//
// Goal order is what gives those priorities: depositing is above fighting, and fighting is above
// picking up.
@Mod.EventBusSubscriber(modid = XincraftMod.MODID)
public class BronzeGolem extends AbstractGolem implements ContainerUser {
    public static final int AREA_HORIZONTAL = 32;
    public static final int AREA_VERTICAL = 8;
    private static final int HOME_CHECK_TICKS = 100;
    // How often it looks for drops. Goals are only checked every other tick, on odd or even ticks
    // according to the mob's id, so this counts rather than testing for particular ticks.
    private static final int DROP_CHECK_TICKS = 10;
    private static final double CHEST_REACH_SQR = 6.25;
    private static final EquipmentSlot SWORD_SLOT = EquipmentSlot.MAINHAND;
    private static final EquipmentSlot CARRY_SLOT = EquipmentSlot.OFFHAND;
    // Marks an item dropped by something a golem killed, as it leaves other items alone
    private static final String DROP_KEY = XincraftMod.MODID + ":GolemDrop";

    private static final EntityDataAccessor<CopperGolemState> DATA_STATE =
        SynchedEntityData.defineId(BronzeGolem.class, EntityDataSerializers.COPPER_GOLEM_STATE);
    private static final EntityDataAccessor<Boolean> DATA_OFFERING =
        SynchedEntityData.defineId(BronzeGolem.class, EntityDataSerializers.BOOLEAN);

    private @Nullable BlockPos home;
    private @Nullable BlockPos openedChestPos;
    private int idleAnimationStartTick;
    private int nextDropCheck;
    private @Nullable ItemEntity foundDrop;
    private final AnimationState idleAnimationState = new AnimationState();
    private final AnimationState dropItemAnimationState = new AnimationState();

    public BronzeGolem(EntityType<? extends AbstractGolem> type, Level level) {
        super(type, level);
        getNavigation().setRequiredPathLength(48.0F);
        getNavigation().setCanOpenDoors(true);
        setPersistenceRequired();
        setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, 16.0F);
        setPathfindingMalus(PathType.DAMAGING_IN_NEIGHBOR, 16.0F);
        setPathfindingMalus(PathType.FIRE, -1.0F);
    }

    // Base attack damage of 1, as a player's, so a diamond sword's bonus makes the 7 it does for a player
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MOVEMENT_SPEED, 0.2F)
            .add(Attributes.STEP_HEIGHT, 1.0)
            .add(Attributes.MAX_HEALTH, 40.0)
            .add(Attributes.ARMOR, 6.0)
            .add(Attributes.ATTACK_DAMAGE, 1.0)
            .add(Attributes.FOLLOW_RANGE, AREA_HORIZONTAL);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        goalSelector.addGoal(1, new PanicGoal(this, 1.5) {
            @Override
            public boolean canUse() {
                return !isCombat() && super.canUse();
            }
        });
        goalSelector.addGoal(2, new DepositGoal());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.5, true));
        goalSelector.addGoal(4, new PickUpDropsGoal());
        goalSelector.addGoal(5, new OfferNuggetGoal());
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // It doesn't turn on players who hit it
        targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return isCombat() && !(getLastHurtByMob() instanceof Player) && super.canUse();
            }
        });
        TargetingConditions.Selector hostile = (target, level) -> isCombat() && target instanceof Enemy && !MobHolder.isCaged(target) && inArea(target);
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 5, true, false, hostile));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_STATE, CopperGolemState.IDLE);
        entityData.define(DATA_OFFERING, false);
    }

    public boolean isCombat() {
        return !getItemBySlot(SWORD_SLOT).isEmpty();
    }

    public void arm(ItemStack sword) {
        setItemSlot(SWORD_SLOT, sword);
    }

    private ItemStack carried() {
        return getItemBySlot(CARRY_SLOT);
    }

    // For the renderer
    public boolean isOfferingNugget() {
        return entityData.get(DATA_OFFERING);
    }

    public CopperGolemState getGolemState() {
        return entityData.get(DATA_STATE);
    }

    public AnimationState getIdleAnimationState() {
        return idleAnimationState;
    }

    public AnimationState getDropItemAnimationState() {
        return dropItemAnimationState;
    }

    // ---- Its area ----

    private boolean inArea(Entity entity) {
        if (home == null) {
            return true;
        }
        return Math.abs(entity.getX() - home.getX()) <= AREA_HORIZONTAL && Math.abs(entity.getZ() - home.getZ()) <= AREA_HORIZONTAL
            && Math.abs(entity.getY() - home.getY()) <= AREA_VERTICAL;
    }

    // Keeps to the chest it has, or finds the nearest; with none around it wanders freely
    private void updateHome(ServerLevel level) {
        if (home != null && !(level.isLoaded(home) && level.getBlockEntity(home) instanceof BronzeChestBlockEntity)) {
            home = null;
        }
        if (home == null) {
            home = findChest(level);
        }
        if (home != null) {
            setHomeTo(home, AREA_HORIZONTAL);
        } else {
            clearHome();
        }
    }

    private @Nullable BlockPos findChest(ServerLevel level) {
        BlockPos at = blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int cx = (at.getX() - AREA_HORIZONTAL) >> 4; cx <= (at.getX() + AREA_HORIZONTAL) >> 4; cx++) {
            for (int cz = (at.getZ() - AREA_HORIZONTAL) >> 4; cz <= (at.getZ() + AREA_HORIZONTAL) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockPos pos : chunk.getBlockEntitiesPos()) {
                    double distance = pos.distSqr(at);
                    if (distance < bestDistance && Math.abs(pos.getX() - at.getX()) <= AREA_HORIZONTAL
                            && Math.abs(pos.getZ() - at.getZ()) <= AREA_HORIZONTAL && Math.abs(pos.getY() - at.getY()) <= AREA_VERTICAL
                            && chunk.getBlockEntity(pos) instanceof BronzeChestBlockEntity) {
                        best = pos;
                        bestDistance = distance;
                    }
                }
            }
        }
        return best;
    }

    // ---- Fighting ----

    // Wears the sword for the hit, unless the sword's own attack already has
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        ItemStack sword = getItemBySlot(SWORD_SLOT);
        int damageBefore = sword.getDamageValue();
        boolean hit = super.doHurtTarget(level, target);
        if (hit && !sword.isEmpty()) {
            swing(InteractionHand.MAIN_HAND, true);
            if (sword.getDamageValue() == damageBefore) {
                sword.hurtAndBreak(1, this, SWORD_SLOT);
            }
        }
        if (sword.isEmpty()) {
            disarm(level);
        }
        return hit;
    }

    // A killing blow takes the sword instead of the golem
    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel level && isCombat()) {
            disarm(level);
            return;
        }
        super.die(source);
    }

    // Back to being a bronze golem, whatever it was carrying dropped where it stands
    private void disarm(ServerLevel level) {
        setItemSlot(SWORD_SLOT, ItemStack.EMPTY);
        dropCarried(level);
        setTarget(null);
        setHealth(getMaxHealth());
        level.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.5, getZ(), 12, 0.25, 0.25, 0.25, 0.02);
        playSound(SoundEvents.ITEM_BREAK.value());
    }

    private void dropCarried(ServerLevel level) {
        ItemStack carried = carried();
        if (!carried.isEmpty()) {
            spawnAtLocation(level, carried);
            setItemSlot(CARRY_SLOT, ItemStack.EMPTY);
        }
    }

    // Marks what a golem's victim drops, for it to pick up
    @SubscribeEvent
    public static boolean onLivingDrops(LivingDropsEvent event) {
        if (event.getSource().getEntity() instanceof BronzeGolem) {
            event.getDrops().forEach(item -> item.getPersistentData().putBoolean(DROP_KEY, true));
        }
        return false;
    }

    // The nearest drop it could carry: one that stacks with what it holds, if it holds anything
    private @Nullable ItemEntity findDrop(ServerLevel level) {
        ItemStack carried = carried();
        if (!carried.isEmpty() && carried.getCount() >= carried.getMaxStackSize()) {
            return null;
        }
        AABB box = home != null ? new AABB(home).inflate(AREA_HORIZONTAL, AREA_VERTICAL, AREA_HORIZONTAL) : getBoundingBox().inflate(AREA_HORIZONTAL, AREA_VERTICAL, AREA_HORIZONTAL);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box, item -> item.isAlive() && item.getPersistentData().getBooleanOr(DROP_KEY, false)
            && (carried.isEmpty() || ItemStack.isSameItemSameComponents(carried, item.getItem())));
        ItemEntity nearest = null;
        for (ItemEntity item : items) {
            if (nearest == null || item.distanceToSqr(this) < nearest.distanceToSqr(this)) {
                nearest = item;
            }
        }
        return nearest;
    }

    // The result of findDrop, looked up afresh every so often
    private @Nullable ItemEntity nearbyDrop(ServerLevel level) {
        if (tickCount >= nextDropCheck) {
            nextDropCheck = tickCount + DROP_CHECK_TICKS;
            foundDrop = findDrop(level);
        }
        return foundDrop != null && foundDrop.isAlive() ? foundDrop : null;
    }

    // ---- Being looked after ----

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(MaterialTags.REPAIRS_BRONZE_ARMOR) && getHealth() < getMaxHealth()) {
            if (level() instanceof ServerLevel) {
                heal(getMaxHealth() / 4.0F);
                playSound(SoundEvents.COPPER_GOLEM_ITEM_GET, 1.0F, 1.2F);
                stack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.isEmpty() && player.isSecondaryUseActive()) {
            if (level() instanceof ServerLevel level) {
                dropCarried(level);
                if (!player.getInventory().add(toItem())) {
                    spawnAtLocation(level, toItem());
                }
                discard();
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    // The golem as an item, sword and name included
    private ItemStack toItem() {
        ItemStack item = new ItemStack(isCombat() ? GolemItems.COMBAT_GOLEM.get() : GolemItems.BRONZE_GOLEM.get());
        if (isCombat()) {
            item.set(CommonComponents.GOLEM_SWORD.get(), getItemBySlot(SWORD_SLOT).copy());
        }
        if (hasCustomName()) {
            item.set(DataComponents.CUSTOM_NAME, getCustomName());
        }
        return item;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(isCombat() ? GolemItems.COMBAT_GOLEM.get() : GolemItems.BRONZE_GOLEM.get());
    }

    // ---- The rest of being a mob ----

    @Override
    protected void customServerAiStep(ServerLevel level) {
        if (tickCount % HOME_CHECK_TICKS == 1) {
            updateHome(level);
        }
        super.customServerAiStep(level);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() && !isNoAi()) {
            setupAnimationStates();
        }
    }

    private void setupAnimationStates() {
        if (getGolemState() == CopperGolemState.DROPPING_ITEM) {
            idleAnimationState.stop();
            idleAnimationStartTick = 0;
            dropItemAnimationState.startIfStopped(tickCount);
            return;
        }
        dropItemAnimationState.stop();
        if (idleAnimationStartTick == tickCount) {
            idleAnimationState.start(tickCount);
        } else if (idleAnimationStartTick == 0) {
            idleAnimationStartTick = tickCount + random.nextInt(200, 240);
        }
        if (tickCount == idleAnimationStartTick + 10) {
            if (!isSilent()) {
                level().playLocalSound(getX(), getY(), getZ(), sounds().spinHeadSound(), getSoundSource(), 1.0F, 1.0F, false);
            }
            idleAnimationStartTick = 0;
        }
    }

    private static CopperGolemOxidationLevel sounds() {
        return CopperGolemOxidationLevels.getOxidationLevel(WeatheringCopper.WeatherState.UNAFFECTED);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return sounds().hurtSound();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return sounds().deathSound();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        playSound(sounds().stepSound(), 1.0F, 1.0F);
    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, 0.75F * getEyeHeight(), 0.0);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (home != null) {
            output.store("Home", BlockPos.CODEC, home);
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        home = input.read("Home", BlockPos.CODEC).orElse(null);
    }

    @Override
    public boolean hasContainerOpen(ContainerOpenersCounter container, BlockPos blockPos) {
        if (openedChestPos == null) {
            return false;
        }
        BlockState state = level().getBlockState(openedChestPos);
        return openedChestPos.equals(blockPos) || state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE
            && ChestBlock.getConnectedBlockPos(openedChestPos, state).equals(blockPos);
    }

    @Override
    public double getContainerInteractionRange() {
        return 3.0;
    }

    // ---- Goals ----

    // Takes what it's carrying to the chest: at once if there's something to fight, or when it can
    // carry no more. If it can't get there, it gives up for a while rather than standing idle while
    // something attacks it. What won't fit in the chest it leaves on the ground beside it, unmarked.
    private class DepositGoal extends Goal {
        private static final int GIVE_UP_TICKS = 300;
        private static final int RETRY_TICKS = 200;
        private static final int OPEN_TICKS = 15;
        private static final int CLOSE_TICKS = 25;
        private int walking;
        private int opened = -1;
        private long retryAt;
        private @Nullable Container container;

        DepositGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (carried().isEmpty() || home == null || level().getGameTime() < retryAt || !(level() instanceof ServerLevel level)) {
                return false;
            }
            return getTarget() != null || nearbyDrop(level) == null;
        }

        @Override
        public boolean canContinueToUse() {
            return home != null && opened <= CLOSE_TICKS && walking < GIVE_UP_TICKS;
        }

        @Override
        public void start() {
            walking = 0;
            opened = -1;
            container = null;
        }

        @Override
        public void tick() {
            if (home == null) {
                return;
            }
            getLookControl().setLookAt(Vec3.atCenterOf(home));
            if (opened < 0) {
                walking++;
                if (distanceToSqr(Vec3.atCenterOf(home)) > CHEST_REACH_SQR) {
                    if (walking % 20 == 1) {
                        getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, getTarget() != null ? 1.5 : 1.0);
                    }
                    return;
                }
                getNavigation().stop();
                open();
                return;
            }
            opened++;
            if (opened == OPEN_TICKS) {
                unload();
            }
        }

        private void open() {
            BlockState state = level().getBlockState(home);
            container = state.getBlock() instanceof ChestBlock chest ? ChestBlock.getContainer(chest, state, level(), home, true) : null;
            opened = 0;
            if (container != null) {
                container.startOpen(BronzeGolem.this);
                openedChestPos = home;
                entityData.set(DATA_STATE, CopperGolemState.DROPPING_ITEM);
                playSound(SoundEvents.COPPER_GOLEM_ITEM_DROP);
            }
        }

        private void unload() {
            if (container == null || !(level() instanceof ServerLevel level)) {
                return;
            }
            ItemStack left = HopperBlockEntity.addItem(null, container, carried(), null);
            setItemSlot(CARRY_SLOT, ItemStack.EMPTY);
            if (!left.isEmpty()) {
                spawnAtLocation(level, left);
            }
        }

        @Override
        public void stop() {
            if (walking >= GIVE_UP_TICKS) {
                retryAt = level().getGameTime() + RETRY_TICKS;
            }
            if (container != null && container.getEntitiesWithContainerOpen().contains(BronzeGolem.this)) {
                container.stopOpen(BronzeGolem.this);
            }
            container = null;
            openedChestPos = null;
            entityData.set(DATA_STATE, CopperGolemState.IDLE);
            getNavigation().stop();
        }
    }

    // Collects what its victims dropped, while there's nothing left to fight
    private class PickUpDropsGoal extends Goal {
        private static final double REACH_SQR = 2.25;
        private @Nullable ItemEntity drop;
        private int ticks;

        PickUpDropsGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!isCombat() || getTarget() != null || !(level() instanceof ServerLevel level)) {
                return false;
            }
            drop = nearbyDrop(level);
            return drop != null;
        }

        @Override
        public boolean canContinueToUse() {
            return isCombat() && getTarget() == null && drop != null && drop.isAlive();
        }

        @Override
        public void start() {
            ticks = 0;
        }

        @Override
        public void tick() {
            if (drop == null) {
                return;
            }
            if (distanceToSqr(drop) <= REACH_SQR) {
                take(drop);
                drop = null;
                foundDrop = null;
                nextDropCheck = 0;
            } else if (ticks++ % 10 == 0) {
                getNavigation().moveTo(drop, 1.25);
            }
        }

        private void take(ItemEntity item) {
            ItemStack stack = item.getItem();
            ItemStack carried = carried();
            int moved = carried.isEmpty() ? stack.getCount() : Math.min(stack.getCount(), carried.getMaxStackSize() - carried.getCount());
            if (carried.isEmpty()) {
                setItemSlot(CARRY_SLOT, stack.copy());
            } else {
                carried.grow(moved);
            }
            BronzeGolem.this.take(item, moved);
            stack.shrink(moved);
            if (stack.isEmpty()) {
                item.discard();
            }
            playSound(SoundEvents.COPPER_GOLEM_ITEM_GET);
        }

        @Override
        public void stop() {
            drop = null;
            getNavigation().stop();
        }
    }

    // Stands and holds out a copper nugget to an iron golem nearby, now and then, with a free hand
    private class OfferNuggetGoal extends Goal {
        private static final double RANGE = 6.0;
        private static final int CHANCE = 400;
        private static final int OFFER_TICKS = 200;
        private final TargetingConditions context = TargetingConditions.forNonCombat().range(RANGE);
        private @Nullable IronGolem friend;
        private int ticks;

        OfferNuggetGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (getTarget() != null || !carried().isEmpty() || random.nextInt(CHANCE) != 0 || !(level() instanceof ServerLevel level)) {
                return false;
            }
            friend = level.getNearestEntity(IronGolem.class, context, BronzeGolem.this, getX(), getY(), getZ(), getBoundingBox().inflate(RANGE, 2.0, RANGE));
            return friend != null;
        }

        @Override
        public boolean canContinueToUse() {
            return ticks > 0 && getTarget() == null && carried().isEmpty() && friend != null && friend.isAlive();
        }

        @Override
        public void start() {
            ticks = adjustedTickDelay(OFFER_TICKS);
            entityData.set(DATA_OFFERING, true);
            getNavigation().stop();
        }

        @Override
        public void tick() {
            ticks--;
            if (friend != null) {
                getLookControl().setLookAt(friend, 30.0F, 30.0F);
                friend.getLookControl().setLookAt(BronzeGolem.this, 30.0F, 30.0F);
            }
        }

        @Override
        public void stop() {
            entityData.set(DATA_OFFERING, false);
            friend = null;
            ticks = 0;
        }
    }

    @SubscribeEvent
    public static void onEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(GolemEntityTypes.BRONZE_GOLEM.get(), createAttributes().build());
    }
}
