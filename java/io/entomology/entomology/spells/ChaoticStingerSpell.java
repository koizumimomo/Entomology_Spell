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
 * Instantly coats the swarm's stingers with chaotic venom. While active,
 * every bee you have summoned inflicts poison or wither on the creatures it stings.
 * The debuff level scales with spell power.
 */
public class ChaoticStingerSpell extends AbstractSpell
{
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "chaotic_stinger");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.INSECT_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(30.0)
            .build();

    public ChaoticStingerSpell()
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
        entity.addEffect(new MobEffectInstance(EffectRegistry.CHAOTIC_STINGER.get(), duration, amplifier, false, false, true));

        MagicManager.spawnParticles(world, ParticleTypes.WITCH, entity.getX(), entity.getY() + 1.0, entity.getZ(), 12, 0.3, 0.3, 0.3, 0.1, false);

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    /**
     * Every 10 points of spell power grants one level of the applied debuffs.
     * With base power 20, level 1 applies Poison/Wither II.
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
                Component.translatable("ui.entomology_spell.chaotic_stinger_desc"),
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(this.getDuration(spellLevel, caster), 1))
        );
    }

    @Override
    public Vector3f getTargetingColor()
    {
        return Utils.deconstructRGB(EffectRegistry.CHAOTIC_STINGER.get().getColor());
    }
}
