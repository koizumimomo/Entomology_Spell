package io.entomology.entomology.spells;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.entity.SummonedIceSpiderEntity;
import io.entomology.entomology.registries.SchoolRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Summons a single rideable frost spider that fights for the caster.
 * Modeled after T.O. Magic's Echo of the Abyss (Summoned Hullbreaker):
 * the summon is registered with SummonManager (owner + lifetime tracking),
 * and a RecastInstance lets a second cast recall/replace it. Right-click the
 * spider to ride it.
 */
public class SummonIceSpiderSpell extends AbstractSpell
{
    private static final int SUMMON_TIME = 12000; // 10 minutes

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "summon_ice_spider");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.INSECT_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(300.0)
            .build();

    public SummonIceSpiderSpell()
    {
        this.manaCostPerLevel = 20;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 40; // 2 second chant
        this.baseManaCost = 100;
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
        return Optional.of(SoundEvents.SPIDER_AMBIENT);
    }

    public double getSpiderHealth(int spellLevel)
    {
        return 40.0 + spellLevel * 20.0;
    }

    public double getSpiderDamage(int spellLevel, LivingEntity caster)
    {
        return 4.0 + this.getSpellPower(spellLevel, caster) * 1.5;
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData)
    {
        if (world instanceof net.minecraft.server.level.ServerLevel serverLevel)
        {
            // Resummoning while one is alive recalls it: SummonManager's recast
            // handling despawns the old spider when this recast fires.
            SummonedEntitiesCastData castData = new SummonedEntitiesCastData();

            // Spawn in front of the caster, snapped to the ground.
            Vec3 spawnPos = entity.position()
                    .add(entity.getLookAngle().multiply(1, 0, 1).normalize().scale(3.0));
            SummonedIceSpiderEntity spider = new SummonedIceSpiderEntity(world, entity);
            spider.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, entity.getYRot(), 0.0F);
            spider.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                    .setBaseValue(this.getSpiderHealth(spellLevel));
            spider.setHealth(spider.getMaxHealth());
            spider.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                    .setBaseValue(this.getSpiderDamage(spellLevel, entity));
            // Prevent the wild spider's random-player/golem hostility from ever engaging
            spider.setTarget(null);

            world.addFreshEntity(spider);
            // Sets owner + starts the lifetime countdown (auto onUnSummon on expiry)
            SummonManager.initSummon(entity, spider, SUMMON_TIME, castData);

            MagicManager.spawnParticles(world, ParticleTypes.SNOWFLAKE,
                    spawnPos.x, spawnPos.y + 1.0, spawnPos.z,
                    40, 0.5, 0.8, 0.5, 0.03, false);
        }

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster)
    {
        return List.of(
                Component.translatable("ui.entomology_spell.ice_spider_health",
                        Utils.stringTruncation(this.getSpiderHealth(spellLevel), 1)),
                Component.translatable("ui.entomology_spell.ice_spider_damage",
                        Utils.stringTruncation(this.getSpiderDamage(spellLevel, caster), 1)),
                Component.translatable("ui.entomology_spell.ice_spider_ride")
        );
    }
}
