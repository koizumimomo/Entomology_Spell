package io.entomology.entomology.entity;

import io.entomology.entomology.registries.EntityRegistry;
import io.entomology.entomology.registries.EffectRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * 纯视觉地面法阵实体（双层旋转黑魔纹），用于白织侍从仪式。
 *
 * <p>生命周期由 {@code shiraori_attendant} 效果驱动：
 * <ul>
 *   <li>仪式开始时由 {@code ShiraoriFangItem} 在蜘蛛脚下生成，绑定蜘蛛 UUID</li>
 *   <li>每 tick 将自身位置同步到蜘蛛脚下；无重力、无碰撞、不可选中、不可伤害</li>
 *   <li>蜘蛛死亡/消失/被卸载，或效果提前结束时，自行 discard（多路径兜底，
 *       不依赖单一事件钩子）</li>
 *   <li>{@code shouldBeSaved()} 返回 false，区块卸载不留 NBT 空壳</li>
 * </ul>
 */
public class LeyLineAreaEntity extends Entity implements GeoEntity
{
    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("animation.ley_lines_area.spin");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final AnimationController<LeyLineAreaEntity> controller =
            new AnimationController<>(this, "ley_lines", 0, state -> state.setAndContinue(SPIN));

    @Nullable
    private UUID ownerUUID;
    /** 兜底寿命：正常由效果结束清理，此上限防止任何异常路径下法阵永久残留。 */
    private int maxLifetime = 20 * 30 + 40;

    public LeyLineAreaEntity(EntityType<? extends LeyLineAreaEntity> type, Level level)
    {
        super(type, level);
        this.noPhysics = true;
    }

    /** 服务端便捷构造器：绑定仪式蜘蛛并放置在其脚下。 */
    public LeyLineAreaEntity(ServerLevel level, LivingEntity spider)
    {
        this(EntityRegistry.LEY_LINE_AREA.get(), level);
        this.ownerUUID = spider.getUUID();
        this.setPos(spider.getX(), spider.getY() + 0.02D, spider.getZ());
    }

    @Override
    protected void defineSynchedData()
    {
        // 无同步数据：客户端只需播放动画，owner 仅服务端用于位置跟踪/自清理。
    }

    @Override
    public void tick()
    {
        super.tick();

        if (this.level().isClientSide)
        {
            return;
        }

        // 兜底寿命
        if (this.tickCount >= this.maxLifetime)
        {
            this.discard();
            return;
        }

        LivingEntity owner = resolveOwner();
        if (owner == null || !owner.isAlive() || owner.isRemoved())
        {
            // 蜘蛛死亡/消失/跨世界：法阵立即消失（恢复 AI 的责任在事件处理器，
            // 但死蜘蛛无需恢复）
            this.discard();
            return;
        }

        if (!owner.hasEffect(EffectRegistry.SHIRAORI_ATTENDANT.get()))
        {
            // 效果提前结束（喝奶/指令清除等）：让 handler 的 branding 逻辑结算，
            // 法阵在这里自行消失
            this.discard();
            return;
        }

        // 紧贴蜘蛛脚下（蜘蛛仪式期间 setNoAi，位置基本不变，这里仍每 tick 同步，
        // 兼容水流/爆炸推动等外力位移）
        this.setPos(owner.getX(), owner.getY() + 0.02D, owner.getZ());
    }

    @Nullable
    private LivingEntity resolveOwner()
    {
        if (this.ownerUUID == null || !(this.level() instanceof ServerLevel serverLevel))
        {
            return null;
        }
        Entity entity = serverLevel.getEntity(this.ownerUUID);
        return entity instanceof LivingEntity living ? living : null;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound)
    {
        // shouldBeSaved=false，正常不会存盘；保留读取以应对极端情况
        if (compound.hasUUID("Owner"))
        {
            this.ownerUUID = compound.getUUID("Owner");
        }
        this.maxLifetime = compound.getInt("MaxLifetime");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound)
    {
        if (this.ownerUUID != null)
        {
            compound.putUUID("Owner", this.ownerUUID);
        }
        compound.putInt("MaxLifetime", this.maxLifetime);
    }

    @Override
    public boolean shouldBeSaved()
    {
        return false;
    }

    @Override
    public boolean isPickable()
    {
        return false;
    }

    @Override
    public boolean isPushable()
    {
        return false;
    }

    @Override
    public boolean canBeCollidedWith()
    {
        return false;
    }

    @Override
    public boolean isPushedByFluid()
    {
        return false;
    }

    @Override
    public boolean isAttackable()
    {
        return false;
    }

    @Override
    public boolean displayFireAnimation()
    {
        return false;
    }

    @Override
    public boolean saveAsPassenger(CompoundTag compound)
    {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket()
    {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers)
    {
        controllers.add(this.controller);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache()
    {
        return this.cache;
    }
}
