package io.entomology.entomology.client;

import io.entomology.entomology.EntomologyMod;
import io.entomology.entomology.entity.BeeRequiemRenderer;
import io.entomology.entomology.entity.BeeStingerRenderer;
import io.entomology.entomology.entity.ButterflyPrincessRenderer;
import io.entomology.entomology.entity.ButterflyRaidRenderer;
import io.entomology.entomology.entity.ButterflyRenderer;
import io.entomology.entomology.entity.SpiderNestRenderer;
import io.entomology.entomology.entity.WebRootRenderer;
import io.entomology.entomology.registries.EntityRegistry;
import io.entomology.entomology.registries.ItemRegistry;
import io.redspace.ironsspellbooks.render.SpellBookCurioRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

/**
 * Client-side registrations: Curios renderer for the spell book, so it shows
 * on the player's body while equipped in the spellbook slot (same renderer
 * Iron's Spells uses for its own books), and the in-game config screen that
 * Forge shows as the "Config" button on the Mods list.
 */
@Mod.EventBusSubscriber(modid = EntomologyMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup
{
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event)
    {
        // The insect egg textures (32x32) have transparent pixels; without the
        // cutout render layer the placed block renders those pixels as black in
        // the world even though the item icon (always cutout in GUIs) looks fine.
        event.enqueueWork(() -> net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                io.entomology.entomology.registries.BlockRegistry.INSECT_EGG.get(),
                net.minecraft.client.renderer.RenderType.cutout()));

        CuriosRendererRegistry.register(ItemRegistry.SWARM_SPELL_BOOK.get(), SpellBookCurioRenderer::new);
        CuriosRendererRegistry.register(ItemRegistry.BUTTERFLY_WINGS_BLUE.get(),
                io.entomology.entomology.client.curio.ButterflyWingsCurioRenderer::new);
        CuriosRendererRegistry.register(ItemRegistry.BUTTERFLY_WINGS_WHITE.get(),
                io.entomology.entomology.client.curio.ButterflyWingsCurioRenderer::new);
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) ->
                        new io.entomology.entomology.client.EntomologyConfigScreen(parent)));
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(EntityRegistry.WEB_ROOT.get(), WebRootRenderer::new);
        // Ground ritual circle under the Shiraori Attendant spider
        event.registerEntityRenderer(EntityRegistry.LEY_LINE_AREA.get(),
                io.entomology.entomology.entity.LeyLineAreaRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BEE_STINGER.get(), BeeStingerRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BEE_REQUIEM.get(), BeeRequiemRenderer::new);
        event.registerEntityRenderer(EntityRegistry.SPIDER_NEST.get(), SpiderNestRenderer::new);
        // Particle-only visual for the one-shot Swarm Aid firefly
        event.registerEntityRenderer(EntityRegistry.SWARM_FIREFLY.get(),
                io.entomology.entomology.entity.SwarmFireflyRenderer::new);
        // Reuse Iron's Spellbooks' ice spider renderer/model/texture for the summoned one
        event.registerEntityRenderer(EntityRegistry.SUMMONED_ICE_SPIDER.get(),
                io.redspace.ironsspellbooks.entity.mobs.ice_spider.IceSpiderRenderer::new);
        // Summoned insects reuse the vanilla renderers (and thus vanilla textures)
        event.registerEntityRenderer(EntityRegistry.SUMMONED_BEE.get(),
                net.minecraft.client.renderer.entity.BeeRenderer::new);
        // Honey courier + swarm call bees are bee subclasses: vanilla bee renderer
        event.registerEntityRenderer(EntityRegistry.HONEY_COURIER_BEE.get(),
                net.minecraft.client.renderer.entity.BeeRenderer::new);
        event.registerEntityRenderer(EntityRegistry.SWARM_CALL_BEE.get(),
                net.minecraft.client.renderer.entity.BeeRenderer::new);
        event.registerEntityRenderer(EntityRegistry.SUMMONED_SILVERFISH.get(),
                net.minecraft.client.renderer.entity.SilverfishRenderer::new);
        event.registerEntityRenderer(EntityRegistry.SUMMONED_SPIDER.get(),
                net.minecraft.client.renderer.entity.SpiderRenderer::new);
        event.registerEntityRenderer(EntityRegistry.SUMMONED_CAVE_SPIDER.get(),
                io.entomology.entomology.entity.SummonedCaveSpiderRenderer::new);
        // Reuse Alex's Mobs' cockroach renderer (maracas layer included); skipped without alexsmobs
        if (EntityRegistry.SUMMONED_COCKROACH != null)
        {
            event.registerEntityRenderer(EntityRegistry.SUMMONED_COCKROACH.get(),
                    com.github.alexthe666.alexsmobs.client.render.RenderCockroach::new);
        }
        // Reuse Alex's Mobs' crimson mosquito renderer (blood belly layer included); skipped without alexsmobs
        if (EntityRegistry.SUMMONED_MOSQUITO != null)
        {
            event.registerEntityRenderer(EntityRegistry.SUMMONED_MOSQUITO.get(),
                    com.github.alexthe666.alexsmobs.client.render.RenderCrimsonMosquito::new);
        }
        // Summoned centipede (head only — Alex's head tick auto-spawns the body
        // and tail parts, which keep using Alex's own body/tail renderers).
        if (EntityRegistry.SUMMONED_CENTIPEDE != null)
        {
            event.registerEntityRenderer(EntityRegistry.SUMMONED_CENTIPEDE.get(),
                    com.github.alexthe666.alexsmobs.client.render.RenderCentipedeHead::new);
        }
        // Summoned Warped Mosco reuses Alex's warped mosco renderer (glow layer included).
        if (EntityRegistry.SUMMONED_WARPED_MOSCO != null)
        {
            event.registerEntityRenderer(EntityRegistry.SUMMONED_WARPED_MOSCO.get(),
                    com.github.alexthe666.alexsmobs.client.render.RenderWarpedMosco::new);
        }
        // Butterfly swarm entity (wild mob + Summon Butterfly spell visual)
        event.registerEntityRenderer(EntityRegistry.BUTTERFLY.get(), ButterflyRenderer::new);
        // Butterfly raid projectile (particle-only, no visible model)
        event.registerEntityRenderer(EntityRegistry.BUTTERFLY_RAID.get(), ButterflyRaidRenderer::new);
        // Butterfly Princess — both variants share the same renderer/model/texture
        event.registerEntityRenderer(EntityRegistry.SUMMONED_BUTTERFLY_PRINCESS.get(), ButterflyPrincessRenderer::new);
        event.registerEntityRenderer(EntityRegistry.NPC_BUTTERFLY_PRINCESS.get(), ButterflyPrincessRenderer::new);
        // Shiraori (Spider Mother) — GeckoLib-rendered wizard
        event.registerEntityRenderer(EntityRegistry.SHIRAORI.get(),
                io.entomology.entomology.entity.ShiraoriRenderer::new);
        // Guardian Spider — GeckoLib-rendered large spider
        event.registerEntityRenderer(EntityRegistry.GUARDIAN_SPIDER.get(),
                io.entomology.entomology.entity.GuardianSpiderRenderer::new);
        // Bug Beetle — GeckoLib-rendered neutral beetle
        event.registerEntityRenderer(EntityRegistry.BUG_BEETLE.get(),
                io.entomology.entomology.entity.BugBeetleRenderer::new);
        // Cicada — GeckoLib-rendered flying cicada
        event.registerEntityRenderer(EntityRegistry.CICADA.get(),
                io.entomology.entomology.entity.CicadaRenderer::new);
        // Summoned variants hatched from Shiraori's insect eggs reuse the wild renderers/models
        event.registerEntityRenderer(EntityRegistry.SUMMONED_CICADA.get(),
                io.entomology.entomology.entity.CicadaRenderer::new);
        event.registerEntityRenderer(EntityRegistry.SUMMONED_BUG_BEETLE.get(),
                io.entomology.entomology.entity.BugBeetleRenderer::new);
    }
}
