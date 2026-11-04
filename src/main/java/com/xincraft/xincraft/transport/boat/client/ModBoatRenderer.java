package com.xincraft.xincraft.transport.boat.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.transport.boat.Barge;
import com.xincraft.xincraft.transport.train.CartUpgrade;
import com.xincraft.xincraft.transport.boat.Ferry;
import com.xincraft.xincraft.transport.boat.Motorboat;
import com.xincraft.xincraft.transport.boat.MotorboatType;
import com.xincraft.xincraft.transport.boat.PartyBoat;
import com.xincraft.xincraft.transport.train.UpgradableCart;
import com.xincraft.xincraft.transport.TransportEntityTypes;
import com.xincraft.xincraft.transport.TransportItems;
import com.xincraft.xincraft.transport.train.client.CellCage;
import com.xincraft.xincraft.transport.train.client.LinkingBar;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.client.renderer.entity.AbstractMinecartRenderer;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RaftRenderer;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jspecify.annotations.Nullable;

/**
 * @brief Draws the mod's boats as the vanilla boat or raft each is built on, using vanilla's own
 *        renderer.
 *
 * It adds their upgrades as small blocks along the gunwales, the linking bar to a boat they tow,
 * and on a motorboat, a propeller at the stern. The party boat is an oak motorboat with a jukebox
 * in its back seat, the barge an oak boat carrying a shulker box, and the ferry a bamboo raft.
 *
 * @see transport.train.client.LinkingBar
 * @see transport.train.client.CellCage
 */
@Mod.EventBusSubscriber(modid = XincraftMod.MODID, value = Dist.CLIENT)
public class ModBoatRenderer<T extends AbstractBoat & UpgradableCart> extends EntityRenderer<T, ModBoatRenderer.State> {
    /** @brief Vanilla's model for each kind of boat. */
    private static final Map<MotorboatType, ModelLayerLocation> MODELS = Map.of(
        MotorboatType.OAK, ModelLayers.OAK_BOAT,
        MotorboatType.SPRUCE, ModelLayers.SPRUCE_BOAT,
        MotorboatType.BIRCH, ModelLayers.BIRCH_BOAT,
        MotorboatType.JUNGLE, ModelLayers.JUNGLE_BOAT,
        MotorboatType.ACACIA, ModelLayers.ACACIA_BOAT,
        MotorboatType.CHERRY, ModelLayers.CHERRY_BOAT,
        MotorboatType.DARK_OAK, ModelLayers.DARK_OAK_BOAT,
        MotorboatType.PALE_OAK, ModelLayers.PALE_OAK_BOAT,
        MotorboatType.MANGROVE, ModelLayers.MANGROVE_BOAT,
        MotorboatType.BAMBOO, ModelLayers.BAMBOO_RAFT);
    /** @brief Degrees the propeller turns each tick while cruising. */
    private static final float PROPELLER_SPEED = 45.0F;
    private static final float PROPELLER_SCALE = 0.5F;
    private static final float UPGRADE_SCALE = 0.25F;
    /** @brief How many sections of the advanced antenna rise above the RC mast. */
    private static final int ANTENNA_SECTIONS = 2;
    /** @brief How much narrower each antenna section is than the one below. */
    private static final float ANTENNA_TAPER = 0.05F;
    private static final float JUKEBOX_SCALE = 0.5F;
    private static final float SHULKER_BOX_SCALE = 0.75F;
    /** @brief Half-width of the cell upgrade's cage around the ferry. */
    private static final float CAGE_HALF_WIDTH = 0.625F;
    /** @brief Half-length of the cell upgrade's cage around the ferry. */
    private static final float CAGE_HALF_LENGTH = 0.875F;
    /** @brief Where the cage stands, at the ferry's deck. */
    private static final float CAGE_BOTTOM = -0.2F;
    /** @brief How tall the cage is, up past the spawn egg shown for a mob sitting on the deck. */
    private static final float CAGE_HEIGHT = 1.2F;

    private final AbstractBoatRenderer boat;
    private final ItemModelResolver itemModelResolver;
    private final BlockModelResolver blockModelResolver;

    public ModBoatRenderer(EntityRendererProvider.Context context, MotorboatType kind) {
        super(context);
        this.boat = kind.raft() ? new RaftRenderer(context, MODELS.get(kind)) : new BoatRenderer(context, MODELS.get(kind));
        this.itemModelResolver = context.getItemModelResolver();
        this.blockModelResolver = context.getBlockModelResolver();
        this.shadowRadius = 0.8F;
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (MotorboatType kind : MotorboatType.values()) {
            event.registerEntityRenderer(TransportEntityTypes.MOTORBOATS.get(kind).get(), context -> new ModBoatRenderer<>(context, kind));
        }
        event.registerEntityRenderer(TransportEntityTypes.PARTY_BOAT.get(), context -> new ModBoatRenderer<PartyBoat>(context, MotorboatType.OAK));
        event.registerEntityRenderer(TransportEntityTypes.BARGE.get(), context -> new ModBoatRenderer<Barge>(context, MotorboatType.OAK));
        event.registerEntityRenderer(TransportEntityTypes.FERRY.get(), context -> new ModBoatRenderer<Ferry>(context, MotorboatType.BAMBOO));
    }

    /** @brief Vanilla's boat state plus the propeller, upgrades, cargo and linking bar. */
    public static class State extends BoatRenderState {
        public final ItemStackRenderState propeller = new ItemStackRenderState();
        public boolean hasPropeller;
        public float propellerAngle;
        public final List<BlockModelRenderState> upgradeModels = new ArrayList<>();
        public int upgradeCount;
        /** @brief Which of the upgrades is the RC mast the advanced antenna extends, or -1. */
        public int antennaMast = -1;
        /** @brief The block it carries: the party boat's jukebox or the barge's shulker box. */
        public final BlockModelRenderState cargo = new BlockModelRenderState();
        /** @brief Whether it carries a block. */
        public boolean hasCargo;
        /** @brief How big the carried block is drawn. */
        public float cargoScale;
        /** @brief Where the carried block's corner is, in the boat's frame. */
        public Vec3 cargoCorner = Vec3.ZERO;
        public final CellCage.Models cage = new CellCage.Models();
        public LinkingBar.@Nullable Link link;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T entity, State state, float partialTicks) {
        boat.extractRenderState(entity, state, partialTicks);
        state.hasPropeller = entity instanceof Motorboat;
        if (entity instanceof Motorboat motorboat) {
            itemModelResolver.updateForTopItem(state.propeller, new ItemStack(TransportItems.PROPELLER.get()), ItemDisplayContext.FIXED,
                entity.level(), null, entity.getId());
            state.propellerAngle = motorboat.isCruising() && !motorboat.isWaiting() ? (entity.tickCount + partialTicks) * PROPELLER_SPEED : 0.0F;
        }
        // The antenna is drawn as the RC mast extended, and the cell as the cage, not as blocks
        List<CartUpgrade> installed = entity.upgrades().installed().stream()
            .filter(upgrade -> upgrade != CartUpgrade.ANTENNA && upgrade != CartUpgrade.CELL && upgrade != CartUpgrade.REINFORCED_CAGE).toList();
        CellCage.extract(blockModelResolver, state.cage, entity.upgrades().has(CartUpgrade.CELL),
            entity.upgrades().has(CartUpgrade.REINFORCED_CAGE));
        state.upgradeCount = installed.size();
        state.antennaMast = entity.upgrades().has(CartUpgrade.ANTENNA) ? installed.indexOf(CartUpgrade.RC) : -1;
        for (int i = 0; i < installed.size(); i++) {
            if (state.upgradeModels.size() <= i) {
                state.upgradeModels.add(new BlockModelRenderState());
            }
            blockModelResolver.update(state.upgradeModels.get(i), installed.get(i).block().defaultBlockState(),
                AbstractMinecartRenderer.BLOCK_DISPLAY_CONTEXT);
        }
        state.hasCargo = true;
        if (entity instanceof PartyBoat party) {
            blockModelResolver.update(state.cargo,
                Blocks.JUKEBOX.defaultBlockState().setValue(JukeboxBlock.HAS_RECORD, !party.jukebox().getDisc().isEmpty()),
                AbstractMinecartRenderer.BLOCK_DISPLAY_CONTEXT);
            state.cargoScale = JUKEBOX_SCALE;
            state.cargoCorner = new Vec3(-JUKEBOX_SCALE / 2.0F, -0.3F, 0.35F);
        } else if (entity instanceof Barge) {
            blockModelResolver.update(state.cargo, Blocks.SHULKER_BOX.defaultBlockState(), AbstractMinecartRenderer.BLOCK_DISPLAY_CONTEXT);
            state.cargoScale = SHULKER_BOX_SCALE;
            state.cargoCorner = new Vec3(-SHULKER_BOX_SCALE / 2.0F, -0.3F, -SHULKER_BOX_SCALE / 2.0F);
        } else {
            state.hasCargo = false;
        }
        state.link = LinkingBar.extract(entity, new Vec3(state.x, state.y, state.z), partialTicks);
        CellCage.extractEggs(itemModelResolver, state.cage, entity, new Vec3(state.x, state.y, state.z), partialTicks);
    }

    @Override
    protected AABB getBoundingBoxForCulling(T entity) {
        return LinkingBar.cullingBox(entity, super.getBoundingBoxForCulling(entity));
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.link != null) {
            LinkingBar.submit(state.link, state.lightCoords, poseStack, collector);
        }
        CellCage.submitEggs(state.cage, poseStack, collector, state.lightCoords, state.outlineColor, camera);
        boat.submit(state, poseStack, collector, camera);
        // In the boat's own frame (as vanilla turns it), where +Z is the stern and -Z the bow
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.375F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));

        if (state.hasPropeller) {
            poseStack.pushPose();
            poseStack.translate(0.0F, -0.15F, 0.95F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(state.propellerAngle));
            poseStack.scale(PROPELLER_SCALE, PROPELLER_SCALE, PROPELLER_SCALE);
            state.propeller.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }

        CellCage.submit(state.cage, CAGE_HALF_WIDTH, CAGE_HALF_LENGTH, CAGE_BOTTOM, CAGE_HEIGHT, poseStack, collector,
            state.lightCoords, state.outlineColor);

        if (state.hasCargo) {
            poseStack.pushPose();
            poseStack.translate(state.cargoCorner.x, state.cargoCorner.y, state.cargoCorner.z);
            poseStack.scale(state.cargoScale, state.cargoScale, state.cargoScale);
            state.cargo.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }

        // Upgrades sit on the gunwales, alternating sides, working back from the bow
        for (int i = 0; i < state.upgradeCount; i++) {
            poseStack.pushPose();
            float side = i % 2 == 0 ? 0.55F : -0.55F;
            poseStack.translate(side - UPGRADE_SCALE / 2.0F, 0.0F, -0.45F + (i / 2) * 0.35F - UPGRADE_SCALE / 2.0F);
            poseStack.scale(UPGRADE_SCALE, UPGRADE_SCALE, UPGRADE_SCALE);
            state.upgradeModels.get(i).submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            if (i == state.antennaMast) {
                // The same mast again for each section, stacked and narrowing (in the mast's own frame,
                // a block tall and wide)
                for (int section = 1; section <= ANTENNA_SECTIONS; section++) {
                    float width = 1.0F - section * ANTENNA_TAPER / UPGRADE_SCALE;
                    poseStack.pushPose();
                    poseStack.translate((1.0F - width) / 2.0F, section, (1.0F - width) / 2.0F);
                    poseStack.scale(width, 1.0F, width);
                    state.upgradeModels.get(i).submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
                    poseStack.popPose();
                }
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }
}
