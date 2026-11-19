package conciliummodern;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.*;

/** Retains the consumed BlockState so its original fragments survive the authoritative block removal. */
final class GemCaptureNetwork {
    private static final net.minecraftforge.network.simple.SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(
            ResourceLocation.parse("thaumicconcilium:gem_capture"),()->"1","1"::equals,"1"::equals);
    record Capture(ResourceLocation dimension,BlockPos source,BlockState state,Vec3 target){}
    static void init(){
        CHANNEL.messageBuilder(Capture.class,0,NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet,buffer)->{buffer.writeResourceLocation(packet.dimension);buffer.writeBlockPos(packet.source);buffer.writeVarInt(Block.getId(packet.state));buffer.writeDouble(packet.target.x);buffer.writeDouble(packet.target.y);buffer.writeDouble(packet.target.z);})
                .decoder(buffer->new Capture(buffer.readResourceLocation(),buffer.readBlockPos(),Block.stateById(buffer.readVarInt()),new Vec3(buffer.readDouble(),buffer.readDouble(),buffer.readDouble())))
                .consumerMainThread((packet,context)->{var connection=context.get().getNetworkManager();net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->GemCaptureEffects.receive(packet,connection));context.get().setPacketHandled(true);}).add();
    }
    static void emit(Level level,BlockPos source,BlockState state,Vec3 target){
        if(level.isClientSide)return;
        CHANNEL.send(PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(source.getX(),source.getY(),source.getZ(),64,level.dimension())),new Capture(level.dimension().location(),source.immutable(),state,target));
    }
    private GemCaptureNetwork(){}
}
