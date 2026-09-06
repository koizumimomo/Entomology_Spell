package io.entomology.entomology.effect;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.registries.EffectRegistry;
import io.entomology.entomology.util.InsectDamage;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Granted by Queen Bee while bees are nearby. Like Iron's Spells "Charged",
 * the buff is registered with a spell-power attribute modifier (+5% insect
 * spell power per level), which SchoolType.getPowerFor reads directly.
 * For summoned creatures (whose damage is fixed at summon time) the hurt
 * event applies the same multiplier instead.
 */
@Mod.EventBusSubscriber(modid = EntomologyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SwarmWillEffect extends MobEffect
{
    public static final float DAMAGE_AMPLIFIER_PER_LEVEL = 0.05F;

    public SwarmWillEffect()
    {
        super(MobEffectCategory.BENEFICIAL, 0xffc107);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event)
    {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker))
            return;

        // Player spell damage already scales through the insect spell power
        // attribute modifier (see EffectRegistry) - don't double-count.
        if (attacker instanceof Player)
            return;

        // The will bearer themselves or the owner of the attacking summon
        LivingEntity willHolder = null;
        if (attacker.hasEffect(EffectRegistry.SWARM_WILL.get()))
        {
            willHolder = attacker;
        }
        else
        {
            Entity owner = SummonManager.getOwner(attacker);
            if (owner instanceof LivingEntity living && living.hasEffect(EffectRegistry.SWARM_WILL.get()))
            {
                willHolder = living;
            }
        }
        if (willHolder == null)
            return;
        if (!InsectDamage.isInsectSchoolDamage(event.getSource()))
            return;

        int layers = willHolder.getEffect(EffectRegistry.SWARM_WILL.get()).getAmplifier() + 1;
        event.setAmount(event.getAmount() * (1.0F + DAMAGE_AMPLIFIER_PER_LEVEL * layers));
    }
}
