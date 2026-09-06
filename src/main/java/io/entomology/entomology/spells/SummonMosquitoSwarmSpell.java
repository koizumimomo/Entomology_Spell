package io.entomology.entomology.spells;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.entity.SummonedMosquitoEntity;
import io.entomology.entomology.registries.SchoolRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * Summons a swarm of crimson mosquitoes (Alex's Mobs). They dive-bite targets
 * - enemy players first, then hostile mobs - and every hit they land heals
 * nearby summons belonging to the same caster. The swarm only lives 20
 * seconds; count scales with spell level (bee swarm count minus one).
 */
public class SummonMosquitoSwarmSpell extends AbstractSpell
{
    private static final int SUMMON_TIME = 400; // 20 seconds

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "summon_mosquito_swarm");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.INSECT_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(120.0)
            .build();

    public SummonMosquitoSwarmSpell()
    {
        this.manaCostPerLevel = 12;
        this.baseSpellPower = 2;
        this.spellPowerPerLevel = 1;
        this.castTime = 25;
        this.baseManaCost = 70;
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
        return Optional.of(SoundEvents.PHANTOM_BITE);
    }

    /** Bee swarm is spellLevel+2; mosquitoes are one fewer. */
    public int getSwarmSize(int spellLevel)
    {
        return spellLevel + 1;
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData)
    {
        if (world instanceof net.minecraft.server.level.ServerLevel)
        {
            int count = this.getSwarmSize(spellLevel);
            for (int i = 0; i < count; i++)
            {
                double angle = Math.toRadians(entity.getYRot() + world.getRandom().nextFloat() * 60.0F - 30.0F)
                        + (Math.PI * 2 / count) * i;
                double xOffset = Math.cos(angle) * 2.0D;
                double zOffset = Math.sin(angle) * 2.0D;
                SummonedMosquitoEntity mosquito = new SummonedMosquitoEntity(world, entity);
                mosquito.moveTo(entity.getX() + xOffset, entity.getY() + 1.2D, entity.getZ() + zOffset,
                        (float) Math.toDegrees(angle) + 180.0F, 0.0F);
                world.addFreshEntity(mosquito);
                SummonManager.initSummon(entity, mosquito, SUMMON_TIME,
                        new io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData());
            }

            MagicManager.spawnParticles(world, ParticleTypes.CRIMSON_SPORE,
                    entity.getX(), entity.getY() + 1.5D, entity.getZ(),
                    30, 1.0D, 0.8D, 1.0D, 0.08D, false);
        }

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster)
    {
        return List.of(
                Component.translatable("ui.irons_spellbooks.summon_count", this.getSwarmSize(spellLevel)),
                Component.translatable("ui.entomology_spell.mosquito_lifesteal"),
                Component.translatable("ui.entomology_spell.mosquito_duration")
        );
    }
}
