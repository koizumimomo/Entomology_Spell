package io.entomology.entomology.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Armor material for the swarm (insect) school set.
 * Repaired with spider eyes, which matches the school's focus item.
 */
public enum SwarmArmorMaterial implements ArmorMaterial
{
    INSTANCE("entomology_spell:swarm", 18, new int[]{2, 5, 6, 2}, 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0.5F, 0.0F, () -> Ingredient.of(Items.SPIDER_EYE));

    private final String name;
    private final int durabilityMultiplier;
    private final int[] protectionAmounts; // boots, leggings, chestplate, helmet
    private final int enchantmentValue;
    private final SoundEvent equipSound;
    private final float toughness;
    private final float knockbackResistance;
    private final java.util.function.Supplier<Ingredient> repairIngredient;

    SwarmArmorMaterial(String name, int durabilityMultiplier, int[] protectionAmounts, int enchantmentValue,
                       SoundEvent equipSound, float toughness, float knockbackResistance,
                       java.util.function.Supplier<Ingredient> repairIngredient)
    {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.protectionAmounts = protectionAmounts;
        this.enchantmentValue = enchantmentValue;
        this.equipSound = equipSound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repairIngredient = repairIngredient;
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type)
    {
        // Standard per-slot multipliers, same as vanilla materials
        return switch (type)
        {
            case BOOTS -> 13 * this.durabilityMultiplier;
            case LEGGINGS -> 15 * this.durabilityMultiplier;
            case CHESTPLATE -> 16 * this.durabilityMultiplier;
            case HELMET -> 11 * this.durabilityMultiplier;
        };
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type)
    {
        // protectionAmounts is indexed by EquipmentSlot: 0 boots, 1 leggings, 2 chestplate, 3 helmet
        return switch (type)
        {
            case BOOTS -> this.protectionAmounts[0];
            case LEGGINGS -> this.protectionAmounts[1];
            case CHESTPLATE -> this.protectionAmounts[2];
            case HELMET -> this.protectionAmounts[3];
        };
    }

    @Override
    public int getEnchantmentValue()
    {
        return this.enchantmentValue;
    }

    @Override
    public SoundEvent getEquipSound()
    {
        return this.equipSound;
    }

    @Override
    public Ingredient getRepairIngredient()
    {
        return this.repairIngredient.get();
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public float getToughness()
    {
        return this.toughness;
    }

    @Override
    public float getKnockbackResistance()
    {
        return this.knockbackResistance;
    }
}
