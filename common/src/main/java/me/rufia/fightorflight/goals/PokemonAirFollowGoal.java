package me.rufia.fightorflight.goals;
// In your sidemod's AI goal package
// (e.g., me.rufia.fightorflight.ai.goal.PokemonAirFollowGoal.java)

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import me.rufia.fightorflight.CobblemonFightOrFlight; // Your logger

import java.util.EnumSet;

public class PokemonAirFollowGoal extends FollowOwnerGoal {
    private final PokemonEntity pokemon;
    private LivingEntity owner;

    private final double followSpeedModifier; // Speed when actively flying towards owner
    private final float stopFollowingDistance; // Pokémon will stop trying to get closer inside this radius
    private final float startFollowingDistance; // Pokémon will start moving if outside this radius (but within max range)
    private final float maxRangeBeforeTeleportConsideration; // Max distance before we consider teleporting if stuck
    private final float teleportToDistanceOffset; // How close to teleport

    private int pathfindingCooldown; // Ticks before trying to repath
    private int stuckAndFarTicks;   // Ticks Pokemon has been far and not making progress
    private Vec3 lastPokemonPos;

    private static final int MAX_STUCK_AND_FAR_TICKS_BEFORE_TELEPORT = 100; // 5 seconds

    public PokemonAirFollowGoal(PokemonEntity pokemon, double speedModifier,
                                float stopFollowDist, float startFollowDist,
                                float maxRangeTeleport, float teleportToDist) {
        super(pokemon, speedModifier, startFollowDist, stopFollowDist);
        this.pokemon = pokemon;
        this.followSpeedModifier = speedModifier;
        this.stopFollowingDistance = stopFollowDist; // e.g., 16.0F
        this.startFollowingDistance = startFollowDist; // e.g., 12.0F (must be < stopFollowDistance for hysteresis)
        this.maxRangeBeforeTeleportConsideration = maxRangeTeleport; // e.g., 40.0F
        this.teleportToDistanceOffset = teleportToDist; // e.g., 5.0F

        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));

        // Attempt to ensure flying navigation if appropriate
        if (canPokemonFlyEffectively(this.pokemon) && !(((TamableAnimal)this.pokemon).getNavigation() instanceof FlyingPathNavigation)) {
            PathNavigation currentNav = this.pokemon.getNavigation();
            FlyingPathNavigation flyingNav = new FlyingPathNavigation(this.pokemon, this.pokemon.level());
            // Configure flyingNav (canFlyOverLava, etc.) if needed
            // This is a tricky part - replacing navigation can have side effects.
            // A less intrusive way is to primarily use MoveControl for flying if pathing fails.
            // For now, we'll assume it might have one or we use MoveControl.
            CobblemonFightOrFlight.LOGGER.info("Pokemon {} has ground nav but might need flying for AirFollowGoal.", pokemon.getPokemon().getSpecies().getName());
        }
    }

    // Inside PokemonAirFollowGoal.canUse()
    @Override
    public boolean canUse() {
        super.canUse();
        CobblemonFightOrFlight.LOGGER.debug("AirFollowGoal: Checking canUse for {}", pokemon.getPokemon().getSpecies().getName());
        LivingEntity currentOwner = this.pokemon.getOwner();

        if (currentOwner == null) {
            CobblemonFightOrFlight.LOGGER.debug("AirFollowGoal: Owner is null.");
            return false;
        }
        if (!currentOwner.isAlive()) {
            CobblemonFightOrFlight.LOGGER.debug("AirFollowGoal: Owner {} is not alive.", currentOwner.getName().getString());
            return false;
        }
        if (this.pokemon.isLeashed()) {
            CobblemonFightOrFlight.LOGGER.debug("AirFollowGoal: {} is leashed.", pokemon.getPokemon().getSpecies().getName());
            return false;
        }

        if (! canPokemonFlyEffectively(this.pokemon)) { // Your corrected helper method
            CobblemonFightOrFlight.LOGGER.debug("AirFollowGoal: {} cannot fly effectively.", pokemon.getPokemon().getSpecies().getName());
            return false;
        }

        if (!isOwnerHighOrFlying(currentOwner, this.pokemon.level())) {
            CobblemonFightOrFlight.LOGGER.debug("AirFollowGoal: Owner {} is not high or flying.", currentOwner.getName().getString());
            return false;
        }

        double distanceSq = this.pokemon.distanceToSqr(currentOwner);
        CobblemonFightOrFlight.LOGGER.debug("AirFollowGoal: DistanceSq to owner: {}, Required min: {}", distanceSq, (this.startFollowingDistance * this.startFollowingDistance));
        if (distanceSq < (this.startFollowingDistance * this.startFollowingDistance)) {
            CobblemonFightOrFlight.LOGGER.debug("AirFollowGoal: {} is too close to owner ({} < {}).", pokemon.getPokemon().getSpecies().getName(), distanceSq, (this.startFollowingDistance * this.startFollowingDistance));
            return false;
        }

        this.owner = currentOwner; // Set owner field only if all conditions pass
        CobblemonFightOrFlight.LOGGER.info("AirFollowGoal: {} CAN USE goal for owner {}", pokemon.getPokemon().getSpecies().getName(), owner.getName().getString());
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.owner == null || !this.owner.isAlive() || this.pokemon.isLeashed() || !canPokemonFlyEffectively(this.pokemon)) {
            return false;
        }
        if (!isOwnerHighOrFlying(this.owner, this.pokemon.level())) { // If owner lands, this goal might stop
            return false;
        }
        // Continue if further than the *stop* following distance OR if currently pathfinding to get closer
        return this.pokemon.distanceToSqr(this.owner) > (this.stopFollowingDistance * this.stopFollowingDistance) ||
                !this.pokemon.getNavigation().isDone();
    }

    @Override
    public void start() {
        this.pathfindingCooldown = 0;
        this.stuckAndFarTicks = 0;
        this.lastPokemonPos = this.pokemon.position();
        this.pokemon.getNavigation().stop(); // Clear any previous path
        CobblemonFightOrFlight.LOGGER.debug("{} started AirFollowGoal for owner {}", pokemon.getPokemon().getSpecies().getName(), owner.getName().getString());
    }

    @Override
    public void stop() {
        this.owner = null;
        this.pokemon.getNavigation().stop();
        CobblemonFightOrFlight.LOGGER.debug("{} stopped AirFollowGoal.", pokemon.getPokemon().getSpecies().getName());
    }

    @Override
    public void tick() {
        if (this.owner == null || !this.owner.isAlive()) {
            return;
        }

        this.pokemon.getLookControl().setLookAt(this.owner, 10.0F, (float) this.pokemon.getMaxHeadXRot());

        if (--this.pathfindingCooldown <= 0) {
            this.pathfindingCooldown = this.adjustedTickDelay(10); // Try to repath every 0.5 seconds

            double distanceSqToOwner = this.pokemon.distanceToSqr(this.owner);

            if (distanceSqToOwner > (this.maxRangeBeforeTeleportConsideration * this.maxRangeBeforeTeleportConsideration)) {
                // Check if stuck before teleporting
                if (this.pokemon.position().distanceToSqr(this.lastPokemonPos) < 1.0*1.0) { // Moved less than 1 block since last check
                    this.stuckAndFarTicks++;
                } else {
                    this.stuckAndFarTicks = 0; // Reset if moved
                }
                this.lastPokemonPos = this.pokemon.position();

                if (this.stuckAndFarTicks > MAX_STUCK_AND_FAR_TICKS_BEFORE_TELEPORT) {
                    CobblemonFightOrFlight.LOGGER.info("{} is far and stuck, attempting air teleport to owner {}.", pokemon.getPokemon().getSpecies().getName(), owner.getName().getString());
                    teleportToOwnerNearTargetPoint(calculateFollowPos());
                    this.stuckAndFarTicks = 0; // Reset after teleport attempt
                    return; // Don't try to pathfind this tick
                }
            } else {
                this.stuckAndFarTicks = 0; // Reset if not in teleport consideration range
                this.lastPokemonPos = this.pokemon.position();
            }


            // Only pathfind if outside the stopFollowingDistance
            if (distanceSqToOwner > (this.stopFollowingDistance * this.stopFollowingDistance)) {
                Vec3 targetFollowPos = calculateFollowPos();
                boolean pathSuccess = this.pokemon.getNavigation().moveTo(targetFollowPos.x, targetFollowPos.y, targetFollowPos.z, this.followSpeedModifier);

                if (!pathSuccess) {
                    // If standard pathfinding fails (e.g. target too high or complex terrain for current navigator)
                    // try using MoveControl for more direct flying movement.
                    // This assumes the Pokemon has a MoveControl that can handle air movement (e.g. FlyingMoveControl)
                    // CobblemonFightOrFlight.LOGGER.debug("Pathfinding failed for {}, using MoveControl.", pokemon.getSpecies().getName().getString());
                    this.pokemon.getMoveControl().setWantedPosition(targetFollowPos.x, targetFollowPos.y, targetFollowPos.z, this.followSpeedModifier);
                }
            } else {
                this.pokemon.getNavigation().stop(); // Close enough
            }
        }
    }

    private Vec3 calculateFollowPos() {
        // Try to stay slightly above and behind/to-the-side of the owner
        Vec3 ownerPos = this.owner.position();
        Vec3 ownerLookVec = this.owner.getViewVector(1.0f);

        // Position behind the owner
        double behindDist = -2.0; // 2 blocks behind
        // Position above owner
        double aboveDist = 1.5 + this.pokemon.getRandom().nextFloat() * 1.0; // 1.5 to 2.5 blocks above owner's base

        // Sideways offset
        double sideDist = (this.pokemon.getRandom().nextBoolean() ? 1.0 : -1.0) * (1.0 + this.pokemon.getRandom().nextFloat()); // 1-2 blocks to the side

        // Calculate right vector from owner's look vector
        Vec3 ownerRightVec = new Vec3(-ownerLookVec.z, 0, ownerLookVec.x).normalize(); // Perpendicular to look in XZ plane

        return ownerPos
                .add(ownerLookVec.scale(behindDist))
                .add(0, aboveDist, 0)
                .add(ownerRightVec.scale(sideDist));
    }

    private void teleportToOwnerNearTargetPoint(Vec3 idealTargetPoint) {
        for (int i = 0; i < 10; ++i) {
            double dx = (this.pokemon.getRandom().nextDouble() - 0.5D) * 4.0D; // Random offset
            double dy = (this.pokemon.getRandom().nextDouble() - 0.5D) * 2.0D;
            double dz = (this.pokemon.getRandom().nextDouble() - 0.5D) * 4.0D;
            double tpX = idealTargetPoint.x() + dx;
            double tpY = idealTargetPoint.y() + dy;
            double tpZ = idealTargetPoint.z() + dz;

            if (canTeleportTo(tpX, tpY, tpZ)) {
                this.pokemon.teleportTo(tpX, tpY, tpZ);
                this.pokemon.getNavigation().stop();
                this.pokemon.getLookControl().setLookAt(this.owner, 30f, (float)this.pokemon.getMaxHeadXRot());
                return;
            }
        }
    }

    private boolean canTeleportTo(double x, double y, double z) {
        return this.pokemon.level().noCollision(this.pokemon, this.pokemon.getBoundingBox());
    }

    // Your helper methods from before
    private boolean canPokemonFlyEffectively(PokemonEntity p) {
        // More robust check using Cobblemon's species behavior data
        return p.getBehaviour().getMoving().getFly().getCanFly();
    }

    private boolean isOwnerHighOrFlying(LivingEntity ownerToCheck, Level level) {
        if (ownerToCheck.onGround()) {
            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
            int airBlocksBelow = 0;
            for (int i = 1; i <= 8; i++) { // Check up to 8 blocks down
                mutablePos.set(ownerToCheck.getX(), ownerToCheck.getY() - i, ownerToCheck.getZ());
                if (!level.getBlockState(mutablePos).isAir()) {
                    break; // Hit ground
                }
                airBlocksBelow++;
            }
            return airBlocksBelow > 4; // e.g., owner is more than 4 blocks above solid ground
        }
        return true; // If not on ground, assume high enough or flying
    }
}
