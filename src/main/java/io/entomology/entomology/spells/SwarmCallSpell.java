package io.entomology.entomology.spells;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.effect.SwarmCallEffect;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * Calls the swarm to guard its queen: while the buff lasts, taking damage has
 * a chance to summon bees that immediately swarm the attacker. The chance and
 * (from level 3) the bee count scale with the spell level.
 */
public class SwarmCallSpell extends AbstractSpell
{
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "swarm_call");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.INSECT_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(60.0)
            .build();

    public SwarmCallSpell()
    {
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 30;
        this.spellPowerPerLevel = 5;
        this.castTime = 0;
        this.baseManaCost = 40;
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
    public Optional<SoundEvent> getCastFinishSound()
    {
        return Optional.of(SoundEvents.BEEHIVE_EXIT);
    }

    /** Buff amplifier: 0 at level 1 up to 4 at level 5. */
    public int getAmplifier(int spellLevel)
    {
        return Math.min(4, Math.max(0, spellLevel - 1));
    }

    public int getDuration(int spellLevel, LivingEntity caster)
    {
        return (int) (this.getSpellPower(spellLevel, caster) * 20);
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData)
    {
        int amplifier = this.getAmplifier(spellLevel);
        int duration = this.getDuration(spellLevel, entity);
        entity.addEffect(new MobEffectInstance(EffectRegistry.SWARM_CALL.get(), duration, amplifier, false, false, true));

        MagicManager.spawnParticles(world, ParticleTypes.WAX_ON,
                entity.getX(), entity.getY() + 1.0D, entity.getZ(), 16, 0.4D, 0.5D, 0.4D, 0.15D, false);

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster)
    {
        return List.of(
                Component.translatable("ui.entomology_spell.swarm_call_chance",
                        String.format("%.0f%%", SwarmCallEffect.getProcChance(this.getAmplifier(spellLevel)) * 100.0F)),
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(this.getDuration(spellLevel, caster), 1))
        );
    }

    @Override
    public Vector3f getTargetingColor()
    {
        return Utils.deconstructRGB(EffectRegistry.SWARM_CALL.get().getColor());
    }
}
