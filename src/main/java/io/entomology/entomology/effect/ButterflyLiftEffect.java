package io.entomology.entomology.effect;

import io.entomology.entomology.registries.EffectRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Applied to the target of Summon Butterfly: lifts the entity straight up
 * for a height determined by the spell level (5 blocks per amplifier step),
 * then releases them to fall and take fall damage. The butterflies
 * (ButterflyEntity) are purely visual.
 */
public class ButterflyLiftEffect extends MobEffect
{
    /** Blocks of lift per amplifier step (level 1 → amplifier 0 → 5 blocks). */
    public static final int BLOCKS_PER_STEP = 5;
    private static final double LIFT_VELOCITY = 0.4D;

    public ButterflyLiftEffect()
    {
        super(MobEffectCategory.HARMFUL, 0xff66cc);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier)
    {
        return true; // tick every server tick
    }

    @Override
    public void applyEffectTick(LivingEntity holder, int amplifier)
    {
        if (holder.level().isClientSide)
            return;

        int maxBlocks = BLOCKS_PER_STEP * (amplifier + 1);
        double startY = holder.getPersistentData().getFloat("ButterflyLiftStartY");
        double currentY = holder.getY();
        double heightGained = currentY - startY;

        // Stop if we've reached max height or hit a solid block above
        if (heightGained >= maxBlocks)
        {
            holder.removeEffect(EffectRegistry.BUTTERFLY_LIFT.get());
            return;
        }

        BlockPos above = holder.blockPosition().above();
        BlockState stateAbove = holder.level().getBlockState(above);
        if (stateAbove.isSolidRender(holder.level(), above))
        {
            holder.removeEffect(EffectRegistry.BUTTERFLY_LIFT.get());
            return;
        }

        // Apply upward velocity
        Vec3 motion = holder.getDeltaMovement();
        holder.setDeltaMovement(motion.x, LIFT_VELOCITY, motion.z);
        holder.hurtMarked = true;
    }

    /**
     * Records the entity's Y position when the effect is first applied so we
     * can measure total height gained. Called from the spell's onCast.
     */
    public static void markStartY(LivingEntity target)
    {
        target.getPersistentData().putFloat("ButterflyLiftStartY", (float) target.getY());
    }
}
