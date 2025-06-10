package me.rufia.fightorflight;

import me.rufia.fightorflight.api.NetworkHandler;
import me.rufia.fightorflight.net.packet.S2CPlayInhaleEffectPacket; // Your packet class (should be in common or fabric depending on dependencies)
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class FabricNetworkHandler implements NetworkHandler {
    @Override
    public void sendPlayInhaleEffectPacketToClients(Entity attacker, ResourceLocation particleId, String locatorName, float duration) {
        if (attacker.level().isClientSide()) return;

        S2CPlayInhaleEffectPacket packet = new S2CPlayInhaleEffectPacket(
                attacker.getId(),
                particleId,
                locatorName,
                duration
        );

        for (ServerPlayer observingPlayer : PlayerLookup.tracking(attacker)) {
            ServerPlayNetworking.send(observingPlayer, packet);
        }
        if (attacker.getControllingPassenger() instanceof ServerPlayer ownerPlayer && // Example: if Pokemon is controlled
                !PlayerLookup.tracking(attacker).contains(ownerPlayer)) { // Or attacker.getOwner() logic
            ServerPlayNetworking.send(ownerPlayer, packet);
        }
        // Add logic for pokemonEntity.getOwner() if that's how your Pokemon is linked to a player
    }
}
