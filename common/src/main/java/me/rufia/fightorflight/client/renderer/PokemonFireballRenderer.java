package me.rufia.fightorflight.client.renderer;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import me.rufia.fightorflight.CobblemonFightOrFlight;
import me.rufia.fightorflight.client.model.HostileMonsModelLayers;
import me.rufia.fightorflight.client.model.PokemonBulletModel;
import me.rufia.fightorflight.client.model.PokemonFireballModel;
import me.rufia.fightorflight.entity.projectile.PokemonBullet;
import me.rufia.fightorflight.entity.projectile.PokemonFireball;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class PokemonFireballRenderer extends EntityRenderer<PokemonFireball> {
    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath(CobblemonFightOrFlight.MODID, "textures/entity/tracing_bullet_spark.png");
    private static final RenderType RENDER_TYPE;
    private final PokemonFireballModel<PokemonFireball> model;
    public PokemonFireballRenderer(EntityRendererProvider.Context context) {
        super(context);
        // 4. Bake your custom model layer
        this.model = new PokemonFireballModel<PokemonFireball>(context.bakeLayer(HostileMonsModelLayers.FIREBALL)); // Use your registered layer
        // Prevent shadow (optional, good for projectiles)
        this.shadowRadius = 0.0F;
    }
    // Keep this - makes the fireball always appear bright
    @Override
    protected int getBlockLightLevel(PokemonFireball entity, BlockPos pos) {
        return 15;
    }
    @Override
    public void render(PokemonFireball entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // --- Adjust Transformations ---
        // Optional: Translate slightly up if needed
        poseStack.translate(0.0D, 0.1D, 0.0D);

        // --- Rotation ---
        // Option A: Simple rotation matching entity's direction (Recommended for non-spherical models)
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));
        // Option B: No rotation (Good for perfect spheres or sprites)
        // If your model is a sphere, you might not need rotation, comment out the above lines.
        // If rendering a sprite, you'd use billboarding rotations instead.

        // --- Remove Wild Spinning from Template ---
        // The complex Axis rotations based on tickCount are removed.

        // --- Scaling ---
        // Adjust scale as needed for your model/texture size
        float scale = 1.0f; // Example scale factor
        poseStack.scale(scale, scale, scale);


        // --- Render the Model ---
        // Use the RenderType defined by the model itself (or FIREBALL_RENDER_TYPE)
        VertexConsumer vertexConsumer = buffer.getBuffer(this.model.renderType(TEXTURE_LOCATION));

        // Setup animations (if your model has any, otherwise defaults are fine)
        this.model.setupAnim(entity, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F); // Use default values

        // Render the main model
        // Apply a color tint if desired (e.g., full white = 0xFFFFFFFF)
        this.model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF); // Use default white tint


        // --- Optional: Second Render Pass for Glow (like template) ---
        /*
        if (true) { // Condition to enable glow
            poseStack.pushPose(); // Isolate transformations for glow
            // Slightly larger scale for the glow effect
            float glowScale = 1.2f;
            poseStack.scale(glowScale, glowScale, glowScale);

            // Use a translucent or additive render type for the glow
             VertexConsumer glowVertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(FIREBALL_TEXTURE_LOCATION));
             // OR Try an additive blend for a brighter glow:
             // VertexConsumer glowVertexConsumer = buffer.getBuffer(RenderType.energySwirl(getTextureLocation(entity), 0, 0)); // Experiment with this

            // Render the model again for the glow
            // Use a specific color for the glow (e.g., semi-transparent orange)
            // int glowColor = FastColor.ARGB32.color(150, 255, 150, 0); // Alpha, Red, Green, Blue
             int glowColor = 0x90FF9600; // Example: Semi-transparent orange ARGB hex

            this.model.renderToBuffer(poseStack, glowVertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, glowColor);

            poseStack.popPose(); // Restore transformations
        }
        */


        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight); // Call superclass render
    }

    @Override
    public ResourceLocation getTextureLocation(PokemonFireball entity) {
        return TEXTURE_LOCATION;
    }
    static {
        RENDER_TYPE = RenderType.entityTranslucent(TEXTURE_LOCATION);
    }
}
