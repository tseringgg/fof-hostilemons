package me.rufia.fightorflight.entity.rangedAttackOutOfBattle;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import me.rufia.fightorflight.entity.projectile.AbstractPokemonProjectile;
import me.rufia.fightorflight.entity.projectile.PokemonBullet;
import me.rufia.fightorflight.utils.PokemonUtils;
import net.minecraft.world.entity.LivingEntity;

import java.util.Random;

public class MultipleBulletAttack extends PokemonRangedAttack {
    public MultipleBulletAttack(PokemonEntity owner, LivingEntity target) {
        super(owner, target);
    }

    @Override
    public void performRangedAttack() {
        PokemonEntity pokemonEntity = this.owner;
//        CobblemonFightOrFlight.LOGGER.info("Ranged Attack Type: Bullet Move");
        Random rand = new Random();
        Move move = PokemonUtils.getRangeAttackMove(pokemonEntity);
        AbstractPokemonProjectile bullet;
        for (int i = 0; i < rand.nextInt(3) + 1; ++i) {
            bullet = new PokemonBullet(pokemonEntity.level(), pokemonEntity, target);
            shootProjectileEntity(pokemonEntity, target, bullet);
            addProjectileEntity(pokemonEntity, target, bullet, move);
        }
    }
}
