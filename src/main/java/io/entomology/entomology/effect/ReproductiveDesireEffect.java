package io.entomology.entomology.effect;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.registries.EffectRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Granted permanently while the Bee Incarnation leggings are worn. When one of
 * the holder's summons (bees, parasite silverfish...) lands a kill, there is a
 * chance a new summon of the same kind hatches to keep fighting.
 */
@Mod.EventBusSubscriber(modid = EntomologyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ReproductiveDesireEffect extends MobEffect
{
    public static final float SPAWN_CHANCE = 0.25F;
    public static final int SPAWN_LIFETIME_TICKS = 600; // 30 seconds

    public ReproductiveDesireEffect()
    {
        super(MobEffectCategory.BENEFICIAL, 0x66bb6a);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event)
    {
        if (!(event.getSource().getEntity() instanceof LivingEntity killer))
            return;

        Entity owner = SummonManager.getOwner(killer);
        if (!(owner instanceof Player player))
            return;
        if (!player.hasEffect(EffectRegistry.REPRODUCTIVE_DESIRE.get()))
            return;

        Level world = event.getEntity().level();
        if (world.isClientSide)
            return;

        if (world.random.nextFloat() >= SPAWN_CHANCE)
            return;

        LivingEntity victim = event.getEntity();
        Mob newborn = (Mob) killer.getType().create(world);
        if (newborn == null)
            return;

        newborn.moveTo(victim.getX(), victim.getY(), victim.getZ(), world.random.nextFloat() * 360.0F, 0.0F);
        SummonManager.setOwner(newborn, player);
        SummonManager.setDuration(newborn, SPAWN_LIFETIME_TICKS);

        LivingEntity target = player.getLastHurtMob();
        if (target != null && target.isAlive() && target != newborn)
        {
            newborn.setTarget(target);
        }

        world.addFreshEntity(newborn);
        MagicManager.spawnParticles(world, ParticleTypes.HAPPY_VILLAGER, victim.getX(), victim.getY() + 0.5, victim.getZ(), 12, 0.3, 0.3, 0.3, 0.1, false);
    }
}
