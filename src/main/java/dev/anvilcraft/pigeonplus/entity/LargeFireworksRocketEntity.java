package dev.anvilcraft.pigeonplus.entity;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.client.render.LargeFireworksRocketBurst;
import dev.anvilcraft.pigeonplus.init.AddonDamageTypes;
import dev.anvilcraft.pigeonplus.init.AddonEntities;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 大型烟花发射后的实体。造型复用方块的模型，发射前后外观一致。
 */
public class LargeFireworksRocketEntity extends Entity {
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
    /**
     * 载人时的引信上限（tick）。
     *
     * <p>登月需飞到 500 格以上，按现有推力曲线约在 life=224 达标，取 300 留足余量。
     * 到顶仍未等到午夜窗口就照常爆炸，避免载人火箭无限上升。
     */
    private static final int MOON_FUSE = 300;
    /**
     * 登月成功概率。
     *
     * <p>失败时火箭继续升空，到 {@link #MOON_FUSE} 后照常爆炸。
     */
    private static final double MOON_SUCCESS_CHANCE = 0.5;
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
     * 悬浮珍珠相对火箭底部的高度（格）。
     *
     * <p>箭尖最高点在顶段模型的 Y=30（相对 Y7_C 锚点即 {@code 7 + 30/16 ≈ 8.88}），
     * 所以取 {@link #BODY_HEIGHT}（9.0）让它刚好坐在箭尖上，而不是浮在半空。
     * 想让它贴近或飘高，只调这一个值。
     *
     * <p>实体渲染器与方块渲染器共用此值：方块形态的底部就是主部件，两者原点只差半格水平偏移，
     * 所以同一个高度在两处都成立。两处用同一常量，避免改了一边忘了另一边。
     */
    public static final double PEARL_RENDER_HEIGHT = BODY_HEIGHT;

    /**
     * 悬浮珍珠的额外缩放倍数。
     *
     * <p>物品用 {@code ItemDisplayContext.GROUND} 渲染时会套用它自带的 display 变换，
     * 其中 {@code ground} 一项是 {@code scale: 0.5}——珍珠按「地上掉落物」的大小显示，
     * 挂在 9 格高的箭尖上几乎看不见。
     *
     * <p>这里再乘一层补回来：取 2.0 正好抵消那个 0.5，即按物品的原始尺寸显示
     * （相比之前放大一倍）。想再大就调这个值。
     */
    public static final float PEARL_RENDER_SCALE = 2.0F;

    /**
     * 最多可装填的末影珍珠数。
     *
     * <p>每颗珍珠由投放它的玩家绑定，发射时各自把自己的主人送上月球——
     * 「多人登月」靠的就是这个，而非多个座位。
     */
    public static final int MAX_PEARLS = 4;

    /** 月球维度（AnvilCraft 本体）。 */
    private static final ResourceKey<Level> MUN_LEVEL =
        ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("anvilcraft", "mun"));
    /** 登月所需高度（格）。与本体 {@code MunTravelEventListener.ESCAPE_HEIGHT} 一致。 */
    private static final double ESCAPE_HEIGHT = 512.0;
    /**
     * 登月的时间窗口：午夜（{@code dayTime % 24000} 落在 18000±1000）。
     */
    private static final long MIDNIGHT = 18000L;
    private static final long MIDNIGHT_WINDOW = 1000L;
    /** 月球落点与月球出生点的水平距离范围（格）。 */
    private static final int ARRIVAL_MIN_DISTANCE = 8;
    private static final int ARRIVAL_MAX_DISTANCE = 16;

    /**
     * 已飞行 tick 数。
     */
    @Getter
    private int life;

    /**
     * 客户端插值状态：从服务端位置包收到的目标点，以及还要几 tick 走完。
     */
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;
    private double lerpXRot;

    /**
     * 本次飞行的登月判定结果：{@code null} = 还没掷过骰。
     *
     * <p><b>必须缓存，不能每次都掷。</b>{@link #tryMoonTravel} 每 5 tick 被调用一次，
     * 从达标到引信烧完大约会调用 15 次；若每次重掷，失败概率会从 50% 塌缩到
     * {@code 0.5^15}（约 0.003%），这个特性就等于不存在。
     *
     * <p>用三态而非 {@code boolean}：{@code false} 是「已判定为失败」这一有效结果，
     * 需要一个独立的值表示「尚未判定」。
     */
    @Nullable
    private Boolean moonTravelSucceeds;

    private static final EntityDataAccessor<Integer> DATA_FUSE =
        SynchedEntityData.defineId(LargeFireworksRocketEntity.class, EntityDataSerializers.INT);

    /**
     * 装填的烟火之星，用「带 FIREWORKS 组件的烟花火箭物品」承载。
     */
    private static final EntityDataAccessor<ItemStack> DATA_FIREWORKS_ITEM =
        SynchedEntityData.defineId(LargeFireworksRocketEntity.class, EntityDataSerializers.ITEM_STACK);

    /** 起爆后每颗烟火之星之间的间隔（tick）。 */
    private static final int BURST_INTERVAL = 2;
    /** 最后一颗绽放后实体再多存活这么多 tick，避免客户端还没放完就被移除。 */
    private static final int BURST_LINGER = 20;

    /** 是否已进入起爆阶段。客户端据此停尾焰并隐藏箭体。 */
    private static final EntityDataAccessor<Boolean> DATA_DETONATING =
        SynchedEntityData.defineId(LargeFireworksRocketEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * 已装填的末影珍珠数量，仅用于客户端显示。
     *
     * <p>真正的珍珠归属（绑定了哪些玩家）只在服务端持有，见 {@link #pearlOwners}。
     */
    private static final EntityDataAccessor<Integer> DATA_PEARLS =
        SynchedEntityData.defineId(LargeFireworksRocketEntity.class, EntityDataSerializers.INT);

    /**
     * 装填的末影珍珠各自绑定的玩家。
     *
     * <p>照搬末影珍珠的语义：谁投放就绑定谁。发射到达高度后，每颗珍珠把自己的主人
     * 传送到月球——<b>主人不需要坐在火箭上</b>，人在别处也会被拉过去。
     * -- GETTER --
     * 装填的珍珠各自绑定的玩家。

     */
    @Getter
    private List<UUID> pearlOwners = List.of();

    /**
     * 装填 1 颗烟火之星时的分散半径（格）。
     *
     * <p>即便只装 1 颗也留出体积——它同样会炸成
     * {@link LargeFireworksRocketBurst#BALLS_PER_STAR} 个球，挤在一点上就白拆了。
     */
    private static final double SPREAD_BASE = 5.0;
    /** 每多装 1 颗额外增加的分散半径（格）。 */
    private static final double SPREAD_PER_STAR = 1.5;
    /** 分散半径上限（格）。16 颗即 64 球，铺开到 27 格左右。 */
    private static final double SPREAD_MAX = 27.0;

    /**
     * 水平朝向（南北东西）。
     */
    private static final EntityDataAccessor<Direction> DATA_FACING =
        SynchedEntityData.defineId(LargeFireworksRocketEntity.class, EntityDataSerializers.DIRECTION);

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

    public LargeFireworksRocketEntity(EntityType<? extends LargeFireworksRocketEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = false;
    }

    public LargeFireworksRocketEntity(Level level, double x, double y, double z, int fuse) {
        this(AddonEntities.LARGE_FIREWORKS_ROCKET.get(), level);
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

    /**
     * 在指定主部件位置点火发射并返回新实体。{@code facing} 取自方块状态，发射后沿用。
     *
     * @param pearlOwners 已装填末影珍珠绑定的玩家；非空时必定登月，且各自被送上月球
     */
    public static LargeFireworksRocketEntity launch(
        ServerLevel level,
        BlockPos mainPos,
        Direction facing,
        @Nullable Entity owner,
        List<FireworkExplosion> explosions,
        List<UUID> pearlOwners
    ) {
        LargeFireworksRocketEntity firework = new LargeFireworksRocketEntity(
            level,
            mainPos.getX() + 0.5,
            mainPos.getY(),
            mainPos.getZ() + 0.5,
            DEFAULT_FUSE
        );
        firework.setOwner(owner);
        firework.setFacing(facing);
        firework.setExplosions(explosions);
        firework.setPearlOwners(pearlOwners);
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
        builder.define(DATA_PEARLS, 0);
    }

    /** 已装填的末影珍珠数量；大于 0 时本次发射必定登月。 */
    public int getPearls() {
        return this.entityData.get(DATA_PEARLS);
    }

    /** 设置珍珠归属，并同步数量供客户端读取。 */
    public void setPearlOwners(List<UUID> owners) {
        this.pearlOwners = List.copyOf(owners.subList(0, Math.min(owners.size(), MAX_PEARLS)));
        this.entityData.set(DATA_PEARLS, this.pearlOwners.size());
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

    private void setLife(int life) {
        this.life = life;
    }

    public int getFuse() {
        return this.entityData.get(DATA_FUSE);
    }

    private void setFuse(int fuse) {
        this.entityData.set(DATA_FUSE, fuse);
    }

    private void lerpClientStep() {
        if (this.lerpSteps > 0) {
            this.lerpPositionAndRotationStep(
                this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.lerpXRot);
            this.lerpSteps--;
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = yRot;
        this.lerpXRot = xRot;
        this.lerpSteps = steps;
    }

    @Override
    public double lerpTargetX() {
        return this.lerpSteps > 0 ? this.lerpX : this.getX();
    }

    @Override
    public double lerpTargetY() {
        return this.lerpSteps > 0 ? this.lerpY : this.getY();
    }

    @Override
    public double lerpTargetZ() {
        return this.lerpSteps > 0 ? this.lerpZ : this.getZ();
    }

    @Override
    public float lerpTargetXRot() {
        return this.lerpSteps > 0 ? (float) this.lerpXRot : this.getXRot();
    }

    @Override
    public float lerpTargetYRot() {
        return this.lerpSteps > 0 ? (float) this.lerpYRot : this.getYRot();
    }

    @Override
    public void tick() {
        super.tick();

        // 客户端在起爆阶段只播粒子
        if (this.level().isClientSide() && this.isDetonating()) {
            this.tickClientBurst();
            return;
        }
        // 客户端不跑推力积分，只把服务端位置插值过来（见 lerpTo 的说明）。
        //
        // 两端各跑一份物理看似更跟手，实际正是抖动的根源：life 不参与网络同步，
        // 客户端从 0 起算、服务端从点火那一刻起算，两者必然错开几 tick；
        // 而推力随 life 递增，于是每 tick 的位移量都不同，客户端位置持续偏离服务端，
        // 服务端每 tick 发来的位置包再把它硬拽回去一次——高帧率下就是「一卡一卡」。
        if (this.level().isClientSide()) {
            this.lerpClientStep();
            // 仍然推进 life：尾焰的疏密节奏用它取模，不推进会让烟羽每 tick 都喷。
            this.setLife(this.getLife() + 1);
            this.spawnClientTrail();
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

        // 登月：高度达标 + 午夜 → 送人去月球，火箭本体消失
        if (this.tryMoonTravel(life)) {
            return;
        }
        // 撞到方块即炸（贴上即触发，不必等引信）
        if (this.horizontalCollision || this.verticalCollision) {
            this.detonate();
            return;
        }
        // 需要长引信的情况都延长到 MOON_FUSE：默认 93 tick 只够飞到 78 格，
        // 而登月要 500 格以上（约 life=224）。
        //   - 有乘客：人要跟着上到那里
        //   - 有珍珠：珍珠的主人要等火箭飞到那里才被传送，哪怕没人在火箭上
        // 但不能无上限——若整段飞行都没等到午夜窗口，到 MOON_FUSE 仍照常爆炸。
        boolean needsAltitude =
            !this.getPassengers().isEmpty() || !this.pearlOwners.isEmpty();
        int fuse = needsAltitude ? MOON_FUSE : this.getFuse();
        if (life >= fuse) {
            this.detonate();
        }
    }

    /**
     * 尝试登月。有两条路径：
     *
     * <h2>珍珠路径（必定成功）</h2>
     * 装填了末影珍珠时，珍珠各自把绑定的玩家送上月球——<b>该玩家不必坐在火箭上</b>，
     * 这也正是「多人登月」的由来：4 颗珍珠 = 4 名玩家，各自由自己的那颗带走。
     * 走到这里不再掷骰，必定成功。
     *
     * <h2>乘客路径（{@link #MOON_SUCCESS_CHANCE} 概率）</h2>
     * 没有珍珠时，只有坐在火箭上的那名乘客有机会登月，成功与否掷一次骰并缓存
     * （原因见 {@link #moonTravelSucceeds}）。失败则继续升空，到 {@link #MOON_FUSE} 爆炸。
     *
     * <p>两条路径共用同一套判定：高度 &gt; {@link #ESCAPE_HEIGHT}、主世界、午夜窗口内。
     *
     * @param life 已飞 tick 数，用于降低检测频率
     * @return 是否已触发登月
     */
    private boolean tryMoonTravel(int life) {
        // 每 5 tick 检一次：条件一旦满足会持续成立，不必逐 tick 判定
        if (life % 5 != 0) {
            return false;
        }
        if (this.getY() <= ESCAPE_HEIGHT) {
            return false;
        }
        if (!(this.level() instanceof ServerLevel overworld)
            || !overworld.dimension().equals(Level.OVERWORLD)) {
            return false;
        }
        long time = Math.abs(Math.floorMod(overworld.getDayTime(), 24000L) - MIDNIGHT);
        if (time > MIDNIGHT_WINDOW) {
            return false;
        }
        MinecraftServer server = overworld.getServer();
        ServerLevel mun = server.getLevel(MUN_LEVEL);
        if (mun == null) {
            return false;
        }

        // 珍珠路径优先：装了就必定成功，且不要求有人在火箭上。
        if (!this.pearlOwners.isEmpty()) {
            List<ServerPlayer> already = new ArrayList<>();
            for (UUID owner : this.pearlOwners) {
                ServerPlayer player = server.getPlayerList().getPlayer(owner);
                if (player != null) {
                    this.sendToMoon(player, mun);
                    already.add(player);
                }
            }
            // 火箭要被消耗掉了，还坐在上面的乘客也必须一起送走。
            // 否则他们会从 500 格高空被抛下摔死——那是静默且不公平的结果，
            // 毕竟乘客只是搭了个便车，决定权不在他手里。
            for (Entity rider : List.copyOf(this.getPassengers())) {
                if (rider instanceof ServerPlayer player && !already.contains(player)) {
                    this.sendToMoon(player, mun);
                }
            }
            // 无论主人在不在线，火箭本体都消失：珍珠已经消耗掉了，
            // 不该让它在月球上空继续飘着或原地爆炸。
            this.discard();
            return true;
        }

        // 乘客路径：只有坐在火箭上的玩家能登月，且要掷骰。
        if (this.getPassengers().isEmpty()
            || !(this.getPassengers().getFirst() instanceof ServerPlayer passenger)) {
            return false;
        }
        // 到这里才算「本可以登月」，此时才掷骰——并且整趟飞行只掷这一次。
        if (this.moonTravelSucceeds == null) {
            this.moonTravelSucceeds = this.random.nextDouble() < MOON_SUCCESS_CHANCE;
        }
        if (!this.moonTravelSucceeds) {
            // 失败：什么也不做，让火箭继续升空，到 MOON_FUSE 自然爆炸。
            return false;
        }
        this.sendToMoon(passenger, mun);        // 火箭本体消失，不在月球上空留下失控烟花
        this.discard();
        return true;
    }

    /**
     * 把一名玩家送到月球出生点附近的安全地表。
     */
    private void sendToMoon(ServerPlayer player, ServerLevel mun) {
        player.stopRiding();
        RandomSource random = mun.getRandom();
        double angle = random.nextDouble() * (Math.PI * 2.0);
        double distance = ARRIVAL_MIN_DISTANCE
            + random.nextDouble() * (ARRIVAL_MAX_DISTANCE - ARRIVAL_MIN_DISTANCE);
        BlockPos spawn = mun.getSharedSpawnPos();
        BlockPos landing = CelestialTravelManager.findSafeSurfaceLandingPos(
            mun,
            new BlockPos(
                spawn.getX() + Mth.floor(Math.cos(angle) * distance),
                0,
                spawn.getZ() + Mth.floor(Math.sin(angle) * distance)
            )
        );
        if (landing == null) {
            return;
        }
        player.changeDimension(new DimensionTransition(
            mun,
            Vec3.atBottomCenterOf(landing),
            Vec3.ZERO,
            player.getYRot(),
            player.getXRot(),
            DimensionTransition.DO_NOTHING
        ));
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

        // 坐在火箭上的乘客必死，且用专门的死因文案。
        //
        // 不能指望下面的范围伤害：爆点在箭体中心（y+4.5），而乘客坐在顶面（y+9），
        // 相距 4.5 格时只结算 (12-4.5)*1.5 = 11.25 点，裸装玩家还剩 8.75 血。
        // 所以这里直接判死，而不是给一个大伤害值——避免被护甲、抗性、吸收等削减，
        // 也让「跟火箭一起炸了」这件事在任何装备下都成立。
        this.killRiders(serverLevel);

        // 范围伤害：按距离衰减，带击退
        AABB area = new AABB(x, y, z, x, y, z).inflate(BLAST_RADIUS);
        DamageSource source = this.damageSources().explosion(this, this.owner);
        for (Entity target : serverLevel.getEntities(this, area)) {
            if (isRider(target)) {
                // 乘客已经由 escapeTowerFailure 处理，不再吃第二份爆炸伤害
                continue;
            }
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

    /** 该实体是不是正骑在火箭上的乘客（含间接乘客，例如乘客再载着的东西）。 */
    private boolean isRider(Entity entity) {
        return entity.getRootVehicle() == this;
    }

    /**
     * 让所有乘客必死，死因显示为「逃逸塔没有启动」。
     *
     * <p>用一次 {@code hurt} 打出致死伤害，而不是先 {@code kill()} 再补 {@code hurt}：
     * {@code kill()} 本身就是 {@code hurt(genericKill, MAX_VALUE)}，调用后实体立即进入
     * 死亡状态，后续 {@code hurt} 会被 {@code isDeadOrDying()} 挡掉，
     * 死亡消息就会显示成原版的 {@code genericKill} 而非这里的自定义死因。
     *
     * <p>伤害取 {@code Float.MAX_VALUE}：这是剧情性的确定死亡，不该被护甲、抗性提升、
     * 伤害吸收或无敌帧削减，也不该出现「掉一半血活下来」的中间态。
     *
     * <p>生物走 {@code hurt} 以便带上死因文案；非生物（如乘客再载着的盔甲架）
     * 拿不到死亡消息，直接 {@code kill()} 掉，免得留在原地。
     * 原版 {@code Entity#hurt} 只做标记、不会致死，所以这里必须分开处理。
     *
     * <p>取递归乘客（含间接）而非 {@link #getPassengers()}：范围伤害已按 {@link #isRider}
     * 把间接乘客排除在外，这里要一并处理，否则它会既不吃爆炸也不被判死。
     *
     * <p>先拷贝再遍历：死亡会触发 {@code stopRiding} 改动乘客列表。
     */
    private void killRiders(ServerLevel level) {
        // getIndirectPassengers() 是惰性视图，先实体化再遍历：死亡会改动乘客列表。
        List<Entity> riders = new ArrayList<>();
        this.getIndirectPassengers().forEach(riders::add);
        if (riders.isEmpty()) {
            return;
        }
        DamageSource source = AddonDamageTypes.escapeTowerFailure(level, this);
        for (Entity rider : riders) {
            if (rider instanceof LivingEntity) {
                rider.hurt(source, Float.MAX_VALUE);
            } else {
                rider.kill();
            }
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
        for (int ball = 0; ball < LargeFireworksRocketBurst.BALLS_PER_STAR; ball++) {
            Vec3 offset = randomOffset(random, radius);
            LargeFireworksRocketBurst.spawn(
                this.level(),
                random,
                this.getX() + offset.x,
                this.blastY() + offset.y,
                this.getZ() + offset.z,
                explosion,
                LargeFireworksRocketBurst.ballScale(random, LargeFireworksRocketBurst.randomScale(random))
            );
        }
    }

    /**
     * 在半径为 {@code radius} 的球内均匀取一点。
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
        tag.putInt("Pearls", this.getPearls());
        if (!this.pearlOwners.isEmpty()) {
            ListTag owners = new ListTag();
            for (UUID owner : this.pearlOwners) {
                owners.add(StringTag.valueOf(owner.toString()));
            }
            tag.put("PearlOwners", owners);
        }
        // 存下登月判定，否则区块重载会重掷骰子：一次已判失败的飞行可能因此又成功。
        if (this.moonTravelSucceeds != null) {
            tag.putBoolean("MoonTravelSucceeds", this.moonTravelSucceeds);
        }
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
        List<UUID> owners = new ArrayList<>();
        if (tag.contains("PearlOwners", Tag.TAG_LIST)) {
            for (Tag element : tag.getList("PearlOwners", Tag.TAG_STRING)) {
                try {
                    owners.add(UUID.fromString(element.getAsString()));
                } catch (IllegalArgumentException error) {
                    AnvilCraftPigeonPlus.LOGGER.warn(
                        "巨型烟花：无法解析珍珠归属的 UUID，已跳过：{}", element.getAsString());
                }
            }
        }
        this.setPearlOwners(owners);
        // 只有存过才恢复，没存过就保持 null（= 尚未判定），让它照常在飞行中掷骰。
        if (tag.contains("MoonTravelSucceeds")) {
            this.moonTravelSucceeds = tag.getBoolean("MoonTravelSucceeds");
        }
        Direction.CODEC.parse(NbtOps.INSTANCE, tag.get("Facing"))
            .resultOrPartial(error -> AnvilCraftPigeonPlus.LOGGER.warn(
                "巨型烟花：无法读取朝向，按北处理：{}", error))
            .ifPresent(this::setFacing);
    }

    /**
     * 乘客坐在火箭顶面（实体原点是底面，往上抬一个箭体高度）。
     *
     * <p>乘客每 tick 由 {@code Entity#rideTick} 调 {@code vehicle.positionRider(this)}
     * 重新定位，所以这个偏移持续生效，人跟着火箭一起飞。
     *
     * <p><b>刻意不给乘客任何免伤</b>：坐在火箭顶上被自己的烟花炸死是预期行为。
     */
    @Override
    protected @NonNull Vec3 getPassengerAttachmentPoint(@NonNull Entity passenger, @NonNull EntityDimensions dimensions, float partialTick) {
        return new Vec3(0.0, BODY_HEIGHT, 0.0);
    }

    /**
     * 剔除用的包围箱要比实体本身高一些。
     *
     * <p>装了末影珍珠时，那颗珍珠悬在 {@link #PEARL_RENDER_HEIGHT}（约 9.6 格）处，
     * 而实体自身只有 9 格高；{@code EntityRenderer#shouldRender} 用的是
     * {@code getBoundingBoxForCulling().inflate(0.5)}，顶到 9.5 就没了，
     * 珍珠会连同整支火箭一起被视锥剔除。这里把它抬到珍珠之上。
     */
    @Override
    public @NonNull AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().expandTowards(0.0, PEARL_RENDER_HEIGHT + 1.0 - BODY_HEIGHT, 0.0);
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
    public boolean ignoreExplosion(@NonNull Explosion explosion) {
        return true;
    }
}
