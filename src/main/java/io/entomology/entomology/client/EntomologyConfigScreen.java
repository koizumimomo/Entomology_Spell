package io.entomology.entomology.client;

import io.entomology.entomology.Config;
import io.entomology.entomology.EntomologyMod;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Minimal in-game config screen, opened from the "Config" button next to the
 * mod on Forge's Mods list screen (registered as a ConfigScreenFactory
 * extension point in ClientSetup). Toggling writes through the ForgeConfigSpec,
 * so the change is saved to entomology_spell-common.toml immediately.
 */
public class EntomologyConfigScreen extends Screen
{
    private final Screen parent;

    public EntomologyConfigScreen(Screen parent)
    {
        super(Component.translatable("gui.entomology_spell.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init()
    {
        int centerX = this.width / 2;
        this.addRenderableWidget(Button.builder(this.toggleLabel(), button ->
                {
                    boolean newValue = !Config.PREVENT_SAME_SUMMONER_FRIENDLY_FIRE.get();
                    Config.PREVENT_SAME_SUMMONER_FRIENDLY_FIRE.set(newValue);
                    Config.preventSameSummonerFriendlyFire = newValue;
                    button.setMessage(this.toggleLabel());
                })
                .bounds(centerX - 155, 60, 310, 20)
                .build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(centerX - 100, this.height - 30, 200, 20)
                .build());
    }

    private Component toggleLabel()
    {
        return Component.translatable("gui.entomology_spell.config.prevent_same_summoner_friendly_fire",
                Config.PREVENT_SAME_SUMMONER_FRIENDLY_FIRE.get() ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
    }

    @Override
    public void onClose()
    {
        if (this.minecraft != null)
        {
            this.minecraft.setScreen(this.parent);
        }
    }

    @NotNull
    @Override
    public Component getNarrationMessage()
    {
        return Component.translatable("gui.entomology_spell.config.title");
    }
}
