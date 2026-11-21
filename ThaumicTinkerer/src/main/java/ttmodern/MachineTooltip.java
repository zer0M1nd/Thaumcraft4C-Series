package ttmodern;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Server-owned, display-only state. Never sends inventories or complete block NBT. */
public final class MachineTooltip {
    public static List<Component> lines(BlockEntity entity){
        var lines=new ArrayList<Component>();if(entity==null||entity.isRemoved())return lines;
        if(entity instanceof TabletEntity tablet){
            var stack=tablet.items.getStackInSlot(0);lines.add(Component.translatable("ttwaila.currentTool",stack.isEmpty()?Component.translatable("ttwaila.nothing"):stack.getHoverName()));
            if(!stack.isEmpty()){lines.add(Component.translatable(tablet.leftClick()?"ttwaila.leftClick":"ttwaila.rightClick"));lines.add(Component.translatable(tablet.redstone()?"ttwaila.redstone":"ttwaila.autonomous"));}
        }else if(entity instanceof InterfaceEntity link){
            var target=link.destination();lines.add(Component.translatable("ttwaila.connected",target==null?Component.translatable("ttwaila.nothing"):target.getBlockState().getBlock().getName()));
            if(target!=null){var pos=target.getBlockPos();lines.add(Component.literal("x: "+pos.getX()+" y: "+pos.getY()+" z: "+pos.getZ()));}
        }else if(entity instanceof RestorerEntity restorer){
            var stack=restorer.inventory.getStackInSlot(0);if(!stack.isEmpty())lines.add(Component.translatable(stack.isDamaged()?"ttwaila.repairing":"ttwaila.finishedRepairing",stack.getHoverName()));
        }else if(entity instanceof WarpGateEntity gate){
            lines.add(Component.translatable(gate.locked()?"ttwaila.disallowIncoming":"ttwaila.allowIncoming"));
        }else if(entity instanceof MagnetEntity magnet){
            String mode=magnet.pulling()?"pulling":"pushing";lines.add(Component.translatable("ttwaila."+mode+"Mode"));
            if(magnet.mob()){
                var stack=magnet.filter.getStackInSlot(0);String age=magnet.adult()?"Adult":"Child";
                lines.add(SoulMouldItem.pattern(stack).isEmpty()?Component.translatable("ttwaila."+mode+age):Component.translatable("ttwaila."+mode+age+"Type",SoulMouldItem.patternName(stack)));
            }
        }
        return lines;
    }
    public static CompoundTag snapshot(BlockEntity entity){var data=new CompoundTag();var values=new ListTag();for(var line:lines(entity)){String json=Component.Serializer.toJson(line);if(json.length()<=8192)values.add(StringTag.valueOf(json));}data.put("Lines",values);return data;}
    public static List<Component> read(CompoundTag data){var lines=new ArrayList<Component>();var values=data.getList("Lines",Tag.TAG_STRING);for(int i=0;i<Math.min(16,values.size());i++){String json=values.getString(i);if(json.length()>8192)continue;try{var line=Component.Serializer.fromJson(json);if(line!=null)lines.add(line);}catch(RuntimeException ignored){}}return lines;}
    private MachineTooltip(){}
}
