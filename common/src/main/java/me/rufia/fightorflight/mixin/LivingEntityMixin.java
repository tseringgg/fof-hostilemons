package me.rufia.fightorflight.mixin;

import me.rufia.fightorflight.CobblemonFightOrFlight;
import me.rufia.fightorflight.effects.FOFEffects;
import net.minecraft.core.Holder;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow
    @Nullable
    public abstract MobEffectInstance getEffect(Holder<MobEffect> effect);

    @Shadow
    public abstract boolean hasEffect(Holder<MobEffect> effect);

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private float updateDmg(float damageAmount) {
        if (hasEffect(FOFEffects.RESISTANCE_WEAKENED)) {
            int amp = -1;
            var effect = getEffect(FOFEffects.RESISTANCE_WEAKENED);
            if (effect != null) {
                //CobblemonFightOrFlight.LOGGER.info("EFFECT DETECTED! Amp:{}", effect.getAmplifier());
                amp = effect.getAmplifier();
            }
            if (amp > 3) {
                amp = 3;
            }
            return damageAmount * 5 / (4 - amp);
        }
        return damageAmount;
    }

/*
    //I don't know why the following code doesn't work
    @ModifyArg(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float updateDamageAmount(DamageSource damageSource, float damageAmount) {
        if (damageSource.is(DamageTypeTags.BYPASSES_EFFECTS)) {
            return damageAmount;
        } else {
            if (hasEffect(FOFEffects.RESISTANCE_WEAKENED) && !damageSource.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
                int amp = -1;
                var effect = getEffect(FOFEffects.RESISTANCE_WEAKENED);
                if (effect != null) {
                    CobblemonFightOrFlight.LOGGER.info("EFFECT DETECTED! Amp:{}", effect.getAmplifier());
                    amp = effect.getAmplifier();
                }
                if (amp > 3) {
                    amp = 3;
                }
                return damageAmount * 5 / (4 - amp);
            }
        }
        return damageAmount;
    }*/
}
