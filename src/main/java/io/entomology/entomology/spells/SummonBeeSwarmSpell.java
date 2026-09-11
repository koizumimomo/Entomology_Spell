package io.entomology.entomology.spells;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.entity.SummonedBeeEntity;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Summons a swarm of bees that defend the caster, attacking whatever the caster last struck.
 * Also grants:
 * - Summoned Bee Swarm (status indicator while the swarm lives)
 * - Queen Bee (attacks mark targets with Swarm Wrath: +25% damage taken from
 *   summoned bees and insect school spells)
 */
public class SummonBeeSwarmSpell extends AbstractSpell
{
    private static final int SUMMON_TIME = 12000; // 10 minutes

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "summon_bee_swarm");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.INSECT_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(150.0)
            .build();

    public SummonBeeSwarmSpell()
    {
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 2;
        this.spellPowerPerLevel = 1;
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
        return Optional.of(SoundEvents.EVOKER_PREPARE_SUMMON);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound()
    {
        return Optional.of(SoundEvents.BEEHIVE_EXIT);
    }

    public int getSummonCount(int spellLevel, LivingEntity caster)
    {
        return spellLevel + 2;
    }

    /**
     * Damage per bee sting, scaling with spell power (which already includes
     * global spell power and insect school power modifiers).
     */
    public double getBeeDamage(int spellLevel, LivingEntity caster)
    {
        return 2.0 + this.getSpellPower(spellLevel, caster) * 1.5;
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
            serverPlayer.removeEffect(EffectRegistry.SUMMONED_BEE_SWARM.get());
            serverPlayer.removeEffect(EffectRegistry.QUEEN_BEE.get());
            super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
        }
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData)
    {
        int count = this.getSummonCount(spellLevel, entity);

        if (world instanceof ServerLevel serverLevel)
        {
            PlayerRecasts recasts = playerMagicData.getPlayerRecasts();

            // First cast: summon the swarm and create a recast instance so a
            // second cast (or timeout) auto-dismisses all swarm members.
            if (!recasts.hasRecastForSpell(this))
            {
                SummonedEntitiesCastData castData = new SummonedEntitiesCastData();

                for (int i = 0; i < count; i++)
                {
                    SummonedBeeEntity bee = new SummonedBeeEntity(world, entity);
                    Vec3 offset = new Vec3(Utils.getRandomScaled(2.0), 0.5, Utils.getRandomScaled(2.0));
                    bee.moveTo(entity.getEyePosition().add(offset));
                    bee.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(this.getBeeDamage(spellLevel, entity));
                    bee.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D * 1.8D);
                    double baseHealth = bee.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
                    bee.getAttribute(Attributes.MAX_HEALTH).setBaseValue(
                            baseHealth + this.getSpellPower(spellLevel, entity) * 2.0D + (spellLevel - 1) * 4.0D);
                    bee.setHealth(bee.getMaxHealth());
                    world.addFreshEntity(bee);
                    SummonManager.initSummon(entity, bee, SUMMON_TIME, castData);
                }

                entity.addEffect(new MobEffectInstance(EffectRegistry.SUMMONED_BEE_SWARM.get(), SUMMON_TIME, 0, false, false, true));
                entity.addEffect(new MobEffectInstance(EffectRegistry.QUEEN_BEE.get(), SUMMON_TIME, 0, false, false, true));

                RecastInstance recastInstance = new RecastInstance(
                        this.getSpellId(), spellLevel, this.getRecastCount(spellLevel, entity),
                        SUMMON_TIME, castSource, castData);
                recasts.addRecast(recastInstance, playerMagicData);
            }
        }

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster)
    {
        return List.of(
                Component.translatable("ui.irons_spellbooks.summon_count", this.getSummonCount(spellLevel, caster)),
                Component.translatable("ui.entomology_spell.queen_bee_desc")
        );
    }
}
