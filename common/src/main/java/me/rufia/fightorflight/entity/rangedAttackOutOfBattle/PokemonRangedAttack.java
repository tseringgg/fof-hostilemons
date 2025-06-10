package me.rufia.fightorflight.entity.rangedAttackOutOfBattle;

import com.bedrockk.molang.runtime.MoLangRuntime;
import com.cobblemon.mod.common.api.molang.MoLangFunctions;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.moves.animations.ActionEffectContext;
import com.cobblemon.mod.common.api.moves.animations.ActionEffectTimeline;
import com.cobblemon.mod.common.api.moves.animations.TargetsProvider;
import com.cobblemon.mod.common.api.moves.animations.UsersProvider;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import kotlin.Unit;
import me.rufia.fightorflight.CobblemonFightOrFlight;
import me.rufia.fightorflight.entity.PokemonAttackEffect;
import me.rufia.fightorflight.entity.projectile.AbstractPokemonProjectile;
import me.rufia.fightorflight.utils.PokemonUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/*
* Pokemon attacks should never be interrupted by another attack call. Control attack timeline (delayed actions)
* and lock the pokemon into an "isAttacking" state in this class to prevent interrupting attack calls.
* */

public abstract class PokemonRangedAttack {
    PokemonEntity owner;
    LivingEntity target;
    public PokemonRangedAttack(PokemonEntity owner, LivingEntity target){
        this.owner = owner;
        this.target = target;
    }
    public void performRangedAttack(){
        PokemonUtils.sendAnimationPacket(owner, "special");

//        var timeline = triggerActionEffectTimeline(pokemonEntity, target); // Call ActionEffectTimeline for Cobblemon Animation

    }
    public float getDuration() {
        return 0F;
    }
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

    public static PokemonRangedAttack createPokemonRangedAttack(PokemonEntity owner, LivingEntity target) {
        Move move = PokemonUtils.getRangeAttackMove(owner);
        String moveName = move == null ? "" : move.getName();
        var config = CobblemonFightOrFlight.moveConfig();
        if(moveName.equals("flamethrower")) return new ParticleStreamAttack(owner, target);
        if(Arrays.stream(config.single_bullet_moves).toList().contains(moveName)) return new SingleBulletAttack(owner, target);
        if(Arrays.stream(config.multiple_bullet_moves).toList().contains(moveName)) return new MultipleBulletAttack(owner, target);
        if(Arrays.stream(config.single_tracing_bullet_moves).toList().contains(moveName)) return new SingleTracingBulletAttack(owner, target);
        if(Arrays.stream(config.multiple_tracing_bullet_moves).toList().contains(moveName)) return new MultipleTracingBulletAttack(owner, target);
        if(Arrays.stream(config.single_beam_moves).toList().contains(moveName)) return new SingleBeamAttack(owner, target);
        if(PokemonUtils.isExplosiveMove(moveName)) return new ExplosiveAttack(owner, target);
        if(Arrays.stream(config.sound_based_moves).toList().contains(moveName)) return new SoundBasedAttack(owner, target);
        if(Arrays.stream(config.magic_attack_moves).toList().contains(moveName)) return new RangedMagicAttack(owner, target);
        return new OtherRangedAttack(owner, target);
    }


    private static CompletableFuture<Unit> triggerActionEffectTimeline(PokemonEntity pokemonEntity, LivingEntity target) {
        if(target == null) {
            CobblemonFightOrFlight.LOGGER.info("No target found: Canceled ActionEffect");
            return null;
        }
        String moveName = PokemonUtils.getRangeAttackMove(pokemonEntity).getName();
        Level level = pokemonEntity.level();

        if (level.isClientSide()) {
            return null; // All logic here is server-side for initiating the effect
        }

        MoveTemplate moveTemplate = Moves.INSTANCE.getByName(moveName);

        ActionEffectTimeline actionEffect = moveTemplate.getActionEffect();

        // --- Step 3: Construct Providers ---
        List<Object> providers = new ArrayList<>();
        providers.add(new UsersProvider(pokemonEntity));
        if (target.isAlive()) {
            providers.add(new TargetsProvider(target));
        }

        // --- Step 4: Construct MoLangRuntime ---
        MoLangRuntime runtime = new MoLangRuntime();
        MoLangFunctions.INSTANCE.addStandardFunctions(runtime.getEnvironment().query); // Use Cobblemon's helper

        // Attempt to add the 'move' query function.
        // You MUST verify how `move.struct` is correctly accessed from MoveTemplate.
        // It might be a public field `moveTemplate.struct` or a getter `moveTemplate.getStruct()`.
        // This is a common point of failure if not accessed correctly.
        runtime.getEnvironment().query.addFunction("move", params -> moveTemplate.getStruct());

        ActionEffectContext context = new ActionEffectContext(
                actionEffect,
                new HashSet<>(),  // holds (default: empty mutable set)
                providers,        // your providers list
                runtime,          // your runtime
                false,            // canBeInterrupted (default: false)
                false,            // interrupted (default: false)
                new ArrayList<>() // currentKeyframes (default: empty mutable list of ActionEffectKeyframe)
                // Note: ActionEffectKeyframe is likely abstract, so new ArrayList<>() is fine.
        );

        return actionEffect.run(context);
    }
}
