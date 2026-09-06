package io.entomology.entomology.event;

import io.entomology.entomology.Config;
import io.entomology.entomology.EntomologyMod;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Config-gated rule: when preventSameSummonerFriendlyFire is enabled, a
 * summon can no longer damage another summon that belongs to the same owner.
 *
 * Deliberately narrower than "insects never hurt insects": summons still
 * attack wild insects and other players' summons, so farming swarm school
 * materials (royal jelly, venom glands) with summon damage keeps working.
 * Covers melee, bee stings and projectiles (the projectile's owner is the
 * responsible entity, so alarm bee stingers are caught too).
 */
@Mod.EventBusSubscriber(modid = EntomologyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SummonFriendlyFireHandler
{
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event)
    {
        if (!Config.preventSameSummonerFriendlyFire)
            return;

        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();
        Entity attacker = source == null ? null : source.getEntity();
        if (attacker == null || attacker == victim)
            return;

        Entity attackerOwner = SummonManager.getOwner(attacker);
        if (attackerOwner == null)
            return;
        Entity victimOwner = SummonManager.getOwner(victim);
        if (victimOwner != null && victimOwner == attackerOwner)
        {
            event.setCanceled(true);
        }
    }
}
