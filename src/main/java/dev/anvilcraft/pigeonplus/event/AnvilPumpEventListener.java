package dev.anvilcraft.pigeonplus.event;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.block.entity.AnvilPumpBlockEntity;
import dev.anvilcraft.pigeonplus.block.entity.FeedSpreaderBlockEntity;
import dev.anvilcraft.pigeonplus.network.AnvilPumpPressPacket;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID)
public class AnvilPumpEventListener {
    @SubscribeEvent
    public static void onAnvilLand(AnvilEvent.OnLand event) {
        Level level = event.getLevel();
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos pumpPos = event.getPos().below();
        if (level.getBlockEntity(pumpPos) instanceof AnvilPumpBlockEntity pump) {
            if (pump.activate(event.getFallDistance())) {
                AnvilPumpEventListener.notifyPress(serverLevel, pumpPos, false);
            }
            return;
        }
        if (level.getBlockEntity(pumpPos) instanceof FeedSpreaderBlockEntity feedSpreader) {
            feedSpreader.activate(event.getFallDistance());
            AnvilPumpEventListener.notifyPress(serverLevel, pumpPos, true);
        }
    }

    private static void notifyPress(ServerLevel level, BlockPos pos, boolean spreader) {
        PacketDistributor.sendToPlayersTrackingChunk(
            level,
            new net.minecraft.world.level.ChunkPos(pos),
            new AnvilPumpPressPacket(pos, spreader)
        );
    }
}
