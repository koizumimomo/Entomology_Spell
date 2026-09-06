package io.entomology.entomology.item;

import io.entomology.entomology.client.armor.BeeIncarnationModel;
import io.entomology.entomology.client.armor.SwarmArmorRenderer;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class BeeIncarnationArmorItem extends SwarmArmorItem
{
    public BeeIncarnationArmorItem(String name, ArmorMaterial material, ArmorItem.Type type, Item.Properties properties, AttributeContainer... attributes)
    {
        super(name, material, type, properties, attributes);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public GeoArmorRenderer<?> supplyRenderer()
    {
        return new SwarmArmorRenderer(new BeeIncarnationModel());
    }
}
