package io.entomology.entomology.effect;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.entity.SummonedSilverfishEntity;
import io.entomology.entomology.registries.EffectRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Silverfish counterpart of Chaotic Stinger: while the owner carries this
 * effect, every silverfish they have summoned inflicts Hunger on its bite,
 * plus a Slowness that stacks by one level per bite (capped at Slowness V).
 */
@Mod.EventBusSubscriber(modid = EntomologyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ParasiticBreathEffect extends MobEffect
{
    public static final int HUNGER_DURATION = 200; // 10 seconds
    public static final int SLOWNESS_DURATION = 100; // 5 seconds
    public static final int MAX_SLOWNESS_STACKS = 4; // Slowness V

    public ParasiticBreathEffect()
    {
        super(MobEffectCategory.BENEFICIAL, 0x9b59b6);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event)
    {
        // Silverfish are melee only, so the direct entity check is enough
        if (!(event.getSource().getEntity() instanceof SummonedSilverfishEntity fish))
        {
            return;
        }

        Entity ownerEntity = SummonManager.getOwner(fish);
        if (!(ownerEntity instanceof LivingEntity owner))
        {
            return;
        }

        MobEffectInstance breath = owner.getEffect(EffectRegistry.PARASITIC_BREATH.get());
        if (breath == null)
        {
            return;
        }

        LivingEntity victim = event.getEntity();
        int baseAmplifier = breath.getAmplifier();

        // Hunger refreshes at the breath's level on every bite
        victim.addEffect(new MobEffectInstance(MobEffects.HUNGER, HUNGER_DURATION, baseAmplifier, false, true, true));

        // Slowness stacks: +1 level per bite, refreshed duration
        int slownessAmplifier = StackingEffects.nextAmplifier(victim, MobEffects.MOVEMENT_SLOWDOWN, baseAmplifier, MAX_SLOWNESS_STACKS);
        StackingEffects.applyForced(victim, MobEffects.MOVEMENT_SLOWDOWN, SLOWNESS_DURATION, slownessAmplifier);
    }
}
