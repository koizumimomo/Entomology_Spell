package io.entomology.entomology.spells;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.entity.SummonedIceSpiderEntity;
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
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * Butterfly Lovers (变形): transforms specific insects into stronger forms.
 * Spider/Cave Spider → Summoned Ice Spider (owned by caster).
 * Alex's Mobs Fly → Crimson Mosquito → Warped Mosco (chain, each cast advances one step).
 * Only transforms entities the mod recognises; others are unaffected.
 */
public class ButterflyLoversSpell extends AbstractSpell
{
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "butterfly_lovers");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.INSECT_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(60.0)
            .build();

    public ButterflyLoversSpell()
    {
        this.manaCostPerLevel = 20;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 0;
        this.castTime = 30; // 1.5 seconds
        this.baseManaCost = 50;
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
        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, 16, 0.35f);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData playerMagicData)
    {
        LivingEntity target = null;
        if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData)
        {
            target = targetData.getTarget((ServerLevel) level);
        }
        if (target == null || target.isDeadOrDying())
            return;

        String entityId = EntityType.getKey(target.getType()).toString();

        boolean transformed = false;

        // Spider / Cave Spider → Summoned Ice Spider
        if (entityId.equals("minecraft:spider") || entityId.equals("minecraft:cave_spider"))
        {
            SummonedIceSpiderEntity iceSpider = new SummonedIceSpiderEntity(level, caster);
            iceSpider.moveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
            iceSpider.finalizeSpawn(((ServerLevel) level), level.getCurrentDifficultyAt(iceSpider.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            level.addFreshEntity(iceSpider);
            target.discard();
            transformed = true;
        }
        // Alex's Mobs chain: Fly → Crimson Mosquito → Warped Mosco
        else if (ModList.get().isLoaded("alexsmobs"))
        {
            if (entityId.equals("alexsmobs:fly"))
            {
                spawnAlexMob(level, "alexsmobs:crimson_mosquito", target);
                target.discard();
                transformed = true;
            }
            else if (entityId.equals("alexsmobs:crimson_mosquito"))
            {
                spawnAlexMob(level, "alexsmobs:warped_mosco", target);
                target.discard();
                transformed = true;
            }
        }

        if (transformed)
        {
            MagicManager.spawnParticles(level, ParticleTypes.HAPPY_VILLAGER,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    16, 0.4, 0.5, 0.4, 0.1, false);
        }

        super.onCast(level, spellLevel, caster, castSource, playerMagicData);
    }

    /**
     * Spawns an Alex's Mobs entity by EntityType key, copying position/rotation
     * from the original. Uses reflection-free Forge registry lookup so the mod
     * compiles without Alex's Mobs as a hard dependency.
     */
    @SuppressWarnings("unchecked")
    private void spawnAlexMob(Level level, String entityKey, LivingEntity original)
    {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse(entityKey));
        if (type == null)
            return;
        Entity mob = type.create(level);
        if (mob == null)
            return;
        mob.moveTo(original.getX(), original.getY(), original.getZ(), original.getYRot(), original.getXRot());
        if (mob instanceof Mob mobEntity && level instanceof ServerLevel serverLevel)
        {
            mobEntity.finalizeSpawn(serverLevel, level.getCurrentDifficultyAt(mobEntity.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
        }
        level.addFreshEntity(mob);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster)
    {
        return List.of(
                Component.translatable("ui.entomology_spell.butterfly_lovers_desc"),
                Component.translatable("ui.entomology_spell.butterfly_credits").withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC)
        );
    }

    @Override
    public Vector3f getTargetingColor()
    {
        return Utils.deconstructRGB(0xff66cc);
    }
}
