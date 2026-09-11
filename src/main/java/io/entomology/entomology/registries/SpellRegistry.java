package io.entomology.entomology.registries;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.spells.BeeAlarmSpell;
import io.entomology.entomology.spells.BeeRequiemSpell;
import io.entomology.entomology.spells.BeeStingerSpell;
import io.entomology.entomology.spells.ButterflyLoversSpell;
import io.entomology.entomology.spells.ChaoticStingerSpell;
import io.entomology.entomology.spells.InsectPheromoneSpell;
import io.entomology.entomology.spells.HoneyCourierSpell;
import io.entomology.entomology.spells.ParasiteSpell;
import io.entomology.entomology.spells.ParasiticBreathSpell;

import io.entomology.entomology.spells.SummonBeeSwarmSpell;
import io.entomology.entomology.spells.SummonButterflyPrincessSpell;
import io.entomology.entomology.spells.SummonButterflySpell;
import io.entomology.entomology.spells.SummonCockroachDanceSpell;
import io.entomology.entomology.spells.SummonIceSpiderSpell;
import io.entomology.entomology.spells.SummonMosquitoSwarmSpell;
import io.entomology.entomology.spells.SummonShiraoriSpell;
import io.entomology.entomology.spells.SwarmAegisSpell;
import io.entomology.entomology.spells.SwarmCallSpell;
import io.entomology.entomology.spells.SwarmExemptionSpell;
import io.entomology.entomology.spells.WebEntangleSpell;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers this mod's spells into Iron's Spells 'n Spellbooks' shared spell registry.
 */
public class SpellRegistry
{
    // Register into the shared "irons_spellbooks:spells" registry
    public static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister.create(
            io.redspace.ironsspellbooks.api.registry.SpellRegistry.SPELL_REGISTRY_KEY,
            EntomologyMod.MODID);

    public static final RegistryObject<AbstractSpell> INSECT_PHEROMONE_SPELL = SPELLS.register("insect_pheromone", InsectPheromoneSpell::new);
    public static final RegistryObject<AbstractSpell> SWARM_AEGIS_SPELL = SPELLS.register("swarm_aegis", SwarmAegisSpell::new);
    public static final RegistryObject<AbstractSpell> SUMMON_BEE_SWARM_SPELL = SPELLS.register("summon_bee_swarm", SummonBeeSwarmSpell::new);
    public static final RegistryObject<AbstractSpell> CHAOTIC_STINGER_SPELL = SPELLS.register("chaotic_stinger", ChaoticStingerSpell::new);
    public static final RegistryObject<AbstractSpell> PARASITE_SPELL = SPELLS.register("parasite", ParasiteSpell::new);
    public static final RegistryObject<AbstractSpell> WEB_ENTANGLE_SPELL = SPELLS.register("web_entangle", WebEntangleSpell::new);
    public static final RegistryObject<AbstractSpell> BEE_STINGER_SPELL = SPELLS.register("bee_stinger", BeeStingerSpell::new);
    public static final RegistryObject<AbstractSpell> BEE_REQUIEM_SPELL = SPELLS.register("bee_requiem", BeeRequiemSpell::new);
    public static final RegistryObject<AbstractSpell> BEE_ALARM_SPELL = SPELLS.register("bee_alarm", BeeAlarmSpell::new);
    public static final RegistryObject<AbstractSpell> SUMMON_ICE_SPIDER_SPELL = SPELLS.register("summon_ice_spider", SummonIceSpiderSpell::new);
    // Alex's Mobs optional integration: only offer the dance troupe if the mod is present
    public static final RegistryObject<AbstractSpell> SUMMON_COCKROACH_DANCE_SPELL = isAlexsMobsLoaded()
            ? SPELLS.register("summon_cockroach_dance", SummonCockroachDanceSpell::new)
            : null;
    // Alex's Mobs optional integration: crimson mosquito swarm (heal-on-hit support)
    public static final RegistryObject<AbstractSpell> SUMMON_MOSQUITO_SWARM_SPELL = isAlexsMobsLoaded()
            ? SPELLS.register("summon_mosquito_swarm", SummonMosquitoSwarmSpell::new)
            : null;
    // Marks a target so insects may attack it despite Insect Kinship protection
    public static final RegistryObject<AbstractSpell> SWARM_EXEMPTION_SPELL = SPELLS.register("swarm_exemption", SwarmExemptionSpell::new);
    // Peaceful bee courier that restores health/mana/food when it returns
    public static final RegistryObject<AbstractSpell> HONEY_COURIER_SPELL = SPELLS.register("honey_courier", HoneyCourierSpell::new);
    // Silverfish counterpart of Chaotic Stinger (Hunger + stacking Slowness)
    public static final RegistryObject<AbstractSpell> PARASITIC_BREATH_SPELL = SPELLS.register("parasitic_breath", ParasiticBreathSpell::new);
    // Chance to summon retaliation bees when hurt
    public static final RegistryObject<AbstractSpell> SWARM_CALL_SPELL = SPELLS.register("swarm_call", SwarmCallSpell::new);
    // Butterfly swarm spell: lifts and drops the target (2 levels, not craftable)
    public static final RegistryObject<AbstractSpell> SUMMON_BUTTERFLY_SPELL = SPELLS.register("summon_butterfly", SummonButterflySpell::new);
    // Summon Butterfly Princess: 5-minute ally that casts insect spells
    public static final RegistryObject<AbstractSpell> SUMMON_BUTTERFLY_PRINCESS_SPELL = SPELLS.register("summon_butterfly_princess", SummonButterflyPrincessSpell::new);
    // Butterfly Lovers: transforms insects into stronger forms (spider→ice spider, fly→mosquito→mosco)
    public static final RegistryObject<AbstractSpell> BUTTERFLY_LOVERS_SPELL = SPELLS.register("butterfly_lovers", ButterflyLoversSpell::new);
    // Summon Shiraori (Spider Mother): summons the spider mother wizard
    public static final RegistryObject<AbstractSpell> SUMMON_SHIRAORI_SPELL = SPELLS.register("summon_shiraori", SummonShiraoriSpell::new);

    public static boolean isAlexsMobsLoaded()
    {
        return net.minecraftforge.fml.ModList.get().isLoaded("alexsmobs");
    }

    public static void register(IEventBus eventBus)
    {
        SPELLS.register(eventBus);
    }
}
