package me.rufia.fightorflight.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.Entity;

public class PokemonFireballModel<T extends Entity> extends HierarchicalModel<T> {

    // This 'main' ModelPart will hold all the cubes/parts of your model
    private final ModelPart root;
    private final ModelPart main;

    public PokemonFireballModel(ModelPart root) {
        // Assign the 'main' part from the baked model data
        // The name "main" must match what you use in createBodyLayer()
        this.root = root;
        this.main = root.getChild("main");
    }

    /**
     * This method defines the shape of your model.
     * It's called when the model layer is registered.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Define the 'main' part of your fireball
        // You can add multiple cubes to this part definition to make more complex shapes
        // CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, width, height, depth, CubeDeformation.NONE)
        // - (u, v) are the texture offsets on your fireball.png
        // - (x, y, z) is the starting corner of the box relative to the model's origin (0,0,0)
        // - (width, height, depth) are the dimensions of the box
        // - CubeDeformation.NONE means no extra inflation/deflation

        // Example: A single 8x8x8 cube centered at the origin
        // Texture offsets (0, 0) assume your texture for this cube starts at the top-left of your fireball.png
        partdefinition.addOrReplaceChild("main", CubeListBuilder.create()
                        .texOffs(0, 0) // Top-left corner of your texture map for this cube
                        .addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, CubeDeformation.NONE),
                PartPose.ZERO); // PartPose.ZERO means no initial translation or rotation for this part

        // To make it more spherical, you'd add more cubes, potentially smaller and rotated,
        // or use a tool like Blockbench to create a JSON model and then convert that to LayerDefinition.

        return LayerDefinition.create(meshdefinition, 32, 32); // (textureWidth, textureHeight) of your fireball.png
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // This method is for animations.
        // For a simple, non-animated fireball, you can leave this empty or apply basic rotations if needed.
        // For example, if you want it to slowly spin:
        // this.main.yRot = ageInTicks / 20.0F; // Spin around Y axis
        // this.main.xRot = ageInTicks / 30.0F; // Spin around X axis
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        // This method renders all defined parts.
        // 'main' is the root part containing our cube(s).
        main.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }
}
