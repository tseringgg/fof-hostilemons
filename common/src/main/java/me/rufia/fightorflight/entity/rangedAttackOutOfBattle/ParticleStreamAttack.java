package me.rufia.fightorflight.entity.rangedAttackOutOfBattle;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.activestate.InactivePokemonState;
import com.ibm.icu.text.MessagePattern;
import me.rufia.fightorflight.CobblemonFightOrFlight;
import me.rufia.fightorflight.entity.PokemonAttackEffect;
import me.rufia.fightorflight.entity.projectile.*;
import me.rufia.fightorflight.net.packet.S2CPlayInhaleEffectPacket;
import me.rufia.fightorflight.utils.PokemonUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class ParticleStreamAttack extends PokemonRangedAttack {
    private static final int NUM_PROJECTILES_IN_STREAM = 10;
    private static final int DELAY_BETWEEN_PROJECTILES_TICKS = 2; // Spawn a projectile every 2 ticks
    private static final float PROJECTILE_SPEED = 1.0f; // Speed of each invisible projectile
    private static final float SPREAD_FACTOR = 0.2f; // How much the projectiles spread out
    private static final float BASE_PROJECTILE_SPEED = 1.0f; // Default from shootProjectileEntity, can be adjusted

    public ParticleStreamAttack(PokemonEntity owner, LivingEntity target){
        super(owner, target);
    }

    @Override
    public float getDuration(){
        return 1.15F + 0.6F + 0.1F + 1.9F;
    }

    @Override
    public void performRangedAttack() {
        super.performRangedAttack();
        delayAction(0F, this::chargeAttackAnimation);
        delayAction(1.15F, this::shootBullets);
//        delayAction(1.15F, this::playShootAnimation);
    }

    private void delayAction(float delay, Runnable action){
        var future = owner.delayedFuture(delay);
        future.whenComplete((s,e)->{
            action.run();
        });
    }
    private void playShootAnimation(){
        PokemonEntity attackerPokemon = this.owner;
        Level level = attackerPokemon.level();
        // This method is server-side where the decision to charge is made
        if (level.isClientSide() /* || !(level instanceof ServerLevel) - this check is better done by the network handler */) {
            return;
        }

        CobblemonFightOrFlight.LOGGER.info("FoF Common: {} starting chargeAttack visual via NetworkHandler.", attackerPokemon.getPokemon().getSpecies().getName());

        ResourceLocation inhaleEffectId = ResourceLocation.fromNamespaceAndPath(Cobblemon.MODID, "flamethrower_actor");
        String locatorName = "mouth"; // "mouth" or "special_attack", "face"
        float duration = 1.25f;

        // Use the loaded NetworkHandler
        CobblemonFightOrFlight.NETWORK_HANDLER.sendPlayInhaleEffectPacketToClients(
                attackerPokemon,
                inhaleEffectId,
                locatorName,
                duration
        );
    }

    private void chargeAttackAnimation(){
        PokemonEntity attackerPokemon = this.owner;
        Level level = attackerPokemon.level();
        // This method is server-side where the decision to charge is made
        if (level.isClientSide() /* || !(level instanceof ServerLevel) - this check is better done by the network handler */) {
            return;
        }
        if(attackerPokemon.getPokemon().getState() instanceof InactivePokemonState) {
            CobblemonFightOrFlight.LOGGER.info("Pokemon was recalled, inhale animation cancelled.");
            return;
        }

        CobblemonFightOrFlight.LOGGER.info("FoF Common: {} starting chargeAttack visual via NetworkHandler.", attackerPokemon.getPokemon().getSpecies().getName());

        ResourceLocation inhaleEffectId = ResourceLocation.fromNamespaceAndPath(CobblemonFightOrFlight.MODID, "inhale_charge");
        String locatorName = "special_attack"; // "mouth" or "special_attack", "face"
        float duration = 1.0f;

        // Use the loaded NetworkHandler
        CobblemonFightOrFlight.NETWORK_HANDLER.sendPlayInhaleEffectPacketToClients(
                attackerPokemon,
                inhaleEffectId,
                locatorName,
                duration
        );
    }

    private void shootBullets() {
        PokemonEntity attackerPokemon = this.owner;
        LivingEntity target = this.target;



        Move move = PokemonUtils.getRangeAttackMove(attackerPokemon);
        for(int i = 0; i < 40; i++) {
            if(attackerPokemon.getBeamMode() != 0) {
//                CobblemonFightOrFlight.LOGGER.info("Pokemon was recalled, ParticleStreamAttack cancelled.");
                break;
            }

            Random random = new Random();
            attackerPokemon.after(i*0.03F, () -> {
                Vec3 aimDirection;
                if(attackerPokemon.getBeamMode() != 0) {
//                    CobblemonFightOrFlight.LOGGER.info("Pokemon was recalled, ParticleStreamAttack cancelled.");
                    return null;
                }
                if(!attackerPokemon.isAttackable()) {
                    return null;
                }
                if (target != null && target.isAlive()) {
                    // Calculate vector from projectile origin to target's approximate center/upper body
                    Vec3 targetCenter = target.getBoundingBox().getCenter();
                    // Or, for a slightly higher aim point on the target:
                    // Vec3 targetAimPoint = targetEntity.position().add(0, targetEntity.getBbHeight() * 0.75, 0);
                    Vec3 targetAimPoint = target.getBoundingBox().getCenter(); // Aiming for eye level is often good

                    aimDirection = targetAimPoint.subtract(attackerPokemon.getBoundingBox().getCenter()).normalize();
                } else {
                    // If no target, use the Pokemon's current look vector
                    aimDirection = attackerPokemon.getViewVector(1.0f);
                }
                if (SPREAD_FACTOR > 0.001f) {
                    Vec3 spreadOffset = new Vec3(
                            (random.nextDouble() - 0.5) * SPREAD_FACTOR,
                            (random.nextDouble() - 0.5) * SPREAD_FACTOR, // Spread can also affect Y
                            (random.nextDouble() - 0.5) * SPREAD_FACTOR
                    );
                    aimDirection = aimDirection.add(spreadOffset).normalize();
                }

                AbstractPokemonProjectile bullet = new InvisibleFlamethrowerHitProjectile(attackerPokemon.level(), attackerPokemon, move.getTemplate());
                Vec3 bulletDirection = attackerPokemon.getViewVector(1.0f);
                double d = aimDirection.x;
                double e = aimDirection.y;
                double f = aimDirection.z;

                bullet.shoot(d, e, f, BASE_PROJECTILE_SPEED, SPREAD_FACTOR);
                bullet.setDamage(PokemonAttackEffect.calculatePokemonDamage(attackerPokemon, target, true));
                ((LivingEntity) attackerPokemon).level().addFreshEntity(bullet);
                return null;
            });
        }
    }
}
