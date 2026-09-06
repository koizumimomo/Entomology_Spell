package io.entomology.entomology.effect;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.registries.EffectRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * While active, the holder's attacks mark the target with Swarm Wrath, making
 * it take bonus damage from summoned bees and insect school spells. Also grants
 * Swarm Will every second: +5% swarm spell damage per bee within 32 blocks
 * (capped at 25 levels).
 */
@Mod.EventBusSubscriber(modid = EntomologyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class QueenBeeEffect extends MobEffect
{
    public static final int WRATH_DURATION_TICKS = 200; // 10 seconds
    public static final double BEE_SCAN_RADIUS = 32.0D;
    public static final int MAX_SWARM_WILL_LEVEL = 25;

    public QueenBeeEffect()
    {
        super(MobEffectCategory.BENEFICIAL, 0xf5c518);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier)
    {
        return duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity holder, int amplifier)
    {
        if (holder.level().isClientSide)
            return;

        int beeCount = holder.level().getEntitiesOfClass(Bee.class, holder.getBoundingBox().inflate(BEE_SCAN_RADIUS)).size();
        if (beeCount > 0)
        {
            holder.addEffect(new MobEffectInstance(EffectRegistry.SWARM_WILL.get(), 40,
                    Math.min(MAX_SWARM_WILL_LEVEL, beeCount) - 1, false, false, true));
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event)
    {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker))
            return;
        if (!attacker.hasEffect(EffectRegistry.QUEEN_BEE.get()))
            return;

        event.getEntity().addEffect(new MobEffectInstance(
                EffectRegistry.SWARM_WRATH.get(), WRATH_DURATION_TICKS, 0, false, false, true));
    }
}
