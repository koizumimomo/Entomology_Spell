package io.entomology.entomology.event;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.entity.LeyLineAreaEntity;
import io.entomology.entomology.registries.EffectRegistry;
import io.entomology.entomology.registries.SpellRegistry;
import io.entomology.entomology.util.SwarmCreatures;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Backs the Shiraori's Attendant ritual ({@code shiraori_s_fang} item +
 * {@code shiraori_attendant} effect) on vanilla {@link Spider} mobs
 * (including cave spiders):
 *
 * <ol>
 *   <li><b>Ritual phase (effect active):</b> the spider is frozen (setNoAi)
 *       inside a spinning {@link LeyLineAreaEntity} circle; every second,
 *       non-insect hostile mobs within 14 blocks are taunted into targeting
 *       the spider. Insect / swarm creatures are deliberately excluded.</li>
 *   <li><b>Branding phase (effect expires / removed):</b> the spider regains
 *       its AI, the circle is removed, and it is permanently marked with the
 *       {@code ShiraoriAttendant} NBT flag; its name gains the 白织的侍从
 *       prefix.</li>
 *   <li><b>Loot phase:</b> a spider carrying the branding flag always drops a
 *       level 1 Summon Shiraori scroll on death.</li>
 * </ol>
 *
 * Lifecycle note: the circle is reconciled from the tick handler (respawned
 * if missing, e.g. after a chunk reload) and removed on every possible exit
 * path (effect end / spider death); the entity also self-discards when its
 * owner vanishes, so no single hook failure can leave an orphaned circle.
 */
@Mod.EventBusSubscriber(modid = EntomologyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ShiraoriAttendantHandler
{
    /** Set while the ritual effect is active; consumed once the effect ends to apply branding. */
    private static final String NBT_RITUAL_PENDING = "ShiraoriAttendantPending";
    /** Permanent branding flag. Spiders with this flag drop the Summon Shiraori scroll. */
    public static final String NBT_ATTENDANT = "ShiraoriAttendant";

    private static final int TAUNT_INTERVAL = 20;
    private static final int CIRCLE_RECONCILE_INTERVAL = 40;
    private static final double TAUNT_RADIUS = 14.0D;
    private static final double CIRCLE_SEARCH_RADIUS = 3.0D;

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event)
    {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !(entity instanceof Spider spider))
        {
            return;
        }
        ServerLevel level = (ServerLevel) spider.level();
        CompoundTag data = spider.getPersistentData();

        if (spider.hasEffect(EffectRegistry.SHIRAORI_ATTENDANT.get()))
        {
            data.putBoolean(NBT_RITUAL_PENDING, true);
            spider.setPersistenceRequired();
            // Defensive: a chunk reload or external effect removal could clear NoAI
            // mid-ritual; the spider must stay inside the circle.
            if (!spider.isNoAi())
            {
                spider.setNoAi(true);
            }
            if (spider.tickCount % TAUNT_INTERVAL == 0)
            {
                tauntNonInsectHostiles(level, spider);
            }
            // Reconcile the circle: covers chunk reload (circle is not saved) or
            // any rare desync where the visual entity went missing.
            if (spider.tickCount % CIRCLE_RECONCILE_INTERVAL == 0 && !hasCircle(level, spider))
            {
                level.addFreshEntity(new LeyLineAreaEntity(level, spider));
            }
        }
        else if (data.getBoolean(NBT_RITUAL_PENDING))
        {
            data.remove(NBT_RITUAL_PENDING);
            endRitual(level, spider);
            brandAttendant(level, spider, data);
        }
    }

    /**
     * Spider killed (or otherwise removed via the death path) during the
     * ritual: drop nothing and make sure its circle does not linger. The
     * circle self-discards on its own tick as well; this is the explicit hook.
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event)
    {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !(entity instanceof Spider spider))
        {
            return;
        }
        if (spider.getPersistentData().getBoolean(NBT_RITUAL_PENDING)
                || spider.hasEffect(EffectRegistry.SHIRAORI_ATTENDANT.get()))
        {
            if (spider.level() instanceof ServerLevel level)
            {
                removeCircle(level, spider);
            }
        }
    }

    /** Restores the spider's AI and despawns the ritual circle. */
    private static void endRitual(ServerLevel level, Spider spider)
    {
        spider.setNoAi(false);
        spider.setTarget(null);
        removeCircle(level, spider);
    }

    private static boolean hasCircle(ServerLevel level, Spider spider)
    {
        AABB area = spider.getBoundingBox().inflate(CIRCLE_SEARCH_RADIUS);
        return !level.getEntitiesOfClass(LeyLineAreaEntity.class, area).isEmpty();
    }

    private static void removeCircle(ServerLevel level, Spider spider)
    {
        AABB area = spider.getBoundingBox().inflate(CIRCLE_SEARCH_RADIUS);
        for (LeyLineAreaEntity circle : level.getEntitiesOfClass(LeyLineAreaEntity.class, area))
        {
            circle.discard();
        }
    }

    /**
     * Forces every non-insect hostile mob in range to attack the ritual
     * spider, overriding its current target (the ritual spider acts as a taunt
     * beacon). Same-insect mobs never participate.
     */
    private static void tauntNonInsectHostiles(ServerLevel level, Spider spider)
    {
        AABB area = spider.getBoundingBox().inflate(TAUNT_RADIUS);
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, area, mob ->
                mob != spider
                        && mob.isAlive()
                        && mob instanceof Enemy
                        && !SwarmCreatures.isSwarmCreature(mob));
        for (Mob mob : mobs)
        {
            if (mob.getTarget() != spider)
            {
                mob.setTarget(spider);
            }
        }
    }

    /** Permanently brands the spider: NBT flag + 白织的侍从 name prefix. */
    private static void brandAttendant(ServerLevel level, Spider spider, CompoundTag data)
    {
        data.putBoolean(NBT_ATTENDANT, true);
        spider.setPersistenceRequired();

        Component base = spider.getCustomName() != null
                ? spider.getCustomName()
                : Component.translatable(spider.getType().getDescriptionId());
        spider.setCustomName(Component.translatable("prefix.entomology_spell.shiraori_attendant", base));

        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                spider.getX(), spider.getY() + 0.6D, spider.getZ(),
                24, 0.4D, 0.5D, 0.4D, 0.05D);
        level.playSound(null, spider.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                net.minecraft.sounds.SoundSource.HOSTILE, 0.9F, 1.2F);
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event)
    {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Spider))
        {
            return;
        }
        if (!entity.getPersistentData().getBoolean(NBT_ATTENDANT))
        {
            return;
        }
        if (!(entity.level() instanceof ServerLevel level))
        {
            return;
        }

        ItemStack scroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(SpellRegistry.SUMMON_SHIRAORI_SPELL.get(), 1, scroll);
        event.getDrops().add(new ItemEntity(level, entity.getX(), entity.getY() + 0.5D, entity.getZ(), scroll));
    }
}
