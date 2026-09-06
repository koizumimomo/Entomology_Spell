package io.entomology.entomology.entity.ai;

import io.entomology.entomology.registries.EffectRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Bee;

/**
 * Summoned bees live as long as their owner keeps the Summoned Bee Swarm buff.
 * Once per second this goal checks the owner; if the buff is gone (or the owner
 * is gone), the bee disappears.
 */
public class SummonedBeeDespawnGoal extends Goal
{
    private final Bee bee;

    public SummonedBeeDespawnGoal(Bee bee)
    {
        this.bee = bee;
    }

    @Override
    public boolean canUse()
    {
        return true;
    }

    @Override
    public void tick()
    {
        if (this.bee.tickCount % 20 != 0)
            return;

        LivingEntity owner = this.getOwner();
        if (owner == null || !owner.hasEffect(EffectRegistry.SUMMONED_BEE_SWARM.get()))
        {
            this.bee.discard();
        }
    }

    private LivingEntity getOwner()
    {
        Entity entity = SummonManager.getOwner(this.bee);
        return entity instanceof LivingEntity livingEntity ? livingEntity : null;
    }
}
