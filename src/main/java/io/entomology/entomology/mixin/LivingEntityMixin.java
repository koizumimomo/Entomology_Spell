package io.entomology.entomology.mixin;

import io.entomology.entomology.registries.EffectRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes entities affected by the Insect Pheromone effect count as arthropods,
 * so enchantments like Bane of Arthropods deal their bonus damage to them.
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin
{
    @Inject(method = "getMobType", at = @At("HEAD"), cancellable = true)
    private void entomology$insectPheromoneArthropod(CallbackInfoReturnable<MobType> cir)
    {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.hasEffect(EffectRegistry.INSECT_PHEROMONE.get()))
        {
            cir.setReturnValue(MobType.ARTHROPOD);
        }
    }
}
