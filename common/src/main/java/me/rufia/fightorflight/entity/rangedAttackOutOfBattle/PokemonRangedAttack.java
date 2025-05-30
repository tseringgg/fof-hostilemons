package me.rufia.fightorflight.entity.rangedAttackOutOfBattle;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import me.rufia.fightorflight.CobblemonFightOrFlight;
import me.rufia.fightorflight.entity.PokemonAttackEffect;
import me.rufia.fightorflight.entity.projectile.AbstractPokemonProjectile;
import me.rufia.fightorflight.utils.PokemonUtils;
import net.minecraft.world.entity.LivingEntity;

import java.util.Arrays;

public abstract class PokemonRangedAttack {
    public PokemonRangedAttack(){}
    public abstract void performRangedAttack(PokemonEntity pokemonEntity, LivingEntity target);
    protected static void addProjectileEntity(PokemonEntity pokemonEntity, LivingEntity target, AbstractPokemonProjectile projectile) {
        projectile.setElementalType(pokemonEntity.getPokemon().getPrimaryType().getName());
        projectile.setDamage(PokemonAttackEffect.calculatePokemonDamage(pokemonEntity, target, true));
        ((LivingEntity) pokemonEntity).level().addFreshEntity(projectile);
    }
    protected static void addProjectileEntity(PokemonEntity pokemonEntity, LivingEntity target, AbstractPokemonProjectile projectile, Move move) {
        projectile.setElementalType(move.getType().getName());
        projectile.setDamage(PokemonAttackEffect.calculatePokemonDamage(pokemonEntity, target, move));
        ((LivingEntity) pokemonEntity).level().addFreshEntity(projectile);
    }

    protected static void shootProjectileEntity(PokemonEntity pokemonEntity, LivingEntity target, AbstractPokemonProjectile projectile) {
        double d = target.getX() - pokemonEntity.getX();
        double e = target.getY(0.5) - projectile.getY();
        double f = target.getZ() - pokemonEntity.getZ();
        float velocity = 1.6f; // 1.6f is default
        projectile.accurateShoot(d, e, f, velocity, 0.1f);
    }

    public static PokemonRangedAttack createPokemonRangedAttack(String moveName) {
        var config = CobblemonFightOrFlight.moveConfig();
        if(moveName.equals("flamethrower")) return new ParticleStreamAttack();
        if(Arrays.stream(config.single_bullet_moves).toList().contains(moveName)) return new SingleBulletAttack();
        if(Arrays.stream(config.multiple_bullet_moves).toList().contains(moveName)) return new MultipleBulletAttack();
        if(Arrays.stream(config.single_tracing_bullet_moves).toList().contains(moveName)) return new SingleTracingBulletAttack();
        if(Arrays.stream(config.multiple_tracing_bullet_moves).toList().contains(moveName)) return new MultipleTracingBulletAttack();
        if(Arrays.stream(config.single_beam_moves).toList().contains(moveName)) return new SingleBeamAttack();
        if(PokemonUtils.isExplosiveMove(moveName)) return new ExplosiveAttack();
        if(Arrays.stream(config.sound_based_moves).toList().contains(moveName)) return new SoundBasedAttack();
        if(Arrays.stream(config.magic_attack_moves).toList().contains(moveName)) return new MagicAttackAttack();
        return new OtherRangedAttack();
    }
}
