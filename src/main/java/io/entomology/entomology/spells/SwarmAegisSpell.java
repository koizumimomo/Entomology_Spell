package io.entomology.entomology.spells;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.effect.SwarmAegisEffect;
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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * After a short chant, surrounds the caster with a defensive swarm:
 * - Swarm Aegis: reduces incoming damage by a flat amount plus a percentage, scaling with spell power.
 * - Swarm Aid: each melee attack summons two fireflies to strike the target.
 */
public class SwarmAegisSpell extends AbstractSpell
{
    private static final double PARTICLE_RADIUS = 1.5D;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "swarm_aegis");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.INSECT_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(35.0)
            .build();

    public SwarmAegisSpell()
    {
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 2;
        this.castTime = 20; // 1 second chant
        this.baseManaCost = 60;
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
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData)
    {
        int amplifier = this.getAmplifier(spellLevel, entity);
        int duration = this.getDuration(spellLevel, entity);

        entity.addEffect(new MobEffectInstance(EffectRegistry.SWARM_AEGIS.get(), duration, amplifier, false, false, true));
        entity.addEffect(new MobEffectInstance(EffectRegistry.SWARM_AID.get(), duration, amplifier, false, false, true));

        // A green particle ring to visualize the protective swarm
        int count = 24;
        for (int i = 0; i < count; i++)
        {
            double theta = Math.toRadians(360.0 / count) * i;
            double x = Math.cos(theta) * PARTICLE_RADIUS;
            double z = Math.sin(theta) * PARTICLE_RADIUS;
            MagicManager.spawnParticles(world, ParticleTypes.HAPPY_VILLAGER,
                    entity.getX() + x, entity.getY() + 1.0, entity.getZ() + z,
                    1, 0.0, 0.0, 0.0, 0.1, false);
        }

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    /**
     * Every 5 points of spell power grants one level. With the base spell power of 8,
     * level 1 grants amplifier 0 (5 flat + 10% reduction).
     */
    public int getAmplifier(int spellLevel, LivingEntity caster)
    {
        return Math.max(0, (int) (this.getSpellPower(spellLevel, caster) / 5.0F) - 1);
    }

    public int getDuration(int spellLevel, LivingEntity caster)
    {
        return (int) (this.getSpellPower(spellLevel, caster) * 20);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster)
    {
        int amplifier = this.getAmplifier(spellLevel, caster);
        return List.of(
                Component.translatable("ui.entomology_spell.swarm_aegis_desc",
                        String.format("%.0f", SwarmAegisEffect.getFlatReduction(amplifier)),
                        String.format("%.0f", SwarmAegisEffect.getPercentReduction(amplifier) * 100.0F)),
                Component.translatable("ui.entomology_spell.swarm_aid_desc"),
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(this.getDuration(spellLevel, caster), 1))
        );
    }

    @Override
    public Vector3f getTargetingColor()
    {
        return Utils.deconstructRGB(EffectRegistry.SWARM_AEGIS.get().getColor());
    }
}
