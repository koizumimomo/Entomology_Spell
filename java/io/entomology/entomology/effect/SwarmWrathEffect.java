package io.entomology.entomology.effect;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.registries.EffectRegistry;
import io.entomology.entomology.registries.SchoolRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Afflicted entities take bonus damage from summoned bees and from
 * spells of the insect (swarm) school. +25% per level.
 */
@Mod.EventBusSubscriber(modid = EntomologyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SwarmWrathEffect extends MobEffect
{
    public static final float DAMAGE_AMPLIFIER_PER_LEVEL = 0.25F;

    public SwarmWrathEffect()
    {
        super(MobEffectCategory.HARMFUL, 0xd4a017);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event)
    {
        LivingEntity victim = event.getEntity();
        MobEffectInstance effect = victim.getEffect(EffectRegistry.SWARM_WRATH.get());
        if (effect == null)
            return;

        DamageSource source = event.getSource();
        boolean amplified = false;

        if (source instanceof SpellDamageSource spellSource
                && spellSource.spell().getSchoolType() == SchoolRegistry.INSECT.get())
        {
            amplified = true;
        }
        else if (source.getEntity() instanceof Bee bee && SummonManager.getOwner(bee) != null)
        {
            amplified = true;
        }

        if (amplified)
        {
            event.setAmount(event.getAmount() * (1.0F + DAMAGE_AMPLIFIER_PER_LEVEL * (effect.getAmplifier() + 1)));
        }
    }
}
