package io.entomology.entomology.spells;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.registries.EffectRegistry;
import io.entomology.entomology.registries.SchoolRegistry;
import io.entomology.entomology.registries.SoundRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Marks the targeted entity with Swarm Exemption: while marked, its Insect
 * Kinship protection (Weaver Spider Chelicerae and similar) no longer keeps
 * insects away, so your swarm can attack it even in summoner-vs-summoner
 * standoffs. Requires a selected target.
 */
public class SwarmExemptionSpell extends AbstractSpell
{
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "swarm_exemption");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.INSECT_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(30.0)
            .build();

    public SwarmExemptionSpell()
    {
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 30;
        this.spellPowerPerLevel = 5;
        this.castTime = 20; // 1 second chant
        this.baseManaCost = 35;
    }

    @Override
    public CastType getCastType()
    {
        return CastType.LONG;
    }

    @Override
    public DefaultConfig getDefaultConfig()
    {
        return this.defaultConfig;
    }

    @Override
    public ResourceLocation getSpellResource()
    {
        return this.spellId;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound()
    {
        return Optional.of(SoundRegistry.INSECT_CAST.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound()
    {
        return Optional.of(SoundEvents.VINDICATOR_CELEBRATE);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData)
    {
        // Requires a selected target; without one the cast is refused
        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, 32, 0.35f);
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData)
    {
        LivingEntity target = null;
        if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData)
        {
            target = targetData.getTarget((ServerLevel) world);
        }
        if (target == null || target.isDeadOrDying())
        {
            return;
        }

        int duration = this.getDuration(spellLevel, entity);
        target.addEffect(new MobEffectInstance(
                EffectRegistry.SWARM_EXEMPTION.get(), duration, 0, false, false, true));

        // Command ALL of the caster's own summons to attack the marked target
        // (mirrors how swarm_wrath rallies bees, but applied to every summon).
        // Bees need their persistent-anger flag set for their melee BeeAttackGoal
        // to fire; other mobs (e.g. the butterfly princess) just need setTarget.
        ServerLevel serverLevel = (ServerLevel) world;
        for (UUID summonId : SummonManager.getSummons(entity))
        {
            Entity summonEntity = serverLevel.getEntity(summonId);
            if (!(summonEntity instanceof LivingEntity summon) || !summon.isAlive() || summon == target)
            {
                continue;
            }
            if (summon instanceof Bee bee)
            {
                bee.setTarget(target);
                bee.setRemainingPersistentAngerTime(duration);
                bee.setPersistentAngerTarget(target.getUUID());
            }
            else if (summon instanceof Mob mob)
            {
                mob.setTarget(target);
            }
        }

        MagicManager.spawnParticles(world, ParticleTypes.ANGRY_VILLAGER,
                target.getX(), target.getY() + target.getBbHeight() * 0.9D, target.getZ(),
                12, 0.4D, 0.4D, 0.4D, 0.05D, false);

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    public int getDuration(int spellLevel, LivingEntity caster)
    {
        return (int) (this.getSpellPower(spellLevel, caster) * 20);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster)
    {
        return List.of(
                Component.translatable("ui.entomology_spell.swarm_exemption_desc"),
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(this.getDuration(spellLevel, caster), 1))
        );
    }

    @Override
    public Vector3f getTargetingColor()
    {
        return Utils.deconstructRGB(EffectRegistry.SWARM_EXEMPTION.get().getColor());
    }
}
