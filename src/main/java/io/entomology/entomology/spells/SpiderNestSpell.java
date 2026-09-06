package io.entomology.entomology.spells;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.entity.SpiderNestEntity;
import io.entomology.entomology.registries.EffectRegistry;
import io.entomology.entomology.registries.SchoolRegistry;
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
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * Summons a spider nest entity that periodically spawns spiders to defend the area.
 * The nest has health and can be destroyed. Spiders prioritize protecting the nest,
 * then the caster, and never attack the caster.
 */
public class SpiderNestSpell extends AbstractSpell
{
    private static final int BASE_DURATION_TICKS = 2400; // 120 seconds
    
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "spider_nest");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.INSECT_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(15.0)
            .build();

    public SpiderNestSpell()
    {
        this.manaCostPerLevel = 20;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 1;
        this.castTime = 40; // 2 seconds cast
        this.baseManaCost = 80;
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
        return Optional.of(SoundEvents.EVOKER_PREPARE_SUMMON);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound()
    {
        return Optional.of(SoundEvents.SPIDER_STEP);
    }

    public int getDuration(int spellLevel, LivingEntity caster)
    {
        return BASE_DURATION_TICKS + (spellLevel * 120); // +6 seconds per level
    }

    public float getNestHealth(int spellLevel, LivingEntity caster)
    {
        return 10.0F; // 5 hearts
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData)
    {
        if (world.isClientSide)
        {
            super.onCast(world, spellLevel, entity, castSource, playerMagicData);
            return;
        }

        if (entity == null)
        {
            super.onCast(world, spellLevel, null, castSource, playerMagicData);
            return;
        }

        // Nest is placed in place at the caster's feet; the model is offset
        // downwards, so anchor above the ground to keep it out of the dirt
        Vec3 nestPos = new Vec3(entity.position().x, entity.position().y + 0.3, entity.position().z);

        // Spawn spider nest
        SpiderNestEntity nest = new SpiderNestEntity(world, nestPos, entity, spellLevel);
        world.addFreshEntity(nest);

        // Duration buff: shows remaining time on the nest; it collapses when it runs out
        nest.addEffect(new MobEffectInstance(EffectRegistry.NEST_DURATION.get(),
                this.getDuration(spellLevel, entity), 0, false, false, true));

        // First wave immediately
        nest.summonSpiders();

        // Visual effect - web particles
        for (int i = 0; i < 16; i++)
        {
            double theta = Math.toRadians(360.0 / 16) * i;
            double x = Math.cos(theta) * 1.5;
            double z = Math.sin(theta) * 1.5;
            
            MagicManager.spawnParticles(world, ParticleTypes.SQUID_INK,
                    nestPos.x + x, nestPos.y + 1.0, nestPos.z + z,
                    1, 0.0, 0.1, 0.0, 0.05, false);
        }

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster)
    {
        return List.of(
                Component.translatable("ui.entomology_spell.spider_nest_health", 
                        String.format("%.0f", this.getNestHealth(spellLevel, caster))),
                Component.translatable("ui.irons_spellbooks.effect_length", 
                        Utils.timeFromTicks(this.getDuration(spellLevel, caster), 1))
        );
    }

    @Override
    public Vector3f getTargetingColor()
    {
        return new Vector3f(0.5f, 0.3f, 0.2f); // Spider web color
    }
}