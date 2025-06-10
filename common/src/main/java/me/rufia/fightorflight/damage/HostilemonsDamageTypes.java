package me.rufia.fightorflight.damage;

import dev.architectury.registry.registries.DeferredRegister;
import me.rufia.fightorflight.CobblemonFightOrFlight;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

public interface HostilemonsDamageTypes {
    public static final ResourceKey<DamageType> FLAMETHROWER_STREAM = ResourceKey.create(
            Registries.DAMAGE_TYPE, // The registry this key belongs to
            ResourceLocation.fromNamespaceAndPath(CobblemonFightOrFlight.MODID, "flamethrower_stream") // The ID of your JSON
    );
}
