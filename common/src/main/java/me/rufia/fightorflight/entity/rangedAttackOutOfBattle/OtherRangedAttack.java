package me.rufia.fightorflight.entity.rangedAttackOutOfBattle;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import me.rufia.fightorflight.CobblemonFightOrFlight;
import me.rufia.fightorflight.entity.projectile.AbstractPokemonProjectile;
import me.rufia.fightorflight.entity.projectile.PokemonArrow;
import me.rufia.fightorflight.utils.PokemonUtils;
import net.minecraft.world.entity.LivingEntity;

public class OtherRangedAttack extends PokemonRangedAttack {
    public OtherRangedAttack(PokemonEntity owner, LivingEntity target) {
        super(owner, target);
    }

    @Override
    public void performRangedAttack() {
        PokemonEntity pokemonEntity = this.owner;
        CobblemonFightOrFlight.LOGGER.info("Ranged Attack Type: Other"); //
        Move move = PokemonUtils.getRangeAttackMove(pokemonEntity);
        AbstractPokemonProjectile bullet;
        bullet = new PokemonArrow(pokemonEntity.level(), pokemonEntity, target);
        shootProjectileEntity(pokemonEntity, target, bullet);
        if(move != null) {
            addProjectileEntity(pokemonEntity, target, bullet, move);
        } else {
            addProjectileEntity(pokemonEntity,target, bullet);
        }


    }
}
