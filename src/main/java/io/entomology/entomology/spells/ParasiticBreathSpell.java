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
 * Coats your silverfish with a parasitic stench (silverfish counterpart of
 * Chaotic Stinger): while active, every silverfish you have summoned inflicts
 * Hunger plus a stacking Slowness on the creatures it bites. The debuff level
 * scales with spell power.
 */
public class ParasiticBreathSpell extends AbstractSpell
{
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "parasitic_breath");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.INSECT_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(30.0)
            .build();

    public ParasiticBreathSpell()
    {
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 20;
        this.spellPowerPerLevel = 5;
        this.castTime = 0;
        this.baseManaCost = 45;
    }

    @Override
    public CastType getCastType()
    {
        return CastType.INSTANT;
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
        entity.addEffect(new MobEffectInstance(EffectRegistry.PARASITIC_BREATH.get(), duration, amplifier, false, false, true));

        MagicManager.spawnParticles(world, ParticleTypes.SNEEZE,
                entity.getX(), entity.getY() + 1.0D, entity.getZ(), 14, 0.3D, 0.3D, 0.3D, 0.1D, false);

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    /**
     * Every 10 points of spell power grants one level of the applied debuffs.
     * With base power 20, level 1 applies Hunger/Slowness II.
     */
    public int getAmplifier(int spellLevel, LivingEntity caster)
    {
        return Math.max(0, (int) (this.getSpellPower(spellLevel, caster) / 10.0F) - 1);
    }

    public int getDuration(int spellLevel, LivingEntity caster)
    {
        return (int) (this.getSpellPower(spellLevel, caster) * 20);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster)
    {
        return List.of(
                Component.translatable("ui.entomology_spell.parasitic_breath_desc"),
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(this.getDuration(spellLevel, caster), 1))
        );
    }

    @Override
    public Vector3f getTargetingColor()
    {
        return Utils.deconstructRGB(EffectRegistry.PARASITIC_BREATH.get().getColor());
    }
}
