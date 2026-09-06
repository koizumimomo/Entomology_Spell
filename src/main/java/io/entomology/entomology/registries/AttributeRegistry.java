package io.entomology.entomology.registries;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.entity.SpiderNestEntity;
import io.entomology.entomology.entity.WebRootEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = EntomologyMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class AttributeRegistry
{
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, EntomologyMod.MODID);

    // Multipliers applied to insect school spells; 1.0 is the neutral value.
    public static final RegistryObject<Attribute> INSECT_SPELL_POWER = ATTRIBUTES.register("insect_spell_power",
            () -> new RangedAttribute("attribute.entomology_spell.insect_spell_power", 1.0, 1.0, 10.0).setSyncable(true));

    public static final RegistryObject<Attribute> INSECT_MAGIC_RESIST = ATTRIBUTES.register("insect_magic_resist",
            () -> new RangedAttribute("attribute.entomology_spell.insect_magic_resist", 1.0, 0.0, 10.0).setSyncable(true));

    public static void register(IEventBus eventBus)
    {
        ATTRIBUTES.register(eventBus);
    }

    // Attach our school attributes to players so gear can modify them and
    // SchoolType.getPowerFor / getResistanceFor can pick them up.
    @SubscribeEvent
    public static void modifyEntityAttributes(EntityAttributeModificationEvent event)
    {
        event.add(EntityType.PLAYER, INSECT_SPELL_POWER.get());
        event.add(EntityType.PLAYER, INSECT_MAGIC_RESIST.get());
    }

    // Custom living entities must register their attribute supplier here,
    // otherwise Forge's DefaultAttributes returns null and constructing the
    // entity crashes (NPE in LivingEntity's constructor).
    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event)
    {
        event.put(EntityRegistry.WEB_ROOT.get(), WebRootEntity.createLivingAttributes().build());
        event.put(EntityRegistry.SPIDER_NEST.get(), SpiderNestEntity.createLivingAttributes().build());
        // Same attribute set as the vanilla ice spider; health/damage are rescaled per-cast by the spell
        event.put(EntityRegistry.SUMMONED_ICE_SPIDER.get(),
                io.redspace.ironsspellbooks.entity.mobs.ice_spider.IceSpiderEntity.prepareAttributes().build());
        // Summoned insects reuse their vanilla counterparts' attribute sets
        event.put(EntityRegistry.SUMMONED_BEE.get(),
                net.minecraft.world.entity.animal.Bee.createAttributes().build());
        event.put(EntityRegistry.SUMMONED_SILVERFISH.get(),
                net.minecraft.world.entity.monster.Silverfish.createAttributes().build());
        event.put(EntityRegistry.SUMMONED_SPIDER.get(),
                net.minecraft.world.entity.monster.Spider.createAttributes().build());
        event.put(EntityRegistry.SUMMONED_CAVE_SPIDER.get(),
                net.minecraft.world.entity.monster.Spider.createAttributes().build());
        if (EntityRegistry.SUMMONED_COCKROACH != null)
        {
            event.put(EntityRegistry.SUMMONED_COCKROACH.get(),
                    com.github.alexthe666.alexsmobs.entity.EntityCockroach.bakeAttributes().build());
        }
        if (EntityRegistry.SUMMONED_MOSQUITO != null)
        {
            event.put(EntityRegistry.SUMMONED_MOSQUITO.get(),
                    com.github.alexthe666.alexsmobs.entity.EntityCrimsonMosquito.bakeAttributes().build());
        }
    }
}
