package io.entomology.entomology.registries;

import io.entomology.entomology.EntomologyMod;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers the schools of magic added by this mod into Iron's Spells 'n Spellbooks' school registry.
 */
public class SchoolRegistry
{
    // Register into the shared "irons_spellbooks:schools" registry
    public static final DeferredRegister<SchoolType> SCHOOLS = DeferredRegister.create(
            io.redspace.ironsspellbooks.api.registry.SchoolRegistry.SCHOOL_REGISTRY_KEY,
            EntomologyMod.MODID);

    public static final ResourceLocation INSECT_RESOURCE = ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "insect");

    // The focus tag determines which item is used to craft scrolls of this school.
    // For now it only contains the spider eye; we may replace it with a custom item later.
    public static final TagKey<Item> INSECT_FOCUS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "insect_focus"));

    public static final ResourceKey<DamageType> INSECT_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "insect_magic"));

    public static final RegistryObject<SchoolType> INSECT = SCHOOLS.register("insect", () -> new SchoolType(
            INSECT_RESOURCE,
            INSECT_FOCUS,
            Component.translatable("school.entomology_spell.insect").withStyle(Style.EMPTY.withColor(0x6b8e23)),
            AttributeRegistry.INSECT_SPELL_POWER::get,
            AttributeRegistry.INSECT_MAGIC_RESIST::get,
            SoundRegistry.INSECT_CAST::get,
            INSECT_MAGIC
    ));

    public static void register(IEventBus eventBus)
    {
        SCHOOLS.register(eventBus);
    }
}
