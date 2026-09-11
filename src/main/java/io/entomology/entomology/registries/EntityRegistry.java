package io.entomology.entomology.registries;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.entity.BeeRequiemProjectile;
import io.entomology.entomology.entity.BeeStingerProjectile;
import io.entomology.entomology.entity.BugBeetleEntity;
import io.entomology.entomology.entity.ButterflyEntity;
import io.entomology.entomology.entity.ButterflyRaidProjectile;
import io.entomology.entomology.entity.CicadaEntity;
import io.entomology.entomology.entity.GuardianSpiderEntity;
import io.entomology.entomology.entity.HoneyCourierBeeEntity;
import io.entomology.entomology.entity.LeyLineAreaEntity;
import io.entomology.entomology.entity.NPCButterflyPrincessEntity;
import io.entomology.entomology.entity.ShiraoriEntity;
import io.entomology.entomology.entity.SpiderNestEntity;
import io.entomology.entomology.entity.SummonedButterflyPrincessEntity;
import io.entomology.entomology.entity.SummonedBeeEntity;
import io.entomology.entomology.entity.SummonedCaveSpiderEntity;
import io.entomology.entomology.entity.SummonedCockroach;
import io.entomology.entomology.entity.SummonedIceSpiderEntity;
import io.entomology.entomology.entity.SummonedMosquitoEntity;
import io.entomology.entomology.entity.SummonedSilverfishEntity;
import io.entomology.entomology.entity.SummonedSpiderEntity;
import io.entomology.entomology.entity.SummonedCicadaEntity;
import io.entomology.entomology.entity.SummonedBugBeetleEntity;
import io.entomology.entomology.entity.SummonedCentipedeEntity;
import io.entomology.entomology.entity.SummonedWarpedMoscoEntity;
import io.entomology.entomology.entity.SwarmCallBeeEntity;
import io.entomology.entomology.entity.SwarmFireflyProjectile;
import io.entomology.entomology.entity.WebRootEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers the mod's entities.
 */
public class EntityRegistry
{
    public static final RegistryObject<EntityType<WebRootEntity>> WEB_ROOT = register("web_root", WebRootEntity::new, 1.0F, 0.5F);
    // Non-collidable ground ritual circle visual (Shiraori Attendant ritual).
    // Tiny hitbox; collision/pickability are all disabled in the class itself.
    public static final RegistryObject<EntityType<LeyLineAreaEntity>> LEY_LINE_AREA = register("ley_lines_area", LeyLineAreaEntity::new, 0.1F, 0.1F);
    public static final RegistryObject<EntityType<BeeStingerProjectile>> BEE_STINGER = register("bee_stinger", BeeStingerProjectile::new, 0.25F, 0.25F);
    public static final RegistryObject<EntityType<BeeRequiemProjectile>> BEE_REQUIEM = register("bee_requiem", BeeRequiemProjectile::new, 0.25F, 0.25F);
    public static final RegistryObject<EntityType<SpiderNestEntity>> SPIDER_NEST = register("spider_nest", SpiderNestEntity::new, 1.0F, 0.8F);
    // One-shot firefly strike for Swarm Aid: attacks once, then vanishes (Echoing Strikes pattern)
    public static final RegistryObject<EntityType<SwarmFireflyProjectile>> SWARM_FIREFLY = register("swarm_firefly", SwarmFireflyProjectile::new, 0.5F, 0.5F);
    // Same hitbox as Iron's Spellbooks' ice spider so the reused renderer/model lines up
    public static final RegistryObject<EntityType<SummonedIceSpiderEntity>> SUMMONED_ICE_SPIDER = register("summoned_ice_spider", SummonedIceSpiderEntity::new, 1.75F, 1.9F);
    // Same hitboxes as their vanilla counterparts so the vanilla renderers line up.
    // These replace the vanilla bee/silverfish/spider spawns so summon spells keep
    // working in modpacks with natural mob spawning disabled.
    public static final RegistryObject<EntityType<SummonedBeeEntity>> SUMMONED_BEE = register("summoned_bee", SummonedBeeEntity::new, 0.7F, 0.6F);
    public static final RegistryObject<EntityType<SummonedSilverfishEntity>> SUMMONED_SILVERFISH = register("summoned_silverfish", SummonedSilverfishEntity::new, 0.4F, 0.3F);
    public static final RegistryObject<EntityType<SummonedSpiderEntity>> SUMMONED_SPIDER = register("summoned_spider", SummonedSpiderEntity::new, 1.4F, 0.9F);
    public static final RegistryObject<EntityType<SummonedCaveSpiderEntity>> SUMMONED_CAVE_SPIDER = register("summoned_cave_spider", SummonedCaveSpiderEntity::new, 0.7F, 0.5F);
    // Same hitbox as Alex's Mobs' cockroach (0.7 x 0.3) so its renderer lines up.
    // Only registered when alexsmobs is installed; null otherwise.
    public static final RegistryObject<EntityType<SummonedCockroach>> SUMMONED_COCKROACH = SpellRegistry.isAlexsMobsLoaded()
            ? register("summoned_cockroach", SummonedCockroach::new, 0.7F, 0.3F)
            : null;
    // Alex's Mobs crimson mosquito: airborne, so register its in-flight size (1.2 x 1.8).
    // Only registered when alexsmobs is installed; null otherwise.
    public static final RegistryObject<EntityType<SummonedMosquitoEntity>> SUMMONED_MOSQUITO = SpellRegistry.isAlexsMobsLoaded()
            ? register("summoned_mosquito", SummonedMosquitoEntity::new, 1.2F, 1.8F)
            : null;
    // Peaceful courier bee (Honey Courier) and retaliation bee (Swarm Call) reuse
    // the vanilla bee hitbox (0.7 x 0.6) and renderer.
    public static final RegistryObject<EntityType<HoneyCourierBeeEntity>> HONEY_COURIER_BEE = register("honey_courier_bee", HoneyCourierBeeEntity::new, 0.7F, 0.6F);
    public static final RegistryObject<EntityType<SwarmCallBeeEntity>> SWARM_CALL_BEE = register("swarm_call_bee", SwarmCallBeeEntity::new, 0.7F, 0.6F);
    // Butterfly swarm entity (group model, 15 butterflies) used by Summon Butterfly spell.
    // Uses CREATURE category so it can spawn naturally via biome modifiers.
    public static final RegistryObject<EntityType<ButterflyEntity>> BUTTERFLY = registerCreature("butterfly", ButterflyEntity::new, 1.0F, 1.0F);
    // Butterfly raid projectile fired by the Butterfly Princess
    public static final RegistryObject<EntityType<ButterflyRaidProjectile>> BUTTERFLY_RAID = register("butterfly_raid", ButterflyRaidProjectile::new, 0.5F, 0.5F);
    // Butterfly Princess — summoned ally variant (fights for the caster for 5 minutes)
    public static final RegistryObject<EntityType<SummonedButterflyPrincessEntity>> SUMMONED_BUTTERFLY_PRINCESS = register("summoned_butterfly_princess", SummonedButterflyPrincessEntity::new, 0.6F, 1.8F);
    // Butterfly Princess — NPC variant (tradeable, neutral, found in the world)
    public static final RegistryObject<EntityType<NPCButterflyPrincessEntity>> NPC_BUTTERFLY_PRINCESS = register("npc_butterfly_princess", NPCButterflyPrincessEntity::new, 0.6F, 1.8F);
    // Shiraori (Spider Mother) — boss-sized wizard; oversized hitbox so the swarm
    // of brood/incarnation summons cannot stack directly on top of her
    public static final RegistryObject<EntityType<ShiraoriEntity>> SHIRAORI = register("shiraori", ShiraoriEntity::new, 3.0F, 4.0F);
    // Guardian Spider — large spider summoned by Shiraori, has leap attack and berserk mode
    public static final RegistryObject<EntityType<GuardianSpiderEntity>> GUARDIAN_SPIDER = register("guardian_spider", GuardianSpiderEntity::new, 3.2F, 2.8F);
    // Bug Beetle — neutral mob that spawns underground, can be hatched from insect eggs
    public static final RegistryObject<EntityType<BugBeetleEntity>> BUG_BEETLE = registerCreature("bug_beetle", BugBeetleEntity::new, 0.7F, 0.5F);
    // Cicada — flying mob with area debuff (Slowness + Darkness), spawns in forests/plains
    public static final RegistryObject<EntityType<CicadaEntity>> CICADA = registerCreature("cicada", CicadaEntity::new, 0.5F, 0.4F);
    // Summoned variants hatched from insect eggs (laid by Shiraori)
    public static final RegistryObject<EntityType<SummonedCicadaEntity>> SUMMONED_CICADA = register("summoned_cicada", SummonedCicadaEntity::new, 0.5F, 0.4F);
    public static final RegistryObject<EntityType<SummonedBugBeetleEntity>> SUMMONED_BUG_BEETLE = register("summoned_bug_beetle", SummonedBugBeetleEntity::new, 0.7F, 0.5F);
    // Summoned centipede / Warped Mosco (hatched from insect eggs, only with Alex's Mobs).
    // Centipede head hitbox matches the wild Alex head (1.5 x 1.5). Mosco hitbox
    // matches the wild Warped Mosco (1.5 x 2.5). Both render via Alex's own
    // renderers (registered conditionally in ClientSetup).
    public static final RegistryObject<EntityType<SummonedCentipedeEntity>> SUMMONED_CENTIPEDE = SpellRegistry.isAlexsMobsLoaded()
            ? register("summoned_centipede", SummonedCentipedeEntity::new, 1.5F, 1.5F)
            : null;
    public static final RegistryObject<EntityType<SummonedWarpedMoscoEntity>> SUMMONED_WARPED_MOSCO = SpellRegistry.isAlexsMobsLoaded()
            ? register("summoned_warped_mosco", SummonedWarpedMoscoEntity::new, 1.5F, 2.5F)
            : null;

    public static void init()
    {
        // Forces this class to load early so entities are registered before registry events fire.
    }

    private static <T extends net.minecraft.world.entity.Entity> RegistryObject<EntityType<T>> register(
            String name, EntityType.EntityFactory<T> factory, float width, float height)
    {
        return register(name, factory, width, height, MobCategory.MISC);
    }

    private static <T extends net.minecraft.world.entity.Entity> RegistryObject<EntityType<T>> registerCreature(
            String name, EntityType.EntityFactory<T> factory, float width, float height)
    {
        return register(name, factory, width, height, MobCategory.CREATURE);
    }

    private static <T extends net.minecraft.world.entity.Entity> RegistryObject<EntityType<T>> register(
            String name, EntityType.EntityFactory<T> factory, float width, float height, MobCategory category)
    {
        return EntomologyMod.ENTITIES.register(name,
                () -> EntityType.Builder.of(factory, category)
                        .sized(width, height)
                        .clientTrackingRange(64)
                        .updateInterval(1)
                        .build(EntomologyMod.MODID + ":" + name));
    }
}
