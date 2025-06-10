package me.rufia.fightorflight.entity.rangedAttackOutOfBattle;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import me.rufia.fightorflight.CobblemonFightOrFlight;
import net.minecraft.world.entity.LivingEntity;

public class ExplosiveAttack extends PokemonRangedAttack {

    public ExplosiveAttack(PokemonEntity owner, LivingEntity target) {
        super(owner, target);
    }

    @Override
    public void performRangedAttack() {
        CobblemonFightOrFlight.LOGGER.info("Ranged Attack Type: Explosive");
        //Should not be processed here.
    }
}
