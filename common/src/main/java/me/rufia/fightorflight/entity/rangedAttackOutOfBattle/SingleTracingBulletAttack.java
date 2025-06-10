package me.rufia.fightorflight.entity.rangedAttackOutOfBattle;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import me.rufia.fightorflight.entity.projectile.AbstractPokemonProjectile;
import me.rufia.fightorflight.entity.projectile.PokemonTracingBullet;
import me.rufia.fightorflight.utils.PokemonUtils;
import net.minecraft.world.entity.LivingEntity;

public class SingleTracingBulletAttack extends PokemonRangedAttack {
    public SingleTracingBulletAttack(PokemonEntity owner, LivingEntity target) {
        super(owner, target);
    }

    @Override
    public void performRangedAttack() {
        PokemonEntity pokemonEntity = this.owner;
//        CobblemonFightOrFlight.LOGGER.info("Ranged Attack Type: Tracing Bullet Move");
        Move move = PokemonUtils.getRangeAttackMove(pokemonEntity);
        AbstractPokemonProjectile bullet;
        for (int i = 0; i < 1; ++i) {
            bullet = new PokemonTracingBullet(pokemonEntity.level(), pokemonEntity, target, pokemonEntity.getDirection().getAxis());
            addProjectileEntity(pokemonEntity, target, bullet, move);
        }
    }
}
