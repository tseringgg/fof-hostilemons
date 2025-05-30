package me.rufia.fightorflight.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.rufia.fightorflight.CobblemonFightOrFlight;
import me.rufia.fightorflight.entity.projectile.InvisibleFlamethrowerHitProjectile;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class InvisibleFlamethrowerProjectileRenderer extends EntityRenderer<InvisibleFlamethrowerHitProjectile> {
    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath(CobblemonFightOrFlight.MODID, "textures/entity/tracing_bullet_spark.png");
    private static final RenderType RENDER_TYPE;

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

        // Scale the projectile visual if needed
        float scale = 0.5F; // Adjust to make it bigger/smaller
        poseStack.scale(scale, scale, scale);

        PoseStack.Pose lastPose = poseStack.last();
        Matrix4f poseMatrix = lastPose.pose();
        Matrix3f normalMatrix = lastPose.normal();

        // Get the appropriate VertexConsumer for our texture
        // RenderType.entityTranslucentCull allows for transparency and culls back faces
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucentCull(getTextureLocation(entity)));

        // Draw a textured quad (a square made of two triangles)
        // UV coordinates: (0,0) top-left, (1,0) top-right, (1,1) bottom-right, (0,1) bottom-left
        // X, Y, Z are relative to the entity's center after scaling and rotation
        // Z is 0 because we're billboarded (flat against the view plane)

        // Top-left vertex
        vertex(vertexConsumer, poseMatrix, normalMatrix, packedLight, -0.5F,  0.5F, 0.0F, 0.0F, 0.0F); // x, y, z, u, v
        // Bottom-left vertex
        vertex(vertexConsumer, poseMatrix, normalMatrix, packedLight, -0.5F, -0.5F, 0.0F, 0.0F, 1.0F);
        // Bottom-right vertex
        vertex(vertexConsumer, poseMatrix, normalMatrix, packedLight,  0.5F, -0.5F, 0.0F, 1.0F, 1.0F);
        // Top-right vertex
        vertex(vertexConsumer, poseMatrix, normalMatrix, packedLight,  0.5F,  0.5F, 0.0F, 1.0F, 0.0F);


        // If drawing a different shape or using a more complex model, that logic would go here.

        poseStack.popPose();
        // Don't call super.render if you are fully handling rendering,
        // unless the superclass does something essential like a name tag.
        // For a simple projectile, often not needed.
        // super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    // Helper method to add a vertex
    private void vertex(VertexConsumer consumer, Matrix4f poseMatrix, Matrix3f normalMatrix, int light, float x, float y, float z, float u, float v) {
        consumer.addVertex(poseMatrix, x, y, z)
                .setColor(255, 255, 255, 255) // White, no tint
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY) // No damage overlay
                .setLight(light) // Packed light value
                .setNormal(0.0F, 1.0F, 0.0F); // A generic upward normal, good enough for billboards
    }


    static {
        RENDER_TYPE = RenderType.entityTranslucent(TEXTURE_LOCATION);
    }
}
