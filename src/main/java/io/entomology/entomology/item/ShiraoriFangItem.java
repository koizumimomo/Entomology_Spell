package io.entomology.entomology.item;

import io.entomology.entomology.entity.LeyLineAreaEntity;
import io.entomology.entomology.registries.EffectRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Shiraori's Fang (白织的尖牙). Crafted from cobwebs, spider venom glands,
 * spider eyes and an insect crystal. Right-clicking a spider <b>on a full-moon
 * night in the Overworld</b> begins the Attendant ritual:
 *
 * <ol>
 *   <li>the spider is frozen ({@code setNoAi}) and a spinning ley-line circle
 *       ({@link LeyLineAreaEntity}) appears under its feet;</li>
 *   <li>for 30 seconds non-insect hostiles within 14 blocks are drawn to attack
 *       the spider (handled by {@code ShiraoriAttendantHandler});</li>
 *   <li>if the spider survives, it is permanently branded with the
 *       {@code ShiraoriAttendant} NBT and drops a Summon Shiraori scroll on
 *       death.</li>
 * </ol>
 *
 * The fang is not consumed.
 */
public class ShiraoriFangItem extends Item
{
    /** 30 seconds */
    public static final int RITUAL_DURATION = 20 * 30;

    public ShiraoriFangItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand)
    {
        if (!(target instanceof Spider spider))
        {
            return InteractionResult.PASS;
        }
        Level level = player.level();
        if (level.isClientSide)
        {
            return InteractionResult.SUCCESS;
        }

        // Already a branded attendant, or the ritual already in progress: do nothing.
        if (spider.getPersistentData().getBoolean("ShiraoriAttendant")
                || spider.hasEffect(EffectRegistry.SHIRAORI_ATTENDANT.get()))
        {
            return InteractionResult.PASS;
        }

        // The ritual may only be performed under a full moon at night in the Overworld.
        // Level.getMoonPhase(): 0 = full moon, 4 = new moon (8-phase cycle).
        if (level.dimension() != Level.OVERWORLD
                || level.getMoonPhase() != 0
                || level.isDay())
        {
            player.displayClientMessage(
                    Component.translatable("tooltip.entomology_spell.shiraori_s_fang.not_full_moon")
                            .withStyle(ChatFormatting.RED), true);
            player.playSound(SoundEvents.DISPENSER_FAIL, 0.7F, 0.8F);
            return InteractionResult.PASS;
        }

        ServerLevel serverLevel = (ServerLevel) level;

        spider.addEffect(new MobEffectInstance(EffectRegistry.SHIRAORI_ATTENDANT.get(), RITUAL_DURATION));
        // Ritual spiders are special: never let them naturally despawn before completion.
        spider.setPersistenceRequired();
        // The spider stays motionless inside the ritual circle for the 30s.
        spider.setNoAi(true);
        spider.setTarget(null);

        // Spinning ley-line circle locked to the spider's feet.
        LeyLineAreaEntity circle = new LeyLineAreaEntity(serverLevel, spider);
        serverLevel.addFreshEntity(circle);

        level.playSound(null, spider.blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                net.minecraft.sounds.SoundSource.HOSTILE, 0.9F, 1.3F);
        serverLevel.sendParticles(ParticleTypes.WITCH,
                spider.getX(), spider.getY() + 0.6D, spider.getZ(),
                18, 0.4D, 0.5D, 0.4D, 0.02D);
        player.swing(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced)
    {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        tooltipComponents.add(Component.translatable("tooltip.entomology_spell.shiraori_s_fang").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.entomology_spell.shiraori_s_fang.full_moon").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
