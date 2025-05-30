package me.rufia.fightorflight.entity.projectile;

import com.cobblemon.mod.common.api.moves.MoveTemplate;
import me.rufia.fightorflight.entity.EntityFightOrFlight;
import me.rufia.fightorflight.entity.PokemonAttackEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class InvisibleFlamethrowerHitProjectile extends AbstractPokemonProjectile{
    private MoveTemplate moveUsed;

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
    }

    // Constructor for networking/loading (less used for simple projectiles but good practice)
//    public InvisibleFlamethrowerHitProjectile(Level level, double x, double y, double z) {
//        super(EntityFightOrFlight.POKEMON_FLAMETHROWER_PROJECTILE.get(), x, y, z, level);
//        this.setNoGravity(true);
//    }

    @Override
    public void tick() {
        super.tick();

        // Despawn after a certain time
        if (this.tickCount > 60 && !this.level().isClientSide()) { // Despawn logic on server
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
}
