package io.entomology.entomology.effect;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.util.SwarmCreatures;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;

/**
 * The afflicted entity draws the attacks of every insect (silverfish, spiders,
 * bees...) within 32 blocks. Those creatures are forced to target the holder
 * while the effect lasts.
 */
public class ChildWrathEffect extends MobEffect
{
    public static final double ATTRACT_RADIUS = 32.0D;
    public static final int RETARGET_INTERVAL = 20;

    public ChildWrathEffect()
    {
        super(MobEffectCategory.HARMFUL, 0xa83232);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier)
    {
        return duration % RETARGET_INTERVAL == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity holder, int amplifier)
    {
        if (holder.level().isClientSide)
            return;

        for (Mob insect : holder.level().getEntitiesOfClass(Mob.class, holder.getBoundingBox().inflate(ATTRACT_RADIUS)))
        {
            if (insect == holder || !SwarmCreatures.isSwarmCreature(insect) || !insect.isAlive())
                continue;

            insect.setTarget(holder);
            // Neutral mobs (bees) only fight while angry: refresh their anger
            // every interval so they never calm down while the effect lasts.
            if (insect instanceof NeutralMob neutral)
            {
                neutral.setPersistentAngerTarget(holder.getUUID());
                neutral.setRemainingPersistentAngerTime(600);
            }
        }
    }
}
