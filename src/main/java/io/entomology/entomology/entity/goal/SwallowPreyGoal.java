package io.entomology.entomology.entity.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * <b>Reserved — not registered on any entity yet.</b> Swallow attack for the
 * summoned spiders (ice spider / guardian spider, or a future spider type):
 * when the host's melee target is small enough, the spider gulps it down
 * whole. The prey is held inside the spider's belly, taking digestion damage
 * every second, while the spider turns and scurries off in a random
 * direction. The prey is spat back out once the digest timer expires; the
 * only way to free it early is to kill the spider.
 *
 * <h3>Enabling it later</h3>
 * In the host entity's {@code registerGoals()}:
 * <pre>
 *   SwallowPreyGoal swallowGoal = new SwallowPreyGoal(this);
 *   this.goalSelector.addGoal(3, swallowGoal);
 * </pre>
 * and override {@code die()} in the host to call
 * {@code swallowGoal.releasePrey();} before {@code super.die(...)} so a killed
 * spider always disgorges its meal.
 *
 * <h3>Constraints</h3>
 * <ul>
 *   <li>Players are never swallowed (teleporting a held player is messy).</li>
 *   <li>Prey larger than {@link #MAX_PREY_VOLUME} (bb width × height) is
 *       rejected, so bosses and big mobs are immune.</li>
 *   <li>While swallowed the prey is invisible, invulnerable to outside
 *       damage and (if a Mob) has its AI suspended; everything is restored
 *       on release.</li>
 * </ul>
 */
public class SwallowPreyGoal extends Goal
{
    /** Ticks the prey stays inside (default 6 seconds). */
    private static final int DIGEST_TICKS = 120;
    /** Digestion damage applied once per second. */
    private static final float DIGEST_DAMAGE = 2.0F;
    /** Re-pick a flee direction this often. */
    private static final int FLEE_INTERVAL = 30;
    /** bb width × height limit for swallowable prey. */
    private static final double MAX_PREY_VOLUME = 2.5D;
    /** Melee reach within which a swallow attempt can start. */
    private static final double SWALLOW_REACH_SQR = 3.0D * 3.0D;
    /** Cooldown after a release before the next swallow attempt. */
    private static final int SWALLOW_COOLDOWN = 200;
    /** Chance per eligible check to actually swallow (keeps it occasional). */
    private static final float SWALLOW_CHANCE = 0.25F;

    private final PathfinderMob host;
    @Nullable
    private LivingEntity prey;
    private int digestTimer = 0;
    private int fleeTimer = 0;
    private int cooldown = 0;
    private boolean preyWasInvulnerable = false;

    public SwallowPreyGoal(PathfinderMob host)
    {
        this.host = host;
        // Movement is driven manually while fleeing with a full belly.
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.TARGET));
    }

    @Override
    public boolean canUse()
    {
        if (this.host.level().isClientSide || this.prey != null)
            return false;
        if (this.cooldown > 0)
        {
            this.cooldown--;
            return false;
        }
        LivingEntity target = this.host.getTarget();
        if (target == null || !target.isAlive() || target instanceof Player)
            return false;
        if (target.getBbWidth() * target.getBbHeight() > MAX_PREY_VOLUME)
            return false;
        if (this.host.distanceToSqr(target) > SWALLOW_REACH_SQR)
            return false;
        return this.host.getRandom().nextFloat() < SWALLOW_CHANCE;
    }

    @Override
    public void start()
    {
        LivingEntity target = this.host.getTarget();
        if (target == null)
            return;
        this.prey = target;
        this.digestTimer = DIGEST_TICKS;
        this.fleeTimer = 0;

        // Tuck the prey away: invisible, immune to outside damage, no AI.
        this.preyWasInvulnerable = target.isInvulnerable();
        target.setInvisible(true);
        target.setInvulnerable(true);
        if (target instanceof Mob mob)
        {
            mob.setNoAi(true);
            mob.setTarget(null);
        }
        target.setDeltaMovement(Vec3.ZERO);

        // The spider stops fighting and gets ready to bolt.
        this.host.setTarget(null);
        this.host.getNavigation().stop();
    }

    @Override
    public boolean canContinueToUse()
    {
        return this.prey != null && this.prey.isAlive() && this.digestTimer > 0 && this.host.isAlive();
    }

    @Override
    public void tick()
    {
        LivingEntity prey = this.prey;
        if (prey == null)
            return;

        // Hold the prey at the spider's position, motionless.
        prey.setPos(this.host.getX(), this.host.getY() + 0.25D, this.host.getZ());
        prey.setDeltaMovement(Vec3.ZERO);

        // Digestion damage, once per second.
        this.digestTimer--;
        if (this.digestTimer % 20 == 0)
        {
            // Temporarily drop the swallow invulnerability so our own
            // digestion damage lands.
            prey.setInvulnerable(this.preyWasInvulnerable);
            prey.hurt(this.host.damageSources().mobAttack(this.host), DIGEST_DAMAGE);
            if (prey.isAlive())
                prey.setInvulnerable(true);
        }

        // With a full belly the spider scurries off in a random direction.
        if (--this.fleeTimer <= 0)
        {
            this.fleeTimer = FLEE_INTERVAL;
            double angle = this.host.getRandom().nextDouble() * Math.PI * 2.0D;
            double dist = 8.0D + this.host.getRandom().nextDouble() * 6.0D;
            double x = this.host.getX() + Math.cos(angle) * dist;
            double z = this.host.getZ() + Math.sin(angle) * dist;
            this.host.getNavigation().moveTo(x, this.host.getY(), z, 1.35D);
        }
    }

    @Override
    public void stop()
    {
        releasePrey();
    }

    /**
     * Spits the prey back out: restores visibility / invulnerability / AI and
     * pops it out with a small upward toss. Safe to call from the host's
     * {@code die()} so a killed spider always disgorges its meal. Also safe
     * to call twice.
     */
    public void releasePrey()
    {
        LivingEntity prey = this.prey;
        this.prey = null;
        this.digestTimer = 0;
        this.cooldown = SWALLOW_COOLDOWN;
        if (prey == null || this.host.level().isClientSide)
            return;
        if (!prey.isRemoved())
        {
            prey.setInvisible(false);
            prey.setInvulnerable(this.preyWasInvulnerable);
            if (prey instanceof Mob mob)
                mob.setNoAi(false);
            prey.setPos(this.host.getX(), this.host.getY() + 0.5D, this.host.getZ());
            prey.setDeltaMovement(
                    (this.host.getRandom().nextDouble() - 0.5D) * 0.4D,
                    0.4D,
                    (this.host.getRandom().nextDouble() - 0.5D) * 0.4D);
            prey.hurtMarked = true;
        }
    }

    /** The currently swallowed prey, or {@code null} if the belly is empty. */
    @Nullable
    public LivingEntity getPrey()
    {
        return this.prey;
    }
}
