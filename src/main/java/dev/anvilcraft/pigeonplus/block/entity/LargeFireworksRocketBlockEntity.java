package dev.anvilcraft.pigeonplus.block.entity;

import com.mojang.serialization.Codec;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 巨型烟花的方块实体：存放玩家装填的烟火之星，每颗对应一条 {@link FireworkExplosion}
 * （形状 / 颜色 / 淡出色 / 拖尾 / 闪烁），发射时整份交给实体去播真正的原版烟花粒子。
 *
 * <p>只在主部件（{@code Y0_C}）持有，多方块是一体的，不必 81 格各存一份。
 * 也不做网络同步：装填数量属服务端状态，客户端渲染与粒子都不依赖它。
 */
public class LargeFireworksRocketBlockEntity extends BlockEntity {
    /** 可装填的烟火之星上限。 */
    public static final int MAX_STARS = 16;

    private static final Codec<List<FireworkExplosion>> EXPLOSIONS_CODEC =
        FireworkExplosion.CODEC.listOf();
    private static final String TAG_EXPLOSIONS = "Explosions";

    private final List<FireworkExplosion> explosions = new ArrayList<>();

    public LargeFireworksRocketBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LARGE_FIREWORKS_ROCKET.get(), pos, state);
    }

    /** 已装填的烟火之星（不可变副本）。 */
    public List<FireworkExplosion> getExplosions() {
        return List.copyOf(this.explosions);
    }

    public int getStarCount() {
        return this.explosions.size();
    }

    public boolean isFull() {
        return this.explosions.size() >= MAX_STARS;
    }

    /** 装填一个烟火之星，已满则返回 {@code false}。 */
    public boolean addExplosion(FireworkExplosion explosion) {
        if (this.isFull()) {
            return false;
        }
        this.explosions.add(explosion);
        this.setChanged();
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.explosions.isEmpty()) {
            return;
        }
        EXPLOSIONS_CODEC.encodeStart(NbtOps.INSTANCE, this.explosions)
            .resultOrPartial(error -> AnvilCraftPigeonPlus.LOGGER.warn(
                "巨型烟花：无法序列化烟火之星，本次装填将丢失：{}", error))
            .ifPresent(encoded -> tag.put(TAG_EXPLOSIONS, encoded));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.explosions.clear();
        if (!tag.contains(TAG_EXPLOSIONS)) {
            return;
        }
        EXPLOSIONS_CODEC.parse(NbtOps.INSTANCE, tag.get(TAG_EXPLOSIONS))
            .resultOrPartial(error -> AnvilCraftPigeonPlus.LOGGER.warn(
                "巨型烟花：无法读取烟火之星，按空处理：{}", error))
            .ifPresent(loaded -> {
                this.explosions.clear();
                // 存档可能来自把上限调大的旧版本，这里仍然夹住，避免超限触发 Fireworks 的校验异常。
                this.explosions.addAll(loaded.subList(0, Math.min(loaded.size(), MAX_STARS)));
            });
    }
}
