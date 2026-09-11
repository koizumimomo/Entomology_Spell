package io.entomology.entomology.spells;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.entity.ShiraoriEntity;
import io.entomology.entomology.registries.SchoolRegistry;
import io.entomology.entomology.registries.SoundRegistry;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * Summons a single Shiraori the Spider Mother ally that fights for the caster
 * for 5 minutes (6000 ticks). Shiraori is a full {@link ShiraoriEntity} — she
 * inherits NeutralWizard's spell casting and runs the {@link
 * io.entomology.entomology.entity.goal.ShiraoriCombatGoal} rotation (web
 * entangle roots plus periodic Guardian Spider summons).
 *
 * <p>Ownership and lifetime are wired through {@link SummonManager}: damage
 * Shiraori deals with her spells is attributed back to the player via
 * {@link SummonManager#getOwner}, and the SummonManager's expiration queue is
 * what dismisses her once the 6000-tick timer runs out (her
 * {@link ShiraoriEntity#onUnSummon()} then plays the despawn particles and
 * discards her).
 *
 * <p>Scroll-crafting is disabled (the scroll is obtained by branding a spider
 * with Shiraori's Fang — the Shiraori's Attendant ritual — and slaying the
 * branded attendant), and the spell is capped at level 1.
 */
public class SummonShiraoriSpell extends AbstractSpell
{
    /** 5 minutes — matches the task spec. Managed by SummonManager. */
    public static final int SUMMON_TIME = 6000;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "summon_shiraori");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.INSECT_RESOURCE)
            .setMaxLevel(1)
            .setAllowCrafting(false)
            .setCooldownSeconds(180.0)
            .build();

    public SummonShiraoriSpell()
    {
        this.manaCostPerLevel = 0; // single-level spell
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 0;
        this.castTime = 30; // 1.5 second chant
        this.baseManaCost = 150;
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
        return Optional.of(SoundEvents.EVOKER_PREPARE_SUMMON);
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
            super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
        }
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData)
    {
        if (world instanceof ServerLevel serverLevel)
        {
            PlayerRecasts recasts = playerMagicData.getPlayerRecasts();

            if (!recasts.hasRecastForSpell(this))
            {
                SummonedEntitiesCastData castData = new SummonedEntitiesCastData();

                ShiraoriEntity shiraori = new ShiraoriEntity(serverLevel, entity);
                double angle = Math.toRadians(entity.getYRot());
                double dx = -Math.sin(angle) * 1.5D;
                double dz = Math.cos(angle) * 1.5D;
                shiraori.moveTo(entity.getX() + dx, entity.getY() + 1.0D, entity.getZ() + dz, entity.getYRot(), 0.0F);
                serverLevel.addFreshEntity(shiraori);
                SummonManager.initSummon(entity, shiraori, SUMMON_TIME, castData);

                RecastInstance recastInstance = new RecastInstance(
                        this.getSpellId(), spellLevel, this.getRecastCount(spellLevel, entity),
                        SUMMON_TIME, castSource, castData);
                recasts.addRecast(recastInstance, playerMagicData);

                MagicManager.spawnParticles(serverLevel, ParticleTypes.HAPPY_VILLAGER,
                        entity.getX(), entity.getY() + 1.5D, entity.getZ(),
                        24, 0.6D, 0.6D, 0.6D, 0.08D, false);
            }
        }

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster)
    {
        return List.of(
                Component.translatable("ui.entomology_spell.summon_shiraori_desc"),
                Component.translatable("ui.irons_spellbooks.summon_duration", SUMMON_TIME / 20),
                Component.translatable("ui.entomology_spell.shiraori_credits").withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC)
        );
    }
}
