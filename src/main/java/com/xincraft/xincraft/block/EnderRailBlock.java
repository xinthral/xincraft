package com.xincraft.xincraft.block;

import java.util.ArrayDeque;
import java.util.Set;
import com.mojang.serialization.MapCodec;
import com.xincraft.xincraft.Config;
import com.xincraft.xincraft.entity.EnderMinecart;
import com.xincraft.xincraft.item.EnderRailItem;
import com.xincraft.xincraft.registry.ModDataComponents;
import com.xincraft.xincraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// A straight rail that teleports ender minecarts (and their riders) to the ender rail it's linked
// to, keeping their speed; other minecarts only if the config allows it. To link: right-click a placed ender rail with an ender rail item to attune
// the item to it, then place that item anywhere in the same dimension.
public class EnderRailBlock extends BaseRailBlock implements EntityBlock {
    public static final MapCodec<EnderRailBlock> CODEC = simpleCodec(EnderRailBlock::new);
    public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE_STRAIGHT;
    public static final BooleanProperty LINKED = BooleanProperty.create("linked");
    // True for a moment after linking; drives the bright texture and light level
    public static final BooleanProperty GLOWING = BooleanProperty.create("glowing");

    private static final int GLOW_TICKS = 20;
    // A cart moving slower than this is treated as parked and isn't teleported
    private static final double MIN_TELEPORT_SPEED_SQR = 1.0E-4;
    // Minecart persistent-data keys recording the rail a cart was teleported onto, so it isn't
    // immediately sent back while it's still rolling off that rail
    private static final String ARRIVAL_POS = "xincraft:ender_rail_arrival_pos";
    private static final String ARRIVAL_TICK = "xincraft:ender_rail_arrival_tick";
    // Game time before which the cart can't be teleported again (see Config.enderRailCooldownTicks)
    private static final String COOLDOWN_UNTIL = "xincraft:ender_rail_cooldown_until";
    // Server tick of each teleport in the last second, oldest first (only touched on the server thread)
    private static final ArrayDeque<Integer> RECENT_TELEPORTS = new ArrayDeque<>();

    public EnderRailBlock(BlockBehaviour.Properties properties) {
        super(true, properties);
        registerDefaultState(stateDefinition.any()
            .setValue(SHAPE, RailShape.NORTH_SOUTH)
            .setValue(LINKED, false)
            .setValue(GLOWING, false)
            .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<EnderRailBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnderRailBlockEntity(pos, state);
    }

    // Right-clicking with an ender rail item attunes the item to this rail
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.ENDER_RAIL.get())) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        if (level instanceof ServerLevel serverLevel) {
            stack.set(ModDataComponents.ENDER_RAIL_LINK.get(), GlobalPos.of(level.dimension(), pos));
            EnderRailItem.flashGlint(stack, level);
            glow(serverLevel, pos);
            player.sendOverlayMessage(Component.translatable("message.xincraft.ender_rail.attuned"));
        }
        return InteractionResult.SUCCESS;
    }

    // Placing an attuned item links the new rail to the one it was attuned to
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        GlobalPos target = stack.get(ModDataComponents.ENDER_RAIL_LINK.get());
        if (target == null || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        // The rest of the stack goes back to being unattuned
        stack.remove(ModDataComponents.ENDER_RAIL_LINK.get());
        EnderRailItem.clearGlint(stack);

        Component failure = null;
        if (!target.dimension().equals(level.dimension())) {
            failure = Component.translatable("message.xincraft.ender_rail.other_dimension");
        } else if (!inRange(pos, target.pos())) {
            failure = Component.translatable("message.xincraft.ender_rail.too_far",
                (int) Math.sqrt(pos.distSqr(target.pos())), Config.enderRailMaxDistance);
        } else if (!(level.getBlockEntity(target.pos()) instanceof EnderRailBlockEntity)) {
            failure = Component.translatable("message.xincraft.ender_rail.target_missing");
        }
        if (failure != null) {
            if (by instanceof Player player) {
                player.sendOverlayMessage(failure);
            }
            return;
        }

        link(serverLevel, pos, target.pos());
        if (by instanceof Player player) {
            player.sendOverlayMessage(Component.translatable("message.xincraft.ender_rail.linked"));
        }
    }

    // Links a and b to each other, unlinking whatever either was previously linked to
    static void link(ServerLevel level, BlockPos a, BlockPos b) {
        for (BlockPos pos : new BlockPos[] {a, b}) {
            if (level.getBlockEntity(pos) instanceof EnderRailBlockEntity rail) {
                BlockPos old = rail.getLink();
                if (old != null && !old.equals(a) && !old.equals(b)
                        && level.getBlockEntity(old) instanceof EnderRailBlockEntity oldRail && pos.equals(oldRail.getLink())) {
                    unlink(level, old);
                }
            }
        }
        setLink(level, a, b);
        setLink(level, b, a);
        glow(level, a);
        glow(level, b);
    }

    static void unlink(ServerLevel level, BlockPos pos) {
        setLink(level, pos, null);
    }

    private static void setLink(ServerLevel level, BlockPos pos, @Nullable BlockPos link) {
        if (level.getBlockEntity(pos) instanceof EnderRailBlockEntity rail) {
            rail.setLink(link);
            BlockState state = level.getBlockState(pos);
            level.setBlock(pos, state.setValue(LINKED, link != null), Block.UPDATE_ALL);
        }
    }

    // A one-second enchanting glow: bright texture and light, enchantment particles and sound
    private static void glow(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof EnderRailBlock block)) {
            return;
        }
        level.setBlock(pos, state.setValue(GLOWING, true), Block.UPDATE_ALL);
        level.scheduleTick(pos, block, GLOW_TICKS);
        level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 40, 0.3, 0.3, 0.3, 0.5);
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.2F);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(GLOWING)) {
            level.setBlock(pos, state.setValue(GLOWING, false), Block.UPDATE_ALL);
        }
    }

    @Override
    public void onMinecartPass(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
        if (!(level instanceof ServerLevel serverLevel) || !(level.getBlockEntity(pos) instanceof EnderRailBlockEntity rail)
                || !(cart instanceof EnderMinecart || Config.enderRailAllowAnyMinecart)) {
            return;
        }

        // Carts are told about every tick they spend on a rail, so a gap means the cart left
        // and has come back, rather than still rolling off the rail it arrived on
        CompoundTag data = cart.getPersistentData();
        long now = level.getGameTime();
        if (data.contains(ARRIVAL_POS) && data.getLongOr(ARRIVAL_POS, 0) == pos.asLong()
                && now - data.getLongOr(ARRIVAL_TICK, 0) <= 2) {
            data.putLong(ARRIVAL_TICK, now);
            return;
        }
        data.remove(ARRIVAL_POS);
        data.remove(ARRIVAL_TICK);

        BlockPos target = rail.getLink();
        Vec3 velocity = cart.getDeltaMovement();
        // Links made before the range was lowered in the config stay, but stop working.
        // While its cooldown runs, a cart just rolls over ender rails like ordinary track.
        if (target == null || velocity.horizontalDistanceSqr() < MIN_TELEPORT_SPEED_SQR || !inRange(pos, target)
                || now < data.getLongOr(COOLDOWN_UNTIL, 0) || !serverHasTeleportBudget(serverLevel)) {
            return;
        }
        // Loads the destination chunk if needed; drop the link if the other end has gone
        BlockState targetState = level.getBlockState(target);
        if (!(targetState.getBlock() instanceof EnderRailBlock)
                || !(level.getBlockEntity(target) instanceof EnderRailBlockEntity targetRail)
                || !pos.equals(targetRail.getLink())) {
            unlink(serverLevel, pos);
            return;
        }

        // Keep the cart's heading; if the exit rail runs across it instead, turn right onto it
        RailShape exitShape = targetState.getValue(SHAPE);
        float turn = 0.0F;
        if (runsEastWest(state.getValue(SHAPE)) != runsEastWest(exitShape)) {
            velocity = new Vec3(-velocity.z, velocity.y, velocity.x);
            turn = 90.0F;
        }
        Vec3 destination = Vec3.atBottomCenterOf(target).add(0.0, exitShape.isSlope() ? 0.5 : 0.0, 0.0);

        RECENT_TELEPORTS.addLast(serverLevel.getServer().getTickCount());
        teleportEffects(serverLevel, cart.position());
        cart.teleport(new TeleportTransition(serverLevel, destination, velocity, cart.getYRot() + turn, cart.getXRot(),
            Set.of(), TeleportTransition.PLACE_PORTAL_TICKET));
        teleportEffects(serverLevel, destination);
        data.putLong(ARRIVAL_POS, target.asLong());
        data.putLong(ARRIVAL_TICK, now);
        data.putLong(COOLDOWN_UNTIL, now + Config.enderRailCooldownTicks);
    }

    // Whether fewer than Config.enderRailMaxTeleportsPerSecond teleports have happened anywhere
    // on the server in the last second (20 ticks)
    private static boolean serverHasTeleportBudget(ServerLevel level) {
        int limit = Config.enderRailMaxTeleportsPerSecond;
        if (limit <= 0) {
            return true;
        }
        int now = level.getServer().getTickCount();
        // The tick count restarts with each server (e.g. reopening a singleplayer world)
        if (!RECENT_TELEPORTS.isEmpty() && RECENT_TELEPORTS.peekLast() > now) {
            RECENT_TELEPORTS.clear();
        }
        while (!RECENT_TELEPORTS.isEmpty() && RECENT_TELEPORTS.peekFirst() <= now - 20) {
            RECENT_TELEPORTS.removeFirst();
        }
        return RECENT_TELEPORTS.size() < limit;
    }

    private static boolean inRange(BlockPos a, BlockPos b) {
        double max = Config.enderRailMaxDistance;
        return a.distSqr(b) <= max * max;
    }

    private static boolean runsEastWest(RailShape shape) {
        return shape == RailShape.EAST_WEST || shape == RailShape.ASCENDING_EAST || shape == RailShape.ASCENDING_WEST;
    }

    private static void teleportEffects(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.PORTAL, pos.x, pos.y + 0.5, pos.z, 32, 0.4, 0.4, 0.4, 0.2);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.8F, 1.0F);
    }

    @Override
    public Property<RailShape> getShapeProperty() {
        return SHAPE;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(SHAPE, rotate(state.getValue(SHAPE), rotation));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(SHAPE, mirror(state.getValue(SHAPE), mirror));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE, LINKED, GLOWING, WATERLOGGED);
    }
}
