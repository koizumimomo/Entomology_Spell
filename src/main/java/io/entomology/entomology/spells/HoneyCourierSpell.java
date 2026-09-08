package io.entomology.entomology.spells;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.entity.HoneyCourierBeeEntity;
import io.entomology.entomology.registries.EntityRegistry;
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
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * Honey Courier: sends out a single peaceful bee that wanders for a
 * spell-level-scaled time (8s at level 1 down to 2s at level 5), then flies
 * back to the caster and delivers health, mana and a little food before
 * vanishing.
 */
public class HoneyCourierSpell extends AbstractSpell
{
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "honey_courier");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.INSECT_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(60.0)
            .build();

    public HoneyCourierSpell()
    {
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 35;
    }

    @Override
    public CastType getCastType()
    {
        return CastType.INSTANT;
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
        return Optional.of(SoundEvents.BEEHIVE_EXIT);
    }

    /** Time before the bee turns back: 8s at level 1 down to 2s at level 5. */
    public int getReturnTime(int spellLevel)
    {
        return Math.max(40, 190 - spellLevel * 30);
    }

    public float getHealAmount(int spellLevel)
    {
        return 6.0F + spellLevel * 2.0F;
    }

    public float getManaAmount(int spellLevel)
    {
        return 15.0F + spellLevel * 5.0F;
    }

    public int getFoodAmount(int spellLevel)
    {
        return spellLevel >= 3 ? 3 : 2;
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData)
    {
        if (world instanceof ServerLevel serverLevel)
        {
            HoneyCourierBeeEntity bee = new HoneyCourierBeeEntity(serverLevel, entity);
            Vec3 offset = new Vec3(Utils.getRandomScaled(3.0D), 1.5D, Utils.getRandomScaled(3.0D));
            bee.moveTo(entity.getEyePosition().add(offset));
            bee.setReturnTime(this.getReturnTime(spellLevel));
            bee.setRewards(this.getHealAmount(spellLevel), this.getManaAmount(spellLevel), this.getFoodAmount(spellLevel));
            // 1.4x vanilla flight speed so the trip home stays snappy
            bee.getAttribute(Attributes.FLYING_SPEED).setBaseValue(0.6D * 1.4D);
            serverLevel.addFreshEntity(bee);
            SummonManager.setOwner(bee, entity);

            MagicManager.spawnParticles(serverLevel, ParticleTypes.FALLING_HONEY,
                    bee.getX(), bee.getY(), bee.getZ(), 16, 0.3D, 0.3D, 0.3D, 0.05D, false);
        }

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster)
    {
        return List.of(
                Component.translatable("ui.entomology_spell.honey_courier_return",
                        Utils.timeFromTicks(this.getReturnTime(spellLevel), 1)),
                Component.translatable("ui.entomology_spell.honey_courier_reward")
        );
    }

    @Override
    public Vector3f getTargetingColor()
    {
        return Utils.deconstructRGB(0xf5a623);
    }
}
