package io.entomology.entomology.entity;

import io.entomology.entomology.entity.goal.ButterflyPrincessCombatGoal;
import io.entomology.entomology.registries.ItemRegistry;
import io.entomology.entomology.registries.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.NeutralWizard;
import io.redspace.ironsspellbooks.entity.mobs.wizards.IMerchantWizard;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import javax.annotation.Nullable;

/**
 * NPC Butterfly Princess — the tradeable, neutral variant found in the world.
 * She is a {@link NeutralWizard} (so her hostility is gated by the vanilla
 * NeutralMob anger system) that also implements {@link IMerchantWizard} and
 * offers a fixed set of insect-themed trades:
 *
 * <ul>
 *   <li>6 insect_crystal + 4 royal_jelly → summon_butterfly_princess scroll</li>
 *   <li>2 insect_crystal → epic ink</li>
 *   <li>4 insect_crystal → legendary ink</li>
 *   <li>1 insect_crystal + 4 royal_jelly → summon_butterfly (level 2) scroll</li>
 *   <li>5 royal_jelly → butterfly_spirit</li>
 * </ul>
 *
 * On death she drops a handful of random flowers, a random ink, and has a
 * chance to drop royal_jelly / insect_crystal. She shares the summoned
 * variant's combat goal (the {@link ButterflyPrincessCombatGoal} insect spell
 * rotation) and is drawn with the same butterfly_princess GeckoLib resources.
 */
public class NPCButterflyPrincessEntity extends NeutralWizard implements IMerchantWizard
{
    private static final int TRADE_MAX_USES = 6;
    private static final int TRADE_XP = 8;
    private static final float TRADE_PRICE_MULTIPLIER = 0.2F;

    private MerchantOffers offers;
    private Player tradingPlayer;
    private int numberOfRestocksToday;
    private long lastRestockGameTime;
    private long lastRestockCheckDayTime;

    public NPCButterflyPrincessEntity(EntityType<? extends io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob> entityType, Level level)
    {
        super(entityType, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        // Same stats as the summoned variant: 36 health (18 hearts), 4 armor.
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 36.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.45D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals()
    {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Same combat rotation the summoned princess uses.
        this.goalSelector.addGoal(2, new ButterflyPrincessCombatGoal(this));
        // Lets her wander around like a normal villager (without this she never walks).
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.5D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 32.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        // Neutral retaliation: only fights back / chases hostiles once provoked.
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Monster.class, 20, true, false, this::isAngryTarget));
    }

    // ---- GeckoLib animation ----
    // The inherited AbstractSpellCastingMob registers Iron's own cast controllers,
    // whose animation names don't exist in our butterfly_princess animation file —
    // so the model would just stand in a bind-pose T-pose. We register one simple
    // controller that picks idle / walk / cast from our own animations.

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.butterfly_princess.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.butterfly_princess.walk");
    private static final RawAnimation FLYING = RawAnimation.begin().thenLoop("animation.butterfly_princess.flying");
    private static final RawAnimation CAST = RawAnimation.begin().thenPlay("animation.butterfly_princess.attack1");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers)
    {
        controllers.add(new AnimationController<>(this, "butterfly_princess_controller", 5, state ->
        {
            if (this.isCasting())
            {
                state.setAnimation(CAST);
            }
            else if (state.isMoving())
            {
                state.setAnimation(WALK);
            }
            else
            {
                state.setAnimation(FLYING);
            }
            return PlayState.CONTINUE;
        }));
    }

    /**
     * Neutral mobs only aggro nearby hostiles while they're actually angry.
     * This predicate keeps the princess passive until something provokes her
     * (matching the inherited NeutralWizard anger threshold).
     */
    private boolean isAngryTarget(@Nullable net.minecraft.world.entity.LivingEntity target)
    {
        if (target == null || !target.isAlive() || target == this)
        {
            return false;
        }
        // NeutralWizard#isHostileTowards gates who the princess considers fair game once angry.
        return this.isHostileTowards(target);
    }

    // ---- Trades ----

    /**
     * Builds the fixed trade list. Called the first time a player opens the
     * trade screen (so the scroll ItemStacks get freshly-bound spell data).
     */
    private void updateTrades()
    {
        MerchantOffers offers = new MerchantOffers();

        // 6 insect_crystal + 4 royal_jelly -> summon_butterfly_princess scroll (level 1)
        offers.add(new MerchantOffer(
                new ItemStack(ItemRegistry.INSECT_CRYSTAL.get(), 6),
                new ItemStack(ItemRegistry.ROYAL_JELLY.get(), 4),
                createSpellScroll(SpellRegistry.SUMMON_BUTTERFLY_PRINCESS_SPELL.get(), 1),
                TRADE_MAX_USES, TRADE_XP + 4, TRADE_PRICE_MULTIPLIER));

        // 2 insect_crystal -> epic ink (ISS ink item)
        offers.add(new MerchantOffer(
                new ItemStack(ItemRegistry.INSECT_CRYSTAL.get(), 2),
                ItemStack.EMPTY,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INK_EPIC.get()),
                TRADE_MAX_USES + 2, TRADE_XP, TRADE_PRICE_MULTIPLIER));

        // 4 insect_crystal -> legendary ink (ISS ink item)
        offers.add(new MerchantOffer(
                new ItemStack(ItemRegistry.INSECT_CRYSTAL.get(), 4),
                ItemStack.EMPTY,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INK_LEGENDARY.get()),
                TRADE_MAX_USES, TRADE_XP + 8, TRADE_PRICE_MULTIPLIER));

        // 1 insect_crystal + 4 royal_jelly -> summon_butterfly (level 2) scroll
        offers.add(new MerchantOffer(
                new ItemStack(ItemRegistry.INSECT_CRYSTAL.get(), 1),
                new ItemStack(ItemRegistry.ROYAL_JELLY.get(), 4),
                createSpellScroll(SpellRegistry.SUMMON_BUTTERFLY_SPELL.get(), 2),
                TRADE_MAX_USES, TRADE_XP + 2, TRADE_PRICE_MULTIPLIER));

        // 5 royal_jelly -> butterfly_spirit
        offers.add(new MerchantOffer(
                new ItemStack(ItemRegistry.ROYAL_JELLY.get(), 5),
                ItemStack.EMPTY,
                butterflySpiritStack(),
                TRADE_MAX_USES, TRADE_XP, TRADE_PRICE_MULTIPLIER));

        // 4 insect_crystal + 2 butterfly_spirit -> butterfly_wings_blue
        offers.add(new MerchantOffer(
                new ItemStack(ItemRegistry.INSECT_CRYSTAL.get(), 4),
                new ItemStack(ItemRegistry.BUTTERFLY_SPIRIT.get(), 2),
                new ItemStack(ItemRegistry.BUTTERFLY_WINGS_BLUE.get()),
                TRADE_MAX_USES, TRADE_XP + 6, TRADE_PRICE_MULTIPLIER));

        // 4 insect_crystal + 2 butterfly_spirit -> butterfly_wings_white
        offers.add(new MerchantOffer(
                new ItemStack(ItemRegistry.INSECT_CRYSTAL.get(), 4),
                new ItemStack(ItemRegistry.BUTTERFLY_SPIRIT.get(), 2),
                new ItemStack(ItemRegistry.BUTTERFLY_WINGS_WHITE.get()),
                TRADE_MAX_USES, TRADE_XP + 6, TRADE_PRICE_MULTIPLIER));

        this.offers = offers;
    }

    /**
     * Creates a spell scroll ItemStack bound to the given spell/level via ISS's
     * {@link ISpellContainer#createScrollContainer}. The scroll item is the
     * vanilla ISS scroll ({@code irons_spellbooks:scroll}).
     */
    private ItemStack createSpellScroll(AbstractSpell spell, int level)
    {
        ItemStack scroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(spell, level, scroll);
        return scroll;
    }

    /**
     * Resolves the butterfly_spirit item. The {@code ButterflySpiritItem}
     * class exists in the mod; it is expected to be registered as
     * {@code ItemRegistry.BUTTERFLY_SPIRIT}. If the field is not yet wired up,
     * fall back to a plain amethyst shard so the trade is still functional.
     */
    private ItemStack butterflySpiritStack()
    {
        RegistryObject<net.minecraft.world.item.Item> butterflySpirit = butterflySpiritRegistry();
        if (butterflySpirit != null)
        {
            return new ItemStack(butterflySpirit.get());
        }
        return new ItemStack(Items.AMETHYST_SHARD);
    }

    /**
     * Reflectively look up {@code ItemRegistry.BUTTERFLY_SPIRIT} so this class
     * still compiles even before that field is added to the registry. Returns
     * {@code null} when the field is absent (the trade then falls back to
     * amethyst). Once the field exists, the trade yields the proper item.
     */
    @SuppressWarnings("unchecked")
    private static RegistryObject<net.minecraft.world.item.Item> butterflySpiritRegistry()
    {
        try
        {
            java.lang.reflect.Field field = ItemRegistry.class.getField("BUTTERFLY_SPIRIT");
            Object value = field.get(null);
            if (value instanceof RegistryObject<?> ro && ro.get() instanceof net.minecraft.world.item.Item)
            {
                return (RegistryObject<net.minecraft.world.item.Item>) ro;
            }
        } catch (Exception ignored)
        {
            // Field not present yet — fall back to amethyst.
        }
        return null;
    }

    // ---- Merchant / IMerchantWizard contract ----

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand)
    {
        if (this.isAlive() && !this.isBaby() && !player.isSecondaryUseActive())
        {
            if (!this.level().isClientSide && this.getTradingPlayer() == null)
            {
                this.updateTrades();
                this.setTradingPlayer(player);
                this.openTradingScreen(player);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    /**
     * Vanilla merchant pattern (mirrors AbstractVillager#openTradingScreen /
     * the Iron's Spellbooks priest). In 1.20.1 the offers are NOT sent through
     * the open-menu screen buffer; instead the menu is opened with
     * {@link net.minecraft.server.level.ServerPlayer#openMenu} (which builds the
     * server {@link MerchantMenu} bound to this entity) and the offer list is
     * then shipped in a separate {@code ClientboundMerchantOffersPacket} via
     * {@link net.minecraft.server.level.ServerPlayer#sendMerchantOffers}.
     * Without that second packet the client keeps an empty ClientSideMerchant
     * and the trade screen shows no offers.
     */
    private void openTradingScreen(Player player)
    {
        net.minecraft.world.MenuProvider menuProvider = new net.minecraft.world.MenuProvider()
        {
            @Override
            public net.minecraft.network.chat.Component getDisplayName()
            {
                return NPCButterflyPrincessEntity.this.getDisplayName();
            }

            @Nullable
            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, Player p)
            {
                if (NPCButterflyPrincessEntity.this.getTradingPlayer() == null)
                {
                    return null;
                }
                return new MerchantMenu(id, inventory, NPCButterflyPrincessEntity.this);
            }
        };

        net.minecraft.server.level.ServerPlayer serverPlayer = (net.minecraft.server.level.ServerPlayer) player;
        java.util.OptionalInt result = serverPlayer.openMenu(menuProvider);
        if (result.isPresent())
        {
            serverPlayer.sendMerchantOffers(result.getAsInt(), this.getOffers(),
                    1, // merchant level (unused by us, just needs to be >= 1 for the client)
                    this.getVillagerXp(), this.showProgressBar(), this.canRestock());
        }
    }

    @Override
    public void setTradingPlayer(@Nullable Player player)
    {
        this.tradingPlayer = player;
    }

    @Override
    public Player getTradingPlayer()
    {
        return this.tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers()
    {
        // Mirror vanilla AbstractVillager: never build trades inside getOffers().
        // The server populates this.offers via updateTrades() in mobInteract().
        // Returning a live (possibly empty) list avoids creating scroll stacks
        // on the client, which can desync / throw during NBT serialization.
        if (this.offers == null)
        {
            this.offers = new MerchantOffers();
        }
        return this.offers;
    }

    @Override
    public void overrideOffers(MerchantOffers offers)
    {
        this.offers = offers;
    }

    @Override
    public void notifyTrade(MerchantOffer offer)
    {
        // Mark the offer as used. Merchant#notifyTrade is abstract (no super
        // implementation to delegate to), so the princess handles the bookkeeping
        // herself — the IMerchantWizard restock defaults consult the counters
        // below when deciding whether to refill the offer list.
        offer.increaseUses();
        this.setAndUpdateRestockFlag(this.level().getGameTime(), this.level().getDayTime());
    }

    /**
     * Mirrors AbstractVillager's restock trigger: bump the per-day restock
     * counter once a day rolls over so {@link IMerchantWizard#shouldRestock}
     * can fire the default {@link IMerchantWizard#restock} at the right cadence. Kept simple so the
     * princess refills her scroll/ink trades without needing a villager loot
     * table or POI logic.
     */
    private void setAndUpdateRestockFlag(long gameTime, long dayTime)
    {
        if (this.getLastRestockCheckDayTime() != dayTime)
        {
            this.setLastRestockCheckDayTime(dayTime);
            this.setRestocksToday(0);
        }
        this.setLastRestockGameTime(gameTime);
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack)
    {
        // Default behaviour: play the trade sound. No special handling needed for the princess.
    }

    @Override
    public SoundEvent getNotifyTradeSound()
    {
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    @Override
    public int getRestocksToday()
    {
        return this.numberOfRestocksToday;
    }

    @Override
    public void setRestocksToday(int count)
    {
        this.numberOfRestocksToday = count;
    }

    @Override
    public long getLastRestockGameTime()
    {
        return this.lastRestockGameTime;
    }

    @Override
    public void setLastRestockGameTime(long time)
    {
        this.lastRestockGameTime = time;
    }

    @Override
    public long getLastRestockCheckDayTime()
    {
        return this.lastRestockCheckDayTime;
    }

    @Override
    public void setLastRestockCheckDayTime(long time)
    {
        this.lastRestockCheckDayTime = time;
    }

    // ---- Save/load ----

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        if (this.offers != null)
        {
            tag.put("Offers", this.offers.createTag());
        }
        tag.putInt("RestocksToday", this.numberOfRestocksToday);
        tag.putLong("LastRestockGameTime", this.lastRestockGameTime);
        tag.putLong("LastRestockCheckDayTime", this.lastRestockCheckDayTime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Offers", 10))
        {
            this.offers = new MerchantOffers(tag.getCompound("Offers"));
        }
        this.numberOfRestocksToday = tag.getInt("RestocksToday");
        this.lastRestockGameTime = tag.getLong("LastRestockGameTime");
        this.lastRestockCheckDayTime = tag.getLong("LastRestockCheckDayTime");
    }

    // ---- Custom death drops ----

    /**
     * On death the NPC princess drops: 1-3 random flowers, one random ink,
     * and a chance (50%) of royal_jelly plus a chance (35%) of insect_crystal.
     * No loot table is needed — everything is spawned here so the drops are
     * deterministic about the items the player actually wants.
     */
    @Override
    public void die(net.minecraft.world.damagesource.DamageSource damageSource)
    {
        super.die(damageSource);
        RandomSource random = this.getRandom();
        // 1-3 random flowers
        int flowerCount = 1 + random.nextInt(3);
        for (int i = 0; i < flowerCount; i++)
        {
            this.spawnAtLocation(flowerStack(random));
        }
        // One random ink (ISS ink tiers)
        this.spawnAtLocation(inkStack(random));
        // Chance drops
        if (random.nextFloat() < 0.5F)
        {
            this.spawnAtLocation(new ItemStack(ItemRegistry.ROYAL_JELLY.get(), 1 + random.nextInt(2)));
        }
        if (random.nextFloat() < 0.35F)
        {
            this.spawnAtLocation(new ItemStack(ItemRegistry.INSECT_CRYSTAL.get(), 1 + random.nextInt(2)));
        }
    }

    private static ItemStack flowerStack(RandomSource random)
    {
        // Vanilla flower palette so the drop works without extra registries.
        ItemStack[] palette = new ItemStack[]{
                new ItemStack(Items.DANDELION),
                new ItemStack(Items.POPPY),
                new ItemStack(Items.BLUE_ORCHID),
                new ItemStack(Items.ALLIUM),
                new ItemStack(Items.AZURE_BLUET),
                new ItemStack(Items.CORNFLOWER),
                new ItemStack(Items.LILY_OF_THE_VALLEY),
                new ItemStack(Items.OXEYE_DAISY)
        };
        return palette[random.nextInt(palette.length)].copy();
    }

    private static ItemStack inkStack(RandomSource random)
    {
        // Pick a random ISS ink tier. Use the registry objects directly so the
        // item resolves at runtime after Forge has finished registering.
        switch (random.nextInt(5))
        {
            case 0:
                return new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INK_COMMON.get());
            case 1:
                return new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INK_UNCOMMON.get());
            case 2:
                return new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INK_RARE.get());
            case 3:
                return new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INK_EPIC.get());
            default:
                return new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INK_LEGENDARY.get());
        }
    }
}
