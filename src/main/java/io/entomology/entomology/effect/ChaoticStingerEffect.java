package io.entomology.entomology.effect;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.compat.BumblezoneCompat;
import io.entomology.entomology.entity.BeeStingerProjectile;
import io.entomology.entomology.registries.EffectRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * While the owner carries this effect, bees they have summoned inflict a random
 * extra debuff (poison or wither) on every creature they sting.
 */
@Mod.EventBusSubscriber(modid = EntomologyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChaoticStingerEffect extends MobEffect
{
    private static final int POISON_DURATION = 100; // 5 seconds
    private static final int WITHER_DURATION = 80; // 4 seconds
    private static final int PARALYZE_DURATION = 60; // 3 seconds
    private static final float PARALYZE_CHANCE = 0.25F;
    private static final int MAX_STACKS = 4; // poison/wither stack up to amplifier 4 (level V)

    public ChaoticStingerEffect()
    {
        super(MobEffectCategory.BENEFICIAL, 0xb8860b);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event)
    {
        DamageSource source = event.getSource();
        LivingEntity attacker = null;

        // Check if damage comes from a Bee entity
        if (source.getEntity() instanceof Bee bee)
        {
            attacker = bee;
        }
        // Check if damage comes from a BeeStingerProjectile with Bee owner
        else if (source.getEntity() instanceof BeeStingerProjectile stinger)
        {
            Entity ownerEntity = stinger.getOwner();
            if (ownerEntity instanceof Bee beeOwner)
            {
                attacker = beeOwner;
            }
        }

        if (attacker == null)
            return;

        Entity ownerEntity = SummonManager.getOwner(attacker);
        if (!(ownerEntity instanceof LivingEntity owner))
            return;

        MobEffectInstance stingerEffect = owner.getEffect(EffectRegistry.CHAOTIC_STINGER.get());
        if (stingerEffect == null)
            return;

        LivingEntity victim = event.getEntity();
        int amplifier = stingerEffect.getAmplifier();

        // Random debuff: poison or wither — both stack up on repeated stings
        boolean poison = victim.level().getRandom().nextBoolean();
        MobEffect poisonOrWither = poison ? MobEffects.POISON : MobEffects.WITHER;
        int duration = poison ? POISON_DURATION : WITHER_DURATION;
        int stackAmplifier = StackingEffects.nextAmplifier(victim, poisonOrWither, amplifier, MAX_STACKS);
        StackingEffects.applyForced(victim, poisonOrWither, duration, stackAmplifier);

        // The Bumblezone integration: a chance to also paralyze the victim
        if (victim.level().getRandom().nextFloat() < PARALYZE_CHANCE)
        {
            MobEffect paralyzed = BumblezoneCompat.getParalyzedEffect();
            if (paralyzed != null && BumblezoneCompat.canParalyze(victim))
            {
                victim.addEffect(new MobEffectInstance(paralyzed, PARALYZE_DURATION, 0, false, true, true));
            }
        }
    }
}
