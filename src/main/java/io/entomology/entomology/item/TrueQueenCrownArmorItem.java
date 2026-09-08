package io.entomology.entomology.item;

import io.entomology.entomology.client.armor.SwarmArmorRenderer;
import io.entomology.entomology.client.armor.TrueQueenCrownModel;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

import java.util.List;
import java.util.UUID;

/**
 * True Queen Crown — crafted from a Queen Bee Crown + Butterfly Spirit.
 * While worn, radiates the Queen's Majesty aura (formerly the Queen Majesty
 * spell): every 32-block summon of the wearer gains Resistance I, Regeneration I,
 * Strength II, Speed I and Haste I, and all of the wearer's summons deal 20%
 * more damage.
 */
public class TrueQueenCrownArmorItem extends QueenBeeCrownArmorItem
{
    private static final double AURA_RADIUS = 32.0D;
    private static final int BUFF_DURATION_TICKS = 60; // 3 seconds for summons, refreshed every second
    private static final float DAMAGE_BONUS = 0.20F;

    public TrueQueenCrownArmorItem(String name, ArmorMaterial material, ArmorItem.Type type, Item.Properties properties, AttributeContainer... attributes)
    {
        super(name, material, type, properties, attributes);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected)
    {
        if (level.isClientSide || !(entity instanceof LivingEntity wearer) || slotId != ArmorItem.Type.HELMET.getSlot().getIndex())
            return;

        // Every 20 ticks (1 second), refresh buffs on nearby summons.
        // Use SummonManager.getSummons() to look up only this caster's summons
        // instead of scanning every entity inside a 32-block AABB.
        if (entity.tickCount % 20 != 0)
            return;

        // The wearer's permanent "Queen's Majesty" buff (while worn) is managed
        // centrally by ArmorEffectHandler, like the other crown armor buffs.

        double radiusSq = AURA_RADIUS * AURA_RADIUS;
        ServerLevel serverLevel = (ServerLevel) level;
        for (UUID summonId : SummonManager.getSummons(wearer))
        {
            Entity summonEntity = serverLevel.getEntity(summonId);
            if (!(summonEntity instanceof LivingEntity summon) || !summon.isAlive())
                continue;
            if (summon.distanceToSqr(wearer) > radiusSq)
                continue;
            summon.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, BUFF_DURATION_TICKS, 0, false, false, false));
            summon.addEffect(new MobEffectInstance(MobEffects.REGENERATION, BUFF_DURATION_TICKS, 0, false, false, false));
            summon.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, BUFF_DURATION_TICKS, 1, false, false, false));
            summon.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, BUFF_DURATION_TICKS, 0, false, false, false));
            summon.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, BUFF_DURATION_TICKS, 0, false, false, false));
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public GeoArmorRenderer<?> supplyRenderer()
    {
        return new SwarmArmorRenderer(new TrueQueenCrownModel());
    }

    // ---- GeckoLib ----

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.true_queen_crown.idle");

    /**
     * The crown's little butterflies flap on their own — this one looping
     * controller drives both the worn armor model and the 3D inventory icon
     * (the base SwarmArmorItem registers no controllers, so the model is static
     * for the other crown pieces).
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers)
    {
        controllers.add(new AnimationController<>(this, "true_queen_crown_controller", 0,
                state -> state.setAndContinue(IDLE)));
    }

    /**
     * Inherits SwarmArmorItem's initializeClient, which only provides
     * getHumanoidArmorModel (GeckoLib armor renderer when worn).
     * The inventory icon uses the standard 2D item model JSON.
     */
}
