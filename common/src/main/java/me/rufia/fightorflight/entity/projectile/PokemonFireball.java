package me.rufia.fightorflight.entity.projectile;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
// Import particle types, damage sources, etc. as needed

public class PokemonFireball extends Fireball {

    public PokemonFireball(EntityType<? extends PokemonFireball> entityType, Level level) {
        super(entityType, level);
        // Optional: Prevent slowdown in water?
        // this.setNoGravity(true); // Already default for AbstractHurtingProjectile, but explicit doesn't hurt
    }

    public PokemonFireball(EntityType<? extends PokemonFireball> entityType, double x, double y, double z, Vec3 movement, Level level) {
        super(entityType, x, y, z, movement, level);
        // this.setNoGravity(true);
    }

    public PokemonFireball(EntityType<? extends PokemonFireball> entityType, LivingEntity owner, Vec3 movement, Level level) {
        super(entityType, owner, movement, level);
        // this.setNoGravity(true);
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        super.onHitEntity(entityHitResult); // Important for basic projectile collision logic
        if (!this.level().isClientSide()) {
            // Apply damage, status effects, etc.
            LivingEntity owner = this.getOwner() instanceof LivingEntity ? (LivingEntity)this.getOwner() : null;
            boolean damaged = entityHitResult.getEntity().hurt(this.damageSources().fireball(this, owner), 6.0F); // Example damage
            if (damaged && entityHitResult.getEntity() instanceof LivingEntity target) {
                // Set target on fire
                target.setRemainingFireTicks(5);
            }
            this.discard(); // Remove projectile after hit
        }
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult); // Handle basic collision
        if (!this.level().isClientSide()) {
            // Optional: Do something on block hit (e.g., place fire, explode slightly)
            // Example: Create explosion effect without block damage
            // this.level().explode(this, this.getX(), this.getY(), this.getZ(), 1.0F, false, Level.ExplosionInteraction.NONE);

            this.discard(); // Remove projectile after hit
        }
    }

    @Override
    public boolean isPickable() {
        // Can players pick it up by punching it? Usually false.
        return false;
    }

    @Override
    public boolean hurt(DamageSource damageSource, float amount) {
        // Can the projectile itself be damaged/destroyed? Usually false unless you want it deflected.
        return false;
    }

    @Override
    protected boolean shouldBurn() {
        // Should the projectile look like it's on fire? Yes for a fireball!
        return true;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        // Particle effect to leave behind
        return ParticleTypes.FLAME; // Or ParticleTypes.SMALL_FLAME, etc.
    }

    // Optional: Override tick() if you need custom movement behavior,
    // but the default AbstractHurtingProjectile movement is usually fine.
    // @Override
    // public void tick() {
    //     super.tick();
    //     // Add custom logic here if needed
    // }
}
