package dev.anvilcraft.pigeonplus.entity;

import dev.anvilcraft.pigeonplus.init.AddonEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
    /** 默认引信时长（tick）。 */
    public static final int DEFAULT_FUSE = 120;
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

    private static final EntityDataAccessor<Integer> DATA_LIFE =
        SynchedEntityData.defineId(GiantFireworkEntity.class, EntityDataSerializers.INT);
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

    /** 装填 2 颗烟火之星时的分散半径（格）。 */
    private static final double SPREAD_BASE = 3.0;
    /** 每多装 1 颗额外增加的分散半径（格）。 */
    private static final double SPREAD_PER_STAR = 1.0;
    /** 分散半径上限（格）。 */
    private static final double SPREAD_MAX = 18.0;

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

    /** 在指定主部件位置点火发射并返回新实体。 */
    public static GiantFireworkEntity launch(
        ServerLevel level,
        BlockPos mainPos,
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
        builder.define(DATA_LIFE, 0);
        builder.define(DATA_FUSE, DEFAULT_FUSE);
        builder.define(DATA_FIREWORKS_ITEM, new ItemStack(Items.FIREWORK_ROCKET));
        builder.define(DATA_DETONATING, false);
    }

    /** 是否已进入起爆阶段。 */
    public boolean isDetonating() {
        return this.entityData.get(DATA_DETONATING);
    }

    private void setDetonating(boolean detonating) {
        this.entityData.set(DATA_DETONATING, detonating);
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
            this.tickClient();
            return;
        }

        // 起爆阶段不再飞行，只计时等最后移除实体
        if (this.isDetonating()) {
            this.tickBurst();
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
     * 客户端每 tick：未起爆时喷尾焰，起爆后按 {@link #BURST_INTERVAL} 逐颗绽放。
     *
     * <h3>为什么绽放节奏放在客户端本地推进</h3>
     * 曾用「事件 id = 17 + 序号」把序号编进 {@code broadcastEntityEvent}，结果客户端崩溃：
     * 原版 {@code ClientPacketListener#handleEntityEvent} 对 id 21/35/63
     * （守卫者、图腾、嗅探兽）有硬编码特判并直接强转类型，序号 4 恰好落在 21 上，
     * 于是装填 5 颗以上必崩（{@code ClassCastException ... cannot be cast to Guardian}）。
     * 事件 id 是全局共享的，挑「没人用的 id」不可靠，因此改为完全不发实体事件：
     * 服务端只同步 {@link #DATA_DETONATING}，节奏由客户端按同一组常量本地推进。
     */
    private void tickClient() {
        if (!this.isDetonating()) {
            // 复位，以便（例如区块重新加载后）再次起爆时从头放
            this.clientBurstTicks = -1;
            this.clientBurstIndex = 0;
            this.spawnClientTrail();
            return;
        }
        // 起爆后停掉尾焰，且允许重载区块后重新喷一次点火烟团
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
     * 播放第 {@code index} 颗烟火之星的爆炸，各给一个球面偏移避免爆点重合
     * （原版 {@code FireworkParticles.Starter} 会把整份列表从同一点发出，会糊成一团）。
     * 空装填则照常调用一次，走 {@code ClientLevel#createFireworks} 的白烟分支。
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
        int total = explosions.size();
        Vec3 offset = spreadOffset(index, total, spreadRadius(total));
        this.level().createFireworks(
            this.getX() + offset.x,
            this.blastY() + offset.y,
            this.getZ() + offset.z,
            0.0, 0.0, 0.0,
            List.of(explosions.get(index))
        );
    }

    /**
     * 球面均匀分布（黄金角 / Fibonacci 球）。用黄金角而非按经纬度等分，
     * 后者会在两极堆积、装填数一多就两头挤中间稀。
     */
    private static Vec3 spreadOffset(int index, int total, double radius) {
        if (total <= 1 || radius <= 0.0) {
            return Vec3.ZERO;
        }
        double goldenAngle = Math.PI * (3.0 - Math.sqrt(5.0));
        double vertical = 1.0 - 2.0 * (index + 0.5) / total;
        double ring = Math.sqrt(Math.max(0.0, 1.0 - vertical * vertical));
        double theta = goldenAngle * index;
        return new Vec3(Math.cos(theta) * ring, vertical, Math.sin(theta) * ring).scale(radius);
    }

    /** 分散半径：装填越多铺得越开；上限避免 16 颗散成 19 格开外的独立烟花。 */
    private static double spreadRadius(int total) {
        if (total <= 1) {
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
