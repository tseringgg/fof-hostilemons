package me.rufia.fightorflight.net.packet;

import io.netty.buffer.ByteBuf;
import me.rufia.fightorflight.CobblemonFightOrFlight; // For packet ID
import me.rufia.fightorflight.net.NetworkPacket;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class S2CPlayInhaleEffectPacket implements NetworkPacket, CustomPacketPayload {
    // 1. Define the Packet ID (as a ResourceLocation for the Type)
    public static final ResourceLocation PACKET_ID =
            ResourceLocation.fromNamespaceAndPath(CobblemonFightOrFlight.MODID, "play_inhale_effect");

    // 2. Define the Packet Type (static final field)
    public static final Type<S2CPlayInhaleEffectPacket> TYPE = new Type<>(PACKET_ID);

    // 3. Declare your fields
    private final int pokemonEntityId;
    private final ResourceLocation particleEffectId;
    private final String locatorName;
    private final float durationSeconds;

    // 4. Constructor that takes all fields
    public S2CPlayInhaleEffectPacket(int pokemonEntityId, ResourceLocation particleEffectId, String locatorName, float durationSeconds) {
        this.pokemonEntityId = pokemonEntityId;
        this.particleEffectId = particleEffectId;
        this.locatorName = locatorName;
        this.durationSeconds = durationSeconds;
    }

    // 5. Getters for your fields
    public int getPokemonEntityId() {
        return pokemonEntityId;
    }

    public ResourceLocation getParticleEffectId() {
        return particleEffectId;
    }

    public String getLocatorName() {
        return locatorName;
    }

    public float getDurationSeconds() {
        return durationSeconds;
    }

    // 6. Implement the type() method from CustomPacketPayload
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE; // Return your static TYPE field
    }

    // 7. Define the StreamCodec for serialization/deserialization
    //    This MUST be static for the Type constructor if not passed directly.
    //    Or, it can be defined and passed to new Type<>(PACKET_ID, STREAM_CODEC);
    //    Let's define it statically as in the example.
    public static final StreamCodec<ByteBuf, S2CPlayInhaleEffectPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, // Use VAR_INT for entity IDs usually, or just INT
            S2CPlayInhaleEffectPacket::getPokemonEntityId,

            ResourceLocation.STREAM_CODEC,
            S2CPlayInhaleEffectPacket::getParticleEffectId,

            ByteBufCodecs.STRING_UTF8, // For strings
            S2CPlayInhaleEffectPacket::getLocatorName,

            ByteBufCodecs.FLOAT,
            S2CPlayInhaleEffectPacket::getDurationSeconds,

            // The constructor reference that takes all these fields in order
            S2CPlayInhaleEffectPacket::new
    );
}
