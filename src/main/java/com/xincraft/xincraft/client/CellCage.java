package com.xincraft.xincraft.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Holder;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.AbstractMinecartRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

// Draws the cell upgrade (entity.MobCell) as a low cage of iron bars standing on all four sides of a
// carriage or ferry. Each side is tiled with panels of vanilla's iron bars, stretched to fit evenly.
// The reinforced cage is an obsidian frame instead, with a glass window filling each side. A mob in
// the cell is shown by its spawn egg, spinning where it sits, rather than as itself (see CagedMobs);
// one without a spawn egg is drawn as usual, and stands out of the cage.
final class CellCage {
    // Iron bars and glass panes joined left and right, making a flat panel through the middle of the
    // block, one running along x and one along z
    private static final BlockState BARS_X = alongX(Blocks.IRON_BARS);
    private static final BlockState BARS_Z = alongZ(Blocks.IRON_BARS);
    private static final BlockState GLASS_X = alongX(Blocks.GLASS_PANE);
    private static final BlockState GLASS_Z = alongZ(Blocks.GLASS_PANE);
    // Roughly how big each panel is, before it's stretched to fit; and how deep it's drawn
    private static final float PANEL = 0.6F;
    private static final float DEPTH = 0.5F;
    // How thick the reinforced cage's obsidian posts and rails are, and how far (two pixels) the frame
    // rises above the glass, so the top of the glass doesn't flicker against the top rail
    private static final float FRAME = 0.1F;
    private static final float FRAME_RISE = 2.0F / 16.0F;
    // A caged mob's spawn egg: how high above the mob's feet it's drawn, how big, and how many
    // degrees it turns each tick
    private static final float EGG_HEIGHT = 0.5F;
    private static final float EGG_SCALE = 2.0F;
    private static final float EGG_SPIN = 3.0F;
    // Each mob type's spawn egg, looked up once. Vanilla finds it by searching the items for one that
    // spawns the type, so mobs added later, and other mods' mobs, find theirs too. The registry is
    // fixed once the game's loaded.
    private static final Map<EntityType<?>, Optional<Holder<Item>>> EGGS = new ConcurrentHashMap<>();

    // A renderer's models of the cage's parts, kept in its render state
    static final class Models {
        final BlockModelRenderState barsX = new BlockModelRenderState();
        final BlockModelRenderState barsZ = new BlockModelRenderState();
        final BlockModelRenderState glassX = new BlockModelRenderState();
        final BlockModelRenderState glassZ = new BlockModelRenderState();
        final BlockModelRenderState obsidian = new BlockModelRenderState();
        boolean shown;
        boolean reinforced;
        // The spawn eggs of the mobs inside, and where each is drawn, relative to the vehicle
        final List<ItemStackRenderState> eggs = new ArrayList<>();
        final List<Vec3> eggSpots = new ArrayList<>();
        int eggCount;
        float spin;
    }

    static void extract(BlockModelResolver resolver, Models models, boolean shown, boolean reinforced) {
        models.shown = shown;
        models.reinforced = shown && reinforced;
        if (models.reinforced) {
            resolver.update(models.glassX, GLASS_X, AbstractMinecartRenderer.BLOCK_DISPLAY_CONTEXT);
            resolver.update(models.glassZ, GLASS_Z, AbstractMinecartRenderer.BLOCK_DISPLAY_CONTEXT);
            resolver.update(models.obsidian, Blocks.OBSIDIAN.defaultBlockState(), AbstractMinecartRenderer.BLOCK_DISPLAY_CONTEXT);
        } else if (shown) {
            resolver.update(models.barsX, BARS_X, AbstractMinecartRenderer.BLOCK_DISPLAY_CONTEXT);
            resolver.update(models.barsZ, BARS_Z, AbstractMinecartRenderer.BLOCK_DISPLAY_CONTEXT);
        }
    }

    private static BlockState alongX(Block block) {
        return block.defaultBlockState().setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true);
    }

    private static BlockState alongZ(Block block) {
        return block.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true);
    }

    // The spawn eggs of the mobs in the cage, standing in for the mobs, which are hidden; origin is
    // where the vehicle is drawn
    static void extractEggs(ItemModelResolver resolver, Models models, Entity vehicle, Vec3 origin, float partialTicks) {
        models.eggCount = 0;
        if (!models.shown) {
            return;
        }
        for (Entity passenger : vehicle.getPassengers()) {
            Optional<Holder<Item>> egg = passenger instanceof Mob ? EGGS.computeIfAbsent(passenger.getType(), SpawnEggItem::byId) : Optional.empty();
            if (egg.isEmpty()) {
                continue;
            }
            CagedMobs.hide(passenger, partialTicks);
            int i = models.eggCount++;
            if (models.eggs.size() <= i) {
                models.eggs.add(new ItemStackRenderState());
                models.eggSpots.add(Vec3.ZERO);
            }
            resolver.updateForTopItem(models.eggs.get(i), new ItemStack(egg.get()), ItemDisplayContext.GROUND, vehicle.level(), null,
                passenger.getId());
            Vec3 at = new Vec3(Mth.lerp(partialTicks, passenger.xOld, passenger.getX()),
                Mth.lerp(partialTicks, passenger.yOld, passenger.getY()), Mth.lerp(partialTicks, passenger.zOld, passenger.getZ()));
            models.eggSpots.set(i, at.subtract(origin));
        }
        models.spin = (vehicle.tickCount + partialTicks) * EGG_SPIN;
    }

    // Draws the spawn eggs, from where the vehicle is drawn
    static void submitEggs(Models models, PoseStack poseStack, SubmitNodeCollector collector, int light, int outlineColor) {
        for (int i = 0; i < models.eggCount; i++) {
            Vec3 at = models.eggSpots.get(i);
            poseStack.pushPose();
            poseStack.translate(at.x, at.y + EGG_HEIGHT, at.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(models.spin));
            poseStack.scale(EGG_SCALE, EGG_SCALE, EGG_SCALE);
            models.eggs.get(i).submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, outlineColor);
            poseStack.popPose();
        }
    }

    // The cage's walls run along the edges of a box halfX by halfZ either side of the origin, from
    // bottom up to bottom + height, in the vehicle's own frame (y up)
    static void submit(Models models, float halfX, float halfZ, float bottom, float height, PoseStack poseStack,
            SubmitNodeCollector collector, int light, int outlineColor) {
        if (!models.shown) {
            return;
        }
        if (models.reinforced) {
            submitReinforced(models, halfX, halfZ, bottom, height, poseStack, collector, light, outlineColor);
            return;
        }
        int rows = Math.max(1, Math.round(height / PANEL));
        float rowHeight = height / rows;
        int alongX = Math.max(1, Math.round(2.0F * halfX / PANEL));
        int alongZ = Math.max(1, Math.round(2.0F * halfZ / PANEL));
        float width = 2.0F * halfX / alongX;
        float depth = 2.0F * halfZ / alongZ;
        for (int row = 0; row < rows; row++) {
            float y = bottom + row * rowHeight;
            for (int i = 0; i < alongX; i++) {
                float x = -halfX + i * width;
                for (float side : new float[] {-halfZ, halfZ}) {
                    panel(models.barsX, x, y, side - DEPTH / 2.0F, width, rowHeight, DEPTH, poseStack, collector, light, outlineColor);
                }
            }
            for (int i = 0; i < alongZ; i++) {
                float z = -halfZ + i * depth;
                for (float side : new float[] {-halfX, halfX}) {
                    panel(models.barsZ, side - DEPTH / 2.0F, y, z, DEPTH, rowHeight, depth, poseStack, collector, light, outlineColor);
                }
            }
        }
    }

    // The reinforced cage: a glass window filling each side, framed in obsidian, with a post at each
    // corner and a rail along the top and bottom of each side
    private static void submitReinforced(Models models, float halfX, float halfZ, float bottom, float height,
            PoseStack poseStack, SubmitNodeCollector collector, int light, int outlineColor) {
        for (float side : new float[] {-halfZ, halfZ}) {
            panel(models.glassX, -halfX, bottom, side - DEPTH / 2.0F, 2.0F * halfX, height, DEPTH, poseStack, collector, light, outlineColor);
        }
        for (float side : new float[] {-halfX, halfX}) {
            panel(models.glassZ, side - DEPTH / 2.0F, bottom, -halfZ, DEPTH, height, 2.0F * halfZ, poseStack, collector, light, outlineColor);
        }
        float half = FRAME / 2.0F;
        float frameHeight = height + FRAME_RISE;
        for (float x : new float[] {-halfX, halfX}) {
            for (float z : new float[] {-halfZ, halfZ}) {
                panel(models.obsidian, x - half, bottom, z - half, FRAME, frameHeight, FRAME, poseStack, collector, light, outlineColor);
            }
        }
        // The bottom rails along the foot of the glass; the top rails over its top edge and up past it
        float[][] rails = {{bottom, FRAME}, {bottom + height - FRAME, FRAME + FRAME_RISE}};
        for (float[] rail : rails) {
            float y = rail[0];
            float thickness = rail[1];
            for (float z : new float[] {-halfZ, halfZ}) {
                panel(models.obsidian, -halfX, y, z - half, 2.0F * halfX, thickness, FRAME, poseStack, collector, light, outlineColor);
            }
            for (float x : new float[] {-halfX, halfX}) {
                panel(models.obsidian, x - half, y, -halfZ, FRAME, thickness, 2.0F * halfZ, poseStack, collector, light, outlineColor);
            }
        }
    }

    private static void panel(BlockModelRenderState model, float x, float y, float z, float sizeX, float sizeY, float sizeZ,
            PoseStack poseStack, SubmitNodeCollector collector, int light, int outlineColor) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.scale(sizeX, sizeY, sizeZ);
        model.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, outlineColor);
        poseStack.popPose();
    }

    private CellCage() {}
}
