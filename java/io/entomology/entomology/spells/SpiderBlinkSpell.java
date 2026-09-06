package io.entomology.entomology.spells;

import io.entomology.entomology.EntomologyMod;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * After a short delay, teleport to the targeted entity's location.
 * Inspired by T.O. Magic's Spectral Blink, but with an insect theme.
 */
public class SpiderBlinkSpell extends AbstractSpell
{
    private static final double MAX_TELEPORT_DISTANCE = 24.0D;
    private static final int CHARGE_TIME_TICKS = 20; // 1 second charge
    
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "spider_blink");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.INSECT_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(15.0)
            .build();

    public SpiderBlinkSpell()
    {
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 1;
        this.castTime = CHARGE_TIME_TICKS;
        this.baseManaCost = 25;
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
        // Try to pick a target with the crosshair
        Utils.preCastTargetHelper(level, entity, playerMagicData, this, 32, 0.35f, false);
        return true;
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData)
    {
        if (!(world instanceof ServerLevel serverLevel))
            return;

        Entity target = null;
        if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData)
        {
            LivingEntity selected = targetData.getTarget(serverLevel);
            if (selected != null && selected != entity)
                target = selected;
        }

        // Teleport to target or random nearby entity if no target selected
        Vec3 targetPos;
        if (target != null && entity.distanceToSqr(target) <= MAX_TELEPORT_DISTANCE * MAX_TELEPORT_DISTANCE)
        {
            targetPos = target.position().add(0, 0.5, 0);
        }
        else
        {
            // Find random nearby entity to teleport to
            Entity nearbyEntity = world.getEntitiesOfClass(LivingEntity.class,
                            entity.getBoundingBox().inflate(MAX_TELEPORT_DISTANCE))
                    .stream()
                    .filter(e -> e != entity && !e.isSpectator() && e.isAlive())
                    .findFirst()
                    .orElse(null);
            
            if (nearbyEntity != null)
                targetPos = nearbyEntity.position().add(0, 0.5, 0);
            else
                targetPos = entity.position().add(entity.getViewVector(1.0F).multiply(3, 0, 3)); // Move forward if no target
        }

        // Create spider web particles at departure and arrival
        spawnWebParticles(world, entity.position());
        entity.teleportTo(targetPos.x, targetPos.y, targetPos.z);
        spawnWebParticles(world, targetPos);

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    private void spawnWebParticles(Level world, Vec3 pos)
    {
        if (!(world instanceof ServerLevel))
            return;

        for (int i = 0; i < 12; i++)
        {
            double theta = Math.toRadians(360.0 / 12) * i;
            double x = Math.cos(theta) * 0.8;
            double z = Math.sin(theta) * 0.8;
            double y = Math.random() * 1.5;
            
            MagicManager.spawnParticles(world, ParticleTypes.WARPED_SPORE,
                    pos.x + x, pos.y + y, pos.z + z,
                    1, 0.0, 0.0, 0.0, 0.05, false);
        }
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster)
    {
        return List.of(
                Component.translatable("ui.entomology_spell.spider_blink_desc"),
                Component.translatable("ui.entomology_spell.teleport_distance", String.format("%.0f", MAX_TELEPORT_DISTANCE)),
                Component.translatable("ui.irons_spellbooks.cast_time", Utils.timeFromTicks(this.castTime, 1))
        );
    }

    @Override
    public Vector3f getTargetingColor()
    {
        return new Vector3f(0.7f, 0.5f, 0.3f); // Brown spider-like color
    }
}