package me.rufia.fightorflight.entity.rangedAttackOutOfBattle;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import me.rufia.fightorflight.entity.PokemonAttackEffect;
import me.rufia.fightorflight.entity.projectile.*;
import me.rufia.fightorflight.utils.PokemonUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class ParticleStreamAttack extends PokemonRangedAttack {
    private static final int NUM_PROJECTILES_IN_STREAM = 10;
    private static final int DELAY_BETWEEN_PROJECTILES_TICKS = 2; // Spawn a projectile every 2 ticks
    private static final float PROJECTILE_SPEED = 1.0f; // Speed of each invisible projectile
    private static final float SPREAD_FACTOR = 0f; // How much the projectiles spread out
    private static final float BASE_PROJECTILE_SPEED = 0.5f; // Default from shootProjectileEntity, can be adjusted

    @Override
    public void performRangedAttack(PokemonEntity attackerPokemon, LivingEntity target) {
        Move move = PokemonUtils.getRangeAttackMove(attackerPokemon);
        for(int i = 0; i < 10; i++) {
            attackerPokemon.after(i*0.2F, () -> {
                AbstractPokemonProjectile bullet = new InvisibleFlamethrowerHitProjectile(attackerPokemon.level(), attackerPokemon, move.getTemplate());
                Vec3 bulletDirection = attackerPokemon.getViewVector(1.0f);
                double d = bulletDirection.x;
                double e = bulletDirection.y;
                double f = bulletDirection.z;
                bullet.shoot(d, e, f, BASE_PROJECTILE_SPEED, SPREAD_FACTOR);
                bullet.setDamage(PokemonAttackEffect.calculatePokemonDamage(attackerPokemon, target, true));
                ((LivingEntity) attackerPokemon).level().addFreshEntity(bullet);
                return null;
            });
        }

    }
}
