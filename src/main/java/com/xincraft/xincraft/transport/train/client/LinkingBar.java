package com.xincraft.xincraft.transport.train.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.transport.train.LinkableTransport;
import com.xincraft.xincraft.transport.train.LinkedTransport;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * @brief Draws the linking bar from a vehicle to the one it tows, for both the minecart and boat
 *        renderers.
 *
 * A flat bronze drawbar spanning the gap between them, with an upright pin through each end where
 * it pivots.
 *
 * It always runs straight from one vehicle's centre toward the other's, so it swings with them
 * around corners.
 *
 * @see transport.train.LinkedTransport
 */
public final class LinkingBar {
    private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(
        Identifier.fromNamespaceAndPath(XincraftMod.MODID, "textures/entity/linking_bar.png"));
    /** @brief Height of the bar above a cart's position: just above its floor. */
    private static final double CART_HEIGHT = 0.25;
    /** @brief Height of the bar above a boat's position: just above its waterline. */
    private static final double BOAT_HEIGHT = 0.3;
    /** @brief How far inside each vehicle's end the bar starts. */
    private static final double BAR_INSET = 0.025;
    /** @brief Half the bar's width. */
    private static final float BAR_HALF_WIDTH = 3.0F / 32.0F;
    /** @brief Half the bar's thickness. */
    private static final float BAR_HALF_THICKNESS = 1.0F / 16.0F;
    /** @brief Half a pin's width. */
    private static final float PIN_HALF_WIDTH = 1.0F / 8.0F;
    /** @brief Half a pin's height. */
    private static final float PIN_HALF_HEIGHT = 5.0F / 32.0F;
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);

    /**
     * @brief A vehicle's bar to the one it tows, as a renderer keeps it.
     *
     * Both ends relative to the vehicle's position, and how far each end is from its vehicle's
     * centre to the end of its body.
     *
     * @param from Where the bar starts, relative to the vehicle's position.
     * @param to Where the bar ends, relative to the vehicle's position.
     * @param fromHalf How far the start is from its vehicle's centre to the end of its body.
     * @param toHalf How far the end is from its vehicle's centre to the end of its body.
     */
    public record Link(Vec3 from, Vec3 to, double fromHalf, double toHalf) {}

    /**
     * @brief The vehicle this one tows with a linking bar, as the client knows it.
     *
     * @param vehicle The vehicle.
     * @return The towed vehicle, or null if it tows none.
     */
    public static @Nullable Entity towed(Entity vehicle) {
        if (!(vehicle instanceof LinkableTransport linkable)) {
            return null;
        }
        int id = linkable.transport().towedId();
        return id == LinkedTransport.NONE ? null : vehicle.level().getEntity(id);
    }

    /**
     * @brief The bar from vehicle, drawn at origin, to the one it tows, or null if it tows none.
     *
     * @param vehicle The vehicle.
     * @param origin Where the vehicle is drawn.
     * @param partialTicks How far through the current tick this frame is.
     * @return The bar, or null if it tows none.
     */
    public static @Nullable Link extract(Entity vehicle, Vec3 origin, float partialTicks) {
        Entity towed = towed(vehicle);
        if (towed == null) {
            return null;
        }
        double height = vehicle instanceof AbstractBoat ? BOAT_HEIGHT : CART_HEIGHT;
        Vec3 from = bodyCentre(vehicle, origin).subtract(origin).add(0.0, height, 0.0);
        Vec3 to = bodyCentre(towed, towed.getPosition(partialTicks)).subtract(origin).add(0.0, height, 0.0);
        return new Link(from, to, LinkedTransport.halfLength(vehicle), LinkedTransport.halfLength(towed));
    }

    /**
     * @brief Keeps a vehicle drawn, and with it the bar, while the one it tows is in view.
     *
     * @param vehicle The vehicle.
     * @param box The vehicle's own culling box.
     * @return The box, stretched to take in the towed vehicle.
     */
    public static AABB cullingBox(Entity vehicle, AABB box) {
        return towed(vehicle) instanceof Entity towed ? box.minmax(towed.getBoundingBox()) : box;
    }

    /**
     * @brief Where a vehicle's body is drawn, as vanilla draws a cart on old-style track along the
     *        rail rather than at its exact position.
     *
     * @param vehicle The vehicle.
     * @param position The vehicle's position.
     * @return The body's centre.
     */
    private static Vec3 bodyCentre(Entity vehicle, Vec3 position) {
        if (vehicle instanceof AbstractMinecart cart && cart.getBehavior() instanceof OldMinecartBehavior behavior) {
            Vec3 onRail = behavior.getPos(position.x, position.y, position.z);
            if (onRail != null) {
                Vec3 front = behavior.getPosOffs(position.x, position.y, position.z, 0.3F);
                Vec3 back = behavior.getPosOffs(position.x, position.y, position.z, -0.3F);
                double frontY = front != null ? front.y : onRail.y;
                double backY = back != null ? back.y : onRail.y;
                return new Vec3(onRail.x, (frontY + backY) / 2.0, onRail.z);
            }
        }
        return position;
    }

    public static void submit(Link link, int light, PoseStack poseStack, SubmitNodeCollector collector) {
        Vec3 start = link.from();
        Vec3 end = link.to();
        Vec3 span = end.subtract(start);
        double length = span.length();
        double frontPivot = link.fromHalf() + LinkedTransport.PIVOT_OVERHANG;
        double backPivot = link.toHalf() + LinkedTransport.PIVOT_OVERHANG;
        if (length <= frontPivot + backPivot) {
            return;
        }
        // The bar's own axes: along it, up through it, and across it
        Vec3 along = span.scale(1.0 / length);
        Vec3 sideways = along.cross(UP);
        Vec3 across = sideways.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : sideways.normalize();
        Vec3 up = across.cross(along);
        Vec3 barStart = start.add(along.scale(link.fromHalf() - BAR_INSET));
        Vec3 barEnd = end.subtract(along.scale(link.toHalf() - BAR_INSET));
        float halfLength = (float) (barEnd.subtract(barStart).length() / 2.0);
        Vec3 frontPin = start.add(along.scale(frontPivot));
        Vec3 backPin = end.subtract(along.scale(backPivot));
        collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, consumer) -> {
            Cuboid bar = new Cuboid(pose, consumer, light, along, up, across);
            bar.draw(barStart.add(barEnd).scale(0.5), halfLength, BAR_HALF_THICKNESS, BAR_HALF_WIDTH, 0.0F, 0.5F);
            bar.draw(frontPin, PIN_HALF_WIDTH, PIN_HALF_HEIGHT, PIN_HALF_WIDTH, 0.5F, 1.0F);
            bar.draw(backPin, PIN_HALF_WIDTH, PIN_HALF_HEIGHT, PIN_HALF_WIDTH, 0.5F, 1.0F);
        });
    }

    /**
     * @brief Draws boxes aligned to three axes x, y, z (right-handed.
     *
     * X cross y is z), each face showing the full width of the texture between rows v0 and v1.
     *
     * @param pose The current pose.
     * @param consumer The vertex consumer to draw to.
     * @param light The packed light level.
     * @param x The box's x axis.
     * @param y The box's y axis.
     * @param z The box's z axis.
     */
    private record Cuboid(PoseStack.Pose pose, VertexConsumer consumer, int light, Vec3 x, Vec3 y, Vec3 z) {
        void draw(Vec3 centre, float halfX, float halfY, float halfZ, float v0, float v1) {
            Vec3 hx = x.scale(halfX);
            Vec3 hy = y.scale(halfY);
            Vec3 hz = z.scale(halfZ);
            // Each face's two edge directions, ordered so their cross product points out of the face
            face(centre.add(hx), hy, hz, x, v0, v1);
            face(centre.subtract(hx), hz, hy, x.reverse(), v0, v1);
            face(centre.add(hy), hz, hx, y, v0, v1);
            face(centre.subtract(hy), hx, hz, y.reverse(), v0, v1);
            face(centre.add(hz), hx, hy, z, v0, v1);
            face(centre.subtract(hz), hy, hx, z.reverse(), v0, v1);
        }

        private void face(Vec3 middle, Vec3 a, Vec3 b, Vec3 normal, float v0, float v1) {
            vertex(middle.subtract(a).subtract(b), 0.0F, v1, normal);
            vertex(middle.add(a).subtract(b), 1.0F, v1, normal);
            vertex(middle.add(a).add(b), 1.0F, v0, normal);
            vertex(middle.subtract(a).add(b), 0.0F, v0, normal);
        }

        private void vertex(Vec3 pos, float u, float v, Vec3 normal) {
            consumer.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
        }
    }

    private LinkingBar() {}
}
