package conciliummodern;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** A saved name or dimension/position reference, independent of optional client classes. */
public final class MonitorSource {
    public static CompoundTag from(ItemStack stack){
        var result=new CompoundTag();var tag=stack.getTag();if(tag==null)return result;
        if(stack.getItem() instanceof tc4revival.content.CastingWandItem&&!tag.getString("Xylography").isBlank()){result.putString("player",tag.getString("Xylography"));return result;}
        if(!net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).equals(ResourceLocation.parse("automagy:crystal_eye")))return result;
        if(tag.contains("Target",Tag.TAG_LONG)&&ResourceLocation.tryParse(tag.getString("Dimension"))!=null){result.putLong("pos",tag.getLong("Target"));result.putString("dimension",tag.getString("Dimension"));}
        else if(tag.contains("lookX")){
            String dimension=switch(tag.getInt("lookDim")){case -1->"minecraft:the_nether";case 0->"minecraft:overworld";case 1->"minecraft:the_end";default->"";};
            if(!dimension.isEmpty()){result.putLong("pos",new BlockPos(tag.getInt("lookX"),tag.getInt("lookY"),tag.getInt("lookZ")).asLong());result.putString("dimension",dimension);}
        }
        return result;
    }
    public static int power(ServerPlayer viewer,CompoundTag source,boolean crossDimension){
        if(!source.contains("pos",Tag.TAG_LONG))return -1;var dimension=ResourceLocation.tryParse(source.getString("dimension"));if(dimension==null)return -1;
        if(!crossDimension&&!dimension.equals(viewer.level().dimension().location()))return -1;
        var level=viewer.server.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,dimension));var pos=BlockPos.of(source.getLong("pos"));
        return level!=null&&level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof RedPoweredMindBlockEntity mind?mind.power():-1;
    }
    public static int[] playerValues(ServerPlayer viewer,CompoundTag source){
        String name=source.getString("player");var target=name.isBlank()?null:viewer.server.getPlayerList().getPlayerByName(name);
        return target!=null&&target.level()==viewer.level()?AstralMonitor.sample(target):new int[]{-1,-1,-1,-1};
    }
    private MonitorSource(){}
}
