package com.xincraft.xincraft.transport.power.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.common.RoseTintedGlassesItem;
import com.xincraft.xincraft.common.client.CrosshairReadout;
import com.xincraft.xincraft.common.client.CrosshairReadout.Line;
import com.xincraft.xincraft.transport.power.PowerNodeBlockEntity;
import com.xincraft.xincraft.transport.rail.ChargingRailBlockEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jspecify.annotations.Nullable;

// While the player wears or holds rose-tinted glasses, the links of the power networks around them are
// traced in dust: cyan where a network has energy, red where it's run dry. Looking at a charging
// station or substation shows what it holds and what its network holds. Looking at a charging rail
// shows what it holds and traces a line to the station or substation it's drawing from, or warns that
// there isn't one. Amounts are synced from the server at most once every 5 seconds, and the client only knows
// the parts of a network within its view distance.
@Mod.EventBusSubscriber(modid = XincraftMod.MODID, value = Dist.CLIENT)
public final class PowerNetworkView {
    private static final Identifier LAYER = Identifier.fromNamespaceAndPath(XincraftMod.MODID, "power_readout");
    // How many chunks around the player networks are traced: at least the 48 blocks buoys are
    private static final int CHUNK_RANGE = 3;
    private static final int RESCAN_TICKS = 10;
    // Dust specks put along each link each tick, per 8 blocks of its length
    private static final double BLOCKS_PER_SPECK = 8.0;
    private static final DustParticleOptions LIVE = new DustParticleOptions(0x4AD8FF, 0.9F);
    private static final DustParticleOptions DEAD = new DustParticleOptions(0xFF2A1A, 0.9F);
    private static final int HEADER_COLOR = 0xFFFFFFFF;
    private static final int LIVE_COLOR = 0xFF7FE3FF;
    private static final int DEAD_COLOR = 0xFFFF6A5A;

    private record Trace(Vec3 from, Vec3 to, boolean live) {}

    private static List<Trace> links = List.of();
    private static @Nullable Trace railTrace;
    private static List<Line> lines = List.of();
    private static int rescanIn;

    @SubscribeEvent
    public static void onAddLayers(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().addAbove(ForgeLayeredDraw.PRE_SLEEP_STACK, LAYER, ForgeLayeredDraw.CROSSHAIR, PowerNetworkView::extract);
    }

    private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        CrosshairReadout.draw(graphics, lines);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null || !RoseTintedGlassesItem.isUsing(player)) {
            links = List.of();
            railTrace = null;
            lines = List.of();
            rescanIn = 0;
            return;
        }
        look(level, minecraft.hitResult instanceof BlockHitResult hit ? level.getBlockEntity(hit.getBlockPos()) : null);
        if (minecraft.isPaused()) {
            return;
        }
        if (--rescanIn <= 0) {
            rescanIn = RESCAN_TICKS;
            links = scan(level, player.blockPosition());
        }
        for (Trace trace : links) {
            spawn(level, trace);
        }
        if (railTrace != null) {
            spawn(level, railTrace);
        }
    }

    private static void spawn(Level level, Trace trace) {
        RandomSource random = level.getRandom();
        Vec3 span = trace.to().subtract(trace.from());
        double specks = span.length() / BLOCKS_PER_SPECK;
        // Whole specks, plus a chance of one more for the fraction left over
        int count = (int) specks + (random.nextDouble() < specks - (int) specks ? 1 : 0);
        for (int i = 0; i < count; i++) {
            Vec3 at = trace.from().add(span.scale(random.nextDouble()));
            level.addParticle(trace.live() ? LIVE : DEAD, at.x, at.y, at.z, 0.0, 0.0, 0.0);
        }
    }

    // The links between the nodes around center, each once
    private static List<Trace> scan(Level level, BlockPos center) {
        List<Trace> found = new ArrayList<>();
        List<PowerNodeBlockEntity> nearby = PowerNodeBlockEntity.inRange(level, center, CHUNK_RANGE);
        for (PowerNodeBlockEntity node : nearby) {
            boolean live = node.networkEnergy() > 0;
            for (BlockPos other : node.links()) {
                // From the lower position, unless the other end is too far away to draw it from there
                if (node.getBlockPos().compareTo(other) < 0
                        || !(level.getBlockEntity(other) instanceof PowerNodeBlockEntity near && nearby.contains(near))) {
                    found.add(new Trace(top(node.getBlockPos()), top(other), live));
                }
            }
        }
        return found;
    }

    private static void look(Level level, @Nullable BlockEntity target) {
        railTrace = null;
        if (target instanceof PowerNodeBlockEntity node) {
            lines = List.of(
                new Line(Component.translatable("hud.xincraft.power.held", node.getBlockState().getBlock().getName(),
                    number(node.energy()), number(node.capacity())), HEADER_COLOR),
                new Line(Component.translatable("hud.xincraft.power.network", number(node.networkEnergy()), number(node.networkCapacity())),
                    node.networkEnergy() > 0 ? LIVE_COLOR : DEAD_COLOR),
                new Line(Component.translatable("hud.xincraft.power.links", node.links().size(), node.maxLinks()), HEADER_COLOR));
        } else if (target instanceof ChargingRailBlockEntity rail) {
            lines = railLines(level, rail);
        } else {
            lines = List.of();
        }
    }

    private static List<Line> railLines(Level level, ChargingRailBlockEntity rail) {
        BlockPos pos = rail.getBlockPos();
        List<Line> found = new ArrayList<>();
        found.add(new Line(Component.translatable("hud.xincraft.power.held", rail.getBlockState().getBlock().getName(),
            number(rail.energy()), number(rail.capacity())), HEADER_COLOR));
        List<PowerNodeBlockEntity> nodes = PowerNodeBlockEntity.inRange(level, pos, rail.chunkRange());
        if (nodes.isEmpty()) {
            found.add(new Line(Component.translatable("hud.xincraft.power.none_in_range", rail.chunkRange()), DEAD_COLOR));
            return found;
        }
        // As the rail chooses: the nearest one whose network has energy
        PowerNodeBlockEntity source = nodes.stream().filter(node -> node.networkEnergy() > 0).findFirst().orElse(null);
        if (source == null) {
            found.add(new Line(Component.translatable("hud.xincraft.power.dry", rail.chunkRange()), DEAD_COLOR));
            railTrace = new Trace(Vec3.atCenterOf(pos), top(nodes.getFirst().getBlockPos()), false);
            return found;
        }
        int blocks = (int) Math.round(Math.sqrt(source.getBlockPos().distSqr(pos)));
        found.add(new Line(Component.translatable("hud.xincraft.power.drawing_from", source.getBlockState().getBlock().getName(), blocks), LIVE_COLOR));
        railTrace = new Trace(Vec3.atCenterOf(pos), top(source.getBlockPos()), true);
        return found;
    }

    // Where a link meets a node: the tip of its antenna
    private static Vec3 top(BlockPos pos) {
        return Vec3.atBottomCenterOf(pos).add(0.0, 1.0, 0.0);
    }

    private static String number(int amount) {
        return String.format(Locale.ROOT, "%,d", amount);
    }

    private PowerNetworkView() {}
}
