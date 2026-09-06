package io.entomology.entomology.registries;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.effect.ChaoticStingerEffect;
import io.entomology.entomology.effect.ChildWrathEffect;
import io.entomology.entomology.effect.InfatuatedEffect;
import io.entomology.entomology.effect.InsectKinshipEffect;
import io.entomology.entomology.effect.InsectPheromoneEffect;
import io.entomology.entomology.effect.NestDurationEffect;
import io.entomology.entomology.effect.ParasiteEffect;
import io.entomology.entomology.effect.QueenBeeEffect;
import io.entomology.entomology.effect.ReproductiveDesireEffect;
import io.entomology.entomology.effect.SummonedAlarmBeeEffect;
import io.entomology.entomology.effect.SummonedBeeSwarmEffect;
import io.entomology.entomology.effect.SummonedCockroachDanceEffect;
import io.entomology.entomology.effect.SwarmAegisEffect;
import io.entomology.entomology.effect.SwarmAidEffect;
import io.entomology.entomology.effect.SwarmWillEffect;
import io.entomology.entomology.effect.SwarmWrathEffect;
import io.entomology.entomology.effect.SweetheartEffect;
import io.entomology.entomology.effect.ToxinExpansionEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EffectRegistry
{
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, EntomologyMod.MODID);

    public static final RegistryObject<MobEffect> INSECT_PHEROMONE = MOB_EFFECTS.register("insect_pheromone", InsectPheromoneEffect::new);
    public static final RegistryObject<MobEffect> SWARM_AEGIS = MOB_EFFECTS.register("swarm_aegis", SwarmAegisEffect::new);
    public static final RegistryObject<MobEffect> SWARM_AID = MOB_EFFECTS.register("swarm_aid", SwarmAidEffect::new);
    public static final RegistryObject<MobEffect> SUMMONED_BEE_SWARM = MOB_EFFECTS.register("summoned_bee_swarm", SummonedBeeSwarmEffect::new);
    // Status indicator for Bee Alarm posts; deliberately separate from the
    // swarm indicator so the alarm bees are not counted as swarm members.
    public static final RegistryObject<MobEffect> SUMMONED_ALARM_BEE = MOB_EFFECTS.register("summoned_alarm_bee", SummonedAlarmBeeEffect::new);
    public static final RegistryObject<MobEffect> QUEEN_BEE = MOB_EFFECTS.register("queen_bee", QueenBeeEffect::new);
    // Status indicator for the cockroach dance troupe summon (alexsmobs optional integration)
    public static final RegistryObject<MobEffect> SUMMONED_COCKROACH_DANCE = MOB_EFFECTS.register("summoned_cockroach_dance", SummonedCockroachDanceEffect::new);
    public static final RegistryObject<MobEffect> SWARM_WRATH = MOB_EFFECTS.register("swarm_wrath", SwarmWrathEffect::new);
    public static final RegistryObject<MobEffect> CHAOTIC_STINGER = MOB_EFFECTS.register("chaotic_stinger", ChaoticStingerEffect::new);
    public static final RegistryObject<MobEffect> PARASITE = MOB_EFFECTS.register("parasite", ParasiteEffect::new);
    public static final RegistryObject<MobEffect> CHILD_WRATH = MOB_EFFECTS.register("child_wrath", ChildWrathEffect::new);
    public static final RegistryObject<MobEffect> TOXIN_EXPANSION = MOB_EFFECTS.register("toxin_expansion", ToxinExpansionEffect::new);
    // Same pattern as Iron's Spells "Charged" (Charge effect): the buff carries a
    // spell-power attribute modifier, so SchoolType.getPowerFor picks it up directly.
    // Note: the modifier id must be a real UUID (Forge's addAttributeModifier
    // parses it with UUID.fromString), so derive it like Iron's AttributeHelper.
    public static final RegistryObject<MobEffect> SWARM_WILL = MOB_EFFECTS.register("swarm_will",
            () -> new SwarmWillEffect().addAttributeModifier(
                    AttributeRegistry.INSECT_SPELL_POWER.get(),
                    java.util.UUID.nameUUIDFromBytes("entomology_spell:mobeffect_swarm_will".getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString(),
                    0.05F,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
    public static final RegistryObject<MobEffect> SWEETHEART = MOB_EFFECTS.register("sweetheart", SweetheartEffect::new);
    public static final RegistryObject<MobEffect> INFATUATED = MOB_EFFECTS.register("infatuated", InfatuatedEffect::new);
    public static final RegistryObject<MobEffect> REPRODUCTIVE_DESIRE = MOB_EFFECTS.register("reproductive_desire", ReproductiveDesireEffect::new);
    public static final RegistryObject<MobEffect> INSECT_KINSHIP = MOB_EFFECTS.register("insect_kinship", InsectKinshipEffect::new);
    public static final RegistryObject<MobEffect> NEST_DURATION = MOB_EFFECTS.register("nest_duration", NestDurationEffect::new);

    public static void register(IEventBus eventBus)
    {
        MOB_EFFECTS.register(eventBus);
    }
}
