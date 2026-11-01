package com.xincraft.xincraft.client;

import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.entity.BronzeGolem;
import com.xincraft.xincraft.registry.ModEntityTypes;
import net.minecraft.client.model.animal.golem.CopperGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.CopperGolemRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Draws the bronze golem (entity.BronzeGolem) with vanilla's copper golem model and animations, in
// its own texture. What it holds shows in its hands: the sword, whatever it's carrying, and the copper
// nugget it offers iron golems, which isn't a real item in its hand.
@Mod.EventBusSubscriber(modid = XincraftMod.MODID, value = Dist.CLIENT)
public final class BronzeGolemRenderer extends MobRenderer<BronzeGolem, CopperGolemRenderState, CopperGolemModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(XincraftMod.MODID, "textures/entity/bronze_golem/bronze_golem.png");
    private static final Identifier EYES = Identifier.fromNamespaceAndPath(XincraftMod.MODID, "textures/entity/bronze_golem/bronze_golem_eyes.png");

    private BronzeGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new CopperGolemModel(context.bakeLayer(ModelLayers.COPPER_GOLEM)), 0.5F);
        addLayer(new LivingEntityEmissiveLayer<>(this, state -> EYES, (state, ageInTicks) -> 1.0F,
            new CopperGolemModel(context.bakeLayer(ModelLayers.COPPER_GOLEM)), RenderTypes::eyes, false));
        addLayer(new ItemInHandLayer<>(this));
    }

    @Override
    public Identifier getTextureLocation(CopperGolemRenderState state) {
        return TEXTURE;
    }

    @Override
    public CopperGolemRenderState createRenderState() {
        return new CopperGolemRenderState();
    }

    @Override
    public void extractRenderState(BronzeGolem golem, CopperGolemRenderState state, float partialTicks) {
        super.extractRenderState(golem, state, partialTicks);
        ArmedEntityRenderState.extractArmedEntityRenderState(golem, state, itemModelResolver, partialTicks);
        state.copperGolemState = golem.getGolemState();
        state.idleAnimationState.copyFrom(golem.getIdleAnimationState());
        state.interactionDropItem.copyFrom(golem.getDropItemAnimationState());
        if (golem.isOfferingNugget()) {
            itemModelResolver.updateForLiving(state.leftHandItemState, new ItemStack(Items.COPPER_NUGGET), ItemDisplayContext.THIRD_PERSON_LEFT_HAND, golem);
        }
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.BRONZE_GOLEM.get(), BronzeGolemRenderer::new);
    }
}
