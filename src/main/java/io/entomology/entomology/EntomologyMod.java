package io.entomology.entomology;

import com.mojang.logging.LogUtils;
import io.entomology.entomology.registries.AttributeRegistry;
import io.entomology.entomology.registries.CreativeTabRegistry;
import io.entomology.entomology.registries.EffectRegistry;
import io.entomology.entomology.registries.ItemRegistry;
import io.entomology.entomology.registries.SchoolRegistry;
import io.entomology.entomology.registries.SoundRegistry;
import io.entomology.entomology.registries.SpellRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(EntomologyMod.MODID)
public class EntomologyMod
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "entomology_spell";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    // Create a Deferred Register to hold Items which will all be registered under the "entomology" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold EntityTypes (summonable insects, etc.)
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);

    public EntomologyMod(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so entities get registered
        ENTITIES.register(modEventBus);

        // Register our schools of magic into Iron's Spells 'n Spellbooks
        SchoolRegistry.register(modEventBus);
        // Register our custom sounds
        SoundRegistry.register(modEventBus);
        // Register our custom attributes
        AttributeRegistry.register(modEventBus);
        // Register our mob effects
        EffectRegistry.register(modEventBus);
        // Register our spells into Iron's Spells 'n Spellbooks
        SpellRegistry.register(modEventBus);
        // Register our creative mode tab
        CreativeTabRegistry.register(modEventBus);
        // Force item registration to happen now (before registry events fire)
        ItemRegistry.init();
        // Force entity registration to happen now (before registry events fire)
        io.entomology.entomology.registries.EntityRegistry.init();

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("Entomology loaded - Iron's Spells 'n Spellbooks addon ready.");
    }
}
