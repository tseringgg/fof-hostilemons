package me.rufia.fightorflight.client.renderer;

import com.cobblemon.mod.common.Cobblemon;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.rufia.fightorflight.CobblemonFightOrFlight;
import me.rufia.fightorflight.entity.projectile.InvisibleFlamethrowerHitProjectile;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class InvisibleFlamethrowerProjectileRenderer extends EntityRenderer<InvisibleFlamethrowerHitProjectile> {
    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath(Cobblemon.MODID, "textures/particle/impact-fire.png");
    private static final RenderType RENDER_TYPE;
    private static final float[][] COLOR_GRADIENT = {
            // Time,   R,   G,   B,   A (Alpha from ARGB hex, e.g., #AARRGGBB)
            {0.0f,   255, 255, 255, 255}, // #FFFFFFFF (White)
            {0.35f,  255, 255, 255, 255}, // #ffffffff (White)
            {0.7f,   255,  83,  83, 255}, // #FFFF5353 (Reddish)
            {0.79f,   46,  20,  13, 105}, // #692E140D (Darker, more transparent red/brown) - Note: D is 13 in decimal for alpha here.
            // The AWT Color class uses 0-255 for alpha. Hex ARGB has Alpha first.
            // #692E140D -> Alpha: 0x69 (105), Red: 0x2E (46), Green: 0x14 (20), Blue: 0x0D (13)
            {0.89f,   61,  16,  16,   0}  // #003D1010 (Very dark, fully transparent red/brown) -> Alpha: 0x00 (0)
            // Corrected for 0 alpha: Red: 0x3D (61), Green: 0x10 (16), Blue: 0x10 (16)
    };

    public InvisibleFlamethrowerProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(InvisibleFlamethrowerHitProjectile entity) {
        return TEXTURE_LOCATION;
    }
    @Override
    public void render(InvisibleFlamethrowerHitProjectile entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // Billboard rendering: always faces the camera
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation()); // Orient towards camera
        // Optional: If your texture needs to be rotated 90 degrees to appear upright when billboarded
        // poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F)); // Example rotation
        // --- Flipbook Animation Logic ---
        // Use entity's age (tickCount) to drive the animation
        // partialTicks helps smooth it between ticks
        float totalTicksElapsed = entity.tickCount + partialTicks;
        int FRAMES_PER_SECOND = 20;
        int MAX_FRAME_INDEX = 7;
//        int currentFrame = (int) ((totalTicksElapsed / 20.0F) * FRAMES_PER_SECOND); // Ticks to seconds, then multiply by FPS

        int currentFrame = entity.LIFETIME_IN_TICKS == 0 ? 0 : (int) (totalTicksElapsed / (float)entity.LIFETIME_IN_TICKS * 7.99);
        currentFrame %= (MAX_FRAME_INDEX + 1); // Loop the animation (0 to MAX_FRAME_INDEX)

        int TEXTURE_WIDTH = 8;
        int TEXTURE_HEIGHT = 56;
        int FRAME_WIDTH_UV = 8;
        int FRAME_HEIGHT_UV = 8;
        int STEP_U = 0;
        int STEP_V = 8;
        // Calculate UV coordinates for the current frame
        float u0 = (float) (0 + currentFrame * STEP_U) / TEXTURE_WIDTH; // Base U + (frame * stepU)
        float v0 = (float) (0 + currentFrame * STEP_V) / TEXTURE_HEIGHT; // Base V + (frame * stepV)
        float u1 = u0 + (float) FRAME_WIDTH_UV / TEXTURE_WIDTH;
        float v1 = v0 + (float) FRAME_HEIGHT_UV / TEXTURE_HEIGHT;
        // --- End Flipbook Logic ---

        // Scale the projectile visual if needed
        float maxScale = 0.9F; // Adjust to make it bigger/smaller
        float minScale = 0.3F;
        float scale = minScale + totalTicksElapsed / (float)entity.LIFETIME_IN_TICKS * (maxScale-minScale);
        poseStack.scale(scale, scale, scale);

        PoseStack.Pose lastPose = poseStack.last();
        Matrix4f poseMatrix = lastPose.pose();
        Matrix3f normalMatrix = lastPose.normal();

        // --- Calculate Interpolated Color ---
        float lifetimeProgress = Math.min(1.0f, totalTicksElapsed / entity.LIFETIME_IN_TICKS);
        // Ensure PROJECTILE_MAX_AGE_TICKS matches when your entity discards for visual consistency.
        // If your projectile discards at 60 ticks, then PROJECTILE_MAX_AGE_TICKS should be 60.

        int[] currentColor = getInterpolatedColor(lifetimeProgress, COLOR_GRADIENT);
        int red = currentColor[0];
        int green = currentColor[1];
        int blue = currentColor[2];
        int alpha = currentColor[3];
        // --- End Color Calculation ---

        // Get the appropriate VertexConsumer for our texture
        // RenderType.entityTranslucentCull allows for transparency and culls back faces
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucentCull(getTextureLocation(entity)));
        int customPackedLight = LightTexture.pack(15, 15);

        // Draw quad with dynamic color and UVs
        vertex(vertexConsumer, poseMatrix, normalMatrix, customPackedLight, -0.5F,  0.5F, 0.0F, u0, v0, red, green, blue, alpha);
        vertex(vertexConsumer, poseMatrix, normalMatrix, customPackedLight, -0.5F, -0.5F, 0.0F, u0, v1, red, green, blue, alpha);
        vertex(vertexConsumer, poseMatrix, normalMatrix, customPackedLight,  0.5F, -0.5F, 0.0F, u1, v1, red, green, blue, alpha);
        vertex(vertexConsumer, poseMatrix, normalMatrix, customPackedLight,  0.5F,  0.5F, 0.0F, u1, v0, red, green, blue, alpha);


        // If drawing a different shape or using a more complex model, that logic would go here.

        poseStack.popPose();
        // Don't call super.render if you are fully handling rendering,
        // unless the superclass does something essential like a name tag.
        // For a simple projectile, often not needed.
        // super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    // Helper method to add a vertex
    private void vertex(VertexConsumer consumer, Matrix4f poseMatrix, Matrix3f normalMatrix, int light, float x, float y, float z, float u, float v,
                        int r, int g, int b, int a) {
        consumer.addVertex(poseMatrix, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY) // No damage overlay
                .setLight(light) // Packed light value
                .setNormal(0.0F, 1.0F, 0.0F); // A generic upward normal, good enough for billboards
    }

    // Helper function to interpolate colors from a gradient
    private static int[] getInterpolatedColor(float progress, float[][] gradient) {
        if (gradient == null || gradient.length == 0) {
            return new int[]{255, 255, 255, 255}; // Default white
        }
        if (gradient.length == 1 || progress <= gradient[0][0]) {
            return new int[]{(int)gradient[0][1], (int)gradient[0][2], (int)gradient[0][3], (int)gradient[0][4]};
        }
        if (progress >= gradient[gradient.length - 1][0]) {
            return new int[]{(int)gradient[gradient.length - 1][1], (int)gradient[gradient.length - 1][2], (int)gradient[gradient.length - 1][3], (int)gradient[gradient.length - 1][4]};
        }

        for (int i = 0; i < gradient.length - 1; i++) {
            float[] p1 = gradient[i];
            float[] p2 = gradient[i + 1];
            if (progress >= p1[0] && progress <= p2[0]) {
                float t = (progress - p1[0]) / (p2[0] - p1[0]); // Normalized progress between these two points
                if (Float.isNaN(t) || Float.isInfinite(t)) t = 0; // Handle division by zero if p1[0]==p2[0]

                int r = (int) Mth.lerp(t, p1[1], p2[1]);
                int g = (int) Mth.lerp(t, p1[2], p2[2]);
                int b = (int) Mth.lerp(t, p1[3], p2[3]);
                int a = (int) Mth.lerp(t, p1[4], p2[4]);
                return new int[]{r, g, b, a};
            }
        }
        // Should not be reached if progress is between 0 and 1 and gradient is sorted by time
        return new int[]{(int)gradient[gradient.length - 1][1], (int)gradient[gradient.length - 1][2], (int)gradient[gradient.length - 1][3], (int)gradient[gradient.length - 1][4]};
    }
    static {
        RENDER_TYPE = RenderType.entityTranslucent(TEXTURE_LOCATION);
    }
}
