package io.entomology.entomology.mixin;

import io.entomology.entomology.EntomologyMod;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Bee;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Summoned bees (those with a SummonManager owner) never count as having spent
 * their stinger, so they keep attacking for as long as they are alive.
 * Their anger is kept refreshed by SummonedBeeOwnerHurtTargetGoal /
 * SummonedBeeOwnerHurtByTargetGoal (see entity.ai).
 */
@Mixin(Bee.class)
public class BeeMixin
{
    private static final ResourceKey<DamageType> INSECT_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "insect_magic"));

    @Inject(method = "hasStung", at = @At("HEAD"), cancellable = true)
    private void entomology$summonedBeeNeverStung(CallbackInfoReturnable<Boolean> cir)
    {
        Bee self = (Bee) (Object) this;
        if (SummonManager.getOwner(self) != null)
        {
            cir.setReturnValue(false);
        }
    }

    /**
     * Stings dealt by summoned bees use the swarm school damage type, so kills
     * count as swarm school kills (enables insect_magic loot, death messages...).
     * Redirect is used instead of ModifyArgs because Mixin 0.8.5's synthetic
     * Args classes break under Forge's class loading (NoClassDefFoundError).
     */
    @Redirect(method = "doHurtTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean entomology$summonedBeeInsectMagicDamage(Entity target, DamageSource source, float amount)
    {
        Bee self = (Bee) (Object) this;
        if (SummonManager.getOwner(self) != null)
        {
            Holder<DamageType> insectMagic = self.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(INSECT_MAGIC);
            return target.hurt(new DamageSource(insectMagic, self), amount);
        }
        return target.hurt(source, amount);
    }
}
