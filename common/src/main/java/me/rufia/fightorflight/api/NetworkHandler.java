package me.rufia.fightorflight.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public interface NetworkHandler {
    void sendPlayInhaleEffectPacketToClients(Entity attacker, ResourceLocation particleId, String locatorName, float duration);
    // Add other network methods here if needed
}
