package dev.anvilcraft.pigeonplus.client.render;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.FireworkParticles;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 巨型烟花的单个烟花球：按随机倍率放大后播放。
 *
 * <p>球的位置由调用方决定（见 {@code GiantFireworkEntity#spawnBurst}）——
 * 一颗烟火之星炸出 4 个球，这 4 个球与其它星的球一起在整个爆炸体积里随机撒开，
 * 而不是挤在这颗星附近。
 *
 * <p>只影响巨型烟花。原版 {@code Starter} 的 speed/radius 写死在 {@code switch(shape)} 里，
 * 且 {@code FireworkExplosion} 没有「大小」字段，所以这里不复用 {@code Level#createFireworks}，
 * 改为直接调用 {@code Starter} 的 public 生成方法。原版的 {@code FireworkShapeFactoryRegistry}
 * 也能改大小，但那是按 shape 全局注册、会波及所有烟花。
 *
 * <p>球的大小由 <b>speed</b> 决定（radius 只决定粒子数与壳层分辨率），
 * 因此半径必须与 speed 同倍率放大，否则球变大反而变稀。
 */
public final class GiantFireworkBurst {
    /** 每颗烟火之星炸出的球数；装填上限 16 颗即最多 64 球。 */
    public static final int BALLS_PER_STAR = 4;
    /** 最小倍率 = 原版大小。 */
    private static final float MIN_SCALE = 1.0F;
    /**
     * 最大倍率。
     *
     * <p>粒子数 ∝ 半径²，所以这个值每 +0.5 开销约翻倍。实际开销还要乘
     * {@link #BALL_SCALE} 与球数，试算见该常量。
     */
    private static final float MAX_SCALE = 2.0F;
    /**
     * 单球相对该星倍率的系数。
     *
     * <p>取 0.5 让「1 星拆成 4 球」的总开销与「1 星 1 大球」持平：
     * 球面粒子数 ∝ 半径²，故 {@code 4 球 × 0.5² = 1}。
     * 于是 16 星最多 64 个普适大小的球，而不是 4 个特大球。
     */
    private static final float BALL_SCALE = 0.5F;
    /** 球之间的尺寸抖动幅度，避免 4 个球一模一样。 */
    private static final float BALL_JITTER = 0.3F;

    /** 与原版 {@code Starter.STAR_PARTICLE_COORDS} 一致；原版该字段是 private，只能照抄。 */
    private static final double[][] STAR_COORDS = {
        {0.0, 1.0},
        {0.3455, 0.309},
        {0.9511, 0.309},
        {0.3795918367346939, -0.12653061224489795},
        {0.6122448279591837, -0.8040816326530612},
        {0.0, -0.35918367346938773}
    };
    /** 与原版 {@code Starter.CREEPER_PARTICLE_COORDS} 一致。 */
    private static final double[][] CREEPER_COORDS = {
        {0.0, 0.2}, {0.2, 0.2}, {0.2, 0.6}, {0.6, 0.6}, {0.6, 0.2}, {0.2, 0.2},
        {0.2, 0.0}, {0.4, 0.0}, {0.4, -0.6}, {0.2, -0.6}, {0.2, -0.4}, {0.0, -0.4}
    };

    private GiantFireworkBurst() {
    }

    /** 随机倍率，下限为原版大小。 */
    public static float randomScale(RandomSource random) {
        return Mth.lerp(random.nextFloat(), MIN_SCALE, MAX_SCALE);
    }

    /** 把「该星倍率」换算成单个球的实际倍率（含抖动）。 */
    public static float ballScale(RandomSource random, float starScale) {
        return starScale * BALL_SCALE
               * (1.0F - BALL_JITTER / 2.0F + random.nextFloat() * BALL_JITTER);
    }

    /**
     * 在指定位置播放一个烟花球。
     *
     * <p>入参用 {@link Level} 而非 {@code ClientLevel}，好让双端的实体代码不必引用客户端类；
     * 类型判断与转换收在本类内部。
     */
    public static void spawn(
        Level level,
        RandomSource random,
        double x,
        double y,
        double z,
        FireworkExplosion explosion,
        float ballScale
    ) {
        if (!(level instanceof ClientLevel clientLevel)) {
            return;
        }
        ParticleEngine engine = Minecraft.getInstance().particleEngine;
        IntList colors = explosion.colors();
        if (colors.isEmpty()) {
            colors = IntList.of(DyeColor.BLACK.getFireworkColor());
        }
        IntList fadeColors = explosion.fadeColors();
        boolean trail = explosion.hasTrail();
        boolean twinkle = explosion.hasTwinkle();

        double speed = ballSpeed(explosion.shape(), ballScale);
        // 借原版 Starter 当粒子工厂：它的 createParticleXxx 是 public，
        // 内部已正确处理颜色 / 拖尾 / 闪烁 / 淡出。BURST 形状会用到这里给的速度，其余只用位置。
        double angle = random.nextDouble() * Math.PI * 2.0;
        FireworkParticles.Starter factory = new FireworkParticles.Starter(
            clientLevel, x, y, z,
            Math.cos(angle) * speed, speed * 0.5, Math.sin(angle) * speed,
            engine, List.of(explosion)
        );

        switch (explosion.shape()) {
            case SMALL_BALL -> factory.createParticleBall(
                0.25 * ballScale, ballRadius(2, ballScale), colors, fadeColors, trail, twinkle);
            case LARGE_BALL -> factory.createParticleBall(
                0.5 * ballScale, ballRadius(4, ballScale), colors, fadeColors, trail, twinkle);
            case STAR -> factory.createParticleShape(
                0.5 * ballScale, STAR_COORDS, colors, fadeColors, trail, twinkle, false);
            case CREEPER -> factory.createParticleShape(
                0.5 * ballScale, CREEPER_COORDS, colors, fadeColors, trail, twinkle, true);
            case BURST -> factory.createParticleBurst(colors, fadeColors, trail, twinkle);
        }

        // 原版会在爆点叠一个带色闪光，这里保持一致
        Particle flash = engine.createParticle(ParticleTypes.FLASH, x, y, z, 0.0, 0.0, 0.0);
        if (flash != null) {
            int color = colors.getInt(0);
            flash.setColor(
                FastColor.ARGB32.red(color) / 255.0F,
                FastColor.ARGB32.green(color) / 255.0F,
                FastColor.ARGB32.blue(color) / 255.0F
            );
        }
    }

    /** 该形状对应的球速（决定球的大小）。 */
    private static double ballSpeed(FireworkExplosion.Shape shape, float scale) {
        return switch (shape) {
            case SMALL_BALL -> 0.25 * scale;
            case LARGE_BALL, STAR, CREEPER, BURST -> 0.5 * scale;
        };
    }

    /** 半径与 speed 同倍率放大，以保住球面密度（粒子数 ∝ r²）。 */
    private static int ballRadius(int baseRadius, float scale) {
        return Math.max(1, Math.round(baseRadius * scale));
    }
}
