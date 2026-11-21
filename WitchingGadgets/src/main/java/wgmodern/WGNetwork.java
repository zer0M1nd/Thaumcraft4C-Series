package wgmodern;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;

public class WGNetwork {
    public static final SimpleChannel CHANNEL=NetworkRegistry.ChannelBuilder.named(new ResourceLocation("witchinggadgets","main")).networkProtocolVersion(()->"1").clientAcceptedVersions("1"::equals).serverAcceptedVersions("1"::equals).simpleChannel();
    public static class Sauna {public Sauna(){}}
    public static class Activate {public final boolean kama;public Activate(boolean kama){this.kama=kama;}}
    public record GemEffect(int kind,net.minecraft.core.BlockPos pos,int color){}
    public record CameraPreview(net.minecraft.nbt.CompoundTag data){}
    public record OpenPouch(){}
    public static class DoubleJump {public DoubleJump(){}}
    public static void init(){PrimordialNetwork.init();CHANNEL.messageBuilder(OpenPouch.class,5,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{}).decoder(b->new OpenPouch()).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null&&p.containerMenu==p.inventoryMenu)EquippedPouchMenu.open(p);c.get().setPacketHandled(true);}).add();CHANNEL.messageBuilder(CameraPreview.class,4,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->b.writeNbt(m.data())).decoder(b->new CameraPreview(b.readNbt())).consumerMainThread((m,c)->{DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->CameraClient.receive(m.data()));c.get().setPacketHandled(true);}).add();CHANNEL.messageBuilder(GemEffect.class,3,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->{b.writeByte(m.kind());b.writeBlockPos(m.pos());b.writeInt(m.color());}).decoder(b->new GemEffect(b.readUnsignedByte(),b.readBlockPos(),b.readInt())).consumerMainThread((m,c)->{DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->GemEffects.receive(m));c.get().setPacketHandled(true);}).add();CHANNEL.messageBuilder(Sauna.class,0,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->{}).decoder(b->new Sauna()).consumerMainThread((m,c)->{DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->WGEffects::sauna);c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(Activate.class,1,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->b.writeBoolean(m.kama)).decoder(b->new Activate(b.readBoolean())).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null&&p.isAlive()&&!p.isSpectator()&&(p.level().getGameTime()-p.getPersistentData().getLong("WGCloakActionTime")>=5||p.level().getGameTime()<p.getPersistentData().getLong("WGCloakActionTime"))){p.getPersistentData().putLong("WGCloakActionTime",p.level().getGameTime());for(var slot:CloakItem.equipped(p))if(slot.stack().getItem() instanceof CloakItem cloak&&cloak.kama==m.kama){cloak.activate(p,slot);break;}}c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(DoubleJump.class,2,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{}).decoder(b->new DoubleJump()).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)WGAccessories.doubleJump(p);c.get().setPacketHandled(true);}).add();}
    public static void gemEffect(ServerPlayer p,int kind,net.minecraft.core.BlockPos pos,int color){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new GemEffect(kind,pos,color));}
    public static void sauna(ServerPlayer player){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Sauna());}
    public static void cameraPreview(ServerPlayer player,net.minecraft.nbt.CompoundTag data){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new CameraPreview(data));}
}
