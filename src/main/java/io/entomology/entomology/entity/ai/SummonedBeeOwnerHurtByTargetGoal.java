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
 * Copied from the vanilla wolf's OwnerHurtByTargetGoal: counter-attacks whoever
 * attacked the owner last. The owner is resolved through Iron's Spells SummonManager.
 */
public class SummonedBeeOwnerHurtByTargetGoal extends TargetGoal
{
    private final Bee bee;
    private LivingEntity ownerLastHurtBy;
    private int timestamp;

    public SummonedBeeOwnerHurtByTargetGoal(Bee bee)
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

        this.ownerLastHurtBy = owner.getLastHurtByMob();
        int i = owner.getLastHurtByMobTimestamp();
        if (i == this.timestamp)
            return false;
        if (this.ownerLastHurtBy == owner)
            return false;
        // do not target friendly summons
        if (this.ownerLastHurtBy instanceof Bee beeTarget && SummonManager.getOwner(beeTarget) != null)
            return false;

        return this.canAttack(this.ownerLastHurtBy, TargetingConditions.DEFAULT) && this.wantsToAttack(this.ownerLastHurtBy, owner);
    }

    private boolean wantsToAttack(LivingEntity target, LivingEntity owner)
    {
        if (target instanceof Player && owner instanceof Player && !((Player) owner).canHarmPlayer((Player) target))
            return false;
        return true;
    }

    @Override
    public void start()
    {
        this.mob.setTarget(this.ownerLastHurtBy);
        this.bee.startPersistentAngerTimer();
        LivingEntity owner = this.getOwner();
        if (owner != null)
            this.timestamp = owner.getLastHurtByMobTimestamp();
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
