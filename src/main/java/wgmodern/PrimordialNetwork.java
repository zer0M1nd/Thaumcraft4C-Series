package wgmodern;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.network.NetworkDirection;
public class PrimordialNetwork {
 public record Action(int kind,int selection){}
 public static void init(){WGNetwork.CHANNEL.messageBuilder(Action.class,6,NetworkDirection.PLAY_TO_SERVER).encoder((a,b)->{b.writeByte(a.kind());b.writeByte(a.selection());}).decoder(b->new Action(b.readUnsignedByte(),b.readUnsignedByte())).consumerMainThread((a,c)->{var p=c.get().getSender();if(p!=null&&p.isAlive()&&!p.isSpectator()&&p.containerMenu==p.inventoryMenu){if(a.kind()==0)PrimordialGlove.select(p.getMainHandItem(),a.selection());else if(a.kind()==1)PrimordialGlove.open(p,false);else if(a.kind()==2&&a.selection()<5){var slot=new EquipmentSlot[]{EquipmentSlot.MAINHAND,EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}[a.selection()];var s=p.getItemBySlot(slot);PrimordialGear.mode(s,PrimordialGear.mode(s)+1);p.getInventory().setChanged();}}c.get().setPacketHandled(true);}).add();}
}
