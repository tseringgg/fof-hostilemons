package me.rufia.fightorflight.entity.projectile;

import com.cobblemon.mod.common.api.moves.MoveTemplate;
import me.rufia.fightorflight.CobblemonFightOrFlight;
import me.rufia.fightorflight.damage.HostilemonsDamageTypes;
import me.rufia.fightorflight.entity.EntityFightOrFlight;
import me.rufia.fightorflight.entity.PokemonAttackEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class InvisibleFlamethrowerHitProjectile extends AbstractPokemonProjectile{
    private MoveTemplate moveUsed;
    public final int LIFETIME_IN_TICKS = 20;
    // New fields to store the randomized motion parameters for this specific projectile instance
    private float initialLinearAccelerationY;
    private float initialLinearDragCoefficient;
    private boolean motionParamsInitialized = false;
    private final float DAMAGE_ON_HIT = 1.0F;

    public InvisibleFlamethrowerHitProjectile(EntityType<? extends InvisibleFlamethrowerHitProjectile> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }
    // Constructor used when an entity shoots it
    public InvisibleFlamethrowerHitProjectile(Level level, LivingEntity owner, MoveTemplate move) {
        super(EntityFightOrFlight.POKEMON_FLAMETHROWER_PROJECTILE.get(), level); // Use your registered EntityType
        this.setOwner(owner);
        this.moveUsed = move;
        this.setNoGravity(true);
        initPosition(owner);
        initializeMotionParameters();
    }

    // Constructor for networking/loading (less used for simple projectiles but good practice)
//    public InvisibleFlamethrowerHitProjectile(Level level, double x, double y, double z) {
//        super(EntityFightOrFlight.POKEMON_FLAMETHROWER_PROJECTILE.get(), x, y, z, level);
//        this.setNoGravity(true);
//    }
    // Helper to initialize motion parameters once
    private void initializeMotionParameters() {
        if (!motionParamsInitialized && !this.level().isClientSide()) { // Initialize on server once
            Random rand = new Random(); // Use the entity's built-in random instance

            // From "linear_acceleration": [0, "math.random(1,4)", 0]
            // math.random(A, B) in Bedrock usually means A + random_float_0_to_1 * (B - A)
            // For simplicity, let's take it as a value between 1 and 4.
            this.initialLinearAccelerationY = 0.3F;//0f * (1.0f + rand.nextFloat() * 3.0f); // Random float between 1.0 and 4.0

            // From "linear_drag_coefficient": "math.random(0.5 ,1)"
            this.initialLinearDragCoefficient = 0.3f + rand.nextFloat() * 0.5f; // Random float between 0.5 and 1.0

            this.motionParamsInitialized = true;
            // CobblemonFightOrFlight.LOGGER.debug("Projectile {} initialized motion: AccelY={}, Drag={}", this.getId(), this.initialLinearAccelerationY, this.initialLinearDragCoefficient);
        }
    }

    @Override
    public void tick() {
        super.tick();
        // Apply custom motion dynamics (server-side authoritative for movement)
        if (!this.level().isClientSide() && this.motionParamsInitialized) {
            Vec3 currentVelocity = this.getDeltaMovement();
            double newVelX = currentVelocity.x;
            double newVelY = currentVelocity.y;
            double newVelZ = currentVelocity.z;

            // 1. Apply Linear Acceleration
            // Acceleration is typically change in velocity per second.
            // We apply it per tick (1/20th of a second).
            // The JSON is [0, "math.random(1,4)", 0]
            double accelYPerTick = this.initialLinearAccelerationY / 20.0;
            newVelY += accelYPerTick;

            // 2. Apply Linear Drag
            // Drag formula: v_new = v_old * (1 - drag_coefficient * delta_time)
            // delta_time is 1 tick = 1/20th of a second = 0.05
            double dragFactor = 1.0 - (this.initialLinearDragCoefficient * 0.05);
            // Ensure dragFactor doesn't go negative if drag coefficient is very high (though it shouldn't with 0.5-1 range)
            dragFactor = Math.max(0, dragFactor);

            newVelX *= dragFactor;
            newVelY *= dragFactor; // Drag also affects vertical motion, including the added acceleration
            newVelZ *= dragFactor;

            this.setDeltaMovement(newVelX, newVelY, newVelZ);
        }

        // Despawn after a certain time
        if (this.tickCount > LIFETIME_IN_TICKS && !this.level().isClientSide()) { // Despawn logic on server
            this.discard();
        }

        // === ADD CLIENT-SIDE PARTICLES FOR VISIBILITY ===
        if (this.level().isClientSide()) {
            // Spawn a simple particle at the projectile's current location each tick
            // Choose a particle that's easy to see, like FLAME or CRIT
//            this.level().addParticle(
//                    ParticleTypes.FLAME, // Or ParticleTypes.CRIT, ParticleTypes.SMOKE
//                    this.getX(),
//                    this.getY(),
//                    this.getZ(),
//                    0.0D, // No initial velocity for the particle itself (it follows the projectile)
//                    0.0D,
//                    0.0D
//            );
        }
    }
    @Override
    protected void makeParticle(int particleAmount) {
        // dont create particles for invisible projectile
    }
    @Override
    protected void onHitEntity(EntityHitResult entityHitResult){
        super.onHitEntity(entityHitResult); // Call super method

        Entity hitEntity = entityHitResult.getEntity();
        Entity owner = this.getOwner(); // The Pokemon that shot this projectile

        // Perform checks on the server side
        if (!this.level().isClientSide() && hitEntity instanceof LivingEntity target) {
            if (owner == target) { // Don't hit self
                // Optionally discard if it hits owner, or just do nothing
                // this.discard();
                return;
            }

            // Ensure owner is also a LivingEntity for proper damage attribution, though DamageSource can handle Entity
            LivingEntity attacker = (owner instanceof LivingEntity) ? (LivingEntity) owner : null;

            // --- Create the custom DamageSource ---
            Level world = this.level();
            DamageSource customDamageSource = new DamageSource(
                    world.registryAccess() // Get RegistryAccess from the level
                            .registryOrThrow(Registries.DAMAGE_TYPE) // Get the DamageType registry
                            .getHolderOrThrow(HostilemonsDamageTypes.FLAMETHROWER_STREAM), // Get our custom DamageType Holder
                    this,    // The direct source of damage (the projectile itself)
                    attacker // The indirect/true source of damage (the Pokemon that fired it)
            );

            // --- Apply Damage ---
            float damageAmount = DAMAGE_ON_HIT; // Half a heart per hit

            CobblemonFightOrFlight.LOGGER.info("FoF Projectile: Applying {} damage to {} from {} via {}",
                    damageAmount, target.getName().getString(),
                    attacker != null ? attacker.getName().getString() : "Unknown Attacker",
                    HostilemonsDamageTypes.FLAMETHROWER_STREAM.location());
            CobblemonFightOrFlight.LOGGER.info("DamageSourceType: {}", customDamageSource.typeHolder().unwrapKey().map(ResourceKey::location).orElse(ResourceLocation.withDefaultNamespace("unknown")));
            boolean damaged = target.hurt(customDamageSource, damageAmount);

            if (damaged) {
                // Optional: Apply burn effect if your DamageType doesn't inherently do it
                // (though "is_fire": true might make some entities catch fire)
                // The "effects": "burning" in JSON is usually just the screen overlay.
                if (world.getRandom().nextFloat() < 0.1f) { // 10% chance to set on fire
                    target.setRemainingFireTicks(5); // Burn for 3 seconds
                }
                CobblemonFightOrFlight.LOGGER.info("FoF Projectile: {} took damage.", target.getName().getString());
            } else {
                CobblemonFightOrFlight.LOGGER.info("FoF Projectile: {} did NOT take damage (maybe immune, or hurt returned false).", target.getName().getString());
            }

//            this.discard(); // Remove projectile after attempting to deal damage
        } else if (!this.level().isClientSide()){
            // If it hit a non-LivingEntity, or if it's just cleaning up
            this.discard();
        }
    }
}
