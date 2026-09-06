package io.entomology.entomology.registries;

import io.entomology.entomology.EntomologyMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SoundRegistry
{
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, EntomologyMod.MODID);

    // Default cast sound for the insect school. Audio file will be added later.
    public static final RegistryObject<SoundEvent> INSECT_CAST = SOUNDS.register("insect_cast",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "insect_cast")));

    public static void register(IEventBus eventBus)
    {
        SOUNDS.register(eventBus);
    }
}
