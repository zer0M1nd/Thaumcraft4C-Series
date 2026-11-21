package thaumicomniverse.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

public class CreateArcaneNetwork {
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.parse("thaumicomniverse:arcane_create"),()->"1","1"::equals,"1"::equals);
    public record Update(int entity,BlockPos local,CompoundTag tag){}
    public static void register(){
        CHANNEL.messageBuilder(Update.class,0,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((p,b)->{b.writeVarInt(p.entity);b.writeBlockPos(p.local);b.writeNbt(p.tag);})
            .decoder(b->new Update(b.readVarInt(),b.readBlockPos(),b.readNbt()))
            .consumerMainThread((p,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->CreateArcaneClient.receive(p))).add();
    }
    public static void sync(AbstractContraptionEntity entity,BlockPos local,CompoundTag tag){
        if(!entity.level().isClientSide)CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(()->entity),new Update(entity.getId(),local,tag));
    }
}
