package tc4revival.network;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import tc4revival.content.InfusionRisk;
import tc4revival.content.InfusionPreview;
import tc4revival.aspect.*;
public record InfusionRiskReply(BlockPos pos,InfusionRisk risk,boolean repeat,AspectAmounts required,AspectAmounts available,
        Integer environment,java.util.List<InfusionPreview.Item> items) {
    public InfusionRiskReply { items=java.util.List.copyOf(items); }
    public InfusionRiskReply(BlockPos pos,InfusionRisk risk,boolean repeat,AspectAmounts required,AspectAmounts available) {
        this(pos,risk,repeat,required,available,risk==null?null:risk.environment(),java.util.List.of());
    }
    public InfusionRiskReply(BlockPos pos,InfusionRisk risk,boolean repeat) { this(pos,risk,repeat,new AspectAmounts(),new AspectAmounts()); }
    public InfusionRiskReply(BlockPos pos,InfusionRisk risk) { this(pos,risk,false,new AspectAmounts(),new AspectAmounts()); }
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);buf.writeBoolean(repeat);buf.writeBoolean(risk!=null);
        if(risk!=null){buf.writeInt(risk.recipe());buf.writeInt(risk.environment());buf.writeInt(risk.actual());}
        var tag=new net.minecraft.nbt.CompoundTag();AspectNbt.write(tag,"required",required);AspectNbt.write(tag,"available",available);buf.writeNbt(tag);
        buf.writeBoolean(environment!=null);if(environment!=null)buf.writeInt(environment);
        buf.writeCollection(items,(out,item)->{out.writeItem(item.stack());out.writeEnum(item.mark());});
    }
    public static InfusionRiskReply decode(FriendlyByteBuf buf) {
        var pos=buf.readBlockPos();boolean repeat=buf.readBoolean();var risk=buf.readBoolean()?new InfusionRisk(buf.readInt(),buf.readInt(),buf.readInt()):null;
        var tag=buf.readNbt();if(tag==null)tag=new net.minecraft.nbt.CompoundTag();
        Integer environment=buf.readBoolean()?buf.readInt():null;
        int count=buf.readVarInt();if(count<0 || count>1024)throw new IllegalArgumentException("Infusion preview item count");
        var items=new java.util.ArrayList<InfusionPreview.Item>(count);
        for(int i=0;i<count;i++)items.add(new InfusionPreview.Item(buf.readItem(),buf.readEnum(InfusionPreview.Mark.class)));
        return new InfusionRiskReply(pos,risk,repeat,AspectNbt.read(tag,"required",TCAspects.catalog()).amounts(),AspectNbt.read(tag,"available",TCAspects.catalog()).amounts(),environment,items);
    }
}
