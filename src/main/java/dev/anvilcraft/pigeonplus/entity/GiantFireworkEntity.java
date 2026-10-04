package dev.anvilcraft.pigeonplus.entity;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.client.render.GiantFireworkBurst;
import dev.anvilcraft.pigeonplus.init.AddonEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 巨型烟花发射后的实体。造型复用方块那三个模型，发射前后外观一致。
 *
 * <p>位置 = 主部件底面中心 {@code (x+0.5, y, z+0.5)}，碰撞箱 3×3×9。
 * 推力随时间递增，所以先慢后快；引信烧完或撞到方块即炸，不破坏地形。
 *
 * <p>装填多颗烟火之星时，空间上按球面偏移分开、时间上按 {@link #BURST_INTERVAL} 逐颗绽放。
 *
 * <h2>飞行物理必须两端都跑</h2>
 * 客户端和服务端执行<b>同一套</b>推力积分与 {@code move}；只有服务端结算碰撞与起爆。
 *
 * <p>若让客户端只播粒子、位置纯靠位置包，画面上会抽搐：普通 {@code Entity} 的
 * {@code lerpTo} 是<b>直接吸附</b>（{@code setPos}），只有 {@code LivingEntity} 才把它覆写成
 * 按 {@code lerpSteps} 插值。而渲染插值用的是 {@code xOld}（tick 之前的位置），
 * 客户端不移动时 {@code xOld == getX()}，于是每个 tick 内都没有位移，
 * 位置只在收包时按 20Hz 整跳一格——高帧率下就是明显的抖动。
 * 原版 {@code FireworkRocketEntity} 与 {@code PrimedTnt} 同样在两端跑物理，这里保持一致。
 *
 * <p>同理，{@link #life} 不做网络同步：两端各自从 0 起算，才能算出同样的推力曲线。
 */
public class GiantFireworkEntity extends Entity {
    /** 点火初速（格/tick）。取小值，否则看不出起飞那一下的「慢」。 */
    private static final double LAUNCH_SPEED = 0.10;
    /** 点火时的基础推力（格/tick²）。 */
    private static final double THRUST_BASE = 0.002;
    /** 推力随已飞 tick 的增量（格/tick³）。递增推力是「先慢后快」的关键。 */
    private static final double THRUST_GAIN = 0.0007;
    /** 每 tick 速度衰减（空气阻力）。 */
    private static final double DRAG = 0.985;
    /** 末速上限（格/tick），防止引信被调很长时速度失控。 */
    private static final double MAX_SPEED = 3.5;
    /**
     * 默认引信时长（tick）。
     *
     * <p>取 93 是为了让爆点落在发射点上方约 80 格。推力曲线未动，
     * 所以「先慢后快」的形状与原先完全一致，只是提前收尾、总高变矮。
     */
    public static final int DEFAULT_FUSE = 93;
    /** 爆炸对实体的作用半径。 */
    private static final float BLAST_RADIUS = 12.0F;
    /** 箭体半径，用于撒尾焰粒子。 */
    private static final double BODY_RADIUS = 1.2;
    /**
     * 芯焰锥体的长度（格）。
     *
     * <p>焰尖向下伸出箭体底面，所以贴着地面放置时火焰会插进地面——
     * 这与实体碰撞箱无关（粒子不参与碰撞），观感上反而像喷口在烧地面。
     */
    private static final double CORE_LENGTH = 1.6;
    /** 箭体高度（格），与实体尺寸一致。 */
    private static final double BODY_HEIGHT = 9.0;

    /**
     * 已飞行 tick 数。
     *
     * <p><b>刻意不做网络同步</b>：两端各自从 0 起算、每 tick 各加一，天然保持在同一步调上。
     * 若改成同步字段，客户端读到的是几 tick 前的旧值，用它算出来的推力会与服务端不一致，
     * 位置随即被位置包「纠正」回来——这正是上升时抽搐的成因之一。
     * 它只用于本类的推力曲线与粒子节奏，外部无人读取。
     */
    private int life;

    private static final EntityDataAccessor<Integer> DATA_FUSE =
        SynchedEntityData.defineId(GiantFireworkEntity.class, EntityDataSerializers.INT);

    /**
     * 装填的烟火之星，用「带 FIREWORKS 组件的烟花火箭物品」承载。
     *
     * <p>因为 {@code EntityDataSerializers} 没有 {@code List<FireworkExplosion>} 的序列化器，
     * 而 ITEM_STACK 自带存档与网络同步（原版 {@code FireworkRocketEntity} 也这么做）。
     */
    private static final EntityDataAccessor<ItemStack> DATA_FIREWORKS_ITEM =
        SynchedEntityData.defineId(GiantFireworkEntity.class, EntityDataSerializers.ITEM_STACK);

    /** 起爆后每颗烟火之星之间的间隔（tick）。 */
    private static final int BURST_INTERVAL = 2;
    /** 最后一颗绽放后实体再多存活这么多 tick，避免客户端还没放完就被移除。 */
    private static final int BURST_LINGER = 20;

    /** 是否已进入起爆阶段。客户端据此停尾焰并隐藏箭体。 */
    private static final EntityDataAccessor<Boolean> DATA_DETONATING =
        SynchedEntityData.defineId(GiantFireworkEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * 装填 1 颗烟火之星时的分散半径（格）。
     *
     * <p>即便只装 1 颗也留出体积——它同样会炸成
     * {@link GiantFireworkBurst#BALLS_PER_STAR} 个球，挤在一点上就白拆了。
     */
    private static final double SPREAD_BASE = 5.0;
    /** 每多装 1 颗额外增加的分散半径（格）。 */
    private static final double SPREAD_PER_STAR = 1.5;
    /** 分散半径上限（格）。16 颗即 64 球，铺开到 27 格左右。 */
    private static final double SPREAD_MAX = 27.0;

    /**
     * 水平朝向（南北东西）。
     *
     * <p>发射前由方块状态带过来，发射后沿用——这样起飞瞬间的朝向与放置时一致，
     * 不会「一离地就转向」。渲染时用它绕 Y 轴旋转模型。
     *
     * <p>用 {@code DIRECTION} 序列化器（原版自带），无需自定义网络编码。
     */
    private static final EntityDataAccessor<Direction> DATA_FACING =
        SynchedEntityData.defineId(GiantFireworkEntity.class, EntityDataSerializers.DIRECTION);

    /** 点燃者，仅用于爆炸伤害的归因（可为 null）。 */
    @Nullable
    private Entity owner;

    /** 服务端：起爆阶段已持续的 tick 数，用于决定何时移除实体。 */
    private int burstTicks;
    /** 客户端：已播放的爆炸序号。 */
    private int clientBurstIndex;
    /** 客户端：起爆阶段已持续的 tick 数；负数表示尚未观察到起爆。 */
    private int clientBurstTicks = -1;
    /** 客户端：是否已喷过点火烟团（只在起飞瞬间放一次）。 */
    private boolean trailStarted;

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

    /** 把装填的烟火之星写入同步数据（用烟花火箭物品承载）。 */
    public void setExplosions(List<FireworkExplosion> explosions) {
        ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
        if (!explosions.isEmpty()) {
            stack.set(DataComponents.FIREWORKS, new Fireworks(0, List.copyOf(explosions)));
        }
        this.entityData.set(DATA_FIREWORKS_ITEM, stack);
    }

    /** 装填的烟火之星；未装填时返回空列表。 */
    public List<FireworkExplosion> getExplosions() {
        Fireworks fireworks = this.entityData.get(DATA_FIREWORKS_ITEM).get(DataComponents.FIREWORKS);
        return fireworks != null ? fireworks.explosions() : List.of();
    }

    /** 在指定主部件位置点火发射并返回新实体。{@code facing} 取自方块状态，发射后沿用。 */
    public static GiantFireworkEntity launch(
        ServerLevel level,
        BlockPos mainPos,
        Direction facing,
        @Nullable Entity owner,
        List<FireworkExplosion> explosions
    ) {
        GiantFireworkEntity firework = new GiantFireworkEntity(
            level,
            mainPos.getX() + 0.5,
            mainPos.getY(),
            mainPos.getZ() + 0.5,
            DEFAULT_FUSE
        );
        firework.setOwner(owner);
        firework.setFacing(facing);
        firework.setExplosions(explosions);
        firework.setDeltaMovement(0.0, LAUNCH_SPEED, 0.0);
        level.addFreshEntity(firework);
        level.playSound(
            null, mainPos, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.NEUTRAL,
            3.0F, 1.0F
        );
        return firework;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_FUSE, DEFAULT_FUSE);
        builder.define(DATA_FIREWORKS_ITEM, new ItemStack(Items.FIREWORK_ROCKET));
        builder.define(DATA_DETONATING, false);
        builder.define(DATA_FACING, Direction.NORTH);
    }

    /** 水平朝向，渲染时用于绕 Y 轴旋转模型。 */
    public Direction getFacing() {
        return this.entityData.get(DATA_FACING);
    }

    public void setFacing(Direction facing) {
        this.entityData.set(DATA_FACING, facing.getAxis().isHorizontal() ? facing : Direction.NORTH);
    }

    /** 是否已进入起爆阶段。 */
    public boolean isDetonating() {
        return this.entityData.get(DATA_DETONATING);
    }

    private void setDetonating(boolean detonating) {
        this.entityData.set(DATA_DETONATING, detonating);
    }

    public int getLife() {
        return this.life;
    }

    private void setLife(int life) {
        this.life = life;
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

        // 客户端在起爆阶段只播粒子；飞行阶段则与服务端跑同一套物理（见下）。
        if (this.level().isClientSide() && this.isDetonating()) {
            this.tickClientBurst();
            return;
        }
        if (this.level().isClientSide()) {
            this.spawnClientTrail();
        }

        // 起爆阶段不再飞行，只计时等最后移除实体
        if (this.isDetonating()) {
            if (!this.level().isClientSide()) {
                this.tickBurst();
            }
            return;
        }

        int life = this.getLife() + 1;
        this.setLife(life);

        // 上升：推力随飞行时间递增 → 先慢后快、逐渐加速
        Vec3 motion = this.getDeltaMovement();
        double thrust = THRUST_BASE + THRUST_GAIN * life;
        motion = motion.add(0.0, thrust, 0.0).scale(DRAG);
        if (motion.y > MAX_SPEED) {
            motion = new Vec3(motion.x, MAX_SPEED, motion.z);
        }
        this.setDeltaMovement(motion);
        this.move(MoverType.SELF, motion);

        // 只有服务端结算碰撞与起爆；客户端跑物理只是为了跟手（见类文档）
        if (this.level().isClientSide()) {
            return;
        }
        // 撞到方块即炸（贴上即触发，不必等引信）
        if (this.horizontalCollision || this.verticalCollision) {
            this.detonate();
            return;
        }
        if (life >= this.getFuse()) {
            this.detonate();
        }
    }

    /**
     * 进入起爆阶段：当场刹停，并结算一次伤害与音效。
     *
     * <p>必须刹停——绽放要持续到 {@code BURST_INTERVAL × 颗数} tick，而末速可达 3 格/tick，
     * 不刹车整串会拖成一条直线。
     * 伤害只在这里结算一次，后续逐颗绽放纯属视觉，否则 16 颗会叠成 16 倍伤害。
     */
    private void detonate() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.setDetonating(true);
        this.setDeltaMovement(Vec3.ZERO);

        double x = this.getX();
        double y = this.blastY();
        double z = this.getZ();

        this.gameEvent(GameEvent.EXPLODE, this.owner);

        // 范围伤害：按距离衰减，带击退
        AABB area = new AABB(x, y, z, x, y, z).inflate(BLAST_RADIUS);
        DamageSource source = this.damageSources().explosion(this, this.owner);
        for (Entity target : serverLevel.getEntities(this, area)) {
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
        this.burstTicks = 0;
    }

    /** 服务端起爆阶段：只计时，等客户端放完整串再移除实体。绽放节奏在客户端。 */
    private void tickBurst() {
        if (++this.burstTicks > burstDuration(this.getExplosions().size())) {
            this.discard();
        }
    }

    /**
     * 起爆阶段应持续的 tick 数。多加 {@link #BURST_LINGER} 是为了让客户端放完最后一颗：
     * 实体被移除后客户端就不再 tick，后续几颗会半路消失。
     */
    private static int burstDuration(int starCount) {
        return Math.max(1, starCount) * BURST_INTERVAL + BURST_LINGER;
    }

    /**
     * 客户端起爆阶段：按 {@link #BURST_INTERVAL} 逐颗绽放。
     *
     * <h3>为什么绽放节奏放在客户端本地推进</h3>
     * 曾用「事件 id = 17 + 序号」把序号编进 {@code broadcastEntityEvent}，结果客户端崩溃：
     * 原版 {@code ClientPacketListener#handleEntityEvent} 对 id 21/35/63
     * （守卫者、图腾、嗅探兽）有硬编码特判并直接强转类型，序号 4 恰好落在 21 上，
     * 于是装填 5 颗以上必崩（{@code ClassCastException ... cannot be cast to Guardian}）。
     * 事件 id 是全局共享的，挑「没人用的 id」不可靠，因此改为完全不发实体事件：
     * 服务端只同步 {@link #DATA_DETONATING}，节奏由客户端按同一组常量本地推进。
     */
    private void tickClientBurst() {
        // 停掉尾焰，且允许重载区块后重新喷一次点火烟团
        this.trailStarted = false;
        if (this.clientBurstTicks < 0) {
            this.clientBurstTicks = 0;
            this.clientBurstIndex = 0;
        }
        int total = Math.max(1, this.getExplosions().size());
        if (this.clientBurstIndex >= total) {
            return;
        }
        if (this.clientBurstTicks % BURST_INTERVAL == 0) {
            this.spawnBurst(this.clientBurstIndex);
            this.clientBurstIndex++;
        }
        this.clientBurstTicks++;
    }

    /**
     * 客户端拖尾：喷口芯焰 + 外圈火花 + 烟羽 + 余烬，强度随速度增长。
     *
     * <p>各层都带向下的初速（{@code -motion.y * 系数}），所以粒子会留在箭体后方拉出尾迹，
     * 而不是跟着火箭一起走。
     */
    private void spawnClientTrail() {
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        Vec3 motion = this.getDeltaMovement();
        double speed = motion.length();
        // 点火时约 0、末速时约 1。尾焰据此从一小团长成整条喷流，呼应「先慢后快」的加速。
        double throttle = Math.min(1.0, speed / MAX_SPEED);

        if (!this.trailStarted) {
            this.trailStarted = true;
            this.spawnIgnitionPuff(x, y, z);
        }

        // 1) 喷口芯焰：以喷口为顶点向下的锥形火焰，中心更密
        //    along 用两次随机相乘来偏置，粒子因此集中在喷口附近，形成亮芯而非均匀雾团。
        int coreCount = 5 + (int) Math.round(throttle * 11.0);
        for (int i = 0; i < coreCount; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double along = this.random.nextDouble() * this.random.nextDouble();
            double radius = BODY_RADIUS * (0.18 + 0.62 * along) * Math.sqrt(this.random.nextDouble());
            double dx = Math.cos(angle) * radius;
            double dz = Math.sin(angle) * radius;
            this.level().addParticle(
                ParticleTypes.FLAME,
                x + dx, y - along * CORE_LENGTH, z + dz,
                dx * 0.2,
                -motion.y * 0.5 - 0.02,
                dz * 0.2
            );
        }

        // 2) 外圈火花：贴喷口外圈的白色火星，向外下方甩
        int sparkCount = 2 + (int) Math.round(throttle * 2.0);
        for (int i = 0; i < sparkCount; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            double outward = 0.04 + this.random.nextDouble() * 0.04;
            this.level().addParticle(
                ParticleTypes.FIREWORK,
                x + dx * BODY_RADIUS, y + 0.2, z + dz * BODY_RADIUS,
                dx * outward,
                -motion.y * 0.35 - this.random.nextDouble() * 0.05,
                dz * outward
            );
        }

        // 3) 烟羽：沿上一 tick 走过的路径补点。火箭每 tick 前进 speed 格，
        //    只在末端喷会拉成一串断点，补点才连得成完整尾巴。
        if (this.getLife() % 2 == 0) {
            int puffs = 1 + (int) Math.round(throttle);
            for (int i = 0; i < puffs; i++) {
                double back = (double) i / puffs;
                this.level().addParticle(
                    this.random.nextBoolean()
                        ? ParticleTypes.LARGE_SMOKE
                        : ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                    x - motion.x * back, y - motion.y * back + 0.4, z - motion.z * back,
                    this.random.nextGaussian() * 0.03,
                    -0.02 - this.random.nextDouble() * 0.03,
                    this.random.nextGaussian() * 0.03
                );
            }
        }

        // 4) 余烬：零星飘落的灰烬，留下「烧过」的残迹
        if (this.getLife() % 3 == 0) {
            this.level().addParticle(
                ParticleTypes.ASH,
                x + this.random.nextGaussian() * BODY_RADIUS,
                y + 0.3,
                z + this.random.nextGaussian() * BODY_RADIUS,
                this.random.nextGaussian() * 0.02,
                -0.05 - this.random.nextDouble() * 0.05,
                this.random.nextGaussian() * 0.02
            );
        }

        // 5) 高速时补一道内层亮流，强化冲刺感
        if (throttle > 0.5) {
            int streaks = (int) Math.round(throttle * 2.0);
            for (int i = 0; i < streaks; i++) {
                double back = this.random.nextDouble();
                this.level().addParticle(
                    ParticleTypes.END_ROD,
                    x - motion.x * back, y - motion.y * back + 0.2, z - motion.z * back,
                    0.0, -motion.y * 0.15, 0.0
                );
            }
        }
    }

    /** 点火瞬间的地面烟团，让「起飞」这一下有分量。 */
    private void spawnIgnitionPuff(double x, double y, double z) {
        for (int i = 0; i < 16; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double radius = BODY_RADIUS * (0.6 + this.random.nextDouble());
            this.level().addParticle(
                ParticleTypes.LARGE_SMOKE,
                x + Math.cos(angle) * radius, y + 0.2, z + Math.sin(angle) * radius,
                Math.cos(angle) * 0.12, 0.02, Math.sin(angle) * 0.12
            );
        }
        for (int i = 0; i < 18; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double radius = BODY_RADIUS * this.random.nextDouble();
            this.level().addParticle(
                ParticleTypes.FLAME,
                x + Math.cos(angle) * radius, y + 0.15, z + Math.sin(angle) * radius,
                Math.cos(angle) * 0.05, 0.01, Math.sin(angle) * 0.05
            );
        }
    }

    /**
     * 播放第 {@code index} 颗烟火之星的爆炸——展开为 {@link GiantFireworkBurst#BALLS_PER_STAR} 个球。
     *
     * <p>这 4 个球<b>不与本星位置绑定</b>：64 个球（16 星）全部在整个爆炸体积内随机撒开，
     * 所以观感是「一大团散开的球」而非「每处挤 4 个」。位置用固定种子生成，
     * 保证各客户端看到的排布一致。
     *
     * <p>空装填则照常调用一次，走 {@code ClientLevel#createFireworks} 的白烟分支。
     */
    private void spawnBurst(int index) {
        List<FireworkExplosion> explosions = this.getExplosions();
        if (explosions.isEmpty()) {
            this.level().createFireworks(
                this.getX(), this.blastY(), this.getZ(), 0.0, 0.0, 0.0, explosions
            );
            return;
        }
        if (index < 0 || index >= explosions.size()) {
            return;
        }
        FireworkExplosion explosion = explosions.get(index);
        double radius = spreadRadius(explosions.size());
        // 以爆炸序号作种子的固定随机源：与实体自身的 random 无关，
        // 因此两端/多次渲染结果稳定，不会出现「同一发烟花排布乱跳」。
        RandomSource random = RandomSource.create(this.getId() * 31L + index);
        for (int ball = 0; ball < GiantFireworkBurst.BALLS_PER_STAR; ball++) {
            Vec3 offset = randomOffset(random, radius);
            GiantFireworkBurst.spawn(
                this.level(),
                random,
                this.getX() + offset.x,
                this.blastY() + offset.y,
                this.getZ() + offset.z,
                explosion,
                GiantFireworkBurst.ballScale(random, GiantFireworkBurst.randomScale(random))
            );
        }
    }

    /**
     * 在半径为 {@code radius} 的球内均匀取一点。
     *
     * <p>立方的根号是为了让点在<b>体积</b>上均匀：直接取 {@code r × random()} 会让点往球心堆积
     * （半径越小、球壳越小，却分到同样多的点），看起来中心过密。
     */
    private static Vec3 randomOffset(RandomSource random, double radius) {
        if (radius <= 0.0) {
            return Vec3.ZERO;
        }
        // 方向：球面均匀（z 均匀 + 方位角均匀，避免两极堆积）
        double z = random.nextDouble() * 2.0 - 1.0;
        double ring = Math.sqrt(Math.max(0.0, 1.0 - z * z));
        double theta = random.nextDouble() * Math.PI * 2.0;
        // 半径：立方根，保证体积均匀
        double r = radius * Math.cbrt(random.nextDouble());
        return new Vec3(Math.cos(theta) * ring * r, z * r, Math.sin(theta) * ring * r);
    }

    /** 分散半径：装填越多铺得越开。 */
    private static double spreadRadius(int total) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.min(SPREAD_MAX, SPREAD_BASE + SPREAD_PER_STAR * (total - 1));
    }

    /** 爆点高度：箭体中心（实体原点在底面）。 */
    private double blastY() {
        return this.getY() + BODY_HEIGHT / 2.0;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Life", this.getLife());
        tag.putInt("Fuse", this.getFuse());
        tag.put("FireworksItem", this.entityData.get(DATA_FIREWORKS_ITEM).save(this.registryAccess()));
        // 存起爆进度，避免起爆途中卸载区块后实体永不消失。
        // 客户端序号不必存档：可由 DETONATING 与爆炸列表重新推导。
        tag.putBoolean("Detonating", this.isDetonating());
        tag.putInt("BurstTicks", this.burstTicks);
        tag.putString("Facing", this.getFacing().getSerializedName());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setLife(tag.getInt("Life"));
        this.setFuse(tag.contains("Fuse") ? tag.getInt("Fuse") : DEFAULT_FUSE);
        if (tag.contains("FireworksItem", Tag.TAG_COMPOUND)) {
            this.entityData.set(
                DATA_FIREWORKS_ITEM,
                ItemStack.parse(this.registryAccess(), tag.getCompound("FireworksItem"))
                    .orElseGet(() -> new ItemStack(Items.FIREWORK_ROCKET))
            );
        }
        this.setDetonating(tag.getBoolean("Detonating"));
        this.burstTicks = tag.getInt("BurstTicks");
        Direction.CODEC.parse(NbtOps.INSTANCE, tag.get("Facing"))
            .resultOrPartial(error -> AnvilCraftPigeonPlus.LOGGER.warn(
                "巨型烟花：无法读取朝向，按北处理：{}", error))
            .ifPresent(this::setFacing);
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
