package io.entomology.entomology;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

// Mod config. Registered as COMMON config in EntomologyMod's constructor, so
// the file lives at config/entomology_spell-common.toml on both the client and
// dedicated servers. Values are cached into plain static fields when the config
// (re)loads; the in-game config screen writes through the spec so both the file
// and the cached fields stay in sync.
@Mod.EventBusSubscriber(modid = EntomologyMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue PREVENT_SAME_SUMMONER_FRIENDLY_FIRE = BUILDER
            .comment("If true, a player's summons can no longer hurt that player's other summons",
                     "(for example spider nest spiders vs your own bee swarm).",
                     "Summoned insects still attack wild insects and other players' insects, so",
                     "farming swarm school materials with summon damage keeps working.",
                     "Default: false (summons can damage each other)")
            .define("preventSameSummonerFriendlyFire", false);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean preventSameSummonerFriendlyFire;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        preventSameSummonerFriendlyFire = PREVENT_SAME_SUMMONER_FRIENDLY_FIRE.get();
    }
}
