package com.xincraft.xincraft.transport.power;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import com.xincraft.xincraft.transport.train.LinkedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * @brief A charging station or substation: a small energy store linked to others into a network.
 *
 * Charging rails draw from the nodes in range of them, and what a node doesn't hold it finds
 * elsewhere on its network. Only nodes in loaded chunks take part, so a network with several
 * stations keeps working when some of it is out of range of the player.
 *
 * @see inRange
 */
public abstract class PowerNodeBlockEntity extends LinkedBlockEntity {
    public static final int MAX_LINKS = 4;
    public static final double LINK_RANGE = 64.0;
    private static final int MAX_NETWORK_SIZE = 256;
    private static final int NETWORK_REFRESH_TICKS = 20;
    private static final int SYNC_TICKS = 100;
    /**
     * @brief Per level, and so per thread: in single player the client and server run side by side.
     */
    private static final Map<Level, Set<PowerNodeBlockEntity>> LOADED = Collections.synchronizedMap(new WeakHashMap<>());

    private final int capacity;
    private int energy;
    private List<PowerNodeBlockEntity> network = List.of();
    private long networkExpiresAt;
    private boolean energyChanged;
    private long nextSync;

    protected PowerNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state);
        this.capacity = capacity;
    }

    /**
     * @brief The loaded nodes within chunkRange chunks of pos (counting chunks on each axis),
     *        nearest first.
     *
     * @param level The level to work in.
     * @param pos The block's position.
     * @param chunkRange How many chunks away to look, on each axis.
     * @return The nodes found, nearest first.
     */
    public static List<PowerNodeBlockEntity> inRange(Level level, BlockPos pos, int chunkRange) {
        Set<PowerNodeBlockEntity> loaded = LOADED.get(level);
        if (loaded == null) {
            return List.of();
        }
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        return loaded.stream()
            .filter(node -> Math.abs(SectionPos.blockToSectionCoord(node.worldPosition.getX()) - chunkX) <= chunkRange
                && Math.abs(SectionPos.blockToSectionCoord(node.worldPosition.getZ()) - chunkZ) <= chunkRange)
            .sorted(Comparator.comparingDouble(node -> node.worldPosition.distSqr(pos)))
            .toList();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) {
            LOADED.computeIfAbsent(level, key -> new LinkedHashSet<>()).add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        Set<PowerNodeBlockEntity> loaded = level == null ? null : LOADED.get(level);
        if (loaded != null) {
            loaded.remove(this);
        }
    }

    @Override
    public int maxLinks() {
        return MAX_LINKS;
    }

    @Override
    public double linkRange() {
        return LINK_RANGE;
    }

    @Override
    public boolean accepts(LinkedBlockEntity other) {
        return other instanceof PowerNodeBlockEntity;
    }

    @Override
    protected String messageKey() {
        return "network";
    }

    public int energy() {
        return energy;
    }

    public int capacity() {
        return capacity;
    }

    /**
     * @brief Adds energy, up to what fits.
     *
     * @param amount The most to add.
     * @return How much it added.
     */
    protected int fill(int amount) {
        int added = Math.max(0, Math.min(amount, capacity - energy));
        if (added > 0) {
            energy += added;
            energyChanged = true;
            setChanged();
        }
        return added;
    }

    /**
     * @brief Takes energy, up to what's held.
     *
     * @param amount The most to take.
     * @return How much it took.
     */
    protected int drain(int amount) {
        int taken = Math.max(0, Math.min(amount, energy));
        if (taken > 0) {
            energy -= taken;
            energyChanged = true;
            setChanged();
        }
        return taken;
    }

    /**
     * @brief Lets clients know a changed amount, for rose-tinted glasses, at most once every 5
     *        seconds.
     *
     * @param level The level to work in.
     */
    protected void serverTick(Level level) {
        long now = level.getGameTime();
        if (energyChanged && now >= nextSync) {
            energyChanged = false;
            nextSync = now + SYNC_TICKS;
            changed();
        }
    }

    /**
     * @brief Whether power enters the network here, which only a charging station with something
     *        connected does.
     *
     * @return True if Power enters the network here, which only a charging station with something
     *     connected does.
     */
    public boolean hasSource() {
        return false;
    }

    /**
     * @brief Whether anything on this node's network has a power source connected.
     *
     * @return True if Anything on this node's network has a power source connected.
     */
    public boolean networkHasSource() {
        return network().stream().anyMatch(node -> !node.isRemoved() && node.hasSource());
    }

    /**
     * @brief What this node's network holds between its nodes.
     *
     * @return The energy held.
     * @see networkCapacity
     */
    public int networkEnergy() {
        return network().stream().filter(node -> !node.isRemoved()).mapToInt(PowerNodeBlockEntity::energy).sum();
    }

    public int networkCapacity() {
        return network().stream().filter(node -> !node.isRemoved()).mapToInt(PowerNodeBlockEntity::capacity).sum();
    }

    /**
     * @brief Takes energy from this node, then the rest of its network.
     *
     * @param amount The most to take.
     * @return How much it took.
     */
    public int drawFromNetwork(int amount) {
        int drawn = 0;
        for (PowerNodeBlockEntity node : network()) {
            if (drawn >= amount) {
                break;
            }
            if (!node.isRemoved()) {
                drawn += node.drain(amount - drawn);
            }
        }
        return drawn;
    }

    /**
     * @brief This node and the loaded nodes linked to it, nearest in links first.
     *
     * Rebuilt about once a second, so a change to the links takes up to that long to count.
     *
     * @return The network's nodes.
     */
    protected List<PowerNodeBlockEntity> network() {
        if (level == null) {
            return List.of(this);
        }
        long now = level.getGameTime();
        if (network.isEmpty() || now >= networkExpiresAt) {
            network = findNetwork();
            networkExpiresAt = now + NETWORK_REFRESH_TICKS;
        }
        return network;
    }

    private List<PowerNodeBlockEntity> findNetwork() {
        List<PowerNodeBlockEntity> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<PowerNodeBlockEntity> queue = new ArrayDeque<>();
        seen.add(worldPosition);
        queue.add(this);
        while (!queue.isEmpty() && found.size() < MAX_NETWORK_SIZE) {
            PowerNodeBlockEntity node = queue.poll();
            found.add(node);
            for (BlockPos pos : node.links()) {
                if (seen.add(pos) && linkedAt(level, pos) instanceof PowerNodeBlockEntity next) {
                    queue.add(next);
                }
            }
        }
        return found;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy = Math.clamp(input.getIntOr("Energy", 0), 0, capacity);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Energy", energy);
    }
}
