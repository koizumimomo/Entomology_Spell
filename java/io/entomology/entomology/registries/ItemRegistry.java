package io.entomology.entomology.registries;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.item.BeeIncarnationArmorItem;
import io.entomology.entomology.item.LootItem;
import io.entomology.entomology.item.QueenBeeCrownArmorItem;
import io.entomology.entomology.item.SwarmArmorItem;
import io.entomology.entomology.item.SwarmArmorMaterial;
import io.entomology.entomology.item.WeaverSpiderCheliceraeArmorItem;
import io.redspace.ironsspellbooks.item.SpellBook;
import io.redspace.ironsspellbooks.item.UpgradeOrbItem;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import io.redspace.ironsspellbooks.item.weapons.StaffTier;
import io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers the swarm school equipment: armor set, spell book and staff.
 */
public class ItemRegistry
{
    /**
     * Forces this class to load early, so items are registered before the
     * registry events fire. Must be called from the main mod constructor.
     */
    public static void init()
    {
    }

    // Staff tier: bonus insect spell power and a bit of global spell power while held
    public static final StaffTier SWARM_STAFF_TIER = new StaffTier(
            5.0F, -3.0F,
            new AttributeContainer(AttributeRegistry.INSECT_SPELL_POWER, 0.15, AttributeModifier.Operation.MULTIPLY_BASE),
            new AttributeContainer(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER, 0.05, AttributeModifier.Operation.MULTIPLY_BASE),
            new AttributeContainer(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MANA_REGEN, 0.25, AttributeModifier.Operation.MULTIPLY_BASE)
    );

    // Unique swarm set: crown (head), chelicerae (body), bee incarnation (legs)
    // Max mana bonus: crown +200, chestplate +500, leggings +350
    // Insect spell power: crown +10%, chestplate +15%, leggings +10%
    public static final RegistryObject<Item> QUEEN_BEE_CROWN = registerArmor("queen_bee_crown", ArmorItem.Type.HELMET, Rarity.EPIC, QueenBeeCrownArmorItem::new, 200.0, 0.10);
    public static final RegistryObject<Item> WEAVER_SPIDER_CHELICERAE = registerArmor("weaver_spider_chelicerae", ArmorItem.Type.CHESTPLATE, Rarity.EPIC, WeaverSpiderCheliceraeArmorItem::new, 500.0, 0.15);
    public static final RegistryObject<Item> BEE_INCARNATION = registerArmor("bee_incarnation", ArmorItem.Type.LEGGINGS, Rarity.EPIC, BeeIncarnationArmorItem::new, 350.0, 0.10);

    // Spell book "The Swarm and I": insect spell power, 20% cast time reduction, bonus mana
    public static final RegistryObject<Item> SWARM_SPELL_BOOK = EntomologyMod.ITEMS.register("swarm_spell_book",
            () -> new SpellBook(10, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant())
                    .withSpellbookAttributes(
                            new AttributeContainer(AttributeRegistry.INSECT_SPELL_POWER, 0.15, AttributeModifier.Operation.MULTIPLY_BASE),
                            new AttributeContainer(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CAST_TIME_REDUCTION, 0.2, AttributeModifier.Operation.MULTIPLY_BASE),
                            new AttributeContainer(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA, 200.0, AttributeModifier.Operation.ADDITION)));

    // Loot drops: royal jelly from bees, spider venom gland from spiders killed by swarm school spells
    public static final RegistryObject<Item> ROYAL_JELLY = EntomologyMod.ITEMS.register("royal_jelly",
            () -> new LootItem(new Item.Properties().rarity(Rarity.UNCOMMON), "tooltip.entomology_spell.royal_jelly"));
    public static final RegistryObject<Item> SPIDER_VENOM_GLAND = EntomologyMod.ITEMS.register("spider_venom_gland",
            () -> new LootItem(new Item.Properties().rarity(Rarity.UNCOMMON), "tooltip.entomology_spell.spider_venom_gland"));

    // Insect crystal: royal jelly + amethyst shard (crafting material for the chelicerae)
    public static final RegistryObject<Item> INSECT_CRYSTAL = EntomologyMod.ITEMS.register("insect_crystal",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    // Vine-wrapped stick: vine + stick (crafting material for the swarm staff)
    public static final RegistryObject<Item> VINE_WRAPPED_STICK = EntomologyMod.ITEMS.register("vine_wrapped_stick",
            () -> new Item(new Item.Properties()));

    // Swarm rune (inscription material, like the vanilla school runes)
    public static final RegistryObject<Item> SWARM_RUNE = EntomologyMod.ITEMS.register("swarm_rune",
            () -> new Item(new Item.Properties()));

    // Swarm upgrade orb (enchants armor with insect spell power)
    public static final RegistryObject<Item> SWARM_UPGRADE_ORB = EntomologyMod.ITEMS.register("swarm_upgrade_orb",
            () -> new UpgradeOrbItem(new Item.Properties().rarity(Rarity.UNCOMMON).fireResistant(),
                    ResourceKey.create(UpgradeOrbTypeRegistry.UPGRADE_ORB_REGISTRY_KEY,
                            ResourceLocation.fromNamespaceAndPath(EntomologyMod.MODID, "insect_power"))));

    // Creative tab icon (uses the queen bee effect art)
    public static final RegistryObject<Item> QUEEN_BEE_ICON = EntomologyMod.ITEMS.register("queen_bee_icon",
            () -> new Item(new Item.Properties()));

    // Staff
    public static final RegistryObject<Item> SWARM_STAFF = EntomologyMod.ITEMS.register("swarm_staff",
            () -> new StaffItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), SWARM_STAFF_TIER));

    private interface ArmorFactory
    {
        SwarmArmorItem create(String name, ArmorMaterial material, ArmorItem.Type type, Item.Properties properties, AttributeContainer... attributes);
    }

    /**
     * Each swarm armor piece grants: insect spell power, 5% cast time reduction
     * and 10% cooldown reduction (positive = faster, these are speed-style
     * attributes) plus the piece's own max mana bonus.
     */
    private static RegistryObject<Item> registerArmor(String name, ArmorItem.Type type, Rarity rarity, ArmorFactory factory, double maxManaBonus, double insectPower)
    {
        return EntomologyMod.ITEMS.register(name,
                () -> factory.create(name, SwarmArmorMaterial.INSTANCE, type, new Item.Properties().rarity(rarity),
                        new AttributeContainer(AttributeRegistry.INSECT_SPELL_POWER, insectPower, AttributeModifier.Operation.MULTIPLY_BASE),
                        new AttributeContainer(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CAST_TIME_REDUCTION, 0.05, AttributeModifier.Operation.MULTIPLY_BASE),
                        new AttributeContainer(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA, maxManaBonus, AttributeModifier.Operation.ADDITION),
                        new AttributeContainer(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.COOLDOWN_REDUCTION, 0.10, AttributeModifier.Operation.MULTIPLY_BASE)));
    }
}
