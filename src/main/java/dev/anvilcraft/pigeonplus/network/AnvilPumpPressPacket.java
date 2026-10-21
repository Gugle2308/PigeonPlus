package dev.anvilcraft.pigeonplus.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.block.entity.AnvilPumpBlockEntity;
import dev.anvilcraft.pigeonplus.block.entity.FeedSpreaderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record AnvilPumpPressPacket(BlockPos pos, boolean spreader) implements IClientboundPacket {
    public static final Type<AnvilPumpPressPacket> TYPE =
        IPacket.type(AnvilCraftPigeonPlus.of("anvil_pump_press"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AnvilPumpPressPacket> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            AnvilPumpPressPacket::pos,
            ByteBufCodecs.BOOL,
            AnvilPumpPressPacket::spreader,
            AnvilPumpPressPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handleOnClient(Player player) {
        if (this.spreader) {
            if (player.level().getBlockEntity(this.pos) instanceof FeedSpreaderBlockEntity entity) {
                entity.startPistonPressAnimation();
            }
            return;
        }
        if (player.level().getBlockEntity(this.pos) instanceof AnvilPumpBlockEntity entity) {
            entity.startPistonPressAnimation();
        }
    }
}
