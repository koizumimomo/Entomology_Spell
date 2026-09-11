package io.entomology.entomology.spells;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.entity.SummonedCockroach;
import io.entomology.entomology.registries.SchoolRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.PlayerRecasts;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Summons a troupe of dancing cockroaches around the caster. They permanently
 * shake maracas (vanilla dance animation) and pulse a rhythm aura of
 * regen/speed/haste/strength onto friendly creatures nearby.
 */
public class SummonCockroachDanceSpell extends AbstractSpell
{
    private static final int SUMMON_TIME = 12000; // 10 minutes
    /** Chance for a summoned roach to be iridescent (Alex's Mobs rainbow glint). */
    private static final float RAINBOW_CHANCE = 0.10F;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "summon_cockroach_dance");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.INSECT_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(600.0)
            .build();

    public SummonCockroachDanceSpell()
    {
        this.manaCostPerLevel = 15;
        this.baseSpellPower = 2;
        this.spellPowerPerLevel = 1;
        this.castTime = 30; // 1.5 second chant
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
        return Optional.of(SoundEvents.NOTE_BLOCK_BASEDRUM.value());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound()
    {
        return Optional.of(SoundEvents.NOTE_BLOCK_HAT.value());
    }

    public int getTroupeSize(int spellLevel)
    {
        return 2 + spellLevel / 2; // level 1 -> 2 roaches, level 5 -> 4 roaches
    }

    @Override
    public int getRecastCount(int spellLevel, LivingEntity entity)
    {
        return 2;
    }

    @Override
    public ICastDataSerializable getEmptyCastData()
    {
        return new SummonedEntitiesCastData();
    }

    @Override
    public void onRecastFinished(ServerPlayer serverPlayer, RecastInstance recastInstance, RecastResult recastResult, ICastDataSerializable castDataSerializable)
    {
        if (SummonManager.recastFinishedHelper(serverPlayer, recastInstance, recastResult, castDataSerializable))
        {
            serverPlayer.removeEffect(io.entomology.entomology.registries.EffectRegistry.SUMMONED_COCKROACH_DANCE.get());
            super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
        }
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData)
    {
        if (world instanceof net.minecraft.server.level.ServerLevel serverLevel)
        {
            PlayerRecasts recasts = playerMagicData.getPlayerRecasts();

            if (!recasts.hasRecastForSpell(this))
            {
                SummonedEntitiesCastData castData = new SummonedEntitiesCastData();
                int count = this.getTroupeSize(spellLevel);
                for (int i = 0; i < count; i++)
                {
                    double angle = Math.toRadians(entity.getYRot()) + (Math.PI * 2 / count) * i;
                    double xOffset = Math.cos(angle) * 2.5;
                    double zOffset = Math.sin(angle) * 2.5;
                    SummonedCockroach roach = new SummonedCockroach(world, entity, spellLevel);
                    roach.moveTo(entity.getX() + xOffset, entity.getY(), entity.getZ() + zOffset,
                            entity.getYRot(), 0.0F);
                    world.addFreshEntity(roach);
                    if (serverLevel.random.nextFloat() < RAINBOW_CHANCE)
                    {
                        com.github.alexthe666.alexsmobs.entity.util.RainbowUtil.setRainbowType(roach, 1);
                    }
                    SummonManager.initSummon(entity, roach, SUMMON_TIME, castData);
                }

                entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        io.entomology.entomology.registries.EffectRegistry.SUMMONED_COCKROACH_DANCE.get(),
                        SUMMON_TIME, 0, false, false, true));

                RecastInstance recastInstance = new RecastInstance(
                        this.getSpellId(), spellLevel, this.getRecastCount(spellLevel, entity),
                        SUMMON_TIME, castSource, castData);
                recasts.addRecast(recastInstance, playerMagicData);

                MagicManager.spawnParticles(world, ParticleTypes.NOTE,
                        entity.getX(), entity.getY() + 1.5, entity.getZ(),
                        30, 1.2, 0.8, 1.2, 0.1, false);
            }
        }

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster)
    {
        return List.of(
                Component.translatable("ui.entomology_spell.cockroach_troupe", 2 + spellLevel / 2),
                Component.translatable("ui.entomology_spell.cockroach_rainbow"),
                Component.translatable("ui.entomology_spell.cockroach_aura")
        );
    }
}
