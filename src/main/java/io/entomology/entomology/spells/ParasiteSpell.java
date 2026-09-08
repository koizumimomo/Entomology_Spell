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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Infects the targeted entity with a Parasite. The higher the spell level, the
 * shorter the incubation time. When the parasite bursts, silverfish hatch
 * around the target and it gains Child's Wrath, drawing all nearby insects
 * to attack it. Falls back to the caster when no target is selected.
 */
public class ParasiteSpell extends AbstractSpell
{
    public static final String CASTER_UUID_KEY = "entomologyParasiteCaster";
    public static final String POWER_KEY = "entomologyParasitePower";

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "parasite");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.INSECT_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(35.0)
            .build();

    public ParasiteSpell()
    {
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 20;
        this.spellPowerPerLevel = 5;
        this.castTime = 30; // 1.5 seconds of chanting
        this.baseManaCost = 45;
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
        // Require a selected target: casting without one is cancelled and shows
        // the "a target is required" message (same behaviour as iron's spells).
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
        if (target == null)
            return; // defensive: checkPreCastConditions already guarantees a target

        int duration = this.getDuration(spellLevel);
        target.addEffect(new MobEffectInstance(EffectRegistry.PARASITE.get(), duration, spellLevel - 1, false, false, true));
        target.getPersistentData().putUUID(CASTER_UUID_KEY, entity.getUUID());
        target.getPersistentData().putFloat(POWER_KEY, this.getSpellPower(spellLevel, entity));

        MagicManager.spawnParticles(world, ParticleTypes.SNEEZE, target.getX(), target.getY() + 1.0, target.getZ(), 16, 0.3, 0.5, 0.3, 0.1, false);

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    /**
     * Higher spell levels mean shorter incubation: level 1 = 30s, level 5 = 10s.
     */
    public int getDuration(int spellLevel)
    {
        return Math.max(100, 600 - (spellLevel - 1) * 100);
    }

    public int getSilverfishCount(int spellLevel)
    {
        return 3 + spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster)
    {
        return List.of(
                Component.translatable("ui.entomology_spell.parasite_duration", Utils.timeFromTicks(this.getDuration(spellLevel), 1)),
                Component.translatable("ui.entomology_spell.parasite_silverfish", this.getSilverfishCount(spellLevel))
        );
    }

    @Override
    public Vector3f getTargetingColor()
    {
        return Utils.deconstructRGB(EffectRegistry.PARASITE.get().getColor());
    }
}
