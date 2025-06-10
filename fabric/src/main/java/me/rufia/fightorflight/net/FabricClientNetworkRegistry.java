package me.rufia.fightorflight.net;


import me.rufia.fightorflight.net.ClientNetworkRegistry;
import me.rufia.fightorflight.net.packet.S2CPlayInhaleEffectPacket; // Assuming packet class is in common or accessible
import me.rufia.fightorflight.net.handler.S2CPlayInhaleEffectHandler; // Assuming handler is in common or accessible
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity; // If needed by handler

public class FabricClientNetworkRegistry implements ClientNetworkRegistry {

    @Override
    public void registerClientReceivers() {
        // Register S2C packet types and their codecs with Fabric's PayloadTypeRegistry
        PayloadTypeRegistry.playS2C().register(S2CPlayInhaleEffectPacket.TYPE, S2CPlayInhaleEffectPacket.STREAM_CODEC);
        // Add other S2C packet type registrations here if you have more

        // Register global receivers for these S2C packets
        ClientPlayNetworking.registerGlobalReceiver(
                S2CPlayInhaleEffectPacket.TYPE,
                (packet, context) -> { // context is ClientPlayContext
                    context.client().execute(() -> { // Ensure on main client thread
                        S2CPlayInhaleEffectHandler handler = new S2CPlayInhaleEffectHandler();
                        // Pass context.player() and context.client().level()
                        handler.handle(packet, context.player(), context.client().level);
                    });
                }
        );
        // Add other S2C packet receivers here
    }
}
