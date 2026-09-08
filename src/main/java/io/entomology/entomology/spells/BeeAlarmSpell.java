package io.entomology.entomology.spells;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.entity.BeeStingerProjectile;
import io.entomology.entomology.entity.SummonedBeeEntity;
import io.entomology.entomology.registries.EffectRegistry;
import io.entomology.entomology.registries.EntityRegistry;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Places alarm bees at fixed posts around the caster. Each bee stays at its
 * post and shoots bee stinger projectiles at hostile entities within detection
 * range. Bee count: 1 at level 1, up to 6 at level 6.
 * Duration: 15s at level 1, +7s per level.
 */
public class BeeAlarmSpell extends AbstractSpell
{
    private static final double DETECTION_RADIUS = 8.0D;
    private static final double PLACEMENT_RADIUS = 5.0D;
    private static final double BEE_STING_DAMAGE = 4.0D;
    private static final int ATTACK_COOLDOWN_TICKS = 40; // 2 seconds between attacks
    
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "bee_alarm");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.INSECT_RESOURCE)
            .setMaxLevel(6)
            .setCooldownSeconds(30.0)
            .build();

    public BeeAlarmSpell()
    {
        this.manaCostPerLevel = 15;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 1;
        this.castTime = 20; // 1 second cast
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
        return Optional.of(SoundEvents.EVOKER_PREPARE_SUMMON);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound()
    {
        return Optional.of(SoundEvents.BEEHIVE_EXIT);
    }

    public int getBeeCount(int spellLevel, LivingEntity caster)
    {
        return spellLevel; // 1 at level 1, up to 6 at level 6
    }

    public int getDuration(int spellLevel, LivingEntity caster)
    {
        return (15 + (spellLevel - 1) * 7) * 20; // 15s + 7s per level
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
            serverPlayer.removeEffect(EffectRegistry.SUMMONED_ALARM_BEE.get());
            super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
        }
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData)
    {
        if (world.isClientSide)
        {
            super.onCast(world, spellLevel, entity, castSource, playerMagicData);
            return;
        }

        if (entity == null)
        {
            super.onCast(world, spellLevel, null, castSource, playerMagicData);
            return;
        }

        int beeCount = this.getBeeCount(spellLevel, entity);
        int duration = this.getDuration(spellLevel, entity);
        double baseDamage = BEE_STING_DAMAGE + (spellLevel * 0.5);

        if (world instanceof ServerLevel serverLevel)
        {
            PlayerRecasts recasts = playerMagicData.getPlayerRecasts();

            if (!recasts.hasRecastForSpell(this))
            {
                SummonedEntitiesCastData castData = new SummonedEntitiesCastData();

                for (int i = 0; i < beeCount; i++)
                {
                    double angle = (Math.PI * 2 / beeCount) * i + world.random.nextDouble() * 0.5;
                    double radius = PLACEMENT_RADIUS * (0.6 + world.random.nextDouble() * 0.6);
                    double x = entity.getX() + Math.cos(angle) * radius;
                    double z = entity.getZ() + Math.sin(angle) * radius;
                    double y = entity.getY() + 0.5 + world.random.nextDouble() * 1.0;

                    AlarmBee alarmBee = new AlarmBee(EntityRegistry.SUMMONED_BEE.get(), world, entity, duration, baseDamage, new Vec3(x, y, z));
                    alarmBee.moveTo(x, y, z);
                    alarmBee.setNoAi(true);
                    alarmBee.setNoGravity(true);
                    alarmBee.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0D);
                    alarmBee.setPersistenceRequired();

                    world.addFreshEntity(alarmBee);
                    SummonManager.setOwner(alarmBee, entity);
                    castData.add(alarmBee);

                    MagicManager.spawnParticles(world, ParticleTypes.FALLING_HONEY, x, y + 0.5, z, 6, 0.15, 0.15, 0.15, 0.1, false);
                }

                entity.addEffect(new MobEffectInstance(EffectRegistry.SUMMONED_ALARM_BEE.get(), duration, 0, false, false, true));

                RecastInstance recastInstance = new RecastInstance(
                        this.getSpellId(), spellLevel, this.getRecastCount(spellLevel, entity),
                        duration, castSource, castData);
                recasts.addRecast(recastInstance, playerMagicData);
            }
        }

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster)
    {
        return List.of(
                Component.translatable("ui.entomology_spell.bee_alarm_desc"),
                Component.translatable("ui.irons_spellbooks.summon_count", String.valueOf(this.getBeeCount(spellLevel, caster))),
                Component.translatable("ui.entomology_spell.detection_radius", String.format("%.0f", DETECTION_RADIUS)),
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(this.getDuration(spellLevel, caster), 1))
        );
    }

    @Override
    public Vector3f getTargetingColor()
    {
        return new Vector3f(1.0f, 0.8f, 0.0f); // Golden bee color
    }

    /**
     * Special alarm bee: stays at a fixed post and shoots bee stinger projectiles
     * at hostile entities that come close. Built on the mod's own summoned bee
     * type so it does not depend on EntityType.BEE existing.
     */
    public static class AlarmBee extends SummonedBeeEntity
    {
        @Override
        public boolean isSwarmMember()
        {
            return false;
        }
        private final LivingEntity owner;
        private final int duration;
        private final double damage;
        private final Vec3 post;
        private int ticksAlive = 0;
        private int attackCooldown = 0;
        private LivingEntity currentTarget;
        public AlarmBee(EntityType<? extends Bee> entityType, Level level, LivingEntity owner, 
                       int duration, double damage, Vec3 post)
        {
            super(entityType, level);
            this.owner = owner;
            this.duration = duration;
            this.damage = damage;
            this.post = post;
        }

        @Override
        public void tick()
        {
            super.tick();
            ticksAlive++;
            
            // Check for expiration
            if (ticksAlive >= duration || (owner != null && !owner.isAlive()))
            {
                this.discard();
                return;
            }

            // Decrease attack cooldown
            if (attackCooldown > 0)
                attackCooldown--;

            // Client-side visual effects: gentle hover bobbing at the post
            if (this.level().isClientSide)
            {
                this.setYRot(this.getYRot() + 3);
                this.setPos(this.post.x, this.post.y + Math.sin(ticksAlive * 0.15) * 0.1, this.post.z);
                
                // Honey particle trail
                if (ticksAlive % 10 == 0)
                {
                    this.level().addParticle(ParticleTypes.FALLING_HONEY,
                            getX() + (Math.random() - 0.5) * 0.3,
                            getY() + 0.5,
                            getZ() + (Math.random() - 0.5) * 0.3,
                            0.0, 0.02, 0.0);
                }
                return;
            }

            // Server-side AI
            // Lock to the fixed post (no movement)
            this.setPos(this.post.x, this.post.y, this.post.z);
            this.setDeltaMovement(Vec3.ZERO);

            if (ticksAlive % 5 == 0) // Check every 5 ticks
            {
                currentTarget = findNearestHostile();
            }

            if (currentTarget != null && currentTarget.isAlive()
                    && this.distanceToSqr(currentTarget) <= DETECTION_RADIUS * DETECTION_RADIUS)
            {
                // Face the target
                this.getLookControl().setLookAt(currentTarget, 30.0F, 30.0F);

                if (attackCooldown == 0)
                {
                    fireBeeStinger();
                    attackCooldown = ATTACK_COOLDOWN_TICKS;
                }
            }
            else
            {
                currentTarget = null;
            }
        }

        private LivingEntity findNearestHostile()
        {
            if (!(this.level() instanceof ServerLevel))
                return null;

            AABB aabb = new AABB(
                    post.x - DETECTION_RADIUS, post.y - DETECTION_RADIUS, post.z - DETECTION_RADIUS,
                    post.x + DETECTION_RADIUS, post.y + DETECTION_RADIUS, post.z + DETECTION_RADIUS);

            List<LivingEntity> candidates = this.level().getEntitiesOfClass(LivingEntity.class, aabb,
                    (entity) -> entity != this && entity != owner && !entity.isSpectator() && entity.isAlive()
                            && entity instanceof Monster);

            return this.level().getNearestEntity(candidates,
                    TargetingConditions.DEFAULT.copy().ignoreLineOfSight().ignoreInvisibilityTesting(),
                    this, this.getX(), this.getY(), this.getZ());
        }

        private void fireBeeStinger()
        {
            if (currentTarget == null)
                return;

            BeeStingerProjectile stinger = new BeeStingerProjectile(this.level(), this);
            stinger.setPos(this.position().add(0.0, this.getEyeHeight() * 0.5, 0.0));
            
            // Calculate direction to target
            Vec3 direction = currentTarget.position().subtract(this.position()).normalize();
            stinger.shoot(direction);
            stinger.setDamage((float) damage);
            
            this.level().addFreshEntity(stinger);
            
            // Attack sound
            this.playSound(SoundEvents.BEE_STING, 1.0f, 1.0f);
        }

        @Override
        public boolean isInvulnerable()
        {
            return true; // Cannot be damaged
        }

        @Override
        public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount)
        {
            // Fully invulnerable; also avoids vanilla Bee#hurt touching the
            // uninitialized beePollinateGoal (we override registerGoals)
            return false;
        }

        @Override
        protected void registerGoals()
        {
            // Override default bee AI with custom alarm behavior
        }

        /**
         * Transient entity tied to a fixed post and the alarm buff's lifetime.
         * Must NOT be saved across log-out / world change, otherwise the
         * SummonManager would resurrect a stale alarm bee at its old post in
         * the new world. Returning false here makes saveSummonerData's
         * entity.save() yield an empty tag, so it is removed on logout but
         * never re-spawned on login.
         */
        @Override
        public boolean shouldBeSaved()
        {
            return false;
        }

        public LivingEntity getOwner()
        {
            return owner;
        }
    }
}