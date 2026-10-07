package dev.anvilcraft.pigeonplus.block.entity;

import com.mojang.serialization.Codec;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    /**
     * 可装填的末影珍珠上限。
     */
    public static final int MAX_PEARLS = 4;

    private static final Codec<List<FireworkExplosion>> EXPLOSIONS_CODEC =
        FireworkExplosion.CODEC.listOf();
    private static final String TAG_EXPLOSIONS = "Explosions";
    private static final String TAG_PEARL_OWNERS = "PearlOwners";

    private final List<FireworkExplosion> explosions = new ArrayList<>();

    /**
     * 已装填的末影珍珠各自绑定的玩家。
     */
    private final List<UUID> pearlOwners = new ArrayList<>();

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

    /** 已装填的末影珍珠数量。 */
    public int getPearlCount() {
        return this.pearlOwners.size();
    }

    /** 已装填的珍珠各自绑定的玩家（不可变副本）。 */
    public List<UUID> getPearlOwners() {
        return List.copyOf(this.pearlOwners);
    }

    public boolean isPearlFull() {
        return this.pearlOwners.size() >= MAX_PEARLS;
    }

    /**
     * 装填一颗末影珍珠，绑定到投放它的玩家；已满则返回 {@code false}。
     *
     * <p>同一名玩家可以放多颗，但那样只会让他被传送多次（没有额外意义），
     * 这里不做限制——保持行为简单可预期。
     */
    public boolean addPearl(UUID owner) {
        if (this.isPearlFull()) {
            return false;
        }
        this.pearlOwners.add(owner);
        this.setChanged();
        this.syncPearls();
        return true;
    }

    /**
     * 把珍珠数据推给客户端。
     *
     * <p>方块实体的装填状态默认<b>不同步</b>（客户端渲染本来不需要它），
     * 但现在箭尖上的那颗悬浮珍珠要按珍珠数量决定画不画，客户端就必须知道。
     * 少了这一步，客户端读到的永远是 0 颗，珍珠一次也不会显示。
     */
    private void syncPearls() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        this.level.sendBlockUpdated(
            this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
    }

    /** 把珍珠归属写进标签；两种用途（存档 / 同步包）共用同一份格式。 */
    private void writePearls(CompoundTag tag) {
        if (this.pearlOwners.isEmpty()) {
            return;
        }
        ListTag owners = new ListTag();
        for (UUID owner : this.pearlOwners) {
            owners.add(StringTag.valueOf(owner.toString()));
        }
        tag.put(TAG_PEARL_OWNERS, owners);
    }

    /**
     * 方块实体同步包的内容。
     *
     * <p>只带珍珠：箭体与烟火之星都只由服务端使用，客户端渲染不依赖它们。
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        this.writePearls(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        this.writePearls(tag);
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
        this.pearlOwners.clear();
        if (tag.contains(TAG_PEARL_OWNERS, Tag.TAG_LIST)) {
            for (Tag element : tag.getList(TAG_PEARL_OWNERS, Tag.TAG_STRING)) {
                try {
                    if (!this.isPearlFull()) {
                        this.pearlOwners.add(UUID.fromString(element.getAsString()));
                    }
                } catch (IllegalArgumentException error) {
                    AnvilCraftPigeonPlus.LOGGER.warn(
                        "巨型烟花：无法解析珍珠归属的 UUID，已跳过：{}", element.getAsString());
                }
            }
        }
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
