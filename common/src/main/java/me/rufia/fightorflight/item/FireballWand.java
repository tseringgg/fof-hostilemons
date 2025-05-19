package me.rufia.fightorflight.item;

import com.bedrockk.molang.runtime.MoLangRuntime;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.moves.animations.ActionEffectContext;
import com.cobblemon.mod.common.api.moves.animations.ActionEffectTimeline;
import com.cobblemon.mod.common.api.moves.animations.TargetsProvider;
import com.cobblemon.mod.common.api.moves.animations.UsersProvider;
import com.cobblemon.mod.common.api.moves.animations.keyframes.ActionEffectKeyframe;
import com.cobblemon.mod.common.api.snowstorm.BedrockParticleOptions;
import com.cobblemon.mod.common.client.particle.BedrockParticleOptionsRepository;
import com.cobblemon.mod.common.client.particle.ParticleStorm;
import com.cobblemon.mod.common.client.render.MatrixWrapper;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import me.rufia.fightorflight.CobblemonFightOrFlight;
import me.rufia.fightorflight.entity.EntityFightOrFlight;
import me.rufia.fightorflight.entity.projectile.PokemonFireball;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3; // For direction calculation
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FireballWand extends Item {
    public FireballWand(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        CobblemonFightOrFlight.LOGGER.info("Used FireballWand");
        ItemStack itemStack = player.getItemInHand(hand);

        // Particle spawning is client-side
        if (level.isClientSide() && level instanceof ClientLevel clientLevel) {
            // 1. Define the Particle Effect ID and Locator
            // This is the ID of the .particle.json file that defines the flame stream
            ResourceLocation particleEffectId = ResourceLocation.fromNamespaceAndPath(Cobblemon.MODID, "flamethrower_actor");
            String locatorName = "root"; // START WITH "root" for testing, or try to calculate a "hand" position

            // 2. Get the BedrockParticleOptions from the Repository
            var effect = BedrockParticleOptionsRepository.INSTANCE.getEffect(particleEffectId);

            if (effect == null) { // getEffect() on BedrockParticleOptions likely returns BedrockParticleEffect
                CobblemonFightOrFlight.LOGGER.warn("FireballWand: Could not find particle effect: {}", particleEffectId);
                return InteractionResultHolder.fail(itemStack);
            }
//            BedrockParticleEffect actualEffect = bedrockOptions.getEffect();
            var actualEffect = effect;
            // 3. Get the Entity to attach to (the player)
            LivingEntity sourceEntity = player;

            // 4. Prepare Matrices for Position and Orientation
            // This is the trickiest part to get right without the full PosableState context.
            // We need to position the emitter in front of the player, facing where they look.

            MatrixWrapper rootMatrix = new MatrixWrapper();
            // Set rootMatrix to the player's current position and orientation
            // The PosableState updates this frequently. We need to emulate a snapshot.
            // Start with player's position
            rootMatrix.updatePosition(sourceEntity.position());
            // Create a rotation matrix based on player's look direction
            Matrix4f playerLookRotation = new Matrix4f();
            // Minecraft rotations: Y is yaw (around vertical axis), X is pitch (up/down)
            // JOML/OpenGL rotations might use different conventions for XYZ if building from scratch.
            // Y-axis rotation (Yaw): Negative because Minecraft yaw is clockwise.
            playerLookRotation.rotateY((float)Math.toRadians(-sourceEntity.getYRot() + 180.0F)); // +180 to face "forward" from player
            // X-axis rotation (Pitch): Negative
            playerLookRotation.rotateX((float)Math.toRadians(-sourceEntity.getXRot()));
            rootMatrix.updateMatrix(playerLookRotation);


            // For the locatorMatrix, if using "root", it can be the same as rootMatrix initially.
            // If you wanted a "hand" locator, you'd calculate an offset from the rootMatrix.
            MatrixWrapper locatorMatrix = rootMatrix; // Start with root

            // If you want an offset for the "locator" (e.g., slightly in front of the player's eyes)
            Vec3 lookVec = sourceEntity.getViewVector(1.0f);
            Vec3 handOffsetPos = sourceEntity.getEyePosition().add(lookVec.scale(0.5)); // 0.5 blocks in front of eyes
            MatrixWrapper handLocatorMatrix = new MatrixWrapper();
            handLocatorMatrix.updatePosition(handOffsetPos);
            handLocatorMatrix.updateMatrix(playerLookRotation); // Use same orientation as player

            // Use handLocatorMatrix if you want it to come from near the hands/eyes
            locatorMatrix = handLocatorMatrix;


            // Initialize the emitter matrix based on the BedrockParticleEffect's space requirements
//            MatrixWrapper emitterSpaceMatrix = actualEffect.getSpace().initializeEmitterMatrix(rootMatrix, locatorMatrix);


            // 5. Prepare MoLangRuntime
            // A basic runtime should be enough if the particle effect doesn't use complex entity-specific queries
            // that are normally set up by PosableState's full runtime.
            MoLangRuntime particleRuntime = new MoLangRuntime();
            // Optional: If particles need to query the entity they are attached to (e.g. q.entity.is_on_ground)
            // You might need to provide a way for the particleRuntime to access player's properties.
            // The PosableState does: particleRuntime.environment.query.addFunction("entity") { main_runtime.environment.query }
            // This is harder to replicate perfectly without the full PosableState's runtime.
            // For now, a fresh client runtime might work for many effects.
            // If you have access to the player's PosableState:
            // if (player instanceof PosableEntity pe && pe.getPosableState() != null) {
            //    particleRuntime.getEnvironment().getQuery().addFunctions(pe.getPosableState().getRuntime().getEnvironment().getQuery().getFunctions());
            // }
            Function0<Unit> onDespawnLambda = () -> {
                // This code runs when the ParticleStorm despawns.
                // You can leave it empty if you don't need to do anything specific.
                // System.out.println("ParticleStorm for " + particleEffectId + " despawned.");
                return null;
            };


            // 6. Create and Spawn the ParticleStorm
            var storm = new ParticleStorm(
                    actualEffect,
                    locatorMatrix,      // Use the chosen locator matrix (e.g., handLocatorMatrix)
                    clientLevel,
                    ()->sourceEntity.getDeltaMovement(), // Lambda for current velocity
                    ()->!sourceEntity.isRemoved() && sourceEntity.isAlive(), // Lambda for alive status
                    ()->!sourceEntity.isInvisible(),
                    onDespawnLambda,
                    particleRuntime,
                    sourceEntity
            );

            storm.spawn();
            player.sendSystemMessage(Component.literal("Attempted to spawn particle storm: " + particleEffectId));


            // Optional: Cooldown
            player.getCooldowns().addCooldown(this, 40);

        } else if (!level.isClientSide()) {
            // If you need to tell the server something happened (e.g. for cooldown)
            // but the visual is client-side.
            player.getCooldowns().addCooldown(this, 40);
            // Send a simple confirmation back to the player who used it (optional)
            // ((ServerPlayer) player).sendSystemMessage(Component.literal("Wand used (server ack)"));
        }


        /*
        // Spawn FIREBALL
        if (!level.isClientSide()) { // Only spawn on the server side
            // Get starting position and direction
            double startX = player.getX();
            double startY = player.getEyeY() - 0.1; // Slightly below eye level
            double startZ = player.getZ();

            Vec3 lookDirection = player.getViewVector(1.0F); // Player's look direction (normalized)
            float speed = 1.5f; // Desired speed for the fireball

            // Create the fireball using the constructor that takes owner and acceleration
            PokemonFireball fireball = new PokemonFireball(
                    EntityFightOrFlight.POKEMON_FIREBALL.get(), // Your registered EntityType
                    player.getX(),                             // Owner (the player)
                    player.getY(),
                    player.getZ(),
                    player.getKnownMovement(),
                    level
            );

            // Set the initial position precisely
            fireball.setPos(startX, startY, startZ);

            // Set rotation based on look direction (important for non-spherical models)
            fireball.setYRot(player.getYRot());
            fireball.setXRot(player.getXRot());


            level.addFreshEntity(fireball);

            // Optional: Add a cooldown so the player can't spam it
            player.getCooldowns().addCooldown(this, 20); // 20 ticks = 1 second cooldown

            // Optional: Consume the item or damage it
            // itemStack.shrink(1);
            // itemStack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(hand));
        }
        */


        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }
}
