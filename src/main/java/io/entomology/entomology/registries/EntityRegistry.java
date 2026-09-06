package io.entomology.entomology.registries;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.entity.BeeRequiemProjectile;
import io.entomology.entomology.entity.BeeStingerProjectile;
import io.entomology.entomology.entity.SpiderNestEntity;
import io.entomology.entomology.entity.SummonedBeeEntity;
import io.entomology.entomology.entity.SummonedCaveSpiderEntity;
import io.entomology.entomology.entity.SummonedCockroach;
import io.entomology.entomology.entity.SummonedIceSpiderEntity;
import io.entomology.entomology.entity.SummonedMosquitoEntity;
import io.entomology.entomology.entity.SummonedSilverfishEntity;
import io.entomology.entomology.entity.SummonedSpiderEntity;
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

    public static void init()
    {
        // Forces this class to load early so entities are registered before registry events fire.
    }

    private static <T extends net.minecraft.world.entity.Entity> RegistryObject<EntityType<T>> register(
            String name, EntityType.EntityFactory<T> factory, float width, float height)
    {
        return EntomologyMod.ENTITIES.register(name,
                () -> EntityType.Builder.of(factory, MobCategory.MISC)
                        .sized(width, height)
                        .clientTrackingRange(64)
                        .updateInterval(1)
                        .build(EntomologyMod.MODID + ":" + name));
    }
}
