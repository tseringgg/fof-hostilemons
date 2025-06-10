package me.rufia.fightorflight.net.handler;

// Import all necessary classes for ParticleStorm, BedrockParticleOptionsRepository, etc.
import com.bedrockk.molang.runtime.MoLangRuntime;
import com.bedrockk.molang.runtime.value.MoValue;
import com.cobblemon.mod.common.api.snowstorm.BedrockParticleEmitter;
import com.cobblemon.mod.common.api.snowstorm.BedrockParticleOptions;
import com.cobblemon.mod.common.client.particle.BedrockParticleOptionsRepository;
import com.cobblemon.mod.common.client.particle.ParticleStorm;
import com.cobblemon.mod.common.client.render.MatrixWrapper;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import com.cobblemon.mod.common.entity.PosableEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import kotlin.jvm.functions.Function0;
import me.rufia.fightorflight.CobblemonFightOrFlight;
import me.rufia.fightorflight.net.packet.S2CPlayInhaleEffectPacket;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player; // For context, though might not be directly used
import net.minecraft.world.level.Level;
import kotlin.Unit; // For Runnable -> () -> Unit mapping

import java.util.ArrayList;
import java.util.List;

public class S2CPlayInhaleEffectHandler { // No need to implement NetworkPacketHandler for ClientPlayNetworking

    public void handle(S2CPlayInhaleEffectPacket packet, Player playerEntity, Level clientLevelUntyped) {
        if (!(clientLevelUntyped instanceof ClientLevel clientLevel)) {
            CobblemonFightOrFlight.LOGGER.warn("S2CPlayInhaleEffectHandler received packet on non-client level!");
            return;
        }

        Entity entity = clientLevel.getEntity(packet.getPokemonEntityId());
        if (entity instanceof PokemonEntity sourcePokemon && entity instanceof PosableEntity) {
            PosableState posableState = (PosableState) ((PosableEntity) sourcePokemon).getDelegate();
            if (posableState == null) {
                CobblemonFightOrFlight.LOGGER.warn("Client: PosableState not found for {} during inhale effect.", sourcePokemon.getDisplayName().getString());
                return;
            }

            BedrockParticleOptions bedrockOptions = BedrockParticleOptionsRepository.INSTANCE.getEffect(packet.getParticleEffectId());
            if (bedrockOptions == null) {
                CobblemonFightOrFlight.LOGGER.warn("Client: Could not find BedrockParticleOptions for inhale effect: {}", packet.getParticleEffectId());
                return;
            }
            var actualEffect = bedrockOptions;

            MatrixWrapper rootMatrix = posableState.getLocatorStates().get("root");
            MatrixWrapper locatorMatrix = posableState.getLocatorStates().get(packet.getLocatorName());


            if (rootMatrix == null) {
                CobblemonFightOrFlight.LOGGER.error("Client: Root locator matrix not found in PosableState for {}.", sourcePokemon.getDisplayName().getString());
                return;
            }
            if (locatorMatrix == null) {
                CobblemonFightOrFlight.LOGGER.warn("Client: Locator '{}' not found in PosableState for {}. Defaulting to root.", packet.getLocatorName(), sourcePokemon.getDisplayName().getString());
                locatorMatrix = rootMatrix;
            }
            BedrockParticleEmitter emitter = new BedrockParticleEmitter();

            MatrixWrapper emitterSpaceMatrix = new MatrixWrapper();
            emitterSpaceMatrix.updatePosition(rootMatrix.getPosition());
            emitterSpaceMatrix.updateMatrix(locatorMatrix.getMatrix());

            MoLangRuntime particleRuntime = new MoLangRuntime();
            particleRuntime.getEnvironment().query.functions.putAll(posableState.getRuntime().getEnvironment().query.getFunctions());
            particleRuntime.getEnvironment().setSimpleVariable("variable.emitter_lifetime", MoValue.of(packet.getDurationSeconds()));

            Function0<Unit> onDespawnLambda = () -> {return null;};
            var locators = new ArrayList<String>();
            locators.add(packet.getLocatorName());
            var storms = ParticleStorm.Companion.createAtEntity(
                    clientLevel,
                    bedrockOptions,
                    sourcePokemon,
                    locators
            );
            storms.getFirst().spawn();

//            ParticleStorm storm = new ParticleStorm(
//                    bedrockOptions,       // 1. val effect: BedrockParticleOptions
//                    emitterSpaceMatrix,   // 2. val matrixWrapper: MatrixWrapper (This is the Emitter Space Matrix)
//                    clientLevel,          // 3. val world: ClientLevel
//                    sourcePokemon::getDeltaMovement, // 4. val sourceVelocity: () -> Vec3
//                    () -> !sourcePokemon.isRemoved() && sourcePokemon.isAlive(), // 5. val sourceAlive: () -> Boolean
//                    () -> !sourcePokemon.isInvisible(), // 6. val sourceVisible: () -> Boolean
//                    onDespawnLambda,      // 7. val onDespawn: () -> Unit
//                    particleRuntime,      // 8. val runtime: MoLangRuntime (use our configured one)
//                    sourcePokemon         // 9. val entity: Entity? (the Pokemon causing the effect)
//            );
//            storm.spawn();
            CobblemonFightOrFlight.LOGGER.info("Client: Spawned inhale ParticleStorm {} for {}", packet.getParticleEffectId(), sourcePokemon.getDisplayName().getString());

        } else {
            CobblemonFightOrFlight.LOGGER.warn("Client: Received S2CPlayInhaleEffectPacket for non-PokemonEntity or non-PosableEntity: {}", packet.getPokemonEntityId());
        }
    }
}