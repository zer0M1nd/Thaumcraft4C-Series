package dyesmodern;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;

public final class DyesNetwork {
    private static final net.minecraftforge.network.simple.SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.parse("thaumicdyes:effects"),()->"1","1"::equals,"1"::equals);
    public record MaskBeam(ResourceLocation dimension,int sourceId,int targetId){}
    private DyesNetwork(){}
    public static void register(){CHANNEL.messageBuilder(MaskBeam.class,0,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((packet,buffer)->{buffer.writeResourceLocation(packet.dimension());buffer.writeVarInt(packet.sourceId());buffer.writeVarInt(packet.targetId());})
            .decoder(buffer->new MaskBeam(buffer.readResourceLocation(),buffer.readVarInt(),buffer.readVarInt()))
            .consumerMainThread((packet,context)->{var connection=context.get().getNetworkManager();DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->dyesmodern.client.DyesClientEffects.receiveMaskBeam(packet,connection));context.get().setPacketHandled(true);}).add();}
    public static void maskBeam(Entity source,Entity target){CHANNEL.send(PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(source.getX(),source.getY(),source.getZ(),64,source.level().dimension())),new MaskBeam(source.level().dimension().location(),source.getId(),target.getId()));}
}
