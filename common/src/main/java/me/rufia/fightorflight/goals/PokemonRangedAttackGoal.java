package me.rufia.fightorflight.goals;

import com.bedrockk.molang.runtime.MoLangRuntime;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.molang.MoLangFunctions;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.moves.animations.ActionEffectContext;
import com.cobblemon.mod.common.api.moves.animations.ActionEffectTimeline;
import com.cobblemon.mod.common.api.moves.animations.TargetsProvider;
import com.cobblemon.mod.common.api.moves.animations.UsersProvider;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.activestate.ShoulderedState;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import me.rufia.fightorflight.CobblemonFightOrFlight;
import me.rufia.fightorflight.PokemonInterface;
import me.rufia.fightorflight.entity.PokemonAttackEffect;
import me.rufia.fightorflight.entity.projectile.AbstractPokemonProjectile;
import me.rufia.fightorflight.entity.projectile.PokemonArrow;
import me.rufia.fightorflight.entity.projectile.PokemonBullet;
import me.rufia.fightorflight.entity.projectile.PokemonTracingBullet;
import me.rufia.fightorflight.entity.rangedAttackOutOfBattle.PokemonRangedAttack;
import me.rufia.fightorflight.utils.PokemonUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PokemonRangedAttackGoal extends PokemonAttackGoal {
    private final PokemonEntity pokemonEntity;
    private final LivingEntity livingEntity;
    @Nullable
    private LivingEntity target;
    private final double speedModifier;
    private int seeTime;

    private boolean strafingClockwise;
    private boolean strafingBackwards;
    private int strafingTime = -1;

    private final float attackRadius;
    private final float attackRadiusSqr;
    private boolean isAttacking = false;

    public PokemonRangedAttackGoal(LivingEntity pokemonEntity, double speedModifier, float attackRadius) {
        setAttackTime(-1);
        this.livingEntity = pokemonEntity;
        if (!(pokemonEntity instanceof PokemonEntity)) {
            throw new IllegalArgumentException("PokemonRangedAttackGoal requires a PokemonEntity");
        } else {
            this.pokemonEntity = (PokemonEntity) pokemonEntity;
            this.speedModifier = speedModifier;

            this.attackRadius = attackRadius;
            this.attackRadiusSqr = attackRadius * attackRadius;

            this.setFlags(EnumSet.of(Goal.Flag.LOOK, Goal.Flag.MOVE));


        }
    }

    public boolean canUse() {
        if (!PokemonUtils.shouldShoot(pokemonEntity) || PokemonUtils.moveCommandAvailable(pokemonEntity)) {
            return false;
        }
        if (pokemonEntity.getPokemon().getState() instanceof ShoulderedState) {
            return false;
        }
        LivingEntity livingEntity = this.pokemonEntity.getTarget();
        if (livingEntity != null && livingEntity.isAlive()) {
            this.target = livingEntity;
            return PokemonUtils.shouldFightTarget(pokemonEntity);
        } else {
            return false;
        }
    }

    public boolean canContinueToUse() {
        if (target == null) {
            return false;
        }
        return (this.canUse() || !this.pokemonEntity.getNavigation().isDone()) && !isTargetInBattle();
    }

    public void stop() {
        this.target = null;
        this.seeTime = 0;
        setAttackTime(-1);
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public boolean isTargetInBattle() {
        if (this.pokemonEntity.getTarget() instanceof ServerPlayer targetAsPlayer) {
            return BattleRegistry.INSTANCE.getBattleByParticipatingPlayer(targetAsPlayer) != null;
        }
        return false;
    }

    public void tick() {
        super.tick();
        if (!CobblemonFightOrFlight.commonConfig().do_pokemon_attack_in_battle) {
            if (isTargetInBattle()) {
                this.pokemonEntity.getNavigation().setSpeedModifier(0);
            }
        }
        if (target == null) {
            return;
        }
        // strafing
        double distanceFromTarget = this.pokemonEntity.distanceToSqr(this.target.getX(), this.target.getY(), this.target.getZ());
        boolean hasLineOfSight = this.pokemonEntity.getSensing().hasLineOfSight(this.target);
        if (hasLineOfSight) {
            ++this.seeTime;
        } else {
            seeTime = 0;
            resetAttackTime(distanceFromTarget);
        }
        if (!(distanceFromTarget > (double) this.attackRadiusSqr) && this.seeTime >= 5 && hasLineOfSight) {
            this.pokemonEntity.getNavigation().stop();
            ++strafingTime;
        } else {
            CobblemonFightOrFlight.LOGGER.info(this.pokemonEntity.getPokemon().getSpecies().getName() + " is trying to fly towards " + this.target.getName());
            this.pokemonEntity.getNavigation().moveTo(this.target, this.speedModifier);
            strafingTime = -1;
        }
        if (this.strafingTime >= 10) {
            if ((double) this.pokemonEntity.getRandom().nextFloat() < 0.3) {
                this.strafingClockwise = !this.strafingClockwise;
            }
            if ((double) this.pokemonEntity.getRandom().nextFloat() < 0.3) {
                this.strafingBackwards = !this.strafingBackwards;
            }
            this.strafingTime = 0;
        }
        if (this.strafingTime > -1) {
            if (distanceFromTarget > (double) (this.attackRadiusSqr * 0.8F)) {
                this.strafingBackwards = false;
            } else if (distanceFromTarget < (double) (this.attackRadiusSqr * 0.2F)) {
                this.strafingBackwards = true;
            }
//            this.pokemonEntity.getMoveControl().strafe(this.strafingBackwards ? -0.5F : 0.5F, this.strafingClockwise ? 0.5F : -0.5F);
            Entity vehicle = this.pokemonEntity.getControlledVehicle();
            if (vehicle instanceof Mob mob) {
                mob.lookAt(livingEntity, 30.0F, 30.0F);
            }
        }

        this.pokemonEntity.getLookControl().setLookAt(this.target);

        // produce particles
        int attackTime = getAttackTime();
        if (attackTime == 7 && ((PokemonInterface) pokemonEntity).usingSound()) {
            PokemonUtils.createSonicBoomParticle(pokemonEntity, target);
        }
        if (attackTime % 5 == 0 && ((PokemonInterface) pokemonEntity).usingMagic()) {
            PokemonAttackEffect.makeMagicAttackParticle(pokemonEntity, target);
        }
        // perform attack
        if (attackTime == 0) {
            if (!hasLineOfSight) {
                return;
            }
            resetAttackTime(distanceFromTarget);
            Vec3 aimPosition = target.getEyePosition();
            double dX = aimPosition.x() - pokemonEntity.getX();
            double dY = aimPosition.y() - pokemonEntity.getEyeY(); // Delta from attacker's eye level
            double dZ = aimPosition.z() - pokemonEntity.getZ();
            double horizontalDistance = Math.sqrt(dX * dX + dZ * dZ);

            float targetYaw = (float) (Mth.atan2(dZ, dX) * (180.0D / Math.PI)) - 90.0F;
            float targetPitch = (float) (-(Mth.atan2(dY, horizontalDistance) * (180.0D / Math.PI)));
        pokemonEntity.setYBodyRot(targetYaw); // body rotation matches head rotation
//            pokemonEntity.setYBodyRot(pokemonEntity.yBodyRot - 90.0F);
            this.performRangedAttack(this.target);
        } else if (attackTime < 0) {
            resetAttackTime(distanceFromTarget);
        }
    }

    @Override
    protected PokemonEntity getPokemonEntity() {
        return pokemonEntity;
    }

    private void performRangedAttack(LivingEntity target) {
        CobblemonFightOrFlight.LOGGER.info("Attempting to perform attack, isAttacking: " + isAttacking);
        if(isAttacking) {
            CobblemonFightOrFlight.LOGGER.info("Attempt FAIL, already attacking.");
            return;
        }
        CobblemonFightOrFlight.LOGGER.info("Attempt SUCCESS, starting attack.");
        isAttacking = true;

        var rangedAttack = PokemonRangedAttack.createPokemonRangedAttack(this.pokemonEntity, target);

        rangedAttack.performRangedAttack();
//        var timeline = triggerActionEffectTimeline(pokemonEntity, target);

        float duration = rangedAttack.getDuration();

        var future = pokemonEntity.delayedFuture(duration);
        future.whenComplete((s, e)->{
            isAttacking = false;
        });
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
