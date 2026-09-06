package io.entomology.entomology.entity;

import io.entomology.entomology.registries.EntityRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Summoned cave spider: same summon AI as {@link SummonedSpiderEntity} but
 * keeps the vanilla cave spider's venomous bite (poison on melee hit).
 */
public class SummonedCaveSpiderEntity extends SummonedSpiderEntity
{
    public SummonedCaveSpiderEntity(EntityType<? extends SummonedCaveSpiderEntity> entityType, Level level)
    {
        super((EntityType<? extends net.minecraft.world.entity.monster.Spider>) entityType, level);
    }

    public SummonedCaveSpiderEntity(Level level, @Nullable LivingEntity owner)
    {
        this(EntityRegistry.SUMMONED_CAVE_SPIDER.get(), level);
        if (owner != null)
        {
            SummonManager.setOwner(this, owner);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target)
    {
        // Vanilla CaveSpiderEntity#doHurtTarget, rebased onto the summon
        if (super.doHurtTarget(target) && target instanceof LivingEntity living)
        {
            int duration = this.level().getDifficulty() == net.minecraft.world.Difficulty.HARD ? 15 : 7;
            if (!living.hasEffect(MobEffects.POISON))
            {
                living.addEffect(new MobEffectInstance(MobEffects.POISON, duration * 20, 0), this);
            }
            return true;
        }
        return false;
    }
}
