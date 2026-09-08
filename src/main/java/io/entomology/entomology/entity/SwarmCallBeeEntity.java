package io.entomology.entomology.entity;

import io.entomology.entomology.entity.ai.SummonedBeeDespawnGoal;
import io.entomology.entomology.registries.EntityRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Retaliation bee for the Swarm Call buff. Functionally a normal summoned
 * bee, but it is not a swarm member (doesn't count towards Summon Bee Swarm
 * top-ups) and its lifetime is governed by SummonManager.initSummon rather
 * than the Summoned Bee Swarm buff - so the buff-checking despawn goal is
 * stripped.
 */
public class SwarmCallBeeEntity extends SummonedBeeEntity
{
    public SwarmCallBeeEntity(EntityType<? extends Bee> entityType, Level level)
    {
        super(entityType, level);
    }

    public SwarmCallBeeEntity(Level level, @Nullable LivingEntity owner)
    {
        this(EntityRegistry.SWARM_CALL_BEE.get(), level);
        if (owner != null)
        {
            SummonManager.setOwner(this, owner);
        }
    }

    @Override
    protected void registerGoals()
    {
        super.registerGoals();
        // Lifetime comes from SummonManager.initSummon, not the swarm buff
        this.goalSelector.getAvailableGoals().removeIf(wrapped -> wrapped.getGoal() instanceof SummonedBeeDespawnGoal);
    }

    @Override
    public boolean isSwarmMember()
    {
        return false;
    }
}
