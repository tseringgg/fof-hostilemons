package me.rufia.fightorflight.entity.rangedAttackOutOfBattle;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import me.rufia.fightorflight.entity.PokemonAttackEffect;
import me.rufia.fightorflight.utils.PokemonUtils;
import net.minecraft.world.entity.LivingEntity;

public class SoundBasedAttack extends PokemonRangedAttack {
    @Override
    public void performRangedAttack(PokemonEntity pokemonEntity, LivingEntity target) {
//        CobblemonFightOrFlight.LOGGER.info("Ranged Attack Type: Single Beam, Sound Based, or Magic Attack");
        Move move = PokemonUtils.getRangeAttackMove(pokemonEntity);
        if (!PokemonUtils.pokemonTryForceEncounter(pokemonEntity, target)) {
            boolean success = target.hurt(pokemonEntity.damageSources().mobAttack(pokemonEntity), PokemonAttackEffect.calculatePokemonDamage(pokemonEntity, target, move));
            PokemonUtils.setHurtByPlayer(pokemonEntity, target);
            PokemonAttackEffect.applyOnHitVisualEffect(pokemonEntity, target, move);
            PokemonAttackEffect.applyPostEffect(pokemonEntity, target, move, success);
        }
    }
}
