package me.rufia.fightorflight.mixin;


import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.pokemon.experience.SidemodExperienceSource;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import me.rufia.fightorflight.CobblemonFightOrFlight;
import me.rufia.fightorflight.PokemonInterface;
import me.rufia.fightorflight.entity.PokemonAttackEffect;
import me.rufia.fightorflight.goals.targeting.*;
import me.rufia.fightorflight.item.component.PokeStaffComponent;
import me.rufia.fightorflight.utils.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.ShoulderRidingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

@Mixin(PokemonEntity.class)
public abstract class PokemonEntityMixin extends Mob implements PokemonInterface {
    @Shadow(remap = false)
    public abstract void cry();

    @Shadow(remap = false)
    public abstract Pokemon getPokemon();

    @Unique
    @Nullable
    private LivingEntity fightorflight$clientSideCachedAttackTarget;
    @Unique
    private static final EntityDataAccessor<Integer> DATA_ID_ATTACK_TARGET;
    @Unique
    private static final EntityDataAccessor<Integer> DATA_ID_CAPTURED_BY;
    @Unique
    private static final EntityDataAccessor<Integer> ATTACK_TIME;
    @Unique
    private static final EntityDataAccessor<Integer> MAX_ATTACK_TIME;
    @Unique
    private static final EntityDataAccessor<String> MOVE;
    @Unique
    private static final EntityDataAccessor<Integer> CRY_CD;
    @Unique
    private static final EntityDataAccessor<String> COMMAND;
    @Unique
    private static final EntityDataAccessor<String> COMMAND_DATA;
    @Unique
    private static final EntityDataAccessor<BlockPos> TARGET_BLOCK_POS;

    static {
        DATA_ID_ATTACK_TARGET = SynchedEntityData.defineId(PokemonEntityMixin.class, EntityDataSerializers.INT);
        DATA_ID_CAPTURED_BY = SynchedEntityData.defineId(PokemonEntityMixin.class, EntityDataSerializers.INT);
        ATTACK_TIME = SynchedEntityData.defineId(PokemonEntityMixin.class, EntityDataSerializers.INT);
        MAX_ATTACK_TIME = SynchedEntityData.defineId(PokemonEntityMixin.class, EntityDataSerializers.INT);
        MOVE = SynchedEntityData.defineId(PokemonEntityMixin.class, EntityDataSerializers.STRING);
        CRY_CD = SynchedEntityData.defineId(PokemonEntityMixin.class, EntityDataSerializers.INT);
        COMMAND = SynchedEntityData.defineId(PokemonEntityMixin.class, EntityDataSerializers.STRING);
        COMMAND_DATA = SynchedEntityData.defineId(PokemonEntityMixin.class, EntityDataSerializers.STRING);
        TARGET_BLOCK_POS = SynchedEntityData.defineId(PokemonEntityMixin.class, EntityDataSerializers.BLOCK_POS);
    }

    protected void createTargetBlockPos() {
        String data = this.getCommandData();
        BlockPos blockPos = BlockPos.ZERO;
        if (data.startsWith("POS_")) {
            if (data.equals("POS_SELF")) {
                blockPos = new BlockPos(getBlockX(), getBlockZ(), getBlockZ());
            }
        } else {
            Vec3i vec3i = FOFUtils.stringToVec3i(data);
            if (vec3i != null) {
                blockPos = new BlockPos(vec3i.getX(), vec3i.getY(), vec3i.getZ());
            }
        }
        setTargetBlockPos(blockPos);
    }

    protected PokemonEntityMixin(EntityType<? extends ShoulderRidingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public LivingEntity getTarget() {
        if (this.level().isClientSide) {
            if (fightorflight$clientSideCachedAttackTarget != null) {
                return fightorflight$clientSideCachedAttackTarget;
            } else {
                Entity entity = this.level().getEntity((Integer) this.entityData.get(DATA_ID_ATTACK_TARGET));
                if (entity instanceof LivingEntity) {
                    fightorflight$clientSideCachedAttackTarget = (LivingEntity) entity;
                    return fightorflight$clientSideCachedAttackTarget;
                }
            }
        }
        return super.getTarget();
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    protected void registerFOFGoals(CallbackInfo ci) {
        PokemonEntity pokemonEntity = (PokemonEntity) (Object) this;
        targetSelector.addGoal(1, new PokemonCommandedTargetGoal<>(pokemonEntity, LivingEntity.class, false));
        targetSelector.addGoal(2, new PokemonOwnerHurtByTargetGoal(pokemonEntity));
        targetSelector.addGoal(3, new PokemonOwnerHurtTargetGoal(pokemonEntity));
        targetSelector.addGoal(3, new PokemonTauntedTargetGoal<>(pokemonEntity, PokemonEntity.class, false));
        targetSelector.addGoal(4, new HurtByTargetGoal(pokemonEntity));
        targetSelector.addGoal(4, new CaughtByTargetGoal(pokemonEntity));
        targetSelector.addGoal(5, new PokemonNearestAttackableTargetGoal<>(pokemonEntity, Player.class, PokemonUtils.getAttackRadius() * 3, true, true));
        targetSelector.addGoal(5, new PokemonProactiveTargetGoal<>(pokemonEntity, Mob.class, 5, false, false, (arg) -> arg instanceof Enemy && !(!CobblemonFightOrFlight.commonConfig().do_pokemon_defend_creeper_proactive && arg instanceof Creeper)));
    }

    @Inject(method = "onSyncedDataUpdated", at = @At("TAIL"))
    public void onSyncedDataUpdated(EntityDataAccessor<?> key, CallbackInfo ci) {
        if (DATA_ID_ATTACK_TARGET.equals(key)) {
            this.fightorflight$clientSideCachedAttackTarget = null;
        }
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    protected void defineSynchedData(SynchedEntityData.Builder builder, CallbackInfo callbackInfo) {
        builder.define(DATA_ID_ATTACK_TARGET, 0);
        builder.define(DATA_ID_CAPTURED_BY, 0);
        builder.define(ATTACK_TIME, 0);
        builder.define(MAX_ATTACK_TIME, -1);
        builder.define(MOVE, "");
        builder.define(CRY_CD, 0);
        builder.define(COMMAND, "");
        builder.define(COMMAND_DATA, "");
        builder.define(TARGET_BLOCK_POS, BlockPos.ZERO);
        PokemonEntity.Companion.createAttributes();
    }

    @Inject(method = "saveWithoutId", at = @At("HEAD"))
    private void writeAdditionalNbt(CompoundTag compoundTag, CallbackInfoReturnable<Boolean> ci) {
        compoundTag.putInt(CRY_CD.toString(), 0);
        compoundTag.putString(COMMAND.toString(), getCommand());
        compoundTag.putString(COMMAND_DATA.toString(), getCommandData());
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void readAdditionalNbt(CompoundTag compoundTag, CallbackInfo ci) {
        entityData.set(CRY_CD, compoundTag.getInt(CRY_CD.toString()));
    }

    public void setTarget(LivingEntity target) {
        super.setTarget(target);
        if (target != null) {
            this.entityData.set(DATA_ID_ATTACK_TARGET, target.getId());
        }
    }

    @Override
    public int getAttackTime() {
        return entityData.get(ATTACK_TIME);
    }

    @Override
    public void setAttackTime(int val) {
        entityData.set(ATTACK_TIME, val);
    }

    @Override
    public int getMaxAttackTime() {
        return entityData.get(MAX_ATTACK_TIME);
    }

    @Override
    public void setMaxAttackTime(int val) {
        entityData.set(MAX_ATTACK_TIME, val);
    }

    @Override
    public boolean usingBeam() {
        if (getCurrentMove().isEmpty()) {
            return false;
        }
        return Arrays.stream(CobblemonFightOrFlight.moveConfig().single_beam_moves).toList().contains(getCurrentMove());
    }

    @Override
    public boolean usingSound() {
        if (getCurrentMove().isEmpty()) {
            return false;
        }
        return Arrays.stream(CobblemonFightOrFlight.moveConfig().sound_based_moves).toList().contains(getCurrentMove());
    }

    @Override
    public boolean usingMagic() {
        if (getCurrentMove().isEmpty()) {
            return false;
        }
        return Arrays.stream(CobblemonFightOrFlight.moveConfig().magic_attack_moves).toList().contains(getCurrentMove());
    }

    @Override
    public void setCurrentMove(Move move) {
        entityData.set(MOVE, move.getName());
    }

    @Override
    public String getCurrentMove() {
        return entityData.get(MOVE);
    }

    @Override
    public int getNextCryTime() {
        return this.entityData.get(CRY_CD);
    }

    @Override
    public void setNextCryTime(int time) {
        this.entityData.set(CRY_CD, time);
    }

    @Override
    public void setCommand(String cmd) {
        entityData.set(COMMAND, cmd);
    }

    @Override
    public String getCommand() {
        return entityData.get(COMMAND);
    }

    @Override
    public void setCommandData(String cmdData) {
        entityData.set(COMMAND_DATA, cmdData);
        createTargetBlockPos();
    }

    @Override
    public String getCommandData() {
        return entityData.get(COMMAND_DATA);
    }

    @Override
    public BlockPos getTargetBlockPos() {
        return this.entityData.get(TARGET_BLOCK_POS);
    }

    @Override
    public void setTargetBlockPos(BlockPos blockPos) {
        this.entityData.set(TARGET_BLOCK_POS, blockPos);
    }

    @Override
    public int getCapturedBy() {
        return entityData.get(DATA_ID_CAPTURED_BY);
    }

    @Override
    public void setCapturedBy(int id) {
        entityData.set(DATA_ID_CAPTURED_BY, id);
    }

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private float hurtDamageTweak(float amount) {
        if (PokemonUtils.shouldRetreat((PokemonEntity) (Object) this)) {
            PokemonAttackEffect.pokemonRecallWithAnimation((PokemonEntity) (Object) this);
            return 0;
        }
        PokemonMultipliers pokemonMultipliers = new PokemonMultipliers((PokemonEntity) (Object) this);
        Pokemon pokemon = getPokemon();
        float def = Math.max(pokemon.getDefence(), pokemon.getSpecialDefence());
        return amount * (1 - pokemonMultipliers.getMaximumDamageReduction() * Math.min(CobblemonFightOrFlight.commonConfig().max_damage_reduction_multiplier, Mth.lerp(def / CobblemonFightOrFlight.commonConfig().defense_stat_limit, 0, CobblemonFightOrFlight.commonConfig().max_damage_reduction_multiplier)));
        //CobblemonFightOrFlight.LOGGER.info(String.format("base dmg:%f,reduced dmg:%f",amount,amount1));
    }

    @Override
    protected void actuallyHurt(DamageSource damageSource, float damageAmount) {
        float prevHealth = getHealth();
        super.actuallyHurt(damageSource, damageAmount);
        if (PokemonUtils.isUsingNewHealthMechanic() && invulnerableTime == 20) {
            float newHealth = getHealth();
            float d = newHealth - prevHealth;
            if (d < 0) {
                PokemonUtils.entityHpToPokemonHp((PokemonEntity) (Object) this, -d, false);
            }
        }
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void hurtImmune(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (CobblemonFightOrFlight.commonConfig().suffocation_immunity && source.type().equals(damageSources().inWall().type())) {
            cir.setReturnValue(false);
        }
        if (source.getEntity() instanceof LivingEntity livingEntity) {
            if (!PokemonAttackEffect.shouldBeHurtByAllyMob(((PokemonEntity) (Object) this), livingEntity)) {
                cir.setReturnValue(false);
            }
        }
        if (CobblemonFightOrFlight.commonConfig().slow_down_after_hurt) {
            if (!getPokemon().isPlayerOwned()) {
                addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0));
            }
        }
    }

    @Override
    public void heal(float healAmount) {
        if (PokemonUtils.isUsingNewHealthMechanic()) {
            PokemonUtils.entityHpToPokemonHp((PokemonEntity) (Object) this, healAmount, true);
        }
        super.heal(healAmount);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(CallbackInfo ci) {
        if (Objects.equals(getCommand(), PokeStaffComponent.CMDMODE.CLEAR.name())) {
            setCommand(PokeStaffComponent.CMDMODE.NOCMD.name());
        }
        var targetEntity = getTarget();
        if (targetEntity != null && targetEntity.isAlive()) {
            if (getNextCryTime() == 0) {
                this.cry();
                if (CobblemonFightOrFlight.commonConfig().multiple_cries) {
                    setNextCryTime(CobblemonFightOrFlight.commonConfig().time_to_cry_again);
                } else {
                    setNextCryTime(-1);
                }
            }
        } else {
            setNextCryTime(0);
        }
        if (getNextCryTime() >= 0) {
            setNextCryTime(getNextCryTime() - 1);
        }
        if (getAttackTime() > 0) {
            setAttackTime(getAttackTime() - 1);
        }

    }

    @Inject(method = "dropAllDeathLoot", at = @At("TAIL"))
    private void dropAllDeathLootInject(ServerLevel world, DamageSource source, CallbackInfo ci) {
        if (getLastHurtByMob() instanceof PokemonEntity pokemonEntity) {
            if (pokemonEntity.getOwner() != null) {
                PokemonEntity self = (PokemonEntity) (Object) this;
                pokemonEntity.getPokemon().addExperience(new SidemodExperienceSource(CobblemonFightOrFlight.MODID), FOFExpCalculator.calculate(pokemonEntity.getPokemon(), self.getPokemon()));
                if (CobblemonFightOrFlight.commonConfig().can_gain_ev) {
                    var map = FOFEVCalculator.calculate(pokemonEntity.getPokemon(), self.getPokemon());
                    for (Map.Entry<Stat, Integer> entry : map.entrySet()) {
                        pokemonEntity.getPokemon().getEvs().add(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
    }
}
