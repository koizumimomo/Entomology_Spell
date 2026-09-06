package io.entomology.entomology.client.armor;

import io.entomology.entomology.item.SwarmArmorItem;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * Shared GeckoLib armor renderer for the swarm set. Each armor piece supplies
 * its own GeoModel through the constructor.
 */
public class SwarmArmorRenderer extends GeoArmorRenderer<SwarmArmorItem>
{
    public SwarmArmorRenderer(GeoModel<SwarmArmorItem> model)
    {
        super(model);
    }
}
