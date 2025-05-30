package me.rufia.fightorflight.entity.rangedAttackOutOfBattle;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import me.rufia.fightorflight.CobblemonFightOrFlight;
import net.minecraft.world.entity.LivingEntity;

public class ExplosiveAttack extends PokemonRangedAttack {
    @Override
    public void performRangedAttack(PokemonEntity pokemonEntity, LivingEntity target) {
        CobblemonFightOrFlight.LOGGER.info("Ranged Attack Type: Explosive");
        //Should not be processed here.
    }
}
