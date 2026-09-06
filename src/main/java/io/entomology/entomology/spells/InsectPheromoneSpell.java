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
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * Chant for one second, then mark the targeted entity (and every entity within a 2 block radius
 * of it) with Insect Pheromone. If no target was selected, the caster is marked instead.
 */
public class InsectPheromoneSpell extends AbstractSpell
{
    private static final double EFFECT_RADIUS = 2.0D;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "insect_pheromone");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.INSECT_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(25.0)
            .build();

    public InsectPheromoneSpell()
    {
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 30;
        this.spellPowerPerLevel = 8;
        this.castTime = 20; // 1 second of chanting
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
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData)
    {
        // Try to pick a target with the crosshair. Failing to find one is fine -
        // the spell will fall back to the caster (silently, so no error message is shown).
        Utils.preCastTargetHelper(level, entity, playerMagicData, this, 32, 0.35f, false);
        return true;
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData)
    {
        LivingEntity center = entity; // fallback: apply to self
        if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData)
        {
            LivingEntity selected = targetData.getTarget((ServerLevel) world);
            if (selected != null)
                center = selected;
        }

        int duration = this.getDuration(spellLevel, entity);
        for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, center.getBoundingBox().inflate(EFFECT_RADIUS)))
        {
            if (target.distanceToSqr(center.position()) <= EFFECT_RADIUS * EFFECT_RADIUS)
            {
                target.addEffect(new MobEffectInstance(EffectRegistry.INSECT_PHEROMONE.get(), duration, 0, false, false, true));
            }
        }

        // A small green particle ring to visualize the marked area
        int count = 24;
        for (int i = 0; i < count; i++)
        {
            double theta = Math.toRadians(360.0 / count) * i;
            double x = Math.cos(theta) * EFFECT_RADIUS;
            double z = Math.sin(theta) * EFFECT_RADIUS;
            MagicManager.spawnParticles(world, ParticleTypes.HAPPY_VILLAGER,
                    center.getX() + x, center.getY() + 0.5, center.getZ() + z,
                    1, 0.0, 0.0, 0.0, 0.1, false);
        }

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    public int getDuration(int spellLevel, LivingEntity caster)
    {
        return (int) (this.getSpellPower(spellLevel, caster) * 20);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster)
    {
        return List.of(
                Component.translatable("ui.entomology_spell.insect_pheromone_desc"),
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(this.getDuration(spellLevel, caster), 1))
        );
    }

    @Override
    public Vector3f getTargetingColor()
    {
        return Utils.deconstructRGB(EffectRegistry.INSECT_PHEROMONE.get().getColor());
    }
}
