package io.entomology.entomology.effect;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.entity.SwarmCallBeeEntity;
import io.entomology.entomology.registries.EffectRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * While the holder carries this effect, taking damage has a chance to summon
 * one or two bees right on top of the attacker, ready to sting back. The
 * chance (and from level 3 the bee count) scales with the effect's amplifier.
 */
@Mod.EventBusSubscriber(modid = EntomologyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SwarmCallEffect extends MobEffect
{
    public static final int BEE_LIFETIME_TICKS = 300; // 15 seconds
    public static final double BEE_ATTACK_DAMAGE = 4.0D;
    public static final int BEE_ANGER_TICKS = 600; // 30 seconds of willingness to sting

    public SwarmCallEffect()
    {
        super(MobEffectCategory.BENEFICIAL, 0xffc107);
    }

    /** Proc chance: 20% at level 1, +5% per level, capped at 45%. */
    public static float getProcChance(int amplifier)
    {
        return Math.min(0.45F, 0.15F + 0.05F * (amplifier + 1));
    }

    /** Two bees from level 3 (amplifier 2) onwards. */
    public static int getBeeCount(int amplifier)
    {
        return amplifier >= 2 ? 2 : 1;
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event)
    {
        LivingEntity victim = event.getEntity();
        MobEffectInstance effect = victim.getEffect(EffectRegistry.SWARM_CALL.get());
        if (effect == null || victim.level().isClientSide)
        {
            return;
        }

        // Any attack by a living attacker can trigger the swarm
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == victim)
        {
            return;
        }

        if (victim.getRandom().nextFloat() >= getProcChance(effect.getAmplifier()))
        {
            return;
        }

        int count = getBeeCount(effect.getAmplifier());
        for (int i = 0; i < count; i++)
        {
            Vec3 offset = new Vec3(
                    (victim.getRandom().nextDouble() - 0.5D) * 2.0D,
                    1.0D,
                    (victim.getRandom().nextDouble() - 0.5D) * 2.0D);
            SwarmCallBeeEntity bee = new SwarmCallBeeEntity(victim.level(), victim);
            bee.moveTo(attacker.getX() + offset.x, attacker.getY() + offset.y, attacker.getZ() + offset.z,
                    victim.getYRot(), 0.0F);
            bee.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(BEE_ATTACK_DAMAGE);
            // Immediate retaliation: BeeAttackGoal only swings while angry
            bee.setTarget(attacker);
            bee.setRemainingPersistentAngerTime(BEE_ANGER_TICKS);
            victim.level().addFreshEntity(bee);
            // Fixed lifetime independent of the (usually absent) swarm buff
            SummonManager.initSummon(victim, bee, BEE_LIFETIME_TICKS, new SummonedEntitiesCastData());
        }
    }
}
