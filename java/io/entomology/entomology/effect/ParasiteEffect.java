package io.entomology.entomology.effect;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.registries.EffectRegistry;
import io.entomology.entomology.spells.ParasiteSpell;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import io.entomology.entomology.entity.SummonedSilverfishEntity;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * A parasite incubates inside the host. When the effect ends (times out or is
 * otherwise removed), silverfish hatch around the host and the host is marked
 * with Child's Wrath, which draws every nearby insect to attack it.
 * Amplifier = spell level - 1, and determines how many silverfish hatch.
 */
@Mod.EventBusSubscriber(modid = EntomologyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ParasiteEffect extends MobEffect
{
    public ParasiteEffect()
    {
        super(MobEffectCategory.HARMFUL, 0x4d7c2a);
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event)
    {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null || instance.getEffect() != EffectRegistry.PARASITE.get())
            return;

        LivingEntity host = event.getEntity();
        Level world = host.level();
        if (world.isClientSide || !host.isAlive())
            return;

        // The caster's UUID is stored on the host when the spell is applied, so
        // hatched silverfish can be bound to them as summons.
        CompoundTag data = host.getPersistentData();
        java.util.UUID casterUuid = data.hasUUID(ParasiteSpell.CASTER_UUID_KEY) ? data.getUUID(ParasiteSpell.CASTER_UUID_KEY) : null;
        data.remove(ParasiteSpell.CASTER_UUID_KEY);
        LivingEntity caster = casterUuid != null && world instanceof ServerLevel serverLevel
                && serverLevel.getEntity(casterUuid) instanceof LivingEntity living ? living : null;

        int spellLevel = instance.getAmplifier() + 1;
        int silverfishCount = 3 + spellLevel; // level 1 hatches 4, level 5 hatches 8

        for (int i = 0; i < silverfishCount; i++)
        {
            // Own registered entity type instead of EntityType.SILVERFISH so
            // hatching still works in modpacks with natural spawning disabled
            SummonedSilverfishEntity silverfish = new SummonedSilverfishEntity(world);
            double angle = world.random.nextDouble() * Math.PI * 2.0;
            double distance = 1.5 + world.random.nextDouble() * 1.5;
            silverfish.moveTo(
                    host.getX() + Math.cos(angle) * distance,
                    host.getY() + 0.2,
                    host.getZ() + Math.sin(angle) * distance,
                    world.random.nextFloat() * 360.0F, 0.0F);
            silverfish.setTarget(host);
            if (caster != null)
            {
                SummonManager.setOwner(silverfish, caster);
                SummonManager.setDuration(silverfish, 12000); // 10 minutes
            }
            world.addFreshEntity(silverfish);
        }

        host.addEffect(new MobEffectInstance(EffectRegistry.CHILD_WRATH.get(), 600, 0, false, false, true));

        MagicManager.spawnParticles(world, ParticleTypes.SNEEZE, host.getX(), host.getY() + 1.0, host.getZ(), 24, 0.4, 0.6, 0.4, 0.1, false);
    }
}
