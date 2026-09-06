package io.entomology.entomology.effect;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.registries.EffectRegistry;
import io.entomology.entomology.util.InsectDamage;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Infatuated: every level increases swarm school damage taken by 5% and slows
 * movement by 10%. Stacked up to level 5 by the Sweetheart effect.
 */
@Mod.EventBusSubscriber(modid = EntomologyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class InfatuatedEffect extends MobEffect
{
    public static final int MAX_AMPLIFIER = 4; // level 5
    public static final float DAMAGE_TAKEN_PER_LEVEL = 0.05F;

    public InfatuatedEffect()
    {
        super(MobEffectCategory.HARMFUL, 0xd81b60);
        // -10% movement speed per level (default modifier value scales with amplifier + 1)
        this.addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,
                "5f9c7a2e-1b4d-4e6a-9c3f-2d8b7e6a5c4d", -0.1D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event)
    {
        MobEffectInstance infatuated = event.getEntity().getEffect(EffectRegistry.INFATUATED.get());
        if (infatuated == null)
            return;
        if (!InsectDamage.isInsectSchoolDamage(event.getSource()))
            return;

        int level = infatuated.getAmplifier() + 1;
        event.setAmount(event.getAmount() * (1.0F + DAMAGE_TAKEN_PER_LEVEL * level));
    }
}
