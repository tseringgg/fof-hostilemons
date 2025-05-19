package me.rufia.fightorflight.client.model;

import me.rufia.fightorflight.CobblemonFightOrFlight;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public class HostileMonsModelLayers {
    public static final ModelLayerLocation FIREBALL = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(CobblemonFightOrFlight.MODID, "textures/entity/fireball.png"), "main"
    );
}
