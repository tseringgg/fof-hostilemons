package me.rufia.fightorflight.net;

import dev.architectury.networking.NetworkManager;
import me.rufia.fightorflight.net.handler.S2CPlayInhaleEffectHandler;
import me.rufia.fightorflight.net.handler.SendCommandHandler;
import me.rufia.fightorflight.net.handler.SendMoveSlotHandler;
import me.rufia.fightorflight.net.packet.S2CPlayInhaleEffectPacket;
import me.rufia.fightorflight.net.packet.SendCommandPacket;
import me.rufia.fightorflight.net.packet.SendMoveSlotPacket;

public class CobblemonFightOrFlightNetwork {
    public static void init(){
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SendCommandPacket.TYPE, SendCommandPacket.STREAM_CODEC, ((packet, context) -> {
            SendCommandHandler handler = new SendCommandHandler();
            handler.handle(packet, context);
        }));
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SendMoveSlotPacket.TYPE, SendMoveSlotPacket.STREAM_CODEC, ((packet, context) -> {
            SendMoveSlotHandler handler = new SendMoveSlotHandler();
            handler.handle(packet, context);
        }));
//        NetworkManager.registerReceiver(NetworkManager.Side.S2C, S2CPlayInhaleEffectPacket.TYPE, S2CPlayInhaleEffectPacket.STREAM_CODEC, ((packet, context)->{
//            S2CPlayInhaleEffectHandler handler = new S2CPlayInhaleEffectHandler();
//            handler.handle(packet, context);
//        }));

    }
//    public static void clientInit() { // Call this from your ClientModInitializer
//        PayloadTypeRegistry.playS2C().register(S2CPlayInhaleEffectPacket.TYPE, S2CPlayInhaleEffectPacket.STREAM_CODEC);
//
//        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
//                S2CPlayInhaleEffectPacket.TYPE,
//                (packet, context) -> { // context is ClientPlayerContext
//                    // Ensure execution on the main client thread
//                    context.client().execute(() -> {
//                        S2CPlayInhaleEffectHandler handler = new S2CPlayInhaleEffectHandler();
//                        handler.handle(packet, context.player(), context.client().level); // Pass player and level
//                    });
//                }
//        );
//    }
}
