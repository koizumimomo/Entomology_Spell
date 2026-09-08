package io.entomology.entomology.entity;

import io.entomology.entomology.registries.EffectRegistry;
import io.entomology.entomology.registries.EntityRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Collections;
import java.util.List;

/**
 * A spider nest entity that periodically summons spiders to defend its area.
 * Has 10 health (5 hearts) and can be destroyed. Its lifetime is driven by the
 * "Nest Duration" effect applied on cast - when it expires, the nest collapses.
 * Spiders protect the caster and prioritize enemies that attack the nest.
 * Wild spider-like mobs near the nest also join the defense.
 */
public class SpiderNestEntity extends LivingEntity implements GeoEntity
{
    private static final int SUMMON_INTERVAL_TICKS = 200; // 10 seconds
    private static final double DETECTION_RADIUS = 16.0D;
    private static final int MAX_SPIDERS = 8;

    private static final EntityDataAccessor<Integer> SUMMON_TICKS_DATA = SynchedEntityData.defineId(SpiderNestEntity.class, EntityDataSerializers.INT);

    private LivingEntity owner;
    private int ticksAlive = 0;
    private int spellLevel = 1;
    private float spellPower = 0.0F;
    /** Recast castData from the spell, so spawned spiders can be registered for auto-dismiss. */
    private io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData castData;

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.spider_nest.idle");
    private final AnimationController<SpiderNestEntity> animController =
            new AnimationController<>(this, "spider_nest_controller", 0, this::animationPredicate);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SpiderNestEntity(EntityType<? extends SpiderNestEntity> entityType, Level level)
    {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public SpiderNestEntity(Level level, Vec3 position, LivingEntity owner, int spellLevel, float spellPower,
                            io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData castData)
    {
        this(EntityRegistry.SPIDER_NEST.get(), level);
        this.moveTo(position.x, position.y, position.z);
        this.owner = owner;
        this.spellLevel = spellLevel;
        this.spellPower = spellPower;
        this.castData = castData;
        // Nest health scales with spell level (+8 each) and spell power (1:1)
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10.0D + spellLevel * 8.0D + spellPower);
        this.setHealth(this.getMaxHealth());
    }

    public static AttributeSupplier.Builder createLivingAttributes()
    {
        // 10 health = 5 hearts
        return LivingEntity.createLivingAttributes().add(Attributes.MAX_HEALTH, 10.0D);
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData(); // LivingEntity defines its keys here (health etc.)
        this.entityData.define(SUMMON_TICKS_DATA, 0);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound)
    {
        this.ticksAlive = compound.getInt("TicksAlive");
        if (compound.hasUUID("OwnerUUID"))
        {
            if (this.level() instanceof ServerLevel serverLevel)
            {
                Entity ownerEntity = serverLevel.getEntity(compound.getUUID("OwnerUUID"));
                if (ownerEntity instanceof LivingEntity living)
                {
                    this.owner = living;
                }
            }
        }
        this.spellLevel = compound.getInt("SpellLevel");
        this.spellPower = compound.getFloat("SpellPower");
        // Re-apply the scaled max health (NBT load bypasses the casting constructor)
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10.0D + this.spellLevel * 8.0D + this.spellPower);
        this.setHealth(this.getMaxHealth());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound)
    {
        compound.putInt("TicksAlive", this.ticksAlive);
        if (this.owner != null)
        {
            compound.putUUID("OwnerUUID", this.owner.getUUID());
        }
        compound.putInt("SpellLevel", this.spellLevel);
        compound.putFloat("SpellPower", this.spellPower);
    }

    @Override
    public void tick()
    {
        super.tick();
        if (this.level().isClientSide)
        {
            // Client-side visual effects
            if (ticksAlive % 10 == 0)
            {
                for (int i = 0; i < 4; i++)
                {
                    double angle = Math.toRadians(360.0 / 4) * i + ticksAlive * 0.05;
                    double x = Math.cos(angle) * 0.8;
                    double z = Math.sin(angle) * 0.8;

                    this.level().addParticle(net.minecraft.core.particles.ParticleTypes.SQUID_INK,
                            this.getX() + x, this.getY() + 0.5, this.getZ() + z,
                            0.0, 0.05, 0.0);
                }
            }
            ticksAlive++;
            return;
        }

        ticksAlive++;

        // Collapse once the duration effect runs out (or the owner is gone)
        if (this.getEffect(EffectRegistry.NEST_DURATION.get()) == null
                || (this.owner != null && !this.owner.isAlive()))
        {
            this.discard();
            return;
        }

        // Server-side summon timer
        int summonTicks = this.entityData.get(SUMMON_TICKS_DATA) + 1;
        if (summonTicks >= SUMMON_INTERVAL_TICKS)
        {
            summonTicks = 0;
            summonSpiders();
        }
        this.entityData.set(SUMMON_TICKS_DATA, summonTicks);

        // Periodically re-direct summoned spiders at hostile mobs
        if (ticksAlive % 40 == 0)
        {
            redirectSpiderTargets();
        }
    }

    /**
     * Summons 1-2 spiders of a random type. Public so the spell can call it
     * immediately on cast for the first wave.
     */
    public void summonSpiders()
    {
        if (!(this.level() instanceof ServerLevel))
            return;

        int spiderCount = countOwnedSpiders();
        int amount = 1 + this.random.nextInt(2); // 1-2 spiders per wave
        amount = Math.min(amount, MAX_SPIDERS + this.spellLevel - spiderCount);
        if (amount <= 0)
            return;

        for (int i = 0; i < amount; i++)
        {
            spawnOneSpider();
        }
    }

    private int countOwnedSpiders()
    {
        int count = 0;
        for (Mob mob : this.level().getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(DETECTION_RADIUS)))
        {
            if (SummonManager.getOwner(mob) == this.owner && isSpiderMob(mob))
                count++;
        }
        return count;
    }

    private boolean isSpiderMob(Mob mob)
    {
        ResourceLocation key = EntityType.getKey(mob.getType());
        if (key == null)
            return false;
        String path = key.getPath();
        return path.contains("spider") || path.contains("wartling");
    }

    /**
     * Wild (non-summon) spider-like mobs near the nest join the defense too.
     */
    private boolean isWildSpiderAlly(Mob mob)
    {
        return SummonManager.getOwner(mob) == null && isSpiderMob(mob);
    }

    private void spawnOneSpider()
    {
        // Own registered entity types instead of vanilla/Goety spiders so the
        // nest keeps working in modpacks with natural mob spawning disabled
        Mob spider = this.random.nextBoolean()
                ? new SummonedSpiderEntity(this.level(), this.owner)
                : new SummonedCaveSpiderEntity(this.level(), this.owner);

        Vec3 spawnPos = this.position().add(
                (Math.random() - 0.5) * 2.0,
                0.5,
                (Math.random() - 0.5) * 2.0
        );

        spider.moveTo(spawnPos);
        // Spider max health scales with the nest's spell power (+2 per point) and spell level (+4 each)
        double baseHealth = spider.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
        spider.getAttribute(Attributes.MAX_HEALTH).setBaseValue(
                baseHealth + 2.0D * this.spellPower + 4.0D * (this.spellLevel - 1));
        spider.setHealth(spider.getMaxHealth());
        this.level().addFreshEntity(spider);
        // Track ownership for recast auto-dismiss
        if (this.owner != null)
        {
            SummonManager.setOwner(spider, this.owner);
            if (this.castData != null)
            {
                this.castData.add(spider);
            }
        }
        this.playSound(SoundEvents.SPIDER_AMBIENT, 1.0F, 0.8F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount)
    {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide)
        {
            Entity attacker = source.getEntity();
            alertSpiders(attacker instanceof LivingEntity living ? living : null);
        }
        return hurt;
    }

    @Override
    public boolean isPushable()
    {
        return false;
    }

    @Override
    public void knockback(double strength, double x, double z)
    {
        // The nest stays where it was placed
    }

    private void alertSpiders(@Nullable LivingEntity attacker)
    {
        if (!(this.level() instanceof ServerLevel))
            return;

        // Never retaliate against the owner or other summons
        if (attacker == null || attacker == this.owner || SummonManager.getOwner(attacker) != null)
            return;

        for (Mob mob : this.level().getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(DETECTION_RADIUS)))
        {
            // Owned summons plus wild spider allies near the nest
            boolean owned = SummonManager.getOwner(mob) == this.owner;
            if (!owned && !isWildSpiderAlly(mob))
                continue;

            if (attacker.isAlive())
            {
                mob.setTarget(attacker);
                mob.setLastHurtByMob(attacker);
            }
        }
    }

    /**
     * Summoned spiders act like hostile mobs: attack nearby monsters, never the
     * owner. Wild spider allies nearby are drafted into the defense as well.
     */
    private void redirectSpiderTargets()
    {
        if (!(this.level() instanceof ServerLevel))
            return;

        for (Mob mob : this.level().getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(DETECTION_RADIUS)))
        {
            boolean owned = SummonManager.getOwner(mob) == this.owner;
            if (!owned && !isWildSpiderAlly(mob))
                continue;

            LivingEntity target = mob.getTarget();
            if (target == null || !target.isAlive() || target == this.owner)
            {
                // Wild allies never fight other spiders (avoids spider-vs-spider brawls)
                mob.setTarget(findHostileNear(mob, !owned));
            }
        }
    }

    private LivingEntity findHostileNear(Mob mob, boolean excludeSpiders)
    {
        AABB aabb = mob.getBoundingBox().inflate(16.0D);
        List<LivingEntity> candidates = this.level().getEntitiesOfClass(LivingEntity.class, aabb,
                e -> e != this.owner && e != mob && e.isAlive() && !e.isSpectator() && e instanceof Monster
                        && SummonManager.getOwner(e) == null // never target other summons
                        && !(excludeSpiders && e instanceof Mob candidate && isSpiderMob(candidate)));

        return this.level().getNearestEntity(candidates,
                TargetingConditions.DEFAULT.copy().ignoreLineOfSight().ignoreInvisibilityTesting(),
                mob, mob.getX(), mob.getY(), mob.getZ());
    }

    public LivingEntity getOwner()
    {
        return this.owner;
    }

    public int getSpellLevel()
    {
        return this.spellLevel;
    }

    @Override
    public boolean isPickable()
    {
        return true;
    }

    @Override
    public HumanoidArm getMainArm()
    {
        return HumanoidArm.RIGHT;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots()
    {
        return Collections.singleton(ItemStack.EMPTY);
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot pSlot)
    {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot pSlot, ItemStack pStack)
    {
    }

    private PlayState animationPredicate(AnimationState<SpiderNestEntity> event)
    {
        event.getController().setAnimation(ANIM_IDLE);
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers)
    {
        controllers.add(this.animController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache()
    {
        return this.cache;
    }
}
