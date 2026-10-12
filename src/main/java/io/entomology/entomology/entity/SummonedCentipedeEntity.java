package io.entomology.entomology.entity;

import com.github.alexthe666.alexsmobs.entity.EntityCentipedeBody;
import com.github.alexthe666.alexsmobs.entity.EntityCentipedeHead;
import io.entomology.entomology.registries.EntityRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericFollowOwnerGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericHurtByTargetGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericOwnerHurtByTargetGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericOwnerHurtTargetGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericProtectOwnerTargetGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Summoned cave centipede (Alex's Mobs optional integration). Hatched from
 * insect eggs laid by Shiraori. The head is registered as our own entity type
 * so it counts as the egg owner's summon (IMagicSummon lifecycle); the body
 * and tail parts are still Alex's Mobs {@code EntityCentipedeBody} entities
 * spawned automatically by the head's tick(), which is fine because they are
 * pure visual chain segments with no AI of their own.
 *
 * <p>Segment count is forced to 3 (head + 1 body + 1 tail) in the convenience
 * constructor so the head builds the minimal chain on its first tick. The wild
 * centipede unconditionally targets Player / AbstractVillager / EntityCockroach,
 * so the target selector is wiped and replaced with the same owner-aware rules
 * the other summoned bugs use.
 */
public class SummonedCentipedeEntity extends EntityCentipedeHead implements IMagicSummon
{
    /** Alex's Mobs cave centipede hatches at the minimum natural segment count (head + body + tail). */
    public static final int SEGMENT_COUNT = 3;

    public SummonedCentipedeEntity(EntityType<? extends EntityCentipedeHead> type, Level level)
    {
        super(type, level);
        this.setPersistenceRequired();
    }

    public SummonedCentipedeEntity(Level level, @Nullable LivingEntity owner)
    {
        this((EntityType<? extends EntityCentipedeHead>) EntityRegistry.SUMMONED_CENTIPEDE.get(), level);
        // Force 3 segments before the first tick builds the body chain.
        this.setSegmentCount(SEGMENT_COUNT);
        if (owner != null)
        {
            SummonManager.setOwner(this, owner);
        }
        this.setHealth(this.getMaxHealth());
    }

    @Override
    protected void registerGoals()
    {
        super.registerGoals();
        // Strip the wild centipede's hostile target AI (HurtByTargetGoal +
        // NearestAttackableTargetGoal for Player / AbstractVillager /
        // EntityCockroach); owner-driven target goals take over.
        this.targetSelector.getAvailableGoals().removeIf(wrappedGoal ->
        {
            Goal goal = wrappedGoal.getGoal();
            return goal instanceof HurtByTargetGoal || goal instanceof NearestAttackableTargetGoal<?>;
        });

        // Owner-aware target goals: protect summoner, share target, retaliate.
        this.targetSelector.addGoal(1, new GenericOwnerHurtByTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(2, new GenericOwnerHurtTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(3, new GenericHurtByTargetGoal(this, entity -> io.entomology.entomology.util.SwarmCreatures.isSameOwnerChain(this, entity)).setAlertOthers());
        this.targetSelector.addGoal(4, new GenericProtectOwnerTargetGoal(this, this::getSummoner));

        // Auto-hunt nearby hostile mobs (excluding owner and owner's summons).
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                target -> !io.entomology.entomology.util.SwarmCreatures.isSameOwnerChain(this, target)
                        && target instanceof Enemy
        ));

        // Follow the owner so the centipede does not wander off on its own.
        this.goalSelector.addGoal(6, new GenericFollowOwnerGoal(this, this::getSummoner, 1.0D, 9.0F, 4.0F, false, 20.0F));
    }

    // ---- Summon lifecycle ----

    @Override
    public void onUnSummon()
    {
        if (!this.level().isClientSide)
        {
            for (int i = 0; i < 12; i++)
            {
                this.level().addParticle(ParticleTypes.PORTAL,
                        this.getX() + (this.random.nextDouble() - 0.5) * 0.5,
                        this.getY() + this.random.nextDouble() * 0.5,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.5,
                        0, 0, 0);
            }
            this.discard();
        }
    }

    @Override
    public void die(DamageSource damageSource)
    {
        this.onDeathHelper();
        super.die(damageSource);
    }

    @Override
    public void onRemovedFromWorld()
    {
        this.onRemovedHelper(this);
        super.onRemovedFromWorld();
    }

    @Override
    public boolean removeWhenFarAway(double distance)
    {
        return false;
    }

    @Override
    public boolean canBeLeashed(Player player)
    {
        return false;
    }

    /**
     * Transient summon: lifespan is governed by SummonManager. Returning
     * false prevents the chunk serializer from writing a stale copy that
     * would be rebuilt as a NoAI shell on chunk reload.
     */
    @Override
    public boolean shouldBeSaved()
    {
        return false;
    }

    // ---- Cross-world restore handling ----

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        SummonedRestorationHelper.writeRestoreFlag(this, tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        SummonedRestorationHelper.readRestoreFlag(this, tag);
    }

    @Override
    public void tick()
    {
        super.tick();
        SummonedRestorationHelper.handleRestoredDespawn(this);
        // Alex's head tick rebuilds its body chain every tick while getChild()
        // is null. If, well after spawning, the chain is still missing (the
        // head's own addFreshEntity calls silently failed — e.g. the segments
        // landed in a chunk that is not fully loaded), force-load the segment
        // area and rebuild the chain ourselves so the player does not end up
        // with a lone head. The head skips its own build while getChild() is
        // non-null, so this cannot duplicate segments.
        if (!this.level().isClientSide && !this.isRemoved()
                && this.tickCount > 20 && this.tickCount % 20 == 0 && this.getChild() == null)
        {
            rebuildMissingBodyChain();
        }
    }

    /**
     * Cached handle on Alex's Mobs' private head {@code parts} array. The head
     * only repositions segments listed in that array, so a manual rebuild must
     * refresh it too. Alex's Mobs' own classes are not obfuscated at runtime,
     * so the field name is stable.
     */
    @Nullable
    private static final java.lang.reflect.Field PARTS_FIELD = initPartsField();

    @Nullable
    private static java.lang.reflect.Field initPartsField()
    {
        try
        {
            java.lang.reflect.Field field = EntityCentipedeHead.class.getDeclaredField("parts");
            field.setAccessible(true);
            return field;
        }
        catch (ReflectiveOperationException e)
        {
            return null;
        }
    }

    /**
     * Server-side replica of {@link EntityCentipedeHead}'s segment-chain build
     * (createBody → setParent → setBodyIndex → child links → addFreshEntity),
     * with the segment chunks force-loaded first and the head's private
     * {@code parts} array refreshed so the head keeps repositioning the live
     * segments afterwards.
     */
    private void rebuildMissingBodyChain()
    {
        ServerLevel serverLevel = (ServerLevel) this.level();
        LivingEntity prev = this;
        net.minecraft.world.phys.Vec3 prevPos = this.position();
        float backOffset = 0.45F;
        int count = Math.max(1, this.getSegmentCount());
        java.util.List<EntityCentipedeBody> chain = new java.util.ArrayList<>(count);
        for (int i = 0; i < count; i++)
        {
            EntityCentipedeBody body = this.createBody(prev, i == count - 1);
            body.setParent(prev);
            body.setBodyIndex(i);
            if (prev == this)
            {
                this.setChildId(body.getUUID());
            }
            else if (prev instanceof EntityCentipedeBody prevBody)
            {
                prevBody.setChildId(body.getUUID());
            }
            net.minecraft.world.phys.Vec3 bodyPos = body.tickMultipartPosition(this.getId(), backOffset, prevPos,
                    this.getXRot(), this.getRingBuffer(4 * i + 4, 1.0F), false);
            body.setPos(bodyPos);
            // The head's own build silently loses segments when their chunk is
            // not loaded; make sure that cannot happen here.
            serverLevel.getChunk(net.minecraft.util.Mth.floor(bodyPos.x) >> 4, net.minecraft.util.Mth.floor(bodyPos.z) >> 4);
            serverLevel.addFreshEntity(body);
            chain.add(body);
            prev = body;
            backOffset = body.getBackOffset();
            prevPos = body.position();
        }
        if (PARTS_FIELD != null)
        {
            try
            {
                PARTS_FIELD.set(this, chain.toArray(new EntityCentipedeBody[0]));
            }
            catch (IllegalAccessException ignored)
            {
            }
        }
    }
}
