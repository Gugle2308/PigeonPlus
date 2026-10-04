package dev.anvilcraft.pigeonplus.entity;

import dev.anvilcraft.pigeonplus.block.LargeFireworksRocketBlock;
import dev.anvilcraft.pigeonplus.init.AddonEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 大型烟花顶上的座位，让玩家能坐到火箭顶面，点火后随火箭一起升空。
 */
public class LargeFireworksRocketSeatEntity extends Entity {
    public LargeFireworksRocketSeatEntity(EntityType<? extends LargeFireworksRocketSeatEntity> type, Level level) {
        super(type, level);
        // 座位不移动也不参与碰撞，只是挂点。
        this.noPhysics = true;
    }

    public LargeFireworksRocketSeatEntity(Level level, double x, double y, double z) {
        this(AddonEntities.LARGE_FIREWORKS_ROCKET_SEAT.get(), level);
        this.setPos(x, y, z);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        if (!this.isVehicle() || !this.isOnRocket()) {
            this.discard();
        }
    }

    /** 脚下的方块还是不是大型烟花。 */
    private boolean isOnRocket() {
        BlockPos below = BlockPos.containing(this.getX(), this.getY() - 1.0E-3, this.getZ());
        return this.level().getBlockState(below).getBlock() instanceof LargeFireworksRocketBlock;
    }

    /**
     * 乘客挂点就是座位自身位置（火箭顶面），不加偏移。
     */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        return Vec3.ZERO;
    }

    /** 不需要同步任何字段，位置由方块决定。 */
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    /** 没有需要持久化的状态：位置由方块决定，乘客关系由原版存档处理。 */
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }
}
