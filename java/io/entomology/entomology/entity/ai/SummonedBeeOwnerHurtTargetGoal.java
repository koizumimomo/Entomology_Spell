package io.entomology.entomology.entity.ai;

import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Copied from the vanilla wolf's OwnerHurtTargetGoal: attacks the entity the
 * owner last attacked. The owner is resolved through Iron's Spells SummonManager
 * because bees are not tameable animals.
 */
public class SummonedBeeOwnerHurtTargetGoal extends TargetGoal
{
    private final Bee bee;
    private LivingEntity ownerLastHurt;
    private int timestamp;

    public SummonedBeeOwnerHurtTargetGoal(Bee bee)
    {
        super(bee, false);
        this.bee = bee;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    private LivingEntity getOwner()
    {
        Entity entity = SummonManager.getOwner(this.bee);
        return entity instanceof LivingEntity livingEntity ? livingEntity : null;
    }

    @Override
    public boolean canUse()
    {
        LivingEntity owner = this.getOwner();
        if (owner == null)
            return false;

        this.ownerLastHurt = owner.getLastHurtMob();
        int i = owner.getLastHurtMobTimestamp();
        if (i == this.timestamp)
            return false;
        if (this.ownerLastHurt == owner)
            return false;
        // do not target friendly summons
        if (this.ownerLastHurt instanceof Bee beeTarget && SummonManager.getOwner(beeTarget) != null)
            return false;

        return this.canAttack(this.ownerLastHurt, TargetingConditions.DEFAULT) && this.wantsToAttack(this.ownerLastHurt, owner);
    }

    private boolean wantsToAttack(LivingEntity target, LivingEntity owner)
    {
        // Mirrors TamableAnimal#wantsToAttack: respect PvP rules between players
        if (target instanceof Player && owner instanceof Player && !((Player) owner).canHarmPlayer((Player) target))
            return false;
        return true;
    }

    @Override
    public void start()
    {
        this.mob.setTarget(this.ownerLastHurt);
        this.bee.startPersistentAngerTimer();
        LivingEntity owner = this.getOwner();
        if (owner != null)
            this.timestamp = owner.getLastHurtMobTimestamp();
        super.start();
    }

    @Override
    public void tick()
    {
        // The vanilla BeeAttackGoal only attacks while the anger timer is running.
        // Bees calm down after each sting, so keep them angry whenever they have a target.
        if (this.bee.getTarget() != null && this.bee.getRemainingPersistentAngerTime() <= 0)
        {
            this.bee.setRemainingPersistentAngerTime(600);
        }
    }
}
