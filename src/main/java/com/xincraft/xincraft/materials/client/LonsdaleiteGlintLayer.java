package com.xincraft.xincraft.materials.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.materials.MaterialTags;
import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.armorstand.ArmorStandArmorModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// The red shimmer on worn lonsdaleite armor (materials.LonsdaleiteArmorItem): vanilla's armor glint, drawn
// the same way over the same models, with a red texture. Vanilla has no hook for the glint's colour,
// so its own is off for these items and this layer draws theirs, on players, mannequins and armor
// stands.
@Mod.EventBusSubscriber(modid = XincraftMod.MODID, value = Dist.CLIENT)
public final class LonsdaleiteGlintLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>, A extends HumanoidModel<S>>
        extends RenderLayer<S, M> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(XincraftMod.MODID, "textures/misc/lonsdaleite_glint.png");
    private static final RenderType GLINT = RenderType.create("xincraft_lonsdaleite_glint",
        RenderSetup.builder(RenderPipelines.GLINT)
            .withTexture("Sampler0", TEXTURE)
            .setTextureTransform(TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING)
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .createRenderSetup());
    // Drawn after the armor and any trim on it, whose orders start at 1
    private static final int ORDER = 8;

    private final ArmorModelSet<A> models;
    private final ArmorModelSet<A> babyModels;

    private LonsdaleiteGlintLayer(RenderLayerParent<S, M> parent, ArmorModelSet<A> models, ArmorModelSet<A> babyModels) {
        super(parent);
        this.models = models;
        this.babyModels = babyModels;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
        submitPiece(poseStack, collector, state.chestEquipment, EquipmentSlot.CHEST, light, state);
        submitPiece(poseStack, collector, state.legsEquipment, EquipmentSlot.LEGS, light, state);
        submitPiece(poseStack, collector, state.feetEquipment, EquipmentSlot.FEET, light, state);
        submitPiece(poseStack, collector, state.headEquipment, EquipmentSlot.HEAD, light, state);
    }

    private void submitPiece(PoseStack poseStack, SubmitNodeCollector collector, ItemStack stack, EquipmentSlot slot, int light, S state) {
        if (stack.is(MaterialTags.LONSDALEITE_ARMOR)) {
            A model = (state.isBaby && state.entityType != EntityTypes.ARMOR_STAND ? babyModels : models).get(slot);
            collector.order(ORDER).submitModel(model, state, poseStack, GLINT, light, OverlayTexture.NO_OVERLAY, -1, null, 0, null);
        }
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerModelType type : event.getModelTypes()) {
            boolean slim = type == PlayerModelType.SLIM;
            AvatarRenderer<AbstractClientPlayer> player = event.getPlayerRenderer(type);
            AvatarRenderer<ClientMannequin> mannequin = event.getMannequinRenderer(type);
            ArmorModelSet<PlayerModel> armor = ArmorModelSet.bake(slim ? ModelLayers.PLAYER_SLIM_ARMOR : ModelLayers.PLAYER_ARMOR,
                event.getEntityModels(), part -> new PlayerModel(part, slim));
            if (player != null) {
                player.addLayer(new LonsdaleiteGlintLayer<>(player, armor, armor));
            }
            if (mannequin != null) {
                mannequin.addLayer(new LonsdaleiteGlintLayer<>(mannequin, armor, armor));
            }
        }
        ArmorStandRenderer stand = event.getEntityRenderer(EntityTypes.ARMOR_STAND);
        if (stand != null) {
            stand.addLayer(new LonsdaleiteGlintLayer<>(stand,
                ArmorModelSet.bake(ModelLayers.ARMOR_STAND_ARMOR, event.getEntityModels(), ArmorStandArmorModel::new),
                ArmorModelSet.bake(ModelLayers.ARMOR_STAND_SMALL_ARMOR, event.getEntityModels(), ArmorStandArmorModel::new)));
        }
    }
}
