package io.entomology.entomology.spells;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.entity.ButterflyEntity;
import io.entomology.entomology.effect.ButterflyLiftEffect;
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
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * Summons a single butterfly swarm entity (group of 15 butterflies) that flies
 * to the target's head. The target is lifted (ButterflyLiftEffect) up to a
 * height that scales with spell level (5 blocks at level 1, 10 at level 2),
 * then dropped for fall damage. Fixed 2 levels, not craftable on the scroll
 * table — only obtainable by right-clicking a wild butterfly with royal jelly.
 */
public class SummonButterflySpell extends AbstractSpell
{
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "summon_butterfly");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.INSECT_RESOURCE)
            .setMaxLevel(2)
            .setCooldownSeconds(20.0)
            .setAllowCrafting(false)
            .build();

    public SummonButterflySpell()
    {
        this.manaCostPerLevel = 15;
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 5;
        this.castTime = 20; // 1 second
        this.baseManaCost = 40;
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
        return Optional.of(SoundEvents.EVOKER_CAST_SPELL);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData)
    {
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
            return;

        // Spawn a swarm of individual butterflies that fly to the target's head.
        // Kept small to avoid GeckoLib render overhead from too many animated entities.
        int swarmSize = 4 + spellLevel * 2; // 6 at level 1, 8 at level 2
        for (int i = 0; i < swarmSize; i++)
        {
            ButterflyEntity butterfly = new ButterflyEntity(world, entity, false);
            double angle = (i / (double) swarmSize) * Math.PI * 2;
            double radius = 1.5D;
            butterfly.moveTo(
                    entity.getEyePosition().x + Math.cos(angle) * radius,
                    entity.getEyePosition().y + 1.0D,
                    entity.getEyePosition().z + Math.sin(angle) * radius);
            butterfly.setTargetEntity(target);
            butterfly.setLifetime(140); // 7 seconds visual (lift effect lasts 3-4s)
            world.addFreshEntity(butterfly);
        }

        // Apply the lift effect: amplifier = spellLevel - 1 (0 → 5 blocks, 1 → 10 blocks)
        ButterflyLiftEffect.markStartY(target);
        int duration = 60 + spellLevel * 20; // 3-4 seconds of lifting
        target.addEffect(new MobEffectInstance(
                EffectRegistry.BUTTERFLY_LIFT.get(), duration, spellLevel - 1, false, true, true));

        MagicManager.spawnParticles(world, ParticleTypes.HAPPY_VILLAGER,
                target.getX(), target.getY() + target.getBbHeight(), target.getZ(),
                16, 0.5, 0.5, 0.5, 0.1, false);

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    public int getMaxLiftHeight(int spellLevel)
    {
        return ButterflyLiftEffect.BLOCKS_PER_STEP * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster)
    {
        return List.of(
                Component.translatable("ui.entomology_spell.butterfly_lift_height", getMaxLiftHeight(spellLevel)),
                Component.translatable("ui.entomology_spell.butterfly_credits").withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC)
        );
    }

    @Override
    public Vector3f getTargetingColor()
    {
        return Utils.deconstructRGB(0xff66cc);
    }
}
