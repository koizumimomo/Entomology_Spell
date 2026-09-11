package io.entomology.entomology.event;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.entity.GuardianSpiderEntity;
import io.entomology.entomology.entity.ShiraoriEntity;
import io.entomology.entomology.entity.SpiderNestEntity;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

@Mod.EventBusSubscriber(modid = EntomologyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GuardianSpiderBerserkHandler
{
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event)
    {
        Level level = (Level) event.getLevel();
        if (level == null || level.isClientSide)
            return;

        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        String blockName = state.getBlock().getDescriptionId();

        // Check if the broken block is a SpiderNest or InsectEggBlock
        // We check by registry name since InsectEggBlock may not be loaded yet
        String registryName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        if (!registryName.contains("spider_nest") && !registryName.contains("insect_egg"))
            return;

        triggerBerserkNearby(level, pos, 16.0D, null);
    }

    @SubscribeEvent
    public static void onShiraoriHurt(LivingHurtEvent event)
    {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof ShiraoriEntity shiraori))
            return;

        Level level = entity.level();
        if (level.isClientSide)
            return;

        triggerBerserkNearby(level, entity.blockPosition(), 16.0D, shiraori);
        alertNests(level, shiraori, event.getSource().getEntity());
    }

    /**
     * Shiraori's guard nests also protect her: when she is hurt, every nest
     * within 16 blocks that lists her as its owner drafts its spiders into
     * attacking the attacker.
     */
    private static void alertNests(Level level, ShiraoriEntity shiraori, net.minecraft.world.entity.Entity attacker)
    {
        if (!(level instanceof ServerLevel serverLevel))
            return;

        LivingEntity livingAttacker = attacker instanceof LivingEntity living ? living : null;
        for (SpiderNestEntity nest : serverLevel.getEntitiesOfClass(SpiderNestEntity.class,
                new net.minecraft.world.phys.AABB(shiraori.blockPosition()).inflate(16.0D)))
        {
            if (nest.getOwner() == shiraori)
            {
                nest.defendAgainst(livingAttacker);
            }
        }
    }

    private static void triggerBerserkNearby(Level level, BlockPos center, double radius, @javax.annotation.Nullable ShiraoriEntity shiraori)
    {
        if (!(level instanceof ServerLevel serverLevel))
            return;

        double r2 = radius * radius;
        for (GuardianSpiderEntity spider : serverLevel.getEntitiesOfClass(GuardianSpiderEntity.class,
                new net.minecraft.world.phys.AABB(center).inflate(radius)))
        {
            if (spider.distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5) > r2)
                continue;

            // Only berserk spiders owned by the same player as the Shiraori (or unowned wild spiders)
            if (shiraori != null)
            {
                LivingEntity shiraoriOwner = (LivingEntity) SummonManager.getOwner(shiraori);
                LivingEntity spiderOwner = (LivingEntity) SummonManager.getOwner(spider);
                if (shiraoriOwner != spiderOwner)
                    continue;
            }

            spider.triggerBerserk();
        }
    }
}
