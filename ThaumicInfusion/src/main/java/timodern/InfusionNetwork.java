package timodern;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

public class InfusionNetwork {
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation("thaumicinfusion","main"),()->"1","1"::equals,"1"::equals);
    public static void init(){
        CHANNEL.messageBuilder(Sync.class,0,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->{b.writeBlockPos(m.pos);b.writeNbt(m.data);}).decoder(b->new Sync(b.readBlockPos(),b.readNbt())).consumerMainThread((m,c)->InfusionClient.receive(m)).add();
        CHANNEL.messageBuilder(Select.class,1,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{b.writeEnum(m.hand);b.writeUtf(m.aspect,64);}).decoder(b->new Select(b.readEnum(InteractionHand.class),b.readUtf(64))).consumerMainThread((m,c)->{
            var p=c.get().getSender();if(p==null)return;var wand=p.getItemInHand(m.hand);
            if(!(tc4revival.content.WandFocusItem.attached(wand).getItem() instanceof InfusingFocus))return;
            if(!m.aspect.isEmpty()&&(InfusionEffects.get(m.aspect)==null||!InfusionConfig.enabled(m.aspect)||tc4revival.aspect.TCAspects.catalog().find(m.aspect).isEmpty()))return;
            if(!m.aspect.isEmpty()&&!p.isCreative()&&!tc4revival.research.KnowledgeSavedData.get(p.server).snapshot(p.getUUID()).map(k->k.knows(tc4revival.aspect.TCAspects.catalog().require(m.aspect))).orElse(false))return;
            wand.getOrCreateTag().putString("InfusionAspect",m.aspect);p.containerMenu.broadcastChanges();
        }).add();
    }
    public record Sync(BlockPos pos,CompoundTag data){}
    public record Select(InteractionHand hand,String aspect){}
    public static void sync(ServerLevel l,BlockPos p,InfusionData.Entry e){CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(()->l.getChunkAt(p)),new Sync(p,e==null?new CompoundTag():e.save()));}
    public static void send(ServerPlayer p,BlockPos pos,InfusionData.Entry e){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Sync(pos,e.save()));}
}
