package me.rufia.fightorflight.entity.rangedAttackOutOfBattle;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import me.rufia.fightorflight.entity.PokemonAttackEffect;
import me.rufia.fightorflight.entity.projectile.AbstractPokemonProjectile;
import me.rufia.fightorflight.entity.projectile.PokemonTracingBullet;
import me.rufia.fightorflight.utils.PokemonUtils;
import net.minecraft.world.entity.LivingEntity;

import java.util.Random;

public class MultipleTracingBulletAttack extends PokemonRangedAttack {
    @Override
    public void performRangedAttack(PokemonEntity pokemonEntity, LivingEntity target) {
//        CobblemonFightOrFlight.LOGGER.info("Ranged Attack Type: Tracing Bullet Move");
        Move move = PokemonUtils.getRangeAttackMove(pokemonEntity);
        AbstractPokemonProjectile bullet;
        Random rand = new Random();
        for (int i = 0; i < rand.nextInt(3) + 1; ++i) {
            bullet = new PokemonTracingBullet(pokemonEntity.level(), pokemonEntity, target, pokemonEntity.getDirection().getAxis());

            bullet.setElementalType(move.getType().getName());
            bullet.setDamage(PokemonAttackEffect.calculatePokemonDamage(pokemonEntity, target, move));
            ((LivingEntity) pokemonEntity).level().addFreshEntity(bullet);
        }
    }
}
