package dev.anvilcraft.pigeonplus.event;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.fluid.PotionFluidUtil;
import dev.anvilcraft.pigeonplus.util.PotionContainerGeometry;
import dev.dubhe.anvilcraft.api.event.FishTankEvent;
import dev.dubhe.anvilcraft.api.event.LargeCauldronEvent;
import dev.dubhe.anvilcraft.api.fluid.LargeCauldronFluidHandler;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * 生物泡在装有药水的鱼缸或大型炼药锅里时，消耗药水获得对应效果。
 */
@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID)
public class PotionSoakEffectListener {
    private static final int SOAK_COST = 250;
    private static final int CHECK_INTERVAL_TICKS = 20;

    private static final String CHECK_STAMP = "anvilcraft_pigeon_plus.potion_soak_check";

    @SubscribeEvent
    public static void onFishTankInside(FishTankEvent.EntityInside event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        FluidTank tank = event.getTank().getFluidHandler();
        apply(event.getEntity(), tank, PotionContainerGeometry.fishTankSurfaceY(event.getPos(), tank));
    }

    @SubscribeEvent
    public static void onCauldronInside(LargeCauldronEvent.EntityInside event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        LargeCauldronBlockEntity main = mainCauldron(level, event.getPos(), event.getState());
        if (main == null) {
            return;
        }
        LargeCauldronFluidHandler fluids = main.getFluids();
        apply(event.getEntity(), fluids, PotionContainerGeometry.cauldronSurfaceY(main.getBlockPos(), fluids));
    }

    private static void apply(Entity entity, IFluidHandler fluids, double surfaceY) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        // entityInside 是按「包围盒重叠的方块位置」触发的，不看方块形状，
        // 所以站在锅沿或锅顶也会被判定为在锅内。这里要求脚部低于液面，才算真正泡进药水。
        if (entity.getY() >= surfaceY) {
            return;
        }
        if (!claimCheck(living, living.level().getGameTime())) {
            return;
        }
        for (FluidStack potion : PotionFluidUtil.allPotions(fluids)) {
            PotionContents contents = PotionFluidUtil.getContents(potion);
            if (contents == null || shouldSkip(living, contents)) {
                continue;
            }
            FluidStack cost = potion.copyWithAmount(SOAK_COST);
            // 先模拟再执行，避免药水不足时抽出零头却不给效果。
            if (fluids.drain(cost, IFluidHandler.FluidAction.SIMULATE).getAmount() < SOAK_COST) {
                continue;
            }
            fluids.drain(cost, IFluidHandler.FluidAction.EXECUTE);
            applyEffects(living, contents);
        }
    }

    // 已有该效果（且强度不低）就不消耗；没有效果可给的药水（水、粗制）也跳过。
    private static boolean shouldSkip(LivingEntity living, PotionContents contents) {
        for (MobEffectInstance instance : contents.getAllEffects()) {
            if (instance.getEffect().value().isInstantenous()) {
                // 瞬时效果不会留存，无法「已拥有」，所以每次都该结算。
                return false;
            }
            MobEffectInstance current = living.getEffect(instance.getEffect());
            if (current == null || current.getAmplifier() < instance.getAmplifier()) {
                return false;
            }
        }
        return true;
    }

    private static void applyEffects(LivingEntity living, PotionContents contents) {
        for (MobEffectInstance instance : contents.getAllEffects()) {
            MobEffect effect = instance.getEffect().value();
            if (effect.isInstantenous()) {
                effect.applyInstantenousEffect(null, null, living, instance.getAmplifier(), 1.0);
            } else {
                // 沿用该药水自身的时长，等价于把这一瓶药水喝下去。
                living.addEffect(new MobEffectInstance(instance));
            }
        }
    }

    // 一次 tick 内 entityInside 可能触发多次（3x3 的锅有多个部件），
    // 用实体自身的持久数据记住上次结算的 tick，既去重又实现间隔，无需静态状态。
    // getPersistentData 是 Entity 上的方法，所以对任何生物都适用。
    private static boolean claimCheck(LivingEntity living, long tick) {
        CompoundTag data = living.getPersistentData();
        long last = data.getLong(CHECK_STAMP);
        if (last != 0L && tick - last < CHECK_INTERVAL_TICKS) {
            return false;
        }
        data.putLong(CHECK_STAMP, tick);
        return true;
    }

    @Nullable
    private static LargeCauldronBlockEntity mainCauldron(Level level, BlockPos pos, BlockState state) {
        LargeCauldronBlockEntity main = LargeCauldronBlockEntity.getMain(level, pos, state);
        return main != null && main.isMainPart() ? main : null;
    }
}
