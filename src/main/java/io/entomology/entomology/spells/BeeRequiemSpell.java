package io.entomology.entomology.spells;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.entity.BeeRequiemProjectile;
import io.entomology.entomology.registries.SchoolRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Bee Requiem: three kamikaze bees materialize above the caster's head (top
 * center and both upper diagonals, forming the upper half of a circle) and
 * dive through everything onto the selected target, dealing swarm school
 * damage. Synergizes with Chaotic Stinger.
 */
public class BeeRequiemSpell extends AbstractSpell
{
    private static final int BEE_COUNT = 3;
    private static final double CIRCLE_RADIUS = 2.0D;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "bee_requiem");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.INSECT_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(20.0)
            .build();

    public BeeRequiemSpell()
    {
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 15;
        this.spellPowerPerLevel = 3;
        this.castTime = 0;
        this.baseManaCost = 50;
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
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData)
    {
        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, 32, 0.35f);
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData)
    {
        LivingEntity target = null;
        if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData)
        {
            target = targetData.getTarget((ServerLevel) world);
        }
        if (target == null || target.isDeadOrDying())
            return;

        // Three spawn points on the upper half of a circle above the caster's head
        double[] angles = {30.0, 90.0, 150.0}; // degrees, CCW from +x
        double centerY = entity.getY() + entity.getEyeHeight();
        double centerX = entity.getX();
        double centerZ = entity.getZ();

        for (double degrees : angles)
        {
            double rad = Math.toRadians(degrees);
            Vec3 spawn = new Vec3(
                    centerX + CIRCLE_RADIUS * Math.cos(rad),
                    centerY + CIRCLE_RADIUS * Math.sin(rad),
                    centerZ);

            BeeRequiemProjectile bee = new BeeRequiemProjectile(world, entity);
            bee.setPos(spawn);
            bee.setTarget(target);
            bee.shoot(target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(spawn).normalize());
            bee.setDamage(this.getDamage(spellLevel, entity));
            world.addFreshEntity(bee);
        }

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    private float getDamage(int spellLevel, LivingEntity entity)
    {
        // Per-bee damage, three bees total
        return this.getSpellPower(spellLevel, entity) * 1.2F;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster)
    {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(this.getDamage(spellLevel, caster) * BEE_COUNT, 2))
        );
    }
}
