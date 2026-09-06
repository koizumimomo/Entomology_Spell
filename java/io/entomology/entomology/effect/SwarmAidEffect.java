package io.entomology.entomology.effect;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.registries.AttributeRegistry;
import io.entomology.entomology.registries.EffectRegistry;
import io.entomology.entomology.entity.SwarmFireflyProjectile;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * While active, each melee attack summons two one-shot fireflies (Echoing Strikes pattern)
 * that strike the target once and vanish. The damage per firefly scales with the
 * effect's amplifier.
 */
@Mod.EventBusSubscriber(modid = EntomologyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SwarmAidEffect extends MobEffect
{
    public static final int FIREFLIES_PER_ATTACK = 2;

    public SwarmAidEffect()
    {
        super(MobEffectCategory.BENEFICIAL, 0x9acd32);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event)
    {
        // Only melee attacks by the effect holder summon fireflies.
        // The direct entity check prevents firefly hits (or other indirect damage) from re-triggering.
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker))
            return;
        if (event.getSource().getDirectEntity() != attacker)
            return;

        MobEffectInstance effect = attacker.getEffect(EffectRegistry.SWARM_AID.get());
        if (effect == null)
            return;

        Level level = attacker.level();
        LivingEntity target = event.getEntity();

        // Firefly damage scales with the owner's spell power (global x insect school)
        double spellPower = attacker.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get());
        double insectPower = attacker.getAttributeValue(AttributeRegistry.INSECT_SPELL_POWER.get());
        float damage = (float) (3.0 + spellPower * insectPower * 2.0);

        for (int i = 0; i < FIREFLIES_PER_ATTACK; i++)
        {
            SwarmFireflyProjectile firefly = new SwarmFireflyProjectile(level, attacker, target, damage);
            level.addFreshEntity(firefly);
        }
    }
}
