package dev.anvilcraft.pigeonplus.entity;

import dev.anvilcraft.pigeonplus.init.AddonEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 巨型烟花发射后的实体。
 *
 * <p>造型完全复用方块那三个 Blockbench 模型（见
 * {@code dev.anvilcraft.pigeonplus.client.renderer.entity.GiantFireworkRenderer}），
 * 所以发射前后外观一致：方块结构消失的同一帧，实体在同一位置接上。
 *
 * <h2>几何约定</h2>
 * 实体位置 = 主部件（{@code Y0_C}）底面中心，即 {@code (x + 0.5, y, z + 0.5)}。
 * 碰撞箱 3×3×9 由实体尺寸给出（{@code y} 起向上 9 格），与模型轮廓一致。
 *
 * <h2>飞行</h2>
 * 持续向上加速 + 空气阻力，终速约 1.25 格/tick，和原版烟花火箭接近。
 * 撞到方块或引信烧完即炸。
 *
 * <h2>爆炸</h2>
 * 默认<b>不破坏地形</b>：原版烟花火箭本来就不炸方块，这里保持一致，
 * 只做特效 + 对范围内实体造成爆炸伤害。若要让它对地形生效，
 * 可在 {@link #explode} 里改用 anvillib 的 {@code ExplosionExecutor}
 * （AnvilCraft 的分层球形爆炸，支持半径/每 tick 上限/融化替换）。
 */
public class GiantFireworkEntity extends Entity {
    /** 上升加速度，终速 ≈ 此值 / 阻力系数。 */
    private static final double THRUST = 0.025;
    /** 每 tick 速度衰减。 */
    private static final double DRAG = 0.98;
    /** 默认引信时长（tick）。 */
    public static final int DEFAULT_FUSE = 120;
    /** 爆炸对实体的作用半径。 */
    private static final float BLAST_RADIUS = 12.0F;
    /** 箭体半径，用于撒尾焰粒子。 */
    private static final double BODY_RADIUS = 1.2;
    /** 箭体高度（格），与实体尺寸一致。 */
    private static final double BODY_HEIGHT = 9.0;

    private static final EntityDataAccessor<Integer> DATA_LIFE =
        SynchedEntityData.defineId(GiantFireworkEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FUSE =
        SynchedEntityData.defineId(GiantFireworkEntity.class, EntityDataSerializers.INT);

    /** 点燃者，仅用于爆炸伤害的归因（可为 null）。 */
    @Nullable
    private Entity owner;

    public GiantFireworkEntity(EntityType<? extends GiantFireworkEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = false;
    }

    public GiantFireworkEntity(Level level, double x, double y, double z, int fuse) {
        this(AddonEntities.GIANT_FIREWORK.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.setFuse(fuse);
    }

    public void setOwner(@Nullable Entity owner) {
        this.owner = owner;
    }

    /**
     * 在指定主部件位置点火发射。
     *
     * @param level   服务端世界
     * @param mainPos 多方块主部件（底层中心）的坐标
     * @param owner   点燃者，用于伤害归因；可为 null
     * @return 生成的实体
     */
    public static GiantFireworkEntity launch(ServerLevel level, BlockPos mainPos, @Nullable Entity owner) {
        GiantFireworkEntity firework = new GiantFireworkEntity(
            level,
            mainPos.getX() + 0.5,
            mainPos.getY(),
            mainPos.getZ() + 0.5,
            DEFAULT_FUSE
        );
        firework.setOwner(owner);
        firework.setDeltaMovement(0.0, 0.5, 0.0);
        level.addFreshEntity(firework);
        level.playSound(
            null, mainPos, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.NEUTRAL,
            3.0F, 1.0F
        );
        return firework;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LIFE, 0);
        builder.define(DATA_FUSE, DEFAULT_FUSE);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    private void setLife(int life) {
        this.entityData.set(DATA_LIFE, life);
    }

    public int getFuse() {
        return this.entityData.get(DATA_FUSE);
    }

    private void setFuse(int fuse) {
        this.entityData.set(DATA_FUSE, fuse);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            this.spawnClientTrail();
            return;
        }

        int life = this.getLife() + 1;
        this.setLife(life);

        // 上升 + 阻力
        Vec3 motion = this.getDeltaMovement();
        motion = motion.add(0.0, THRUST, 0.0).scale(DRAG);
        this.setDeltaMovement(motion);
        this.move(MoverType.SELF, motion);

        // 撞到方块即炸（贴上即触发，不必等引信）
        if (this.horizontalCollision || this.verticalCollision) {
            this.explode();
            return;
        }
        if (life >= this.getFuse()) {
            this.explode();
        }
    }

    /** 客户端拖尾：沿箭体底部环状喷火花。 */
    private void spawnClientTrail() {
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        // 发动机喷口：底部一圈火花 + 烟
        for (int i = 0; i < 3; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double dx = Math.cos(angle) * BODY_RADIUS;
            double dz = Math.sin(angle) * BODY_RADIUS;
            this.level().addParticle(ParticleTypes.FIREWORK, x + dx, y + 0.2, z + dz,
                this.random.nextGaussian() * 0.02,
                -this.random.nextDouble() * 0.08,
                this.random.nextGaussian() * 0.02);
        }
        if (this.getLife() % 2 == 0) {
            this.level().addParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, x, y + 0.5, z,
                0.0, -0.03, 0.0);
        }
    }

    /**
     * 起爆：特效 + 范围内实体伤害，不破坏方块。
     *
     * <p>爆点取<b>箭体中心</b>而非实体原点。实体位置在火箭底面，直接用它起爆会让
     * 整团火花贴在尾巴上；火箭高 {@link #BODY_HEIGHT} 格，取一半即为视觉中心。
     */
    private void explode() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double x = this.getX();
        double y = this.getY() + BODY_HEIGHT / 2.0;
        double z = this.getZ();

        // 立体多层火花：比单次爆点更像「大烟花」
        for (int layer = 0; layer < 3; layer++) {
            double radius = 1.0 + layer * 0.6;
            int count = 60 + layer * 40;
            serverLevel.sendParticles(
                ParticleTypes.FIREWORK, x, y, z, count,
                radius, radius, radius, 0.15
            );
        }
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        serverLevel.sendParticles(ParticleTypes.FLASH, x, y, z, 3, 0.0, 0.0, 0.0, 0.0);

        // 范围伤害：按距离衰减，带击退
        AABB area = new AABB(x, y, z, x, y, z).inflate(BLAST_RADIUS);
        List<Entity> targets = serverLevel.getEntities(this, area);
        DamageSource source = this.damageSources().explosion(this, this.owner);
        for (Entity target : targets) {
            double distance = Math.sqrt(target.distanceToSqr(x, y, z));
            if (distance > BLAST_RADIUS) {
                continue;
            }
            float damage = (float) (BLAST_RADIUS - distance) * 1.5F;
            if (damage <= 0.0F) {
                continue;
            }
            target.hurt(source, damage);
            double push = (BLAST_RADIUS - distance) / BLAST_RADIUS * 2.0;
            Vec3 pushDir = target.position().subtract(x, y, z);
            if (pushDir.lengthSqr() > 1.0E-4) {
                pushDir = pushDir.normalize().scale(push);
                target.push(pushDir.x, pushDir.y * 0.5 + 0.3, pushDir.z);
            }
        }

        serverLevel.playSound(
            null, x, y, z, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.NEUTRAL,
            6.0F, 1.0F
        );
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Life", this.getLife());
        tag.putInt("Fuse", this.getFuse());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setLife(tag.getInt("Life"));
        this.setFuse(tag.contains("Fuse") ? tag.getInt("Fuse") : DEFAULT_FUSE);
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

    /** 不受爆炸推动/伤害自身，避免连锁。 */
    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        return true;
    }
}
