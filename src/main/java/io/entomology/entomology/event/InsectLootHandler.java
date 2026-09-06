package io.entomology.entomology.event;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.entity.SummonedBeeEntity;
import io.entomology.entomology.entity.SummonedSpiderEntity;
import io.entomology.entomology.registries.ItemRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Kills dealt by the swarm school (entomology:insect_magic damage, e.g. summoned
 * bee stings and swarm school spells) have a chance to drop school materials:
 * royal jelly from bees and spider venom glands from spiders. Both are used to
 * craft the swarm spell book.
 */
@Mod.EventBusSubscriber(modid = EntomologyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class InsectLootHandler
{
    public static final float ROYAL_JELLY_DROP_CHANCE = 0.25F;
    public static final float SPIDER_VENOM_DROP_CHANCE = 0.20F;
    private static final ResourceLocation INSECT_MAGIC = ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "insect_magic");

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event)
    {
        DamageSource source = event.getSource();
        if (source == null || !source.typeHolder().is(INSECT_MAGIC))
        {
            return;
        }

        LivingEntity victim = event.getEntity();
        ItemStack drop = null;
        if (victim.getType() == EntityType.BEE || victim instanceof SummonedBeeEntity)
        {
            if (victim.getRandom().nextFloat() < ROYAL_JELLY_DROP_CHANCE)
            {
                drop = new ItemStack(ItemRegistry.ROYAL_JELLY.get());
            }
        }
        else if (victim.getType() == EntityType.SPIDER || victim.getType() == EntityType.CAVE_SPIDER
                || victim instanceof SummonedSpiderEntity)
        {
            if (victim.getRandom().nextFloat() < SPIDER_VENOM_DROP_CHANCE)
            {
                drop = new ItemStack(ItemRegistry.SPIDER_VENOM_GLAND.get());
            }
        }

        if (drop != null)
        {
            event.getDrops().add(new ItemEntity(victim.level(), victim.getX(), victim.getY(), victim.getZ(), drop));
        }
    }
}
